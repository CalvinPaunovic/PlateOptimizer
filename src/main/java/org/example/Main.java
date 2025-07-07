package org.example;

import java.util.*;

public class Main {

    public static final boolean DEBUG = true;  // Zwischenschritte auf GUI aktivieren/deaktivieren.
    public static final boolean allAlgorithms = true;  // Userabfrage nach Wunschalgorithmus aktivieren/deaktivieren. Default ist MacRectBF.

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

        String mode;
        try (Scanner scanner = new Scanner(System.in)) {
            if (allAlgorithms) {
                System.out.print("Algorithmus wählen (1 = First Fit, 2 = MaxRectsBF, 3 = MaxRectsBF Dynamic): ");
                mode = scanner.nextLine().trim();
            } else mode = "2";
        }

        if ("2".equals(mode)) {
            // Führe beide MaxRectBF-Varianten aus und vergleiche
            runMaxRectBFComparison(originalJobs);
        } else if ("3".equals(mode)) {
            // MaxRectBFDynamic
            List<Job> jobs = createJobCopies(originalJobs);
            if (sortJobs) sortJobsBySizeDescending(jobs);
            Plate plateA = new Plate("Plate A (Dynamic)", 963, 650);
            MaxRectBFDynamic maxRectBFDynamic = new MaxRectBFDynamic(plateA);
            for (int i = 0; i < jobs.size(); i++) {
                Job job = jobs.get(i);
                boolean placed = maxRectBFDynamic.placeJob(job);
                if (!placed) {
                    System.out.println("Job " + job.id + " konnte nicht platziert werden.");
                }
            }
            System.out.println("\n=== Job Placement ===");
            for (int i = 0; i < jobs.size(); i++) {
                Job job = jobs.get(i);
                System.out.println(job);
            }
            System.out.println("\n=== Used Plate ===");
            System.out.println(plateA.name + " hat " + plateA.jobs.size() + " Jobs.");
            PlateVisualizer.showPlate(plateA, "3", maxRectBFDynamic);
        } else {
            // First Fit 
            List<Job> jobs = createJobCopies(originalJobs);
            if (sortJobs) sortJobsBySizeDescending(jobs);
            Plate plateA = new Plate("Plate A", 963, 650);
            for (int i = 0; i < jobs.size(); i++) {
                Job job = jobs.get(i);
                boolean placed = plateA.placeJobFFShelf(job);
                if (!placed) {
                    System.out.println("Job " + job.id + " konnte nicht platziert werden.");
                }
            }
            System.out.println("\n=== Job Placement ===");
            for (int i = 0; i < jobs.size(); i++) {
                Job job = jobs.get(i);
                System.out.println(job);
            }
            System.out.println("\n=== Used Plate ===");
            System.out.println(plateA.name + " hat " + plateA.jobs.size() + " Jobs.");
            PlateVisualizer.showPlate(plateA, mode, null);
        }
    }

    private static void runMaxRectBFComparison(List<Job> originalJobs) {
        System.out.println("\n=== VERGLEICH: MaxRectBF FullWidth vs FullHeight ===\n");
        // Test FullWidth
        System.out.println(">>> TESTE FULLWIDTH METHODE <<<");
        List<Job> jobsFullWidth = createJobCopies(originalJobs);
        if (sortJobs) sortJobsBySizeDescending(jobsFullWidth);
        Plate plateFullWidth = new Plate("Plate A (FullWidth)", 963, 650);
        MaxRectBF maxRectBFFullWidth = new MaxRectBF(plateFullWidth, false);
        for (int i = 0; i < jobsFullWidth.size(); i++) {
            Job job = jobsFullWidth.get(i);
            boolean placed = maxRectBFFullWidth.placeJob(job);
            if (!placed) {
                System.out.println("Job " + job.id + " konnte nicht platziert werden (FullWidth).");
            }
        }
        // Test FullHeight
        System.out.println("\n>>> TESTE FULLHEIGHT METHODE <<<");
        List<Job> jobsFullHeight = createJobCopies(originalJobs);
        if (sortJobs) sortJobsBySizeDescending(jobsFullHeight);
        Plate plateFullHeight = new Plate("Plate A (FullHeight)", 963, 650);
        MaxRectBF maxRectBFFullHeight = new MaxRectBF(plateFullHeight, true);
        for (int i = 0; i < jobsFullHeight.size(); i++) {
            Job job = jobsFullHeight.get(i);
            boolean placed = maxRectBFFullHeight.placeJob(job);
            if (!placed) {
                System.out.println("Job " + job.id + " konnte nicht platziert werden (FullHeight).");
            }
        }
        // Vergleiche Ergebnisse
        double coverageFullWidth = PlateVisualizer.calculateCoverageRate(plateFullWidth);
        double coverageFullHeight = PlateVisualizer.calculateCoverageRate(plateFullHeight);
        System.out.println("\n=== VERGLEICHSERGEBNISSE ===");
        System.out.printf("FullWidth Deckungsrate: %.2f%%\n", coverageFullWidth);
        System.out.printf("FullHeight Deckungsrate: %.2f%%\n", coverageFullHeight);

        if (coverageFullWidth > coverageFullHeight) {
            System.out.println(">>> FullWidth Methode ist besser! <<<");
        } else if (coverageFullHeight > coverageFullWidth) {
            System.out.println(">>> FullHeight Methode ist besser! <<<");
        } else {
            System.out.println(">>> Beide Methoden haben die gleiche Deckungsrate! <<<");
        }
        // Zeige finale Visualisierungen
        PlateVisualizer.showPlate(plateFullWidth, "2", maxRectBFFullWidth);
        PlateVisualizer.showPlate(plateFullHeight, "2", maxRectBFFullHeight);
    }

    private static List<Job> createJobCopies(List<Job> originalJobs) {
        List<Job> copies = new ArrayList<>();
        for (int i = 0; i < originalJobs.size(); i++) {
            Job original = originalJobs.get(i);
            copies.add(new Job(original.id, original.width, original.height));
        }
        return copies;
    }
}