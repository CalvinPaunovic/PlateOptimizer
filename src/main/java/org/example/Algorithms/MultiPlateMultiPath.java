package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.Provider.PlateProvider;

public class MultiPlateMultiPath {

    private final List<Plate> plates;
    private final List<MaxRectBF_MultiPath> multiPathAlgorithms;

    public MultiPlateMultiPath(List<PlateProvider.NamedPlate> plateInfos, List<Job> jobs) {
        this.plates = new ArrayList<>();
        this.multiPathAlgorithms = new ArrayList<>();

        // Platte 1 vorbereiten
        PlateProvider.NamedPlate info1 = plateInfos.get(0);
        Plate plate1 = new Plate("Platte 1: " + info1.name, info1.width, info1.height);
        plates.add(plate1);
        MaxRectBF_MultiPath multiPathAlgo1 = new MaxRectBF_MultiPath(plate1);

        // Liste für Jobs, die auf Platte 1 NICHT passen (egal in welchem Pfad, egal ob gedreht)
        List<Job> notPlaced = new ArrayList<>();

        for (Job job : jobs) {
            boolean placed = multiPathAlgo1.placeJob(job);
            if (!placed) {
                notPlaced.add(job);
            }
        }
        multiPathAlgorithms.add(multiPathAlgo1);

        // Falls es eine zweite Platte gibt, versuche dort die übrigen Jobs
        if (plateInfos.size() > 1 && !notPlaced.isEmpty()) {
            PlateProvider.NamedPlate info2 = plateInfos.get(1);
            Plate plate2 = new Plate("Platte 2: " + info2.name, info2.width, info2.height);
            plates.add(plate2);
            MaxRectBF_MultiPath multiPathAlgo2 = new MaxRectBF_MultiPath(plate2);

            for (Job job : notPlaced) {
                multiPathAlgo2.placeJob(job);
            }
            multiPathAlgorithms.add(multiPathAlgo2);
        }
    }

    // Visualisiert nur den ausgewählten Pfad jedes MultiPath-Algorithmus für jede Platte, mit 1 Sekunde Pause
    public void visualizeSelectedPath(String mode, String jobListInfo, int pathIndex) {
        for (int i = 0; i < multiPathAlgorithms.size(); i++) {
            MaxRectBF_MultiPath algo = multiPathAlgorithms.get(i);
            List<MaxRectBF_MultiPath.AlgorithmPath> paths = algo.getAllPaths();
            if (paths.size() > pathIndex) {
                MaxRectBF_MultiPath.AlgorithmPath path = paths.get(pathIndex);
                String title = plates.get(i).name + " | Pfad: " + path.pathDescription;
                org.example.Visualizer.PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                    path.plate, "5", path.freeRects, title, null, jobListInfo
                );
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Visualisiert alle Pfade jedes MultiPath-Algorithmus für jede Platte, mit 1 Sekunde Pause (bestehende Methode bleibt erhalten)
    public void visualizeAllPaths(String mode, String jobListInfo) {
        for (int i = 0; i < multiPathAlgorithms.size(); i++) {
            MaxRectBF_MultiPath algo = multiPathAlgorithms.get(i);
            List<MaxRectBF_MultiPath.AlgorithmPath> paths = algo.getAllPaths();
            for (int j = 0; j < paths.size(); j++) {
                MaxRectBF_MultiPath.AlgorithmPath path = paths.get(j);
                // Zeige alle Pfade, nicht nur die Endpfade
                String title = plates.get(i).name + " | Pfad: " + path.pathDescription;
                org.example.Visualizer.PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                    path.plate, "5", path.freeRects, title, null, jobListInfo
                );
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Debug-Ausgabe: Zeigt alle Zwischenschritte für den ausgewählten Pfad (per Index) auf der Konsole an
    public void debugPathSteps() {
        debugPathSteps(org.example.Main.DEBUG_MultiPlateMultiPath_PathIndex);
    }

    public void debugPathSteps(int pathIndex) {
        if (!org.example.Main.DEBUG_MultiPlateMultiPath) return;
        for (int plateIdx = 0; plateIdx < multiPathAlgorithms.size(); plateIdx++) {
            MaxRectBF_MultiPath algo = multiPathAlgorithms.get(plateIdx);
            List<MaxRectBF_MultiPath.AlgorithmPath> paths = algo.getAllPaths();
            if (paths.size() <= pathIndex) continue;
            MaxRectBF_MultiPath.AlgorithmPath path = paths.get(pathIndex);

            System.out.println("=== Debug: Zwischenschritte für Pfad " + (pathIndex + 1) + " (" + path.pathDescription + ") auf Platte " + (plateIdx + 1) + " ===");
            if (path.plate.jobs.isEmpty()) {
                System.out.println("Keine Jobs platziert.");
            }
            // Zeige nach jedem platzierten Job den Zustand der Platte und freien Rechtecke
            for (int step = 0; step < path.plate.jobs.size(); step++) {
                Job job = path.plate.jobs.get(step);
                System.out.printf("--- Nach Platzierung von Job %d: (%.2f x %.2f) an Position (%.2f, %.2f)%s ---\n",
                    job.id, job.width, job.height, job.x, job.y, job.rotated ? " [gedreht]" : "");
                System.out.printf("  Split: %s, PlacementOrder: %d\n", job.splittingMethod, job.placementOrder);

                // Zeige alle aktuell platzierten Jobs
                System.out.print("  Aktuell platzierte Jobs: ");
                for (int j = 0; j <= step; j++) {
                    Job placed = path.plate.jobs.get(j);
                    System.out.print(placed.id + (placed.rotated ? "(R)" : "") + " ");
                }
                System.out.println();

                // Zeige aktuelle freien Rechtecke (nach diesem Job)
                System.out.println("  Freie Rechtecke:");
                List<MaxRectBF_MultiPath.FreeRectangle> freeRects = path.freeRects;
                if (freeRects.isEmpty()) {
                    System.out.println("    Keine freien Rechtecke mehr.");
                } else {
                    for (int i = 0; i < freeRects.size(); i++) {
                        MaxRectBF_MultiPath.FreeRectangle rect = freeRects.get(i);
                        System.out.printf("    F%d: (%.2f, %.2f, %.2f, %.2f)\n", i, rect.x, rect.y, rect.width, rect.height);
                    }
                }
                System.out.println();
            }
            System.out.println("===============================================");
        }
        System.out.flush();
    }

    public List<Plate> getPlates() {
        return plates;
    }

    public List<MaxRectBF_MultiPath> getMultiPathAlgorithms() {
        return multiPathAlgorithms;
    }
}
