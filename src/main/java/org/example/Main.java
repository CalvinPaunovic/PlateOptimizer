package org.example;

import java.util.*;

public class Main {
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_MaxRectBF = false;
    public static final boolean DEBUG_MaxRectBF_Dynamic = false;
    public static final boolean DEBUG_MultiPath = true;

    public static final boolean rotateJobs = false;  // Bislang nur für MaxRectBestFit-Algorithmus.
    public static final boolean sortJobs = true;  // Bislang nur für MaxRectBestFit-Algorithmus.

    public static void sortJobsBySizeDescending(List<Job> jobs) {
        jobs.sort(new Comparator<Job>() {
            @Override
            public int compare(Job j1, Job j2) {
                int area1 = j1.width * j1.height;
                int area2 = j2.width * j2.height;
                return Integer.compare(area2, area1);
            }
        });
        if (Main.DEBUG) System.out.println("Jobs sortiert nach Fläche absteigend:");
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            int area = job.width * job.height;
            if (Main.DEBUG)  System.out.printf("%d: Job ID=%d, Größe=%dx%d, Fläche=%d\n", i + 1, job.id, job.width, job.height, area);
        }
    }

    public static void sortJobsByLargestEdgeDescending(List<Job> jobs) {
        jobs.sort(new Comparator<Job>() {
            @Override
            public int compare(Job j1, Job j2) {
                int maxEdge1 = Math.max(j1.width, j1.height);
                int maxEdge2 = Math.max(j2.width, j2.height);
                return Integer.compare(maxEdge2, maxEdge1);
            }
        });
        if (Main.DEBUG) System.out.println("Jobs sortiert nach größter Kante absteigend:");
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            int maxEdge = Math.max(job.width, job.height);
            if (Main.DEBUG) System.out.printf("%d: Job ID=%d, Größe=%dx%d, größte Kante=%d\n", i + 1, job.id, job.width, job.height, maxEdge);
        }
    }

    public static void main(String[] args) {
        List<Job> originalJobs = Arrays.asList(
                new Job(0, 402, 480),
                new Job(1, 305, 222),
                new Job(2, 220, 573),
                new Job(3, 205, 153),
                new Job(4, 243, 188),
                new Job(5, 243,188),
                new Job(6,205,153),

                new Job(7, 305, 17)
        );

        String mode = getUserAlgorithmChoice();

        switch (mode) {
            case "2":
                runMaxRectBF(originalJobs);
                break;
            case "3":
                runMaxRectBF_Dynamic(originalJobs);
                break;
            case "4":
                runMaxRectBF_MultiPath(originalJobs);
                break;
            case "b":
            case "B":
                runBenchmark(originalJobs);
                break;
            default:
                runFirstFitAlgorithm(originalJobs);
                break;
        }
    }

    private static void runMaxRectBF(List<Job> originalJobs) {
        // Test FullHeight und FullWidth mit beiden Sortiermethoden
        BenchmarkVisualizer.BenchmarkResult fullWidthByArea = benchmarkMaxRectBF(
            originalJobs, false, Main::sortJobsBySizeDescending, "MaxRectBF FullWidth (nach Fläche)");
        BenchmarkVisualizer.BenchmarkResult fullWidthByEdge = benchmarkMaxRectBF(
            originalJobs, false, Main::sortJobsByLargestEdgeDescending, "MaxRectBF FullWidth (nach Kante)");
        BenchmarkVisualizer.BenchmarkResult fullHeightByArea = benchmarkMaxRectBF(
            originalJobs, true, Main::sortJobsBySizeDescending, "MaxRectBF FullHeight (nach Fläche)");
        BenchmarkVisualizer.BenchmarkResult fullHeightByEdge = benchmarkMaxRectBF(
            originalJobs, true, Main::sortJobsByLargestEdgeDescending, "MaxRectBF FullHeight (nach Kante)");
        PlateVisualizer.showPlate(fullWidthByArea.plate, "2", fullWidthByArea.algorithm);
        PlateVisualizer.showPlate(fullWidthByEdge.plate, "2", fullWidthByEdge.algorithm);
        PlateVisualizer.showPlate(fullHeightByArea.plate, "2", fullHeightByArea.algorithm);
        PlateVisualizer.showPlate(fullHeightByEdge.plate, "2", fullHeightByEdge.algorithm);
    }

    private static void runMaxRectBF_Dynamic(List<Job> originalJobs) {
        // Test mit beiden Sortiermethoden
        BenchmarkVisualizer.BenchmarkResult resultBySize = benchmarkMaxRectBF_Dynamic(
            originalJobs, Main::sortJobsBySizeDescending, "MaxRectBF Dynamic (nach Fläche)");
        BenchmarkVisualizer.BenchmarkResult resultByEdge = benchmarkMaxRectBF_Dynamic(
            originalJobs, Main::sortJobsByLargestEdgeDescending, "MaxRectBF Dynamic (nach Kante)");
        PlateVisualizer.showPlate(resultBySize.plate, "3", resultBySize.algorithm);
        PlateVisualizer.showPlate(resultByEdge.plate, "3", resultByEdge.algorithm);
    }

    private static void runMaxRectBF_MultiPath(List<Job> originalJobs) {
        System.out.println("\n=== MaxRectBF_MultiPath: Multi-Path Algorithmus ===\n");
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) sortJobsBySizeDescending(jobs);
        
        Plate plate = new Plate("MaxRectBF Multi-Path", 963, 650);
        MaxRectBF_MultiPath algorithm = new MaxRectBF_MultiPath(plate);
        
        // Sammle alle Zwischenschritte während der Platzierung
        List<List<MaxRectBF_MultiPath.AlgorithmPath>> stepsByJob = new ArrayList<>();
        
        // Speichere initialen Zustand (vor erstem Job)
        List<MaxRectBF_MultiPath.AlgorithmPath> initialState = new ArrayList<>();
        for (MaxRectBF_MultiPath.AlgorithmPath path : algorithm.getAllPaths()) {
            if (path.isActive) {
                // Erstelle Deep Copy des initialen Zustands
                MaxRectBF_MultiPath.AlgorithmPath snapshot = new MaxRectBF_MultiPath.AlgorithmPath(path.plate, path.pathDescription + " (Start)");
                snapshot.freeRects = new ArrayList<>(path.freeRects);
                initialState.add(snapshot);
            }
        }
        stepsByJob.add(initialState);
        
        for (Job job : jobs) {
            boolean placed = algorithm.placeJob(job);
            if (!placed) {
                System.out.println("Job " + job.id + " konnte in keinem Pfad platziert werden.");
            }
            
            // Erstelle Deep Copy des aktuellen Zustands aller Pfade nach diesem Job
            List<MaxRectBF_MultiPath.AlgorithmPath> currentStepPaths = new ArrayList<>();
            for (MaxRectBF_MultiPath.AlgorithmPath path : algorithm.getAllPaths()) {
                if (path.isActive) {
                    // Deep Copy der Plate mit allen Jobs
                    MaxRectBF_MultiPath.AlgorithmPath snapshot = new MaxRectBF_MultiPath.AlgorithmPath(path.plate, path.pathDescription + " (nach Job " + job.id + ")");
                    
                    // Kopiere alle Jobs in die Snapshot-Plate
                    snapshot.plate.jobs.clear(); // Lösche die aus dem Constructor
                    for (Job originalJob : path.plate.jobs) {
                        Job copiedJob = new Job(originalJob.id, originalJob.width, originalJob.height);
                        copiedJob.x = originalJob.x;
                        copiedJob.y = originalJob.y;
                        copiedJob.rotated = originalJob.rotated;
                        copiedJob.placedOn = snapshot.plate;
                        copiedJob.placementOrder = originalJob.placementOrder;
                        copiedJob.splittingMethod = originalJob.splittingMethod;
                        snapshot.plate.jobs.add(copiedJob);
                    }
                    
                    // Kopiere freie Rechtecke
                    snapshot.freeRects = new ArrayList<>();
                    for (MaxRectBF_MultiPath.FreeRectangle rect : path.freeRects) {
                        snapshot.freeRects.add(new MaxRectBF_MultiPath.FreeRectangle(rect.x, rect.y, rect.width, rect.height));
                    }
                    
                    currentStepPaths.add(snapshot);
                }
            }
            stepsByJob.add(currentStepPaths);
        }
        
        // VISUALISIERUNG: Zeige nur die Endergebnisse aller Pfade
        System.out.println("\n\n=== ENDERGEBNISSE ALLER PFADE ===");
        
        // Sammle alle Pfade mit finalen Statistiken
        List<MaxRectBF_MultiPath.AlgorithmPath> finalPaths = new ArrayList<>();
        
        for (MaxRectBF_MultiPath.AlgorithmPath path : algorithm.getAllPaths()) {
            if (path.isActive) {
                // Berechne finale Statistiken erst hier
                double coverage = PlateVisualizer.calculateCoverageRate(path.plate);
                System.out.printf("\n🎯 %s - ENDERGEBNIS:\n", path.pathDescription);
                System.out.printf("Jobs platziert: %d, Deckungsrate: %.2f%%%n", 
                    path.plate.jobs.size(), coverage);
                
                finalPaths.add(path);
            }
        }
        
        // Zeige Visualisierungen nacheinander mit Wartezeit
        for (int i = 0; i < finalPaths.size(); i++) {
            MaxRectBF_MultiPath.AlgorithmPath path = finalPaths.get(i);
            
            // Erstelle erweiterten Titel mit Job-Informationen
            String extendedTitle = path.plate.name;
            
            if (path.pathDescription.contains("Pfad 1 (FullWidth)")) {
                // Für Pfad 1: Zeige welche Jobs nicht gepasst haben
                List<Integer> missingJobs = new ArrayList<>();
                for (Job originalJob : originalJobs) {
                    boolean found = false;
                    for (Job placedJob : path.plate.jobs) {
                        if (placedJob.id == originalJob.id) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        missingJobs.add(originalJob.id);
                    }
                }
                
                if (!missingJobs.isEmpty()) {
                    extendedTitle += " | Jobs " + missingJobs + " passten nicht in FullWidth → siehe andere Pfade";
                }
                
            } else if (path.pathDescription.contains("FullHeight ab Job")) {
                // Extrahiere die Job-Nummer aus der Beschreibung
                String jobNumberStr = path.pathDescription.substring(
                    path.pathDescription.indexOf("ab Job ") + 7);
                // Extrahiere nur die Zahl (falls weitere Zeichen folgen)
                int spaceIndex = jobNumberStr.indexOf(" ");
                if (spaceIndex > 0) {
                    jobNumberStr = jobNumberStr.substring(0, spaceIndex);
                }
                int bracketIndex = jobNumberStr.indexOf(")");
                if (bracketIndex > 0) {
                    jobNumberStr = jobNumberStr.substring(0, bracketIndex);
                }
                
                extendedTitle += " | Job " + jobNumberStr + " passte nicht in FullWidth → Wechsel zu FullHeight";
                
                // Prüfe welche weiteren Jobs in diesem Pfad nicht platziert wurden
                List<Integer> missingJobs = new ArrayList<>();
                for (Job originalJob : originalJobs) {
                    boolean found = false;
                    for (Job placedJob : path.plate.jobs) {
                        if (placedJob.id == originalJob.id) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        missingJobs.add(originalJob.id);
                    }
                }
                
                if (!missingJobs.isEmpty()) {
                    extendedTitle += " | Jobs " + missingJobs + " passten auch in FullHeight nicht";
                }
            } else if (path.pathDescription.contains("FullWidth ab Job")) {
                // Für FullWidth-Pfade (die von FullHeight wechseln)
                String jobNumberStr = path.pathDescription.substring(
                    path.pathDescription.indexOf("ab Job ") + 7);
                int spaceIndex = jobNumberStr.indexOf(" ");
                if (spaceIndex > 0) {
                    jobNumberStr = jobNumberStr.substring(0, spaceIndex);
                }
                int bracketIndex = jobNumberStr.indexOf(")");
                if (bracketIndex > 0) {
                    jobNumberStr = jobNumberStr.substring(0, bracketIndex);
                }
                
                extendedTitle += " | Job " + jobNumberStr + " passte nicht in FullHeight → Wechsel zu FullWidth";
                
                // Prüfe welche weiteren Jobs in diesem Pfad nicht platziert wurden
                List<Integer> missingJobs = new ArrayList<>();
                for (Job originalJob : originalJobs) {
                    boolean found = false;
                    for (Job placedJob : path.plate.jobs) {
                        if (placedJob.id == originalJob.id) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        missingJobs.add(originalJob.id);
                    }
                }
                
                if (!missingJobs.isEmpty()) {
                    extendedTitle += " | Jobs " + missingJobs + " passten auch in FullWidth nicht";
                }
            }
            
            System.out.printf("Zeige Visualisierung %d/%d: %s\n", i + 1, finalPaths.size(), path.pathDescription);
            
            // Erstelle Algorithmus-Info für MultiPath
            String algorithmInfo = String.format("Algorithmus: MultiPath | Insgesamt %d Pfade erstellt", finalPaths.size());
            
            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(path.plate, "4", path.freeRects, extendedTitle, algorithmInfo);
            
            // Warte zwischen den Fenstern (außer beim letzten)
            if (i < finalPaths.size() - 1) {
                try {
                    Thread.sleep(2000); // 2 Sekunden zwischen den Fenstern
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        /*
        // ZWISCHENSCHRITTE-VISUALISIERUNG (auskommentiert)
        System.out.println("\n\n=== PFADWEISE VISUALISIERUNG ALLER ZWISCHENSCHRITTE ===");
        
        // Erst alle Schritte von Pfad 1
        System.out.println("\n🎯 PFAD 1 (FullWidth) - ALLE ZWISCHENSCHRITTE:");
        for (int jobIndex = 0; jobIndex < stepsByJob.size(); jobIndex++) {
            List<MaxRectBF_MultiPath.AlgorithmPath> pathsAtStep = stepsByJob.get(jobIndex);
            
            // Finde Pfad 1 in diesem Schritt
            for (MaxRectBF_MultiPath.AlgorithmPath path : pathsAtStep) {
                if (path.pathDescription.contains("Pfad 1")) {
                    if (jobIndex == 0) {
                        System.out.println("\n--- Initialer Zustand ---");
                    } else {
                        System.out.println("\n--- Nach Job " + jobs.get(jobIndex - 1).id + " ---");
                    }
                    System.out.printf("Jobs platziert: %d, Deckungsrate: %.2f%%%n", 
                        path.plate.jobs.size(), PlateVisualizer.calculateCoverageRate(path.plate));
                    
                    PlateVisualizer.showPlateWithSpecificFreeRects(path.plate, "4", path.freeRects);
                    
                    try {
                        Thread.sleep(1500); // 1.5 Sekunden zwischen den Schritten
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
            }
        }
        
        // Dann alle Schritte von Pfad 2 (falls vorhanden)
        boolean pfad2HeaderShown = false;
        for (int jobIndex = 0; jobIndex < stepsByJob.size(); jobIndex++) {
            List<MaxRectBF_MultiPath.AlgorithmPath> pathsAtStep = stepsByJob.get(jobIndex);
            
            // Suche nach Pfad 2
            for (MaxRectBF_MultiPath.AlgorithmPath path : pathsAtStep) {
                if (path.pathDescription.contains("Pfad 2")) {
                    // Zeige Header nur beim ersten Mal
                    if (!pfad2HeaderShown) {
                        System.out.println("\n🎯 PFAD 2 (FullHeight) - ALLE ZWISCHENSCHRITTE:");
                        pfad2HeaderShown = true;
                    }
                    
                    if (jobIndex == 0) {
                        System.out.println("\n--- Initialer Zustand ---");
                    } else {
                        System.out.println("\n--- Nach Job " + jobs.get(jobIndex - 1).id + " ---");
                    }
                    System.out.printf("Jobs platziert: %d, Deckungsrate: %.2f%%%n", 
                        path.plate.jobs.size(), PlateVisualizer.calculateCoverageRate(path.plate));
                    
                    PlateVisualizer.showPlateWithSpecificFreeRects(path.plate, "4", path.freeRects);
                    
                    try {
                        Thread.sleep(1500); // 1.5 Sekunden zwischen den Schritten
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
            }
        }
        */
        
        // Zeige beste Lösung
        Plate bestPlate = algorithm.getBestPlate();
        double bestCoverage = PlateVisualizer.calculateCoverageRate(bestPlate);
        System.out.printf("\nBESTE LÖSUNG: %.2f%% Deckungsrate mit %d Jobs\n", 
            bestCoverage, bestPlate.jobs.size());
    }

    private static void runFirstFitAlgorithm(List<Job> originalJobs) {
        BenchmarkVisualizer.BenchmarkResult result = benchmarkFirstFit(originalJobs);
        PlateVisualizer.showPlate(result.plate, "1", null);
    }

    private static void runBenchmark(List<Job> originalJobs) {
        List<BenchmarkVisualizer.BenchmarkResult> results = new ArrayList<>();
        // Test First Fit Algorithm
        results.add(benchmarkFirstFit(originalJobs));
        // Test MaxRectBF FullWidth mit Sortierung nach Fläche
        results.add(benchmarkMaxRectBF(originalJobs, false, Main::sortJobsBySizeDescending, "MaxRectBF FullWidth (nach Fläche)"));
        // Test MaxRectBF FullWidth mit Sortierung nach größter Kante
        results.add(benchmarkMaxRectBF(originalJobs, false, Main::sortJobsByLargestEdgeDescending, "MaxRectBF FullWidth (nach Kante)"));
        // Test MaxRectBF FullHeight mit Sortierung nach Fläche
        results.add(benchmarkMaxRectBF(originalJobs, true, Main::sortJobsBySizeDescending, "MaxRectBF FullHeight (nach Fläche)"));
        // Test MaxRectBF FullHeight mit Sortierung nach größter Kante
        results.add(benchmarkMaxRectBF(originalJobs, true, Main::sortJobsByLargestEdgeDescending, "MaxRectBF FullHeight (nach Kante)"));
        // Test MaxRectBF_Dynamic mit Sortierung nach Fläche
        results.add(benchmarkMaxRectBF_Dynamic(originalJobs, Main::sortJobsBySizeDescending, "MaxRectBF Dynamic (nach Fläche)"));
        // Test MaxRectBF_Dynamic mit Sortierung nach größter Kante
        results.add(benchmarkMaxRectBF_Dynamic(originalJobs, Main::sortJobsByLargestEdgeDescending, "MaxRectBF Dynamic (nach Kante)"));
        // Ergebnisse sortieren nach Deckungsrate (absteigend)
        results.sort((r1, r2) -> Double.compare(r2.coverageRate, r1.coverageRate));
        // Zeige GUI Benchmark Visualizer
        BenchmarkVisualizer.showBenchmarkResults(results);
    }
    
    private static BenchmarkVisualizer.BenchmarkResult benchmarkFirstFit(List<Job> originalJobs) {
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) sortJobsBySizeDescending(jobs);
        Plate plate = new Plate("First Fit", 963, 650);
        placeJobsWithAlgorithm(jobs, plate::placeJobFFShelf);
        int placedJobs = countPlacedJobs(jobs);
        double coverageRate = PlateVisualizer.calculateCoverageRate(plate);
        return new BenchmarkVisualizer.BenchmarkResult("First Fit", plate, null, placedJobs, coverageRate, originalJobs.size());
    }
    
    private static BenchmarkVisualizer.BenchmarkResult benchmarkMaxRectBF(List<Job> originalJobs, boolean useFullHeight, 
        java.util.function.Consumer<List<Job>> sortingMethod, String algorithmName) {
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) sortingMethod.accept(jobs);
        Plate plate = new Plate(algorithmName, 963, 650);
        MaxRectBF algorithm = new MaxRectBF(plate, useFullHeight);
        placeJobsWithAlgorithm(jobs, algorithm::placeJob);
        int placedJobs = countPlacedJobs(jobs);
        double coverageRate = PlateVisualizer.calculateCoverageRate(plate);
        return new BenchmarkVisualizer.BenchmarkResult(algorithmName, plate, algorithm, placedJobs, coverageRate, originalJobs.size());
    }
    
    private static BenchmarkVisualizer.BenchmarkResult benchmarkMaxRectBF_Dynamic(List<Job> originalJobs, 
        java.util.function.Consumer<List<Job>> sortingMethod, String algorithmName) {
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) sortingMethod.accept(jobs);
        Plate plate = new Plate(algorithmName, 963, 650);
        MaxRectBF_Dynamic algorithm = new MaxRectBF_Dynamic(plate);
        placeJobsWithAlgorithm(jobs, algorithm::placeJob);
        int placedJobs = countPlacedJobs(jobs);
        double coverageRate = PlateVisualizer.calculateCoverageRate(plate);
        return new BenchmarkVisualizer.BenchmarkResult(algorithmName, plate, algorithm, placedJobs, coverageRate, originalJobs.size());
    }
    
    private static int countPlacedJobs(List<Job> jobs) {
        int count = 0;
        for (Job job : jobs) {
            if (job.placedOn != null) {
                count++;
            }
        }
        return count;
    }
    
    private static void placeJobsWithAlgorithm(List<Job> jobs, java.util.function.Function<Job, Boolean> placementFunction) {
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            boolean placed = placementFunction.apply(job);
            if (!placed) {
                if (Main.DEBUG) System.out.println("Job " + job.id + " konnte nicht platziert werden.");
            }
        }
    }

    private static List<Job> createJobCopies(List<Job> originalJobs) {
        List<Job> copies = new ArrayList<>();
        for (int i = 0; i < originalJobs.size(); i++) {
            Job original = originalJobs.get(i);
            copies.add(new Job(original.id, original.width, original.height));
        }
        return copies;
    }
    
    private static String getUserAlgorithmChoice() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Algorithmus wählen (1 = First Fit, 2 = MaxRectsBF, 3 = MaxRectsBF Dynamic, 4 = MaxRectsBF Multi-Path, b = Benchmark): ");
            return scanner.nextLine().trim();
        }
    }
}