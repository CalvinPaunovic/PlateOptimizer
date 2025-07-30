package org.example.HelperClasses;

import org.example.Algorithms.MaxRectBF_MultiPlate;
import org.example.DataClasses.Job;
import org.example.Provider.PlateProvider;

import java.util.*;

public class MaxRectBF_MultiPlate_Controller {

    /**
     * Führt den MultiPlate-Algorithmus aus.
     * Erwartet jetzt eine Liste von Platten.
     */
    public static void run_MaxRectBF_MultiPlater(List<Job> originalJobs, List<PlateProvider.NamedPlate> plateInfos, boolean sortJobs) {
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsBySizeDescending(jobs);

        // Ausgabe der Platteninformationen auf der Konsole
        System.out.println("\n=== Plattenliste ===");
        for (PlateProvider.NamedPlate plate : plateInfos) {
            System.out.println("Name=" + plate.name + ", Breite=" + plate.width + ", Höhe=" + plate.height);
        }

        MaxRectBF_MultiPlate algorithm = new MaxRectBF_MultiPlate(plateInfos);

        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            algorithm.placeJob(job);
        }

        // Ausgabe der Pfadübersicht auf der Konsole
        System.out.println("\n=== Pfadübersicht ===");
        algorithm.printPathsOverview();

        // Ausgabe der nicht platzierten Job-IDs pro Pfad
        System.out.println("\n=== Nicht platzierte Job-IDs pro Pfad ===");
        printFailedJobIdsPerPath(algorithm);
    }

    // Gibt für jeden Pfad die nicht platzierten Job-IDs inkl. Plattenbezeichnung auf die Konsole aus.
    public static void printFailedJobIdsPerPath(MaxRectBF_MultiPlate algorithm) {
        List<List<Integer>> failedJobsPerPath = algorithm.getFailedJobIdsPerPath();
        List<MaxRectBF_MultiPlate.AlgorithmPath> paths = algorithm.getAllPaths();
        for (int i = 0; i < failedJobsPerPath.size(); i++) {
            String plateName = paths.get(i).plate.name;
            System.out.println("Pfad " + (i + 1) + " | Platte: " + plateName + " | Nicht platzierte Jobs: " + failedJobsPerPath.get(i));
        }
    }

}