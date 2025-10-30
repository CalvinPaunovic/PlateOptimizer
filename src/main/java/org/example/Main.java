package org.example;

import java.util.*;
import java.io.IOException;
import java.nio.file.Path;

import org.example.Algorithm.Controller;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.IOClasses.JsonInputReader;
import org.example.HelperClasses.JobsSetup;


public class Main {


    // Datatype 'Path' to get the platform-independent file paths
    // 'System.getProperty("user.dir")' contains the Path up to project folder "PlateOptimizer"
    public static final Path BASE_DIRECTORY = Path.of(System.getProperty("user.dir"));

    public static final Path IO_DIRECTORY = BASE_DIRECTORY.resolve("Frontend_and_Server/IOFiles");
    public static final Path IMAGES_DIRECTORY = BASE_DIRECTORY.resolve("Frontend_and_Server/IOFiles/Images");

    public static final Path OUTPUT_FILE = IO_DIRECTORY.resolve("output.json");
    public static final Path INPUT_FILE = IO_DIRECTORY.resolve("input.json");
    public static final Path OUTPUT_SQLITE_FILE = IO_DIRECTORY.resolve("leftover_plates.sqlite");

    public record SortedJobsVariant(String label, List<Job> jobs) {}


    public static void main(String[] args) throws IOException {
        
        // Read input data from JSON
        JsonInputReader.InputData inputData = JsonInputReader.readFromJson(Main.INPUT_FILE.toString());
        List <Job> originalJobs = inputData.jobs;
        Plate originalPlate = inputData.plate;


        // Sort Jobs by Area and Largest Edge, store in List<List<Job>>
        List<SortedJobsVariant> sortedJobsList = new ArrayList<>();
        sortedJobsList.add(new SortedJobsVariant("area", JobsSetup.sortJobsBySizeDescending(originalJobs)));
        sortedJobsList.add(new SortedJobsVariant("edge", JobsSetup.sortJobsByLargestEdgeDescending(originalJobs)));
        // ... More sorting strategies can be added here, if implemented in JobsSetup-class


        // Execute Algorithm
        Controller.runAlgorithm(sortedJobsList, originalPlate);
    }

}