package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;

public class MaxRectBF_MultiPlate {

    public static class AlgorithmPath {
        public Plate plate;
        public String plateId; // z.B. "1", "2", ...
        public List<FreeRectangle> freeRects;
        public String pathDescription;
        public Integer parentPathIndex;
        public List<List<FreeRectangle>> freeRectsPerStep = new ArrayList<>();
        int placementCounter;
        List<FreeRectangle> lastAddedRects;
        public boolean isActive;
        public boolean useFullHeight;
        public List<Integer> failedJobs;
        public String parentPath;
        public List<Job> placedJobs = new ArrayList<>();
        public List<Integer> jobIds = new ArrayList<>();

        public AlgorithmPath(Plate originalPlate, String description) {
            // originalPlate wird im Konstruktor durch die übergebene Platte gesetzt
            // plate enthält jetzt den Namen und die Maße der Platte
            // Das erste freeRects wird hier mit den plate-Größen initialisiert
            // Also originalPlate -> plate -> freeRects
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
            this.plateId = null;
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
            this.placedJobs = new ArrayList<>();
            for (Job copiedJob : this.plate.jobs) {
                this.placedJobs.add(copiedJob);
            }
            this.plateId = null;
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

    public MaxRectBF_MultiPlate(List<Plate> plateInfos) {
        this.paths = new ArrayList<>();
        if (plateInfos != null && !plateInfos.isEmpty()) {
            Plate firstPlate = plateInfos.get(0);
            this.originalPlate = new Plate(firstPlate.name, firstPlate.width, firstPlate.height, firstPlate.plateId);
            this.originalPlate.name = firstPlate.name;
            this.originalPlate.parentPathIndex = 1;

            AlgorithmPath fullWidthPath = new AlgorithmPath(this.originalPlate, "Pfad 1 (FullWidth)");
            fullWidthPath.useFullHeight = false;
            fullWidthPath.plate.name = firstPlate.name;
            fullWidthPath.plateId = firstPlate.plateId;
            paths.add(fullWidthPath);

            AlgorithmPath fullHeightPath = new AlgorithmPath(this.originalPlate, "Pfad 2 (FullHeight)");
            fullHeightPath.useFullHeight = true;
            fullHeightPath.plate.name = firstPlate.name;
            fullHeightPath.plateId = firstPlate.plateId;
            paths.add(fullHeightPath);
        }
    }

    /**
     * Versucht, einen Job in allen aktiven Pfaden zu platzieren.
     * Falls ein Platz gefunden wird, wird der Job platziert.
     * Falls nicht, wird ggf. ein neuer Pfad mit alternativer Strategie erzeugt.
     * Gibt true zurück, wenn der Job in mindestens einem Pfad platziert werden konnte.
     */
    public boolean placeJob(Job originalJob, String plateId, int pathIndex) {
        List<AlgorithmPath> newPaths = new ArrayList<>();
        boolean anySuccess = false;

        // Iteriere über alle existierenden Pfade, beginnend mit pathIndex
        while (pathIndex < paths.size()) {
            AlgorithmPath currentPath = paths.get(pathIndex);

            // Erzeuge eine Kopie des Jobs für diesen Pfad
            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);

            BestFitResult result = new BestFitResult();

            // Suche das beste freie Rechteck für diesen Job (ggf. auch rotiert)
            for (int j = 0; j < currentPath.freeRects.size(); j++) {
                FreeRectangle rect = currentPath.freeRects.get(j);
                testAndUpdateBestFit(job.width, job.height, rect, false, result);
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
            }

            // Falls kein Platz gefunden wurde:
            if (result.bestRect == null) {
                // Markiere den Job als "failed" für diesen Pfad
                currentPath.failedJobs.add(originalJob.id);

                // Falls mindestens zwei Rechtecke zuletzt hinzugefügt wurden, kann ein alternativer Pfad erzeugt werden
                if (currentPath.lastAddedRects.size() >= 2) {
                    // Bestimme die alternative Strategie (FullWidth/FullHeight)
                    String newMethod = currentPath.useFullHeight ? "FullWidth" : "FullHeight";
                    int newPathId = paths.size() + newPaths.size() + 1;
                    // Angenommen, paths.size() = 2 (es gibt 2 existierende Pfade)
                    // und newPaths.size() = 0 (noch keine neuen Pfade erzeugt)
                    // Dann ergibt newPathId = 2 + 0 + 1 = 3

                    // Erzeuge neuen Pfad mit alternativer Strategie ab dem letzten platzierten Job
                    AlgorithmPath newMethodPath = new AlgorithmPath(currentPath, "Pfad " + newPathId + " (" + newMethod + " ab Job " + job.id + ")");
                    newMethodPath.useFullHeight = !currentPath.useFullHeight;
                    newMethodPath.plateId = plateId;

                    // Entferne den zuletzt gescheiterten Job aus der failed-Liste des neuen Pfads
                    newMethodPath.failedJobs.remove(newMethodPath.failedJobs.size() - 1);

                    // Hole den zuletzt platzierten Job
                    Job lastJob = newMethodPath.plate.jobs.get(newMethodPath.plate.jobs.size() - 1);

                    // Entferne die zuletzt hinzugefügten freien Rechtecke
                    List<FreeRectangle> rectsToRemove = new ArrayList<>(newMethodPath.lastAddedRects);
                    for (FreeRectangle rectToRemove : rectsToRemove) {
                        newMethodPath.freeRects.removeIf(rect ->
                            rect.x == rectToRemove.x && rect.y == rectToRemove.y &&
                            rect.width == rectToRemove.width && rect.height == rectToRemove.height);
                    }

                    // Rekonstruiere das ursprüngliche Rechteck des letzten Jobs
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

                    // Splitte das Rechteck nach alternativer Strategie
                    if (newMethod.equals("FullHeight")) {
                        splitFreeRectFullHeight(originalRect, lastJob, newMethodPath);
                    } else {
                        splitFreeRectFullWidth(originalRect, lastJob, newMethodPath);
                    }

                    // Versuche, den Job im neuen Pfad zu platzieren
                    BestFitResult newMethodResult = new BestFitResult();
                    for (int j = 0; j < newMethodPath.freeRects.size(); j++) {
                        FreeRectangle rect = newMethodPath.freeRects.get(j);
                        testAndUpdateBestFit(job.width, job.height, rect, false, newMethodResult);
                        if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, newMethodResult);
                    }

                    // Falls erfolgreich, füge neuen Pfad hinzu
                    if (newMethodResult.bestRect != null) {
                        newPaths.add(newMethodPath);
                        anySuccess = true;
                    } else {
                        // Sonst markiere auch im neuen Pfad als "failed"
                        newMethodPath.isActive = true;
                        newMethodPath.failedJobs.add(originalJob.id);
                        newPaths.add(newMethodPath);
                    }
                }

            } else {
                // Platz wurde gefunden: Platziere den Job im aktuellen Pfad
                String splittingMethod = currentPath.useFullHeight ? "FullHeight" : "FullWidth";
                placeJobInPath(job, result, currentPath, splittingMethod);
                currentPath.plateId = plateId;
                // currentPath.pathId = pathId; // falls du ein Feld pathId hinzufügst
                anySuccess = true;
            }

            pathIndex++;
        }

