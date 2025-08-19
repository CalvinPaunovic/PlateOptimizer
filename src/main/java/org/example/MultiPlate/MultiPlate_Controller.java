package org.example.MultiPlate;

import org.example.DataClasses.Job;
import org.example.DataClasses.MultiPlate_DataClasses;
import org.example.DataClasses.Plate;
import org.example.HelperClasses.JobUtils;

import java.util.*;

public class MultiPlate_Controller {

    /**
     * Führt den MultiPlate-Algorithmus aus.
     * Erwartet jetzt eine Liste von Platten.
     */
    public static void run_MaxRectBF_MultiPlate(List<Job> originalJobs, List<Plate> plateInfos, boolean sortJobs) {
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsBySizeDescending(jobs);

        // Ausgabe der Platteninformationen auf der Konsole
        System.out.println("\n=== Plattenliste ===");
        for (Plate plate : plateInfos) {
            System.out.println("Name=" + plate.name + ", Breite=" + plate.width + ", Höhe=" + plate.height);
        }

        // Jeden Job einzeln auf einer Platte platzieren
        MultiPlate_Algorithm firstPlateAlgorithm = new MultiPlate_Algorithm(plateInfos.subList(0, 1)); // nur erste Platte initial
        for (Job job : jobs) {
            firstPlateAlgorithm.placeJob(job, plateInfos.get(0).plateId);
        }
        // Ausgabe der Pfad-Details für die erste Platte
        System.out.println("\n=== Pfad-Details (Platte 1) ===");
        List<MultiPlate_DataClasses> firstPlatePaths = firstPlateAlgorithm.getPathsAndFailedJobsOverviewData();
        printPathDetails(firstPlatePaths);
        List<MultiPlate_DataClasses> allPathsForSummary = new ArrayList<>(firstPlatePaths);
        if (plateInfos.size() >= 2) {
            Plate secondPlateTemplate = plateInfos.get(1);
            System.out.println("\n=== Zweitplatten pro Pfad (Failed + Missing Jobs) ===");
            for (MultiPlate_DataClasses path : firstPlatePaths) {
                // Sammle gesetzte Job-IDs
                Set<Integer> placed = new HashSet<>();
                for (Job pj : path.plate.jobs) placed.add(pj.id);
                // Sammle fehlende Jobs (alle Originale, die nicht platziert wurden)
                LinkedHashSet<Integer> toPlaceIds = new LinkedHashSet<>();
                for (Job original : jobs) if (!placed.contains(original.id)) toPlaceIds.add(original.id);
                // Ergänze explizite failedJobs (falls vorhanden)
                if (path.failedJobs != null) toPlaceIds.addAll(path.failedJobs);
                if (toPlaceIds.isEmpty()) continue; // Pfad vollständig
                System.out.println("\n--- Zweite Platte für Pfad " + path.pathId + " (#Missing+Failed=" + toPlaceIds.size() + ") -> Platte: " + secondPlateTemplate.name + " ---");
                MultiPlate_Algorithm secondPlateAlgo = new MultiPlate_Algorithm(java.util.Arrays.asList(secondPlateTemplate), path.pathId, true);
                for (Integer jid : toPlaceIds) {
                    // Hole Original-Job-Maße
                    for (Job orig : jobs) if (orig.id == jid) { secondPlateAlgo.placeJob(new Job(orig.id, orig.width, orig.height), secondPlateTemplate.plateId); break; }
                }
                List<MultiPlate_DataClasses> secondPaths = secondPlateAlgo.getPathsAndFailedJobsOverviewData();
                printPathDetails(secondPaths);
                allPathsForSummary.addAll(secondPaths);
            }
        }
        printStrategyMatrix(allPathsForSummary);
    }

