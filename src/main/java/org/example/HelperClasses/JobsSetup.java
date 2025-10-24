package org.example.HelperClasses;

import java.util.*;

import org.example.DataClasses.Job;

public class JobsSetup {

    public static void sortJobsBySizeDescending(List<Job> jobs) {
        jobs.sort((j1, j2) -> Double.compare(j2.width * j2.height, j1.width * j1.height));

    }

    public static void sortJobsByLargestEdgeDescending(List<Job> jobs) {
        jobs.sort((j1, j2) -> Double.compare(Math.max(j2.width, j2.height), Math.max(j1.width, j1.height)));
    }

    public static List<Job> createJobCopies(List<Job> originalJobs) {
        List<Job> copies = new ArrayList<>();
        for (Job original : originalJobs) {
            double widthWithKerf = original.width;
            double heightWithKerf = original.height;

            Job copy = new Job(original.id, widthWithKerf, heightWithKerf);
            copy.originalWidth = original.width;
            copy.originalHeight = original.height;
            copies.add(copy);
        }
        System.out.println();
        return copies;
    }
}
