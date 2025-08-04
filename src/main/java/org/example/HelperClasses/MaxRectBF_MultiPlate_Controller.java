package org.example.HelperClasses;

import org.example.Algorithms.MaxRectBF_MultiPlate;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;

import java.util.*;

public class MaxRectBF_MultiPlate_Controller {

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
            System.out.println("Name=" + plate.name + ", Breite=" + plate.width + ", Höhe=" + plate.height + ", PlateId=" + plate.plateId);
        }

        // Erste Platte übergeben, um erstes freies Rechteck zu initialisieren
        MaxRectBF_MultiPlate algorithm = new MaxRectBF_MultiPlate(plateInfos);

        // Jobs auf der ersten Platte platzieren
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            algorithm.placeJob(job, plateInfos.get(0).plateId, 0);
        }
        // Ausgabe der Pfadübersicht und nicht platzierter Jobs auf der Konsole
        System.out.println("\n=== Pfad- und Nicht-platzierte-Jobs-Übersicht ===");
        printPathsAndFailedJobsOverviewToConsole(algorithm, plateInfos.get(0).plateId);




        // FailedJobs auf Platte 2 pro Pfad einfügen
        System.out.println("\n=== FailedJobs werden auf Platte 2 eingefügt ===");
        algorithm.printFailedJobsAndPlateInfoForPath("2");

    }

    // Gibt nur Pfade für plateIdFilter aus
    public static void printPathsAndFailedJobsOverviewToConsole(MaxRectBF_MultiPlate algorithm, String plateIdFilter) {
        List<MaxRectBF_MultiPlate.AlgorithmPath> overview = algorithm.getPathsAndFailedJobsOverviewData();
        for (int i = 0; i < overview.size(); i++) {
            MaxRectBF_MultiPlate.AlgorithmPath data = overview.get(i);
            if (plateIdFilter == null || plateIdFilter.equals(data.plateId)) {
                System.out.println(data.pathDescription + data.plate.name +
                    " | PlateId: " + data.plateId +
                    " | Breite: " + data.plate.width +
                    " | Höhe: " + data.plate.height +
                    " | Strategie: " + (data.useFullHeight ? "FullHeight" : "FullWidth") +
                    " | Parent: " + (data.parentPath != null ? data.parentPath : "-") +
                    " | Jobs: " + data.jobIds +
                    " | Nicht platzierte Jobs: " + data.failedJobs
                );
                System.out.println("  Platzierte Jobs:");
                for (Job job : data.placedJobs) {
                    System.out.printf("    Job %d: Platte=%s, Pos=(%.2f, %.2f), Größe=%.2fx%.2f, Rotiert=%s, Split=%s, Order=%d%n",
                            job.id,
                            job.placedOn != null ? job.placedOn.name : "",
                            job.x, job.y, job.width, job.height,
                            job.rotated, job.splittingMethod, job.placementOrder);
                }
                System.out.println("  Freie Rechtecke:");
                for (MaxRectBF_MultiPlate.FreeRectangle fr : data.freeRects) {
                    System.out.printf("    Pos=(%.2f, %.2f), Größe=%.2fx%.2f%n", fr.x, fr.y, fr.width, fr.height);
                }
            }
        }
    }

}