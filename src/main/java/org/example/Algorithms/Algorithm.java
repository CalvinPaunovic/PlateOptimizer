package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.PlatePath;
import org.example.DataClasses.Plate;

public class Algorithm {

    List<PlatePath> paths; // aktive Pfade der aktuell bearbeiteten Platte
    Plate originalPlate; // erste Platte
    boolean debugEnabled = false;
    private int pathCounter = 0;
    private String pathIdPrefix = "";
    private List<Plate> plateSequence = new ArrayList<>();
    private int currentPlateIndex = 0;
    private List<PlatePath> completedPlatePaths = new ArrayList<>();

    private static final boolean ALWAYS_BRANCH = true;

    public Algorithm() { }

    // Konstruktor mit Flag für unendliche Platten
    public Algorithm(List<Plate> plateInfos, boolean unlimitedPlates) {
        if (plateInfos != null) this.plateSequence.addAll(plateInfos);
        this.paths = new ArrayList<>();
        if (!plateSequence.isEmpty()) initializePathsForPlate(plateSequence.get(0));
    }

    public Algorithm(List<Plate> plateInfos, String pathIdPrefix) {
        if (plateInfos != null) this.plateSequence.addAll(plateInfos);
        this.pathIdPrefix = pathIdPrefix == null ? "" : pathIdPrefix;
        this.paths = new ArrayList<>();
        if (!plateSequence.isEmpty()) initializePathsForPlate(plateSequence.get(0));
    }

    public Algorithm(List<Plate> plateInfos) {
        if (plateInfos != null) {
            this.plateSequence.addAll(plateInfos); 
        }
        this.paths = new ArrayList<>();
        if (!plateSequence.isEmpty()) {
            initializePathsForPlate(plateSequence.get(0));
        }
    }

    // Initialisiert Basis-Pfade (FULL_WIDTH / FULL_HEIGHT) für die angegebene Platte
    private void initializePathsForPlate(Plate basePlate) {
        // Ursprüngliches Verhalten (erste Platte klonen)
        this.originalPlate = new Plate(basePlate.name, basePlate.width, basePlate.height, basePlate.plateId);
        this.originalPlate.name = basePlate.name;
        this.originalPlate.parentPathIndex = 1;
        this.paths.clear();
        pathCounter++; // FULL_WIDTH
        PlatePath fullWidthPath = new PlatePath(new Plate(basePlate.name, basePlate.width, basePlate.height, basePlate.plateId), pathIdPrefix + pathCounter, PlatePath.Strategy.FULL_WIDTH, pathCounter);
        fullWidthPath.plate.name = basePlate.name; fullWidthPath.plateId = basePlate.plateId; paths.add(fullWidthPath); addInitialSnapshot(fullWidthPath);
        pathCounter++; // FULL_HEIGHT
        PlatePath fullHeightPath = new PlatePath(new Plate(basePlate.name, basePlate.width, basePlate.height, basePlate.plateId), pathIdPrefix + pathCounter, PlatePath.Strategy.FULL_HEIGHT, pathCounter);
        fullHeightPath.plate.name = basePlate.name; fullHeightPath.plateId = basePlate.plateId; paths.add(fullHeightPath); addInitialSnapshot(fullHeightPath);
        syncTotalPathCount();
        if (debugEnabled) System.out.println("[MPA] Initialisierte Platte " + (currentPlateIndex+1) + "/" + plateSequence.size() + ": " + basePlate.name + " prefix='" + pathIdPrefix + "'");
    }

    private void addInitialSnapshot(PlatePath path) {
        if (path.freeRectsPerStep == null) path.freeRectsPerStep = new java.util.ArrayList<>();
        // Anfangszustand: eine freie Fläche = komplette Platte
        java.util.List<PlatePath.FreeRectangle> init = new java.util.ArrayList<>();
        init.add(new PlatePath.FreeRectangle(0,0,path.plate.width,path.plate.height));
        path.freeRectsPerStep.add(init);
    }

