package org.example.HelperClasses;

import org.example.Main;
import org.example.Algorithms.MaxRectBF_MultiPath;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.Visualizer.BenchmarkVisualizer;
import org.example.Visualizer.PlateVisualizer;

import java.util.*;
import java.util.function.Consumer;

public class MaxRectBF_MultiPath_Controller {

    /**
     * Führt den MaxRectBF_MultiPath-Algorithmus aus und gibt für alle aktiven Pfade die Ergebnisse zurück.
     * Gibt für jeden Job, der nicht platziert werden konnte, eine Konsolenmeldung mit Pfadangabe aus.
     */
    public static List<BenchmarkVisualizer.BenchmarkResult> benchmarkMaxRectBF_MultiPath_AllPaths(
        List<Job> originalJobs,
        Consumer<List<Job>> sortingMethod,
        String algorithmBaseName,
        Plate plateInfo // <-- Typ geändert
        ) {
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (Main.sortJobs) sortingMethod.accept(jobs);
        Plate plate = new Plate(algorithmBaseName, plateInfo.width, plateInfo.height, plateInfo.plateId);
        MaxRectBF_MultiPath algorithm = new MaxRectBF_MultiPath(plate);
        // Füge die Platzierungsschleife hinzu!
        for (Job job : jobs) {
            boolean placed = algorithm.placeJob(job);
            if (!placed) {
                // Schreibe immer in die Konsole, in welchen Pfaden der Job nicht platziert werden konnte
                List<MaxRectBF_MultiPath.AlgorithmPath> paths = algorithm.getAllPaths();
                StringBuilder pfadInfo = new StringBuilder();
                for (MaxRectBF_MultiPath.AlgorithmPath path : paths) {
                    if (path.isActive) {
                        pfadInfo.append(path.pathDescription != null ? path.pathDescription : "Pfad ?").append(", ");
                    }
                }
                System.out.printf("Job %d konnte NICHT platziert werden (versuchte Pfade: %s)\n", job.id,
                        pfadInfo.length() > 0 ? pfadInfo.substring(0, pfadInfo.length() - 2) : "-");
            }
        }
        List<BenchmarkVisualizer.BenchmarkResult> pathResults = new ArrayList<>();
        List<MaxRectBF_MultiPath.AlgorithmPath> allPaths = algorithm.getAllPaths();
        for (MaxRectBF_MultiPath.AlgorithmPath path : allPaths) {
            if (path.isActive) {
                int placedJobs = path.plate.jobs.size();
                double coverageRate = PlateVisualizer.calculateCoverageRate(path.plate);
                String pathNumber = "1";
                if (path.pathDescription != null && path.pathDescription.contains("Pfad ")) {
                    int start = path.pathDescription.indexOf("Pfad ") + 5;
                    int end = path.pathDescription.indexOf(" ", start);
                    if (end == -1) end = path.pathDescription.length();
                    pathNumber = path.pathDescription.substring(start, end);
                }
                String strategyCode = algorithm.getStrategyCodeForPath(path);
                String pathName = algorithmBaseName + " - Pfad " + pathNumber;
                if (strategyCode != null && !strategyCode.isEmpty()) pathName += " (" + strategyCode + ")";

                pathResults.add(new BenchmarkVisualizer.BenchmarkResult(
                    pathName,
                    path.plate,
                    algorithm,
                    placedJobs,
                    coverageRate,
                    originalJobs.size(),
                    path.freeRects
                ));
            }
        }
        return pathResults;
    }

    /**
     * Zählt die Anzahl der platzierten Jobs in einer Liste.
     */
    public static int countPlacedJobs(List<Job> jobs) {
        int count = 0;
        for (Job job : jobs) {
            if (job.placedOn != null) count++;
        }
        return count;
    }

    /**
     * Führt den MultiPath-Algorithmus mit Visualisierung nach jedem Schritt aus.
     * Gibt für jeden nicht platzierten Job eine Konsolenmeldung mit Pfadangabe aus.
     */
    public static void run_MaxRectBF_MultiPath(List<Job> originalJobs, Plate plateInfo, boolean sortJobs) {
        System.out.println("\n=== MaxRectBF_MultiPath: Multi-Path Algorithmus ===\n");
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsBySizeDescending(jobs);
        Plate plate = new Plate("MaxRectBF Multi-Path", plateInfo.width, plateInfo.height, plateInfo.plateId);
        MaxRectBF_MultiPath algorithm = new MaxRectBF_MultiPath(plate);

        // === HAUPT-PLATZIERUNGS-SCHLEIFE ===
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            boolean placed = algorithm.placeJob(job);
            if (!placed) {
                List<MaxRectBF_MultiPath.AlgorithmPath> paths = algorithm.getAllPaths();
                StringBuilder pfadInfo = new StringBuilder();
                for (MaxRectBF_MultiPath.AlgorithmPath path : paths) {
                    if (path.isActive) {
                        pfadInfo.append(path.pathDescription != null ? path.pathDescription : "Pfad ?").append(", ");
                    }
                }
                System.out.printf("Job %d konnte NICHT platziert werden (versuchte Pfade: %s)\n", job.id,
                        pfadInfo.length() > 0 ? pfadInfo.substring(0, pfadInfo.length() - 2) : "-");
            }
            showPfad1Step(algorithm, i, jobs.size(), job.id);
        }

