package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.Provider.PlateProvider;

public class MaxRectBF_MultiPlate {

    public static class AlgorithmPath {
        public Plate plate;
        public List<FreeRectangle> freeRects;
        public String pathDescription;
        public Integer parentPathIndex;
        public List<List<FreeRectangle>> freeRectsPerStep = new ArrayList<>();
        int placementCounter;
        List<FreeRectangle> lastAddedRects;
        public boolean isActive;
        boolean useFullHeight;
        List<Integer> failedJobs;
        String parentPath;

        public AlgorithmPath(Plate originalPlate, String description) {
            this.plate = new Plate(originalPlate.name + " - " + description, originalPlate.width, originalPlate.height);
            this.freeRects = new ArrayList<>();
            this.freeRects.add(new FreeRectangle(0, 0, plate.width, plate.height));
            this.lastAddedRects = new ArrayList<>();
            this.pathDescription = description;
            this.placementCounter = 0;
            this.isActive = true;
            this.useFullHeight = false;
            this.failedJobs = new ArrayList<>();
            this.parentPath = null;
        }

        public AlgorithmPath(AlgorithmPath original, String newDescription) {
            this.plate = new Plate(original.plate.name.split(" - ")[0] + " - " + newDescription, original.plate.width, original.plate.height);
            for (int i = 0; i < original.plate.jobs.size(); i++) {
                Job originalJob = original.plate.jobs.get(i);
                Job copiedJob = new Job(originalJob.id, originalJob.width, originalJob.height);
                copiedJob.x = originalJob.x;
                copiedJob.y = originalJob.y;
                copiedJob.rotated = originalJob.rotated;
                copiedJob.placedOn = this.plate;
                copiedJob.placementOrder = originalJob.placementOrder;
                copiedJob.splittingMethod = originalJob.splittingMethod;
                this.plate.jobs.add(copiedJob);
            }
            this.freeRects = new ArrayList<>();
            for (int i = 0; i < original.freeRects.size(); i++) {
                FreeRectangle rect = original.freeRects.get(i);
                this.freeRects.add(new FreeRectangle(rect.x, rect.y, rect.width, rect.height));
            }
            this.lastAddedRects = new ArrayList<>();
            for (int i = 0; i < original.lastAddedRects.size(); i++) {
                FreeRectangle rect = original.lastAddedRects.get(i);
                this.lastAddedRects.add(new FreeRectangle(rect.x, rect.y, rect.width, rect.height));
            }
            this.pathDescription = newDescription;
            this.placementCounter = original.placementCounter;
            this.isActive = true;
            this.useFullHeight = original.useFullHeight;
            this.failedJobs = new ArrayList<>(original.failedJobs);
            this.parentPath = original.pathDescription;
        }
    }

