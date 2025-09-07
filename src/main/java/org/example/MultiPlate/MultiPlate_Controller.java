package org.example.MultiPlate;

import org.example.DataClasses.Job;
import org.example.DataClasses.MultiPlate_DataClasses;
import org.example.DataClasses.Plate;
import org.example.HelperClasses.JobUtils;
import org.example.Visualizer.PlateVisualizer; // hinzugefügt
import org.example.Visualizer.BenchmarkVisualizer; // hinzugefügt
import org.example.MultiPlateIndividual.CutLineCalculator;

import java.util.*;

public class MultiPlate_Controller {

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
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsBySizeDescending(jobs);

        if (plateInfos == null || plateInfos.isEmpty()) return;

        // Erstplatte: alle Jobs platzieren und Basis-Pfade erzeugen
        MultiPlate_Algorithm firstPlateAlgorithm = new MultiPlate_Algorithm(plateInfos.subList(0, 1));
        for (Job job : jobs) firstPlateAlgorithm.placeJob(job, plateInfos.get(0).plateId);

        List<MultiPlate_DataClasses> firstPlatePaths = firstPlateAlgorithm.getPathsAndFailedJobsOverviewData();
        List<MultiPlate_DataClasses> allPathsForSummary = new ArrayList<>(firstPlatePaths);

        // ERWEITERUNG AUF WEITERE PLATTEN: für jeden Pfad die noch fehlenden Jobs auf der nächsten Platte platzieren
        if (plateInfos.size() >= 2) {
            Map<String, MultiPlate_DataClasses> byId = new HashMap<>();
            for (MultiPlate_DataClasses p : allPathsForSummary) byId.put(p.pathId, p);

            List<MultiPlate_DataClasses> frontier = new ArrayList<>(firstPlatePaths);
            for (int pIdx = 1; pIdx < plateInfos.size(); pIdx++) {
                Plate template = plateInfos.get(pIdx);
                List<MultiPlate_DataClasses> nextFrontier = new ArrayList<>();
                for (MultiPlate_DataClasses parentPath : frontier) {
                    // Sammle bereits platzierte Jobs entlang der Elternkette
                    Set<Integer> placedChain = new HashSet<>();
                    String cur = parentPath.pathId;
                    while (cur != null) {
                        MultiPlate_DataClasses anc = byId.get(cur);
                        if (anc != null) for (Job pj : anc.plate.jobs) placedChain.add(pj.id);
                        int dot = cur.lastIndexOf('.');
                        if (dot < 0) break;
                        cur = cur.substring(0, dot);
                    }

                    // Bestimme noch zu platzierende Jobs (in Originalreihenfolge, ohne Duplikate)
                    LinkedHashSet<Integer> toPlaceIds = new LinkedHashSet<>();
                    for (Job original : jobs) if (!placedChain.contains(original.id)) toPlaceIds.add(original.id);
                    if (toPlaceIds.isEmpty()) continue;

                    // Erzeuge Job-Objekte für die nächste Platte und sortiere (Fläche-desc)
                    List<Job> jobsForNext = new ArrayList<>();
                    for (Integer jid : toPlaceIds) {
                        for (Job orig : jobs) if (orig.id == jid) { jobsForNext.add(new Job(orig.id, orig.width, orig.height)); break; }
                    }
                    JobUtils.sortJobsBySizeDescending(jobsForNext);

                    // Erzeuge Kinderpfade auf der aktuellen Template-Platte
                    MultiPlate_Algorithm nextAlgo = new MultiPlate_Algorithm(java.util.Arrays.asList(template), parentPath.pathId, true);
                    for (Job j : jobsForNext) nextAlgo.placeJob(j, template.plateId);
                    List<MultiPlate_DataClasses> childPaths = nextAlgo.getPathsAndFailedJobsOverviewData();

                    for (MultiPlate_DataClasses cp : childPaths) byId.put(cp.pathId, cp);
                    allPathsForSummary.addAll(childPaths);
                    nextFrontier.addAll(childPaths);
                }
                frontier = nextFrontier;
            }
        }

        // Ausgabe / Visualisierung
        printStrategyMatrix(allPathsForSummary);
        visualizeConfiguredBasePaths(allPathsForSummary);
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

