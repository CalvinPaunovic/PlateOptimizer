package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.Visualizer.PlateVisualizer;

public class MaxRectBF_MultiPlate {

    public static class AlgorithmPath {
        public Plate plate;                              // Die Platte mit allen platzierten Jobs in diesem Pfad
        public List<FreeRectangle> freeRects;           // Aktuelle freie Rechtecke auf der Platte
        public String pathDescription;                   // Beschreibung des Pfads für Debugging/Logging
        public Integer parentPathIndex;                 // Index des Eltern-Pfads, von dem sich dieser Pfad abgespalten hat
        // NEU: Liste der freien Rechtecke nach jedem Schritt
        public List<List<FreeRectangle>> freeRectsPerStep = new ArrayList<>();
        int placementCounter;                     // Zähler für die Reihenfolge der Job-Platzierung
        List<FreeRectangle> lastAddedRects;      // Die letzten hinzugefügten freien Rechtecke (für Rollback bei Pfad-Splits)
        public boolean isActive;                        // Ob dieser Pfad noch aktiv verfolgt wird
        boolean useFullHeight;                   // Splitting-Strategie: true=FullHeight, false=FullWidth
        List<Integer> failedJobs;                // Liste der Jobs, die in diesem Pfad nicht platziert werden konnten
        String parentPath;                       // Name des Eltern-Pfads, von dem sich dieser Pfad abgespalten hat
        
        /**
         * Konstruktor für einen neuen AlgorithmPath basierend auf einer ursprünglichen Platte.
         * 
         * ZWECK: Initialisiert einen komplett neuen Pfad für die Optimierung
         * - Erstellt eine leere Kopie der Originalplatte
         * - Setzt den gesamten Plattenbereich als einen einzigen freien Bereich
         * - Startet standardmäßig mit FullWidth-Splitting-Strategie
         */
        public AlgorithmPath(Plate originalPlate, String description) {
            this.plate = new Plate(originalPlate.name + " - " + description, originalPlate.width, originalPlate.height);
            this.freeRects = new ArrayList<>();
            this.freeRects.add(new FreeRectangle(0, 0, plate.width, plate.height));  // Gesamte Platte als freier Bereich
            this.lastAddedRects = new ArrayList<>();
            this.pathDescription = description;
            this.placementCounter = 0;
            this.isActive = true;
            this.useFullHeight = false; // Startet mit FullWidth-Strategie
            this.failedJobs = new ArrayList<>(); // Initialisiere Liste für fehlgeschlagene Jobs
            this.parentPath = null; // Initialer Pfad hat keinen Eltern-Pfad
        }
        
