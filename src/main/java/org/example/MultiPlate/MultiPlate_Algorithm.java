package org.example.MultiPlate;

import java.util.ArrayList;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.MultiPlate_DataClasses;
import org.example.DataClasses.Plate;

public class MultiPlate_Algorithm {

    List<MultiPlate_DataClasses> paths;
    Plate originalPlate;
    boolean debugEnabled = false;

    public MultiPlate_Algorithm() {

    }

    public MultiPlate_Algorithm(List<Plate> plateInfos) {
        this.paths = new ArrayList<>();
        if (plateInfos != null && !plateInfos.isEmpty()) {
            Plate firstPlate = plateInfos.get(0);
            this.originalPlate = new Plate(firstPlate.name, firstPlate.width, firstPlate.height, firstPlate.plateId);
            this.originalPlate.name = firstPlate.name;
            this.originalPlate.parentPathIndex = 1;

            MultiPlate_DataClasses fullWidthPath = new MultiPlate_DataClasses(this.originalPlate, "1", MultiPlate_DataClasses.Strategy.FULL_WIDTH);
            fullWidthPath.plate.name = firstPlate.name;
            fullWidthPath.plateId = firstPlate.plateId;
            paths.add(fullWidthPath);

            MultiPlate_DataClasses fullHeightPath = new MultiPlate_DataClasses(this.originalPlate, "2", MultiPlate_DataClasses.Strategy.FULL_HEIGHT);
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
    // Wird pro Job aufgerufen
    public boolean placeJob(Job originalJob, String plateId) {
        // Liste für neue alternative Pfade
        List<MultiPlate_DataClasses> newPaths = new ArrayList<>();
        // Flag, ob der Job irgendwo platziert werden konnte
        boolean anySuccess = false;

        // Debug-Ausgabe: Start des Platzierungsversuchs
        if (debugEnabled) {
            System.out.println("\n=== Neuer Platzierungsversuch für Job " + originalJob.id + " (" + originalJob.width + "x" + originalJob.height + ") auf PlatteId " + plateId + " ===");
            System.out.println("Aktive Pfade: " + paths.size());
        }

        // Geht mit dem aktuellen Job zuerst alle aktiven Pfade durch und erstellt ggf. neue Pfade
        int initialSize = paths.size();
        for (int pathIndex = 0; pathIndex < initialSize; pathIndex++) {
            MultiPlate_DataClasses currentPath = paths.get(pathIndex);

            // Debug-Ausgabe: Pfad-Info
            if (debugEnabled) {
                System.out.println("\n--- [Pfad " + (pathIndex+1) + ": " + currentPath.pathDescription + "] ---");
                System.out.println("Strategie: " + (currentPath.strategy == MultiPlate_DataClasses.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth"));
                System.out.println("Freie Rechtecke: " + currentPath.freeRects.size());
            }

            // Erzeuge eine Kopie des Jobs für diesen Pfad
            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);
            // Ergebnisobjekt für die beste Platzierung
            MultiPlate_DataClasses.BestFitResult result = new MultiPlate_DataClasses.BestFitResult();

            // Suche das beste freie Rechteck für diesen Job (ggf. auch rotiert)
            for (int j = 0; j < currentPath.freeRects.size(); j++) {
                MultiPlate_DataClasses.FreeRectangle rect = currentPath.freeRects.get(j);
                testAndUpdateBestFit(job.width, job.height, rect, false, result);
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
            }

            // Falls kein Platz gefunden wurde:
            if (result.bestRect == null) {
                // Debug-Ausgabe: Job konnte nicht platziert werden
                if (debugEnabled) System.out.println(currentPath.pathDescription + ": Job " + job.id + " konnte NICHT platziert werden.");

                // Markiere den Job als "failed" für diesen Pfad
                currentPath.failedJobs.add(originalJob.id);

                // Bestimme die alternative Strategie (FullWidth oder FullHeight)
                MultiPlate_DataClasses.Strategy newStrategy = currentPath.strategy == MultiPlate_DataClasses.Strategy.FULL_HEIGHT ? MultiPlate_DataClasses.Strategy.FULL_WIDTH : MultiPlate_DataClasses.Strategy.FULL_HEIGHT;
                int newPathId = paths.size() + newPaths.size() + 1;

                // Debug-Ausgabe: Alternativer Pfad wird erzeugt
                if (debugEnabled) System.out.println(currentPath.pathDescription + ": Erzeuge alternativen Pfad mit Strategie " + newStrategy + " ab Job " + job.id);

                // ERZEUGE NEUEN PFAD mit alternativer Strategie ab dem letzten platzierten Job
                MultiPlate_DataClasses newMethodPath = new MultiPlate_DataClasses(currentPath, String.valueOf(newPathId), newStrategy, currentPath.pathId, job.id);
                newMethodPath.strategy = newStrategy;
                newMethodPath.plateId = plateId;

                // Entferne den zuletzt gescheiterten Job aus der failed-Liste des neuen Pfads
                newMethodPath.failedJobs.remove(newMethodPath.failedJobs.size() - 1);

                // Hole den zuletzt platzierten Job
                Job lastJob = newMethodPath.plate.jobs.get(newMethodPath.plate.jobs.size() - 1);

                // Entferne die zuletzt hinzugefügten freien Rechtecke
                List<MultiPlate_DataClasses.FreeRectangle> rectsToRemove = new ArrayList<>(newMethodPath.lastAddedRects);
                for (MultiPlate_DataClasses.FreeRectangle rectToRemove : rectsToRemove) {
                    newMethodPath.freeRects.removeIf(rect ->
                        rect.x == rectToRemove.x && rect.y == rectToRemove.y &&
                        rect.width == rectToRemove.width && rect.height == rectToRemove.height);
                }

                // Rekonstruiere das ursprüngliche Rechteck des letzten Jobs
                double originalWidth = lastJob.width;
                double originalHeight = lastJob.height;
                for (MultiPlate_DataClasses.FreeRectangle removed : rectsToRemove) {
                    if (removed.x == lastJob.x + lastJob.width && removed.y == lastJob.y) {
                        originalWidth += removed.width;
                    } else if (removed.x == lastJob.x && removed.y == lastJob.y + lastJob.height) {
                        originalHeight += removed.height;
                    }
                }
                MultiPlate_DataClasses.FreeRectangle originalRect = new MultiPlate_DataClasses.FreeRectangle(lastJob.x, lastJob.y, originalWidth, originalHeight);

                // Splitte das Rechteck nach alternativer Strategie
                if (newStrategy.equals(MultiPlate_DataClasses.Strategy.FULL_HEIGHT)) {
                    splitFreeRectFullHeight(originalRect, lastJob, newMethodPath);
                } else {
                    splitFreeRectFullWidth(originalRect, lastJob, newMethodPath);
                }

                // Versuche, den Job im neuen Pfad zu platzieren
                MultiPlate_DataClasses.BestFitResult newMethodResult = new MultiPlate_DataClasses.BestFitResult();
                for (int j = 0; j < newMethodPath.freeRects.size(); j++) {
                    MultiPlate_DataClasses.FreeRectangle rect = newMethodPath.freeRects.get(j);
                    testAndUpdateBestFit(job.width, job.height, rect, false, newMethodResult);
                    if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, newMethodResult);
                }

                // Falls erfolgreich, füge neuen Pfad hinzu
                if (newMethodResult.bestRect != null) {
                    if (debugEnabled) {
                        System.out.println(newMethodPath.pathDescription + ": Job " + job.id + " konnte im alternativen Pfad platziert werden.");
                    }
                    newPaths.add(newMethodPath);
                    anySuccess = true;
                } else {
                    // Sonst markiere auch im neuen Pfad als "failed"
                    if (debugEnabled) {
                        System.out.println(newMethodPath.pathDescription + ": Job " + job.id + " konnte auch im alternativen Pfad nicht platziert werden.");
                    }
                    newMethodPath.isActive = true;
                    newMethodPath.failedJobs.add(originalJob.id);
                    newPaths.add(newMethodPath);
                }

            // Geht hier rein, wenn direkt ein Platz gefunden wurde
            } else {
                if (debugEnabled) {
                    System.out.println(currentPath.pathDescription + ": Job " + job.id + " wird platziert auf Rechteck (" + result.bestRect.x + "," + result.bestRect.y + "," + result.bestRect.width + "x" + result.bestRect.height + ")" + (result.useRotated ? " [rotiert]" : ""));
                }
                // Platziere den Job im aktuellen Pfad
                String splittingMethod = currentPath.strategy == MultiPlate_DataClasses.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth";
                placeJobInPath(job, result, currentPath, splittingMethod);
                currentPath.plateId = plateId;
                anySuccess = true;
            }
            if (debugEnabled) System.out.println("pathIndex: " + pathIndex + ", initialSize: " + initialSize);

            // Erneuter Schleifendurchlauf, wenn initialSize noch nicht erreicht!
        }
        
        // Versuche, den Job auch in allen neu erzeugten Pfaden zu platzieren
        if (debugEnabled) {
            System.out.println("\n--- Starte Platzierung in NEU erzeugten Pfaden ---");
            System.out.println("Anzahl neuer Pfade: " + newPaths.size());
        }

        // Geht nur rein, wenn vorher neue Pfade erzeugt wurden, also newPaths.size() > 0, diese aber nicht mehr beim nächsten
        // Schleifendurchlauf abgearbeitet werden können, weil im Controller bereits die Jobliste durchlaufen wurde
        for (int newPathIndex = 0; newPathIndex < newPaths.size(); newPathIndex++) {
            MultiPlate_DataClasses newPath = newPaths.get(newPathIndex);
            if (debugEnabled) {
                System.out.println("\n[Neuer Pfad " + (newPathIndex+1) + ": " + newPath.pathDescription + "]");
                System.out.println("Versuche Job " + originalJob.id + " zu platzieren. Strategie: " + (newPath.strategy == MultiPlate_DataClasses.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth"));
                System.out.println("Freie Rechtecke im neuen Pfad: " + newPath.freeRects.size());
            }
            // Erzeuge eine Kopie des Jobs für den neuen Pfad
            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);
            // Ergebnisobjekt für die beste Platzierung im neuen Pfad
            MultiPlate_DataClasses.BestFitResult newPathResult = new MultiPlate_DataClasses.BestFitResult();
            // Prüfe alle freien Rechtecke im neuen Pfad, ob der Job dort passt
            for (int j = 0; j < newPath.freeRects.size(); j++) {
                MultiPlate_DataClasses.FreeRectangle rect = newPath.freeRects.get(j);
                testAndUpdateBestFit(job.width, job.height, rect, false, newPathResult);
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, newPathResult);
            }
            // Falls ein passendes Rechteck gefunden wurde, platziere den Job im neuen Pfad
            if (newPathResult.bestRect != null) {
                if (debugEnabled) {
                    System.out.println(newPath.pathDescription + ": Job " + job.id + " wird platziert auf Rechteck (" + newPathResult.bestRect.x + "," + newPathResult.bestRect.y + "," + newPathResult.bestRect.width + "x" + newPathResult.bestRect.height + ")" + (newPathResult.useRotated ? " [rotiert]" : ""));
                }
                String splittingMethod = newPath.strategy == MultiPlate_DataClasses.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth";
                placeJobInPath(job, newPathResult, newPath, splittingMethod);
                newPath.plateId = plateId;
            } else {
                if (debugEnabled) {
                    System.out.println(newPath.pathDescription + ": Job " + job.id + " konnte im neuen Pfad nicht platziert werden.");
                }
            }
        }

        if (debugEnabled) {
            System.out.println("\nFüge " + newPaths.size() + " neue Pfade hinzu. Gesamtpfade: " + (paths.size() + newPaths.size()));
            System.out.println("Job " + originalJob.id + (anySuccess ? " wurde mindestens einmal platziert." : " konnte nicht platziert werden."));
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
    private void placeJobInPath(Job job, MultiPlate_DataClasses.BestFitResult result, MultiPlate_DataClasses path, String splittingMethod) {
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
        path.placedJobs.add(job);
        if (debugEnabled) {
            System.out.println(path.pathDescription + ": Platzierung: Job " + job.id + " auf Platte '" + path.plate.name + "' an Position (" + job.x + "," + job.y + ") Größe: " + job.width + "x" + job.height + (job.rotated ? " [rotiert]" : "") + ", Split: " + splittingMethod);
        }
        if ("FullWidth".equals(splittingMethod)) {
            splitFreeRectFullWidth(result.bestRect, job, path);
        } else {
            splitFreeRectFullHeight(result.bestRect, job, path);
        }
        List<MultiPlate_DataClasses.FreeRectangle> snapshot = new ArrayList<>();
        for (MultiPlate_DataClasses.FreeRectangle fr : path.freeRects) {
            snapshot.add(new MultiPlate_DataClasses.FreeRectangle(fr.x, fr.y, fr.width, fr.height));
        }
        path.freeRectsPerStep.add(snapshot);
        if (debugEnabled) {
            System.out.println(path.pathDescription + ": Snapshot freie Rechtecke nach Platzierung: " + path.freeRects.size());
        }
    }

    public void testAndUpdateBestFit(double testWidth, double testHeight, MultiPlate_DataClasses.FreeRectangle rect, boolean rotated, MultiPlate_DataClasses.BestFitResult result) {
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

    public void splitFreeRectFullWidth(MultiPlate_DataClasses.FreeRectangle rect, Job job, MultiPlate_DataClasses path) {
        path.freeRects.remove(rect);
        path.lastAddedRects.clear();
        if (job.width < rect.width) {
            MultiPlate_DataClasses.FreeRectangle newRectRight = new MultiPlate_DataClasses.FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, job.height);
            path.freeRects.add(newRectRight);
            path.lastAddedRects.add(newRectRight);
        }
        if (job.height < rect.height) {
            MultiPlate_DataClasses.FreeRectangle newRectBelow = new MultiPlate_DataClasses.FreeRectangle(rect.x, rect.y + job.height, rect.width, rect.height - job.height);
            path.freeRects.add(newRectBelow);
            path.lastAddedRects.add(newRectBelow);
        }
    }

    private void splitFreeRectFullHeight(MultiPlate_DataClasses.FreeRectangle rect, Job job, MultiPlate_DataClasses path) {
        path.freeRects.remove(rect);
        path.lastAddedRects.clear();
        if (job.width < rect.width) {
            MultiPlate_DataClasses.FreeRectangle newRectRight = new MultiPlate_DataClasses.FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, rect.height);
            path.freeRects.add(newRectRight);
            path.lastAddedRects.add(newRectRight);
        }
        if (job.height < rect.height) {
            MultiPlate_DataClasses.FreeRectangle newRectBelow = new MultiPlate_DataClasses.FreeRectangle(rect.x, rect.y + job.height, job.width, rect.height - job.height);
            path.freeRects.add(newRectBelow);
            path.lastAddedRects.add(newRectBelow);
        }
    }

    // Getter für die freien Rechtecke des ersten aktiven Pfads
    public List<MultiPlate_DataClasses.FreeRectangle> getFreeRects() {
        for (MultiPlate_DataClasses path : paths) {
            if (path.isActive) {
                return path.freeRects;
            }
        }
        return new ArrayList<>();
    }

    // Getter für die beste Plate (ohne Visualisierung)
    public Plate getBestPlate() {
        MultiPlate_DataClasses bestPath = null;
        double bestCoverage = 0;

        for (MultiPlate_DataClasses path : paths) {
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

    public List<MultiPlate_DataClasses> getAllPaths() {
        return paths;
    }

    public String getStrategyCodeForPath(MultiPlate_DataClasses path) {
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


    public List<MultiPlate_DataClasses> getPathsAndFailedJobsOverviewData() {
        // Setze die jobIds für Übersicht
        for (MultiPlate_DataClasses path : paths) {
            path.jobIds = new ArrayList<>();
            for (Job job : path.plate.jobs) path.jobIds.add(job.id);
        }
        return paths;
    }

}