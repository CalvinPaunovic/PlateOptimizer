package org.example.Provider;

import java.util.Arrays;
import java.util.List;

public class PlateProvider {

    public static class NamedPlate {
        public final String name;
        public final double width;
        public final double height;

        public NamedPlate(String name, double width, double height) {
            this.name = name;
            this.width = width;
            this.height = height;
        }
    }

    public static NamedPlate getStandardPlate() {
        return new NamedPlate("Standardplatte", 963.0, 650.0);
    }

    // Zweite Plattenoption hinzufügen
    public static NamedPlate getLargePlate() {
        return new NamedPlate("Großplatte", 1200.0, 800.0);
    }

    // Gibt eine Liste mit allen verfügbaren Platten zurück
    public static List<NamedPlate> getPlateList() {
        return Arrays.asList(getStandardPlate(), getLargePlate());
    }

    // Gibt explizit eine Liste mit zwei Standardplatten zurück
    public static List<NamedPlate> getTwoStandardPlates() {
        return Arrays.asList(
            new NamedPlate("Standardplatte 1", 963.0, 650.0),
            new NamedPlate("Standardplatte 2", 963.0, 650.0)
        );
    }
}
