package org.example.MultiPlateIndividual;

import org.example.DataClasses.Job;
import org.example.DataClasses.MultiPlate_DataClasses;
import org.example.DataClasses.Plate;
import org.example.HelperClasses.JobUtils;
import org.example.Visualizer.BenchmarkVisualizer;

import java.util.*;

public class MultiPlateIndividual_Controller {

    private static final boolean ENABLE_CONSOLE_OUTPUT_CUTS = false;  // Schnittkoordinaten auf Konsole ausgeben
    private static final boolean ENABLE_CONSOLE_OUTPUTS_JOBSETS = false;

    /*
     * Erster Aufruf
     */
    public static void run_MaxRectBF_MultiPlate(List<Job> originalJobs, List<Plate> plateInfos, boolean sortJobs) {
        if (plateInfos == null || plateInfos.isEmpty() || originalJobs == null || originalJobs.isEmpty()) return;

        /*
         * Platte 1 (Fläche): Jobs kopieren, sortieren, platzieren und failed Jobs holen
         */
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsBySizeDescending(jobs);
        MultiPlateIndividual_Algorithm firstPlateAlgorithm = new MultiPlateIndividual_Algorithm(plateInfos.subList(0, 1)); 
        for (Job job : jobs) firstPlateAlgorithm.placeJob(job, plateInfos.get(0).plateId); 
        List<MultiPlate_DataClasses> firstPlatePaths = firstPlateAlgorithm.getPathsAndFailedJobsOverviewData(); 

        /*
         * Nach Abschluss von Platte 1 (Fläche): Gruppenbildung für unplatzierte Jobs. 
         * (Unterschiedliche Pfade, die aber die gleichen failed Jobs haben, gehören zur selben Gruppe).
         * Gruppen auf Konsole ausgeben.
         * Benchmark bauen.
         */
        Set<Integer> allJobIds = new LinkedHashSet<>(); for (Job j : originalJobs) allJobIds.add(j.id);
        List<JobSetGroup> groups = buildUnplacedJobGroups(firstPlatePaths, allJobIds);
        printJobSetGroups(groups, 1, "Fläche");
        List<BenchmarkVisualizer.BenchmarkResult> resP1 = buildBenchmarkResults(firstPlatePaths, jobs, java.util.Arrays.asList(plateInfos.get(0)));
        for (BenchmarkVisualizer.BenchmarkResult br : resP1) { br.sortLabel = "Platte 1"; br.sortedBy = "Fläche";
            if (br.perPlateSetLabels == null) br.perPlateSetLabels = new java.util.ArrayList<>();
            if (br.perPlateSetLabels.isEmpty()) br.perPlateSetLabels.add(br.rootSetId == null ? "-" : br.rootSetId);
            else br.perPlateSetLabels.set(0, br.rootSetId == null ? "-" : br.rootSetId);
        }
        Map<String,String> rootByPathArea = new HashMap<>();
        for (JobSetGroup g : groups) for (String pid : g.pathIds) rootByPathArea.put(pid, g.rootSetId);
        for (BenchmarkVisualizer.BenchmarkResult br : resP1) {
            String pid = extractPathId(br.algorithmName);
            br.rootSetId = pid != null ? rootByPathArea.getOrDefault(pid, "-") : "-";
        }

        /*
         * Platte 1 (Kante): Jobs kopieren, sortieren, platzieren und failed Jobs holen
         */
        List<MultiPlate_DataClasses> pathsEdgeP1 = new ArrayList<>(); 
        List<Job> jobsEdge = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsByLargestEdgeDescending(jobsEdge);
        MultiPlateIndividual_Algorithm algoEdge = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(plateInfos.get(0)));
        for (Job job : jobsEdge) algoEdge.placeJob(job, plateInfos.get(0).plateId);
        pathsEdgeP1.addAll(algoEdge.getPathsAndFailedJobsOverviewData()); 

        /*
         * Nach Abschluss von Platte 1 (Kante): Gruppenbildung für unplatzierte Jobs.
         * (Unterschiedliche Pfade, die aber die gleichen failed Jobs haben, gehören zur selben Gruppe).
         * Gruppen auf Konsole ausgeben.
         * Benchmark bauen.
         */ 
        List<JobSetGroup> groupsEdge = buildUnplacedJobGroups(pathsEdgeP1, allJobIds);
        printJobSetGroups(groupsEdge, 1, "Kante");
        List<BenchmarkVisualizer.BenchmarkResult> resP1Edge = buildBenchmarkResults(pathsEdgeP1, originalJobs, java.util.Arrays.asList(plateInfos.get(0)));
        for (BenchmarkVisualizer.BenchmarkResult br : resP1Edge) { br.sortLabel = "Platte 1"; br.sortedBy = "Kante"; }
        Map<String,String> rootByPathEdge = new HashMap<>();
        for (JobSetGroup g : groupsEdge) for (String pid : g.pathIds) rootByPathEdge.put(pid, g.rootSetId);
        for (BenchmarkVisualizer.BenchmarkResult br : resP1Edge) {
            String pid = extractPathId(br.algorithmName);
            br.rootSetId = pid != null ? rootByPathEdge.getOrDefault(pid, "-") : "-";
        }

