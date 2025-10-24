package org.example;

import java.util.*;
import java.io.IOException;
import java.nio.file.Path;

import org.example.Algorithm.Controller;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.IOClasses.JsonInputReader;


public class Main {

    // Datatype 'Path' to get the platform-independent file paths
    // 'System.getProperty("user.dir")' containsPath up to project folder "PlateOptimizer"
    public static final Path BASE_DIRECTORY = Path.of(System.getProperty("user.dir"));

    public static final Path IO_DIRECTORY = BASE_DIRECTORY.resolve("IOFiles");
    public static final Path IMAGES_DIRECTORY = BASE_DIRECTORY.resolve("IOFiles/Images");

    public static final Path OUTPUT_FILE = IO_DIRECTORY.resolve("output.json");
    public static final Path INPUT_FILE = IO_DIRECTORY.resolve("input.json");
    public static final Path OUTPUT_SQLITE_FILE = IO_DIRECTORY.resolve("leftover_plates.sqlite");


    public static void main(String[] args) throws IOException {


        List<Job> originalJobs;
        Plate originPlate;
        
        // With JSONInputReader
        JsonInputReader.InputData inputData = JsonInputReader.readFromJson(Main.INPUT_FILE.toString());
        originalJobs = inputData.jobs;
        originPlate = inputData.plate;

        // Execute Algorithm
        Controller.runAlgorithm(originalJobs, originPlate);
    }

}