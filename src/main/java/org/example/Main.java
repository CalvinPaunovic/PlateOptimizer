package org.example;

import java.util.*;
import java.io.IOException;

import org.example.Algorithm.Controller;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.IOClasses.JsonInputReader;


public class Main {

    public static final boolean rotateJobs = true;
    public static final boolean sortJobs = true;

    public static final int KERF_WIDTH = 0;
    
    public static void main(String[] args) throws IOException {
        
        List<Job> originalJobs;
        Plate standardPlate;
        
        // Über PlateProvider und JobListProvider
        /*
        JobListProvider.NamedJobList selection = getUserJobListChoiceWithScanner(scanner);
        originalJobs = selection.jobs;
        standardPlate = PlateProvider.getA1Plate();
        */
        
        // Über JSONInputReader
        String jsonPath = "src/main/IOFiles/input.json";
        JsonInputReader.InputData inputData = JsonInputReader.readFromJson(jsonPath);
        originalJobs = inputData.jobs;
        standardPlate = inputData.plate;

        // Algorithmus ausführen
        Controller.run_MaxRectBF_MultiPlate_Unlimited(originalJobs, standardPlate, sortJobs);
    }


    /* 
    private static JobListProvider.NamedJobList getUserJobListChoiceWithScanner(Scanner scanner) {
        java.util.List<JobListProvider.NamedJobList> lists = JobListProvider.getAllListsInMenuOrder();
        System.out.println("Welche Jobliste möchten Sie verwenden?");
        for (JobListProvider.NamedJobList l : lists) {
            System.out.printf("%d = %s\n   -> %s\n", l.id, l.name, (l.description==null?"":l.description));
        }
        System.out.print("Bitte wählen: ");
        String input = scanner.nextLine().trim();
        int id = Integer.parseInt(input);
        for (JobListProvider.NamedJobList l : lists) if (l.id == id) return l;
        return lists.get(0);
    }
    */

}