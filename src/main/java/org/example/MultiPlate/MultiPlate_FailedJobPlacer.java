package org.example.MultiPlate;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.MultiPlate_DataClasses;
import org.example.DataClasses.Plate;

import java.util.List;

public class MultiPlate_FailedJobPlacer {
    boolean debugEnabled = true;

    private MultiPlate_DataClasses currentPath;
    private Plate newPlate;
    private List<MultiPlate_DataClasses.FreeRectangle> freeRects;
    private List<Job> failedJobsForPath;

    public MultiPlate_FailedJobPlacer(MultiPlate_DataClasses currentPath, Plate newPlate, String pathId, List<Job> failedJobsForPath) {
        this.currentPath = currentPath;
        this.newPlate = newPlate;
        this.freeRects = new java.util.ArrayList<>();
        this.freeRects.add(new MultiPlate_DataClasses.FreeRectangle(0, 0, newPlate.width, newPlate.height));
        this.failedJobsForPath = failedJobsForPath;

        System.out.println("\n=== FailedJobPlacer - Ausgabe ===");
        System.out.println("Pfad-ID: " + currentPath.pathId);
        System.out.println("Platte-ID: '" + currentPath.plate.name + ", Anzahl bereits platzierte Jobs: " + currentPath.plate.jobs.size());
        System.out.println("Neue Platte-ID: '" + newPlate.name + "', noch zu platzierende Jobs:");
        for (Job job : failedJobsForPath) {
            System.out.println("  Job-ID: " + job.id + ", Breite: " + job.width + ", Höhe: " + job.height);
        }
        System.out.println("  Anzahl bereits platzierte Jobs: " + newPlate.jobs.size());
    }

    public void placeAllFailedJobs() {
        System.out.println("\n=== FailedJobPlacer: Platzierungsversuch (nur newPlate) ===");
        System.out.println("Pfad: " + currentPath.pathId);
        System.out.println("Neue Platte: " + newPlate.name);

        if (failedJobsForPath.isEmpty()) {
            if (debugEnabled) System.out.println("[FJP] Keine Jobs zum Platzieren.");
            return;
        }

        MultiPlate_Algorithm algorithm = new MultiPlate_Algorithm();
        MultiPlate_DataClasses.Strategy tempStrategy = currentPath.strategy; // gleiche Strategie wie aktueller Pfad
        MultiPlate_DataClasses tempPath = new MultiPlate_DataClasses(this.newPlate, "NP-" + currentPath.pathId, tempStrategy, currentPath.totalPathCount + 1);
        tempPath.freeRects.clear();
        for (MultiPlate_DataClasses.FreeRectangle fr : freeRects) {
            tempPath.freeRects.add(new MultiPlate_DataClasses.FreeRectangle(fr.x, fr.y, fr.width, fr.height));
        }

        String splittingMethod = tempStrategy == MultiPlate_DataClasses.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth";
        int placedCount = 0;
        int failedCount = 0;

        if (debugEnabled) System.out.println("[FJP] Starte Sequenzplatzierung für " + failedJobsForPath.size() + " Jobs auf neuer Platte");

        for (int jobIndex = 0; jobIndex < failedJobsForPath.size(); jobIndex++) {
            Job currentJob = failedJobsForPath.get(jobIndex);
            if (debugEnabled) {
                System.out.println("[FJP] Job#" + jobIndex + " ID=" + currentJob.id + " (w=" + currentJob.width + ", h=" + currentJob.height + ") - vorhandene freie Rechtecke=" + tempPath.freeRects.size());
                for (int i = 0; i < tempPath.freeRects.size(); i++) {
                    MultiPlate_DataClasses.FreeRectangle fr = tempPath.freeRects.get(i);
                    System.out.println("[FJP]   FR#" + i + " x=" + fr.x + " y=" + fr.y + " w=" + fr.width + " h=" + fr.height);
                }
            }

            MultiPlate_DataClasses.BestFitResult result = new MultiPlate_DataClasses.BestFitResult();
            for (int j = 0; j < tempPath.freeRects.size(); j++) {
                MultiPlate_DataClasses.FreeRectangle rect = tempPath.freeRects.get(j);
                algorithm.testAndUpdateBestFit(currentJob.width, currentJob.height, rect, false, result);
                if (Main.rotateJobs) algorithm.testAndUpdateBestFit(currentJob.height, currentJob.width, rect, true, result);
            }

            if (result.bestRect == null) {
                failedCount++;
                currentPath.failedJobs.add(currentJob.id);
                if (debugEnabled) System.out.println("[FJP] -> Kein Platz gefunden für Job " + currentJob.id + " (bleibt failed)");
                continue;
            }

            algorithm.placeJobInPath(currentJob, result, tempPath, splittingMethod);
            placedCount++;
            if (debugEnabled) {
                System.out.println("[FJP] -> Platziert: Job " + currentJob.id + " an (" + currentJob.x + "," + currentJob.y + ") size=" + currentJob.width + "x" + currentJob.height + (currentJob.rotated ? " (rotated)" : ""));
                System.out.println("[FJP]    Neue freie Rechtecke=" + tempPath.freeRects.size());
            }
        }

        // Lokale freeRects Spiegel aktualisieren
        freeRects.clear();
        for (MultiPlate_DataClasses.FreeRectangle fr : tempPath.freeRects) freeRects.add(new MultiPlate_DataClasses.FreeRectangle(fr.x, fr.y, fr.width, fr.height));

        System.out.println("[FJP] SUMMARY placed=" + placedCount + " failed=" + failedCount + " total=" + failedJobsForPath.size() + " strategy=" + splittingMethod);
    }
}
