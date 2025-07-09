package org.example;

import java.util.*;

public class Main {

    public static final boolean DEBUG = true;  // Zwischenschritte auf GUI aktivieren/deaktivieren.

    public static final boolean rotateJobs = true;  // Bislang nur für MaxRectBestFit-Algorithmus.
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
        System.out.println("Jobs sortiert nach Fläche absteigend:");
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            int area = job.width * job.height;
            System.out.printf("%d: Job ID=%d, Größe=%dx%d, Fläche=%d\n", i + 1, job.id, job.width, job.height, area);
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
        System.out.println("Jobs sortiert nach größter Kante absteigend:");
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            int maxEdge = Math.max(job.width, job.height);
            System.out.printf("%d: Job ID=%d, Größe=%dx%d, größte Kante=%d\n", i + 1, job.id, job.width, job.height, maxEdge);
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
                //new Job(5, 188,243),  // Gedreht
                new Job(6,205,153)
        );

        String mode = getUserAlgorithmChoice();

        switch (mode) {
            case "2":
                runMaxRectBFComparison(originalJobs);
                break;
            case "3":
                runMaxRectBFDynamicComparison(originalJobs);
                break;
            default:
                runFirstFitAlgorithm(originalJobs);
                break;
        }
    }

    private static String getUserAlgorithmChoice() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Algorithmus wählen (1 = First Fit, 2 = MaxRectsBF, 3 = MaxRectsBF Dynamic): ");
            return scanner.nextLine().trim();
        }
    }

    private static void runMaxRectBFDynamicComparison(List<Job> originalJobs) {
        System.out.println("\n=== VERGLEICH: MaxRectBFDynamic mit verschiedenen Sortiermethoden ===\n");
        // Test mit Sortierung nach Fläche
        AlgorithmResult resultBySize = runMaxRectBFDynamicWithSorting(
            originalJobs, 
            "FLAECHE", 
            "Plate A (Dynamic - nach Fläche)",
            Main::sortJobsBySizeDescending
        );
        // Test mit Sortierung nach größter Kante
        AlgorithmResult resultByEdge = runMaxRectBFDynamicWithSorting(
            originalJobs, 
            "GROESSTER KANTE", 
            "Plate B (Dynamic - nach Kante)",
            Main::sortJobsByLargestEdgeDescending
        );
        // Vergleiche und zeige Ergebnisse
        compareAndDisplayResults(
            resultBySize, "Sortierung nach Fläche",
            resultByEdge, "Sortierung nach größter Kante"
        );
        // Zeige Visualisierungen
        PlateVisualizer.showPlate(resultBySize.plate, "3", resultBySize.algorithm);
        PlateVisualizer.showPlate(resultByEdge.plate, "3", resultByEdge.algorithm);
    }

    private static AlgorithmResult runMaxRectBFDynamicWithSorting(
            List<Job> originalJobs, 
            String sortingName, 
            String plateName,
            java.util.function.Consumer<List<Job>> sortingMethod) {
        System.out.println(">>> TESTE SORTIERUNG NACH " + sortingName + " <<<");
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) sortingMethod.accept(jobs);
        
        Plate plate = new Plate(plateName, 963, 650);
        MaxRectBFDynamic algorithm = new MaxRectBFDynamic(plate);
        
        placeJobsWithAlgorithm(jobs, algorithm::placeJob);
        
        return new AlgorithmResult(plate, algorithm, PlateVisualizer.calculateCoverageRate(plate));
    }

    private static void runMaxRectBFComparison(List<Job> originalJobs) {
        System.out.println("\n=== VERGLEICH: MaxRectBF FullWidth vs FullHeight ===\n");
        
        // Test FullWidth
        AlgorithmResult fullWidthResult = runMaxRectBFWithMode(
            originalJobs, 
            "FULLWIDTH METHODE", 
            "Plate A (FullWidth)",
            false
        );
        
        // Test FullHeight
        AlgorithmResult fullHeightResult = runMaxRectBFWithMode(
            originalJobs, 
            "FULLHEIGHT METHODE", 
            "Plate A (FullHeight)",
            true
        );
        
        // Vergleiche und zeige Ergebnisse
        compareAndDisplayResults(
            fullWidthResult, "FullWidth",
            fullHeightResult, "FullHeight"
        );
        
        // Zeige Visualisierungen
        PlateVisualizer.showPlate(fullWidthResult.plate, "2", fullWidthResult.algorithm);
        PlateVisualizer.showPlate(fullHeightResult.plate, "2", fullHeightResult.algorithm);
    }

    private static AlgorithmResult runMaxRectBFWithMode(
            List<Job> originalJobs, 
            String modeName, 
            String plateName,
            boolean useFullHeight) {
        
        System.out.println(">>> TESTE " + modeName + " <<<");
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) sortJobsBySizeDescending(jobs);
        
        Plate plate = new Plate(plateName, 963, 650);
        MaxRectBF algorithm = new MaxRectBF(plate, useFullHeight);
        
        placeJobsWithAlgorithm(jobs, algorithm::placeJob);
        
        return new AlgorithmResult(plate, algorithm, PlateVisualizer.calculateCoverageRate(plate));
    }

    private static void runFirstFitAlgorithm(List<Job> originalJobs) {
        List<Job> jobs = createJobCopies(originalJobs);
        if (sortJobs) sortJobsBySizeDescending(jobs);
        
        Plate plate = new Plate("Plate A", 963, 650);
        placeJobsWithAlgorithm(jobs, plate::placeJobFFShelf);
        
        printJobPlacements(jobs);
        System.out.println("\n=== Used Plate ===");
        System.out.println(plate.name + " hat " + plate.jobs.size() + " Jobs.");
        PlateVisualizer.showPlate(plate, "1", null);
    }

    private static void placeJobsWithAlgorithm(List<Job> jobs, java.util.function.Function<Job, Boolean> placementFunction) {
        for (Job job : jobs) {
            boolean placed = placementFunction.apply(job);
            if (!placed) {
                System.out.println("Job " + job.id + " konnte nicht platziert werden.");
            }
        }
    }

    private static void compareAndDisplayResults(AlgorithmResult result1, String name1, AlgorithmResult result2, String name2) {
        
        System.out.println("\n=== VERGLEICHSERGEBNISSE ===");
        System.out.printf("%s - Deckungsrate: %.2f%%\n", name1, result1.coverageRate);
        System.out.printf("%s - Deckungsrate: %.2f%%\n", name2, result2.coverageRate);
        
        if (result1.coverageRate > result2.coverageRate) {
            System.out.println(">>> " + name1 + " ist besser! <<<");
        } else if (result2.coverageRate > result1.coverageRate) {
            System.out.println(">>> " + name2 + " ist besser! <<<");
        } else {
            System.out.println(">>> Beide Methoden haben die gleiche Deckungsrate! <<<");
        }
    }

    private static void printJobPlacements(List<Job> jobs) {
        System.out.println("\n=== Job Placement ===");
        for (Job job : jobs) {
            System.out.println(job);
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
    
    // Helper class to store algorithm results
    private static class AlgorithmResult {
        final Plate plate;
        final Object algorithm;
        final double coverageRate;
        
        AlgorithmResult(Plate plate, Object algorithm, double coverageRate) {
            this.plate = plate;
            this.algorithm = algorithm;
            this.coverageRate = coverageRate;
        }
    }
}