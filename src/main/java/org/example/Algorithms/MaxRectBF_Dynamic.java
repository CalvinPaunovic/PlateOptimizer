package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.Visualizer.PlateVisualizer;

public class MaxRectBF_Dynamic {
    Plate plate;
    public List<FreeRectangle> freeRects;
    int placementCounter;  // Für Variable placementOrder in Job-Klasse
    List<FreeRectangle> lastAddedRects;  // Verfolgt die letzten beiden hinzugefügten Rechtecke

    public MaxRectBF_Dynamic(Plate plate) {
        this.plate = plate;
        freeRects = new ArrayList<>();
        freeRects.add(new FreeRectangle(0, 0, plate.width, plate.height));
        lastAddedRects = new ArrayList<>();
    }

    // Parameter für freie Rechtecke
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

    // Parameter für eine neue Jobplatzierung
    static class BestFitResult {
        public FreeRectangle bestRect;
        double bestScore = Double.MAX_VALUE;
        boolean useRotated = false;
        double bestWidth = -1;
        double bestHeight = -1;
    }

    // Jeden Job durchgehen, platzieren und visualisieren
    public boolean placeJob(Job job) {
        BestFitResult result = new BestFitResult();
        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("\n\n============== Job " + job.id + " (" + job.width + "x" + job.height + ") - FullWidth Dynamic ==============");
        // Durchsucht alle verfügbaren freien Rechtecke
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle rect = freeRects.get(i);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("  Prüfe FreeRect " + i + ": Startkoordinaten (x=" + rect.x + ", y=" + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
            // Originalposition testen
            testAndUpdateBestFit(job.width, job.height, rect, false, result);
            // Gedrehte Position testen
            if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
        }

        // Wenn kein passender freier Bereich gefunden wurde: Job passt nicht
        if (result.bestRect == null) {
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Kein passendes Rechteck gefunden für Job " + job.id + " mit FullWidth");

            // Lösche die letzten beiden hinzugefügten freien Rechtecke und erstelle neue mit FullHeight
            if (lastAddedRects.size() >= 2) {
                if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Lösche die letzten beiden hinzugefügten freien Rechtecke:");

                // Bestimme den letzten platzierten Job
                Job lastJob = plate.jobs.get(plate.jobs.size() - 1);

                // Entferne die beiden letzten Rechtecke
                for (int i = lastAddedRects.size() - 1; i >= 0; i--) {
                    FreeRectangle rectToRemove = lastAddedRects.get(i);
                    if (Main.DEBUG_MaxRectBF_Dynamic) System.out.printf("   Entferne FreeRect: Start(%.2f, %.2f), Breite=%.2fmm, Höhe=%.2fmm%n",
                        rectToRemove.x, rectToRemove.y, rectToRemove.width, rectToRemove.height);
                    freeRects.remove(rectToRemove);
                }

                // Erstelle das ursprüngliche Rechteck vom letzten Job wieder
                FreeRectangle originalRect = new FreeRectangle(
                    lastJob.x,
                    lastJob.y,
                    lastJob.width + lastAddedRects.get(0).width,
                    lastJob.height + lastAddedRects.get(1).height
                );
                if (Main.DEBUG_MaxRectBF_Dynamic) System.out.printf("   Rekonstruiertes Originalrechteck: Start(%.2f, %.2f), Breite=%.2fmm, Höhe=%.2fmm%n",
                    originalRect.x, originalRect.y, originalRect.width, originalRect.height);

                // Verwende FullHeight-Splitting für den letzten Job
                if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Verwende FullHeight-Splitting für den letzten Job");
                splitFreeRectFullHeight(originalRect, lastJob);

                // Versuche Job erneut zu platzieren
                if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Versuche Job " + job.id + " erneut mit FullHeight-Rechtecken");
                result = new BestFitResult();
                for (int i = 0; i < freeRects.size(); i++) {
                    FreeRectangle rect = freeRects.get(i);
                    if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("  Prüfe FreeRect " + i + " für FullHeight: Startkoordinaten (x=" + rect.x + ", y=" + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
                    testAndUpdateBestFit(job.width, job.height, rect, false, result);
                    if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);
                }

                if (result.bestRect == null) {
                    if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Job " + job.id + " kann auch mit FullHeight nicht platziert werden.");
                    return false;
                }

                // Platziere Job und verwende FullHeight-Splitting
                if (result.useRotated) {
                    if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Job wird GEDREHT platziert! (" + job.width + "x" + job.height + " → " + result.bestWidth + "x" + result.bestHeight + ")");
                    job.rotated = true;
                } else {
                    if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Job wird in Originalausrichtung platziert.");
                }
                job.width = result.bestWidth;
                job.height = result.bestHeight;
                job.x = result.bestRect.x;
                job.y = result.bestRect.y;
                job.placedOn = plate;
                job.placementOrder = placementCounter++;
                job.splittingMethod = "FullHeight";
                plate.jobs.add(job);

                if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Platziert in (" + job.x + ", " + job.y + ") auf " + plate.name + " mit FullHeight");
                splitFreeRectFullHeight(result.bestRect, job);

            } else {
                if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Nicht genügend freie Rechtecke zum Löschen vorhanden.");
                return false;
            }

        } else {
            // Job wurde erfolgreich mit FullWidth platziert
            if (result.useRotated) {
                if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Job wird GEDREHT platziert! (" + job.width + "x" + job.height + " → " + result.bestWidth + "x" + result.bestHeight + ")");
                job.rotated = true;
            } else {
                if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Job wird in Originalausrichtung platziert.");
            }
            job.width = result.bestWidth;
            job.height = result.bestHeight;
            job.x = result.bestRect.x;
            job.y = result.bestRect.y;
            job.placedOn = plate;
            job.placementOrder = placementCounter++;
            job.splittingMethod = "FullWidth";
            plate.jobs.add(job);

            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("-> Platziert in (" + job.x + ", " + job.y + ") auf " + plate.name);
            splitFreeRectFullWidth(result.bestRect, job);
        }

        return true;
    }

