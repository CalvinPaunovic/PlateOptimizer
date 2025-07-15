package org.example;

import java.util.*;

public class Main {
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_MaxRectBF = false;
    public static final boolean DEBUG_MaxRectBF_Dynamic = false;
    public static final boolean DEBUG_MultiPath = true;

    public static final boolean rotateJobs = true;  // Bislang nur für MaxRectBestFit-Algorithmus.
    public static final boolean sortJobs = true;  // Bislang nur für MaxRectBestFit-Algorithmus.
    
    // Schnittbreite in mm wird zu jeder Job-Dimension hinzugefügt
    public static final int KERF_WIDTH = 3;  // 3mm Schnittbreite pro Seite

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
                new Job(6,205,153)

                //new Job(7, 305, 17)
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
            case "0":
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
        
        // === JOB-VORBEREITUNG ===
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) sortJobsBySizeDescending(jobs);
        
        // === ALGORITHMUS-INITIALISIERUNG ===
        Plate plate = new Plate("MaxRectBF Multi-Path", 963, 650);
        MaxRectBF_MultiPath algorithm = new MaxRectBF_MultiPath(plate);
         
        // === HAUPT-PLATZIERUNGS-SCHLEIFE ===
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            boolean placed = algorithm.placeJob(job);
            if (!placed) {
                System.out.println("Job " + job.id + " konnte in keinem Pfad platziert werden.");
            }
            
            // Zeige nur Zwischenschritte von Pfad 1 (FullWidth)
            List<MaxRectBF_MultiPath.AlgorithmPath> allPathsCurrent = algorithm.getAllPaths();
            MaxRectBF_MultiPath.AlgorithmPath path1 = null;
            
            // Finde Pfad 1 (FullWidth)
            for (int j = 0; j < allPathsCurrent.size(); j++) {
                MaxRectBF_MultiPath.AlgorithmPath path = allPathsCurrent.get(j);
                if (path.isActive && path.pathDescription.contains("Pfad 1")) {
                    path1 = path;
                    break;
                }
            }
            
            // Visualisiere Zwischenschritt nur für Pfad 1
            if (path1 != null) {
                String stepTitle = "Pfad 1 - Nach Job " + job.id + " (" + path1.plate.jobs.size() + " Jobs)";
                String algorithmInfo = String.format("Zwischenschritt %d/%d | Jobs platziert: %d", 
                    i + 1, jobs.size(), path1.plate.jobs.size());
                
                PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                    path1.plate, 
                    "4", 
                    path1.freeRects, 
                    stepTitle, 
                    algorithmInfo
                );
                
                // Kurze Pause zwischen den Schritten
                try {
                    Thread.sleep(1500); // 1.5 Sekunden zwischen den Zwischenschritten
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // === ENDERGEBNIS-AUSWERTUNG ===
        System.out.println("\n\n=== ENDERGEBNISSE ALLER PFADE ===");
        
        // Sammle alle Pfade mit finalen Statistiken
        List<MaxRectBF_MultiPath.AlgorithmPath> finalPaths = new ArrayList<>();
        List<MaxRectBF_MultiPath.AlgorithmPath> allPaths = algorithm.getAllPaths();
        for (int i = 0; i < allPaths.size(); i++) {
            MaxRectBF_MultiPath.AlgorithmPath path = allPaths.get(i);
            if (path.isActive) {
            // Berechne finale Statistiken erst hier
            double coverage = PlateVisualizer.calculateCoverageRate(path.plate);
            System.out.printf("\n%s - ENDERGEBNIS:\n", path.pathDescription);
            System.out.printf("Jobs platziert: %d, Deckungsrate: %.2f%%%n", 
                path.plate.jobs.size(), coverage);
            finalPaths.add(path);
            }
        }
        
        // Zeige Visualisierungen nacheinander mit Wartezeit
        for (int i = 0; i < finalPaths.size(); i++) {
            MaxRectBF_MultiPath.AlgorithmPath path = finalPaths.get(i);
            // Einfacher Titel mit Strategie-Code: nur Pfad-Nummer + Strategie
            String pathNumber = "1";
            if (path.pathDescription.contains("Pfad ")) {
                int start = path.pathDescription.indexOf("Pfad ") + 5;
                int end = path.pathDescription.indexOf(" ", start);
                if (end == -1) end = path.pathDescription.length();
                pathNumber = path.pathDescription.substring(start, end);
            }
            // Füge Strategie-Code hinzu
            String strategyCode = algorithm.getStrategyCodeForPath(path);
            String simplifiedTitle = "Pfad " + pathNumber;
            if (!strategyCode.isEmpty()) {
                simplifiedTitle += " (" + strategyCode + ")";
            }
            System.out.printf("Zeige Visualisierung %d/%d: %s\n", i + 1, finalPaths.size(), path.pathDescription);
            // Erstelle Algorithmus-Info für MultiPath
            String algorithmInfo = String.format("Algorithmus: MultiPath | Insgesamt %d Pfade erstellt", finalPaths.size());
            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(path.plate, "4", path.freeRects, simplifiedTitle, algorithmInfo);
            // Warte zwischen den Fenstern (außer beim letzten)
            if (i < finalPaths.size() - 1) {
                try {
                    Thread.sleep(2000); // 2 Sekunden zwischen den Fenstern
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
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
        // Test MaxRectBF_MultiPath und zeige jeden Pfad separat
        results.addAll(benchmarkMaxRectBF_MultiPath_AllPaths(originalJobs, Main::sortJobsBySizeDescending, "MultiPath (nach Fläche)"));
        results.addAll(benchmarkMaxRectBF_MultiPath_AllPaths(originalJobs, Main::sortJobsByLargestEdgeDescending, "MultiPath (nach Kante)"));
        
        // Ergebnisse sortieren nach Deckungsrate (absteigend)
        for (int i = 0; i < results.size() - 1; i++) {
            for (int j = 0; j < results.size() - 1 - i; j++) {
                if (results.get(j).coverageRate < results.get(j + 1).coverageRate) {
                    BenchmarkVisualizer.BenchmarkResult temp = results.get(j);
                    results.set(j, results.get(j + 1));
                    results.set(j + 1, temp);
                }
            }
        }
        // Zeige GUI Benchmark Visualizer
        BenchmarkVisualizer.showBenchmarkResults(results);
    }
    
    private static BenchmarkVisualizer.BenchmarkResult benchmarkFirstFit(List<Job> originalJobs) {
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) sortJobsBySizeDescending(jobs);
        Plate plate = new Plate("First Fit", 963, 650);
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            boolean placed = plate.placeJobFFShelf(job);
            if (!placed) {
                if (Main.DEBUG) System.out.println("Job " + job.id + " konnte nicht platziert werden.");
            }
        }
        int placedJobs = countPlacedJobs(jobs);
        double coverageRate = PlateVisualizer.calculateCoverageRate(plate);
        return new BenchmarkVisualizer.BenchmarkResult("First Fit", plate, null, placedJobs, coverageRate, originalJobs.size());
    }
    
    private static BenchmarkVisualizer.BenchmarkResult benchmarkMaxRectBF(List<Job> originalJobs, boolean useFullHeight, 
        java.util.function.Consumer<List<Job>> sortingMethod, String algorithmName) {
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) {
            sortingMethod.accept(jobs);
        }
        Plate plate = new Plate(algorithmName, 963, 650);
        MaxRectBF algorithm = new MaxRectBF(plate, useFullHeight);
        
        // Lambda-freie Implementierung
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            boolean placed = algorithm.placeJob(job);
            if (!placed) {
                if (Main.DEBUG) System.out.println("Job " + job.id + " konnte nicht platziert werden.");
            }
        }
        int placedJobs = countPlacedJobs(jobs);
        double coverageRate = PlateVisualizer.calculateCoverageRate(plate);
        return new BenchmarkVisualizer.BenchmarkResult(algorithmName, plate, algorithm, placedJobs, coverageRate, originalJobs.size());
    }
    
    private static BenchmarkVisualizer.BenchmarkResult benchmarkMaxRectBF_Dynamic(List<Job> originalJobs, 
        java.util.function.Consumer<List<Job>> sortingMethod, String algorithmName) {
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) {
            sortingMethod.accept(jobs);
        }
        Plate plate = new Plate(algorithmName, 963, 650);
        MaxRectBF_Dynamic algorithm = new MaxRectBF_Dynamic(plate);
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            boolean placed = algorithm.placeJob(job);
            if (!placed) {
                if (Main.DEBUG) System.out.println("Job " + job.id + " konnte nicht platziert werden.");
            }
        }
        int placedJobs = countPlacedJobs(jobs);
        double coverageRate = PlateVisualizer.calculateCoverageRate(plate);
        return new BenchmarkVisualizer.BenchmarkResult(algorithmName, plate, algorithm, placedJobs, coverageRate, originalJobs.size());
    }
    
    private static List<BenchmarkVisualizer.BenchmarkResult> benchmarkMaxRectBF_MultiPath_AllPaths(List<Job> originalJobs, 
        java.util.function.Consumer<List<Job>> sortingMethod, String algorithmBaseName) {
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) {
            sortingMethod.accept(jobs);
        }
        Plate plate = new Plate(algorithmBaseName, 963, 650);
        MaxRectBF_MultiPath algorithm = new MaxRectBF_MultiPath(plate);
        // Platziere Jobs mit MultiPath-Algorithmus (gleiche Logik wie im Einzelmodus)
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            boolean placed = algorithm.placeJob(job);
            if (!placed) {
                if (Main.DEBUG) System.out.println("Job " + job.id + " konnte in keinem Pfad platziert werden.");
            }
        }
        
        // Verwende die gleiche Logik wie im Einzelmodus für finale Pfade
        List<BenchmarkVisualizer.BenchmarkResult> pathResults = new ArrayList<>();
        List<MaxRectBF_MultiPath.AlgorithmPath> allPaths = algorithm.getAllPaths();
        // Sammle alle aktiven Pfade (gleiche Logik wie runMaxRectBF_MultiPath)
        for (int i = 0; i < allPaths.size(); i++) {
            MaxRectBF_MultiPath.AlgorithmPath path = allPaths.get(i);
            if (path.isActive) {
                int placedJobs = path.plate.jobs.size();
                double coverageRate = PlateVisualizer.calculateCoverageRate(path.plate);
                // Einfacher Titel mit Strategie-Code: nur Pfad-Nummer + Strategie
                String pathNumber = "1";
                if (path.pathDescription.contains("Pfad ")) {
                    int start = path.pathDescription.indexOf("Pfad ") + 5;
                    int end = path.pathDescription.indexOf(" ", start);
                    if (end == -1) end = path.pathDescription.length();
                    pathNumber = path.pathDescription.substring(start, end);
                }
                // Füge Strategie-Code hinzu
                String strategyCode = algorithm.getStrategyCodeForPath(path);
                String pathName = algorithmBaseName + " - Pfad " + pathNumber;
                if (!strategyCode.isEmpty()) {
                    pathName += " (" + strategyCode + ")";
                }
                // Verwende den erweiterten Konstruktor mit freien Rechtecken
                pathResults.add(new BenchmarkVisualizer.BenchmarkResult(
                    pathName, 
                    path.plate, 
                    algorithm, 
                    placedJobs, 
                    coverageRate, 
                    originalJobs.size(),
                    path.freeRects  // Füge die spezifischen freien Rechtecke hinzu
                ));
            }
        }
        
        return pathResults;
    }
    
    private static int countPlacedJobs(List<Job> jobs) {
        int count = 0;
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            if (job.placedOn != null) {
                count++;
            }
        }
        return count;
    }

    private static List<Job> createJobCopies(List<Job> originalJobs) {
        List<Job> copies = new ArrayList<>();
        if (DEBUG) System.out.println("Erstelle Jobs mit Schnittbreite von " + KERF_WIDTH + "mm:");
        for (int i = 0; i < originalJobs.size(); i++) {
            Job original = originalJobs.get(i);
            // Füge Schnittbreite zu den Dimensionen hinzu
            int widthWithKerf = original.width + KERF_WIDTH;
            int heightWithKerf = original.height + KERF_WIDTH;
            
            Job copy = new Job(original.id, widthWithKerf, heightWithKerf);
            // Speichere die ursprünglichen Dimensionen für die Anzeige
            copy.originalWidth = original.width;
            copy.originalHeight = original.height;
            copies.add(copy);
            if(DEBUG) System.out.printf("  Job %d: %dx%d -> %dx%d (mit Schnittbreite)\n", 
                original.id, original.width, original.height, widthWithKerf, heightWithKerf);
        }
        System.out.println();
        return copies;
    }
    
    private static String getUserAlgorithmChoice() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Algorithmus wählen (1 = First Fit, 2 = MaxRectsBF, 3 = MaxRectsBF Dynamic, 4 = MaxRectsBF Multi-Path, 0 = Benchmark): ");
            return scanner.nextLine().trim();
        }
    }
}