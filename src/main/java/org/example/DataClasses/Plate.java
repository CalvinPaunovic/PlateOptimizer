package org.example.DataClasses;

import java.util.ArrayList;
import java.util.List;

public class Plate {

    public String name;
    public double width;
    public double height;
    double currentX = 0, currentY = 0;
    double shelfHeight = 0;
    public List<Job> jobs = new ArrayList<>();

    public Plate(String name, double width, double height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    // =============== First-Fit Shelf-Packing =============== //
    public boolean placeJobFFShelf(Job job) {
        // First-Fit Algorithmus, aber jede Zeile wird als ein "Regal mit gleichmäßiger Höhe" betrachtet.
        // Ein neuer Job, der zwar in Regal 2 unter einem bereits platzierten Job von Regal 1 passen würde, könnte möglicherweise nicht platziert werden,
        // weil der höchste Job von Regal 1 die Höhe des Regals bestimmt.
        //
        // passt Job in aktuelle Zeile?
        if (currentX + job.width <= width) {
            // Speichern der Koordinaten auf der Platte (Abstand vom linken Rand und oberen Rand) der linken, oberen Ecke des Jobs.
            job.x = currentX;
            job.y = currentY;
            currentX += job.width;
            shelfHeight = Math.max(shelfHeight, job.height);  // aktualisiere die Höhe des "Regals"
        } else {
            // Die neue Zeile wird dort angefangen, wo der höchste, bereits platzierte Job, endet.
            currentY += shelfHeight;
            // passt der Job von der Höhe her überhaupt in das neue "Regal"?
            if (currentY + job.height > height) {
                return false;
            }
            currentX = 0;
            shelfHeight = job.height;
            job.x = currentX;
            job.y = currentY;
            currentX += job.width;
        }

        job.placedOn = this;
        jobs.add(job);
        return true;
    }

}
