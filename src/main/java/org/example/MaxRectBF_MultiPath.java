package org.example;

import java.util.ArrayList;
import java.util.List;

public class MaxRectBF_MultiPath {
    
    // Pfad-Klasse für die verschiedenen Ausführungswege
    static class AlgorithmPath {
        Plate plate;
        List<FreeRectangle> freeRects;
        int placementCounter;
        List<FreeRectangle> lastAddedRects;
        String pathDescription;
        boolean isActive;
        boolean useFullHeight; // Flag für die Splitting-Methode
        
        public AlgorithmPath(Plate originalPlate, String description) {
            this.plate = new Plate(originalPlate.name + " - " + description, originalPlate.width, originalPlate.height);
            this.freeRects = new ArrayList<>();
            this.freeRects.add(new FreeRectangle(0, 0, plate.width, plate.height));
            this.lastAddedRects = new ArrayList<>();
            this.pathDescription = description;
            this.placementCounter = 0;
            this.isActive = true;
            this.useFullHeight = false; // Startet mit FullWidth
        }
        
        public AlgorithmPath(AlgorithmPath original, String newDescription) {
            this.plate = new Plate(original.plate.name.split(" - ")[0] + " - " + newDescription, original.plate.width, original.plate.height);
            
            // Kopiere alle Jobs
            for (Job originalJob : original.plate.jobs) {
                Job copiedJob = new Job(originalJob.id, originalJob.width, originalJob.height);
                copiedJob.x = originalJob.x;
                copiedJob.y = originalJob.y;
                copiedJob.rotated = originalJob.rotated;
                copiedJob.placedOn = this.plate;
                copiedJob.placementOrder = originalJob.placementOrder;
                copiedJob.splittingMethod = originalJob.splittingMethod;
                this.plate.jobs.add(copiedJob);
            }
            
            // Kopiere freie Rechtecke
            this.freeRects = new ArrayList<>();
            for (FreeRectangle rect : original.freeRects) {
                this.freeRects.add(new FreeRectangle(rect.x, rect.y, rect.width, rect.height));
            }
            
            // Kopiere lastAddedRects
            this.lastAddedRects = new ArrayList<>();
            for (FreeRectangle rect : original.lastAddedRects) {
                this.lastAddedRects.add(new FreeRectangle(rect.x, rect.y, rect.width, rect.height));
            }
            
            this.pathDescription = newDescription;
            this.placementCounter = original.placementCounter;
            this.isActive = true;
            this.useFullHeight = original.useFullHeight; // Übernimmt die Splitting-Methode
        }
    }
    
    // Parameter für freie Rechtecke
    static class FreeRectangle {
        int x, y, width, height;
        public FreeRectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    // Parameter für eine neue Jobplatzierung
    static class BestFitResult {
        public FreeRectangle bestRect;
        int bestScore = Integer.MAX_VALUE;
        boolean useRotated = false;
        int bestWidth = -1;
        int bestHeight = -1;
    }
    
    List<AlgorithmPath> paths;
    Plate originalPlate;

