package org.example;

import java.util.*;

import org.example.DataClasses.Job;
import org.example.HelperClasses.BenchmarkRunner;
import org.example.HelperClasses.JobUtils;
import org.example.Provider.JobListProvider;
import org.example.Provider.PlateProvider;
import org.example.Visualizer.BenchmarkVisualizer;
import org.example.Visualizer.PlateVisualizer;

public class Main {
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_MaxRectBF = false;
    public static final boolean DEBUG_MaxRectBF_Dynamic = false;
    public static final boolean DEBUG_MultiPath = false;

    public static final boolean rotateJobs = true;  // Bislang nur für MaxRectBestFit-Algorithmus.
    public static final boolean sortJobs = true;  // Bislang nur für MaxRectBestFit-Algorithmus.
    
    // Schnittbreite in mm wird zu jeder Job-Dimension hinzugefügt
    public static final int KERF_WIDTH = 0;  // 3mm Schnittbreite pro Seite

    /**
     * Führt das gewählte Platzierungsverfahren mit der gegebenen Jobliste und Platteninfo aus.
     */
    public static void runWithJobs(List<Job> originalJobs, String mode, PlateProvider.NamedPlate plateInfo) {
        switch (mode) {
            case "2":
                runMaxRectBF(originalJobs, plateInfo);
                break;
            case "3":
                runMaxRectBF_Dynamic(originalJobs, plateInfo);
                break;
            case "4":
                BenchmarkRunner.benchmarkMaxRectBF_MultiPathWithVisualization(originalJobs, plateInfo, sortJobs);
                break;
            case "0":
                throw new IllegalStateException("runWithJobs darf für Benchmark nicht mehr verwendet werden!");
            default:
                runFirstFitAlgorithm(originalJobs, plateInfo);
                break;
        }
    }

    /**
     * Haupteinstiegspunkt des Programms. Fragt nach Jobliste und Algorithmus und startet die Berechnung.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        PlateProvider.NamedPlate plateInfo = getUserPlateChoiceWithScanner(scanner);
        JobListProvider.NamedJobList selection = getUserJobListChoiceWithScanner(scanner);
        List<Job> originalJobs = selection.jobs;
        String jobListInfo = selection.name;
        currentJobListInfo = jobListInfo; // Setze aktuelle Joblisten-Info
        String mode = getUserAlgorithmChoice(scanner);
        runWithJobsWithInfo(originalJobs, mode, jobListInfo, plateInfo);
        scanner.close();
    }

    // Nutzer nach Plattenformat fragen
    private static PlateProvider.NamedPlate getUserPlateChoiceWithScanner(Scanner scanner) {
        PlateProvider.NamedPlate[] plates = new PlateProvider.NamedPlate[] {
            PlateProvider.getStandardPlate(),
            PlateProvider.getLargePlate()
        };
        System.out.println("Welches Plattenformat möchten Sie verwenden?");
        for (int i = 0; i < plates.length; i++) {
            System.out.printf("%d = %s (%.1f x %.1f mm)\n", i + 1, plates[i].name, plates[i].width, plates[i].height);
        }
        System.out.print("Bitte wählen: ");
        String input = scanner.nextLine().trim();
        int idx;
        try {
            idx = Integer.parseInt(input) - 1;
        } catch (NumberFormatException e) {
            idx = 0;
        }
        if (idx < 0 || idx >= plates.length) idx = 0;
        return plates[idx];
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
            JobListProvider.getDecimalJobsList()
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
     * Wrapper für Benchmark, damit Info übergeben werden kann.
     */
    private static void runWithJobsWithInfo(List<Job> originalJobs, String mode, String jobListInfo, PlateProvider.NamedPlate plateInfo) {
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
     * Führt den MaxRectBF-Algorithmus mit verschiedenen Sortiermethoden aus.
     */
    private static void runMaxRectBF(List<Job> originalJobs, PlateProvider.NamedPlate plateInfo) {
        BenchmarkVisualizer.BenchmarkResult resultByArea = BenchmarkRunner.benchmarkMaxRectBF(
            originalJobs, true, JobUtils::sortJobsBySizeDescending, "MaxRectBF", plateInfo);
        BenchmarkVisualizer.BenchmarkResult resultByEdge = BenchmarkRunner.benchmarkMaxRectBF(
            originalJobs, true, JobUtils::sortJobsByLargestEdgeDescending, "MaxRectBF", plateInfo);
        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
            resultByArea.plate, "2", null, resultByArea.plate.name, null, getCurrentJobListInfo());
        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
            resultByEdge.plate, "2", null, resultByEdge.plate.name, null, getCurrentJobListInfo());
    }

    /**
     * Führt den MaxRectBF_Dynamic-Algorithmus mit verschiedenen Sortiermethoden aus.
     */
    private static void runMaxRectBF_Dynamic(List<Job> originalJobs, PlateProvider.NamedPlate plateInfo) {
        BenchmarkVisualizer.BenchmarkResult resultByArea = BenchmarkRunner.benchmarkMaxRectBF_Dynamic(
            originalJobs, JobUtils::sortJobsBySizeDescending, "MaxRectBF Dynamic", plateInfo);
        BenchmarkVisualizer.BenchmarkResult resultByEdge = BenchmarkRunner.benchmarkMaxRectBF_Dynamic(
            originalJobs, JobUtils::sortJobsByLargestEdgeDescending, "MaxRectBF Dynamic", plateInfo);
        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
            resultByArea.plate, "2", null, resultByArea.plate.name, null, getCurrentJobListInfo());
        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
            resultByEdge.plate, "2", null, resultByEdge.plate.name, null, getCurrentJobListInfo());
    }

    /**
     * Führt den First-Fit-Algorithmus aus und zeigt das Ergebnis an.
     */
    private static void runFirstFitAlgorithm(List<Job> originalJobs, PlateProvider.NamedPlate plateInfo) {
        BenchmarkVisualizer.BenchmarkResult result = BenchmarkRunner.benchmarkFirstFit(originalJobs, plateInfo);
        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
            result.plate, "1", null, result.plate.name, null, getCurrentJobListInfo());
    }

    // Hilfsmethode, um die aktuelle Joblisten-Info zu speichern/übergeben
    private static String currentJobListInfo = null;
    private static String getCurrentJobListInfo() {
        return currentJobListInfo;
    }

    /**
     * Führt den Benchmark für die aktuelle Jobliste aus.
     */
    private static void runBenchmark(List<Job> originalJobs, String jobListInfo, PlateProvider.NamedPlate plateInfo) {
        System.out.println("=== BENCHMARK ===");
        System.out.println("Jobliste: " + jobListInfo);
        List<BenchmarkVisualizer.BenchmarkResult> results = new ArrayList<>();
        // FirstFit wird nicht mehr zum Benchmark hinzugefügt!
        // results.add(BenchmarkRunner.benchmarkFirstFit(originalJobs));
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
        System.out.print("Algorithmus wählen (1 = First Fit, 2 = MaxRectsBF, 3 = MaxRectsBF Dynamic, 4 = MaxRectsBF Multi-Path, 0 = Benchmark): ");
        return scanner.nextLine().trim();
    }
}