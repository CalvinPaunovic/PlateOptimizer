package org.example.MultiPlateIndividual;

import org.example.DataClasses.Job;
import org.example.DataClasses.MultiPlate_DataClasses;
import org.example.DataClasses.Plate;
import org.example.HelperClasses.JobUtils;
import org.example.Visualizer.BenchmarkVisualizer;

import java.util.*;

public class MultiPlateIndividual_Controller {

    /**
     * 
     * Informationen zur Nutzung von mehreren Platten
     * Für die Benutzung von 3 Platten ist mehr RAM erfoderlich (z.B. 8 GB)
     * cd C:\Users\cpaun\VisualStudioProjects\PlateOptimizer
     * java -Xmx8G -cp target\classes org.example.Main
     *
     */


    private static final String[] VISUALIZE_BASE_PATH_NUMBERS = {"1", "1.1", "1.2","1.3", "1.1.1"};

    private static boolean VISUALIZE_INTERMEDIATE_STEPS = false;  // Zwischenschritte für in die VISUALIZE_BASE_PATH_NUMBERS angegeben Pfade visualisieren

    private static final boolean ENABLE_CONSOLE_OUTPUT = false;  // Strategie-Tabelle auf Konsole ausgeben

    private static final boolean ENABLE_CONSOLE_OUTPUT_CUTS = true;  // Schnittkoordinaten auf Konsole ausgeben

    // Setter/Getter damit andere Klassen die Zwischenschritt-Visualisierung zur Laufzeit ein-/ausschalten können
    public static void setVisualizeIntermediateSteps(boolean enabled) { VISUALIZE_INTERMEDIATE_STEPS = enabled; }
    public static boolean isVisualizeIntermediateSteps() { return VISUALIZE_INTERMEDIATE_STEPS; }

