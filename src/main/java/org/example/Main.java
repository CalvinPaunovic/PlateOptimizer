package org.example;

import java.util.*;

import org.example.Algorithms.Controller;
import org.example.DataClasses.Job;
import org.example.Provider.JobListProvider;


public class Main {

    public static final boolean rotateJobs = true;
    public static final boolean sortJobs = true;
    
    public static final int KERF_WIDTH = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        JobListProvider.NamedJobList selection = getUserJobListChoiceWithScanner(scanner);
        List<Job> originalJobs = selection.jobs;
        org.example.DataClasses.Plate standardPlate = org.example.Provider.PlateProvider.getA1Plate();
        Controller.run_MaxRectBF_MultiPlate_Unlimited(originalJobs, standardPlate, sortJobs);
        scanner.close();
    }


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

}