    // Dynamische Visualisierung: akzeptiert Root-IDs UND Child-IDs
    // Wofür: Öffnet Fenster pro angefragter Pfad-ID (Final + optional Steps).
    // Aufruf: Am Ende von run_MaxRectBF_MultiPlate().
    private static void visualizeConfiguredBasePaths(List<MultiPlate_DataClasses> allPaths) {
        if (VISUALIZE_BASE_PATH_NUMBERS.length == 0) return;
        if (allPaths == null || allPaths.isEmpty()) return;

        // Iteriere über alle Pfade und filtere mit isSelectedBasePath (jetzt nur exakte IDs)
        for (MultiPlate_DataClasses path : allPaths) {
            if (path == null || path.pathId == null) continue;
            boolean isExact = false;
            for (String id : VISUALIZE_BASE_PATH_NUMBERS) {
                if (path.pathId.equals(id)) { isExact = true; break; }
            }
            if (!isExact) continue;

            // Nutze zentrale Druck-Funktion (berechnet Cuts + Schnittpunkte) und zwinge Ausgabe für selektierte Pfade
            printPlateInfo(path.plate, true);
            PlateVisualizer.showPlateWithCutsAndTitleAndInfo(path.plate, "7", computeCutLinesForPlate(path.plate), path.freeRects, "Pfad " + path.pathId + " (Final)", null, null);

            if (!VISUALIZE_INTERMEDIATE_STEPS) continue;

            List<Job> sortedJobs = new ArrayList<>(path.plate.jobs);
            sortedJobs.sort(Comparator.comparingInt(j -> j.placementOrder));
            int totalSteps = sortedJobs.size();
            for (int step = 1; step <= totalSteps; step++) {
                Plate partial = buildPartialPlate(path, step);
                int snapshotIndex = (path.freeRectsPerStep == null) ? -1 : Math.min(step, path.freeRectsPerStep.size()-1);
                List<MultiPlate_DataClasses.FreeRectangle> snapshot = new ArrayList<>();
                if (snapshotIndex >= 0) {
                    for (MultiPlate_DataClasses.FreeRectangle fr : path.freeRectsPerStep.get(snapshotIndex)) snapshot.add(new MultiPlate_DataClasses.FreeRectangle(fr.x, fr.y, fr.width, fr.height));
                }
                // Schrittweise Visualisierung: zeigt Status nach dem jeweiligen Platzierungsschritt
                Job lastPlaced = sortedJobs.get(step - 1);
                // Druck für Zwischenschritt (zentrale Funktion) - erzwungene Ausgabe
                printPlateInfo(partial, true);
                java.util.List<CutLineCalculator.CutLine> partialCuts = computeCutLinesForPlate(partial);
                PlateVisualizer.showPlateWithCutsAndTitleAndInfo(partial, "7", partialCuts, snapshot, "Pfad " + path.pathId + " - Step " + step + " (Job " + lastPlaced.id + ")", null, null);
            }
        }
    }