    // Prüfen, ob Job in originaler oder gedrehter Position einen jeweils kürzeren Abstand vertikal oder horizontal hat
    private void testAndUpdateBestFit(double testWidth, double testHeight, FreeRectangle rect, boolean rotated, BestFitResult result) {
        if (testWidth <= rect.width && testHeight <= rect.height) {
            String ausrichtung;
            if (rotated) {
                ausrichtung = "GEDREHTE Ausrichtung";
            } else {
                ausrichtung = "Originalausrichtung";
            }
            double leftoverHoriz = rect.width - testWidth;
            double leftoverVert = rect.height - testHeight;
            double shortSideFit = Math.min(leftoverHoriz, leftoverVert);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("    -> Passt in " + ausrichtung + "!");
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("       Berechnung leftoverHoriz: " + rect.width + " - " + testWidth + " = " + leftoverHoriz);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("       Berechnung leftoverVert: " + rect.height + " - " + testHeight + " = " + leftoverVert);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("       shortSideFit = " + shortSideFit + ", aktueller bestScore = " + result.bestScore);
            if (shortSideFit < result.bestScore) {
                result.bestScore = shortSideFit;
                result.bestRect = rect;
                result.useRotated = rotated;
                result.bestWidth = testWidth;
                result.bestHeight = testHeight;
                if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("       -> Neuer Best-Fit (" + ausrichtung + ")!");
            }
        } else {
            String ausrichtungsText;
            if (rotated) {
                ausrichtungsText = "GEDREHTER";
            } else {
                ausrichtungsText = "Original";
            }
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("    -> Passt NICHT in " + ausrichtungsText + " Ausrichtung.");
        }
    }

    // Freie Rechtecke mit vollständiger Breitenausdehnung nach rechts erzeugen
    private void splitFreeRectFullWidth(FreeRectangle rect, Job job) {
        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("\n--- splitFreeRect aufgerufen ---");
        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Belegtes Rechteck: Start(" + rect.x + ", " + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Jobgröße: Breite=" + job.width + "mm, Höhe=" + job.height + "mm");
        freeRects.remove(rect);
        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");

        lastAddedRects.clear();

        if (job.width < rect.width) {
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, job.height);
            freeRects.add(newRectRight);
            lastAddedRects.add(newRectRight);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Füge freien Bereich rechts hinzu: Start(" + newRectRight.x + ", " + newRectRight.y + "), Breite=" + newRectRight.width + "mm, Höhe=" + newRectRight.height + "mm");
        }
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, rect.width, rect.height - job.height);
            freeRects.add(newRectBelow);
            lastAddedRects.add(newRectBelow);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Füge freien Bereich unten hinzu: Start(" + newRectBelow.x + ", " + newRectBelow.y + "), Breite=" + newRectBelow.width + "mm, Höhe=" + newRectBelow.height + "mm");
        }

        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("\nAktuelle freie Rechtecke:");
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle r = freeRects.get(i);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("  FreeRect " + i + ": Start(" + r.x + ", " + r.y + "), Breite=" + r.width + "mm, Höhe=" + r.height + "mm");
        }

        if (Main.DEBUG_MaxRectBF_Dynamic) {
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
        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("\n--- splitFreeRectFullHeight aufgerufen ---");
        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Belegtes Rechteck: Start(" + rect.x + ", " + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Jobgröße: Breite=" + job.width + "mm, Höhe=" + job.height + "mm");
        freeRects.remove(rect);
        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");

        lastAddedRects.clear();

        if (job.width < rect.width) {
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, rect.height);
            freeRects.add(newRectRight);
            lastAddedRects.add(newRectRight);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Füge freien Bereich rechts hinzu (volle Höhe): Start(" + newRectRight.x + ", " + newRectRight.y + "), Breite=" + newRectRight.width + "mm, Höhe=" + newRectRight.height + "mm");
        }
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, job.width, rect.height - job.height);
            freeRects.add(newRectBelow);
            lastAddedRects.add(newRectBelow);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("Füge freien Bereich unten hinzu (nur unter Job): Start(" + newRectBelow.x + ", " + newRectBelow.y + "), Breite=" + newRectBelow.width + "mm, Höhe=" + newRectBelow.height + "mm");
        }

        if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("\nAktuelle freie Rechtecke:");
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle r = freeRects.get(i);
            if (Main.DEBUG_MaxRectBF_Dynamic) System.out.println("  FreeRect " + i + ": Start(" + r.x + ", " + r.y + "), Breite=" + r.width + "mm, Höhe=" + r.height + "mm");
        }

        if (Main.DEBUG_MaxRectBF_Dynamic) {
            PlateVisualizer.showPlate(plate, "3", this);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}