        /**
         * Copy-Konstruktor für einen AlgorithmPath basierend auf einem bestehenden Pfad.
         * 
         * ZWECK: Erstellt einen neuen Pfad durch Kopie eines bestehenden Pfads
         * - Wird verwendet für Pfad-Aufteilungen (Branch-and-Bound-Konzept)
         * - Ermöglicht das Testen alternativer Strategien ab einem bestimmten Punkt
         * - Kopiert den aktuellen Zustand (Jobs, freie Rechtecke) und setzt neue Strategie
         * 
         * ANWENDUNG: Wenn ein Job in einem Pfad nicht platziert werden kann,
         * wird ein neuer Pfad mit alternativer Splitting-Methode erstellt
         */
        public AlgorithmPath(AlgorithmPath original, String newDescription) {
            // Erstelle neue Platte mit gleichem Namen-Prefix aber neuer Beschreibung
            this.plate = new Plate(original.plate.name.split(" - ")[0] + " - " + newDescription, original.plate.width, original.plate.height);
            
            // Kopiere alle bereits platzierten Jobs aus dem Original-Pfad
            for (int i = 0; i < original.plate.jobs.size(); i++) {
                Job originalJob = original.plate.jobs.get(i);
                Job copiedJob = new Job(originalJob.id, originalJob.width, originalJob.height);
                copiedJob.x = originalJob.x;
                copiedJob.y = originalJob.y;
                copiedJob.rotated = originalJob.rotated;
                copiedJob.placedOn = this.plate;                    // Verknüpfung zur neuen Platte
                copiedJob.placementOrder = originalJob.placementOrder;
                copiedJob.splittingMethod = originalJob.splittingMethod;
                this.plate.jobs.add(copiedJob);
            }
            // Kopiere die aktuellen freien Rechtecke
            this.freeRects = new ArrayList<>();
            for (int i = 0; i < original.freeRects.size(); i++) {
                FreeRectangle rect = original.freeRects.get(i);
                this.freeRects.add(new FreeRectangle(rect.x, rect.y, rect.width, rect.height));
            }
            // Kopiere die zuletzt hinzugefügten Rechtecke (wichtig für Rollback-Operationen)
            this.lastAddedRects = new ArrayList<>();
            for (int i = 0; i < original.lastAddedRects.size(); i++) {
                FreeRectangle rect = original.lastAddedRects.get(i);
                this.lastAddedRects.add(new FreeRectangle(rect.x, rect.y, rect.width, rect.height));
            }
            // Setze Pfad-spezifische Eigenschaften
            this.pathDescription = newDescription;
            this.placementCounter = original.placementCounter;      // Übernimmt aktuellen Zählerstand
            this.isActive = true;                                   // Neuer Pfad ist automatisch aktiv
            this.useFullHeight = original.useFullHeight;            // Übernimmt zunächst die gleiche Strategie
            this.failedJobs = new ArrayList<>(original.failedJobs); // Kopiere fehlgeschlagene Jobs
            this.parentPath = original.pathDescription;             // Setze Eltern-Pfad
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
    boolean debugEnabled = true; // Standardmäßig true, kann aber von außen gesetzt werden

    public MaxRectBF_MultiPlate(Plate plate) {
        this.originalPlate = plate;
        this.paths = new ArrayList<>();
        
        // Erstelle zwei Standard-Pfade von Anfang an
        AlgorithmPath fullWidthPath = new AlgorithmPath(plate, "Pfad 1 (FullWidth)");
        fullWidthPath.useFullHeight = false; // FullWidth-Strategie
        paths.add(fullWidthPath);
        
        AlgorithmPath fullHeightPath = new AlgorithmPath(plate, "Pfad 2 (FullHeight)");
        fullHeightPath.useFullHeight = true; // FullHeight-Strategie
        paths.add(fullHeightPath);
    }

    // Jeden Job durchgehen, alle aktiven Pfade verfolgen und bei Bedarf neue Pfade erstellen
    public boolean placeJob(Job originalJob) {
        List<AlgorithmPath> newPaths = new ArrayList<>();
        boolean anySuccess = false;
        
        // ERST: Alle bestehenden Pfade durchgehen
        for (int pathIndex = 0; pathIndex < paths.size(); pathIndex++) {
            AlgorithmPath currentPath = paths.get(pathIndex);
            if (!currentPath.isActive) continue;
            
            // Job für diesen Pfad kopieren
            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);
            
            BestFitResult result = new BestFitResult();
            
            // Durchsucht alle verfügbaren freien Rechtecke für diesen Pfad
            for (int i = 0; i < currentPath.freeRects.size(); i++) {
                FreeRectangle rect = currentPath.freeRects.get(i);
                // Originalposition testen
                testAndUpdateBestFit(job.width, job.height, rect, false, result);
                // Gedrehte Position testen
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
            }

            // Wenn kein passender freier Bereich gefunden wurde: Job passt nicht
            if (result.bestRect == null) {
                // Job zur Liste der fehlgeschlagenen Jobs hinzufügen
                currentPath.failedJobs.add(originalJob.id);
                
                // Prüfe ob ein Wechsel der Splitting-Methode möglich ist
                if (currentPath.lastAddedRects.size() >= 2) {
                    String newMethod = currentPath.useFullHeight ? "FullWidth" : "FullHeight";
                    int newPathId = paths.size() + newPaths.size() + 1;
                    
                    // NEUER PFAD: Mit entgegengesetzter Splitting-Methode
                    AlgorithmPath newMethodPath = new AlgorithmPath(currentPath, "Pfad " + newPathId + " (" + newMethod + " ab Job " + job.id + ")");
                    newMethodPath.useFullHeight = !currentPath.useFullHeight; // Setze entgegengesetzte Splitting-Methode
                    
                    // Entferne den fehlgeschlagenen Job aus dem neuen Pfad (da wir ihn neu versuchen)
                    newMethodPath.failedJobs.remove(newMethodPath.failedJobs.size() - 1);
                    
                    // Bestimme den letzten platzierten Job
                    Job lastJob = newMethodPath.plate.jobs.get(newMethodPath.plate.jobs.size() - 1);
                    
                    // Speichere die beiden letzten Rechtecke bevor sie gelöscht werden
                    List<FreeRectangle> rectsToRemove = new ArrayList<>(newMethodPath.lastAddedRects);
                    
                    // Entferne alle FreeRects auf einmal
                    for (FreeRectangle rectToRemove : rectsToRemove) {
                        newMethodPath.freeRects.removeIf(rect -> 
                            rect.x == rectToRemove.x && rect.y == rectToRemove.y && 
                            rect.width == rectToRemove.width && rect.height == rectToRemove.height);
                    }
                    
                    // Berechne das ursprüngliche Rechteck korrekt
                    double originalWidth = lastJob.width;
                    double originalHeight = lastJob.height;
                    
                    // Wenn es ein rechtes Rechteck gab, füge dessen Breite hinzu
                    for (FreeRectangle removed : rectsToRemove) {
                        if (removed.x == lastJob.x + lastJob.width && removed.y == lastJob.y) {
                            // Rechtes Rechteck
                            originalWidth += removed.width;
                        } else if (removed.x == lastJob.x && removed.y == lastJob.y + lastJob.height) {
                            // Unteres Rechteck
                            originalHeight += removed.height;
                        }
                    }
                    
                    FreeRectangle originalRect = new FreeRectangle(lastJob.x, lastJob.y, originalWidth, originalHeight);
                    
                    // Verwende neue Splitting-Methode für den letzten Job im neuen Pfad
                    if (newMethod.equals("FullHeight")) {
                        splitFreeRectFullHeight(originalRect, lastJob, newMethodPath);
                    } else {
                        splitFreeRectFullWidth(originalRect, lastJob, newMethodPath);
                    }
                    
                    // Versuche Job erneut im neuen Pfad zu platzieren
                    BestFitResult newMethodResult = new BestFitResult();
                    for (int i = 0; i < newMethodPath.freeRects.size(); i++) {
                        FreeRectangle rect = newMethodPath.freeRects.get(i);
                        testAndUpdateBestFit(job.width, job.height, rect, false, newMethodResult);
                        if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, newMethodResult);
                    }
                    
                    if (newMethodResult.bestRect != null) {
                        // Job kann im neuen Pfad platziert werden
                        newPaths.add(newMethodPath);
                        anySuccess = true;
                    } else {
                        // Füge den Pfad trotzdem hinzu, aber markiere ihn als fehlgeschlagen
                        newMethodPath.isActive = true; // Lasse ihn aktiv für Visualisierung
                        newMethodPath.failedJobs.add(originalJob.id); // Füge Job zur Failed-Liste hinzu
                        newPaths.add(newMethodPath);
                    }
                }
                
            } else {
                // Job wurde erfolgreich im aktuellen Pfad platziert
                String splittingMethod = currentPath.useFullHeight ? "FullHeight" : "FullWidth";
                placeJobInPath(job, result, currentPath, splittingMethod);
                anySuccess = true;
            }
        }
        
