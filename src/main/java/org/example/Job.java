package org.example;

public class Job {
    int id;
    int width, height;
    int x = -1, y = -1;  // Hier wird gespeichert, auf welcher Position ein Job auf einer Platte platziert wurde (zur Kontrolle)
    Plate placedOn = null;
    boolean rotated;
    int placementOrder = -1;
    String splittingMethod = null;  // "FullWidth", "FullHeight" oder null


    public Job(int id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    // Für die Endergebnisausgabe
    @Override
    public String toString() {
        if (placedOn != null) {
            String splittingInfo = splittingMethod != null ? ", Splitting: " + splittingMethod : "";
            return "Job " + id + ": " + width + "mm x " + height + "mm -> " +
                    "Platte: " + placedOn.name +
                    ", Position: (" + x + ", " + y + ")" + splittingInfo;
        } else {
            return "Job " + id + ": " + width + "mm x " + height + "mm -> nicht platziert";
        }
    }
}
