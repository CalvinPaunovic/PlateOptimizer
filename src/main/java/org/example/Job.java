package org.example;

public class Job {
    int id;
    int width, height;
    int x = -1, y = -1;  // Hier wird gespeichert, auf welcher Position ein Job auf einer Platte platziert wurde (zur Kontrolle)
    Plate placedOn = null;
    boolean rotated;
    int placementOrder = -1;


    public Job(int id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    // Für die Endergebnisausgabe
    @Override
    public String toString() {
        if (placedOn != null) {
            return "Job " + id + ": " + width + "mm x " + height + "mm -> " +
                    "Platte: " + placedOn.name +
                    ", Position: (" + x + ", " + y + ")";
        } else {
            return "Job " + id + ": " + width + "mm x " + height + "mm -> nicht platziert";
        }
    }
}
