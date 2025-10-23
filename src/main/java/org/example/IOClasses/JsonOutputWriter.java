package org.example.IOClasses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.Algorithm.CutLineCalculator;
import org.example.DataClasses.Plate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonOutputWriter {

    // Einfache Datenklassen für JSON
    public static class Cut {
        public int id;
        public String type; // "V" oder "H"
        public double x1, y1, x2, y2;
    }

    public static class LeftoverPlate {
        public int id;
        public double width, height;
        public double x0, x1, y0, y1;
        public int jobCount;
    }

    public static class PlateData {
        public String name;
        public double width, height;
        public List<Cut> cuts = new ArrayList<>();
        public List<LeftoverPlate> leftoverPlates = new ArrayList<>();
    }

    public static class Output {
        public String solutionName;
        public String timestamp;
        public List<PlateData> plates = new ArrayList<>();
    }

    /**
     * Speichert Platten als JSON
     */
    public static void writePlatesToJson(String solutionName, List<Plate> plates, String filePath) throws IOException {
        // Output-Objekt erstellen
        Output output = new Output();
        output.solutionName = solutionName;
        output.timestamp = java.time.LocalDateTime.now().toString();

        // Jede Platte verarbeiten
        for (Plate plate : plates) {
            PlateData plateData = new PlateData();
            plateData.name = plate.name;
            plateData.width = plate.width;
            plateData.height = plate.height;

            // Schnitte hinzufügen
            List<CutLineCalculator.CutLine> cuts = CutLineCalculator.calculateAllFullCuts(plate);
            if (cuts != null) {
                for (CutLineCalculator.CutLine c : cuts) {
                    Cut cut = new Cut();
                    cut.id = c.id;
                    if (c.vertical) {
                        cut.type = "V";
                        cut.x1 = c.coord;
                        cut.y1 = c.start;
                        cut.x2 = c.coord;
                        cut.y2 = c.end;
                    } else {
                        cut.type = "H";
                        cut.x1 = c.start;
                        cut.y1 = c.coord;
                        cut.x2 = c.end;
                        cut.y2 = c.coord;
                    }
                    plateData.cuts.add(cut);
                }
            }

            // Restplatten hinzufügen
            List<CutLineCalculator.ResidualPlate> residuals = CutLineCalculator.calculateResidualPlates(plate);
            if (residuals != null) {
                int id = 1;
                for (CutLineCalculator.ResidualPlate r : residuals) {
                    LeftoverPlate leftover = new LeftoverPlate();
                    leftover.id = id++;
                    leftover.width = r.width();
                    leftover.height = r.height();
                    leftover.x0 = r.x0;
                    leftover.x1 = r.x1;
                    leftover.y0 = r.y0;
                    leftover.y1 = r.y1;
                    leftover.jobCount = r.jobCount;
                    plateData.leftoverPlates.add(leftover);
                }
            }

            output.plates.add(plateData);
        }

        // In Datei schreiben
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(output, writer);
        }
    }
}
