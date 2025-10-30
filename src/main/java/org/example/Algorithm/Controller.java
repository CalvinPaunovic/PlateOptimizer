package org.example.Algorithm;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.PlatePath;
import org.example.DataClasses.Plate;
import org.example.HelperClasses.JobsSetup;
import org.example.Visualizer.BenchmarkVisualizer;

import java.util.*;



public class Controller {

    private static final int startPlateId = 1;

    private static final boolean ENABLE_CONSOLE_OUTPUTS_JOBSETS = false;

    // Stores the best Plate 2 result per job-set label to show as a subline in the summary view.
    private static final Map<String, BenchmarkVisualizer.BenchmarkResult> BEST_PLATE2_BY_JOBSET = new LinkedHashMap<>();

    /*
     * Infinity Plate Algorithm: The plate is cloned infinitely until all jobs are placed.
     */
    public static void runAlgorithm(List<Main.SortedJobsVariant> sortedJobsList, Plate originalPlate) {
        if (originalPlate == null || sortedJobsList == null || sortedJobsList.isEmpty()) return;

        // Reset state for a fresh run
        BEST_PLATE2_BY_JOBSET.clear();

        // Collect all job IDs: [1, 2, 3, 4, 5, ...]
        Set<Integer> allJobIds = new LinkedHashSet<>();
        for (Main.SortedJobsVariant variant : sortedJobsList) {
            for (Job j : variant.jobs()) allJobIds.add(j.id);
        }

        // Iterate over all sorting variants and place jobs on the first plate
        for (int i = 0; i < sortedJobsList.size(); i++) {
            Main.SortedJobsVariant variant = sortedJobsList.get(i);
            String sortType = variant.label();
            List<Job> jobs = variant.jobs();
            PlateRunResult result = placeJobsFirstPlate(jobs, originalPlate, sortType, allJobIds);

            // #region Strategy-Code-Matrix-output
            List<Integer> orderArea = new ArrayList<>();
            for (Job j : jobs) orderArea.add(j.id);
            printStrategyCodeMatrix(result.paths, "Platte 1 - Strategie-Matrix (Fläche)", orderArea);
            
            List<Integer> orderEdge = new ArrayList<>();
            for (Job j : jobs) orderEdge.add(j.id);
            printStrategyCodeMatrix(result.paths, "Platte 1 - Strategie-Matrix (Kante)", orderEdge);
            //#endregion

            // Build the first benchmark window (Plate 1)
            List<BenchmarkVisualizer.BenchmarkResult> plateOneBenchmark = new ArrayList<>();
            plateOneBenchmark.addAll(result.benchmarks);

            // Set all rows in Plate 1 as main rows
            for (BenchmarkVisualizer.BenchmarkResult br : plateOneBenchmark) if (br != null) br.isSubRow = false;
            BenchmarkVisualizer.showBenchmarkResults(plateOneBenchmark, "Platte 1 - Gesamtlauf");

            // Process follow-up plates
            generateFollowUpPlates(variant, originalPlate, result.groups);

            // Show summary of job-sets over all plates
             showSummaryOfJobSets(result);
        }
        
        
    }


    /*
     * (Trying) to place all jobs on the first plate with the specified sorting type.
     * Generates job-set groups for unplaced jobs with buildUnplacedJobGroups
     * Set the rootSetIds for each job-set.
     * Provide results for benchmark building.
     */
    private static PlateRunResult placeJobsFirstPlate(List<Job> originalJobs, Plate plate, String sortType, Set<Integer> allJobIds) {

        List<Job> jobs = JobsSetup.createJobCopies(originalJobs);

        Algorithm algo = new Algorithm(Arrays.asList(clonePlate(plate)));
        // Algorithm-class call
        for (Job job : jobs) {
            algo.placeJob(job, plate.plateId);
        }

        // #region Debug-Output of paths and failed jobs
        List<PlatePath> paths = algo.getPathsAndFailedJobsOverviewData();
        // #endregion

        // Build the job-set groups for unplaced jobs
        List<JobSetGroup> groups = buildUnplacedJobGroups(paths, allJobIds);
        
        // Set rootSetIds for groups. The left over job list (after Plate 1 placements) is the root ID.
        for (JobSetGroup g : groups) {
            if (g.rootSetId == null) {
                List<Integer> sorted = new ArrayList<>(g.jobIds);
                Collections.sort(sorted);
                g.rootSetId = sorted.toString();
            }
        }

        //#region Debug-Output of job-sets
        printJobSetGroups(groups, 1, sortType);
        //#endregion

        // Build the benchmark results
        List<BenchmarkVisualizer.BenchmarkResult> benchmarks = buildBenchmarkResults(paths, jobs, Arrays.asList(plate));

        return new PlateRunResult(jobs, paths, groups, benchmarks);
    }


