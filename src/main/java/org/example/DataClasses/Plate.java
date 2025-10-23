package org.example.DataClasses;

import java.util.ArrayList;
import java.util.List;

public class Plate {

    public String name;
    public double width;
    public double height;
    public String plateId;
    
    public List<Job> jobs = new ArrayList<>();
    public Integer parentPathIndex = null;

    /*
    public Plate(String name, double width, double height) {
        this(name, width, height, null);
    }
    */

    public Plate(String name, double width, double height, String plateId) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.plateId = plateId;
    }

}
