package org.example.Algorithm;

import org.example.DataClasses.Job;
import org.example.DataClasses.PlatePath;
import org.example.DataClasses.Plate;
import org.example.HelperClasses.JobsSetup;
import org.example.Visualizer.BenchmarkVisualizer;

import java.util.*;



public class Controller {

    private static final String sortTypeArea = "Area";
    private static final String sortTypeEdge = "Edge";
    private static final int startPlateId = 1;

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
        PlateRunResult resultArea = placeJobsFirstPlate(originalJobs, originalPlate, sortTypeArea, allJobIds);
        PlateRunResult resultEdge = placeJobsFirstPlate(originalJobs, originalPlate, sortTypeEdge, allJobIds);

        // #region Strategy-Code-Matrix-output
        List<Integer> orderArea = new ArrayList<>();
        for (Job j : resultArea.sortedJobs) orderArea.add(j.id);
        //printStrategyCodeMatrix(resultArea.paths, "Platte 1 - Strategie-Matrix (Fläche)", orderArea);
        
        List<Integer> orderEdge = new ArrayList<>();
        for (Job j : resultEdge.sortedJobs) orderEdge.add(j.id);
        //printStrategyCodeMatrix(resultEdge.paths, "Platte 1 - Strategie-Matrix (Kante)", orderEdge);
        // #endregion

        // Build the first benchmark window (Plate 1)
        List<BenchmarkVisualizer.BenchmarkResult> plateOneBenchmark = new ArrayList<>();
        plateOneBenchmark.addAll(resultArea.benchmarks);
        plateOneBenchmark.addAll(resultEdge.benchmarks);

        // Set all rows in Plate 1 as main rows
        for (BenchmarkVisualizer.BenchmarkResult br : plateOneBenchmark) if (br != null) br.isSubRow = false;
        // Show benchmark results for Plate 1
        //BenchmarkVisualizer.showBenchmarkResults(plateOneBenchmark, "Plate 1 __ First run");

        // Process follow-up plates
        generateFollowUpPlates(originalJobs, originalPlate, resultArea.groups, resultEdge.groups);

        // Show summary of job-sets over all plates
        showSummaryOfJobSets(resultArea, resultEdge);
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
        // Algorithm.placeJob call
        for (Job job : jobs) {
            algo.placeJob(job, plate.plateId);
        }

        // #region Debug-Output of paths and failed jobs
        List<PlatePath> paths = algo.getPathsAndFailedJobsOverviewData();
        // #endregion

        // Build the job-set groups for unplaced jobs
        List<JobSetGroup> unplacedJobSetGroups = buildUnplacedJobGroups(paths, allJobIds);
        
        // Set rootSetIds for groups. The left over job list (after Plate 1 placements) is the root ID.
        for (JobSetGroup g : unplacedJobSetGroups) {
            if (g.rootSetId == null) {
                List<Integer> sorted = new ArrayList<>(g.jobIds);
                Collections.sort(sorted);
                g.rootSetId = sorted.toString();
            }
        }

        //#region Debug-Output of job-sets
        printJobSetGroups(unplacedJobSetGroups, 1, sortType);
        //#endregion

        // Build the benchmark results (Plate 1: all jobs were attempted)
        // For Plate 1, we pass all jobs since that's what we attempted to place
        List<BenchmarkVisualizer.BenchmarkResult> benchmarks = buildBenchmarkResults(paths, jobs, Arrays.asList(plate), "eins", "zwei");

        // Set sortedBy for all Plate 1 benchmarks
        for (BenchmarkVisualizer.BenchmarkResult br : benchmarks) {
            if (br != null) {
                br.sortedBy = sortType;
            }
        }