    /*
     * Create new Plates for each job-set until all jobs are placed
     */
    private static void generateFollowUpPlates
        (Main.SortedJobsVariant variant, Plate plateTemplate, List<JobSetGroup> groups) {
        List<JobSetGroup> currentGroup = groups;
        int plateId = startPlateId;

        // For each job-set
        while (true) {
            if (currentGroup.isEmpty()) break;
            
            List<JobSetGroup> nextGroup = new ArrayList<>();

            // Every sorting gets its own plate
            Plate plate = clonePlate(plateTemplate);

            processJobSetGroups(variant, plate, currentGroup, plateId, nextGroup);

            // new value set in processJobSetGroups
            currentGroup = nextGroup;
            plateId++;  // set new plateId
        }
    }


    /*
     * Processes job-set groups with the specified sorting type
     */
    private static void processJobSetGroups(
        Main.SortedJobsVariant variant, Plate currentPlate, List<JobSetGroup> groups, int plateId, List<JobSetGroup> nextGroups) {

        // Go through all job-set groups
        for (JobSetGroup group : groups) {


            JobSetRunResult result = placeLeftoverJobs(variant, currentPlate, group);

            String setLabel = group.jobIds.toString();
            
            // Set labels for benchmarks
            for (BenchmarkVisualizer.BenchmarkResult benchmarkResult : result.benchmarks) {
                benchmarkResult.isSubRow = false;
                benchmarkResult.sortLabel = "Set " + setLabel;
                benchmarkResult.jobSetLabel = setLabel;
                benchmarkResult.sortedBy = variant.label();
                benchmarkResult.rootSetId = group.rootSetId;

                if (benchmarkResult.perPlateSetLabels == null) benchmarkResult.perPlateSetLabels = new ArrayList<>();
                while (benchmarkResult.perPlateSetLabels.size() < plateId + 1) benchmarkResult.perPlateSetLabels.add("-");
                benchmarkResult.perPlateSetLabels.set(0, group.rootSetId == null ? "-" : group.rootSetId);
                benchmarkResult.perPlateSetLabels.set(plateId, setLabel);
            }

            // Show benchmark window for this sorting type
            BenchmarkVisualizer.showBenchmarkResults(result.benchmarks,
                "Plate " + (plateId + 1) + " ___ Sorting: " + variant.label() + " ___ Job-Set: " + group.jobIds.toString() + " ___ Root-Set: " + group.rootSetId);

            // If we are on Plate 2 (index 1), remember the best result for this job-set for later summary sublines
            if (plateId == 1) {
                BenchmarkVisualizer.BenchmarkResult bestP2 = null;
                for (BenchmarkVisualizer.BenchmarkResult br : result.benchmarks) {
                    if (br == null) continue;
                    if (bestP2 == null || br.coverageRate > bestP2.coverageRate) bestP2 = br;
                }
                if (bestP2 != null) {
                    // store a shallow copy with adjusted labeling to indicate Plate 2
                    BenchmarkVisualizer.BenchmarkResult copy = cloneBenchmarkResult(bestP2);
                    copy.sortLabel = "Platte 2";
                    if (copy.jobSetLabel == null || "-".equals(copy.jobSetLabel)) copy.jobSetLabel = group.jobIds.toString();
                    BEST_PLATE2_BY_JOBSET.put(group.jobIds.toString(), copy);
                }
            }

            // Sammle Gruppen für nächste Platte
            List<PlatePath> pathsForNext = result.paths;
            List<JobSetGroup> subGroups = buildUnplacedJobGroups(pathsForNext, group.jobIds);
            
                for (JobSetGroup sg : subGroups) {
                    if (sg.jobIds.isEmpty()) continue;
                
                // Übernehme rootSetId vom Eltern-Set
                if (sg.rootSetId == null) sg.rootSetId = group.rootSetId;
                
                nextGroups.add(sg);
            }
        }
    }