        // Versuche, den Job auch in allen neu erzeugten Pfaden zu platzieren
        for (int newPathIndex = 0; newPathIndex < newPaths.size(); newPathIndex++) {
            AlgorithmPath newPath = newPaths.get(newPathIndex);
            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);

            BestFitResult newPathResult = new BestFitResult();

            // Prüfe alle freien Rechtecke im neuen Pfad, ob der Job dort passt
            for (int j = 0; j < newPath.freeRects.size(); j++) {
                FreeRectangle rect = newPath.freeRects.get(j);
                testAndUpdateBestFit(job.width, job.height, rect, false, newPathResult);
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, newPathResult);
            }

            // Falls ein passendes Rechteck gefunden wurde, platziere den Job im neuen Pfad
            if (newPathResult.bestRect != null) {
                String splittingMethod = newPath.useFullHeight ? "FullHeight" : "FullWidth";
                placeJobInPath(job, newPathResult, newPath, splittingMethod);
                newPath.plateId = plateId;
                // newPath.pathId = pathId; // falls du ein Feld pathId hinzufügst
            }
        }
        
        // Füge alle neuen Pfade zur Pfadliste hinzu
        paths.addAll(newPaths);

        return anySuccess;
    }

    /**
     * Platziert einen Job in einem Pfad und aktualisiert die freien Rechtecke.
     * Setzt alle relevanten Felder des Jobs, fügt ihn zur Plate und zur placedJobs-Liste hinzu.
     * Splitte das belegte Rechteck nach der gewählten Strategie.
     * Speichere einen Snapshot der freien Rechtecke nach dem Platzieren.
     */
    private void placeJobInPath(Job job, BestFitResult result, AlgorithmPath path, String splittingMethod) {
        // Falls Rotation nötig, setze das Flag
        if (result.useRotated) {
            job.rotated = true;
        }
        // Setze die endgültigen Maße und Position des Jobs
        job.width = result.bestWidth;
        job.height = result.bestHeight;
        job.x = result.bestRect.x;
        job.y = result.bestRect.y;
        job.placedOn = path.plate;
        job.placementOrder = path.placementCounter++;
        job.splittingMethod = splittingMethod;

        // Füge Job zur Plate und zur placedJobs-Liste hinzu
        path.plate.jobs.add(job);
        path.placedJobs.add(job);

        // Splitte das belegte Rechteck nach Strategie
        if ("FullWidth".equals(splittingMethod)) {
            splitFreeRectFullWidth(result.bestRect, job, path);
        } else {
            splitFreeRectFullHeight(result.bestRect, job, path);
        }

        // Speichere einen Snapshot der aktuellen freien Rechtecke
        List<FreeRectangle> snapshot = new ArrayList<>();
        for (FreeRectangle fr : path.freeRects) {
            snapshot.add(new FreeRectangle(fr.x, fr.y, fr.width, fr.height));
        }
        path.freeRectsPerStep.add(snapshot);
        // Der Snapshot wird in freeRectsPerStep gespeichert.
        // Zweck: Die Liste freeRectsPerStep enthält nach jedem Platzierungsschritt
        // eine Kopie der freien Rechtecke. Damit kann der Zustand der freien Flächen
        // zu jedem Zeitpunkt (Schritt) nachverfolgt oder visualisiert werden.
        // Typische Verwendung: Für Debugging, Visualisierung, Animation oder Analyse
        // des Algorithmusverlaufs (z.B. wie sich die freien Rechtecke nach jedem Job verändern).
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


    public List<AlgorithmPath> getPathsAndFailedJobsOverviewData() {
        // Setze die jobIds für Übersicht
        for (AlgorithmPath path : paths) {
            path.jobIds = new ArrayList<>();
            for (Job job : path.plate.jobs) path.jobIds.add(job.id);
        }
        return paths;
    }


    /**
     * Gibt für die übergebenen Pfad- und Platten-IDs die Plattengröße und die zugehörigen failedJobs aus.
     */
    public void printFailedJobsAndPlateInfoForPath(String pathId) {
        for (AlgorithmPath path : paths) {
            if (path.pathDescription.equals(pathId)) {
                System.out.printf("Pfad: %s, Platte: %s (PlateId=%s), Größe: %.2fx%.2f%n",
                    path.pathDescription, path.plate.name, path.plateId, path.plate.width, path.plate.height);
                System.out.println("FailedJobs: " + path.failedJobs);
                System.out.println("Jobgrößen:");
                for (Integer jobId : path.failedJobs) {
                    Job foundJob = null;
                    for (Job job : path.plate.jobs) {
                        if (job.id == jobId) {
                            foundJob = job;
                            break;
                        }
                    }
                    if (foundJob != null) {
                        System.out.printf("  Job %d: %.2fx%.2f%n", foundJob.id, foundJob.width, foundJob.height);
                    } else {
                        System.out.printf("  Job %d: Größe unbekannt%n", jobId);
                    }
                }
                return;
            }
        }
        System.out.println("Kein Pfad mit PfadId=" + pathId + " gefunden.");
    }

}