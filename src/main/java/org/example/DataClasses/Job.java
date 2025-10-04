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


    public Job(int id, double width, double height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    public Job(int id, double x, double y, double width, double height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        if (placedOn != null) {
            String splittingInfo = splittingMethod != null ? ", Splitting: " + splittingMethod : "";
            return String.format("Job %d: %.2fmm x %.2fmm -> Platte: %s, Position: (%.2f, %.2f)%s",
                    id, width, height, placedOn.name, x, y, splittingInfo);
        } else {
            return String.format("Job %d: %.2fmm x %.2fmm -> nicht platziert", id, width, height);
        }
    }
}