    /*
     * Similar to placeAllJobsFirstPlate
     * (Trying) to place all leftover jobs on the provided plate (this method is called for each sorting type).
     * DO NOT generates job-set groups for unplaced jobs.
     * Provide results for benchmark building.
     */
    private static JobSetRunResult placeLeftoverJobs(Main.SortedJobsVariant variant, Plate currentPlate, JobSetGroup group) {
        List<Job> subset = getJobsSubsetForIds(variant, group.jobIds);

        // Algorithm-class call
        Algorithm algo = new Algorithm(Arrays.asList(currentPlate));
        for (Job j : subset) {
            algo.placeJob(j, currentPlate.plateId);
        }

        // #region Debug-Output of paths and failed jobs
        List<PlatePath> paths = algo.getPathsAndFailedJobsOverviewData();
        // #endregion

        // Build the benchmark results
        List<BenchmarkVisualizer.BenchmarkResult> benchmarks = buildBenchmarkResults(paths, subset, Arrays.asList(currentPlate));

        return new JobSetRunResult(paths, benchmarks);
    }


    // Helper: Zeigt Zusammenfassung der Job-Sets
    private static void showSummaryOfJobSets(PlateRunResult result) {
        List<List<BenchmarkVisualizer.BenchmarkResult>> allJobSetResults = new ArrayList<>();

        collectJobSetResults(result.groups, result.benchmarks, allJobSetResults);

        showSummaryOfBestBenchmarks(allJobSetResults);
    }