    public MaxRectBF_MultiPath(Plate plate) {
        this.originalPlate = plate;
        this.paths = new ArrayList<>();
        
        // Startet mit einem einzigen Pfad
        AlgorithmPath initialPath = new AlgorithmPath(plate, "Pfad 1 (FullWidth)");
        paths.add(initialPath);
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
            
            System.out.println("\n\n============== " + currentPath.pathDescription + " - Job " + job.id + " (" + job.width + "x" + job.height + ") ==============");
            
            BestFitResult result = new BestFitResult();
            
            // Durchsucht alle verfügbaren freien Rechtecke für diesen Pfad
            for (int i = 0; i < currentPath.freeRects.size(); i++) {
                FreeRectangle rect = currentPath.freeRects.get(i);
                if (Main.DEBUG_MultiPath) System.out.println("  Prüfe FreeRect " + i + ": Startkoordinaten (x=" + rect.x + ", y=" + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
                // Originalposition testen
                testAndUpdateBestFit(job.width, job.height, rect, false, result);
                // Gedrehte Position testen
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
            }

            // Wenn kein passender freier Bereich gefunden wurde: Job passt nicht in FullWidth
            if (result.bestRect == null) {
                System.out.println("****************************************** ALGORITHMUS WECHSELT VON FULLWIDTH ZU FULLHEIGHT ******************************************");
                System.out.println("*** JOB " + job.id + " TRIGGERT DEN WECHSEL ***");
                System.out.println("*** AKTUELLER PFAD: " + currentPath.pathDescription + " (verwendet " + (currentPath.useFullHeight ? "FULLHEIGHT" : "FULLWIDTH") + " Splitting) ***");
                
                // Prüfe ob ein Wechsel der Splitting-Methode möglich ist
                if (currentPath.lastAddedRects.size() >= 2) {
                    String newMethod = currentPath.useFullHeight ? "FullWidth" : "FullHeight";
                    int newPathId = paths.size() + newPaths.size() + 1;
                    
                    System.out.println("-> Erstelle Pfad-Aufteilung:");
                    System.out.println("   AKTUELLER PFAD: " + currentPath.pathDescription + " verwendet " + (currentPath.useFullHeight ? "FULLHEIGHT" : "FULLWIDTH") + " Splitting");
                    System.out.println("   NEUER PFAD: Pfad " + newPathId + " (" + newMethod + " ab Job " + job.id + ") wird erstellt mit " + newMethod.toUpperCase() + " Splitting");
                    
                    // AKTUELLER PFAD: Läuft weiter (Job passt nicht, aber Pfad bleibt aktiv)
                    System.out.println("   " + currentPath.pathDescription + " - Job " + job.id + " passt nicht, Pfad läuft aber weiter");
                    
                    // NEUER PFAD: Mit entgegengesetzter Splitting-Methode
                    AlgorithmPath newMethodPath = new AlgorithmPath(currentPath, "Pfad " + newPathId + " (" + newMethod + " ab Job " + job.id + ")");
                    newMethodPath.useFullHeight = !currentPath.useFullHeight; // Setze entgegengesetzte Splitting-Methode
                    
                    // Bestimme den letzten platzierten Job
                    Job lastJob = newMethodPath.plate.jobs.get(newMethodPath.plate.jobs.size() - 1);
                    
                    // Speichere die beiden letzten Rechtecke bevor sie gelöscht werden
                    List<FreeRectangle> rectsToRemove = new ArrayList<>(newMethodPath.lastAddedRects);
                    
                    // Entferne die beiden letzten Rechtecke aus Pfad 2 - KOMPAKTE AUSGABE
                    System.out.printf("   Zu entfernende FreeRects aus lastAddedRects: %d Stück%n", rectsToRemove.size());
                    
                    // Zeige vor dem Entfernen alle vorhandenen FreeRects
                    System.out.println("   Vorhandene FreeRects vor Entfernung:");
                    for (int i = 0; i < newMethodPath.freeRects.size(); i++) {
                        FreeRectangle r = newMethodPath.freeRects.get(i);
                        System.out.printf("     FreeRect %d: Start(%d, %d), Breite=%dmm, Höhe=%dmm%n", 
                            i, r.x, r.y, r.width, r.height);
                    }
                    
                    // Zeige alle zu entfernenden FreeRects in einer Liste
                    System.out.println("   Entferne folgende FreeRects:");
                    for (int rectIndex = 0; rectIndex < rectsToRemove.size(); rectIndex++) {
                        FreeRectangle rectToRemove = rectsToRemove.get(rectIndex);
                        
                        // Finde die AKTUELLE ID (Index) des zu entfernenden FreeRects
                        int rectId = -1;
                        for (int i = 0; i < newMethodPath.freeRects.size(); i++) {
                            FreeRectangle r = newMethodPath.freeRects.get(i);
                            if (r.x == rectToRemove.x && r.y == rectToRemove.y && 
                                r.width == rectToRemove.width && r.height == rectToRemove.height) {
                                rectId = i;
                                break;
                            }
                        }
                        
                        System.out.printf("     - FreeRect %d/%d (ID %d): Start(%d, %d), Breite=%dmm, Höhe=%dmm%n", 
                            rectIndex + 1, rectsToRemove.size(), rectId, rectToRemove.x, rectToRemove.y, rectToRemove.width, rectToRemove.height);
                    }
                    
                    // Entferne alle FreeRects auf einmal
                    for (FreeRectangle rectToRemove : rectsToRemove) {
                        newMethodPath.freeRects.removeIf(rect -> 
                            rect.x == rectToRemove.x && rect.y == rectToRemove.y && 
                            rect.width == rectToRemove.width && rect.height == rectToRemove.height);
                    }
                    
                    // Zeige nach dem Entfernen alle verbleibenden FreeRects
                    System.out.println("   Verbleibende FreeRects nach Entfernung:");
                    if (newMethodPath.freeRects.isEmpty()) {
                        System.out.println("     (keine FreeRects verbleibend)");
                    } else {
                        for (int i = 0; i < newMethodPath.freeRects.size(); i++) {
                            FreeRectangle r = newMethodPath.freeRects.get(i);
                            System.out.printf("     FreeRect %d: Start(%d, %d), Breite=%dmm, Höhe=%dmm%n", 
                                i, r.x, r.y, r.width, r.height);
                        }
                    }
                    System.out.println();
                    
                    // Berechne das ursprüngliche Rechteck korrekt
                    // Das ursprüngliche Rechteck war das, was durch FullWidth-Splitting aufgeteilt wurde
                    int originalWidth = lastJob.width;
                    int originalHeight = lastJob.height;
                    
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
                    System.out.printf("   Rekonstruiertes Originalrechteck: Start(%d, %d), Breite=%dmm, Höhe=%dmm%n",
                        originalRect.x, originalRect.y, originalRect.width, originalRect.height);
                    
                    // Verwende neue Splitting-Methode für den letzten Job im neuen Pfad
                    System.out.println("   " + newMethodPath.pathDescription + " - Verwende " + newMethod + "-Splitting für den letzten Job");
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
                        // Speichere den Pfad für später
                        newPaths.add(newMethodPath);
                        System.out.println("   " + newMethodPath.pathDescription + " - Job " + job.id + " wird später platziert");
                        anySuccess = true;
                    } else {
                        System.out.println("   " + newMethodPath.pathDescription + " - Job " + job.id + " kann auch mit " + newMethod + " nicht platziert werden");
                        // Füge den Pfad trotzdem hinzu, aber markiere ihn als fehlgeschlagen
                        newMethodPath.isActive = true; // Lasse ihn aktiv für Visualisierung
                        newPaths.add(newMethodPath);
                    }
                    
                } else {
                    System.out.println("-> Nicht genügend freie Rechtecke zum Löschen vorhanden in " + currentPath.pathDescription);
                    System.out.println("   " + currentPath.pathDescription + " - Job " + job.id + " passt nicht, aber Pfad läuft weiter");
                }
                
            } else {
                // Job wurde erfolgreich im aktuellen Pfad platziert
                String splittingMethod = currentPath.useFullHeight ? "FullHeight" : "FullWidth";
                placeJobInPath(job, result, currentPath, splittingMethod);
                anySuccess = true;
                System.out.println("   " + currentPath.pathDescription + " - Job " + job.id + " erfolgreich mit " + splittingMethod + " platziert");
            }
        }
        