        return new PlateRunResult(jobs, paths, unplacedJobSetGroups, benchmarks);
    }


    /*
     * Create new Plates for each job-set until all jobs are placed
     */
    private static void generateFollowUpPlates(List<Job> originalJobs, Plate plateTemplate, List<JobSetGroup> initialGroupsArea, List<JobSetGroup> initialGroupsEdge) {
        List<JobSetGroup> currentGroupsArea = initialGroupsArea;
        List<JobSetGroup> currentGroupsEdge = initialGroupsEdge;
        int plateId = startPlateId;

        while (true) {
            if (currentGroupsArea.isEmpty() && currentGroupsEdge.isEmpty()) break;
            
            List<JobSetGroup> nextGroupsArea = new ArrayList<>();
            List<JobSetGroup> nextGroupsEdge = new ArrayList<>();

            // Every sorting gets its own plate
            Plate plateForArea = clonePlate(plateTemplate);
            Plate plateForEdge = clonePlate(plateTemplate);

            processJobSetGroups(originalJobs, plateForArea, currentGroupsArea, sortTypeArea, plateId, nextGroupsArea);
            processJobSetGroups(originalJobs, plateForEdge, currentGroupsEdge, sortTypeEdge, plateId, nextGroupsEdge);

            // new value is set in processJobSetGroups with List<JobSetGroup> nextGroups
            currentGroupsArea = nextGroupsArea;
            currentGroupsEdge = nextGroupsEdge;
            plateId++;  // set new plateId
        }
    }


    /*
     * Processes job-set groups with the specified sorting type
     * groups: List of job-set groups to process
     * nextGroups: List to fill with unplaced job-set groups for the next plate. Also contains the rootSetId and parentSetId (the current group's job IDs).
     */
    private static void processJobSetGroups(
        List<Job> originalJobs, Plate currentPlate, List<JobSetGroup> groups, String sortType, int plateId, List<JobSetGroup> nextGroups) {

        // Go through all job-set groups
        for (JobSetGroup group : groups) {

            // Run algorithm with the specified sorting type only
            JobSetRunResult result = placeLeftoverJobs(originalJobs, currentPlate, group, nextGroups);

            // Set labels for benchmarks
            for (BenchmarkVisualizer.BenchmarkResult benchmarkResult : result.benchmarks) {
                benchmarkResult.isSubRow = false;
                benchmarkResult.currentJobSet = group.jobIds.toString();
                benchmarkResult.sortedBy = sortType;
                benchmarkResult.rootSetId = group.rootSetId;
                benchmarkResult.parentSetId = group.parentSetId;
            }

            // Removed: BenchmarkVisualizer.showBenchmarkResults(result.benchmarks, ...)

            // TODO: Auslagern
            // For all plates with index >= 1 (>= Plate 2), remember the best result for this job-set for later summary sublines
            if (plateId >= 1) {
                BenchmarkVisualizer.BenchmarkResult bestP2 = null;
                // iterate over all benchmarks to find the best one and print each iteration
                System.out.println("\n");
                for (BenchmarkVisualizer.BenchmarkResult benchmarkResult : result.benchmarks) {
                    if (bestP2 == null || benchmarkResult.coverageRate > bestP2.coverageRate) bestP2 = benchmarkResult;
                }
                if (bestP2 != null) {
                    // store a shallow copy with adjusted labeling to indicate Plate 2+
                    BenchmarkVisualizer.BenchmarkResult copy = cloneBenchmarkResult(bestP2);
                    if (copy.currentJobSet == null || "-".equals(copy.currentJobSet)) copy.currentJobSet = group.jobIds.toString();
                    BEST_PLATE2_BY_JOBSET.put(group.jobIds.toString(), copy);
                }
            }



            // Build the job-set groups for unplaced jobs
            List<PlatePath> pathsForNext = result.paths;
            List<JobSetGroup> unplacedJobSetGroups = buildUnplacedJobGroups(pathsForNext, group.jobIds);
            
            // Set rootSetIds and parentSetId for this group
            for (JobSetGroup jobSetGroup : unplacedJobSetGroups) {
                if (jobSetGroup.jobIds.isEmpty()) continue;  // Important: Skip empty sets
                jobSetGroup.rootSetId = group.rootSetId;
                // Save the parent set id (the current group's job IDs) so the next iteration knows its origin. Useful for the summary view.
                jobSetGroup.parentSetId = group.jobIds.toString();
                // nextGroups was provided as an empty parameter and is filled here with the unplaced job-sets for the next iteration (next plate).
                nextGroups.add(jobSetGroup);
            }
        }
    }


    /*
     * Similar to placeAllJobsFirstPlate
     * (Trying) to place all leftover jobs on the provided plate (this method is called for each sorting type).
     * DO NOT generates job-set groups for unplaced jobs.
     * Provide results for benchmark building.
     */
    private static JobSetRunResult placeLeftoverJobs(List<Job> originalJobs, Plate currentPlate, JobSetGroup group, List<JobSetGroup> nextGroups) {
        // Convert JobSetGroup group to Job-objects for Algorithm.placeJob
        Map<Integer, Job> byId = new HashMap<>();
        for (Job j : originalJobs) byId.put(j.id, j);
        List<Job> jobs = new ArrayList<>();
        for (Integer id : group.jobIds) {
            Job o = byId.get(id);
            jobs.add(new Job(o.id, o.width, o.height));
        }

        // Algorithm.placeJob call
        Algorithm algo = new Algorithm(Arrays.asList(currentPlate));
        // Place actual Job objects from subset (not the integer ids)
        for (Job j : jobs) {
            algo.placeJob(j, currentPlate.plateId);
        }

        // #region Debug-Output of paths and failed jobs
        List<PlatePath> paths = algo.getPathsAndFailedJobsOverviewData();
        // #endregion

        // Build and show the benchmark results
        List<BenchmarkVisualizer.BenchmarkResult> benchmarks = buildBenchmarkResults(paths, jobs, Arrays.asList(currentPlate), "Moin", "Hallo");
        //BenchmarkVisualizer.showBenchmarkResults(benchmarks, "Benchmarks for current plate");

        /*
        BenchmarkVisualizer.showBenchmarkResults(benchmarks,
            "Plate " + (plateId + 1) + " ___ Sorting: " + sortType + " ___ Job-Set: " + group.jobIds.toString() + " ___ Root-Set: " + group.rootSetId);
        */

        return new JobSetRunResult(paths, benchmarks);
    }


    // Helper: Zeigt Zusammenfassung der Job-Sets
    private static void showSummaryOfJobSets(PlateRunResult resultArea, PlateRunResult resultEdge) {
        List<List<BenchmarkVisualizer.BenchmarkResult>> allJobSetResults = new ArrayList<>();
        
        collectJobSetResults(resultArea.groups, resultArea.benchmarks, allJobSetResults);
        collectJobSetResults(resultEdge.groups, resultEdge.benchmarks, allJobSetResults);
        
        showSummaryOfBestBenchmarks(allJobSetResults);
    }

    // Helper: Sammelt Benchmark-Ergebnisse für Job-Sets
    private static void collectJobSetResults(List<JobSetGroup> groups, List<BenchmarkVisualizer.BenchmarkResult> benchmarks,List<List<BenchmarkVisualizer.BenchmarkResult>> allJobSetResults) {
        for (JobSetGroup g : groups) {
            List<Integer> idsSorted = new ArrayList<>(g.jobIds);
            Collections.sort(idsSorted);
            String currentJobSet = idsSorted.toString();
            
            List<BenchmarkVisualizer.BenchmarkResult> jobSetResults = new ArrayList<>();
            for (BenchmarkVisualizer.BenchmarkResult br : benchmarks) {
                if (br.currentJobSet == null || "-".equals(br.currentJobSet)) {
                    br.currentJobSet = currentJobSet;
                }
                if (currentJobSet.equals(br.currentJobSet)) {
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
     * Prepare benchmark results for the BenchmarkVisualizer. The BenchmarkVisualizer is called with BenchmarkVisualizer.showBenchmarkResults.
     */
    private static List<BenchmarkVisualizer.BenchmarkResult> buildBenchmarkResults(
        List<PlatePath> allPaths, List<Job> originalJobs, List<Plate> plateInfos, String rootSetId, String parentSetId) {
            
        List<BenchmarkVisualizer.BenchmarkResult> results = new ArrayList<>();
        int totalJobs = originalJobs.size();

        for (PlatePath p : allPaths) {
            int placed = p.plate.jobs.size();
            double used = 0.0;
            for (Job j : p.plate.jobs) used += (j.width * j.height);
            double totalArea = p.plate.width * p.plate.height;
            double coverage = (used / totalArea) * 100.0;

            String algoName = "Pfad " + p.pathId;
            BenchmarkVisualizer.BenchmarkResult row = new BenchmarkVisualizer.BenchmarkResult(
                    algoName,
                    p.plate,
                    null,
                    placed,
                    coverage,
                    totalJobs
            );

            List<Integer> failed = new ArrayList<>(p.failedJobs);
            Collections.sort(failed);
            row.currentJobSet = failed.toString();

            row.platesRefs = new ArrayList<>();
            row.platesRefs.add(p.plate);
            row.platesNames = new ArrayList<>();
            row.platesNames.add(p.plate.name);
            row.platesCoverages = new ArrayList<>();
            row.platesCoverages.add(coverage);
            row.platesFreeRects = new ArrayList<>();
            row.platesFreeRects.add(p.freeRects);
            row.currentPlateIndex = 0;
            row.rootSetId = rootSetId;
            row.parentSetId = parentSetId;

            results.add(row);
        }

        results.sort((a, b) -> Double.compare(b.coverageRate, a.coverageRate));
        return results;
    }


    /*
     * PlateVisualizer ruft CutLineCalculator auf und bekommt die Schnittkoordianten zurück
     */
    public static List<CutLineCalculator.CutLine> computeCutLinesForPlate(Plate plate) {
        return CutLineCalculator.calculateCutLinesForPlate(plate);
    }



    /**
     * Zeigt ein zusammenfassendes Benchmark-Fenster mit den jeweils besten Ergebnissen aller Job-Sets.
     * Sortiert nach Bedeckungsrate, zeigt Job-Set, Coverage, Platzierungsanzahl, Strategie etc.
     */
    private static void showSummaryOfBestBenchmarks(List<List<BenchmarkVisualizer.BenchmarkResult>> allJobSetResults) {
        // Map: currentJobSet -> bestes Ergebnis
        Map<String, BenchmarkVisualizer.BenchmarkResult> bestByJobSet = new LinkedHashMap<>();
        for (List<BenchmarkVisualizer.BenchmarkResult> jobSetResults : allJobSetResults) {
            for (BenchmarkVisualizer.BenchmarkResult benchmarkResult : jobSetResults) {
                String key = benchmarkResult.currentJobSet != null ? benchmarkResult.currentJobSet : benchmarkResult.algorithmName;
                BenchmarkVisualizer.BenchmarkResult old = bestByJobSet.get(key);
                if (old == null || benchmarkResult.coverageRate > old.coverageRate) {
                    bestByJobSet.put(key, benchmarkResult);
                }
            }
        }
        // Sortiere nach Coverage absteigend
        List<BenchmarkVisualizer.BenchmarkResult> bestList = new ArrayList<>(bestByJobSet.values());
        bestList.sort((a, b) -> Double.compare(b.coverageRate, a.coverageRate));

        // Füge ein Ranking hinzu und schreibe das Job-Set in die Zeile
        for (BenchmarkVisualizer.BenchmarkResult benchmarkResult : bestList) {
            // Schreibe das Job-Set in den Algorithmusnamen
            if (benchmarkResult.currentJobSet != null && !"-".equals(benchmarkResult.currentJobSet)) {
                benchmarkResult.algorithmName = benchmarkResult.algorithmName + "  [Job-Set: " + benchmarkResult.currentJobSet + "]";
            }
            // Hauptzeile markieren
            benchmarkResult.isSubRow = false;
        }

        // Zeige das beste Ergebniss pro Job-Set von Platte 2 als Unterzeile an
        List<BenchmarkVisualizer.BenchmarkResult> withSubRows = new ArrayList<>();
        for (BenchmarkVisualizer.BenchmarkResult benchmarkResult : bestList) {
            withSubRows.add(benchmarkResult);
            String key = benchmarkResult.currentJobSet != null ? benchmarkResult.currentJobSet : benchmarkResult.algorithmName;
            BenchmarkVisualizer.BenchmarkResult p2 = BEST_PLATE2_BY_JOBSET.get(key);
            if (p2 != null) {
                BenchmarkVisualizer.BenchmarkResult sub = cloneBenchmarkResult(p2);
                sub.algorithmName = "       Platte 2: " + sub.algorithmName;
                   sub.sortedBy = benchmarkResult.sortedBy;
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
        dst.rootSetId = src.rootSetId;
        dst.parentSetId = src.parentSetId;
        dst.sortedBy = src.sortedBy;
        dst.currentJobSet = src.currentJobSet;
        dst.currentPlateIndex = src.currentPlateIndex;
        dst.isSubRow = src.isSubRow;
        return dst;
    }


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
        String parentSetId;
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
        List<CutLineCalculator.CutLine> cuts = CutLineCalculator.calculateAllFullCuts(plate);
        List<CutLineCalculator.ResidualPlate> residuals = CutLineCalculator.calculateResidualPlates(plate);

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
    String setInfo = (res.currentJobSet == null || "-".equals(res.currentJobSet)) ? "" : (" | Job-Set: " + res.currentJobSet);
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
        List<?> list = res.platesFreeRects.get(index);
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