    private void syncTotalPathCount() {
        if (paths != null) {
            for (int i = 0; i < paths.size(); i++) {
                PlatePath p = paths.get(i);
                p.totalPathCount = pathCounter;
            }
        }
    }

    // Versucht, ein Job auf der aktuellen Platte zu platzieren (inkl. Erzeugung alternativer Pfade)
    private boolean attemptPlaceOnCurrentPlate(Job originalJob, String plateId) {
        List<PlatePath> newBranchPaths = new ArrayList<>();
        boolean anySuccess = false;
        int initialSize = paths.size();
        for (int pathIndex = 0; pathIndex < initialSize; pathIndex++) {
            PlatePath currentPath = paths.get(pathIndex);
            int jobsBeforePlacement = currentPath.plate.jobs.size();

            // Debug-Ausgabe: Pfad-Info
            if (debugEnabled) {
                System.out.println("\n--- [Pfad " + (pathIndex+1) + ": " + currentPath.pathId + " | Platte=" + currentPath.plate.name + "] ---");
                System.out.println("Strategie: " + (currentPath.strategy == PlatePath.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth"));
                System.out.println("Freie Rechtecke: " + currentPath.freeRects.size());
            }

            // Erzeuge eine Kopie des Jobs für diesen Pfad
            Job jobCandidate = new Job(originalJob.id, originalJob.width, originalJob.height);
            // Ergebnisobjekt für die beste Platzierung
            PlatePath.BestFitResult result = new PlatePath.BestFitResult();

            // Suche das beste freie Rechteck für diesen Job (ggf. auch rotiert)
            for (int j = 0; j < currentPath.freeRects.size(); j++) {
                PlatePath.FreeRectangle rect = currentPath.freeRects.get(j);
                testAndUpdateBestFit(jobCandidate.width, jobCandidate.height, rect, false, result);
                if (Main.rotateJobs) testAndUpdateBestFit(jobCandidate.height, jobCandidate.width, rect, true, result);
            }

            // Falls kein Platz gefunden wurde:
            if (result.bestRect == null) {
                // Debug-Ausgabe: Job konnte nicht platziert werden
                if (debugEnabled) System.out.println(currentPath.pathId + ": Job " + jobCandidate.id + " konnte NICHT platziert werden (Plate#" + (currentPlateIndex+1) + ").");
                // Nur markieren, kein Branching erzeugen
                currentPath.failedJobs.add(jobCandidate.id);

            // Geht hier rein, wenn direkt ein Platz gefunden wurde
            } else {
                if (debugEnabled) System.out.println("[MP] Plate#" + (currentPlateIndex+1) + " Path " + currentPath.pathId + " platziert Job " + jobCandidate.id + " (" + result.bestRect.x + "," + result.bestRect.y + "," + result.bestRect.width + "x" + result.bestRect.height + ")" + (result.useRotated ? " [rot]" : "") );
                
                // Platziere den Job im aktuellen Pfad
                String splittingMethod = currentPath.strategy == PlatePath.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth";
                placeJobInPath(jobCandidate, result, currentPath, splittingMethod);
                currentPath.plateId = plateId;
                anySuccess = true;
                if (result.bestRect != null) { // Erfolgspfad
                    // Branch nur wenn nicht erster Job (jobsBeforePlacement > 0)
                    if (ALWAYS_BRANCH && jobsBeforePlacement > 0) {
                        // Alternativen Pfad mit entgegengesetzter Strategie erzeugen
                        PlatePath.Strategy newStrategy = currentPath.strategy == PlatePath.Strategy.FULL_HEIGHT ? PlatePath.Strategy.FULL_WIDTH : PlatePath.Strategy.FULL_HEIGHT;
                        // immer einfache neue Pfad-ID (ohne Hierarchie)
                        pathCounter++;
                        String newPathId = pathIdPrefix + pathCounter;
                        PlatePath altPath = new PlatePath(currentPath, newPathId, newStrategy, currentPath.pathId, jobCandidate.id, pathCounter);
                        altPath.strategy = newStrategy; altPath.plateId = plateId;
                        // setze Parent-/Fork-Informationen für spätere Analyse/Visualisierung
                        setParentAndForkInfo(altPath, currentPath.pathId, jobCandidate.id);
                        if (currentPath.freeRectsPerStep != null) {
                            altPath.freeRectsPerStep = new java.util.ArrayList<>();
                            for (java.util.List<PlatePath.FreeRectangle> snap : currentPath.freeRectsPerStep) {
                                java.util.List<PlatePath.FreeRectangle> copySnap = new java.util.ArrayList<>();
                                for (PlatePath.FreeRectangle fr : snap) copySnap.add(new PlatePath.FreeRectangle(fr.x, fr.y, fr.width, fr.height));
                                altPath.freeRectsPerStep.add(copySnap);
                            }
                        }
                        // Letzten Job (soeben platzierter) Referenz holen
                        Job lastJob = altPath.plate.jobs.get(altPath.plate.jobs.size() - 1);
                        // Alte (fuer ursprüngliche Strategie) hinzugefügte freeRects entfernen
                        java.util.List<PlatePath.FreeRectangle> rectsToRemove = new java.util.ArrayList<>(altPath.lastAddedRects);
                        for (PlatePath.FreeRectangle rem : rectsToRemove) {
                            altPath.freeRects.removeIf(r -> r.x == rem.x && r.y == rem.y && r.width == rem.width && r.height == rem.height);
                        }
                        // Ursprüngliches Rechteck rekonstruieren
                        double originalWidth = lastJob.width; double originalHeight = lastJob.height;
                        for (PlatePath.FreeRectangle rem : rectsToRemove) {
                            if (rem.x == lastJob.x + lastJob.width && rem.y == lastJob.y) originalWidth += rem.width; else if (rem.x == lastJob.x && rem.y == lastJob.y + lastJob.height) originalHeight += rem.height;
                        }
                        PlatePath.FreeRectangle originalRect = new PlatePath.FreeRectangle(lastJob.x, lastJob.y, originalWidth, originalHeight);
                        // Neu splitten nach neuer Strategie
                        if (newStrategy == PlatePath.Strategy.FULL_HEIGHT) {
                            splitFreeRectFullHeight(originalRect, lastJob, altPath);
                            lastJob.splittingMethod = "FullHeight";
                        } else {
                            splitFreeRectFullWidth(originalRect, lastJob, altPath);
                            lastJob.splittingMethod = "FullWidth";
                        }
                        // Snapshot nach UMsplittung hinzufügen
                        java.util.List<PlatePath.FreeRectangle> snapshot = new java.util.ArrayList<>();
                        for (PlatePath.FreeRectangle fr : altPath.freeRects) snapshot.add(new PlatePath.FreeRectangle(fr.x, fr.y, fr.width, fr.height));
                        altPath.freeRectsPerStep.add(snapshot);
                        newBranchPaths.add(altPath);
                        if (debugEnabled) System.out.println("[MP] Branch -> Neuer AltPfad " + altPath.pathId + " (Strategie " + (newStrategy==PlatePath.Strategy.FULL_HEIGHT?"FullHeight":"FullWidth") + ") nach Job " + jobCandidate.id);
                    }
                }
            }
            if (debugEnabled) System.out.println("pathIndex: " + pathIndex + ", initialSize: " + initialSize);

            // Erneuter Schleifendurchlauf, wenn initialSize noch nicht erreicht!
        }
        
        // Versuche, den Job auch in allen neu erzeugten Pfaden zu platzieren
        if (debugEnabled) {
            System.out.println("\n--- Starte Platzierung in NEU erzeugten Pfaden ---");
            System.out.println("Anzahl neuer Pfade: " + newBranchPaths.size());
        }

        // Geht nur rein, wenn vorher neue Pfade erzeugt wurden, also newPaths.size() > 0, diese aber nicht mehr beim nächsten
        // Schleifendurchlauf abgearbeitet werden können, weil im Controller bereits die Jobliste durchlaufen wurde
        for (int newPathIndex = 0; newPathIndex < newBranchPaths.size(); newPathIndex++) {
            PlatePath newPath = newBranchPaths.get(newPathIndex);
            boolean alreadyPlaced = newPath.plate.jobs.stream().anyMatch(j -> j.id == originalJob.id);
            if (alreadyPlaced) {
                if (debugEnabled) System.out.println("[MP] Job " + originalJob.id + " already placed in new path " + newPath.pathId + ", skipping attempt.");
                continue;
            }
            if (debugEnabled) {
                System.out.println("\n[Neuer Pfad " + (newPathIndex+1) + ": " + newPath.pathId + "]");
                System.out.println("Versuche Job " + originalJob.id + " zu platzieren. Strategie: " + (newPath.strategy == PlatePath.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth"));
                System.out.println("Freie Rechtecke im neuen Pfad: " + newPath.freeRects.size());
            }
            // Erzeuge eine Kopie des Jobs für den neuen Pfad
            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);
            // Ergebnisobjekt für die beste Platzierung im neuen Pfad
            PlatePath.BestFitResult newPathResult = new PlatePath.BestFitResult();
            // Prüfe alle freien Rechtecke im neuen Pfad, ob der Job dort passt
            for (int j = 0; j < newPath.freeRects.size(); j++) {
                PlatePath.FreeRectangle rect = newPath.freeRects.get(j);
                testAndUpdateBestFit(job.width, job.height, rect, false, newPathResult);
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, newPathResult);
            }
            // Falls ein passendes Rechteck gefunden wurde, platziere den Job im neuen Pfad
            if (newPathResult.bestRect != null) {
                String splittingMethod = newPath.strategy == PlatePath.Strategy.FULL_HEIGHT ? "FullHeight" : "FullWidth";
                placeJobInPath(job, newPathResult, newPath, splittingMethod);
                newPath.plateId = plateId; anySuccess = true;
                if (debugEnabled) System.out.println("[MP] Plate#" + (currentPlateIndex+1) + " AltPath(after) " + newPath.pathId + " platziert Job " + job.id + " (" + newPathResult.bestRect.x + "," + newPathResult.bestRect.y + "," + newPathResult.bestRect.width + "x" + newPathResult.bestRect.height + ")" + (newPathResult.useRotated ? " [rot]" : ""));
            } else {
                if (debugEnabled) {
                    System.out.println(newPath.pathId + ": Job " + job.id + " konnte im neuen Pfad nicht platziert werden.");
                }
            }
        }

        if (debugEnabled) {
            System.out.println("\nFüge " + newBranchPaths.size() + " neue Pfade hinzu. Gesamtpfade: " + (paths.size() + newBranchPaths.size()));
            System.out.println("Job " + originalJob.id + (anySuccess ? " wurde mindestens einmal platziert." : " konnte nicht platziert werden."));
        }
        // Füge alle neuen Pfade zur Pfadliste hinzu
        paths.addAll(newBranchPaths);
        // Nach Hinzufügen aller neuen Pfade erneut synchronisieren
        syncTotalPathCount();
        return anySuccess;
    }

    // Wechsel zur nächsten Platte (falls vorhanden)
    private boolean switchToNextPlate() {
        if (currentPlateIndex + 1 >= plateSequence.size()) {
            return false; // keine weitere Platte im aktuellen Algorithmus
        }
        // Zusammenfassung aktuelle Platte
        if (debugEnabled) {
            int jobsTotal = 0; int failedTotal = 0;
            for (PlatePath p : paths) { jobsTotal += p.plate.jobs.size(); failedTotal += p.failedJobs.size(); }
            System.out.println("\n[MP] >>> Abschluss Platte#" + (currentPlateIndex+1) + " '" + plateSequence.get(currentPlateIndex).name + "' : placedJobs=" + jobsTotal + " failedMarked=" + failedTotal + " Pfade=" + paths.size());
        }
        completedPlatePaths.addAll(paths);
        currentPlateIndex++;
        initializePathsForPlate(plateSequence.get(currentPlateIndex));
        if (debugEnabled) System.out.println("[MP] >>> Wechsel zu Platte#" + (currentPlateIndex+1) + " '" + plateSequence.get(currentPlateIndex).name + "'");
        return true;
    }

    /**
     * Versucht einen Job auf der aktuellen Platte zu platzieren; falls nicht möglich wird automatisch zur nächsten Platte gewechselt.
     * Gibt true zurück, wenn der Job letztlich auf irgendeiner Platte platziert wurde.
     */
    public boolean placeJob(Job originalJob, String plateId) {
        if (plateSequence.isEmpty()) return false;
        boolean placed = attemptPlaceOnCurrentPlate(originalJob, plateSequence.get(currentPlateIndex).plateId);
        while (!placed) {
            boolean advanced = switchToNextPlate();
            if (!advanced) break; // keine weitere Platte
            placed = attemptPlaceOnCurrentPlate(originalJob, plateSequence.get(currentPlateIndex).plateId);
        }
        if (debugEnabled && placed) System.out.println("[MP] Job " + originalJob.id + " FINAL platziert auf Platte#" + (currentPlateIndex+1));
        if (debugEnabled && !placed) System.out.println("[MP] Job " + originalJob.id + " konnte auf keiner Platte platziert werden.");
        return placed;
    }

    /**
     * Platziert einen Job in einem Pfad und aktualisiert die freien Rechtecke.
     * Setzt alle relevanten Felder des Jobs, fügt ihn zur Plate und zur placedJobs-Liste hinzu.
     * Splitte das belegte Rechteck nach der gewählten Strategie.
     * Speichere einen Snapshot der freien Rechtecke nach dem Platzieren.
     */
    public void placeJobInPath(Job job, PlatePath.BestFitResult result, PlatePath path, String splittingMethod) {
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

        if (debugEnabled) System.out.println(path.pathId + ": Platzierung: Job " + job.id + " auf Platte '" + path.plate.name + "' an Position (" + job.x + "," + job.y + ") Größe: " + job.width + "x" + job.height + (job.rotated ? " [rotiert]" : "") + ", Split: " + splittingMethod);
        
        if ("FullWidth".equals(splittingMethod)) {
            splitFreeRectFullWidth(result.bestRect, job, path);
        } else {
            splitFreeRectFullHeight(result.bestRect, job, path);
        }
        // Entferne initialen Snapshot-Platzhalter falls dies der erste echte Job ist und doppelte Darstellung vermieden werden soll
        // (Optional belassen wir ihn für Step 0 Visualisierung – daher nichts entfernen.)
        java.util.List<PlatePath.FreeRectangle> snapshot = new java.util.ArrayList<>();
        for (PlatePath.FreeRectangle fr : path.freeRects) {
            snapshot.add(new PlatePath.FreeRectangle(fr.x, fr.y, fr.width, fr.height));
        }
        path.freeRectsPerStep.add(snapshot);
        if (debugEnabled) {
            System.out.println(path.pathId + ": Snapshot freie Rechtecke nach Platzierung: " + path.freeRects.size());
        }
    }

    public void testAndUpdateBestFit(double testWidth, double testHeight, PlatePath.FreeRectangle rect, boolean rotated, PlatePath.BestFitResult result) {
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

    public void splitFreeRectFullWidth(PlatePath.FreeRectangle rect, Job job, PlatePath path) {
        path.freeRects.remove(rect);
        path.lastAddedRects.clear();
        if (job.width < rect.width) {
            PlatePath.FreeRectangle newRectRight = new PlatePath.FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, job.height);
            path.freeRects.add(newRectRight);
            path.lastAddedRects.add(newRectRight);
        }
        if (job.height < rect.height) {
            PlatePath.FreeRectangle newRectBelow = new PlatePath.FreeRectangle(rect.x, rect.y + job.height, rect.width, rect.height - job.height);
            path.freeRects.add(newRectBelow);
            path.lastAddedRects.add(newRectBelow);
        }
    }

    public void splitFreeRectFullHeight(PlatePath.FreeRectangle rect, Job job, PlatePath path) {
        path.freeRects.remove(rect);
        path.lastAddedRects.clear();
        if (job.width < rect.width) {
            PlatePath.FreeRectangle newRectRight = new PlatePath.FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, rect.height);
            path.freeRects.add(newRectRight);
            path.lastAddedRects.add(newRectRight);
        }
        if (job.height < rect.height) {
            PlatePath.FreeRectangle newRectBelow = new PlatePath.FreeRectangle(rect.x, rect.y + job.height, job.width, rect.height - job.height);
            path.freeRects.add(newRectBelow);
            path.lastAddedRects.add(newRectBelow);
        }
    }

    // Getter der freien Rechtecke des ersten aktiven Pfads auf aktueller Platte
    public List<PlatePath.FreeRectangle> getFreeRects() {
        for (PlatePath path : paths) {
            if (path.isActive) {
                return path.freeRects;
            }
        }
        return new ArrayList<>();
    }

    // Beste Plate nur auf aktueller Platte (verhalten beibehalten)
    public Plate getBestPlate() {
        PlatePath bestPath = null;
        double bestCoverage = 0;
        for (PlatePath path : paths) {
            if (path.isActive) {
                double coverage = 0; // (Coverage-Berechnung ggf. später)
                if (coverage > bestCoverage) {
                    bestCoverage = coverage; bestPath = path;
                }
            }
        }
        if (bestPath != null) return bestPath.plate; else return originalPlate;
    }

    // Liefert alle Pfade über alle bisher bearbeiteten Platten
    public List<PlatePath> getAllPaths() {
        List<PlatePath> all = new ArrayList<>(completedPlatePaths);
        all.addAll(paths);
        return all;
    }

    public List<PlatePath> getPathsAndFailedJobsOverviewData() {
        // Alle Pfade (abgeschlossene + aktuelle) vorbereiten
        List<PlatePath> all = getAllPaths();
        for (PlatePath path : all) {
            path.jobIds = new ArrayList<>();
            for (Job job : path.plate.jobs) path.jobIds.add(job.id);
        }
        return all;
    }

    public int getPathCounter() { return pathCounter; }

    // Helper: setze Parent- und Fork-Infos auf dem neuen Pfad für Controller/Matrix (mehrere Feldnamen versuchen)
    private void setParentAndForkInfo(PlatePath pathObj, String parentPathId, Integer forkJobId) {
        if (pathObj == null) return;
        // mögliche Feldnamen für Parent-Id
        String[] parentNames = { "parentPathId", "parentId", "previousPathId", "originParentPathId", "parentPath", "parent" };
        for (String n : parentNames) trySetStringField(pathObj, n, parentPathId);
        // mögliche Feldnamen für Fork-Job-Id
        String[] forkNames = { "forkJobId", "startFromJobId", "branchFromJobId", "copiedAtJobId", "splitFromJobId", "forkJob" };
        for (String n : forkNames) trySetIntField(pathObj, n, forkJobId);
    }

    private void trySetStringField(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || value == null) return;
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (NoSuchFieldException nsf) {
            // Feld nicht vorhanden — ignorieren
        } catch (Exception ex) {
            // andere Fehler — ignorieren
        }
    }

    private void trySetIntField(Object obj, String fieldName, Integer value) {
        if (obj == null || fieldName == null || value == null) return;
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Class<?> t = f.getType();
            if (t == int.class || t == Integer.class) f.set(obj, value);
            else {
                // falls String erwartet wird, setze String-Repräsentation
                f.set(obj, String.valueOf(value));
            }
        } catch (NoSuchFieldException nsf) {
            // Feld nicht vorhanden — ignorieren
        } catch (Exception ex) {
            // andere Fehler — ignorieren
        }
    }
}