    // Hilfsfunktion: baut eine Teil-Platte mit Jobs bis inkl. gegebener Anzahl platzierter Jobs
    // Wofür: Snapshot-Visualisierung einzelner Steps innerhalb eines Pfades.
    // Aufruf: visualizeConfiguredBasePaths(), wenn VISUALIZE_INTERMEDIATE_STEPS aktiv ist.
     private static Plate buildPartialPlate(MultiPlate_DataClasses path, int placedJobsCount) {
         Plate src = path.plate;
         Plate clone = new Plate(src.name, src.width, src.height, src.plateId);
         // Jobs nach placementOrder sortieren
         List<Job> sorted = new ArrayList<>(src.jobs);
         sorted.sort(Comparator.comparingInt(j -> j.placementOrder));
         int added = 0;
         for (Job j : sorted) {
             if (added >= placedJobsCount) break;
             Job copy = new Job(j.id, j.width, j.height);
             copy.x = j.x; copy.y = j.y; copy.rotated = j.rotated; copy.placementOrder = j.placementOrder; copy.splittingMethod = j.splittingMethod; copy.placedOn = clone;
             clone.jobs.add(copy); added++;
         }
         return clone;
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
         java.util.List<BenchmarkVisualizer.BenchmarkResult> combined = new java.util.ArrayList<>();

         // Lauf 1: Sortierung nach Fläche (desc)
         {
             // Vorbereitung: Kopien + Sortierung
             List<Job> jobs = JobUtils.createJobCopies(originalJobs);
             JobUtils.sortJobsBySizeDescending(jobs);
             // Erster Plattenlauf (Pfaderzeugung)
             MultiPlate_Algorithm firstPlateAlgorithm = new MultiPlate_Algorithm(plateInfos.subList(0, 1));
             for (Job job : jobs) firstPlateAlgorithm.placeJob(job, plateInfos.get(0).plateId);
             List<MultiPlate_DataClasses> firstPlatePaths = firstPlateAlgorithm.getPathsAndFailedJobsOverviewData();
             List<MultiPlate_DataClasses> allPathsForSummary = new ArrayList<>(firstPlatePaths);
             if (plateInfos.size() >= 2) {
                 Map<String, MultiPlate_DataClasses> byId = new HashMap<>(); for (MultiPlate_DataClasses p : allPathsForSummary) byId.put(p.pathId, p);
                 List<MultiPlate_DataClasses> frontier = new ArrayList<>(firstPlatePaths);
                 for (int pIdx = 1; pIdx < plateInfos.size(); pIdx++) {
                     Plate template = plateInfos.get(pIdx);
                     List<MultiPlate_DataClasses> nextFrontier = new ArrayList<>();
                     for (MultiPlate_DataClasses parentPath : frontier) {
                         Set<Integer> placedChain = new HashSet<>(); String cur = parentPath.pathId;
                         while (cur != null) { MultiPlate_DataClasses anc = byId.get(cur); if (anc != null) for (Job pj : anc.plate.jobs) placedChain.add(pj.id); int dot = cur.lastIndexOf('.'); if (dot < 0) break; else cur = cur.substring(0, dot); }
                         LinkedHashSet<Integer> toPlaceIds = new LinkedHashSet<>(); for (Job original : jobs) if (!placedChain.contains(original.id)) toPlaceIds.add(original.id);
                         if (toPlaceIds.isEmpty()) continue;
                         List<Job> jobsForNext = new ArrayList<>(); for (Integer jid : toPlaceIds) { for (Job orig : jobs) if (orig.id == jid) { jobsForNext.add(new Job(orig.id, orig.width, orig.height)); break; } }
                         JobUtils.sortJobsBySizeDescending(jobsForNext);
                         MultiPlate_Algorithm nextAlgo = new MultiPlate_Algorithm(java.util.Arrays.asList(template), parentPath.pathId, true);
                         for (Job j : jobsForNext) nextAlgo.placeJob(j, template.plateId);
                         List<MultiPlate_DataClasses> childPaths = nextAlgo.getPathsAndFailedJobsOverviewData();
                         for (MultiPlate_DataClasses cp : childPaths) byId.put(cp.pathId, cp);
                         allPathsForSummary.addAll(childPaths); nextFrontier.addAll(childPaths);
                     }
                     frontier = nextFrontier;
                 }
             }
             // Aggregation + Label
             combined.addAll(buildBenchmarkResults(allPathsForSummary, originalJobs, plateInfos));
             // Label für diesen Lauf setzen
             for (BenchmarkVisualizer.BenchmarkResult br : combined) if (br.sortLabel == null || "-".equals(br.sortLabel)) br.sortLabel = "Fläche";
         }

         
         // Lauf 2: Sortierung nach größter Kante (desc)
         {
             // Vorbereitung: Kopien + Sortierung
             List<Job> jobs = JobUtils.createJobCopies(originalJobs);
             JobUtils.sortJobsByLargestEdgeDescending(jobs);
             // Erster Plattenlauf (Pfaderzeugung)
             MultiPlate_Algorithm firstPlateAlgorithm = new MultiPlate_Algorithm(plateInfos.subList(0, 1));
             for (Job job : jobs) firstPlateAlgorithm.placeJob(job, plateInfos.get(0).plateId);
             List<MultiPlate_DataClasses> firstPlatePaths = firstPlateAlgorithm.getPathsAndFailedJobsOverviewData();
             List<MultiPlate_DataClasses> allPathsForSummary = new ArrayList<>(firstPlatePaths);
             if (plateInfos.size() >= 2) {
                 Map<String, MultiPlate_DataClasses> byId = new HashMap<>(); for (MultiPlate_DataClasses p : allPathsForSummary) byId.put(p.pathId, p);
                 List<MultiPlate_DataClasses> frontier = new ArrayList<>(firstPlatePaths);
                 for (int pIdx = 1; pIdx < plateInfos.size(); pIdx++) {
                     Plate template = plateInfos.get(pIdx);
                     List<MultiPlate_DataClasses> nextFrontier = new ArrayList<>();
                     for (MultiPlate_DataClasses parentPath : frontier) {
                         Set<Integer> placedChain = new HashSet<>(); String cur = parentPath.pathId;
                         while (cur != null) { MultiPlate_DataClasses anc = byId.get(cur); if (anc != null) for (Job pj : anc.plate.jobs) placedChain.add(pj.id); int dot = cur.lastIndexOf('.'); if (dot < 0) break; else cur = cur.substring(0, dot); }
                         LinkedHashSet<Integer> toPlaceIds = new LinkedHashSet<>(); for (Job original : jobs) if (!placedChain.contains(original.id)) toPlaceIds.add(original.id);
                         if (toPlaceIds.isEmpty()) continue;
                         List<Job> jobsForNext = new ArrayList<>(); for (Integer jid : toPlaceIds) { for (Job orig : jobs) if (orig.id == jid) { jobsForNext.add(new Job(orig.id, orig.width, orig.height)); break; } }
                         JobUtils.sortJobsByLargestEdgeDescending(jobsForNext);
                         MultiPlate_Algorithm nextAlgo = new MultiPlate_Algorithm(java.util.Arrays.asList(template), parentPath.pathId, true);
                         for (Job j : jobsForNext) nextAlgo.placeJob(j, template.plateId);
                         List<MultiPlate_DataClasses> childPaths = nextAlgo.getPathsAndFailedJobsOverviewData();
                         for (MultiPlate_DataClasses cp : childPaths) byId.put(cp.pathId, cp);
                         allPathsForSummary.addAll(childPaths); nextFrontier.addAll(childPaths);
                     }
                     frontier = nextFrontier;
                 }
             }
             // Aggregation + Label
             java.util.List<BenchmarkVisualizer.BenchmarkResult> secondPass = buildBenchmarkResults(allPathsForSummary, originalJobs, plateInfos);
             for (BenchmarkVisualizer.BenchmarkResult br : secondPass) br.sortLabel = "Größte Kante";
             combined.addAll(secondPass);
         }


         // Übergabe an GUI: Gemeinsames Ranking aller Pfade (beide Sortierungen) im Benchmark-Fenster
         String info = "MultiPlate: " + (plateInfos==null?"?":String.valueOf(plateInfos.size())) + " Platte(n)";
         BenchmarkVisualizer.showBenchmarkResults(combined, info);
     }
 
