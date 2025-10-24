package org.example.Algorithm;

import org.example.DataClasses.Job;
import org.example.DataClasses.PlatePath;
import org.example.DataClasses.Plate;
import org.example.HelperClasses.JobsSetup;
import org.example.Visualizer.BenchmarkVisualizer;

import java.util.*;

public class Controller {

    private static final boolean ENABLE_CONSOLE_OUTPUTS_JOBSETS = false;

    // Stores the best Plate 2 result per job-set label to show as a subline in the summary view.
    private static final Map<String, BenchmarkVisualizer.BenchmarkResult> BEST_PLATE2_BY_JOBSET = new LinkedHashMap<>();

    /*
     * Infinity Plate Algorithm: The plate is cloned infinitely until all jobs are placed.
     */
    public static void runAlgorithm(List<Job> originalJobs, Plate originalPlate) {
        if (originalPlate == null || originalJobs == null || originalJobs.isEmpty()) return;

        // Reset state for a fresh run
        BEST_PLATE2_BY_JOBSET.clear();

        // Collect all job IDs: [1, 2, 3, 4, 5, ...]
        Set<Integer> allJobIds = new LinkedHashSet<>();
        for (Job j : originalJobs) allJobIds.add(j.id);

        // Plate 1: Both Sortings (Area and Edge)
        PlateRunResult resultArea = runPlateWithSorting(originalJobs, originalPlate, "Fläche", allJobIds);
        PlateRunResult resultEdge = runPlateWithSorting(originalJobs, originalPlate, "Kante", allJobIds);

        // #region Strategy-Code-Matrix-output
        List<Integer> orderArea = new ArrayList<>();
        for (Job j : resultArea.sortedJobs) orderArea.add(j.id);
        printStrategyCodeMatrix(resultArea.paths, "Platte 1 - Strategie-Matrix (Fläche)", orderArea);
        
        List<Integer> orderEdge = new ArrayList<>();
        for (Job j : resultEdge.sortedJobs) orderEdge.add(j.id);
        printStrategyCodeMatrix(resultEdge.paths, "Platte 1 - Strategie-Matrix (Kante)", orderEdge);
        // #endregion

        // P1 anzeigen
        List<BenchmarkVisualizer.BenchmarkResult> combinedP1 = new ArrayList<>();
        combinedP1.addAll(resultArea.benchmarks);
        combinedP1.addAll(resultEdge.benchmarks);
        // Markiere alle Reihen als Hauptzeilen (keine Unterzeilen)
        for (BenchmarkVisualizer.BenchmarkResult br : combinedP1) {
            if (br != null) br.isSubRow = false;
        }
        BenchmarkVisualizer.showBenchmarkResults(combinedP1, "Platte 1 - Gesamtlauf");

        // Folgeplatten
        processFollowUpPlates(originalJobs, originalPlate, resultArea.groups, resultEdge.groups, 1);

        // Zusammenfassung
        showSummaryOfJobSets(resultArea, resultEdge);
    }

    private static PlateRunResult runPlateWithSorting(List<Job> originalJobs, Plate plate, 
                                                     String sortType, Set<Integer> allJobIds) {

        List<Job> jobs = JobsSetup.createJobCopies(originalJobs);

        Algorithm algo = new Algorithm(Arrays.asList(clonePlate(plate)));
        for (Job job : jobs) algo.placeJob(job, plate.plateId);
        List<PlatePath> paths = algo.getPathsAndFailedJobsOverviewData();

        List<JobSetGroup> groups = buildUnplacedJobGroups(paths, allJobIds);
        printJobSetGroups(groups, 1, sortType);

        List<BenchmarkVisualizer.BenchmarkResult> benchmarks = buildBenchmarkResults(paths, jobs, Arrays.asList(plate));
        
        // Set labels und rootSetIds
        Map<String, String> rootByPath = new HashMap<>();
        for (JobSetGroup g : groups) {
            for (String pid : g.pathIds) rootByPath.put(pid, g.rootSetId);
        }

        for (int i = 0; i < benchmarks.size(); i++) {
            BenchmarkVisualizer.BenchmarkResult br = benchmarks.get(i);
            br.sortLabel = "Platte 1";
            br.sortedBy = sortType;
            
            PlatePath path = findPathById(paths, br.algorithmName);
            if (path != null && path.failedJobs != null) {
                List<Integer> failed = new ArrayList<>(path.failedJobs);
                Collections.sort(failed);
                br.jobSetLabel = failed.toString();
            } else {
                br.jobSetLabel = "-";
            }

            String pid = extractPathId(br.algorithmName);
            br.rootSetId = pid != null ? rootByPath.getOrDefault(pid, "-") : "-";

            if (br.perPlateSetLabels == null) br.perPlateSetLabels = new ArrayList<>();
            if (br.perPlateSetLabels.isEmpty()) br.perPlateSetLabels.add(br.rootSetId == null ? "-" : br.rootSetId);
            else br.perPlateSetLabels.set(0, br.rootSetId == null ? "-" : br.rootSetId);
        }

        return new PlateRunResult(jobs, paths, groups, benchmarks);
    }

