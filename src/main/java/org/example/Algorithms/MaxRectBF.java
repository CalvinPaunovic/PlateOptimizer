package org.example.Algorithms;

import java.util.ArrayList;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.Visualizer.PlateVisualizer;

public class MaxRectBF {
    Plate plate;
    public List<FreeRectangle> freeRects;
    int placementCounter;  // Für Variable placementOrder in Job-Klasse
    private boolean useFullHeight;

    public MaxRectBF(Plate plate) {
        this(plate, false); // Default: fullWidth
    }

    public MaxRectBF(Plate plate, boolean useFullHeight) {
        this.plate = plate;
        this.useFullHeight = useFullHeight;
        freeRects = new ArrayList<>();
        freeRects.add(new FreeRectangle(0, 0, plate.width, plate.height));
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
        String methodName;
        if (useFullHeight) {
            methodName = "FullHeight";
        } else {
            methodName = "FullWidth";
        }
        if (Main.DEBUG_MaxRectBF) System.out.println("\n\n============== Job " + job.id + " (" + job.width + "x" + job.height + ") - " + methodName + " ==============");
        // Durchsucht alle verfügbaren freien Rechtecke
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle rect = freeRects.get(i);
            if (Main.DEBUG_MaxRectBF) System.out.println("  Prüfe FreeRect " + i + ": Startkoordinaten (x=" + rect.x + ", y=" + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
            // Originalposition testen
            testAndUpdateBestFit(job.width, job.height, rect, false, result);
            // Gedrehte Position testen
            if (Main.rotateJobs) testAndUpdateBestFit(job.height, job.width, rect, true, result);

        }
        // Wenn kein passender freier Bereich gefunden wurde: Job passt nicht
        if (result.bestRect == null) {
            if (Main.DEBUG_MaxRectBF) System.out.println("-> Kein passendes Rechteck gefunden für Job " + job.id);
            return false;
        }
        // Aktuellen Job speichern
        if (result.useRotated) {
            if (Main.DEBUG_MaxRectBF) System.out.println("-> Job wird GEDREHT platziert! (" + job.width + "x" + job.height + " → " + result.bestWidth + "x" + result.bestHeight + ")");
            job.rotated = true;
        } else {
            if (Main.DEBUG_MaxRectBF) System.out.println("-> Job wird in Originalausrichtung platziert.");
        }
        job.width = result.bestWidth;
        job.height = result.bestHeight;
        job.x = result.bestRect.x;
        job.y = result.bestRect.y;
        job.placedOn = plate;
        job.placementOrder = placementCounter++;
        plate.jobs.add(job);

        if (Main.DEBUG_MaxRectBF) System.out.println("-> Platziert in (" + job.x + ", " + job.y + ") auf " + plate.name);

        // Wähle die Splitting-Methode basierend auf useFullHeight
        if (useFullHeight) {
            splitFreeRectFullHeight(result.bestRect, job);
        } else {
            splitFreeRectFullWidth(result.bestRect, job);
        }

        // Zwischenschritte visualisieren
        if (Main.DEBUG) {
            PlateVisualizer.showPlate(plate, "2", this);
            // Wichtig: Mit Pause, sonst läuft das Programm zu schnell durch und es wird in jedem Fenster nur das Endergebnis angezeigt.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
            if (Main.DEBUG_MaxRectBF) System.out.println("    -> Passt in " + ausrichtung + "!");
            if (Main.DEBUG_MaxRectBF) System.out.println("       Berechnung leftoverHoriz: " + rect.width + " - " + testWidth + " = " + leftoverHoriz);
            if (Main.DEBUG_MaxRectBF) System.out.println("       Berechnung leftoverVert: " + rect.height + " - " + testHeight + " = " + leftoverVert);
            if (Main.DEBUG_MaxRectBF) System.out.println("       shortSideFit = " + shortSideFit + ", aktueller bestScore = " + result.bestScore);
            if (shortSideFit < result.bestScore) {
                result.bestScore = shortSideFit;
                result.bestRect = rect;
                result.useRotated = rotated;
                result.bestWidth = testWidth;
                result.bestHeight = testHeight;
                if (Main.DEBUG_MaxRectBF) System.out.println("       -> Neuer Best-Fit (" + ausrichtung + ")!");
            }
        } else {
            String ausrichtungsText;
            if (rotated) {
                ausrichtungsText = "GEDREHTER";
            } else {
                ausrichtungsText = "Original";
            }
            if (Main.DEBUG_MaxRectBF) System.out.println("    -> Passt NICHT in " + ausrichtungsText + " Ausrichtung.");
        }
    }