        // DANN: Neue Pfade hinzufügen und deren Jobs platzieren
        for (AlgorithmPath newPath : newPaths) {
            // Job für den neuen Pfad kopieren
            Job job = new Job(originalJob.id, originalJob.width, originalJob.height);
            
            System.out.println("\n\n============== " + newPath.pathDescription + " - Job " + job.id + " (" + job.width + "x" + job.height + ") ==============");
            
            // Job im FullHeight-Pfad platzieren
            BestFitResult fullHeightResult = new BestFitResult();
            for (int i = 0; i < newPath.freeRects.size(); i++) {
                FreeRectangle rect = newPath.freeRects.get(i);
                if (Main.DEBUG_MultiPath) System.out.println("  Prüfe FreeRect " + i + ": Startkoordinaten (x=" + rect.x + ", y=" + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
                testAndUpdateBestFit(job.width, job.height, rect, false, fullHeightResult);
                if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, fullHeightResult);
            }
            
            if (fullHeightResult.bestRect != null) {
                placeJobInPath(job, fullHeightResult, newPath, "FullHeight");
                System.out.println("   " + newPath.pathDescription + " - Job " + job.id + " erfolgreich platziert");
            }
        }
        
        paths.addAll(newPaths);
        
        // Zeige alle aktiven Pfade
        System.out.println("\n=== AKTIVE PFADE ===");
        for (AlgorithmPath path : paths) {
            if (path.isActive) {
                double coverage = PlateVisualizer.calculateCoverageRate(path.plate);
                System.out.printf("%s - %d Jobs platziert, %.2f%% Deckungsrate%n", 
                    path.pathDescription, path.plate.jobs.size(), coverage);
            }
        }
        