    /**
     * Abschnitt: Multi-Plate Hauptlauf (Modus 7)
     * Wofür: Führt die mehrstufige Pfad-Erzeugung über mehrere Platten aus, 
     *        zeigt Pfadmatrix und optionale Visualisierung der selektierten Pfade.
     * Aufruf: Main.runMode(...), wenn der Nutzer Modus "7" wählt.
     * Ablauf:
     *  1) Kopiere/Sortiere Jobs (derzeit Fläche desc), platziere sequentiell auf Platte#1.
     *  2) Erzeuge Kinderpfade für fehlende Jobs auf weiteren Platten (2..N) im hierarchischen Modus.
     *  3) Drucke Strategie-Matrix; visualisiere ausgewählte Pfade und ggf. deren Steps.
     * Hinweis: Hier wird kein Benchmark-Fenster geöffnet (nur in BenchmarkOnly).
     * Führt den MultiPlate-Algorithmus aus.
     * Erwartet jetzt eine Liste von Platten.
     */
    public static void run_MaxRectBF_MultiPlate(List<Job> originalJobs, List<Plate> plateInfos, boolean sortJobs) {
        if (plateInfos == null || plateInfos.isEmpty() || originalJobs == null || originalJobs.isEmpty()) return;

        // Platte 1: alle Pfade erzeugen (vollständige Jobliste)
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsBySizeDescending(jobs);
        MultiPlateIndividual_Algorithm firstPlateAlgorithm = new MultiPlateIndividual_Algorithm(plateInfos.subList(0, 1));
        for (Job job : jobs) firstPlateAlgorithm.placeJob(job, plateInfos.get(0).plateId);
        List<MultiPlate_DataClasses> firstPlatePaths = firstPlateAlgorithm.getPathsAndFailedJobsOverviewData();

        // Gruppen der nicht platzierten Jobs pro Pfad bilden und nach Abschluss von Platte 1 ausgeben
        Set<Integer> allJobIds = new LinkedHashSet<>(); for (Job j : originalJobs) allJobIds.add(j.id);
        List<JobSetGroup> groups = buildUnplacedJobGroups(firstPlatePaths, allJobIds);
        printJobSetGroups(groups, 1);

        // NEU: Benchmark-Fenster für Platte 1 (Gesamtlauf)
        {
            List<BenchmarkVisualizer.BenchmarkResult> resP1 = buildBenchmarkResults(firstPlatePaths, jobs, java.util.Arrays.asList(plateInfos.get(0)));
            for (BenchmarkVisualizer.BenchmarkResult br : resP1) br.sortLabel = "Platte 1";
            BenchmarkVisualizer.showBenchmarkResults(resP1, "Platte 1 - Gesamtlauf");
        }

        // Sammle Pfade für Visualisierung/Matrix
        List<MultiPlate_DataClasses> allPathsForSummary = new ArrayList<>(firstPlatePaths);

        // Allgemeine Verarbeitung für weitere Platten (2..N)
        List<JobSetGroup> currentGroups = groups;
        for (int plateIdx = 1; plateIdx < plateInfos.size(); plateIdx++) {
            Plate currentPlate = plateInfos.get(plateIdx);
            Map<String, JobSetGroup> aggregatedForNext = new LinkedHashMap<>();

            for (JobSetGroup g : currentGroups) {
                if (g.jobIds.isEmpty()) continue;
                List<Job> subset = getJobsSubsetForIds(originalJobs, g.jobIds);
                if (sortJobs) JobUtils.sortJobsBySizeDescending(subset);
                MultiPlateIndividual_Algorithm algo = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(currentPlate));
                for (Job j : subset) algo.placeJob(j, currentPlate.plateId);
                List<MultiPlate_DataClasses> pathsK = algo.getPathsAndFailedJobsOverviewData();
                allPathsForSummary.addAll(pathsK);

                // Benchmark-Fenster pro Job-Set für aktuelle Platte
                List<Integer> idsSorted = new ArrayList<>(g.jobIds); Collections.sort(idsSorted);
                String setLabel = idsSorted.toString();
                List<BenchmarkVisualizer.BenchmarkResult> resSet = buildBenchmarkResults(pathsK, subset, java.util.Arrays.asList(currentPlate));
                for (BenchmarkVisualizer.BenchmarkResult br : resSet) { br.sortLabel = "Set " + setLabel; br.jobSetLabel = setLabel; }
                BenchmarkVisualizer.showBenchmarkResults(resSet, "Platte " + (plateIdx + 1) + " - Job-Set: " + setLabel);

                // Gruppenbildung für nächste Platte vorbereiten
                List<JobSetGroup> sub = buildUnplacedJobGroups(pathsK, g.jobIds);
                for (JobSetGroup sg : sub) {
                    if (sg.jobIds.isEmpty()) continue; // leere Sets überspringen
                    List<Integer> idsSortedSub = new ArrayList<>(sg.jobIds); Collections.sort(idsSortedSub);
                    String key = idsSortedSub.toString();
                    JobSetGroup dst = aggregatedForNext.computeIfAbsent(key, k -> new JobSetGroup(new LinkedHashSet<>(sg.jobIds)));
                    dst.pathIds.addAll(sg.pathIds);
                }
            }

            // Print der aggregierten Gruppen für diese Platte
            List<JobSetGroup> nextGroupsList = new ArrayList<>(aggregatedForNext.values());
            printJobSetGroups(nextGroupsList, plateIdx + 1);
            currentGroups = nextGroupsList;
            if (currentGroups.isEmpty()) break; // nichts mehr offen -> abbrechen
        }

