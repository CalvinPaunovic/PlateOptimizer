package org.example;

import java.util.ArrayList;
import java.util.List;

public class MaxRectBFMerge {
    Plate plate;
    List<FreeRectangle> freeRects = new ArrayList<>();
    int placementCounter;  // Für Variable placementOrder in Job-Klasse

    public MaxRectBFMerge(Plate plate) {
        this.plate = plate;
        freeRects = new ArrayList<>();
        freeRects.add(new FreeRectangle(0, 0, plate.width, plate.height));
    }

    static class FreeRectangle {
        int x, y, width, height;
        public FreeRectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    static class BestFitResult {
        public FreeRectangle bestRect;
        int bestScore = Integer.MAX_VALUE;
        boolean useRotated = false;
        int bestWidth = -1;
        int bestHeight = -1;
    }

    public boolean placeJob(Job job) {
        System.out.println("\n\n============== Job " + job.id + " (" + job.width + "x" + job.height + ") ==============");
        // Versuch 1: direkte Platzierung
        if (tryPlaceJob(job)) {
            return true;
        }
        // Versuch 2: Merge freie Rechtecke und versuche nochmal
        boolean merged = tryMergeFreeRectangles();
        if (merged) {
            System.out.println("-> Rechtecke wurden gemerged. Neuer Versuch zur Platzierung.");
            if (tryPlaceJob(job)) {
                return true;
            }
        }
        System.out.println("-> Job " + job.id + " passt auch nach Merge nicht.");
        return false;
    }


    private boolean tryPlaceJob(Job job) {
        BestFitResult result = new BestFitResult();
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle rect = freeRects.get(i);
            System.out.printf("  Prüfe FreeRect %d: Startkoordinaten (x=%d, y=%d), Breite=%dmm, Höhe=%dmm\n",
                    i, rect.x, rect.y, rect.width, rect.height);
            testAndUpdateBestFit(job.width, job.height, rect, false, result);
            if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
        }

        if (result.bestRect == null) {
            System.out.println("-> Kein passendes Rechteck gefunden für Job " + job.id);
            return false;
        }