    // Helper: Sammelt Benchmark-Ergebnisse für Job-Sets
    private static void collectJobSetResults(List<JobSetGroup> groups, List<BenchmarkVisualizer.BenchmarkResult> benchmarks,List<List<BenchmarkVisualizer.BenchmarkResult>> allJobSetResults) {
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


    /*
     * Create groups for unplaced jobs.
     * Different paths that have the same failed jobs belong to the same group.
     */
    private static List<JobSetGroup> buildUnplacedJobGroups(List<PlatePath> paths, Set<Integer> allJobIds) {
        Map<String, JobSetGroup> map = new LinkedHashMap<>();
        for (PlatePath p : paths) {
            if (p == null || p.plate == null) continue;
            Set<Integer> placed = new LinkedHashSet<>(); for (Job j : p.plate.jobs) placed.add(j.id);
            Set<Integer> unplaced = new LinkedHashSet<>(); for (Integer id : allJobIds) if (!placed.contains(id)) unplaced.add(id);
            List<Integer> sorted = new ArrayList<>(unplaced); Collections.sort(sorted);
            String key = sorted.toString();
            JobSetGroup g = map.computeIfAbsent(key, k -> new JobSetGroup(new LinkedHashSet<>(unplaced)));
            g.pathIds.add(p.pathId);
        }
        return new ArrayList<>(map.values());
    }


    /*
     * Hilfsmethode: Filtert von originalJobs nur die mit den angegebenen IDs heraus
     */
    private static List<Job> getJobsSubsetForIds(Main.SortedJobsVariant variant, Set<Integer> ids) {
        Map<Integer, Job> byId = new HashMap<>();
        for (Job j : variant.jobs()) {
            byId.put(j.id, j);
        }
        List<Job> out = new ArrayList<>();
        for (Integer id : ids) {
            Job o = byId.get(id);
            if (o != null) out.add(new Job(o.id, o.width, o.height));
        }
        return out;
    }



    /*
     * Build benchmark results
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
            java.util.List<PlatePath> chain = new java.util.ArrayList<>();
            String id = p.pathId; String[] parts = id.split("\\."); StringBuilder acc = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) acc.append('.');
                acc.append(parts[i]);
                PlatePath node = byId.get(acc.toString());
                if (node != null && node.plate != null) chain.add(node);
            }
            if (chain.isEmpty()) continue;
            double usedSum = 0.0, totalSum = 0.0; int placedSum = 0;
            for (PlatePath node : chain) {
                double used = 0.0; for (Job j : node.plate.jobs) used += (j.width * j.height);
                double total = node.plate.width * node.plate.height;
                usedSum += used; totalSum += total; placedSum += node.plate.jobs.size();
            }
            double covAgg = totalSum > 0 ? (usedSum / totalSum) * 100.0 : 0.0;
            String algoName = "Pfad " + p.pathId;
            BenchmarkVisualizer.BenchmarkResult row = new BenchmarkVisualizer.BenchmarkResult(algoName, p.plate, null, placedSum, covAgg, totalJobs);
            
            if (p.failedJobs != null) {
                List<Integer> failed = new ArrayList<>(p.failedJobs);
                Collections.sort(failed);
                row.jobSetLabel = failed.toString();
            } else {
                row.jobSetLabel = "-";
            }

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



    /**
     * Zeigt ein zusammenfassendes Benchmark-Fenster mit den jeweils besten Ergebnissen aller Job-Sets.
     * Sortiert nach Bedeckungsrate, zeigt Job-Set, Coverage, Platzierungsanzahl, Strategie etc.
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
        BenchmarkVisualizer.showBenchmarkResults(withSubRows, "The best results per Job-Set");
    }




    //#region Benchmark building
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
    //#endregion


    //#region small helper-classes
    private static Plate clonePlate(Plate base) {
        Plate p = new Plate(base.name, base.width, base.height, base.plateId);
        p.name = base.name;
        return p;
    }
    //#endregion


    //#region static classes
    private static class JobSetGroup {
        final Set<Integer> jobIds; 
        final List<String> pathIds = new ArrayList<>();
        String rootSetId;
        JobSetGroup(Set<Integer> jobIds) { 
            this.jobIds = jobIds; 
        }
    }

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
    //#endregion

    
    //#region Debug-Outputs
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

     private static void printStrategyCodeMatrix(List<PlatePath> allPaths, String title, List<Integer> desiredOrder) {
        if (allPaths == null || allPaths.isEmpty()) return;

        Map<String, PlatePath> byId = new HashMap<>();
        List<PlatePath> candidates = new ArrayList<>();
        for (PlatePath p : allPaths) {
            if (p != null && p.pathId != null) { byId.put(p.pathId, p); candidates.add(p); }
        }
        if (candidates.isEmpty()) return;

        List<List<Job>> jobsPerPath = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            PlatePath path = candidates.get(i);
            List<Job> jobsLoc = new ArrayList<>(path.plate.jobs);
            jobsLoc.sort(Comparator.comparingInt(j -> j.placementOrder));
            jobsPerPath.add(jobsLoc);
        }

        LinkedHashSet<Integer> allJobIds = new LinkedHashSet<>();
        for (int i = 0; i < candidates.size(); i++) {
            for (Job j : jobsPerPath.get(i)) allJobIds.add(j.id);
            if (candidates.get(i).failedJobs != null) allJobIds.addAll(candidates.get(i).failedJobs);
        }

        List<Integer> colJobIds = new ArrayList<>();
        if (desiredOrder != null && !desiredOrder.isEmpty()) {
            for (Integer id : desiredOrder) if (id != null) colJobIds.add(id);
            for (Integer id : allJobIds) if (!colJobIds.contains(id)) colJobIds.add(id);
        } else {
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

        StringBuilder header = new StringBuilder();
        header.append("\n").append(title).append("\n");
        header.append(String.format("%-14s | ", "Pfad"));
        for (int i = 0; i < maxCols; i++) {
            String lbl = "J" + colJobIds.get(i);
            if (lbl.length() > 4) lbl = lbl.substring(0, 4);
            header.append(String.format("%-4s", lbl));
        }
        System.out.println(header.toString());

        StringBuilder sep = new StringBuilder();
        sep.append("------------------");
        for (int i = 0; i < maxCols; i++) sep.append("----");
        System.out.println(sep.toString());

        Map<Integer, Integer> colIndexByJobId = new HashMap<>();
        for (int i = 0; i < colJobIds.size(); i++) colIndexByJobId.put(colJobIds.get(i), i);

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
            String parentId = path.splitFromPathId;
            Integer forkJobId = path.splitFromJobId >= 0 ? path.splitFromJobId : null;
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

    private static void printFreeRectanglesForIndex(BenchmarkVisualizer.BenchmarkResult res, int index) {
        if (res == null || res.platesFreeRects == null || index < 0 || index >= res.platesFreeRects.size()) return;
        java.util.List<?> list = res.platesFreeRects.get(index);
        if (list == null || list.isEmpty()) return;
        System.out.println("-- Freie Rechtecke (" + list.size() + "): ");
        for (Object o : list) {
            if (o instanceof PlatePath.FreeRectangle) {
                PlatePath.FreeRectangle fr = (PlatePath.FreeRectangle) o;
                System.out.printf("FR  [x=%.3f..%.3f, y=%.3f..%.3f]  size=%.3f x %.3f%n", fr.x, fr.x + fr.width, fr.y, fr.y + fr.height, fr.width, fr.height);
            }
        }
    }
    //#endregion


}