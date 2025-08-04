package org.example.Provider;

import java.util.Arrays;
import java.util.List;
import org.example.DataClasses.Plate;

public class PlateProvider {

    public static Plate getStandardPlate() {
        return new Plate("Standardplatte", 963.0, 650.0, "1");
    }

    public static Plate getLargePlate() {
        return new Plate("Großplatte", 1200.0, 800.0, "1");
    }

    // Gibt eine Liste mit allen verfügbaren Platten zurück
    public static List<Plate> getPlateList() {
        return Arrays.asList(getStandardPlate(), getLargePlate());
    }

    public static List<Plate> getTwoStandardPlates() {
        return Arrays.asList(
            new Plate("Standardplatte 1", 963.0, 650.0, "1"),
            new Plate("Standardplatte 2", 963.0, 650.0, "2")
        );
    }
}
