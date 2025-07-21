package org.example.Provider;

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

    // Methode zur Auswahl der Platte anhand eines Namens
    public static NamedPlate getPlateByName(String name) {
        if ("Standardplatte".equalsIgnoreCase(name)) {
            return getStandardPlate();
        } else if ("Großplatte".equalsIgnoreCase(name) || "Grossplatte".equalsIgnoreCase(name)) {
            return getLargePlate();
        }
        return null;
    }
}
