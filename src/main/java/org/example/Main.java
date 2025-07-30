package org.example;

import java.util.*;

import org.example.DataClasses.Job;
import org.example.HelperClasses.BenchmarkRunner;
import org.example.HelperClasses.JobUtils;
import org.example.Provider.JobListProvider;
import org.example.Provider.PlateProvider;
import org.example.Visualizer.BenchmarkVisualizer;

public class Main {
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_MultiPath = true;
    public static final boolean DEBUG_MultiPlateMultiPath = true;
    public static final int DEBUG_MultiPlateMultiPath_PathIndex = 1;

    public static final boolean rotateJobs = true;  // Bislang nur für MaxRectBestFit-Algorithmus.
    public static final boolean sortJobs = true;  // Bislang nur für MaxRectBestFit-Algorithmus.
    
    // Schnittbreite in mm wird zu jeder Job-Dimension hinzugefügt
    public static final int KERF_WIDTH = 0;  // 3mm Schnittbreite pro Seite

    public static final int VISUALIZE_PATH_INDEX = 1; // Nur dieser Pfad wird visualisiert
    
    /**
     * Führt das gewählte Platzierungsverfahren mit der gegebenen Jobliste und Platteninfo aus.
     */
    public static void runWithJobs(List<Job> originalJobs, String mode, PlateProvider.NamedPlate plateInfo) {
        switch (mode) {
            case "4":
                BenchmarkRunner.benchmarkMaxRectBF_MultiPathWithVisualization(originalJobs, plateInfo, sortJobs);
                break;
            case "0":
                throw new IllegalStateException("runWithJobs darf für Benchmark nicht mehr verwendet werden!");
            default:
                break;
        }
    }

    /**
     * Haupteinstiegspunkt des Programms. Fragt nach Jobliste und Algorithmus und startet die Berechnung.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // Plattenauswahl: Standard, Groß oder zwei Standardplatten
        List<PlateProvider.NamedPlate> plates;
        System.out.println("Plattenmodus wählen:");
        System.out.println("1 = Standardplatte");
        System.out.println("2 = Großplatte");
        System.out.println("3 = Zwei Standardplatten");
        System.out.print("Bitte wählen: ");
        String plateModeInput = scanner.nextLine().trim();
        switch (plateModeInput) {
            case "2":
                plates = Arrays.asList(PlateProvider.getLargePlate());
                break;
            case "3":
                plates = PlateProvider.getTwoStandardPlates();
                break;
            default:
                plates = Arrays.asList(PlateProvider.getStandardPlate());
                break;
        }

        JobListProvider.NamedJobList selection = getUserJobListChoiceWithScanner(scanner);
        List<Job> originalJobs = selection.jobs;
        String jobListInfo = selection.name;
        // Füge die Variable currentJobListInfo hinzu, falls sie fehlt
        String mode = getUserAlgorithmChoice(scanner);

        runMultiPlateMode(originalJobs, mode, jobListInfo, plates);

        scanner.close();
    }


    /**
     * Zeigt dem Nutzer eine Auswahl aller vordefinierten Joblisten und gibt die gewählte zurück.
     */
    private static JobListProvider.NamedJobList getUserJobListChoiceWithScanner(Scanner scanner) {
        JobListProvider.NamedJobList[] lists = new JobListProvider.NamedJobList[] {
            JobListProvider.getStandardJobList(),
            JobListProvider.getSmallJobList(),
            JobListProvider.getMediumJobList(),
            JobListProvider.getLargeJobList(),
            JobListProvider.getAllSameSizeList(),
            JobListProvider.getDuplicateJobsList(),
            JobListProvider.getTooLargeJobsList(),
            JobListProvider.getVerySmallJobsList(),
            JobListProvider.getMixedExtremeList(),
            JobListProvider.getSingleJobFitsExactly(),
            JobListProvider.getSingleJobTooLarge(),
            JobListProvider.getManyTinyJobs(),
            JobListProvider.getAlternatingSizesList(),
            JobListProvider.getDecimalJobsList(),
            JobListProvider.getExtendedStandardJobList() // <-- hinzugefügt
        };
        System.out.println("Welche Jobliste möchten Sie verwenden?");
        for (int i = 0; i < lists.length; i++) {
            System.out.printf("%d = %s\n", i + 1, lists[i].name);
        }
        System.out.print("Bitte wählen: ");
        String input = scanner.nextLine().trim();
        int idx;
        try {
            idx = Integer.parseInt(input) - 1;
        } catch (NumberFormatException e) {
            idx = 0;
        }
        if (idx < 0 || idx >= lists.length) idx = 0;
        return lists[idx];
    }