        // DANN: Neue Pfade hinzufügen und deren Jobs platzieren
        for (int newPathIndex = 0; newPathIndex < newPaths.size(); newPathIndex++) {
            AlgorithmPath newPath = newPaths.get(newPathIndex);
            // Job für den neuen Pfad kopieren
            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);
            
            // Job im neuen Pfad platzieren
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
        
        // Kompakte Übersicht nach jedem Job
        printJobSummary(originalJob);
        
        return anySuccess;
    }
    
    // Hilfsmethode zum Platzieren eines Jobs in einem spezifischen Pfad
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
        
        // NEU: Speichere eine Kopie der aktuellen freien Rechtecke nach dem Platzieren des Jobs
        List<FreeRectangle> snapshot = new ArrayList<>();
        for (FreeRectangle fr : path.freeRects) {
            snapshot.add(new FreeRectangle(fr.x, fr.y, fr.width, fr.height));
        }
        path.freeRectsPerStep.add(snapshot);
    }
    
    private void printJobSummary(Job originalJob) {
        if (Main.DEBUG_MultiPath && debugEnabled) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("UEBERSICHT NACH JOB " + originalJob.id + " (" + originalJob.width + "x" + originalJob.height + ")"
                + " auf Platte: " + originalPlate.name);
            System.out.println("=".repeat(80));
            
            for (int pathIndex = 0; pathIndex < paths.size(); pathIndex++) {
                AlgorithmPath path = paths.get(pathIndex);
                if (path.isActive) {
                    double coverage = PlateVisualizer.calculateCoverageRate(path.plate);
                    
                    System.out.printf("\n%s", path.pathDescription);
                    
                    // Zeige Eltern-Pfad falls vorhanden
                    if (path.parentPath != null) {
                        System.out.printf(" (abgespalten von: %s)", path.parentPath);
                    }
                    System.out.println();
                    
                    System.out.printf("   Deckungsrate: %.2f%% | Platzierte Jobs: %d\n", coverage, path.plate.jobs.size());
                    
                    if (path.plate.jobs.isEmpty() && path.failedJobs.isEmpty()) {
                        System.out.println("   Keine Jobs verarbeitet");
                    } else {
                        System.out.print("   Jobs: ");
                        
                        // Sammle alle Job-IDs und erstelle Lookup-Listen
                        java.util.List<Integer> allJobIds = new java.util.ArrayList<>();
                        java.util.List<Job> placedJobsList = new java.util.ArrayList<>();
                        
                        // Sammle platzierte Jobs
                        for (int i = 0; i < path.plate.jobs.size(); i++) {
                            Job job = path.plate.jobs.get(i);
                            if (!allJobIds.contains(job.id)) {
                                allJobIds.add(job.id);
                                placedJobsList.add(job);
                            }
                        }
                        
                        // Sammle fehlgeschlagene Jobs
                        for (int i = 0; i < path.failedJobs.size(); i++) {
                            Integer failedJobId = path.failedJobs.get(i);
                            if (!allJobIds.contains(failedJobId)) {
                                allJobIds.add(failedJobId);
                            }
                        }
                        
                        // Sortiere Job-IDs
                        for (int i = 0; i < allJobIds.size() - 1; i++) {
                            for (int j = 0; j < allJobIds.size() - 1 - i; j++) {
                                if (allJobIds.get(j) > allJobIds.get(j + 1)) {
                                    Integer temp = allJobIds.get(j);
                                    allJobIds.set(j, allJobIds.get(j + 1));
                                    allJobIds.set(j + 1, temp);
                                }
                            }
                        }
                        
                        // Sammle Strategie-Kürzel für Namens-Erweiterung
                        java.util.List<String> strategyCodes = new java.util.ArrayList<>();
                        
                        // Zeige alle Jobs in der richtigen Reihenfolge an
                        boolean first = true;
                        for (int i = 0; i < allJobIds.size(); i++) {
                            Integer jobId = allJobIds.get(i);
                            if (!first) {
                                System.out.print(" -> ");
                            }
                            first = false;
                            
                            // Suche nach platziertem Job
                            boolean foundPlaced = false;
                            for (int j = 0; j < placedJobsList.size(); j++) {
                                Job job = placedJobsList.get(j);
                                if (job.id == jobId.intValue()) {
                                    String rotationInfo = "";
                                    if (job.rotated) {
                                        rotationInfo = "(gedreht)";
                                    }
                                    System.out.printf("Job%d[%s]%s", job.id, job.splittingMethod, rotationInfo);
                                    
                                    // Füge Strategie-Code hinzu
                                    if ("FullWidth".equals(job.splittingMethod)) {
                                        strategyCodes.add("W");
                                    } else if ("FullHeight".equals(job.splittingMethod)) {
                                        strategyCodes.add("H");
                                    } else {
                                        strategyCodes.add("?"); // Fallback
                                    }
                                    foundPlaced = true;
                                    break;
                                }
                            }
                            
                            if (!foundPlaced) {
                                // Job wurde nicht platziert
                                System.out.printf("Job%d[nicht platziert]", jobId);
                                strategyCodes.add("N"); // N für "nicht platziert"
                            }
                        }
                        
                        // Zeige Strategie-Erweiterung
                        if (!strategyCodes.isEmpty()) {
                            System.out.print(" (" + String.join("->", strategyCodes) + ")");
                        }
                        
                        System.out.println();
                    }
                }
            }
            System.out.println();
        }
    }

    // Prüfen, ob Job in originaler oder gedrehter Position einen jeweils kürzeren Abstand vertikal oder horizontal hat
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

    // Freie Rechtecke mit vollständiger Breitenausdehnung nach rechts erzeugen
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

    // Freie Rechtecke mit vollständiger Höhenausdehnung nach rechts erzeugen
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
    
    // Getter für PlateVisualizer Zugriff
    public List<FreeRectangle> getFreeRects() {
        // Gib die freien Rechtecke des ersten aktiven Pfads zurück
        for (AlgorithmPath path : paths) {
            if (path.isActive) {
                return path.freeRects;
            }
        }
        return new ArrayList<>();
    }
    
    // Getter für die beste Plate
    public Plate getBestPlate() {
        AlgorithmPath bestPath = null;
        double bestCoverage = 0;
        
        for (AlgorithmPath path : paths) {
            if (path.isActive) {
                double coverage = PlateVisualizer.calculateCoverageRate(path.plate);
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
    
    // Getter für alle Pfade (für finale Ausgabe)
    public List<AlgorithmPath> getAllPaths() {
        return paths;
    }
    
    // Hilfsmethode: Erstelle Strategie-Code für einen Pfad
    public String getStrategyCodeForPath(AlgorithmPath path) {
        // Sammle alle Job-IDs (platziert und fehlgeschlagen)
        java.util.List<Integer> allJobIds = new java.util.ArrayList<>();
        
        // Füge platzierte Jobs hinzu
        for (int i = 0; i < path.plate.jobs.size(); i++) {
            Job job = path.plate.jobs.get(i);
            if (!allJobIds.contains(job.id)) {
                allJobIds.add(job.id);
            }
        }
        
        // Füge fehlgeschlagene Jobs hinzu
        for (int i = 0; i < path.failedJobs.size(); i++) {
            Integer failedJobId = path.failedJobs.get(i);
            if (!allJobIds.contains(failedJobId)) {
                allJobIds.add(failedJobId);
            }
        }
        
        // Sortiere Job-IDs
        for (int i = 0; i < allJobIds.size() - 1; i++) {
            for (int j = 0; j < allJobIds.size() - 1 - i; j++) {
                if (allJobIds.get(j) > allJobIds.get(j + 1)) {
                    Integer temp = allJobIds.get(j);
                    allJobIds.set(j, allJobIds.get(j + 1));
                    allJobIds.set(j + 1, temp);
                }
            }
        }
        
        // Sammle Strategie-Kürzel
        java.util.List<String> strategyCodes = new java.util.ArrayList<>();
        
        for (int i = 0; i < allJobIds.size(); i++) {
            Integer jobId = allJobIds.get(i);
            boolean found = false;
            
            // Suche nach platziertem Job
            for (int j = 0; j < path.plate.jobs.size(); j++) {
                Job job = path.plate.jobs.get(j);
                if (job.id == jobId.intValue()) {
                    if ("FullWidth".equals(job.splittingMethod)) {
                        strategyCodes.add("W");
                    } else if ("FullHeight".equals(job.splittingMethod)) {
                        strategyCodes.add("H");
                    } else {
                        strategyCodes.add("?"); // Fallback
                    }
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                // Job wurde nicht platziert
                strategyCodes.add("N"); // N für "nicht platziert"
            }
        }
        
        if (strategyCodes.size() == 0) {
            return "";
        } else {
            return String.join("->", strategyCodes);
        }
    }

}