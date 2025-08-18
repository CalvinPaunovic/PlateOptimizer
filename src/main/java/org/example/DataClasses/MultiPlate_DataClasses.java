package org.example.DataClasses;

import java.util.ArrayList;
import java.util.List;

public class MultiPlate_DataClasses {
    public String pathId ;
    public Plate plate;
    public String plateId;
    public List<MultiPlate_DataClasses.FreeRectangle> freeRects;
    public String pathDescription;
    public Integer parentPathIndex;
    public List<List<MultiPlate_DataClasses.FreeRectangle>> freeRectsPerStep = new ArrayList<>();
    public int placementCounter;
    public List<MultiPlate_DataClasses.FreeRectangle> lastAddedRects;
    public boolean isActive;
    public Strategy strategy;
    public List<Integer> failedJobs;
    public String parentPath;
    public List<Job> placedJobs = new ArrayList<>();
    public List<Integer> jobIds = new ArrayList<>();
    public String splitFromPathId;
    public int splitFromJobId = -1;

    public enum Strategy {
        FULL_HEIGHT,
        FULL_WIDTH
    }

    // Konstruktor für die ersten zwei Elternpfade
    public MultiPlate_DataClasses(Plate originalPlate, String pathId, Strategy strategy) {
        this.plate = new Plate(originalPlate.name, originalPlate.width, originalPlate.height);
        this.freeRects = new ArrayList<>();
        this.freeRects.add(new MultiPlate_DataClasses.FreeRectangle(0, 0, plate.width, plate.height));
        this.lastAddedRects = new ArrayList<>();
        this.strategy = strategy;
        this.pathId = pathId;
        this.placementCounter = 0;
        this.isActive = true;
        this.failedJobs = new ArrayList<>();
        this.parentPath = null;
        this.plateId = null;
    }
    
    // Konstruktor für Kinderpfade
    public MultiPlate_DataClasses(MultiPlate_DataClasses original, String pathId, Strategy strategy, String splitFromPathId, int splitFromJobId) {
        this.plate = new Plate(original.plate.name, original.plate.width, original.plate.height, original.plate.plateId);
        for (int i = 0; i < original.plate.jobs.size(); i++) {
            Job originalJob = original.plate.jobs.get(i);
            Job copiedJob = new Job(originalJob.id, originalJob.width, originalJob.height);
            copiedJob.x = originalJob.x;
            copiedJob.y = originalJob.y;
            copiedJob.rotated = originalJob.rotated;
            copiedJob.placedOn = this.plate;
            copiedJob.placementOrder = originalJob.placementOrder;
            copiedJob.splittingMethod = originalJob.splittingMethod;
            this.plate.jobs.add(copiedJob);
        }
        this.freeRects = new ArrayList<>();
        for (int i = 0; i < original.freeRects.size(); i++) {
            MultiPlate_DataClasses.FreeRectangle rect = original.freeRects.get(i);
            this.freeRects.add(new MultiPlate_DataClasses.FreeRectangle(rect.x, rect.y, rect.width, rect.height));
        }
        this.lastAddedRects = new ArrayList<>();
        for (int i = 0; i < original.lastAddedRects.size(); i++) {
            MultiPlate_DataClasses.FreeRectangle rect = original.lastAddedRects.get(i);
            this.lastAddedRects.add(new MultiPlate_DataClasses.FreeRectangle(rect.x, rect.y, rect.width, rect.height));
        }
        this.strategy = original.strategy;
        this.placementCounter = original.placementCounter;
        this.isActive = true;
        this.failedJobs = new ArrayList<>(original.failedJobs);
        this.parentPath = original.pathDescription;
        this.placedJobs = new ArrayList<>();
        for (Job copiedJob : this.plate.jobs) {
            this.placedJobs.add(copiedJob);
        }
        this.plateId = null;
        this.pathId = pathId;
        this.splitFromPathId = splitFromPathId;
        this.splitFromJobId = splitFromJobId;
    }


    public static class FreeRectangle {
        public double x;
        public double y;
        public double width;
        public double height;
        public FreeRectangle(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }



    public static class BestFitResult {
        public FreeRectangle bestRect;
        public double bestScore = Double.MAX_VALUE;
        public boolean useRotated = false;
        public double bestWidth = -1;
        public double bestHeight = -1;
    }
}