        // Ausgabe / Visualisierung
        printStrategyMatrix(allPathsForSummary);
        // Einzelplatten-Visualisierung deaktiviert: nur Benchmarks werden geöffnet
        // visualizeSequentialResult(filterByPlateId(allPathsForSummary, plateInfos.get(0).plateId));
        // if (plateInfos.size() >= 2) visualizeSequentialResult(filterByPlateId(allPathsForSummary, plateInfos.get(1).plateId));
    }

    // Gruppe der unplatzierten Jobs je Pfad (unique Sets)
    private static List<JobSetGroup> buildUnplacedJobGroups(List<MultiPlate_DataClasses> paths, Set<Integer> allJobIds) {
        Map<String, JobSetGroup> map = new LinkedHashMap<>();
        for (MultiPlate_DataClasses p : paths) {
            if (p == null || p.plate == null) continue;
            Set<Integer> placed = new LinkedHashSet<>(); for (Job j : p.plate.jobs) placed.add(j.id);
            Set<Integer> unplaced = new LinkedHashSet<>(); for (Integer id : allJobIds) if (!placed.contains(id)) unplaced.add(id);
            // Schlüssel für das Set erzeugen (stabil sortiert)
            List<Integer> sorted = new ArrayList<>(unplaced); Collections.sort(sorted);
            String key = sorted.toString();
            JobSetGroup g = map.computeIfAbsent(key, k -> new JobSetGroup(new LinkedHashSet<>(unplaced)));
            g.pathIds.add(p.pathId);
        }
        return new ArrayList<>(map.values());
    }

    // Drucke die gefundenen Job-Sets nach Abschluss einer Platte
    private static void printJobSetGroups(List<JobSetGroup> groups, int plateNumber) {
        System.out.println("\n=== Nicht platzierte Job-Sets nach Platte " + plateNumber + " ===");
        if (groups == null || groups.isEmpty()) {
            System.out.println("(keine Gruppen)");
            return;
        }
        int idx = 1;
        for (JobSetGroup g : groups) {
            List<Integer> ids = new ArrayList<>(g.jobIds); Collections.sort(ids);
            System.out.println(String.format("Gruppe %d: Jobs %s | erzeugt von Pfaden: %s", idx++, ids, g.pathIds));
            System.out.println();
        }
    }

    private static List<Job> getJobsSubsetForIds(List<Job> originalJobs, Set<Integer> ids) {
        Map<Integer, Job> byId = new HashMap<>(); for (Job j : originalJobs) byId.put(j.id, j);
        List<Job> out = new ArrayList<>();
        for (Integer id : ids) {
            Job o = byId.get(id);
            if (o != null) out.add(new Job(o.id, o.width, o.height));
        }
        return out;
    }

    // Hilfsstruktur für Gruppenbildung
    private static class JobSetGroup {
        final Set<Integer> jobIds; final List<String> pathIds = new ArrayList<>();
        JobSetGroup(Set<Integer> jobIds) { this.jobIds = jobIds; }
    }


    // Baut die Benchmark-Ergebnisliste
    // Wofür: Aggregiert pro Blattpfad die belegten/gesamten Flächen und erzeugt
    //        BenchmarkResult-Objekte inkl. pro-Platte-Coverage/FreeRects.
    // Nutzung: In run_MaxRectBF_MultiPlate_BenchmarkOnly() für die beiden Sortierläufe
    //          und ggf. für andere Auswertungen.
    private static java.util.List<BenchmarkVisualizer.BenchmarkResult> buildBenchmarkResults(List<MultiPlate_DataClasses> allPaths, List<Job> originalJobs, List<Plate> plateInfos) {
        java.util.List<BenchmarkVisualizer.BenchmarkResult> results = new java.util.ArrayList<>();
        java.util.Map<String, MultiPlate_DataClasses> byId = new java.util.HashMap<>();
        for (MultiPlate_DataClasses p : allPaths) byId.put(p.pathId, p);
        int platesTotal = plateInfos == null ? 1 : plateInfos.size();
        int totalJobs = originalJobs == null ? 0 : originalJobs.size();
        java.util.Set<String> parentIds = new java.util.HashSet<>();
        for (MultiPlate_DataClasses p : allPaths) {
            if (p == null || p.pathId == null) continue;
            int dot = p.pathId.lastIndexOf('.');
            if (dot > 0) parentIds.add(p.pathId.substring(0, dot));
        }
        for (MultiPlate_DataClasses p : allPaths) {
            if (p == null || p.plate == null) continue;
            if (parentIds.contains(p.pathId)) continue; // nur Blattpfade
            // Elternkette aufbauen
            java.util.List<MultiPlate_DataClasses> chain = new java.util.ArrayList<>();
            String id = p.pathId; String[] parts = id.split("\\."); StringBuilder acc = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) acc.append('.');
                acc.append(parts[i]);
                MultiPlate_DataClasses node = byId.get(acc.toString());
                if (node != null && node.plate != null) chain.add(node);
            }
            if (chain.isEmpty()) continue;
            // Aggregation
            double usedSum = 0.0, totalSum = 0.0; int placedSum = 0;
            for (MultiPlate_DataClasses node : chain) {
                double used = 0.0; for (Job j : node.plate.jobs) used += (j.width * j.height);
                double total = node.plate.width * node.plate.height;
                usedSum += used; totalSum += total; placedSum += node.plate.jobs.size();
            }
            double covAgg = totalSum > 0 ? (usedSum / totalSum) * 100.0 : 0.0;
            String algoName = "Pfad " + p.pathId;
            BenchmarkVisualizer.BenchmarkResult row = new BenchmarkVisualizer.BenchmarkResult(algoName, p.plate, null, placedSum, covAgg, totalJobs);
            row.platesTotal = platesTotal; row.platesUsed = chain.size();
            // Plate1/2 für Rückwärtskompatibilität
            row.plate1Name = chain.size() >= 1 ? chain.get(0).plate.name : null;
            if (chain.size() >= 1) { double u1 = 0.0; for (Job j : chain.get(0).plate.jobs) u1 += (j.width * j.height); double t1 = chain.get(0).plate.width * chain.get(0).plate.height; row.coveragePlate1 = t1>0?(u1/t1)*100.0:0.0; row.plate1Ref = chain.get(0).plate; row.plate1FreeRects = chain.get(0).freeRects; }
            if (chain.size() >= 2) { row.plate2Name = chain.get(1).plate.name; double u2 = 0.0; for (Job j : chain.get(1).plate.jobs) u2 += (j.width * j.height); double t2 = chain.get(1).plate.width * chain.get(1).plate.height; row.coveragePlate2 = t2>0?(u2/t2)*100.0:0.0; row.plate2Ref = chain.get(1).plate; row.plate2FreeRects = chain.get(1).freeRects; }
            // Dynamische Listen
            row.platesRefs = new java.util.ArrayList<>(); row.platesNames = new java.util.ArrayList<>(); row.platesCoverages = new java.util.ArrayList<>(); row.platesFreeRects = new java.util.ArrayList<>();
            for (MultiPlate_DataClasses node : chain) {
                row.platesRefs.add(node.plate);
                row.platesNames.add(node.plate.name);
                double u = 0.0; for (Job j : node.plate.jobs) u += (j.width * j.height);
                double t = node.plate.width * node.plate.height;
                row.platesCoverages.add(t>0?(u/t)*100.0:0.0);
                row.platesFreeRects.add(node.freeRects);
            }
            results.add(row);
        }
        results.sort((a,b) -> Double.compare(b.coverageRate, a.coverageRate));
        return results;
    }

     // Abschnitt: Strategie-Matrix (Konsole)
     // Wofür: Textuelle Übersicht je Pfad/Job mit Split-Strategien, Fehlschlägen und Abspaltungen.
     // Aufruf: Am Ende des Hauptlaufes (Modus 7). Druck auf System.out (oder ORIGINAL_OUT bei deaktivierter Konsole).
     private static void printStrategyMatrix(List<MultiPlate_DataClasses> allPaths) {
         if (!ENABLE_CONSOLE_OUTPUT) return;
         String title = "\n=== Strategie-Code Matrix (Pfad x Job) ===";
         System.out.println(title);
         // Job-Daten sammeln
         Map<Integer, Job> jobSample = new HashMap<>();
         Set<Integer> jobIdSet = new HashSet<>();
         int maxPlacementCounter = 0;
         for (MultiPlate_DataClasses p : allPaths) {
             for (Job j : p.plate.jobs) { jobIdSet.add(j.id); jobSample.putIfAbsent(j.id, j); }
             if (p.failedJobs != null) jobIdSet.addAll(p.failedJobs);
             if (p.splitFromJobId != -1) jobIdSet.add(p.splitFromJobId);
             if (p.placementCounter > maxPlacementCounter) maxPlacementCounter = p.placementCounter;
         }
         List<Integer> jobIds = new ArrayList<>(jobIdSet);
         jobIds.sort((a,b)->{
             Job ja = jobSample.get(a); Job jb = jobSample.get(b);
             double areaA = ja==null?0:ja.width*ja.height;
             double areaB = jb==null?0:jb.width*jb.height;
             int cmp = Double.compare(areaB, areaA);
             if (cmp!=0) return cmp;
             return Integer.compare(a,b);
         });
         Map<String, Map<Integer, List<String>>> splitMap = new HashMap<>();
         Map<String, MultiPlate_DataClasses> pathMap = new HashMap<>();
         for (MultiPlate_DataClasses p : allPaths) {
             pathMap.put(p.pathId, p);
             if (p.splitFromPathId != null && p.splitFromJobId != -1)
                 splitMap.computeIfAbsent(p.splitFromPathId, k -> new HashMap<>())
                         .computeIfAbsent(p.splitFromJobId, k -> new ArrayList<>())
                         .add(p.pathId);
         }
         int cellWidth = 11;
         // Verwende größere StratSeq-Spalte statt separatem Left-Padding, damit alle Prefix-Felder gleiche Breite haben
         int stratSeqWidth = 20 + 24; // bisher 20 + zusätzlicher Left-Padding 24
         String prefixFormat = "%-12s%-8s%-" + stratSeqWidth + "s"; // Pfad, Failed, StratSeq (erweitert)
         StringBuilder header = new StringBuilder();
         header.append(String.format(prefixFormat, "Pfad", "Failed", "StratSeq"));
         for (Integer id : jobIds) header.append(String.format("%-" + cellWidth + "s", "J" + id));

         StringBuilder order = new StringBuilder();
         order.append(String.format(prefixFormat, "", "", ""));
         for (Integer id : jobIds) order.append(String.format("%-" + cellWidth + "s", id));
         System.out.println(header.toString());
         System.out.println(order.toString());
         System.out.println(repeat('-', header.length()));
         Integer lastSplitMarker = null;
         for (MultiPlate_DataClasses p : allPaths) {
             if (p.splitFromJobId != -1 && (lastSplitMarker == null || !lastSplitMarker.equals(p.splitFromJobId))) {
                 System.out.println(); lastSplitMarker = p.splitFromJobId;
             }
             String stratSeq = buildStrategySequenceForJobIds(p, jobIds);
             // Sicherstellen, dass StratSeq die feste Breite nicht überschreitet
             if (stratSeq == null) stratSeq = "";
             if (stratSeq.length() > stratSeqWidth) stratSeq = stratSeq.substring(0, stratSeqWidth);
             StringBuilder row = new StringBuilder();
             row.append(String.format(prefixFormat, p.pathId, (p.failedJobs==null?0:p.failedJobs.size()), stratSeq));
             for (Integer jid : jobIds) row.append(formatJobCell(p, jid, splitMap, pathMap, cellWidth));
             System.out.println(row.toString());
         }
         System.out.println("Legende:\n" +
             "   PfadID(s) = Abspaltung(en) erfolgte hier\n" +
             "   -(Parent) = Dieser Kinderpfad konnte den Job trotz Strategie-Wechsel (inkl. versuchter Rotation) nicht platzieren\n" +
             "   x = Dieser ältere Job konnte von seinem Elternpfad nicht platziert werden.\n" +
             "   StratSeq = Reihenfolge der verwendeten Split-Strategien (H/W) pro platzierter Job in Pfad-Reihenfolge");
     }

     private static String buildStrategySequenceForJobIds(MultiPlate_DataClasses path, List<Integer> jobIds) {
         // Wofür: Erzeugt eine kompakte Sequenz der Strategien (H/W) pro Job-ID-Spalte für einen Pfad.
         Map<Integer, Job> byId = new HashMap<>();
         for (Job j : path.plate.jobs) byId.put(j.id, j);
         List<String> codes = new ArrayList<>();
         for (Integer id : jobIds) {
             Job j = byId.get(id);
             if (j == null || j.splittingMethod == null) {
                 codes.add("-");
             } else {
                 codes.add("FullHeight".equals(j.splittingMethod)?"H":"FullWidth".equals(j.splittingMethod)?"W":"?");
             }
         }
         return String.join("->", codes);
     }

     private static String formatJobCell(MultiPlate_DataClasses path, int jobId, Map<String, Map<Integer, List<String>>> splitMap, Map<String, MultiPlate_DataClasses> pathMap, int width) {
         // Wofür: Einzelzellenformatierung der Matrix:
         //  - W/H: verwendete Split-Strategie
         //  - PfadIDs: Abspaltungsstelle(n)
         //  - -(...): Job vom Kind nicht platzierbar trotz Strategie-Wechsel
         //  - x: älterer Job wurde in einem Geschwisterpfad platziert
         for (Job j : path.plate.jobs) if (j.id == jobId) { String val = "?"; if ("FullWidth".equals(j.splittingMethod)) val = "W"; else if ("FullHeight".equals(j.splittingMethod)) val = "H"; return String.format("%-"+width+"s", val); }
         List<String> childs = splitMap.getOrDefault(path.pathId, Collections.emptyMap()).get(jobId);
         if (childs != null && !childs.isEmpty()) { String joined = String.join(",", childs); if (joined.length()>width) joined = joined.substring(0,width-1)+"…"; return String.format("%-"+width+"s", joined); }
         if (path.failedJobs != null && path.failedJobs.contains(jobId)) { String mark = path.splitFromPathId != null ? "-(" + path.splitFromPathId + ")" : "-"; if (mark.length()>width) mark = mark.substring(0,width-1)+"…"; return String.format("%-"+width+"s", mark); }
         if (path.splitFromPathId != null) {
             String parentId = path.splitFromPathId;
             if (jobId < path.splitFromJobId) {
                 boolean siblingPlaced = false;
                 for (MultiPlate_DataClasses candidate : pathMap.values()) {
                     if (candidate.splitFromPathId != null && candidate.splitFromPathId.equals(parentId)) {
                         for (Job sj : candidate.plate.jobs) if (sj.id == jobId) { siblingPlaced = true; break; }
                         if (siblingPlaced) break;
                     }
                 }
                 if (siblingPlaced) return String.format("%-"+width+"s", "x");
             }
         }
         return String.format("%-"+width+"s", "");
     }

     // Benchmark-only entrypoint: führe Platzierungen aus, aber zeige nur den Benchmark (keine Matrix, keine Visualisierung)
     // Wofür: Vergleich der beiden Sortierstrategien (Fläche vs. größte Kante) in EINEM Ranking.
     // Aufruf: Main.runMode(...), wenn Modus "8" gewählt wird.
     // Ablauf:
     //  - Lauf 1 (Fläche): Pfade/Ergebnisse erzeugen, labeln (sortLabel="Fläche") und sammeln
     //  - Lauf 2 (Kante):  Pfade/Ergebnisse erzeugen, labeln (sortLabel="Größte Kante") und anhängen
     //  - Übergabe an BenchmarkVisualizer.showBenchmarkResults(..)
     public static void run_MaxRectBF_MultiPlate_BenchmarkOnly(List<Job> originalJobs, List<Plate> plateInfos, boolean sortJobs) {
         if (plateInfos == null || plateInfos.isEmpty()) return;

         for (Plate template : plateInfos) {
             if (template == null) continue;

             // Lauf 1: Fläche
             List<MultiPlate_DataClasses> pathsArea = new ArrayList<>();
             {
                 List<Job> jobs = JobUtils.createJobCopies(originalJobs);
                 JobUtils.sortJobsBySizeDescending(jobs);
                 MultiPlateIndividual_Algorithm algo = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(template));
                 for (Job job : jobs) algo.placeJob(job, template.plateId);
                 pathsArea.addAll(algo.getPathsAndFailedJobsOverviewData());
             }
             List<BenchmarkVisualizer.BenchmarkResult> resArea = buildBenchmarkResults(pathsArea, originalJobs, java.util.Arrays.asList(template));
             for (BenchmarkVisualizer.BenchmarkResult br : resArea) br.sortLabel = "Fläche";

             // Lauf 2: Größte Kante
             List<MultiPlate_DataClasses> pathsEdge = new ArrayList<>();
             {
                 List<Job> jobs = JobUtils.createJobCopies(originalJobs);
                 JobUtils.sortJobsByLargestEdgeDescending(jobs);
                 MultiPlateIndividual_Algorithm algo = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(template));
                 for (Job job : jobs) algo.placeJob(job, template.plateId);
                 pathsEdge.addAll(algo.getPathsAndFailedJobsOverviewData());
             }
             List<BenchmarkVisualizer.BenchmarkResult> resEdge = buildBenchmarkResults(pathsEdge, originalJobs, java.util.Arrays.asList(template));
             for (BenchmarkVisualizer.BenchmarkResult br : resEdge) br.sortLabel = "Größte Kante";

             // Konsolen-Ausgabe der nicht platzierten Job-Sets nach Abschluss dieser Platte (über beide Läufe aggregiert)
             List<MultiPlate_DataClasses> combinedPaths = new ArrayList<>(pathsArea);
             combinedPaths.addAll(pathsEdge);
             Set<Integer> allJobIds = new LinkedHashSet<>(); for (Job j : originalJobs) allJobIds.add(j.id);
             List<JobSetGroup> groups = buildUnplacedJobGroups(combinedPaths, allJobIds);
             String plateLabel = (template.name == null ? template.plateId : template.name);
             printJobSetGroupsForPlateLabel(groups, plateLabel);

             // Kombiniere und zeige separates Fenster für diese Platte
             List<BenchmarkVisualizer.BenchmarkResult> combined = new ArrayList<>();
             combined.addAll(resArea);
             combined.addAll(resEdge);
             String info = "Individuell pro Platte: " + plateLabel;
             BenchmarkVisualizer.showBenchmarkResults(combined, info);
         }
     }

     // Drucke Job-Sets mit frei definiertem Platten-Label (für Benchmark-Only)
     private static void printJobSetGroupsForPlateLabel(List<JobSetGroup> groups, String plateLabel) {
         System.out.println("\n=== Nicht platzierte Job-Sets nach Platte " + plateLabel + " ===");
         if (groups == null || groups.isEmpty()) {
             System.out.println("(keine Gruppen)");
             return;
         }
         int idx = 1;
         for (JobSetGroup g : groups) {
             List<Integer> ids = new ArrayList<>(g.jobIds); Collections.sort(ids);
             System.out.println(String.format("Gruppe %d: Jobs %s | erzeugt von Pfaden: %s", idx++, ids, g.pathIds));
         }
     }

    // ---------------------------------------------
    // Schnittlinien-Berechnung (DTO + Logik)
    // ---------------------------------------------
    /**
     * Repräsentiert eine Schnittlinie (entweder vertikal oder horizontal).
     * coord = x (bei vertical=true) bzw. y (bei vertical=false).
     * start/end definieren den Bereich entlang der Linie (in mm).
     */
    public static class CutLine {
        public final int id;
        public final boolean vertical;
        public final double coord;
        public final double start;
        public final double end;
        public CutLine(int id, boolean vertical, double coord, double start, double end) {
            this.id = id; this.vertical = vertical; this.coord = coord; this.start = start; this.end = end;
        }
    }

    public static java.util.List<CutLine> computeCutLinesForPlate(Plate plate) {
        java.util.List<CutLine> cuts = new java.util.ArrayList<>();
        if (plate == null) return cuts;

        final double EPS = 1e-6;
        // Gruppiere vertikale Schnitte nach x und horizontale nach y, sammle Intervalle
        java.util.Map<Double, java.util.List<double[]>> vertMap = new java.util.TreeMap<>();
        java.util.Map<Double, java.util.List<double[]>> horizMap = new java.util.TreeMap<>();

        for (Job j : plate.jobs) {
            if (j == null || j.placedOn == null) continue;
            double x1 = normalize(j.x);
            double x2 = normalize(j.x + j.width);
            double y1 = normalize(j.y);
            double y2 = normalize(j.y + j.height);

            vertMap.computeIfAbsent(x1, k -> new java.util.ArrayList<>()).add(new double[]{y1, y2});
            vertMap.computeIfAbsent(x2, k -> new java.util.ArrayList<>()).add(new double[]{y1, y2});

            horizMap.computeIfAbsent(y1, k -> new java.util.ArrayList<>()).add(new double[]{x1, x2});
            horizMap.computeIfAbsent(y2, k -> new java.util.ArrayList<>()).add(new double[]{x1, x2});
        }

        // Hilfsfunktion: Intervalle zusammenführen
        java.util.function.BiConsumer<java.util.Map<Double, java.util.List<double[]>>, Boolean> processMap = (map, isVertical) -> {
            for (java.util.Map.Entry<Double, java.util.List<double[]>> entry : map.entrySet()) {
                double coord = entry.getKey();
                java.util.List<double[]> segs = entry.getValue();
                if (segs == null || segs.isEmpty()) continue;
                // sortiere nach Start
                segs.sort((a, b) -> Double.compare(a[0], b[0]));
                java.util.List<double[]> merged = new java.util.ArrayList<>();
                for (double[] s : segs) {
                    if (merged.isEmpty()) { merged.add(new double[]{s[0], s[1]}); continue; }
                    double[] last = merged.get(merged.size() - 1);
                    if (s[0] <= last[1] + EPS) {
                        // überlappend oder angrenzend -> zusammenführen
                        last[1] = Math.max(last[1], s[1]);
                    } else {
                        merged.add(new double[]{s[0], s[1]});
                    }
                }
                // Erzeuge CutLine-Einträge für die zusammengeführten Segmente
                for (double[] m : merged) {
                    if (Math.abs(m[1] - m[0]) < EPS) continue; // vernachlässige Null-Länge
                    if (isVertical) {
                        cuts.add(new CutLine(0, true, coord, m[0], m[1]));
                    } else {
                        cuts.add(new CutLine(0, false, coord, m[0], m[1]));
                    }
                }
            }
        };

        processMap.accept(vertMap, true);
        processMap.accept(horizMap, false);

        // Vergib eindeutige, fortlaufende IDs (1..N)
        // (vorher war hier eine leere Schleife, die unbenutzte Variablen erzeugte)
        // Baue neue Liste mit korrekten IDs in deterministischer Reihenfolge (zuerst vertikale nach x, dann horizontale nach y)
        java.util.List<CutLine> finalCuts = new java.util.ArrayList<>();
        // sortiere zunächst: vertikal vor horizontal, dann nach coord
        cuts.sort((a, b) -> {
            if (a.vertical == b.vertical) return Double.compare(a.coord, b.coord);
            return a.vertical ? -1 : 1;
        });
        int nextId = 1;
        for (CutLine c : cuts) {
            finalCuts.add(new CutLine(nextId++, c.vertical, c.coord, c.start, c.end));
        }

        return finalCuts;
    }

    private static double normalize(double v) { return Math.round(v * 1000.0) / 1000.0; }

    /**
     * Druckt Job-Koordinaten sowie Cuts und Schnittpunkte auf die ORIGINAL_OUT-Konsole.
     * Diese zentrale Methode kann vom Controller und vom Benchmark-Visualizer aufgerufen werden.
     */
    public static void printPlateInfo(Plate plate) { printPlateInfo(plate, false); }

    public static void printPlateInfo(Plate plate, boolean force) {
        if (plate == null) return;
        if (ENABLE_CONSOLE_OUTPUT) {
            // Job-Koordinaten
            System.out.println("Job-Koordinaten auf Platte '" + (plate.name == null ? "<unnamed>" : plate.name) + "':");
            if (plate.jobs != null) {
                for (Job job : plate.jobs) {
                    if (job == null) continue;
                    System.out.println(String.format("Job %d: x=%.3f, y=%.3f, w=%.3f, h=%.3f, rotated=%s", job.id, job.x, job.y, job.width, job.height, job.rotated ? "ja" : "nein"));
                }
            }
            System.out.println("");
        }
        // Cuts + Schnittpunkte immer ausgeben, wenn ENABLE_CONSOLE_OUTPUT_CUTS true ist
        printCutsAndIntersections(plate);
    }

    public static void printCutsAndIntersections(Plate plate) { printCutsAndIntersections(plate, false); }
    public static void printCutsAndIntersections(Plate plate, boolean force) {
        if (plate == null) return;
        java.util.List<CutLine> cuts = computeCutLinesForPlate(plate);
        if (cuts == null || cuts.isEmpty()) return;
        // Ausgabe: pro Cut die Endpunkte, aber nur wenn ENABLE_CONSOLE_OUTPUT_CUTS true ist
        if (ENABLE_CONSOLE_OUTPUT_CUTS) {
            for (CutLine c : cuts) {
                String orient = c.vertical ? "V" : "H";
                String p1, p2;
                if (c.vertical) {
                    p1 = String.format("(%.3f, %.3f)", c.coord, c.start);
                    p2 = String.format("(%.3f, %.3f)", c.coord, c.end);
                } else {
                    p1 = String.format("(%.3f, %.3f)", c.start, c.coord);
                    p2 = String.format("(%.3f, %.3f)", c.end, c.coord);
                }
                System.out.println(String.format("[Cut #%d] %s %s -> %s", c.id, orient, p1, p2));
            }
            System.out.println("\n");
        }
        // Schnittpunkte ermitteln (vertikal x horizontal)
        for (int i = 0; i < cuts.size(); i++) {
            CutLine ci = cuts.get(i);
            if (!ci.vertical) continue;
            for (int j = 0; j < cuts.size(); j++) {
                CutLine cj = cuts.get(j);
                if (cj.vertical) continue;
                if (ci.coord >= cj.start && ci.coord <= cj.end && cj.coord >= ci.start && cj.coord <= ci.end) {
                    // Intersection-Ausgabe optional
                    //System.out.println(String.format("[Intersection] Cut #%d (V) x Cut #%d (H) at (%.3f, %.3f)", ci.id, cj.id, ci.coord, cj.coord));
                }
            }
        }
    }

    public static boolean isPathSelected(String pathId) {
        return isSelectedBasePath(pathId);
    }

    /**
     * Helper: Druckt Plate-Info (inkl. Cuts) nur dann, wenn
     * - ENABLE_CONSOLE_OUTPUT == true (globale Konsolenausgabe)
     * oder
     * - pathId in VISUALIZE_BASE_PATH_NUMBERS enthalten ist (force für selektierte Pfade)
     *
     * Diese Methode wird vom Benchmark-Visualizer aufgerufen, um genau die
     * gewünschten Pfad-spezifischen Konsolenausgaben zu ermöglichen.
     */
    public static void printPlateInfoIfSelected(Plate plate, String pathId) {
        if (plate == null) return;
        if (ENABLE_CONSOLE_OUTPUT) {
            // globale Ausgabe eingeschaltet -> normale Ausgabe
            printPlateInfo(plate, true);
            return;
        }
        if (isSelectedBasePath(pathId)) {
            // Konsole deaktiviert, aber Pfad ist in der Liste -> erzwungene Ausgabe
            printPlateInfo(plate, true);
        }
    }

    public static boolean isSelectedBasePath(String pathId) {
        if (pathId == null || pathId.isEmpty()) return false;
        for (String id : VISUALIZE_BASE_PATH_NUMBERS) {
            if (pathId.equals(id)) return true;
        }
        return false;
    }

    private static String repeat(char c, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(c);
        return sb.toString();
    }
}