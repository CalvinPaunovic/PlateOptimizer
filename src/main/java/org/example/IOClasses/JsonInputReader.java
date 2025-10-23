package org.example.IOClasses;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonInputReader {

    // Hilfsklasse zum Zurückgeben von Jobs und Platte zusammen
    public static class InputData {
        public List<Job> jobs;
        public Plate plate;

        // Konstruktor
        public InputData(List<Job> jobs, Plate plate) {
            this.jobs = jobs;
            this.plate = plate;
        }
    }

    /**
     * Liest eine JSON-Datei und extrahiert Jobs und Plate-Informationen.
     * 
     * Erwartetes JSON-Format:
     * {
     *   "jobs": [
     *     { "width": 100, "height": 200, "quantity": 2 },
     *     { "width": 150, "height": 100, "quantity": 1 }
     *   ],
     *   "plate": { "width": 1000, "height": 2000, "name": "A1 Platte" }
     * }
     * 
     * @param filePath Der Pfad zur JSON-Datei
     * @return InputData mit den gelesenen Jobs und der Platte
     * @throws IOException wenn die Datei nicht gelesen werden kann
     */
    public static InputData readFromJson(String filePath) throws IOException {
        Gson gson = new Gson();
        JsonObject jsonObject;
        
        try (FileReader reader = new FileReader(filePath)) {
            jsonObject = gson.fromJson(reader, JsonObject.class);
        }

        // Jobs einlesen
        List<Job> jobs = new ArrayList<>();
        JsonArray jobsArray = jsonObject.getAsJsonArray("jobs");
        int jobId = 1;
        
        for (JsonElement element : jobsArray) {
            JsonObject jobObj = element.getAsJsonObject();
            double width = jobObj.get("width").getAsDouble();
            double height = jobObj.get("height").getAsDouble();
            int quantity = jobObj.get("quantity").getAsInt();
            
            // Jedem Job eine ID zuweisen und neue Job-Objekte erstellen
            for (int i = 0; i < quantity; i++) {
                jobs.add(new Job(jobId++, width, height));
            }
        }

        // Platte einlesen
        JsonObject plateObj = jsonObject.getAsJsonObject("plate");
        String plateName = plateObj.get("name").getAsString();
        double plateWidth = plateObj.get("width").getAsDouble();
        double plateHeight = plateObj.get("height").getAsDouble();
        // Neues Plate-Objekt erstellen
        Plate plate = new Plate(plateName, plateWidth, plateHeight, "1");

        return new InputData(jobs, plate);
    }

}