        if (result.useRotated) {
            System.out.printf("-> Job wird GEDREHT platziert! (%dx%d → %dx%d)\n", job.width, job.height, result.bestWidth, result.bestHeight);
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

        System.out.printf("-> Platziert in (%d, %d) auf %s\n", job.x, job.y, plate.name);

        splitFreeRect(result.bestRect, job);

        if (Main.DEBUG) {
            PlateVisualizer.showPlate(plate, "3", this);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return true;
    }


    private void testAndUpdateBestFit(int testWidth, int testHeight, FreeRectangle rect, boolean rotated, BestFitResult result) {
        if (testWidth <= rect.width && testHeight <= rect.height) {
            String ausrichtung = rotated ? "GEDREHTE Ausrichtung" : "Originalausrichtung";
            int leftoverHoriz = rect.width - testWidth;
            int leftoverVert = rect.height - testHeight;
            int shortSideFit = Math.min(leftoverHoriz, leftoverVert);

            System.out.printf("    -> Passt in %s!\n", ausrichtung);
            System.out.printf("       Berechnung leftoverHoriz: %d - %d = %d\n", rect.width, testWidth, leftoverHoriz);
            System.out.printf("       Berechnung leftoverVert: %d - %d = %d\n", rect.height, testHeight, leftoverVert);
            System.out.printf("       shortSideFit = %d, aktueller bestScore = %d\n", shortSideFit, result.bestScore);

            if (shortSideFit < result.bestScore) {
                result.bestScore = shortSideFit;
                result.bestRect = rect;
                result.useRotated = rotated;
                result.bestWidth = testWidth;
                result.bestHeight = testHeight;
                System.out.printf("       -> Neuer Best-Fit (%s)!\n", ausrichtung);
            }
        } else {
            System.out.printf("    -> Passt NICHT in %s Ausrichtung.\n", rotated ? "GEDREHTER" : "Original");
        }
    }


    private void splitFreeRect(FreeRectangle rect, Job job) {
        System.out.println("\n--- splitFreeRect aufgerufen ---");
        System.out.printf("Belegtes Rechteck: Start(%d, %d), Breite=%dmm, Höhe=%dmm\n", rect.x, rect.y, rect.width, rect.height);
        System.out.printf("Jobgröße: Breite=%dmm, Höhe=%dmm\n", job.width, job.height);
        freeRects.remove(rect);
        System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");
        if (job.width < rect.width) {
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, job.height);
            freeRects.add(newRectRight);
            System.out.printf("Füge freien Bereich rechts hinzu: Start(%d, %d), Breite=%dmm, Höhe=%dmm\n",
                    newRectRight.x, newRectRight.y, newRectRight.width, newRectRight.height);
        }
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, rect.width, rect.height - job.height);
            freeRects.add(newRectBelow);
            System.out.printf("Füge freien Bereich unten hinzu: Start(%d, %d), Breite=%dmm, Höhe=%dmm\n",
                    newRectBelow.x, newRectBelow.y, newRectBelow.width, newRectBelow.height);
        }
        System.out.println("\nAktuelle freie Rechtecke:");
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle r = freeRects.get(i);
            System.out.printf("  FreeRect %d: Start(%d, %d), Breite=%dmm, Höhe=%dmm\n", i, r.x, r.y, r.width, r.height);
        }
    }


    // Versucht freie Rechtecke zu mergen, wenn sie nebeneinander liegen.
    // Aktuell nur Debug-Ausgabe: prüft, welche Rechtecke sich angrenzen und wo.
    private boolean tryMergeFreeRectangles() {
        System.out.println("\n--- Versuche Merge ---");
        boolean mergedAny = false;
        // Gehe alle freien Rechtecke durch (r1)
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle r1 = freeRects.get(i);
            System.out.printf("Prüfe Rechteck %d: Start(%d,%d), Größe %dx%d\n", i, r1.x, r1.y, r1.width, r1.height);
            // Vergleiche r1 mit allen nachfolgenden Rechtecken (r2)
            for (int j = i + 1; j < freeRects.size(); j++) {
                FreeRectangle r2 = freeRects.get(j);
                System.out.printf("  Vergleiche mit Rechteck %d: Start(%d,%d), Größe %dx%d\n", j, r2.x, r2.y, r2.width, r2.height);
                // === Horizontale Berührung prüfen ===
                if (r1.y == r2.y && r1.height == r2.height) {
                    // Prüfe, ob r1 rechts direkt an r2 angrenzt (r1 links von r2)
                    if (r1.x + r1.width == r2.x) {
                        int sharedEdgeX = r1.x + r1.width;
                        int sharedEdgeYStart = r1.y;
                        int sharedEdgeYEnd = r1.y + r1.height;
                        System.out.printf("  [Horizontale Berührung] Rechteck %d liegt LINKS neben Rechteck %d%n", i, j);
                        System.out.printf("    Gemeinsame Kante von (%d,%d) bis (%d,%d)%n",
                                sharedEdgeX, sharedEdgeYStart, sharedEdgeX, sharedEdgeYEnd);
                    }
                    // Prüfe, ob r2 rechts direkt an r1 angrenzt (r1 rechts von r2)
                    else if (r2.x + r2.width == r1.x) {
                        int sharedEdgeX = r2.x + r2.width;
                        int sharedEdgeYStart = r2.y;
                        int sharedEdgeYEnd = r2.y + r2.height;
                        System.out.printf("  [Horizontale Berührung] Rechteck %d liegt RECHTS neben Rechteck %d%n", i, j);
                        System.out.printf("    Gemeinsame Kante von (%d,%d) bis (%d,%d)%n",
                                sharedEdgeX, sharedEdgeYStart, sharedEdgeX, sharedEdgeYEnd);
                    }
                }
                // === Vertikale Berührung prüfen ===
                if (r1.x == r2.x && r1.width == r2.width) {
                    // Prüfe, ob r1 oben direkt an r2 angrenzt (r1 oberhalb von r2)
                    if (r1.y + r1.height == r2.y) {
                        int sharedEdgeY = r1.y + r1.height;
                        int sharedEdgeXStart = r1.x;
                        int sharedEdgeXEnd = r1.x + r1.width;
                        System.out.printf("  [Vertikale Berührung] Rechteck %d liegt OBERHALB von Rechteck %d%n", i, j);
                        System.out.printf("    Gemeinsame Kante von (%d,%d) bis (%d,%d)%n",
                                sharedEdgeXStart, sharedEdgeY, sharedEdgeXEnd, sharedEdgeY);
                    }
                    // Prüfe, ob r2 oben direkt an r1 angrenzt (r1 unterhalb von r2)
                    else if (r2.y + r2.height == r1.y) {
                        int sharedEdgeY = r2.y + r2.height;
                        int sharedEdgeXStart = r2.x;
                        int sharedEdgeXEnd = r2.x + r2.width;
                        System.out.printf("  [Vertikale Berührung] Rechteck %d liegt UNTERHALB von Rechteck %d%n", i, j);
                        System.out.printf("    Gemeinsame Kante von (%d,%d) bis (%d,%d)%n",
                                sharedEdgeXStart, sharedEdgeY, sharedEdgeXEnd, sharedEdgeY);
                    }
                }
            }
        }
        System.out.println("Kein Merge ausgeführt (nur Analyse).");
        return mergedAny;  // Noch kein Merge, nur Analyse
    }

}