    public static class FreeRectangle {
        public double x;
        public double y;
        public double width;
        public double height;
        public FreeRectangle(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    static class BestFitResult {
        public FreeRectangle bestRect;
        double bestScore = Double.MAX_VALUE;
        boolean useRotated = false;
        double bestWidth = -1;
        double bestHeight = -1;
    }

    List<AlgorithmPath> paths;
    Plate originalPlate;
    boolean debugEnabled = true;

    // TODO: Nimmt bei mehreren Platten erstmal nur die erste Platte
    public MaxRectBF_MultiPlate(List<PlateProvider.NamedPlate> plateInfos) {
        this.paths = new ArrayList<>();
        if (plateInfos != null && !plateInfos.isEmpty()) {
            PlateProvider.NamedPlate firstPlate = plateInfos.get(0);
            this.originalPlate = new Plate(firstPlate.name, firstPlate.width, firstPlate.height);
            AlgorithmPath fullWidthPath = new AlgorithmPath(this.originalPlate, "Pfad 1 (FullWidth)");
            fullWidthPath.useFullHeight = false;
            paths.add(fullWidthPath);

            AlgorithmPath fullHeightPath = new AlgorithmPath(this.originalPlate, "Pfad 2 (FullHeight)");
            fullHeightPath.useFullHeight = true;
            paths.add(fullHeightPath);
        }
    }

    public boolean placeJob(Job originalJob) {
        List<AlgorithmPath> newPaths = new ArrayList<>();
        boolean anySuccess = false;

        for (int pathIndex = 0; pathIndex < paths.size(); pathIndex++) {
            AlgorithmPath currentPath = paths.get(pathIndex);
            if (!currentPath.isActive) continue;

            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);

            BestFitResult result = new BestFitResult();

            for (int i = 0; i < currentPath.freeRects.size(); i++) {
                FreeRectangle rect = currentPath.freeRects.get(i);
                testAndUpdateBestFit(job.width, job.height, rect, false, result);
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
            }

            // HIER wird ein failedJob hinzugefügt, falls kein Platz gefunden wurde:
            if (result.bestRect == null) {
                currentPath.failedJobs.add(originalJob.id);

                if (currentPath.lastAddedRects.size() >= 2) {
                    String newMethod = currentPath.useFullHeight ? "FullWidth" : "FullHeight";
                    int newPathId = paths.size() + newPaths.size() + 1;

                    AlgorithmPath newMethodPath = new AlgorithmPath(currentPath, "Pfad " + newPathId + " (" + newMethod + " ab Job " + job.id + ")");
                    newMethodPath.useFullHeight = !currentPath.useFullHeight;

                    newMethodPath.failedJobs.remove(newMethodPath.failedJobs.size() - 1);

                    Job lastJob = newMethodPath.plate.jobs.get(newMethodPath.plate.jobs.size() - 1);

                    List<FreeRectangle> rectsToRemove = new ArrayList<>(newMethodPath.lastAddedRects);

                    for (FreeRectangle rectToRemove : rectsToRemove) {
                        newMethodPath.freeRects.removeIf(rect ->
                            rect.x == rectToRemove.x && rect.y == rectToRemove.y &&
                            rect.width == rectToRemove.width && rect.height == rectToRemove.height);
                    }

                    double originalWidth = lastJob.width;
                    double originalHeight = lastJob.height;

                    for (FreeRectangle removed : rectsToRemove) {
                        if (removed.x == lastJob.x + lastJob.width && removed.y == lastJob.y) {
                            originalWidth += removed.width;
                        } else if (removed.x == lastJob.x && removed.y == lastJob.y + lastJob.height) {
                            originalHeight += removed.height;
                        }
                    }

                    FreeRectangle originalRect = new FreeRectangle(lastJob.x, lastJob.y, originalWidth, originalHeight);

                    if (newMethod.equals("FullHeight")) {
                        splitFreeRectFullHeight(originalRect, lastJob, newMethodPath);
                    } else {
                        splitFreeRectFullWidth(originalRect, lastJob, newMethodPath);
                    }

                    BestFitResult newMethodResult = new BestFitResult();
                    for (int i = 0; i < newMethodPath.freeRects.size(); i++) {
                        FreeRectangle rect = newMethodPath.freeRects.get(i);
                        testAndUpdateBestFit(job.width, job.height, rect, false, newMethodResult);
                        if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, newMethodResult);
                    }

                    if (newMethodResult.bestRect != null) {
                        newPaths.add(newMethodPath);
                        anySuccess = true;
                    } else {
                        newMethodPath.isActive = true;
                        newMethodPath.failedJobs.add(originalJob.id);
                        newPaths.add(newMethodPath);
                    }
                }

            } else {
                String splittingMethod = currentPath.useFullHeight ? "FullHeight" : "FullWidth";
                placeJobInPath(job, result, currentPath, splittingMethod);
                anySuccess = true;
            }
        }

        for (int newPathIndex = 0; newPathIndex < newPaths.size(); newPathIndex++) {
            AlgorithmPath newPath = newPaths.get(newPathIndex);
            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);

            BestFitResult newPathResult = new BestFitResult();
            for (int i = 0; i < newPath.freeRects.size(); i++) {
                FreeRectangle rect = newPath.freeRects.get(i);
                testAndUpdateBestFit(job.width, job.height, rect, false, newPathResult);
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, newPathResult);
            }