    // Helper: Verarbeitet Folgeplatten
    private static void processFollowUpPlates(List<Job> originalJobs, Plate plateTemplate,
                                              List<JobSetGroup> initialGroupsArea, List<JobSetGroup> initialGroupsEdge,
                                              int startPlateIdx) {
        List<JobSetGroup> currentGroupsArea = initialGroupsArea;
        List<JobSetGroup> currentGroupsEdge = initialGroupsEdge;
        int plateIdx = startPlateIdx;

        while (true) {
            if (currentGroupsArea.isEmpty() && currentGroupsEdge.isEmpty()) break;
            
            Plate currentPlate = clonePlate(plateTemplate);
            FollowUpResult result = processFollowUpPlate(originalJobs, currentPlate,
                                                         currentGroupsArea, currentGroupsEdge, plateIdx);
            
            currentGroupsArea = result.nextGroupsArea;
            currentGroupsEdge = result.nextGroupsEdge;
            plateIdx++;
        }
    }

    // Helper: Verarbeitet eine einzelne Folgeplatte
    private static FollowUpResult processFollowUpPlate(List<Job> originalJobs, Plate currentPlate,
                                                        List<JobSetGroup> groupsArea, List<JobSetGroup> groupsEdge,
                                                        int plateIdx) {
        Map<String, JobSetGroup> aggregatedForNextArea = new LinkedHashMap<>();
        Map<String, JobSetGroup> aggregatedForNextEdge = new LinkedHashMap<>();

        // Verarbeite beide Sortierungen
        processJobSetGroups(originalJobs, currentPlate, groupsArea, true, plateIdx, aggregatedForNextArea);
        processJobSetGroups(originalJobs, currentPlate, groupsEdge, false, plateIdx, aggregatedForNextEdge);

        List<JobSetGroup> nextArea = new ArrayList<>(aggregatedForNextArea.values());
        List<JobSetGroup> nextEdge = new ArrayList<>(aggregatedForNextEdge.values());

        return new FollowUpResult(nextArea, nextEdge);
    }

    // Helper: Verarbeitet Job-Set-Gruppen für eine Sortierung
    private static void processJobSetGroups(List<Job> originalJobs, Plate currentPlate,
                                             List<JobSetGroup> groups, boolean byArea, int plateIdx,
                                             Map<String, JobSetGroup> aggregatedForNext) {
        String seedSuffix = byArea ? "" : " | Seed=P1-Kante";

        for (JobSetGroup g : groups) {
            if (g.jobIds.isEmpty()) continue;

            List<Integer> idsSorted = new ArrayList<>(g.jobIds);
            Collections.sort(idsSorted);
            String setLabel = idsSorted.toString();

            // Beide Sortierungen durchführen
            JobSetRunResult areaResult = runJobSetWithSorting(originalJobs, currentPlate, g, true);
            JobSetRunResult edgeResult = runJobSetWithSorting(originalJobs, currentPlate, g, false);

            // Benchmarks aktualisieren
            updateBenchmarksForJobSet(areaResult.benchmarks, setLabel, "Fläche", g.rootSetId, plateIdx);
            updateBenchmarksForJobSet(edgeResult.benchmarks, setLabel, "Kante", g.rootSetId, plateIdx);

            // Benchmarks anzeigen
            List<BenchmarkVisualizer.BenchmarkResult> combinedSet = new ArrayList<>();
            combinedSet.addAll(areaResult.benchmarks);
            combinedSet.addAll(edgeResult.benchmarks);
            for (BenchmarkVisualizer.BenchmarkResult br : combinedSet) {
                if (br != null) br.isSubRow = false;
            }
            BenchmarkVisualizer.showBenchmarkResults(combinedSet, 
                "Platte " + (plateIdx + 1) + " - Job-Set: " + setLabel + seedSuffix);

            // If we are on Plate 2 (index 1), remember the best result for this job-set for later summary sublines
            if (plateIdx == 1) {
                BenchmarkVisualizer.BenchmarkResult bestP2 = null;
                for (BenchmarkVisualizer.BenchmarkResult br : combinedSet) {
                    if (br == null) continue;
                    if (bestP2 == null || br.coverageRate > bestP2.coverageRate) bestP2 = br;
                }
                if (bestP2 != null) {
                    // store a shallow copy with adjusted labeling to indicate Plate 2
                    BenchmarkVisualizer.BenchmarkResult copy = cloneBenchmarkResult(bestP2);
                    copy.sortLabel = "Platte 2";
                    if (copy.jobSetLabel == null || "-".equals(copy.jobSetLabel)) copy.jobSetLabel = setLabel;
                    BEST_PLATE2_BY_JOBSET.put(setLabel, copy);
                }
            }

            // Aggregiere Gruppen für nächste Platte (nur basierend auf Seed-Sortierung)
            List<PlatePath> pathsForNext = byArea ? areaResult.paths : edgeResult.paths;
            List<JobSetGroup> subGroups = buildUnplacedJobGroups(pathsForNext, g.jobIds);
            
            for (JobSetGroup sg : subGroups) {
                if (sg.jobIds.isEmpty()) continue;
                
                List<Integer> idsSortedSub = new ArrayList<>(sg.jobIds);
                Collections.sort(idsSortedSub);
                String key = idsSortedSub.toString();
                
                JobSetGroup dst = aggregatedForNext.computeIfAbsent(key, 
                    k -> new JobSetGroup(new LinkedHashSet<>(sg.jobIds)));
                dst.pathIds.addAll(sg.pathIds);
                if (dst.rootSetId == null) dst.rootSetId = g.rootSetId;
            }
        }
    }

