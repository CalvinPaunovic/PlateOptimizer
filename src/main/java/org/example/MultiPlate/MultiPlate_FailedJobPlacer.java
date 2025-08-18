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
            System.out.println("  Job-ID: " + job.id + 
                             ", Breite: " + job.width + 
                             ", Höhe: " + job.height);
        }
        System.out.println("  Anzahl bereits platzierte Jobs: " + newPlate.jobs.size());
    }

    
    
    public void placeAllFailedJobs() {
        System.out.println("\n=== FailedJobPlacer: Platzierungsversuch ===");
        System.out.println("Pfad: " + currentPath.pathId);
        System.out.println("Platte: " + newPlate.name);

        MultiPlate_Algorithm algorithm = new MultiPlate_Algorithm();

        // Ersten Job aus der Liste der fehlgeschlagenen Jobs nehmen
        Job firstJob = failedJobsForPath.get(0);
        
        // Ergebnisobjekt für die beste Platzierung
        MultiPlate_DataClasses.BestFitResult result = new MultiPlate_DataClasses.BestFitResult();
        
        // Suche das beste freie Rechteck für diesen Job (ggf. auch rotiert)
        for (int j = 0; j < currentPath.freeRects.size(); j++) {
            MultiPlate_DataClasses.FreeRectangle rect = currentPath.freeRects.get(j);
            algorithm.testAndUpdateBestFit(firstJob.width, firstJob.height, rect, false, result);
            if (Main.rotateJobs) algorithm.testAndUpdateBestFit(firstJob.height, firstJob.width, rect, true, result);
        }

        // Falls kein Platz gefunden wurden, dann neuen Pfad erzeugen
        if (result.bestRect == null) {
            if (debugEnabled) System.out.println(currentPath.pathDescription + ": Job " + firstJob.id + " konnte NICHT platziert werden.");

            // Markiere den Job als "failed" für diesen Pfad
            currentPath.failedJobs.add(firstJob.id);

            // Bestimme die alternative Strategie (FullWidth oder FullHeight)
            MultiPlate_DataClasses.Strategy newStrategy = currentPath.strategy == MultiPlate_DataClasses.Strategy.FULL_HEIGHT ? MultiPlate_DataClasses.Strategy.FULL_WIDTH : MultiPlate_DataClasses.Strategy.FULL_HEIGHT;

            // TODO: mach eine neue Variable in beiden Konstruktoren von MultiPlate_DataClasses.
            // Jedes Pfad-Objekt, soll wissen, was die aktuelle Gesamtanzahl der Pfade ist, damit ich das hier einfach inkrementieren kann.
            // Ändere dazu auch die MultiPlate_Algorithm-Klasse, wenn ein neuer Pfad erzeugt wird, dann inkrementiere diese Anzahl einfach.

        }
    }
}