        /*
         * Platte 1 (Strategie-Code-Matrix) auf Konsole ausgeben
         */
        {
            List<Integer> orderArea = new ArrayList<>(); for (Job j : jobs) orderArea.add(j.id);
            printStrategyCodeMatrix(firstPlatePaths, "Platte 1 - Strategie-Matrix (Fläche)", orderArea);
            List<Integer> orderEdge = new ArrayList<>(); for (Job j : jobsEdge) orderEdge.add(j.id);
            printStrategyCodeMatrix(pathsEdgeP1,   "Platte 1 - Strategie-Matrix (Kante)", orderEdge);
        }

        /*
         * Platte 1 (Fläche + Kante): Zusammen im BenchmarkVisualizer anzeigen
         */
        List<BenchmarkVisualizer.BenchmarkResult> combinedP1 = new ArrayList<>();
        combinedP1.addAll(resP1);
        combinedP1.addAll(resP1Edge);
        BenchmarkVisualizer.showBenchmarkResults(combinedP1, "Platte 1 - Gesamtlauf");
        
        /*
         * Platten n+1: Pro Job-Set Fläche- und Kante-Lauf benchmarken und ggf. Gruppen für nächste Platte bilden
         * Jetzt für beide Seeds: aus P1-Fläche (groups) und aus P1-Kante (groupsEdge)
         */
        List<JobSetGroup> currentGroupsArea = groups;
        List<JobSetGroup> currentGroupsEdge = groupsEdge;
        for (int plateIdx = 1; plateIdx < plateInfos.size(); plateIdx++) {
            Plate currentPlate = plateInfos.get(plateIdx);
            Map<String, JobSetGroup> aggregatedForNextArea = new LinkedHashMap<>();
            Map<String, JobSetGroup> aggregatedForNextEdge = new LinkedHashMap<>();

            // A) Seed = P1-Fläche
            for (JobSetGroup g : currentGroupsArea) {
                if (g.jobIds.isEmpty()) continue;
                List<Job> subset = getJobsSubsetForIds(originalJobs, g.jobIds);
                if (sortJobs) JobUtils.sortJobsBySizeDescending(subset);
                MultiPlateIndividual_Algorithm algoArea = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(currentPlate));
                for (Job j : subset) algoArea.placeJob(j, currentPlate.plateId);
                List<MultiPlate_DataClasses> pathsArea = algoArea.getPathsAndFailedJobsOverviewData();
                List<Job> subsetEdgeView = getJobsSubsetForIds(originalJobs, g.jobIds);
                JobUtils.sortJobsByLargestEdgeDescending(subsetEdgeView);
                MultiPlateIndividual_Algorithm algoEdgeView = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(currentPlate));
                for (Job j : subsetEdgeView) algoEdgeView.placeJob(j, currentPlate.plateId);
                List<MultiPlate_DataClasses> pathsEdgeView = algoEdgeView.getPathsAndFailedJobsOverviewData();

                List<Integer> idsSorted = new ArrayList<>(g.jobIds); Collections.sort(idsSorted);
                String setLabel = idsSorted.toString();

                List<BenchmarkVisualizer.BenchmarkResult> resSetArea = buildBenchmarkResults(pathsArea, subset, java.util.Arrays.asList(currentPlate));
                for (BenchmarkVisualizer.BenchmarkResult br : resSetArea) {
                    br.sortLabel = "Set " + setLabel; br.jobSetLabel = setLabel; br.sortedBy = "Fläche"; br.rootSetId = g.rootSetId;
                    // Set-Labels auffüllen: Index 0=P1, Index plateIdx=P(plateIdx+1)
                    if (br.perPlateSetLabels == null) br.perPlateSetLabels = new java.util.ArrayList<>();
                    ensureSize(br.perPlateSetLabels, plateIdx + 1);
                    br.perPlateSetLabels.set(0, g.rootSetId == null ? "-" : g.rootSetId);
                    br.perPlateSetLabels.set(plateIdx, setLabel);
                }
                List<BenchmarkVisualizer.BenchmarkResult> resSetEdge = buildBenchmarkResults(pathsEdgeView, subset, java.util.Arrays.asList(currentPlate));
                for (BenchmarkVisualizer.BenchmarkResult br : resSetEdge) {
                    br.sortLabel = "Set " + setLabel; br.jobSetLabel = setLabel; br.sortedBy = "Kante"; br.rootSetId = g.rootSetId;
                    if (br.perPlateSetLabels == null) br.perPlateSetLabels = new java.util.ArrayList<>();
                    ensureSize(br.perPlateSetLabels, plateIdx + 1);
                    br.perPlateSetLabels.set(0, g.rootSetId == null ? "-" : g.rootSetId);
                    br.perPlateSetLabels.set(plateIdx, setLabel);
                }