     // Kleine Hilfsfunktion: String mit wiederholtem Zeichen erzeugen
    private static String repeat(char c, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(c);
        return sb.toString();
    }

    // ---------------------------------------------
    // Schnittlinien-Berechnung
    // ---------------------------------------------
    /**
     * Repräsentiert eine Schnittlinie (entweder vertikal oder horizontal).
     * coord = x (bei vertical=true) bzw. y (bei vertical=false).
     * start/end definieren den Bereich entlang der Linie (in mm).
     */
    public static java.util.List<CutLineCalculator.CutLine> computeCutLinesForPlate(Plate plate) {
        return CutLineCalculator.calculateCutLinesForPlate(plate);
    }

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
        java.util.List<CutLineCalculator.CutLine> cuts = computeCutLinesForPlate(plate);
        if (cuts == null || cuts.isEmpty()) return;
        // Ausgabe: pro Cut die Endpunkte, aber nur wenn ENABLE_CONSOLE_OUTPUT_CUTS true ist
        if (ENABLE_CONSOLE_OUTPUT_CUTS) {
            for (CutLineCalculator.CutLine c : cuts) {
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
            CutLineCalculator.CutLine ci = cuts.get(i);
            if (!ci.vertical) continue;
            for (int j = 0; j < cuts.size(); j++) {
                CutLineCalculator.CutLine cj = cuts.get(j);
                if (cj.vertical) continue;
                if (ci.coord >= cj.start && ci.coord <= cj.end && cj.coord >= ci.start && cj.coord <= ci.end) {
                    // Intersection-Ausgabe optional
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
}