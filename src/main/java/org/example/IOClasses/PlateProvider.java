package org.example.IOClasses;

import org.example.DataClasses.Plate;

public class PlateProvider {

    public static Plate getStandardPlate() {
        return new Plate("Standardplatte", 963.0, 650.0, "1");
    }

    public static Plate getA1Plate() {
        return new Plate("A1 Platte", 841.0, 594.0, "1");
    }


}