        return anySuccess;
    }
    
    // Hilfsmethode zum Platzieren eines Jobs in einem spezifischen Pfad
    private void placeJobInPath(Job job, BestFitResult result, AlgorithmPath path, String splittingMethod) {
        if (result.useRotated) {
            System.out.println("-> Job wird GEDREHT platziert! (" + job.width + "x" + job.height + " → " + result.bestWidth + "x" + result.bestHeight + ")");
            job.rotated = true;
        } else {
            System.out.println("-> Job wird in Originalausrichtung platziert.");
        }
        job.width = result.bestWidth;
        job.height = result.bestHeight;
        job.x = result.bestRect.x;
        job.y = result.bestRect.y;
        job.placedOn = path.plate;
        job.placementOrder = path.placementCounter++;
        job.splittingMethod = splittingMethod;
        path.plate.jobs.add(job);

        System.out.println("-> Platziert in (" + job.x + ", " + job.y + ") auf " + path.plate.name);
        
        if ("FullWidth".equals(splittingMethod)) {
            splitFreeRectFullWidth(result.bestRect, job, path);
        } else {
            splitFreeRectFullHeight(result.bestRect, job, path);
        }
        
        // KEINE Visualisierung hier - wird in Main am Ende gemacht
    }

    // Prüfen, ob Job in originaler oder gedrehter Position einen jeweils kürzeren Abstand vertikal oder horizontal hat
    private void testAndUpdateBestFit(int testWidth, int testHeight, FreeRectangle rect, boolean rotated, BestFitResult result) {
        if (testWidth <= rect.width && testHeight <= rect.height) {
            String ausrichtung;
            if (rotated) {
                ausrichtung = "GEDREHTE Ausrichtung";
            } else {
                ausrichtung = "Originalausrichtung";
            }
            int leftoverHoriz = rect.width - testWidth;
            int leftoverVert = rect.height - testHeight;
            int shortSideFit = Math.min(leftoverHoriz, leftoverVert);
            if (Main.DEBUG_MultiPath) System.out.println("    -> Passt in " + ausrichtung + "!");
            if (Main.DEBUG_MultiPath) System.out.println("       Berechnung leftoverHoriz: " + rect.width + " - " + testWidth + " = " + leftoverHoriz);
            if (Main.DEBUG_MultiPath) System.out.println("       Berechnung leftoverVert: " + rect.height + " - " + testHeight + " = " + leftoverVert);
            if (Main.DEBUG_MultiPath) System.out.println("       shortSideFit = " + shortSideFit + ", aktueller bestScore = " + result.bestScore);
            // Kriterium für "Best Fit": Das Rechteck, worin der Job den kleinsten Abstand entweder vertikal ODER horizontal zum nächsten freien Rechteck oder zum Rand hat.
            // Weitere Möglichkeit für "Best Fit": durchschnittlicher Abstand vertikal UND horizontal zum jeweiligen nächsten freien Rechteck oder zum Rand.
            if (shortSideFit < result.bestScore) {
                result.bestScore = shortSideFit;
                result.bestRect = rect;
                result.useRotated = rotated;
                result.bestWidth = testWidth;
                result.bestHeight = testHeight;
                if (Main.DEBUG_MultiPath) System.out.println("       -> Neuer Best-Fit (" + ausrichtung + ")!");
            }
        } else {
            String ausrichtungsText;
            if (rotated) {
                ausrichtungsText = "GEDREHTER";
            } else {
                ausrichtungsText = "Original";
            }
            if (Main.DEBUG_MultiPath) System.out.println("    -> Passt NICHT in " + ausrichtungsText + " Ausrichtung.");
        }
    }

