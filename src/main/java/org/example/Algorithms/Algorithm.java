package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.PlatePath;
import org.example.DataClasses.Plate;

public class Algorithm {

    List<PlatePath> paths;
    Plate originalPlate;
    boolean debugEnabled = false;
    private int pathCounter = 0;
    private String pathIdPrefix = "";
    private List<Plate> plateSequence = new ArrayList<>();
    private int currentPlateIndex = 0;
    private List<PlatePath> completedPlatePaths = new ArrayList<>();

    private static final boolean ALWAYS_BRANCH = true;

    public Algorithm() { }

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

    // Speichert den Anfangszustand: Die komplette Platte ist am Anfang eine einzige freie Fläche
    private void addInitialSnapshot(PlatePath path) {
        if (path.freeRectsPerStep == null) {
            path.freeRectsPerStep = new ArrayList<>();
        }
        
        // Erstelle eine neue Liste für den Anfangszustand
        List<PlatePath.FreeRectangle> anfangszustand = new ArrayList<>();
        
        // Die komplette Platte ist am Anfang frei
        PlatePath.FreeRectangle kompletteFlaeche = new PlatePath.FreeRectangle(0, 0, path.plate.width, path.plate.height);
        anfangszustand.add(kompletteFlaeche);
        
        // Speichere diesen Anfangszustand
        path.freeRectsPerStep.add(anfangszustand);
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
            
            // Prüfe, ob der Job bereits in diesem Pfad platziert wurde
            boolean bereitsPlatziert = false;
            for (int i = 0; i < newPath.plate.jobs.size(); i++) {
                Job job = newPath.plate.jobs.get(i);
                if (job.id == originalJob.id) {
                    bereitsPlatziert = true;
                    break;
                }
            }
            
            if (bereitsPlatziert) {
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

    // Prüft, ob ein Job in ein freies Rechteck passt und ob diese Platzierung besser ist als die bisherige beste
    public void testAndUpdateBestFit(double testWidth, double testHeight, PlatePath.FreeRectangle rect, boolean rotated, PlatePath.BestFitResult result) {
        // Prüfe, ob der Job in das Rechteck passt
        boolean passtRein = (testWidth <= rect.width) && (testHeight <= rect.height);
        
        if (passtRein) {
            // Berechne, wie viel Platz übrig bleibt (horizontal und vertikal)
            double uebrigHorizontal = rect.width - testWidth;
            double uebrigVertikal = rect.height - testHeight;
            
            // Wähle den kleineren der beiden Restwerte (Short Side Fit Heuristik)
            double kuerzereSeite = Math.min(uebrigHorizontal, uebrigVertikal);
            
            // Ist diese Platzierung besser als die bisherige beste?
            if (kuerzereSeite < result.bestScore) {
                result.bestScore = kuerzereSeite;
                result.bestRect = rect;
                result.useRotated = rotated;
                result.bestWidth = testWidth;
                result.bestHeight = testHeight;
            }
        }
    }

    // Teilt ein freies Rechteck nach dem FullWidth-Muster (Breite wird komplett genutzt)
    public void splitFreeRectFullWidth(PlatePath.FreeRectangle rect, Job job, PlatePath path) {
        // Entferne das alte Rechteck, da es jetzt belegt ist
        path.freeRects.remove(rect);
        path.lastAddedRects.clear();
        
        // Wenn der Job schmaler ist als das Rechteck: erstelle neues Rechteck rechts davon
        if (job.width < rect.width) {
            double neueBreite = rect.width - job.width;
            PlatePath.FreeRectangle rechtsRechteck = new PlatePath.FreeRectangle(
                rect.x + job.width,  // X-Position: rechts vom Job
                rect.y,              // Y-Position: gleiche Höhe wie Job
                neueBreite,          // Breite: Restbreite
                job.height           // Höhe: nur so hoch wie der Job
            );
            path.freeRects.add(rechtsRechteck);
            path.lastAddedRects.add(rechtsRechteck);
        }
        
        // Wenn der Job niedriger ist als das Rechteck: erstelle neues Rechteck darunter
        if (job.height < rect.height) {
            double neueHoehe = rect.height - job.height;
            PlatePath.FreeRectangle rechteckUnten = new PlatePath.FreeRectangle(
                rect.x,                // X-Position: gleiche wie ursprüngliches Rechteck
                rect.y + job.height,   // Y-Position: unterhalb des Jobs
                rect.width,            // Breite: komplette ursprüngliche Breite
                neueHoehe              // Höhe: Resthöhe
            );
            path.freeRects.add(rechteckUnten);
            path.lastAddedRects.add(rechteckUnten);
        }
    }

    // Teilt ein freies Rechteck nach dem FullHeight-Muster (Höhe wird komplett genutzt)
    public void splitFreeRectFullHeight(PlatePath.FreeRectangle rect, Job job, PlatePath path) {
        // Entferne das alte Rechteck, da es jetzt belegt ist
        path.freeRects.remove(rect);
        path.lastAddedRects.clear();
        
        // Wenn der Job schmaler ist als das Rechteck: erstelle neues Rechteck rechts davon
        if (job.width < rect.width) {
            double neueBreite = rect.width - job.width;
            PlatePath.FreeRectangle rechtsRechteck = new PlatePath.FreeRectangle(
                rect.x + job.width,  // X-Position: rechts vom Job
                rect.y,              // Y-Position: gleiche wie ursprüngliches Rechteck
                neueBreite,          // Breite: Restbreite
                rect.height          // Höhe: komplette ursprüngliche Höhe
            );
            path.freeRects.add(rechtsRechteck);
            path.lastAddedRects.add(rechtsRechteck);
        }
        
        // Wenn der Job niedriger ist als das Rechteck: erstelle neues Rechteck darunter
        if (job.height < rect.height) {
            double neueHoehe = rect.height - job.height;
            PlatePath.FreeRectangle rechteckUnten = new PlatePath.FreeRectangle(
                rect.x,              // X-Position: gleiche wie Job
                rect.y + job.height, // Y-Position: unterhalb des Jobs
                job.width,           // Breite: nur so breit wie der Job
                neueHoehe            // Höhe: Resthöhe
            );
            path.freeRects.add(rechteckUnten);
            path.lastAddedRects.add(rechteckUnten);
        }
    }

    // Gibt die freien Rechtecke des ersten aktiven Pfads zurück
    public List<PlatePath.FreeRectangle> getFreeRects() {
        // Gehe durch alle Pfade
        for (int i = 0; i < paths.size(); i++) {
            PlatePath path = paths.get(i);
            
            // Wenn dieser Pfad aktiv ist, gib seine freien Rechtecke zurück
            if (path.isActive) {
                return path.freeRects;
            }
        }
        
        // Falls kein aktiver Pfad gefunden wurde, gib eine leere Liste zurück
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

    // Bereitet alle Pfade für die Übersicht vor (sammelt Job-IDs)
    public List<PlatePath> getPathsAndFailedJobsOverviewData() {
        // Hole alle Pfade (abgeschlossene + aktuelle)
        List<PlatePath> allePfade = getAllPaths();
        
        // Gehe durch alle Pfade und sammle die Job-IDs
        for (int i = 0; i < allePfade.size(); i++) {
            PlatePath path = allePfade.get(i);
            path.jobIds = new ArrayList<>();
            
            // Sammle alle Job-IDs von den platzierten Jobs
            for (int j = 0; j < path.plate.jobs.size(); j++) {
                Job job = path.plate.jobs.get(j);
                path.jobIds.add(job.id);
            }
        }
        
        return allePfade;
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

    // Versucht, ein Text-Feld in einem Objekt zu setzen (per Reflection)
    private void trySetStringField(Object obj, String fieldName, String value) {
        // Prüfe, ob alle Parameter vorhanden sind
        if (obj == null) return;
        if (fieldName == null) return;
        if (value == null) return;
        
        try {
            // Hole das Feld mit dem angegebenen Namen
            java.lang.reflect.Field feld = obj.getClass().getDeclaredField(fieldName);
            // Mache das Feld zugänglich (auch wenn es private ist)
            feld.setAccessible(true);
            // Setze den neuen Wert
            feld.set(obj, value);
        } catch (NoSuchFieldException fehler) {
            // Feld existiert nicht - ignorieren (ist ok, wir probieren mehrere Namen)
        } catch (Exception andererFehler) {
            // Anderer Fehler - ebenfalls ignorieren
        }
    }

    // Versucht, ein Zahlen-Feld in einem Objekt zu setzen (per Reflection)
    private void trySetIntField(Object obj, String fieldName, Integer value) {
        // Prüfe, ob alle Parameter vorhanden sind
        if (obj == null) return;
        if (fieldName == null) return;
        if (value == null) return;
        
        try {
            // Hole das Feld mit dem angegebenen Namen
            java.lang.reflect.Field feld = obj.getClass().getDeclaredField(fieldName);
            // Mache das Feld zugänglich (auch wenn es private ist)
            feld.setAccessible(true);
            
            // Prüfe, welchen Typ das Feld hat
            Class<?> feldTyp = feld.getType();
            
            // Wenn das Feld ein int oder Integer ist, setze die Zahl direkt
            boolean istZahlTyp = (feldTyp == int.class) || (feldTyp == Integer.class);
            if (istZahlTyp) {
                feld.set(obj, value);
            } else {
                // Falls es ein String-Feld ist, wandle die Zahl in Text um
                String textWert = String.valueOf(value);
                feld.set(obj, textWert);
            }
        } catch (NoSuchFieldException fehler) {
            // Feld existiert nicht - ignorieren (ist ok, wir probieren mehrere Namen)
        } catch (Exception andererFehler) {
            // Anderer Fehler - ebenfalls ignorieren
        }
    }
}