    // Helper: Führt einen Job-Set-Lauf mit einer Sortierung durch
    private static JobSetRunResult runJobSetWithSorting(List<Job> originalJobs, Plate currentPlate, 
                                                         JobSetGroup group, boolean byArea) {
        List<Job> subset = getJobsSubsetForIds(originalJobs, group.jobIds);

        Algorithm algo = new Algorithm(Arrays.asList(currentPlate));
        for (Job j : subset) algo.placeJob(j, currentPlate.plateId);
        List<PlatePath> paths = algo.getPathsAndFailedJobsOverviewData();

        List<BenchmarkVisualizer.BenchmarkResult> benchmarks = 
            buildBenchmarkResults(paths, subset, Arrays.asList(currentPlate));

        return new JobSetRunResult(paths, benchmarks);
    }

    // Helper: Aktualisiert Benchmark-Ergebnisse für ein Job-Set
    private static void updateBenchmarksForJobSet(List<BenchmarkVisualizer.BenchmarkResult> benchmarks,
                                                   String setLabel, String sortedBy, String rootSetId, int plateIdx) {
        for (BenchmarkVisualizer.BenchmarkResult br : benchmarks) {
            br.sortLabel = "Set " + setLabel;
            br.jobSetLabel = setLabel;
            br.sortedBy = sortedBy;
            br.rootSetId = rootSetId;
            
            if (br.perPlateSetLabels == null) br.perPlateSetLabels = new ArrayList<>();
            ensureSize(br.perPlateSetLabels, plateIdx + 1);
            br.perPlateSetLabels.set(0, rootSetId == null ? "-" : rootSetId);
            br.perPlateSetLabels.set(plateIdx, setLabel);
        }
    }

    // Helper: Zeigt Zusammenfassung der Job-Sets
    private static void showSummaryOfJobSets(PlateRunResult resultArea, PlateRunResult resultEdge) {
        List<List<BenchmarkVisualizer.BenchmarkResult>> allJobSetResults = new ArrayList<>();
        
        collectJobSetResults(resultArea.groups, resultArea.benchmarks, allJobSetResults);
        collectJobSetResults(resultEdge.groups, resultEdge.benchmarks, allJobSetResults);
        
        showSummaryOfBestBenchmarks(allJobSetResults);
    }

    // Helper: Sammelt Benchmark-Ergebnisse für Job-Sets
    private static void collectJobSetResults(List<JobSetGroup> groups, 
                                              List<BenchmarkVisualizer.BenchmarkResult> benchmarks,
                                              List<List<BenchmarkVisualizer.BenchmarkResult>> allJobSetResults) {
        for (JobSetGroup g : groups) {
            List<Integer> idsSorted = new ArrayList<>(g.jobIds);
            Collections.sort(idsSorted);
            String setLabel = idsSorted.toString();
            
            List<BenchmarkVisualizer.BenchmarkResult> jobSetResults = new ArrayList<>();
            for (BenchmarkVisualizer.BenchmarkResult br : benchmarks) {
                if (br.jobSetLabel == null || "-".equals(br.jobSetLabel)) {
                    br.jobSetLabel = setLabel;
                }
                if (setLabel.equals(br.jobSetLabel)) {
                    jobSetResults.add(br);
                }
            }
            
            if (!jobSetResults.isEmpty()) {
                allJobSetResults.add(jobSetResults);
            }
        }
    }

