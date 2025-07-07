package org.example;

import java.util.ArrayList;
import java.util.List;

public class MaxRectBFDynamic {
    Plate plate;
    List<FreeRectangle> freeRects;
    int placementCounter;  // Für Variable placementOrder in Job-Klasse
    List<FreeRectangle> lastAddedRects;  // Verfolgt die letzten beiden hinzugefügten Rechtecke

    public MaxRectBFDynamic(Plate plate) {
        this.plate = plate;
        freeRects = new ArrayList<>();
        freeRects.add(new FreeRectangle(0, 0, plate.width, plate.height));
        lastAddedRects = new ArrayList<>();
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

    // Jeden Job durchgehen, platzieren und visualisieren
    public boolean placeJob(Job job) {
        BestFitResult result = new BestFitResult();
        System.out.println("\n\n============== Job " + job.id + " (" + job.width + "x" + job.height + ") - FullWidth Dynamic ==============");
        // Durchsucht alle verfügbaren freien Rechtecke
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle rect = freeRects.get(i);
            System.out.println("  Prüfe FreeRect " + i + ": Startkoordinaten (x=" + rect.x + ", y=" + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
            // Originalposition testen
            testAndUpdateBestFit(job.width, job.height, rect, false, result);
            // Gedrehte Position testen
            if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
        }

        // Wenn kein passender freier Bereich gefunden wurde: Job passt nicht
        if (result.bestRect == null) {
            System.out.println("-> Kein passendes Rechteck gefunden für Job " + job.id + " mit FullWidth");
            
            // Lösche die letzten beiden hinzugefügten freien Rechtecke und erstelle neue mit FullHeight
            if (lastAddedRects.size() >= 2) {
                System.out.println("-> Lösche die letzten beiden hinzugefügten freien Rechtecke:");
                
                // Bestimme den letzten platzierten Job
                Job lastJob = plate.jobs.get(plate.jobs.size() - 1);
                
                // Entferne die beiden letzten Rechtecke
                for (int i = lastAddedRects.size() - 1; i >= 0; i--) {
                    FreeRectangle rectToRemove = lastAddedRects.get(i);
                    System.out.printf("   Entferne FreeRect: Start(%d, %d), Breite=%dmm, Höhe=%dmm%n", 
                        rectToRemove.x, rectToRemove.y, rectToRemove.width, rectToRemove.height);
                    freeRects.remove(rectToRemove);
                }
                
                // Erstelle das ursprüngliche Rechteck vom letzten Job wieder
                FreeRectangle originalRect = new FreeRectangle(lastJob.x, lastJob.y, 
                    lastJob.width + lastAddedRects.get(0).width, 
                    lastJob.height + lastAddedRects.get(1).height);
                
                // Verwende FullHeight-Splitting für den letzten Job
                System.out.println("-> Verwende FullHeight-Splitting für den letzten Job");
                splitFreeRectFullHeight(originalRect, lastJob);
                
                // Versuche Job erneut zu platzieren
                System.out.println("-> Versuche Job " + job.id + " erneut mit FullHeight-Rechtecken");
                result = new BestFitResult();
                for (int i = 0; i < freeRects.size(); i++) {
                    FreeRectangle rect = freeRects.get(i);
                    System.out.println("  Prüfe FreeRect " + i + " für FullHeight: Startkoordinaten (x=" + rect.x + ", y=" + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
                    testAndUpdateBestFit(job.width, job.height, rect, false, result);
                    if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
                }
                
                if (result.bestRect == null) {
                    System.out.println("-> Job " + job.id + " kann auch mit FullHeight nicht platziert werden.");
                    return false;
                }
                
                // Platziere Job und verwende FullHeight-Splitting
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
                job.placedOn = plate;
                job.placementOrder = placementCounter++;
                plate.jobs.add(job);

                System.out.println("-> Platziert in (" + job.x + ", " + job.y + ") auf " + plate.name + " mit FullHeight");
                splitFreeRectFullHeight(result.bestRect, job);
                
            } else {
                System.out.println("-> Nicht genügend freie Rechtecke zum Löschen vorhanden.");
                return false;
            }
            
        } else {
            // Job wurde erfolgreich mit FullWidth platziert
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
            job.placedOn = plate;
            job.placementOrder = placementCounter++;
            plate.jobs.add(job);

            System.out.println("-> Platziert in (" + job.x + ", " + job.y + ") auf " + plate.name);
            splitFreeRectFullWidth(result.bestRect, job);
        }

        return true;
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
            System.out.println("    -> Passt in " + ausrichtung + "!");
            System.out.println("       Berechnung leftoverHoriz: " + rect.width + " - " + testWidth + " = " + leftoverHoriz);
            System.out.println("       Berechnung leftoverVert: " + rect.height + " - " + testHeight + " = " + leftoverVert);
            System.out.println("       shortSideFit = " + shortSideFit + ", aktueller bestScore = " + result.bestScore);
            // Kriterium für "Best Fit": Das Rechteck, worin der Job den kleinsten Abstand entweder vertikal ODER horizontal zum nächsten freien Rechteck oder zum Rand hat.
            // Weitere Möglichkeit für "Best Fit": durchschnittlicher Abstand vertikal UND horizontal zum jeweiligen nächsten freien Rechteck oder zum Rand.
            if (shortSideFit < result.bestScore) {
                result.bestScore = shortSideFit;
                result.bestRect = rect;
                result.useRotated = rotated;
                result.bestWidth = testWidth;
                result.bestHeight = testHeight;
                System.out.println("       -> Neuer Best-Fit (" + ausrichtung + ")!");
            }
        } else {
            String ausrichtungsText;
            if (rotated) {
                ausrichtungsText = "GEDREHTER";
            } else {
                ausrichtungsText = "Original";
            }
            System.out.println("    -> Passt NICHT in " + ausrichtungsText + " Ausrichtung.");
        }
    }

    // Freie Rechtecke mit vollständiger Breitenausdehnung nach rechts erzeugen
    private void splitFreeRectFullWidth(FreeRectangle rect, Job job) {
        System.out.println("\n--- splitFreeRect aufgerufen ---");
        System.out.println("Belegtes Rechteck: Start(" + rect.x + ", " + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
        System.out.println("Jobgröße: Breite=" + job.width + "mm, Höhe=" + job.height + "mm");
        freeRects.remove(rect);
        System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");
        
        // Lösche vorherige Einträge, da wir nur die letzten zwei verfolgen wollen
        lastAddedRects.clear();
        
        // Neuer freier Bereich rechts neben dem Job
        if (job.width < rect.width) {
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, job.height);
            freeRects.add(newRectRight);
            lastAddedRects.add(newRectRight);
            System.out.println("Füge freien Bereich rechts hinzu: Start(" + newRectRight.x + ", " + newRectRight.y + "), Breite=" + newRectRight.width + "mm, Höhe=" + newRectRight.height + "mm");
        }
        // Neuer freier Bereich unterhalb des Jobs
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, rect.width, rect.height - job.height);
            freeRects.add(newRectBelow);
            lastAddedRects.add(newRectBelow);
            System.out.println("Füge freien Bereich unten hinzu: Start(" + newRectBelow.x + ", " + newRectBelow.y + "), Breite=" + newRectBelow.width + "mm, Höhe=" + newRectBelow.height + "mm");
        }
        