                List<BenchmarkVisualizer.BenchmarkResult> combinedSet = new ArrayList<>(); combinedSet.addAll(resSetArea); combinedSet.addAll(resSetEdge);
                BenchmarkVisualizer.showBenchmarkResults(combinedSet, "Platte " + (plateIdx + 1) + " - Job-Set: " + setLabel);

                List<JobSetGroup> sub = buildUnplacedJobGroups(pathsArea, g.jobIds);
                for (JobSetGroup sg : sub) {
                    if (sg.jobIds.isEmpty()) continue;
                    List<Integer> idsSortedSub = new ArrayList<>(sg.jobIds); Collections.sort(idsSortedSub);
                    String key = idsSortedSub.toString();
                    JobSetGroup dst = aggregatedForNextArea.computeIfAbsent(key, k -> new JobSetGroup(new LinkedHashSet<>(sg.jobIds)));
                    dst.pathIds.addAll(sg.pathIds);
                    if (dst.rootSetId == null) dst.rootSetId = g.rootSetId;
                }
            }

            // B) Seed = P1-Kante
            for (JobSetGroup g : currentGroupsEdge) {
                if (g.jobIds.isEmpty()) continue;
                List<Job> subset = getJobsSubsetForIds(originalJobs, g.jobIds);
                if (sortJobs) JobUtils.sortJobsBySizeDescending(subset);
                MultiPlateIndividual_Algorithm algoArea = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(currentPlate));
                for (Job j : subset) algoArea.placeJob(j, currentPlate.plateId);
                List<MultiPlate_DataClasses> pathsArea = algoArea.getPathsAndFailedJobsOverviewData();
                List<Job> subsetEdgeView = getJobsSubsetForIds(originalJobs, g.jobIds);
                JobUtils.sortJobsByLargestEdgeDescending(subsetEdgeView);
                MultiPlateIndividual_Algorithm algoEdgeView = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(currentPlate));
                for (Job j : subsetEdgeView) algoEdgeView.placeJob(j, currentPlate.plateId);
                List<MultiPlate_DataClasses> pathsEdgeView = algoEdgeView.getPathsAndFailedJobsOverviewData();

                List<Integer> idsSorted = new ArrayList<>(g.jobIds); Collections.sort(idsSorted);
                String setLabel = idsSorted.toString();

                List<BenchmarkVisualizer.BenchmarkResult> resSetArea = buildBenchmarkResults(pathsArea, subset, java.util.Arrays.asList(currentPlate));
                for (BenchmarkVisualizer.BenchmarkResult br : resSetArea) {
                    br.sortLabel = "Set " + setLabel; br.jobSetLabel = setLabel; br.sortedBy = "Fläche"; br.rootSetId = g.rootSetId;
                    if (br.perPlateSetLabels == null) br.perPlateSetLabels = new java.util.ArrayList<>();
                    ensureSize(br.perPlateSetLabels, plateIdx + 1);
                    br.perPlateSetLabels.set(0, g.rootSetId == null ? "-" : g.rootSetId);
                    br.perPlateSetLabels.set(plateIdx, setLabel);
                }
                List<BenchmarkVisualizer.BenchmarkResult> resSetEdge = buildBenchmarkResults(pathsEdgeView, subset, java.util.Arrays.asList(currentPlate));
                for (BenchmarkVisualizer.BenchmarkResult br : resSetEdge) {
                    br.sortLabel = "Set " + setLabel; br.jobSetLabel = setLabel; br.sortedBy = "Kante"; br.rootSetId = g.rootSetId;
                    if (br.perPlateSetLabels == null) br.perPlateSetLabels = new java.util.ArrayList<>();
                    ensureSize(br.perPlateSetLabels, plateIdx + 1);
                    br.perPlateSetLabels.set(0, g.rootSetId == null ? "-" : g.rootSetId);
                    br.perPlateSetLabels.set(plateIdx, setLabel);
                }

                List<BenchmarkVisualizer.BenchmarkResult> combinedSet = new ArrayList<>(); combinedSet.addAll(resSetArea); combinedSet.addAll(resSetEdge);
                BenchmarkVisualizer.showBenchmarkResults(combinedSet, "Platte " + (plateIdx + 1) + " - Job-Set: " + setLabel + " | Seed=P1-Kante");

                List<JobSetGroup> subEdge = buildUnplacedJobGroups(pathsEdgeView, g.jobIds);
                for (JobSetGroup sg : subEdge) {
                    if (sg.jobIds.isEmpty()) continue;
                    List<Integer> idsSortedSub = new ArrayList<>(sg.jobIds); Collections.sort(idsSortedSub);
                    String key = idsSortedSub.toString();
                    JobSetGroup dst = aggregatedForNextEdge.computeIfAbsent(key, k -> new JobSetGroup(new LinkedHashSet<>(sg.jobIds)));
                    dst.pathIds.addAll(sg.pathIds);
                    if (dst.rootSetId == null) dst.rootSetId = g.rootSetId;
                }
            }

            List<JobSetGroup> nextArea = new ArrayList<>(aggregatedForNextArea.values());
            currentGroupsArea = nextArea;

            // Kante-Seed Gruppen auch weiterreichen
            currentGroupsEdge = new ArrayList<>(aggregatedForNextEdge.values());

            // Abbruch, wenn in beiden Bäumen nichts mehr offen ist
            if (currentGroupsArea.isEmpty() && currentGroupsEdge.isEmpty()) break;
        }
    }

    /*
     * Gruppenbildung für unplatzierte Jobs. Unterschiedliche Pfade, die aber die gleichen failed Jobs haben, gehören zur selben Gruppe.
     */
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
            if (g.rootSetId == null) g.rootSetId = key; // Root-Set (P1) auf Gruppen-Key setzen
            g.pathIds.add(p.pathId);
        }
        return new ArrayList<>(map.values());
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

    private static class JobSetGroup {
        final Set<Integer> jobIds; 
        final List<String> pathIds = new ArrayList<>();
        String rootSetId; // Root-Set-ID von Platte 1
        JobSetGroup(Set<Integer> jobIds) { 
            this.jobIds = jobIds; 
        }
    }

    // Pfad-ID aus "Pfad X[.Y]" im algorithmName extrahieren
    private static String extractPathId(String algoName) {
        if (algoName == null) return null;
        int idx = algoName.indexOf("Pfad ");
        if (idx < 0) return null;
        int start = idx + 5;
        int end = start;
        while (end < algoName.length()) {
            char c = algoName.charAt(end);
            if (!Character.isDigit(c) && c != '.') break;
            end++;
        }
        return end > start ? algoName.substring(start, end) : null;
    }

    /*
     * Benchmark-Fenster bauen
     */
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
            if (parentIds.contains(p.pathId)) continue;
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


    /*
     * Schnittlinien-Berechnung
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

    public static java.util.List<CutLine> calculateCutLinesForPlate(Plate plate) {
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

    private static double normalize(double v) { 
        return Math.round(v * 1000.0) / 1000.0; 
    }

    public static void printCutsAndIntersections(Plate plate, boolean force) {
        if (plate == null) return;
        java.util.List<CutLine> cuts = calculateCutLinesForPlate(plate);
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
                }
            }
        }
    }

    /*
     * Gib die Job-Sets auf die Konsole nach Abschluss jeder Platte aus
     */
    private static void printJobSetGroups(List<JobSetGroup> groups, int plateNumber, String variant) {
        String suffix = (variant == null || variant.isEmpty()) ? "" : " (" + variant + ")";
        System.out.println("\n=== Nicht platzierte Job-Sets nach Platte " + plateNumber + suffix + " ===");
        if (groups == null || groups.isEmpty()) {
            System.out.println("(keine Gruppen)");
            return;
        }
        int idx = 1;
        for (JobSetGroup g : groups) {
            List<Integer> ids = new ArrayList<>(g.jobIds); Collections.sort(ids);
            if (ENABLE_CONSOLE_OUTPUTS_JOBSETS) {
                System.out.println(String.format("Gruppe %d: Jobs %s | erzeugt von Pfaden: %s", idx++, ids, g.pathIds));
            } else {
                System.out.println(String.format("Gruppe %d: Jobs %s", idx++, ids));
            }
            System.out.println();
        }
    }

    // Hilfsfunktion: Liste auf Zielgröße auffüllen und mit "-" vorbelegen
    private static void ensureSize(java.util.List<String> list, int size) {
        while (list.size() < size) list.add("-");
    }

    // Unendlicher Plattenmodus (Template wird pro Stufe neu geklont, bis keine Gruppen mehr offen sind)
    public static void run_MaxRectBF_MultiPlate_Unlimited(List<Job> originalJobs, Plate plateTemplate, boolean sortJobs) {
        if (plateTemplate == null || originalJobs == null || originalJobs.isEmpty()) return;

        // Platte 1 (Fläche)
        List<Job> jobs = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsBySizeDescending(jobs);
        MultiPlateIndividual_Algorithm firstPlateAlgorithm = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(clonePlate(plateTemplate)));
        for (Job job : jobs) firstPlateAlgorithm.placeJob(job, plateTemplate.plateId);
        List<MultiPlate_DataClasses> firstPlatePaths = firstPlateAlgorithm.getPathsAndFailedJobsOverviewData();

        // Gruppenbildung P1 (Fläche)
        Set<Integer> allJobIds = new LinkedHashSet<>(); for (Job j : originalJobs) allJobIds.add(j.id);
        List<JobSetGroup> groups = buildUnplacedJobGroups(firstPlatePaths, allJobIds);
        printJobSetGroups(groups, 1, "Fläche");

        // Benchmarks P1 (Fläche)
        List<BenchmarkVisualizer.BenchmarkResult> resP1 = buildBenchmarkResults(firstPlatePaths, jobs, java.util.Arrays.asList(plateTemplate));
        for (BenchmarkVisualizer.BenchmarkResult br : resP1) { br.sortLabel = "Platte 1"; br.sortedBy = "Fläche"; }
        Map<String,String> rootByPathArea = new HashMap<>();
        for (JobSetGroup g : groups) for (String pid : g.pathIds) rootByPathArea.put(pid, g.rootSetId);
        for (BenchmarkVisualizer.BenchmarkResult br : resP1) {
            String pid = extractPathId(br.algorithmName);
            br.rootSetId = pid != null ? rootByPathArea.getOrDefault(pid, "-") : "-";
        }

        // Platte 1 (Kante)
        List<Job> jobsEdge = JobUtils.createJobCopies(originalJobs);
        if (sortJobs) JobUtils.sortJobsByLargestEdgeDescending(jobsEdge);
        MultiPlateIndividual_Algorithm algoEdge = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(clonePlate(plateTemplate)));
        for (Job job : jobsEdge) algoEdge.placeJob(job, plateTemplate.plateId);
        List<MultiPlate_DataClasses> pathsEdgeP1 = algoEdge.getPathsAndFailedJobsOverviewData();

        // Gruppenbildung/Benchmark P1 (Kante)
        List<JobSetGroup> groupsEdge = buildUnplacedJobGroups(pathsEdgeP1, allJobIds);
        printJobSetGroups(groupsEdge, 1, "Kante");
        List<BenchmarkVisualizer.BenchmarkResult> resP1Edge = buildBenchmarkResults(pathsEdgeP1, originalJobs, java.util.Arrays.asList(plateTemplate));
        for (BenchmarkVisualizer.BenchmarkResult br : resP1Edge) { br.sortLabel = "Platte 1"; br.sortedBy = "Kante"; }
        Map<String,String> rootByPathEdge = new HashMap<>();
        for (JobSetGroup g : groupsEdge) for (String pid : g.pathIds) rootByPathEdge.put(pid, g.rootSetId);
        for (BenchmarkVisualizer.BenchmarkResult br : resP1Edge) {
            String pid = extractPathId(br.algorithmName);
            br.rootSetId = pid != null ? rootByPathEdge.getOrDefault(pid, "-") : "-";
        }

        // Matrix-Ausgabe für Platte 1
        {
            List<Integer> orderArea = new ArrayList<>(); for (Job j : jobs) orderArea.add(j.id);
            printStrategyCodeMatrix(firstPlatePaths, "Platte 1 - Strategie-Matrix (Fläche)", orderArea);
            List<Integer> orderEdge = new ArrayList<>(); for (Job j : jobsEdge) orderEdge.add(j.id);
            printStrategyCodeMatrix(pathsEdgeP1,   "Platte 1 - Strategie-Matrix (Kante)", orderEdge);
        }

        // P1 anzeigen
        List<BenchmarkVisualizer.BenchmarkResult> combinedP1 = new ArrayList<>();
        combinedP1.addAll(resP1); combinedP1.addAll(resP1Edge);
        BenchmarkVisualizer.showBenchmarkResults(combinedP1, "Platte 1 - Gesamtlauf");

        // Folgeplatten: solange Gruppen existieren, pro Stufe eine neue Platte aus Template erzeugen
        List<JobSetGroup> currentGroupsArea = groups;
        List<JobSetGroup> currentGroupsEdge = groupsEdge;
        int plateIdx = 1; // nächste Platte ist P2
        while (true) {
            if (currentGroupsArea.isEmpty() && currentGroupsEdge.isEmpty()) break;
            Plate currentPlate = clonePlate(plateTemplate);
            Map<String, JobSetGroup> aggregatedForNextArea = new LinkedHashMap<>();
            Map<String, JobSetGroup> aggregatedForNextEdge = new LinkedHashMap<>();

            // A) Seed = Fläche
            for (JobSetGroup g : currentGroupsArea) {
                if (g.jobIds.isEmpty()) continue;
                List<Job> subset = getJobsSubsetForIds(originalJobs, g.jobIds);
                if (sortJobs) JobUtils.sortJobsBySizeDescending(subset);
                MultiPlateIndividual_Algorithm algoArea = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(currentPlate));
                for (Job j : subset) algoArea.placeJob(j, currentPlate.plateId);
                List<MultiPlate_DataClasses> pathsArea = algoArea.getPathsAndFailedJobsOverviewData();

                List<Job> subsetEdgeView = getJobsSubsetForIds(originalJobs, g.jobIds);
                JobUtils.sortJobsByLargestEdgeDescending(subsetEdgeView);
                MultiPlateIndividual_Algorithm algoEdgeView = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(currentPlate));
                for (Job j : subsetEdgeView) algoEdgeView.placeJob(j, currentPlate.plateId);
                List<MultiPlate_DataClasses> pathsEdgeView = algoEdgeView.getPathsAndFailedJobsOverviewData();

                List<Integer> idsSorted = new ArrayList<>(g.jobIds); Collections.sort(idsSorted);
                String setLabel = idsSorted.toString();

                List<BenchmarkVisualizer.BenchmarkResult> resSetArea = buildBenchmarkResults(pathsArea, subset, java.util.Arrays.asList(currentPlate));
                for (BenchmarkVisualizer.BenchmarkResult br : resSetArea) {
                    br.sortLabel = "Set " + setLabel; br.jobSetLabel = setLabel; br.sortedBy = "Fläche"; br.rootSetId = g.rootSetId;
                    if (br.perPlateSetLabels == null) br.perPlateSetLabels = new java.util.ArrayList<>();
                    ensureSize(br.perPlateSetLabels, plateIdx + 1);
                    br.perPlateSetLabels.set(0, g.rootSetId == null ? "-" : g.rootSetId);
                    br.perPlateSetLabels.set(plateIdx, setLabel);
                }
                List<BenchmarkVisualizer.BenchmarkResult> resSetEdge = buildBenchmarkResults(pathsEdgeView, subset, java.util.Arrays.asList(currentPlate));
                for (BenchmarkVisualizer.BenchmarkResult br : resSetEdge) {
                    br.sortLabel = "Set " + setLabel; br.jobSetLabel = setLabel; br.sortedBy = "Kante"; br.rootSetId = g.rootSetId;
                    if (br.perPlateSetLabels == null) br.perPlateSetLabels = new java.util.ArrayList<>();
                    ensureSize(br.perPlateSetLabels, plateIdx + 1);
                    br.perPlateSetLabels.set(0, g.rootSetId == null ? "-" : g.rootSetId);
                    br.perPlateSetLabels.set(plateIdx, setLabel);
                }

                List<BenchmarkVisualizer.BenchmarkResult> combinedSet = new ArrayList<>(); combinedSet.addAll(resSetArea); combinedSet.addAll(resSetEdge);
                BenchmarkVisualizer.showBenchmarkResults(combinedSet, "Platte " + (plateIdx + 1) + " - Job-Set: " + setLabel);

                List<JobSetGroup> sub = buildUnplacedJobGroups(pathsArea, g.jobIds);
                for (JobSetGroup sg : sub) {
                    if (sg.jobIds.isEmpty()) continue;
                    List<Integer> idsSortedSub = new ArrayList<>(sg.jobIds); Collections.sort(idsSortedSub);
                    String key = idsSortedSub.toString();
                    JobSetGroup dst = aggregatedForNextArea.computeIfAbsent(key, k -> new JobSetGroup(new LinkedHashSet<>(sg.jobIds)));
                    dst.pathIds.addAll(sg.pathIds);
                    if (dst.rootSetId == null) dst.rootSetId = g.rootSetId;
                }
            }

            // B) Seed = Kante
            for (JobSetGroup g : currentGroupsEdge) {
                if (g.jobIds.isEmpty()) continue;
                List<Job> subset = getJobsSubsetForIds(originalJobs, g.jobIds);
                if (sortJobs) JobUtils.sortJobsBySizeDescending(subset);
                MultiPlateIndividual_Algorithm algoArea = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(currentPlate));
                for (Job j : subset) algoArea.placeJob(j, currentPlate.plateId);
                List<MultiPlate_DataClasses> pathsArea = algoArea.getPathsAndFailedJobsOverviewData();

                List<Job> subsetEdgeView = getJobsSubsetForIds(originalJobs, g.jobIds);
                JobUtils.sortJobsByLargestEdgeDescending(subsetEdgeView);
                MultiPlateIndividual_Algorithm algoEdgeView = new MultiPlateIndividual_Algorithm(java.util.Arrays.asList(currentPlate));
                for (Job j : subsetEdgeView) algoEdgeView.placeJob(j, currentPlate.plateId);
                List<MultiPlate_DataClasses> pathsEdgeView = algoEdgeView.getPathsAndFailedJobsOverviewData();

                List<Integer> idsSorted = new ArrayList<>(g.jobIds); Collections.sort(idsSorted);
                String setLabel = idsSorted.toString();

                List<BenchmarkVisualizer.BenchmarkResult> resSetArea = buildBenchmarkResults(pathsArea, subset, java.util.Arrays.asList(currentPlate));
                for (BenchmarkVisualizer.BenchmarkResult br : resSetArea) {
                    br.sortLabel = "Set " + setLabel; br.jobSetLabel = setLabel; br.sortedBy = "Fläche"; br.rootSetId = g.rootSetId;
                    if (br.perPlateSetLabels == null) br.perPlateSetLabels = new java.util.ArrayList<>();
                    ensureSize(br.perPlateSetLabels, plateIdx + 1);
                    br.perPlateSetLabels.set(0, g.rootSetId == null ? "-" : g.rootSetId);
                    br.perPlateSetLabels.set(plateIdx, setLabel);
                }
                List<BenchmarkVisualizer.BenchmarkResult> resSetEdge = buildBenchmarkResults(pathsEdgeView, subset, java.util.Arrays.asList(currentPlate));
                for (BenchmarkVisualizer.BenchmarkResult br : resSetEdge) {
                    br.sortLabel = "Set " + setLabel; br.jobSetLabel = setLabel; br.sortedBy = "Kante"; br.rootSetId = g.rootSetId;
                    if (br.perPlateSetLabels == null) br.perPlateSetLabels = new java.util.ArrayList<>();
                    ensureSize(br.perPlateSetLabels, plateIdx + 1);
                    br.perPlateSetLabels.set(0, g.rootSetId == null ? "-" : g.rootSetId);
                    br.perPlateSetLabels.set(plateIdx, setLabel);
                }

                List<BenchmarkVisualizer.BenchmarkResult> combinedSet = new ArrayList<>(); combinedSet.addAll(resSetArea); combinedSet.addAll(resSetEdge);
                BenchmarkVisualizer.showBenchmarkResults(combinedSet, "Platte " + (plateIdx + 1) + " - Job-Set: " + setLabel + " | Seed=P1-Kante");

                List<JobSetGroup> subEdge = buildUnplacedJobGroups(pathsEdgeView, g.jobIds);
                for (JobSetGroup sg : subEdge) {
                    if (sg.jobIds.isEmpty()) continue;
                    List<Integer> idsSortedSub = new ArrayList<>(sg.jobIds); Collections.sort(idsSortedSub);
                    String key = idsSortedSub.toString();
                    JobSetGroup dst = aggregatedForNextEdge.computeIfAbsent(key, k -> new JobSetGroup(new LinkedHashSet<>(sg.jobIds)));
                    dst.pathIds.addAll(sg.pathIds);
                    if (dst.rootSetId == null) dst.rootSetId = g.rootSetId;
                }
            }

            List<JobSetGroup> nextArea = new ArrayList<>(aggregatedForNextArea.values());
            currentGroupsArea = nextArea;
            currentGroupsEdge = new ArrayList<>(aggregatedForNextEdge.values());
            plateIdx++;
        }
    }

    // Hilfsfunktion: geklonte Platte (Template)
    private static Plate clonePlate(Plate base) {
        Plate p = new Plate(base.name, base.width, base.height, base.plateId);
        p.name = base.name;
        return p;
    }

    /**
     * Gibt eine Strategie-Code-Matrix auf die Konsole aus.
     * - Spalten = ALLE Jobs (globale Reihenfolge): zuerst wie im repräsentativen Pfad platziert, restliche Job-IDs numerisch.
     * - Zellen: W/H (+r für rotiert) oder "--" wenn im Pfad nicht platziert.
     * - Herkunft: nur am Fork-Job des Pfads "P<parentPathId>", vorherige (vererbte) Jobs bleiben "--".
     */
    private static void printStrategyCodeMatrix(List<MultiPlate_DataClasses> allPaths, String title, List<Integer> desiredOrder) {
        if (allPaths == null || allPaths.isEmpty()) return;

        Map<String, MultiPlate_DataClasses> byId = new HashMap<>();
        List<MultiPlate_DataClasses> candidates = new ArrayList<>();
        for (MultiPlate_DataClasses p : allPaths) {
            if (p != null && p.pathId != null) { byId.put(p.pathId, p); candidates.add(p); }
        }
        if (candidates.isEmpty()) return;

        // Jobs pro Pfad nach placementOrder sammeln
        List<List<Job>> jobsPerPath = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            MultiPlate_DataClasses path = candidates.get(i);
            List<Job> jobsLoc = new ArrayList<>(path.plate.jobs);
            jobsLoc.sort(Comparator.comparingInt(j -> j.placementOrder));
            jobsPerPath.add(jobsLoc);
        }

        // Globale Job-ID-Menge: placed ∪ failed
        LinkedHashSet<Integer> allJobIds = new LinkedHashSet<>();
        for (int i = 0; i < candidates.size(); i++) {
            for (Job j : jobsPerPath.get(i)) allJobIds.add(j.id);
            List<Integer> failed = getFailedJobsReflect(candidates.get(i));
            if (failed != null) allJobIds.addAll(failed);
        }

        // Spaltenreihenfolge bestimmen: exakt gewünschte Sortier-Reihenfolge, fehlende ggf. hinten anfügen
        List<Integer> colJobIds = new ArrayList<>();
        if (desiredOrder != null && !desiredOrder.isEmpty()) {
            for (Integer id : desiredOrder) if (id != null) colJobIds.add(id);
            for (Integer id : allJobIds) if (!colJobIds.contains(id)) colJobIds.add(id);
        } else {
            // Fallback: bisherige Logik (repräsentativer Pfad, dann Rest numerisch)
            int repIndex = -1, repCount = -1;
            for (int i = 0; i < candidates.size(); i++) {
                int sz = jobsPerPath.get(i).size();
                if (sz > repCount) { repCount = sz; repIndex = i; }
            }
            if (repIndex >= 0) for (Job j : jobsPerPath.get(repIndex)) colJobIds.add(j.id);
            List<Integer> rest = new ArrayList<>();
            for (Integer id : allJobIds) if (!colJobIds.contains(id)) rest.add(id);
            Collections.sort(rest);
            colJobIds.addAll(rest);
        }
        int maxCols = Math.max(1, colJobIds.size());

        // Kopfzeile
        StringBuilder header = new StringBuilder();
        header.append("\n").append(title).append("\n");
        header.append(String.format("%-14s | ", "Pfad"));
        for (int i = 0; i < maxCols; i++) {
            String lbl = "J" + colJobIds.get(i);
            if (lbl.length() > 4) lbl = lbl.substring(0, 4);
            header.append(String.format("%-4s", lbl));
        }
        System.out.println(header.toString());

        // Trenner
        StringBuilder sep = new StringBuilder();
        sep.append("------------------");
        for (int i = 0; i < maxCols; i++) sep.append("----");
        System.out.println(sep.toString());

        // Map JobId -> Spaltenindex
        Map<Integer, Integer> colIndexByJobId = new HashMap<>();
        for (int i = 0; i < colJobIds.size(); i++) colIndexByJobId.put(colJobIds.get(i), i);

        // Für jeden Pfad: Strategie-Zeile + Herkunft-Zeile (nur am Fork-Job)
        for (int idx = 0; idx < candidates.size(); idx++) {
            MultiPlate_DataClasses path = candidates.get(idx);
            List<Job> jobsLoc = jobsPerPath.get(idx);

            String[] stratCells = new String[maxCols];
            Arrays.fill(stratCells, "--");
            for (Job j : jobsLoc) {
                Integer ci = colIndexByJobId.get(j.id);
                if (ci == null || ci < 0 || ci >= maxCols) continue;
                String code;
                if (j.splittingMethod == null) {
                    code = j.rotated ? "?r" : "? ";
                } else if ("FullHeight".equalsIgnoreCase(j.splittingMethod)) {
                    code = j.rotated ? "Hr" : "H ";
                } else if ("FullWidth".equalsIgnoreCase(j.splittingMethod)) {
                    code = j.rotated ? "Wr" : "W ";
                } else {
                    code = j.rotated ? "?r" : "? ";
                }
                stratCells[ci] = code;
            }
            StringBuilder rowStrategy = new StringBuilder();
            rowStrategy.append(String.format("%-14s | ", ("Pfad " + path.pathId)));
            for (int i = 0; i < maxCols; i++) rowStrategy.append(String.format("%-4s", stratCells[i]));
            System.out.println(rowStrategy.toString());

            String[] originCells = new String[maxCols];
            Arrays.fill(originCells, "--");
            String parentId = getParentPathIdReflect(path);
            Integer forkJobId = getForkJobIdReflect(path);
            if (parentId != null && forkJobId != null) {
                Integer ci = colIndexByJobId.get(forkJobId);
                if (ci != null && ci >= 0 && ci < maxCols) originCells[ci] = ("P" + parentId);
            }
            StringBuilder rowOrigin = new StringBuilder();
            rowOrigin.append(String.format("%-14s | ", "Herkunft"));
            for (int i = 0; i < maxCols; i++) {
                String display = originCells[i];
                if (display == null) display = "--";
                if (display.length() > 4) display = display.substring(display.length() - 4);
                rowOrigin.append(String.format("%-4s", display));
            }
            System.out.println(rowOrigin.toString());
        }

        System.out.println();
    }

    // Reflection-Helpers: lese Parent-Id / Fork-JobId (mehrere mögliche Feldnamen)
    private static String getParentPathIdReflect(MultiPlate_DataClasses p) {
        if (p == null) return null;
        String[] names = { "parentPathId", "parentId", "previousPathId", "originParentPathId", "parentPath", "parent" };
        for (String n : names) {
            String v = tryGetStringField(p, n);
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private static Integer getForkJobIdReflect(MultiPlate_DataClasses p) {
        if (p == null) return null;
        String[] namesInt = { "forkJobId", "startFromJobId", "branchFromJobId", "copiedAtJobId", "splitFromJobId", "forkJob" };
        for (String n : namesInt) {
            Integer v = tryGetIntField(p, n);
            if (v != null) return v;
        }
        return null;
    }

    private static List<Integer> getFailedJobsReflect(MultiPlate_DataClasses p) {
        if (p == null) return null;
        try {
            java.lang.reflect.Field f = p.getClass().getDeclaredField("failedJobs");
            f.setAccessible(true);
            Object v = f.get(p);
            if (v instanceof List<?>) {
                List<?> raw = (List<?>) v;
                List<Integer> out = new ArrayList<>();
                for (Object o : raw) {
                    if (o instanceof Number) out.add(((Number) o).intValue());
                    else if (o != null) out.add(Integer.parseInt(String.valueOf(o)));
                }
                return out;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static String tryGetStringField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(obj);
            return (v == null) ? null : String.valueOf(v);
        } catch (Exception ignored) { return null; }
    }

    private static Integer tryGetIntField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignored) { }
        return null;
    }

    // ...existing code...
}