            if (newPathResult.bestRect != null) {
                String splittingMethod = newPath.useFullHeight ? "FullHeight" : "FullWidth";
                placeJobInPath(job, newPathResult, newPath, splittingMethod);
            }
        }

        paths.addAll(newPaths);

        return anySuccess;
    }

    private void placeJobInPath(Job job, BestFitResult result, AlgorithmPath path, String splittingMethod) {
        if (result.useRotated) {
            job.rotated = true;
        }
        job.width = result.bestWidth;
        job.height = result.bestHeight;
        job.x = result.bestRect.x;
        job.y = result.bestRect.y;
        job.placedOn = path.plate;
        job.placementOrder = path.placementCounter++;
        job.splittingMethod = splittingMethod;
        path.plate.jobs.add(job);

        if ("FullWidth".equals(splittingMethod)) {
            splitFreeRectFullWidth(result.bestRect, job, path);
        } else {
            splitFreeRectFullHeight(result.bestRect, job, path);
        }

        List<FreeRectangle> snapshot = new ArrayList<>();
        for (FreeRectangle fr : path.freeRects) {
            snapshot.add(new FreeRectangle(fr.x, fr.y, fr.width, fr.height));
        }
        path.freeRectsPerStep.add(snapshot);
    }

    private void testAndUpdateBestFit(double testWidth, double testHeight, FreeRectangle rect, boolean rotated, BestFitResult result) {
        if (testWidth <= rect.width && testHeight <= rect.height) {
            double leftoverHoriz = rect.width - testWidth;
            double leftoverVert = rect.height - testHeight;
            double shortSideFit = Math.min(leftoverHoriz, leftoverVert);
            if (shortSideFit < result.bestScore) {
                result.bestScore = shortSideFit;
                result.bestRect = rect;
                result.useRotated = rotated;
                result.bestWidth = testWidth;
                result.bestHeight = testHeight;
            }
        }
    }

    private void splitFreeRectFullWidth(FreeRectangle rect, Job job, AlgorithmPath path) {
        path.freeRects.remove(rect);
        path.lastAddedRects.clear();
        if (job.width < rect.width) {
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, job.height);
            path.freeRects.add(newRectRight);
            path.lastAddedRects.add(newRectRight);
        }
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, rect.width, rect.height - job.height);
            path.freeRects.add(newRectBelow);
            path.lastAddedRects.add(newRectBelow);
        }
    }

    private void splitFreeRectFullHeight(FreeRectangle rect, Job job, AlgorithmPath path) {
        path.freeRects.remove(rect);
        path.lastAddedRects.clear();
        if (job.width < rect.width) {
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, rect.height);
            path.freeRects.add(newRectRight);
            path.lastAddedRects.add(newRectRight);
        }
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, job.width, rect.height - job.height);
            path.freeRects.add(newRectBelow);
            path.lastAddedRects.add(newRectBelow);
        }
    }

    // Getter für die freien Rechtecke des ersten aktiven Pfads
    public List<FreeRectangle> getFreeRects() {
        for (AlgorithmPath path : paths) {
            if (path.isActive) {
                return path.freeRects;
            }
        }
        return new ArrayList<>();
    }

    // Getter für die beste Plate (ohne Visualisierung)
    public Plate getBestPlate() {
        AlgorithmPath bestPath = null;
        double bestCoverage = 0;

        for (AlgorithmPath path : paths) {
            if (path.isActive) {
                double coverage = 0;
                if (coverage > bestCoverage) {
                    bestCoverage = coverage;
                    bestPath = path;
                }
            }
        }

        if (bestPath != null) {
            return bestPath.plate;
        } else {
            return originalPlate;
        }
    }

    public List<AlgorithmPath> getAllPaths() {
        return paths;
    }

    public String getStrategyCodeForPath(AlgorithmPath path) {
        java.util.List<Integer> allJobIds = new java.util.ArrayList<>();

        for (int i = 0; i < path.plate.jobs.size(); i++) {
            Job job = path.plate.jobs.get(i);
            if (!allJobIds.contains(job.id)) {
                allJobIds.add(job.id);
            }
        }

        for (int i = 0; i < path.failedJobs.size(); i++) {
            Integer failedJobId = path.failedJobs.get(i);
            if (!allJobIds.contains(failedJobId)) {
                allJobIds.add(failedJobId);
            }
        }

        for (int i = 0; i < allJobIds.size() - 1; i++) {
            for (int j = 0; j < allJobIds.size() - 1 - i; j++) {
                if (allJobIds.get(j) > allJobIds.get(j + 1)) {
                    Integer temp = allJobIds.get(j);
                    allJobIds.set(j, allJobIds.get(j + 1));
                    allJobIds.set(j + 1, temp);
                }
            }
        }

        java.util.List<String> strategyCodes = new java.util.ArrayList<>();

        for (int i = 0; i < allJobIds.size(); i++) {
            Integer jobId = allJobIds.get(i);
            boolean found = false;

            for (int j = 0; j < path.plate.jobs.size(); j++) {
                Job job = path.plate.jobs.get(j);
                if (job.id == jobId.intValue()) {
                    if ("FullWidth".equals(job.splittingMethod)) {
                        strategyCodes.add("W");
                    } else if ("FullHeight".equals(job.splittingMethod)) {
                        strategyCodes.add("H");
                    } else {
                        strategyCodes.add("?");
                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                strategyCodes.add("N");
            }
        }

        if (strategyCodes.size() == 0) {
            return "";
        } else {
            return String.join("->", strategyCodes);
        }
    }

    // Gibt eine kurze Übersicht aller Pfade mit Strategie, Abstammung und Job-IDs inkl. Plattenbezeichnung
    public void printPathsOverview() {
        for (AlgorithmPath path : paths) {
            StringBuilder sb = new StringBuilder();
            sb.append(path.pathDescription);
            sb.append(path.plate.name);
            sb.append(" | Strategie: ").append(path.useFullHeight ? "FullHeight" : "FullWidth");
            sb.append(" | Parent: ").append(path.parentPath != null ? path.parentPath : "-");
            sb.append(" | Jobs: ");
            List<Integer> jobIds = new ArrayList<>();
            for (Job job : path.plate.jobs) jobIds.add(job.id);
            sb.append(jobIds);
            System.out.println(sb.toString());
        }
    }

    // Gibt für jeden Pfad die nicht platzierten Job-IDs (failedJobs) als 2D-Liste zurück.
    public List<List<Integer>> getFailedJobIdsPerPath() {
        List<List<Integer>> failedJobsPerPath = new ArrayList<>();
        for (AlgorithmPath path : paths) {
            failedJobsPerPath.add(new ArrayList<>(path.failedJobs));
        }
        return failedJobsPerPath;
    }

}