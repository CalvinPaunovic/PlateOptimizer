package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.Provider.PlateProvider;

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
    public void placeJobsOnPlates(List<PlateProvider.NamedPlate> plateInfos, List<Job> jobs, String jobListInfo) {
        // Diese Methode wird nur einmal aufgerufen
        // Sie übernimmt die komplette Verteilung der Jobs auf ALLE Platten.
        // Die Schleife im Inneren sorgt dafür, dass nach Platte 1 automatisch die restlichen Jobs auf Platte 2 usw. verteilt werden.
        // 1. notPlaced = alle Jobs
        // 2. Schleife: Platte 1 bekommt alle Jobs, platziert so viele wie möglich, die übrigen werden in stillNotPlaced gesammelt
        // 3. notPlaced = stillNotPlaced
        // 4. Schleife: Platte 2 bekommt alle noch nicht platzierten Jobs, platziert so viele wie möglich, usw.
        // 5. Das wiederholt sich für alle Platten in plateInfos

        this.plates.clear();
        this.multiPathAlgorithms.clear();

        // Zu Beginn: alle Jobs sind noch nicht platziert
        List<Job> notPlaced = new ArrayList<>(jobs);

        int plateIdx = 0;
        // Solange es noch nicht platzierte Jobs gibt und noch Platten verfügbar sind
        while (!notPlaced.isEmpty() && plateIdx < plateInfos.size()) {
            PlateProvider.NamedPlate info = plateInfos.get(plateIdx);
            Plate plate = new Plate("Platte " + (plateIdx + 1) + ": " + info.name, info.width, info.height);
            plates.add(plate);

            MaxRectBF_MultiPath algo = new MaxRectBF_MultiPath(plate);
            algo.debugEnabled = false;

            List<Job> stillNotPlaced = new ArrayList<>();
            for (int i = 0; i < notPlaced.size(); i++) {
                Job job = notPlaced.get(i);
                algo.placeJob(job);  // Hier wird der Job platziert
                if (!isJobPlacedInAnyPath(algo, job.id)) {
                    stillNotPlaced.add(job);
                }
            }
            multiPathAlgorithms.add(algo);

            // Nach Abschluss dieser Platte: EINMALIGE Debug-Ausgabe für alle Pfade
            if (org.example.Main.DEBUG_MultiPath && jobListInfo != null && jobListInfo.toLowerCase().contains("fläche")) {
                printPlateSummary(algo, plate);
            }

            // Konsolenausgabe: Zeige alle nicht platzierten Jobs nach dieser Platte
            if (!stillNotPlaced.isEmpty()) {
                System.out.print("Nicht platzierte Jobs nach Platte " + (plateIdx + 1) + ": ");
                for (int i = 0; i < stillNotPlaced.size(); i++) {
                    System.out.print(stillNotPlaced.get(i).id);
                    if (i < stillNotPlaced.size() - 1) System.out.print(", ");
                }
                System.out.println();
            } else {
                System.out.println("Alle Jobs konnten auf Platte " + (plateIdx + 1) + " platziert werden.");
            }

            notPlaced = stillNotPlaced; // stillNotPlaced wird innerhalb der Schleife aktualisiert, ggf. auf 0 gesetzt
            plateIdx++;
        }

        // Visualisiere alle Zwischenschritte für den gewünschten Pfadindex, erst Platte 1, dann Platte 2
        int debugPathIndex = org.example.Main.DEBUG_MultiPlateMultiPath_PathIndex;
        if (org.example.Main.DEBUG_MultiPlateMultiPath && jobListInfo != null && jobListInfo.toLowerCase().contains("fläche")) {
            for (int i = 0; i < multiPathAlgorithms.size(); i++) {
                // Aktiviere die Visualisierung explizit für jede Platte
                multiPathAlgorithms.get(i).debugEnabled = true;
                multiPathAlgorithms.get(i).visualizeAllStepsForPathIndex(debugPathIndex, jobListInfo, i + 1);
            }
        }
    }

    public List<Plate> getPlates() {
        return plates;
    }

    public List<MaxRectBF_MultiPath> getMultiPathAlgorithms() {
        return multiPathAlgorithms;
    }

    // Hilfsmethode prüfen, ob ein Job in irgendeinem Pfad platziert wurde
    private boolean isJobPlacedInAnyPath(MaxRectBF_MultiPath algo, int jobId) {
        for (MaxRectBF_MultiPath.AlgorithmPath path : algo.getAllPaths()) {
            for (Job job : path.plate.jobs) {
                if (job.id == jobId) return true;
            }
        }
        return false;
    }

    // Hilfsmethode für einmalige Plattenübersicht
    private void printPlateSummary(MaxRectBF_MultiPath algo, Plate plate) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("GESAMT-UEBERSICHT FÜR " + plate.name);
        System.out.println("=".repeat(80));
        int pfadNr = 1;
        for (MaxRectBF_MultiPath.AlgorithmPath path : algo.getAllPaths()) {
            double coverage = org.example.Visualizer.PlateVisualizer.calculateCoverageRate(path.plate);
            System.out.printf("\nPfad %d (%s)\n", pfadNr++, path.pathDescription);
            System.out.printf("   Deckungsrate: %.2f%% | Platzierte Jobs: %d\n", coverage, path.plate.jobs.size());
            // ...optional: weitere Infos...
        }
        System.out.println();

        // Abschlussausgabe für den Debug-Pfadindex (wie oben)
        int debugPathIndex = org.example.Main.DEBUG_MultiPlateMultiPath_PathIndex;
        List<MaxRectBF_MultiPath.AlgorithmPath> paths = algo.getAllPaths();
        MaxRectBF_MultiPath.AlgorithmPath debugPath = null;
        if (paths.size() > debugPathIndex) {
            debugPath = paths.get(debugPathIndex);
        }
        if (debugPath != null && !debugPath.failedJobs.isEmpty()) {
            System.out.println("\n" + "!!!".repeat(20));
            System.out.println(">>> ABSCHLUSS PLATTE " + plate.name + " / Pfad " + (debugPathIndex) + " <<<");
            System.out.print("Nicht platzierte Jobs: ");
            for (int j = 0; j < debugPath.failedJobs.size(); j++) {
                System.out.print(debugPath.failedJobs.get(j));
                if (j < debugPath.failedJobs.size() - 1) System.out.print(", ");
            }
            System.out.println();
            System.out.println("!!!".repeat(20) + "\n");
        } else if (debugPath != null) {
            System.out.println("\n" + ">>> ABSCHLUSS PLATTE " + plate.name + " / Pfad " + (debugPathIndex) + ": Alle Jobs konnten platziert werden. <<<\n");
        }
    }
}