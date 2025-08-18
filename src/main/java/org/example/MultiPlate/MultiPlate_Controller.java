package org.example.MultiPlate;

import org.example.DataClasses.Job;
import org.example.DataClasses.MultiPlate_DataClasses;
import org.example.DataClasses.Plate;
import org.example.HelperClasses.JobUtils;

import java.util.*;

public class MultiPlate_Controller {

    /**
     * Führt den MultiPlate-Algorithmus aus.
     * Erwartet jetzt eine Liste von Platten.
     */
    public static void run_MaxRectBF_MultiPlate(List<Job> originalJobs, List<Plate> plateInfos, boolean sortJobs) {
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsBySizeDescending(jobs);

        // Ausgabe der Platteninformationen auf der Konsole
        System.out.println("\n=== Plattenliste ===");
        for (Plate plate : plateInfos) {
            System.out.println("Name=" + plate.name + ", Breite=" + plate.width + ", Höhe=" + plate.height);
        }

        // Jeden Job einzeln auf einer Platte platzieren
        MultiPlate_Algorithm algorithm = new MultiPlate_Algorithm(plateInfos);
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            //System.out.printf("placeJob ERNEUT AUFGERUFEN\n");
            algorithm.placeJob(job, "1");
        }

        // Ausgabe der Pfad-Details
        System.out.println("\n=== Pfad-Details ===");
        List<MultiPlate_DataClasses> pathDetails = algorithm.getPathsAndFailedJobsOverviewData();
        printPathDetails(pathDetails);

        // Für jeden Pfad die zugehörigen failedJobs auf dem jeweiligen Pfad platzieren
        for (MultiPlate_DataClasses path : pathDetails) {
            if (path.failedJobs != null && !path.failedJobs.isEmpty()) {
                List<Job> failedJobsForPath = new ArrayList<>();
                for (Job job : jobs) {
                    if (path.failedJobs.contains(job.id)) {
                        failedJobsForPath.add(job);
                    }
                }
                if (!failedJobsForPath.isEmpty()) {
                    MultiPlate_FailedJobPlacer placer = new MultiPlate_FailedJobPlacer(path, plateInfos.get(1), path.pathDescription, failedJobsForPath);
                    placer.placeAllFailedJobs();
                }
            }
        }

    }

    public static void printPathDetails(List<MultiPlate_DataClasses> pathDetails) {
        for (MultiPlate_DataClasses info : pathDetails) {
            System.out.println("Pfad: " + info.pathId);
            System.out.println("  Platte: " + info.plate.name);
            System.out.println("  Strategie: " + (info.strategy == MultiPlate_DataClasses.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth"));
            System.out.println("  Parent: " + (info.parentPath != null ? info.parentPath : "-"));
            System.out.println("  Abgespalten von Pfad: " + (info.splitFromPathId != null ? info.splitFromPathId : "-") + ", ab Job: " + (info.splitFromJobId != -1 ? info.splitFromJobId : "-"));
            System.out.println("  Jobs:");
            for (Job job : info.plate.jobs) {
                System.out.println("    " + job.toString());
            }
            System.out.println("  Nicht platzierte Jobs: " + info.failedJobs);
            System.out.println();
        }
    }
}