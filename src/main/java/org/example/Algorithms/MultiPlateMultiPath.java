package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;

/**
 * MultiPlateMultiPath ist eine Klasse, die mehrere Platten (Plates) verwaltet
 * und für jede Platte einen eigenen MaxRectBF_MultiPath-Algorithmus ausführt.
 * Ziel ist es, eine gegebene Liste von Jobs möglichst effizient auf mehrere Platten zu verteilen.
 * Dabei werden verschiedene Sortierstrategien und Platzierungswege (Paths) berücksichtigt.
 */
public class MultiPlateMultiPath {

    // Liste der verwendeten Platten (jede Platte entspricht einer Materialplatte)
    private final List<Plate> plates;
    // Für jede Platte wird ein eigener MultiPath-Algorithmus verwaltet
    private final List<MaxRectBF_MultiPath> multiPathAlgorithms;

    /**
     * Konstruktor: Initialisiert die MultiPlateMultiPath-Instanz.
     * Die eigentliche Platzierungslogik erfolgt über placeJobsOnPlates().
     */
    public MultiPlateMultiPath() {
        this.plates = new ArrayList<>();
        this.multiPathAlgorithms = new ArrayList<>();
    }

    // Ausgelagerte Methode für die eigentliche Platzierungslogik
    public void placeJobsOnPlates(List<Plate> plateInfos, List<Job> jobs, String jobListInfo) {
        this.plates.clear();
        this.multiPathAlgorithms.clear();
        List<Job> notPlaced = new ArrayList<>(jobs);
        for (int plateIdx = 0; plateIdx < plateInfos.size(); plateIdx++) {
            Plate info = plateInfos.get(plateIdx);
            Plate plate = new Plate("Platte " + (plateIdx + 1) + ": " + info.name, info.width, info.height);
            plates.add(plate);
            org.example.HelperClasses.JobUtils.sortJobsBySizeDescending(notPlaced);
            MaxRectBF_MultiPath algo = new MaxRectBF_MultiPath(plate);
            algo.debugEnabled = false;
            System.out.print("[DEBUG] Jobs an placeJob für Platte " + (plateIdx + 1) + ": ");
            for (int i = 0; i < notPlaced.size(); i++) {
                System.out.print(notPlaced.get(i).id);
                if (i < notPlaced.size() - 1) System.out.print(", ");
            }
            System.out.println();
            for (Job job : notPlaced) {
                algo.placeJob(job);
            }
            // Debug-Ausgabe: Zeige für jeden Pfad die nicht platzierten Jobs
            for (MaxRectBF_MultiPath.AlgorithmPath path : algo.getAllPaths()) {
                System.out.print("[DEBUG] Pfad '" + path.pathDescription + "' NICHT platzierte Jobs: ");
                for (int i = 0; i < path.failedJobs.size(); i++) {
                    System.out.print(path.failedJobs.get(i));
                    if (i < path.failedJobs.size() - 1) System.out.print(", ");
                }
                System.out.println();
            }
            // Für die nächste Platte: nur die failedJobs des Visualize-Path-Index übernehmen
            List<Job> nextNotPlaced = new ArrayList<>();
            if (algo.getAllPaths().size() > org.example.Main.Visualize_MultiPlateMultiPath_PathIndex) {
                MaxRectBF_MultiPath.AlgorithmPath visualizePath = algo.getAllPaths().get(org.example.Main.Visualize_MultiPlateMultiPath_PathIndex);
                for (Integer failedId : visualizePath.failedJobs) {
                    for (Job job : jobs) {
                        if (job.id == failedId) {
                            nextNotPlaced.add(job);
                            break;
                        }
                    }
                }
            }
            notPlaced = nextNotPlaced;
            // Debug-Ausgabe: Zeige alle nicht platzierten Jobs nach Abschluss der Platte
            System.out.print("[DEBUG] Nach Abschluss von Platte " + (plateIdx + 1) + " NICHT platzierte Jobs: ");
            for (int i = 0; i < notPlaced.size(); i++) {
                System.out.print(notPlaced.get(i).id);
                if (i < notPlaced.size() - 1) System.out.print(", ");
            }
            System.out.println();
            multiPathAlgorithms.add(algo);
            printPlateSummary(algo, plate);
            // Ausgabe: Nicht platzierte Jobs nach Abschluss der Platte
            if (!notPlaced.isEmpty()) {
                System.out.print("[INFO] Nach Abschluss von Platte " + (plateIdx + 1) + " konnten folgende Jobs NICHT platziert werden: ");
                for (int i = 0; i < notPlaced.size(); i++) {
                    System.out.print(notPlaced.get(i).id);
                    if (i < notPlaced.size() - 1) System.out.print(", ");
                }
                System.out.println();
            } else {
                System.out.println("[INFO] Nach Abschluss von Platte " + (plateIdx + 1) + " wurden alle Jobs platziert.");
            }
            // Nur die nicht platzierten Jobs für die nächste Platte verwenden
            notPlaced = new ArrayList<>(nextNotPlaced);
            System.out.print("Jobs für nächste Platte: ");
            for (int i = 0; i < notPlaced.size(); i++) {
                System.out.print(notPlaced.get(i).id);
                if (i < notPlaced.size() - 1) System.out.print(", ");
            }
            System.out.println();
        }
        // Visualisiere alle Platten und deren belegte Jobs
        List<List<Integer>> jobsPerPlate = new ArrayList<>();
        for (int plateIdx = 0; plateIdx < plateInfos.size(); plateIdx++) {
            List<Integer> jobsOnThisPlate = new ArrayList<>();
            MaxRectBF_MultiPath algo = multiPathAlgorithms.get(plateIdx);
            List<MaxRectBF_MultiPath.AlgorithmPath> paths = algo.getAllPaths();
            MaxRectBF_MultiPath.AlgorithmPath bestPath = null;
            int maxJobs = -1;
            for (MaxRectBF_MultiPath.AlgorithmPath path : paths) {
                if (path.plate.jobs.size() > maxJobs) {
                    maxJobs = path.plate.jobs.size();
                    bestPath = path;
                }
            }
            if (bestPath != null) {
                for (Job job : bestPath.plate.jobs) {
                    jobsOnThisPlate.add(job.id);
                }
            }
            jobsPerPlate.add(jobsOnThisPlate);
        }
        for (int i = 0; i < multiPathAlgorithms.size(); i++) {
            multiPathAlgorithms.get(i).debugEnabled = true;
            MaxRectBF_MultiPath algo = multiPathAlgorithms.get(i);
            List<MaxRectBF_MultiPath.AlgorithmPath> paths = algo.getAllPaths();
            MaxRectBF_MultiPath.AlgorithmPath bestPath = null;
            int maxJobs = -1;
            for (MaxRectBF_MultiPath.AlgorithmPath path : paths) {
                if (path.plate.jobs.size() > maxJobs) {
                    maxJobs = path.plate.jobs.size();
                    bestPath = path;
                }
            }
            if (bestPath != null) {
                //java.util.Set<Integer> jobsOnThisPlateSet = new java.util.HashSet<>(jobsPerPlate.get(i));
                // Konsolenausgabe: Nicht platzierte Jobs auf dieser Platte
                List<Integer> notPlacedOnPlate = new ArrayList<>();
                // Die nicht platzierten Jobs sind die, die zu Beginn des Plattendurchlaufs übergeben wurden, aber nicht platziert wurden
                List<Job> inputJobsForPlate = i == 0 ? jobs : new ArrayList<>(jobs);
                if (i > 0) {
                    for (int prev = 0; prev < i; prev++) {
                        for (Integer placedId : jobsPerPlate.get(prev)) {
                            inputJobsForPlate.removeIf(j -> j.id == placedId);
                        }
                    }
                }
                for (Job job : inputJobsForPlate) {
                    if (!jobsPerPlate.get(i).contains(job.id)) {
                        notPlacedOnPlate.add(job.id);
                    }
                }
                if (!notPlacedOnPlate.isEmpty()) {
                    System.out.print("[INFO] Platte " + (i + 1) + " konnte folgende Jobs NICHT platzieren: ");
                    for (int k = 0; k < notPlacedOnPlate.size(); k++) {
                        System.out.print(notPlacedOnPlate.get(k));
                        if (k < notPlacedOnPlate.size() - 1) System.out.print(", ");
                    }
                    System.out.println();
                } else {
                    System.out.println("[INFO] Platte " + (i + 1) + " hat alle Jobs platziert.");
                }
            } else {
                Plate emptyPlate = new Plate("Platte " + (i + 1), plates.get(i).width, plates.get(i).height);
                org.example.Visualizer.PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                    emptyPlate,
                    "5",
                    new ArrayList<>(),
                    "Endausgabe Platte " + (i + 1) + " (leer)",
                    null,
                    jobListInfo
                );
            }
        }
        // Konsolenausgabe: Zeige für jede Platte die platzierten Jobs NUR für den besten Pfad
        for (int i = 0; i < multiPathAlgorithms.size(); i++) {
            MaxRectBF_MultiPath algo = multiPathAlgorithms.get(i);
            List<MaxRectBF_MultiPath.AlgorithmPath> paths = algo.getAllPaths();
            MaxRectBF_MultiPath.AlgorithmPath bestPath = null;
            int maxJobs = -1;
            for (MaxRectBF_MultiPath.AlgorithmPath path : paths) {
                if (path.plate.jobs.size() > maxJobs) {
                    maxJobs = path.plate.jobs.size();
                    bestPath = path;
                }
            }
            if (bestPath != null) {
                System.out.print("Platte " + (i + 1) + " (bester Pfad) hat folgende Jobs: ");
                for (int j = 0; j < bestPath.plate.jobs.size(); j++) {
                    System.out.print(bestPath.plate.jobs.get(j).id);
                    if (j < bestPath.plate.jobs.size() - 1) System.out.print(", ");
                }
                System.out.println();
            } else {
                System.out.println("Platte " + (i + 1) + " (bester Pfad) hat keine Jobs.");
            }
        }
        // Konsolenausgabe: Zeige am Ende alle nicht platzierten Jobs
        if (!notPlaced.isEmpty()) {
            System.out.print("[INFO] Nach Abschluss aller Platten konnten folgende Jobs NICHT platziert werden: ");
            for (int i = 0; i < notPlaced.size(); i++) {
                System.out.print(notPlaced.get(i).id);
                if (i < notPlaced.size() - 1) System.out.print(", ");
            }
            System.out.println();
        } else {
            System.out.println("[INFO] Nach Abschluss aller Platten wurden alle Jobs platziert.");
        }
    }

    public List<Plate> getPlates() {
        return plates;
    }

    public List<MaxRectBF_MultiPath> getMultiPathAlgorithms() {
        return multiPathAlgorithms;
    }

    // Hilfsmethode für einmalige Plattenübersicht
    private void printPlateSummary(MaxRectBF_MultiPath algo, Plate plate) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("GESAMT-UEBERSICHT FÜR " + plate.name);
        System.out.println("=".repeat(80));
        int visualizeIdx = org.example.Main.Visualize_MultiPlateMultiPath_PathIndex;
        List<MaxRectBF_MultiPath.AlgorithmPath> paths = algo.getAllPaths();
        if (paths.size() > visualizeIdx) {
            MaxRectBF_MultiPath.AlgorithmPath path = paths.get(visualizeIdx);
            double coverage = org.example.Visualizer.PlateVisualizer.calculateCoverageRate(path.plate);
            System.out.printf("\nPfad %d (%s)\n", visualizeIdx + 1, path.pathDescription);
            System.out.printf("   Deckungsrate: %.2f%% | Platzierte Jobs: %d\n", coverage, path.plate.jobs.size());
            // ...optional: weitere Infos...
        }
        System.out.println();
    }
}