    // NEU: Tabellarische Übersicht pro Pfad und Job-ID (inkl. erzeugte Kinderpfade)
    private static void printStrategyMatrix(List<MultiPlate_DataClasses> allPaths) {
        System.out.println("\n=== Strategie-Code Matrix (Pfad x Job) ===");
        // Alle Job-IDs sammeln
        Set<Integer> jobIdSet = new HashSet<>();
        for (MultiPlate_DataClasses p : allPaths) {
            for (Job j : p.plate.jobs) jobIdSet.add(j.id);
            if (p.failedJobs != null) jobIdSet.addAll(p.failedJobs);
            if (p.splitFromJobId != -1) jobIdSet.add(p.splitFromJobId); // sicherstellen, dass Split-Job sichtbar
        }
        List<Integer> jobIds = new ArrayList<>(jobIdSet);
        Collections.sort(jobIds);

        // Mapping: parentPathId -> (jobId -> List(childPathIds))
        Map<String, Map<Integer, List<String>>> splitMap = new HashMap<>();
        Map<String, MultiPlate_DataClasses> pathMap = new HashMap<>();
        for (MultiPlate_DataClasses p : allPaths) {
            pathMap.put(p.pathId, p);
            if (p.splitFromPathId != null && p.splitFromJobId != -1)
                splitMap.computeIfAbsent(p.splitFromPathId, k -> new HashMap<>())
                        .computeIfAbsent(p.splitFromJobId, k -> new ArrayList<>())
                        .add(p.pathId);
        }

        int cellWidth = 11; // etwas breiter für -(Parent)
        StringBuilder header = new StringBuilder();
        header.append(String.format("%-12s", "Pfad"));
        header.append(String.format("%-8s", "Failed"));
        for (Integer id : jobIds) header.append(String.format(("J"+id).length()<cellWidth?"%-"+cellWidth+"s":"%s", "J"+id));
        System.out.println(header);
        System.out.println(repeat('-', header.length()));

        for (MultiPlate_DataClasses p : allPaths) {
            StringBuilder row = new StringBuilder();
            row.append(String.format("%-12s", p.pathId));
            row.append(String.format("%-8s", p.failedJobs==null?0:p.failedJobs.size()));
            for (Integer jid : jobIds) row.append(formatJobCell(p, jid, splitMap, pathMap, cellWidth));
            System.out.println(row);
        }
        System.out.println("Legende:\n" +
            "   PfadID(s) = Abspaltung(en) erfolgte hier\n" +
            "   -(Parent) = Dieser Kinderpfad konnte den Job trotz Strategie-Wechsel (inkl. versuchter Rotation) nicht platzieren\n" +
            "   x = Dieser ältere Job konnte von seinem Elternpfad nicht platziert werden.");
            
    }

    private static String formatJobCell(MultiPlate_DataClasses path, int jobId, Map<String, Map<Integer, List<String>>> splitMap, Map<String, MultiPlate_DataClasses> pathMap, int width) {
        // platziert?
        for (Job j : path.plate.jobs) if (j.id == jobId) { String val = "?"; if ("FullWidth".equals(j.splittingMethod)) val = "W"; else if ("FullHeight".equals(j.splittingMethod)) val = "H"; return String.format("%-"+width+"s", val); }
        // Abspaltung(en)?
        List<String> childs = splitMap.getOrDefault(path.pathId, Collections.emptyMap()).get(jobId);
        if (childs != null && !childs.isEmpty()) { String joined = String.join(",", childs); if (joined.length()>width) joined = joined.substring(0,width-1)+"…"; return String.format("%-"+width+"s", joined); }
        // Failed? (zeige Parent falls Kinderpfad)
        if (path.failedJobs != null && path.failedJobs.contains(jobId)) { String mark = path.splitFromPathId != null ? "-(" + path.splitFromPathId + ")" : "-"; if (mark.length()>width) mark = mark.substring(0,width-1)+"…"; return String.format("%-"+width+"s", mark); }
        // Unplatziert & nicht failed: unterscheide ob anderer Sibling denselben Job platziert hat
        if (path.splitFromPathId != null) {
            String parentId = path.splitFromPathId;
            // Nur frühere Jobs vor dem Split-Zeitpunkt betrachten
            if (jobId < path.splitFromJobId) {
                // Prüfe Siblings
                boolean siblingPlaced = false;
                for (MultiPlate_DataClasses candidate : pathMap.values()) {
                    if (candidate.splitFromPathId != null && candidate.splitFromPathId.equals(parentId)) {
                        for (Job sj : candidate.plate.jobs) if (sj.id == jobId) { siblingPlaced = true; break; }
                        if (siblingPlaced) break;
                    }
                }
                if (siblingPlaced) return String.format("%-"+width+"s", "x"); // Divergenz-Marker
            }
        }
        return String.format("%-"+width+"s", "");
    }

    private static String repeat(char c, int len) { StringBuilder sb = new StringBuilder(len); for(int i=0;i<len;i++) sb.append(c); return sb.toString(); }

    public static void printPathDetails(List<MultiPlate_DataClasses> pathDetails) {
        // Kompakte Ausgabe: eine Zeile pro Pfad
        for (MultiPlate_DataClasses info : pathDetails) {
            String strat = info.strategy == MultiPlate_DataClasses.Strategy.FULL_HEIGHT ? "H" : "W";
            String parent = info.splitFromPathId == null ? "-" : info.splitFromPathId;
            String splitJob = info.splitFromJobId == -1 ? "-" : String.valueOf(info.splitFromJobId);
            StringBuilder jobs = new StringBuilder();
            jobs.append("[");
            for (int i=0;i<info.plate.jobs.size();i++) { if (i>0) jobs.append(","); jobs.append(info.plate.jobs.get(i).id); }
            jobs.append("]");
            String failed = info.failedJobs==null||info.failedJobs.isEmpty()?"[]":info.failedJobs.toString();
            System.out.println("Pfad " + info.pathId + "\tStrat=" + strat + "\tParent=" + parent + "\tSplitJob=" + splitJob + "\tJobs=" + jobs + "\tFailed=" + failed);
        }
        System.out.println();
    }
}