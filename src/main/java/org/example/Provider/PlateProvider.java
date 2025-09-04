package org.example.Provider;

import java.util.Arrays;
import java.util.List;
import org.example.DataClasses.Plate;

public class PlateProvider {

    public static Plate getStandardPlate() {
        return new Plate("Standardplatte", 963.0, 650.0, "1");
    }

    public static Plate getLargePlate() {
        return new Plate("Großplatte", 1200.0, 800.0, "0");
    }

    public static List<Plate> getPlateList() {
        return Arrays.asList(getStandardPlate(), getLargePlate());
    }

    public static List<Plate> getTwoStandardPlates() {
        return Arrays.asList(
            new Plate("Standardplatte 1 (963 x 650)", 963.0, 650.0, "1"),
            new Plate("Standardplatte 2 (963 x 650)", 963.0, 650.0, "2")
        );
    }

    public static List<Plate> getThreeStandardPlates() { return createStandardPlates(3); }
    public static List<Plate> getFourStandardPlates() { return createStandardPlates(4); }
    public static List<Plate> getNStandardPlates(int n) { return createStandardPlates(n); }

    private static java.util.List<Plate> createStandardPlates(int count) {
        java.util.List<Plate> list = new java.util.ArrayList<>(Math.max(0, count));
        for (int i = 1; i <= count; i++) {
            list.add(new Plate("Standardplatte " + i + " (963 x 650)", 963.0, 650.0, String.valueOf(i)));
        }
        return list;
    }
}