    // Helper: Findet einen Pfad anhand des Algorithmusnamens
    private static PlatePath findPathById(List<PlatePath> paths, String algorithmName) {
        for (PlatePath p : paths) {
            if (p != null && ("Pfad " + p.pathId).equals(algorithmName)) {
                return p;
            }
        }
        return null;
    }

    // Helper-Klassen für Ergebnisse
    private static class PlateRunResult {
        final List<Job> sortedJobs;
        final List<PlatePath> paths;
        final List<JobSetGroup> groups;
        final List<BenchmarkVisualizer.BenchmarkResult> benchmarks;

        PlateRunResult(List<Job> sortedJobs, List<PlatePath> paths, List<JobSetGroup> groups,
                       List<BenchmarkVisualizer.BenchmarkResult> benchmarks) {
            this.sortedJobs = sortedJobs;
            this.paths = paths;
            this.groups = groups;
            this.benchmarks = benchmarks;
        }
    }

    private static class JobSetRunResult {
        final List<PlatePath> paths;
        final List<BenchmarkVisualizer.BenchmarkResult> benchmarks;

        JobSetRunResult(List<PlatePath> paths, List<BenchmarkVisualizer.BenchmarkResult> benchmarks) {
            this.paths = paths;
            this.benchmarks = benchmarks;
        }
    }

    private static class FollowUpResult {
        final List<JobSetGroup> nextGroupsArea;
        final List<JobSetGroup> nextGroupsEdge;

        FollowUpResult(List<JobSetGroup> nextGroupsArea, List<JobSetGroup> nextGroupsEdge) {
            this.nextGroupsArea = nextGroupsArea;
            this.nextGroupsEdge = nextGroupsEdge;
        }
    }

