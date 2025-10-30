package org.example.DataClasses;

public class Job {
    public int id;
    public double width;
    public double height;
    public double x = -1;
    public double y = -1;
    public Plate placedOn = null;
    public boolean rotated;
    public int placementOrder = -1;
    public String splittingMethod = null;

    public double originalWidth = -1;
    public double originalHeight = -1;

    int numberOfCuts = 0;


    // For unplaced jobs
    public Job(int id, double width, double height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    // For placed jobs
    public Job(int id, double x, double y, double width, double height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Only used to print a Job on the console
    @Override
    public String toString() {
        if (placedOn != null) {
            return String.format("Job %d: %.2fx%.2fmm auf %s bei (%.2f, %.2f)", id, width, height, placedOn.name, x, y);
        } else {
            return String.format("Job %d: %.2fx%.2fmm nicht platziert", id, width, height);
        }
    }
}