        // === ENDERGEBNIS-AUSWERTUNG ===
        System.out.println("\n\n=== ENDERGEBNISSE ALLER PFADE ===");
        List<MaxRectBF_MultiPath.AlgorithmPath> finalPaths = new ArrayList<>();
        for (MaxRectBF_MultiPath.AlgorithmPath path : algorithm.getAllPaths()) {
            if (path.isActive) {
                double coverage = PlateVisualizer.calculateCoverageRate(path.plate);
                System.out.printf("\n%s - ENDERGEBNIS:\n", path.pathDescription);
                System.out.printf("Jobs platziert: %d, Deckungsrate: %.2f%%%n",
                        path.plate.jobs.size(), coverage);
                finalPaths.add(path);
            }
        }

        // Zeige Visualisierungen nacheinander mit Wartezeit
        for (int i = 0; i < finalPaths.size(); i++) {
            visualizePathResult(finalPaths.get(i), algorithm, i + 1, finalPaths.size());
        }

        // Zeige beste Lösung
        Plate bestPlate = algorithm.getBestPlate();
        double bestCoverage = PlateVisualizer.calculateCoverageRate(bestPlate);
        System.out.printf("\nBESTE LÖSUNG: %.2f%% Deckungsrate mit %d Jobs\n",
                bestCoverage, bestPlate.jobs.size());
    }

    // Zeigt den aktuellen Stand von Pfad 1 nach jedem Job
    private static void showPfad1Step(MaxRectBF_MultiPath algorithm, int stepIdx, int totalSteps, int jobId) {
        List<MaxRectBF_MultiPath.AlgorithmPath> allPathsCurrent = algorithm.getAllPaths();
        MaxRectBF_MultiPath.AlgorithmPath path1 = null;
        for (MaxRectBF_MultiPath.AlgorithmPath path : allPathsCurrent) {
            if (path.isActive && path.pathDescription != null && path.pathDescription.contains("Pfad 1")) {
                path1 = path;
                break;
            }
        }
        if (path1 != null) {
            String stepTitle = "Pfad 1 - Nach Job " + jobId + " (" + path1.plate.jobs.size() + " Jobs)";
            String algorithmInfo = String.format("Zwischenschritt %d/%d | Jobs platziert: %d",
                    stepIdx + 1, totalSteps, path1.plate.jobs.size());

            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                    path1.plate,
                    "4",
                    path1.freeRects,
                    stepTitle,
                    algorithmInfo,
                    "" // Add an empty string or a suitable value for the sixth argument
            );

            // Kurze Pause zwischen den Schritten
            try {
                Thread.sleep(1500); // 1.5 Sekunden zwischen den Zwischenschritten
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Visualisiert das Ergebnis eines bestimmten Pfads nach Abschluss der Platzierung.
     */
    private static void visualizePathResult(MaxRectBF_MultiPath.AlgorithmPath path, MaxRectBF_MultiPath algorithm, int index, int total) {
        String pathNumber = "1";
        if (path.pathDescription != null && path.pathDescription.contains("Pfad ")) {
            int start = path.pathDescription.indexOf("Pfad ") + 5;
            int end = path.pathDescription.indexOf(" ", start);
            if (end == -1) end = path.pathDescription.length();
            pathNumber = path.pathDescription.substring(start, end);
        }
        String strategyCode = algorithm.getStrategyCodeForPath(path);
        String simplifiedTitle = "Pfad " + pathNumber;
        if (strategyCode != null && !strategyCode.isEmpty()) {
            simplifiedTitle += " (" + strategyCode + ")";
        }
        System.out.printf("Zeige Visualisierung %d/%d: %s\n", index, total, path.pathDescription);
        String algorithmInfo = String.format("Algorithmus: MultiPath | Insgesamt %d Pfade erstellt", total);
        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(path.plate, "4", path.freeRects, simplifiedTitle, algorithmInfo, "");

        if (index < total) {
            try {
                Thread.sleep(2000); // 2 Sekunden zwischen den Fenstern
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}