    // Freie Rechtecke mit vollständiger Breitenausdehnung nach rechts erzeugen
    private void splitFreeRectFullWidth(FreeRectangle rect, Job job) {
        if (Main.DEBUG_MaxRectBF) System.out.println("\n--- splitFreeRect aufgerufen ---");
        if (Main.DEBUG_MaxRectBF) System.out.println("Belegtes Rechteck: Start(" + rect.x + ", " + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
        if (Main.DEBUG_MaxRectBF) System.out.println("Jobgröße: Breite=" + job.width + "mm, Höhe=" + job.height + "mm");
        freeRects.remove(rect);
        if (Main.DEBUG_MaxRectBF) System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");
        // Neuer freier Bereich rechts neben dem Job
        if (job.width < rect.width) {
            // Leere Rechtecke werden sich nicht überschneiden, weil die ersten zwei gesplitteten Rechtecke (vom ersten Job) sich
            // natürlicherweise nicht überschneiden.
            // → Neue leere Rechtecke beziehen sich nicht auf den Rand der Platte, sondern auf die Größe des freien Rechtecks, 
            // in dem der Job platziert wurde (rect.width - job.width)
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, job.height);
            freeRects.add(newRectRight);
            if (Main.DEBUG_MaxRectBF) System.out.println("Füge freien Bereich rechts hinzu: Start(" + newRectRight.x + ", " + newRectRight.y + "), Breite=" + newRectRight.width + "mm, Höhe=" + newRectRight.height + "mm");
        }
        // Neuer freier Bereich unterhalb des Jobs
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, rect.width, rect.height - job.height);
            freeRects.add(newRectBelow);
            if (Main.DEBUG_MaxRectBF) System.out.println("Füge freien Bereich unten hinzu: Start(" + newRectBelow.x + ", " + newRectBelow.y + "), Breite=" + newRectBelow.width + "mm, Höhe=" + newRectBelow.height + "mm");
        }
        if (Main.DEBUG_MaxRectBF) System.out.println("\nAktuelle freie Rechtecke:");
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle r = freeRects.get(i);
            if (Main.DEBUG_MaxRectBF) System.out.println("  FreeRect " + i + ": Start(" + r.x + ", " + r.y + "), Breite=" + r.width + "mm, Höhe=" + r.height + "mm");
        }
    }

    // Freie Rechtecke mit vollständiger Höhenausdehnung nach rechts erzeugen
    private void splitFreeRectFullHeight(FreeRectangle rect, Job job) {
        if (Main.DEBUG_MaxRectBF) System.out.println("\n--- splitFreeRectFullHeight aufgerufen ---");
        if (Main.DEBUG_MaxRectBF) System.out.println("Belegtes Rechteck: Start(" + rect.x + ", " + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
        if (Main.DEBUG_MaxRectBF) System.out.println("Jobgröße: Breite=" + job.width + "mm, Höhe=" + job.height + "mm");
        freeRects.remove(rect);
        if (Main.DEBUG_MaxRectBF) System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");
        // Neuer freier Bereich rechts neben dem Job mit vollständiger Höhe
        if (job.width < rect.width) {
            // Unterschied ist der letzte Parameter: Höhe des neuen freien Rechtecks ist gleich der Höhe des vorherigen freien Rechtecks (rect.height) 
            // Also einfach umgekehrt zu splitFreeRectFullWidth, dort war die Breite des neuen freien Rechtecks gleich der Breite des vorherigen freien Rechtecks
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y, rect.width - job.width, rect.height);  
            freeRects.add(newRectRight);
            if (Main.DEBUG_MaxRectBF) System.out.println("Füge freien Bereich rechts hinzu (volle Höhe): Start(" + newRectRight.x + ", " + newRectRight.y + "), Breite=" + newRectRight.width + "mm, Höhe=" + newRectRight.height + "mm");
        }
        // Neuer freier Bereich unterhalb des Jobs
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, job.width, rect.height - job.height);
            freeRects.add(newRectBelow);
            if (Main.DEBUG_MaxRectBF) System.out.println("Füge freien Bereich unten hinzu (nur unter Job): Start(" + newRectBelow.x + ", " + newRectBelow.y + "), Breite=" + newRectBelow.width + "mm, Höhe=" + newRectBelow.height + "mm");
        }
        if (Main.DEBUG_MaxRectBF) System.out.println("\nAktuelle freie Rechtecke:");
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle r = freeRects.get(i);
            if (Main.DEBUG_MaxRectBF) System.out.println("  FreeRect " + i + ": Start(" + r.x + ", " + r.y + "), Breite=" + r.width + "mm, Höhe=" + r.height + "mm");
        }
    }
}