    // Freie Rechtecke mit vollständiger Breitenausdehnung nach rechts erzeugen
    private void splitFreeRectFullWidth(FreeRectangle rect, Job job, AlgorithmPath path) {
        if (Main.DEBUG_MultiPath) System.out.println("\n--- splitFreeRectFullWidth aufgerufen für " + path.pathDescription + " ---");
        if (Main.DEBUG_MultiPath) System.out.println("Belegtes Rechteck: Start(" + rect.x + ", " + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
        if (Main.DEBUG_MultiPath) System.out.println("Jobgröße: Breite=" + job.width + "mm, Höhe=" + job.height + "mm");
        path.freeRects.remove(rect);
        if (Main.DEBUG_MultiPath) System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");
        
        // Lösche vorherige Einträge, da wir nur die letzten zwei verfolgen wollen
        path.lastAddedRects.clear();
        
        // Neuer freier Bereich rechts neben dem Job
        if (job.width < rect.width) {
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, job.height);
            path.freeRects.add(newRectRight);
            path.lastAddedRects.add(newRectRight);
            if (Main.DEBUG_MultiPath) System.out.println("Füge freien Bereich rechts hinzu: Start(" + newRectRight.x + ", " + newRectRight.y + "), Breite=" + newRectRight.width + "mm, Höhe=" + newRectRight.height + "mm");
        }
        // Neuer freier Bereich unterhalb des Jobs
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, rect.width, rect.height - job.height);
            path.freeRects.add(newRectBelow);
            path.lastAddedRects.add(newRectBelow);
            if (Main.DEBUG_MultiPath)            if (Main.DEBUG_MultiPath) System.out.println("Füge freien Bereich unten hinzu: Start(" + newRectBelow.x + ", " + newRectBelow.y + "), Breite=" + newRectBelow.width + "mm, Höhe=" + newRectBelow.height + "mm");
        }
        
        if (Main.DEBUG_MultiPath) {
            System.out.println("\nAktuelle freie Rechtecke für " + path.pathDescription + ":");
            for (int i = 0; i < path.freeRects.size(); i++) {
                FreeRectangle r = path.freeRects.get(i);
                System.out.println("  FreeRect " + i + ": Start(" + r.x + ", " + r.y + "), Breite=" + r.width + "mm, Höhe=" + r.height + "mm");
            }
        }
    }

    // Freie Rechtecke mit vollständiger Höhenausdehnung nach rechts erzeugen
    private void splitFreeRectFullHeight(FreeRectangle rect, Job job, AlgorithmPath path) {
        if (Main.DEBUG_MultiPath) System.out.println("\n--- splitFreeRectFullHeight aufgerufen für " + path.pathDescription + " ---");
        if (Main.DEBUG_MultiPath) System.out.println("Belegtes Rechteck: Start(" + rect.x + ", " + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
        if (Main.DEBUG_MultiPath) System.out.println("Jobgröße: Breite=" + job.width + "mm, Höhe=" + job.height + "mm");
        path.freeRects.remove(rect);
        if (Main.DEBUG_MultiPath) System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");
        
        // Lösche vorherige Einträge, da wir nur die letzten zwei verfolgen wollen
        path.lastAddedRects.clear();
        
        // Neuer freier Bereich rechts neben dem Job mit vollständiger Höhe
        if (job.width < rect.width) {
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, rect.height);  
            path.freeRects.add(newRectRight);
            path.lastAddedRects.add(newRectRight);
            if (Main.DEBUG_MultiPath) System.out.println("Füge freien Bereich rechts hinzu (volle Höhe): Start(" + newRectRight.x + ", " + newRectRight.y + "), Breite=" + newRectRight.width + "mm, Höhe=" + newRectRight.height + "mm");
        }
        // Neuer freier Bereich unterhalb des Jobs
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, job.width, rect.height - job.height);
            path.freeRects.add(newRectBelow);
            path.lastAddedRects.add(newRectBelow);
            if (Main.DEBUG_MultiPath) System.out.println("Füge freien Bereich unten hinzu (nur unter Job): Start(" + newRectBelow.x + ", " + newRectBelow.y + "), Breite=" + newRectBelow.width + "mm, Höhe=" + newRectBelow.height + "mm");
        }
        
        if (Main.DEBUG_MultiPath) {
            System.out.println("\nAktuelle freie Rechtecke für " + path.pathDescription + ":");
            for (int i = 0; i < path.freeRects.size(); i++) {
                FreeRectangle r = path.freeRects.get(i);
                System.out.println("  FreeRect " + i + ": Start(" + r.x + ", " + r.y + "), Breite=" + r.width + "mm, Höhe=" + r.height + "mm");
            }
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
        
        return bestPath != null ? bestPath.plate : originalPlate;
    }
    
    // Getter für alle Pfade (für finale Ausgabe)
    public List<AlgorithmPath> getAllPaths() {
        return paths;
    }
}