    /*
     * Gruppenbildung für unplatzierte Jobs. 
     * Unterschiedliche Pfade, die aber die gleichen failed Jobs haben, gehören zur selben Gruppe.
     */
    private static List<JobSetGroup> buildUnplacedJobGroups(List<PlatePath> paths, Set<Integer> allJobIds) {
        Map<String, JobSetGroup> map = new LinkedHashMap<>();
        for (PlatePath p : paths) {
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


    /*
     * Hilfsmethode: Filtert von originalJobs nur die mit den angegebenen IDs heraus
     */
    private static List<Job> getJobsSubsetForIds(List<Job> originalJobs, Set<Integer> ids) {
        Map<Integer, Job> byId = new HashMap<>(); for (Job j : originalJobs) byId.put(j.id, j);
        List<Job> out = new ArrayList<>();
        for (Integer id : ids) {
            Job o = byId.get(id);
            if (o != null) out.add(new Job(o.id, o.width, o.height));
        }
        return out;
    }

    /*
     * Nach jeder Platte werden alle failed Jobs in Job-Sets gruppiert
     */
    private static class JobSetGroup {
        final Set<Integer> jobIds; 
        final List<String> pathIds = new ArrayList<>();
        String rootSetId; // Root-Set-ID von Platte 1
        JobSetGroup(Set<Integer> jobIds) { 
            this.jobIds = jobIds; 
        }
    }

    /*
     * Für Debug-Konsole-Ausgabe
     */
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
    private static java.util.List<BenchmarkVisualizer.BenchmarkResult> buildBenchmarkResults(List<PlatePath> allPaths, List<Job> originalJobs, List<Plate> plateInfos) {
        java.util.List<BenchmarkVisualizer.BenchmarkResult> results = new java.util.ArrayList<>();
        java.util.Map<String, PlatePath> byId = new java.util.HashMap<>();
        for (PlatePath p : allPaths) byId.put(p.pathId, p);
        int totalJobs = originalJobs == null ? 0 : originalJobs.size();
        java.util.Set<String> parentIds = new java.util.HashSet<>();
        for (PlatePath p : allPaths) {
            if (p == null || p.pathId == null) continue;
            int dot = p.pathId.lastIndexOf('.');
            if (dot > 0) parentIds.add(p.pathId.substring(0, dot));
        }
        for (PlatePath p : allPaths) {
            if (p == null || p.plate == null) continue;
            if (parentIds.contains(p.pathId)) continue;
            // Elternkette aufbauen
            java.util.List<PlatePath> chain = new java.util.ArrayList<>();
            String id = p.pathId; String[] parts = id.split("\\."); StringBuilder acc = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) acc.append('.');
                acc.append(parts[i]);
                PlatePath node = byId.get(acc.toString());
                if (node != null && node.plate != null) chain.add(node);
            }
            if (chain.isEmpty()) continue;
            // Aggregation
            double usedSum = 0.0, totalSum = 0.0; int placedSum = 0;
            for (PlatePath node : chain) {
                double used = 0.0; for (Job j : node.plate.jobs) used += (j.width * j.height);
                double total = node.plate.width * node.plate.height;
                usedSum += used; totalSum += total; placedSum += node.plate.jobs.size();
            }
            double covAgg = totalSum > 0 ? (usedSum / totalSum) * 100.0 : 0.0;
            String algoName = "Pfad " + p.pathId;
            BenchmarkVisualizer.BenchmarkResult row = new BenchmarkVisualizer.BenchmarkResult(algoName, p.plate, null, placedSum, covAgg, totalJobs);
            
            // Dynamische Listen
            row.platesRefs = new java.util.ArrayList<>(); row.platesNames = new java.util.ArrayList<>(); row.platesCoverages = new java.util.ArrayList<>(); row.platesFreeRects = new java.util.ArrayList<>();
            for (PlatePath node : chain) {
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
     * PlateVisualizer ruft CutLineCalculator auf und bekommt die Schnittkoordianten zurück
     */
    public static java.util.List<CutLineCalculator.CutLine> computeCutLinesForPlate(Plate plate) {
        return CutLineCalculator.calculateCutLinesForPlate(plate);
    }

    /*
     * Konsolenausgabe: CutLineCalculator aufrufen und Schnitte auf Konsole ausgeben (wenn ENABLE_CONSOLE_OUTPUT_CUTS true ist)
     */
    public static void printCutsAndIntersections(Plate plate, boolean force) {
        if (plate == null) return;
        // Vollständige Schnittliste und Restplatten berechnen
        java.util.List<CutLineCalculator.CutLine> cuts = CutLineCalculator.calculateAllFullCuts(plate);
        java.util.List<CutLineCalculator.ResidualPlate> residuals = CutLineCalculator.calculateResidualPlates(plate);

        // Ausgabe aktiv: immer drucken (Anforderung: alle relevanten Daten in Konsole)
        System.out.println("=== Schnitte und Restplatten für Ursprung '" + plate.name + "' (" + plate.width + " x " + plate.height + ") ===");

        // Schnittkoordinaten der Ursprungsplatte ausgeben
        if (cuts != null && !cuts.isEmpty()) {
            System.out.println("-- Schnittkoordinaten (x-y Paare):");
            for (CutLineCalculator.CutLine c : cuts) {
                if (c.vertical) {
                    // Vertikale Linie: x = c.coord, y von c.start bis c.end
                    System.out.printf("C%02d  V  (%.3f, %.3f) -> (%.3f, %.3f)%n", c.id, c.coord, c.start, c.coord, c.end);
                } else {
                    // Horizontale Linie: y = c.coord, x von c.start bis c.end
                    System.out.printf("C%02d  H  (%.3f, %.3f) -> (%.3f, %.3f)%n", c.id, c.start, c.coord, c.end, c.coord);
                }
            }
        } else {
            System.out.println("(Keine Schnitte)");
        }

        // Restplatten (Größen) ausgeben
        if (residuals != null && !residuals.isEmpty()) {
            System.out.println("-- Restplatten (Größen und Bounds):");
            int idx = 1;
            for (CutLineCalculator.ResidualPlate r : residuals) {
                double w = r.width();
                double h = r.height();
                System.out.printf("R%02d  size=%.3f x %.3f  [x=%.3f..%.3f, y=%.3f..%.3f]  jobs=%d%n",
                        idx++, w, h, r.x0, r.x1, r.y0, r.y1, r.jobCount);
            }
        } else {
            System.out.println("(Keine Restplatten)");
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

    private static void ensureSize(java.util.List<String> list, int size) {
        while (list.size() < size) list.add("-");
    }

    private static Plate clonePlate(Plate base) {
        Plate p = new Plate(base.name, base.width, base.height, base.plateId);
        p.name = base.name;
        return p;
    }

    /**
     * Gibt eine Strategie-Code-Matrix auf die Konsole aus.
     */
    private static void printStrategyCodeMatrix(List<PlatePath> allPaths, String title, List<Integer> desiredOrder) {
        if (allPaths == null || allPaths.isEmpty()) return;

        Map<String, PlatePath> byId = new HashMap<>();
        List<PlatePath> candidates = new ArrayList<>();
        for (PlatePath p : allPaths) {
            if (p != null && p.pathId != null) { byId.put(p.pathId, p); candidates.add(p); }
        }
        if (candidates.isEmpty()) return;

        // Jobs pro Pfad nach placementOrder sammeln
        List<List<Job>> jobsPerPath = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            PlatePath path = candidates.get(i);
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
            PlatePath path = candidates.get(idx);
            List<Job> jobsLoc = jobsPerPath.get(idx);

            String[] stratCells = new String[maxCols];
            Arrays.fill(stratCells, "--");
            for (Job j : jobsLoc) {
                Integer ci = colIndexByJobId.get(j.id);
                if (ci == null || ci < 0 || ci >= maxCols) continue;
                String code;
                if (j.splittingMethod == null) {
                    code = "? ";
                } else if ("FullHeight".equalsIgnoreCase(j.splittingMethod)) {
                    code = "H ";
                } else if ("FullWidth".equalsIgnoreCase(j.splittingMethod)) {
                    code = "W ";
                } else {
                    code = "? ";
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
    private static String getParentPathIdReflect(PlatePath p) {
        if (p == null) return null;
        String[] names = { "parentPathId", "parentId", "previousPathId", "originParentPathId", "parentPath", "parent" };
        for (String n : names) {
            String v = tryGetStringField(p, n);
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private static Integer getForkJobIdReflect(PlatePath p) {
        if (p == null) return null;
        String[] namesInt = { "forkJobId", "startFromJobId", "branchFromJobId", "copiedAtJobId", "splitFromJobId", "forkJob" };
        for (String n : namesInt) {
            Integer v = tryGetIntField(p, n);
            if (v != null) return v;
        }
        return null;
    }

    // Liest die Liste der fehlgeschlagenen Jobs aus einem Pfad (per Reflection)
    private static List<Integer> getFailedJobsReflect(PlatePath p) {
        if (p == null) {
            return null;
        }
        
        try {
            // Hole das Feld "failedJobs"
            java.lang.reflect.Field feld = p.getClass().getDeclaredField("failedJobs");
            // Mache das Feld zugänglich
            feld.setAccessible(true);
            // Lese den Wert aus
            Object wert = feld.get(p);
            
            // Prüfe, ob der Wert eine Liste ist
            if (wert instanceof List<?>) {
                List<?> rohListe = (List<?>) wert;
                List<Integer> ergebnis = new ArrayList<>();
                
                // Gehe durch alle Einträge in der Liste
                for (int i = 0; i < rohListe.size(); i++) {
                    Object eintrag = rohListe.get(i);
                    
                    // Wenn der Eintrag eine Zahl ist, wandle ihn in Integer um
                    if (eintrag instanceof Number) {
                        Number zahl = (Number) eintrag;
                        ergebnis.add(zahl.intValue());
                    } 
                    // Wenn der Eintrag kein null ist, parse ihn als Text
                    else if (eintrag != null) {
                        String text = String.valueOf(eintrag);
                        ergebnis.add(Integer.parseInt(text));
                    }
                }
                
                return ergebnis;
            }
        } catch (Exception fehler) {
            // Bei jedem Fehler nichts tun
        }
        
        return null;
    }

    // Versucht, einen Text-Wert aus einem Feld eines Objekts zu lesen (per Reflection)
    private static String tryGetStringField(Object obj, String fieldName) {
        try {
            // Hole das Feld mit dem angegebenen Namen
            java.lang.reflect.Field feld = obj.getClass().getDeclaredField(fieldName);
            // Mache das Feld zugänglich (auch wenn es private ist)
            feld.setAccessible(true);
            // Lese den Wert aus
            Object wert = feld.get(obj);
            
            // Wenn der Wert null ist, gib null zurück, sonst wandle in Text um
            if (wert == null) {
                return null;
            } else {
                return String.valueOf(wert);
            }
        } catch (Exception fehler) {
            // Bei jedem Fehler gib null zurück
            return null;
        }
    }

    // Versucht, einen Zahlen-Wert aus einem Feld eines Objekts zu lesen (per Reflection)
    private static Integer tryGetIntField(Object obj, String fieldName) {
        try {
            // Hole das Feld mit dem angegebenen Namen
            java.lang.reflect.Field feld = obj.getClass().getDeclaredField(fieldName);
            // Mache das Feld zugänglich (auch wenn es private ist)
            feld.setAccessible(true);
            // Lese den Wert aus
            Object wert = feld.get(obj);
            
            // Wenn der Wert eine Zahl ist, wandle ihn in Integer um
            if (wert instanceof Number) {
                Number zahl = (Number) wert;
                return zahl.intValue();
            }
            
            // Wenn der Wert kein null ist, versuche ihn als Text zu parsen
            if (wert != null) {
                String text = String.valueOf(wert);
                return Integer.parseInt(text);
            }
        } catch (Exception fehler) {
            // Bei jedem Fehler nichts tun
        }
        
        return null;
    }

    /**
     * Zeigt ein zusammenfassendes Benchmark-Fenster mit den jeweils besten Ergebnissen aller Job-Sets.
     * Kreativ: Sortiert nach Bedeckungsrate, zeigt Job-Set, Coverage, Platzierungsanzahl, Strategie etc.
     */
    private static void showSummaryOfBestBenchmarks(List<List<BenchmarkVisualizer.BenchmarkResult>> allJobSetResults) {
        // Map: JobSetLabel -> bestes Ergebnis
        Map<String, BenchmarkVisualizer.BenchmarkResult> bestByJobSet = new LinkedHashMap<>();
        for (List<BenchmarkVisualizer.BenchmarkResult> jobSetResults : allJobSetResults) {
            for (BenchmarkVisualizer.BenchmarkResult res : jobSetResults) {
                String key = res.jobSetLabel != null ? res.jobSetLabel : res.algorithmName;
                BenchmarkVisualizer.BenchmarkResult old = bestByJobSet.get(key);
                if (old == null || res.coverageRate > old.coverageRate) {
                    bestByJobSet.put(key, res);
                }
            }
        }
        // Sortiere nach Coverage absteigend
        List<BenchmarkVisualizer.BenchmarkResult> bestList = new ArrayList<>(bestByJobSet.values());
        bestList.sort((a, b) -> Double.compare(b.coverageRate, a.coverageRate));

        // Füge ein Ranking hinzu und schreibe das Job-Set in die Zeile
        int rank = 1;
        for (BenchmarkVisualizer.BenchmarkResult res : bestList) {
            res.sortLabel = "#" + rank++;
            // Schreibe das Job-Set in den Algorithmusnamen
            if (res.jobSetLabel != null && !"-".equals(res.jobSetLabel)) {
                res.algorithmName = res.algorithmName + "  [Job-Set: " + res.jobSetLabel + "]";
            }
            // Hauptzeile markieren
            res.isSubRow = false;
        }

        // Zeige das beste Ergebniss pro Job-Set von Platte 2 als Unterzeile an
        List<BenchmarkVisualizer.BenchmarkResult> withSubRows = new ArrayList<>();
        for (BenchmarkVisualizer.BenchmarkResult res : bestList) {
            withSubRows.add(res);
            String key = res.jobSetLabel != null ? res.jobSetLabel : res.algorithmName;
            BenchmarkVisualizer.BenchmarkResult p2 = BEST_PLATE2_BY_JOBSET.get(key);
            if (p2 != null) {
                BenchmarkVisualizer.BenchmarkResult sub = cloneBenchmarkResult(p2);
                sub.algorithmName = "       Platte 2: " + sub.algorithmName;
                   sub.sortedBy = res.sortedBy;
                sub.isSubRow = true;
                withSubRows.add(sub);
            }
        }
        // Vorab: Für jede Zeile Schnitte berechnen und auf Konsole ausgeben (inkl. freie Rechtecke)
        System.out.println("\n>>> Vorberechnete Ausgaben: Die besten Ergebnisse pro Job-Set");
        for (BenchmarkVisualizer.BenchmarkResult row : withSubRows) {
            try {
                printPrecomputedCutsForBenchmarkRow(row);
            } catch (Throwable t) {
                System.out.println("[Warnung] Vorab-Ausgabe fehlgeschlagen für Zeile: " + (row==null?"null":row.algorithmName));
                t.printStackTrace();
            }
        }
        BenchmarkVisualizer.showBenchmarkResults(withSubRows, "Die besten Ergebnisse pro Job-Set");
    }

    // Druckt für eine Benchmark-Zeile die vollständigen Schnitte und (falls vorhanden) die freien Rechtecke
    private static void printPrecomputedCutsForBenchmarkRow(BenchmarkVisualizer.BenchmarkResult res) {
        if (res == null) return;
        String header = (res.isSubRow ? "(Sub) " : "") + (res.algorithmName == null ? "(ohne Name)" : res.algorithmName);
        String setInfo = (res.jobSetLabel == null || "-".equals(res.jobSetLabel)) ? "" : (" | Job-Set: " + res.jobSetLabel);
        System.out.println("\n=== Vorab: " + header + setInfo + " ===");

        if (res.platesRefs != null && !res.platesRefs.isEmpty()) {
            for (int i = 0; i < res.platesRefs.size(); i++) {
                Plate p = res.platesRefs.get(i);
                String plateName = (p != null ? (p.name + " (" + p.width + " x " + p.height + ")") : "null");
                System.out.println("-- Platte " + (i + 1) + ": " + plateName);
                if (p != null) {
                    printCutsAndIntersections(p, true);
                    printFreeRectanglesForIndex(res, i);
                }
            }
        } else if (res.plate != null) {
            Plate p = res.plate;
            System.out.println("-- Platte: " + p.name + " (" + p.width + " x " + p.height + ")");
            printCutsAndIntersections(p, true);
            // Einzelplatte: spezifische freie Rechtecke, falls vorhanden
            if (res.specificFreeRects != null && !res.specificFreeRects.isEmpty()) {
                System.out.println("-- Freie Rechtecke (" + res.specificFreeRects.size() + "): ");
                for (PlatePath.FreeRectangle fr : res.specificFreeRects) {
                    System.out.printf("FR  [x=%.3f..%.3f, y=%.3f..%.3f]  size=%.3f x %.3f%n", fr.x, fr.x + fr.width, fr.y, fr.y + fr.height, fr.width, fr.height);
                }
            } else {
                printFreeRectanglesForIndex(res, 0);
            }
        } else {
            System.out.println("(Keine Platte hinterlegt)");
        }
    }

    // Gibt freie Rechtecke für den i-ten Platteneintrag einer Benchmark-Zeile aus (soweit vorhanden)
    private static void printFreeRectanglesForIndex(BenchmarkVisualizer.BenchmarkResult res, int index) {
        if (res == null || res.platesFreeRects == null || index < 0 || index >= res.platesFreeRects.size()) return;
        java.util.List<?> list = res.platesFreeRects.get(index);
        if (list == null || list.isEmpty()) return;
        System.out.println("-- Freie Rechtecke (" + list.size() + "): ");
        for (Object o : list) {
            if (o instanceof PlatePath.FreeRectangle) {
                PlatePath.FreeRectangle fr = (PlatePath.FreeRectangle) o;
                System.out.printf("FR  [x=%.3f..%.3f, y=%.3f..%.3f]  size=%.3f x %.3f%n", fr.x, fr.x + fr.width, fr.y, fr.y + fr.height, fr.width, fr.height);
            } else {
                // Fallback: versuche via Reflection Standardfelder zu lesen
                Double x = tryGetDoubleField(o, "x");
                Double y = tryGetDoubleField(o, "y");
                Double w = tryGetDoubleField(o, "width");
                Double h = tryGetDoubleField(o, "height");
                if (x != null && y != null && w != null && h != null) {
                    System.out.printf("FR  [x=%.3f..%.3f, y=%.3f..%.3f]  size=%.3f x %.3f%n", x, x + w, y, y + h, w, h);
                } else {
                    System.out.println(String.valueOf(o));
                }
            }
        }
    }

    // Versucht, einen Double-Wert aus einem Feld eines Objekts zu lesen (per Reflection)
    private static Double tryGetDoubleField(Object obj, String fieldName) {
        try {
            if (obj == null || fieldName == null || fieldName.isEmpty()) return null;
            java.lang.reflect.Field feld = null;
            Class<?> c = obj.getClass();
            while (c != null) {
                try { feld = c.getDeclaredField(fieldName); break; } catch (NoSuchFieldException ignore) { c = c.getSuperclass(); }
            }
            if (feld == null) return null;
            feld.setAccessible(true);
            Object wert = feld.get(obj);
            if (wert instanceof Number) return ((Number) wert).doubleValue();
            if (wert != null) {
                String text = String.valueOf(wert);
                return Double.parseDouble(text);
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private static BenchmarkVisualizer.BenchmarkResult cloneBenchmarkResult(BenchmarkVisualizer.BenchmarkResult src) {
        BenchmarkVisualizer.BenchmarkResult dst = new BenchmarkVisualizer.BenchmarkResult(
                src.algorithmName,
                src.plate,
                src.algorithm,
                src.placedJobs,
                src.coverageRate,
                src.totalJobs,
                src.specificFreeRects
        );
        dst.platesRefs = src.platesRefs == null ? null : new ArrayList<>(src.platesRefs);
        dst.platesNames = src.platesNames == null ? null : new ArrayList<>(src.platesNames);
        dst.platesCoverages = src.platesCoverages == null ? null : new ArrayList<>(src.platesCoverages);
        dst.platesFreeRects = src.platesFreeRects == null ? null : new ArrayList<>(src.platesFreeRects);
        dst.sortLabel = src.sortLabel;
        dst.rootSetId = src.rootSetId;
        dst.sortedBy = src.sortedBy;
        dst.jobSetLabel = src.jobSetLabel;
        dst.perPlateSetLabels = src.perPlateSetLabels == null ? null : new ArrayList<>(src.perPlateSetLabels);
        dst.isSubRow = src.isSubRow;
        return dst;
    }

}