package org.example.HelperClasses;

import java.util.*;

import org.example.Main;
import org.example.DataClasses.Job;

public class JobUtils {

    
    public static void sortJobsBySizeDescending(List<Job> jobs) {
        jobs.sort((j1, j2) -> Double.compare(j2.width * j2.height, j1.width * j1.height));
        if (Main.DEBUG) {
            System.out.println("Jobs sortiert nach Fläche absteigend:");
            for (int i = 0; i < jobs.size(); i++) {
                Job job = jobs.get(i);
                double area = job.width * job.height;
                System.out.printf("%d: Job ID=%d, Größe=%.2fx%.2f, Fläche=%.2f\n", i + 1, job.id, job.width, job.height, area);
            }
        }
    }

    public static void sortJobsByLargestEdgeDescending(List<Job> jobs) {
        jobs.sort((j1, j2) -> Double.compare(
            Math.max(j2.width, j2.height), Math.max(j1.width, j1.height)
        ));
        if (Main.DEBUG) {
            System.out.println("Jobs sortiert nach größter Kante absteigend:");
            for (int i = 0; i < jobs.size(); i++) {
                Job job = jobs.get(i);
                double maxEdge = Math.max(job.width, job.height);
                System.out.printf("%d: Job ID=%d, Größe=%.2fx%.2f, größte Kante=%.2f\n", i + 1, job.id, job.width, job.height, maxEdge);
            }
        }
    }

    public static List<Job> createJobCopies(List<Job> originalJobs) {
        List<Job> copies = new ArrayList<>();
        for (Job original : originalJobs) {
            double widthWithKerf = original.width + Main.KERF_WIDTH;
            double heightWithKerf = original.height + Main.KERF_WIDTH;

            Job copy = new Job(original.id, widthWithKerf, heightWithKerf);
            copy.originalWidth = original.width;
            copy.originalHeight = original.height;
            copies.add(copy);
        }
        System.out.println();
        return copies;
    }
}