        System.out.println("\nAktuelle freie Rechtecke:");
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle r = freeRects.get(i);
            System.out.println("  FreeRect " + i + ": Start(" + r.x + ", " + r.y + "), Breite=" + r.width + "mm, Höhe=" + r.height + "mm");
        }
        
        // Visualisierung nach splitFreeRectFullWidth
        if (Main.DEBUG) {
            PlateVisualizer.showPlate(plate, "3", this);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Freie Rechtecke mit vollständiger Höhenausdehnung nach rechts erzeugen
    private void splitFreeRectFullHeight(FreeRectangle rect, Job job) {
        System.out.println("\n--- splitFreeRectFullHeight aufgerufen ---");
        System.out.println("Belegtes Rechteck: Start(" + rect.x + ", " + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
        System.out.println("Jobgröße: Breite=" + job.width + "mm, Höhe=" + job.height + "mm");
        freeRects.remove(rect);
        System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");
        
        // Lösche vorherige Einträge, da wir nur die letzten zwei verfolgen wollen
        lastAddedRects.clear();
        
        // Neuer freier Bereich rechts neben dem Job mit vollständiger Höhe
        if (job.width < rect.width) {
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, rect.height);  
            freeRects.add(newRectRight);
            lastAddedRects.add(newRectRight);
            System.out.println("Füge freien Bereich rechts hinzu (volle Höhe): Start(" + newRectRight.x + ", " + newRectRight.y + "), Breite=" + newRectRight.width + "mm, Höhe=" + newRectRight.height + "mm");
        }
        // Neuer freier Bereich unterhalb des Jobs
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, job.width, rect.height - job.height);
            freeRects.add(newRectBelow);
            lastAddedRects.add(newRectBelow);
            System.out.println("Füge freien Bereich unten hinzu (nur unter Job): Start(" + newRectBelow.x + ", " + newRectBelow.y + "), Breite=" + newRectBelow.width + "mm, Höhe=" + newRectBelow.height + "mm");
        }
        
        System.out.println("\nAktuelle freie Rechtecke:");
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle r = freeRects.get(i);
            System.out.println("  FreeRect " + i + ": Start(" + r.x + ", " + r.y + "), Breite=" + r.width + "mm, Höhe=" + r.height + "mm");
        }
        
        // Visualisierung nach splitFreeRectFullHeight
        if (Main.DEBUG) {
            PlateVisualizer.showPlate(plate, "3", this);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