    /**
     * Führt das gewählte Platzierungsverfahren für eine einzelne Platte aus.
     */
    private static void runSinglePlateMode(List<Job> originalJobs, String mode, String jobListInfo, PlateProvider.NamedPlate plateInfo) {
        switch (mode) {
            case "0":
                runBenchmark(originalJobs, jobListInfo, plateInfo);
                break;
            default:
                runWithJobs(originalJobs, mode, plateInfo);
                break;
        }
    }

    /**
     * Führt das gewählte Platzierungsverfahren für mehrere Platten aus.
     */
    private static void runMultiPlateMode(List<Job> originalJobs, String mode, String jobListInfo, List<PlateProvider.NamedPlate> plates) {
        if ("5".equals(mode)) {
            runMaxRectBF_MultiPath(originalJobs, plates.get(0), plates);
        } else {
            runSinglePlateMode(originalJobs, mode, jobListInfo, plates.get(0));
        }
    }

    /**
     * Führt den MaxRectBF_MultiPath Multi-Path Algorithmus für eine oder mehrere Platten aus.
     */
    private static void runMaxRectBF_MultiPath(List<Job> originalJobs, PlateProvider.NamedPlate selectedPlate, List<PlateProvider.NamedPlate> selectedPlates) {
        System.out.println("\n=== MaxRectBF_MultiPath: Multi-Path Algorithmus ===\n");
        org.example.Algorithms.MultiPlateMultiPath multiPlateAlgo = new org.example.Algorithms.MultiPlateMultiPath();
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsBySizeDescending(jobs);
        String jobListInfo = selectedPlates.size() > 1 ? selectedPlate.name + " + weitere" : selectedPlate.name;
        multiPlateAlgo.placeJobsOnPlates(selectedPlates, jobs, jobListInfo);
        org.example.Visualizer.PlateVisualizer.showBenchmarkResults(multiPlateAlgo, jobListInfo);
    }

    /**
     * Führt den Benchmark für die aktuelle Jobliste aus.
     */
    private static void runBenchmark(List<Job> originalJobs, String jobListInfo, PlateProvider.NamedPlate plateInfo) {
        System.out.println("=== BENCHMARK ===");
        System.out.println("Jobliste: " + jobListInfo);
        List<BenchmarkVisualizer.BenchmarkResult> results = new ArrayList<>();
        // Entfernt: results.add(BenchmarkRunner.benchmarkFirstFit(originalJobs));
        results.addAll(BenchmarkRunner.benchmarkMaxRectBF_MultiPath_AllPaths(
            originalJobs, JobUtils::sortJobsBySizeDescending, "MultiPath (nach Fläche)", plateInfo));
        results.addAll(BenchmarkRunner.benchmarkMaxRectBF_MultiPath_AllPaths(
            originalJobs, JobUtils::sortJobsByLargestEdgeDescending, "MultiPath (nach Kante)", plateInfo));
        // Sortiere nach Deckungsrate absteigend
        results.sort((a, b) -> Double.compare(b.coverageRate, a.coverageRate));
        System.out.printf("Benchmark abgeschlossen für Jobliste: %s (%d Jobs)\n", jobListInfo, originalJobs.size());
        BenchmarkVisualizer.showBenchmarkResults(results, jobListInfo);
    }

    /**
     * Fragt den Nutzer nach dem gewünschten Algorithmus.
     */
    private static String getUserAlgorithmChoice(Scanner scanner) {
        System.out.print("Algorithmus wählen (4 = MaxRectsBF Multi-Path, 5 = MultiPlateMultiPath, 6 = MultiPlateMultiPath Benchmark, 7 = MaxRectBF_MultiPlate, 0 = Benchmark): ");
        return scanner.nextLine().trim();
    }
}