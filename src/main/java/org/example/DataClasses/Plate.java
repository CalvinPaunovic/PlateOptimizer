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
    public Integer parentPathIndex = null; // Index des Elternpfads (falls vorhanden), sonst null

    public Plate(String name, double width, double height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

}
