package org.example;

import java.util.ArrayList;
import java.util.List;

public class MaxRectBF {
    Plate plate;
    List<FreeRectangle> freeRects;
    int placementCounter;  // Für Variable placementOrder in Job-Klasse

    public MaxRectBF(Plate plate) {
        this.plate = plate;
        freeRects = new ArrayList<>();
        freeRects.add(new FreeRectangle(0, 0, plate.width, plate.height));
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
        System.out.println("\n\n============== Job " + job.id + " (" + job.width + "x" + job.height + ") ==============");
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
            System.out.println("-> Kein passendes Rechteck gefunden für Job " + job.id);
            return false;
        }
        // Aktuellen Job speichern
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

        splitFreeRect(result.bestRect, job);

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
    private void testAndUpdateBestFit(int testWidth, int testHeight, FreeRectangle rect, boolean rotated, BestFitResult result) {
        if (testWidth <= rect.width && testHeight <= rect.height) {
            String ausrichtung = rotated ? "GEDREHTE Ausrichtung" : "Originalausrichtung";
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
            System.out.println("    -> Passt NICHT in " + (rotated ? "GEDREHTER" : "Original") + " Ausrichtung.");
        }
    }

    // Freie Rechtecke nach dem Platzieren eines Jobs erzeugen
    private void splitFreeRect(FreeRectangle rect, Job job) {
        System.out.println("\n--- splitFreeRect aufgerufen ---");
        System.out.println("Belegtes Rechteck: Start(" + rect.x + ", " + rect.y + "), Breite=" + rect.width + "mm, Höhe=" + rect.height + "mm");
        System.out.println("Jobgröße: Breite=" + job.width + "mm, Höhe=" + job.height + "mm");
        freeRects.remove(rect);
        System.out.println("Entferne belegtes Rechteck aus freien Bereichen.");
        // Neuer freier Bereich rechts neben dem Job
        if (job.width < rect.width) {
            // Leere Rechtecke werden sich nicht überschneiden, weil die ersten zwei gesplitteten Rechtecke (vom ersten Job) sich natürlicherweise nicht überschneiden.
            // → Neue leere Rechtecke beziehen sich nicht auf den Rand der Platte, sondern auf die Größe des Rechtecks, in dem der Job platziert wurde (rect.width - job.width)
            FreeRectangle newRectRight = new FreeRectangle(rect.x + job.width, rect.y,rect.width - job.width, job.height);
            freeRects.add(newRectRight);
            System.out.println("Füge freien Bereich rechts hinzu: Start(" + newRectRight.x + ", " + newRectRight.y + "), Breite=" + newRectRight.width + "mm, Höhe=" + newRectRight.height + "mm");
        }
        // Neuer freier Bereich unterhalb des Jobs
        if (job.height < rect.height) {
            FreeRectangle newRectBelow = new FreeRectangle(rect.x, rect.y + job.height, rect.width, rect.height - job.height);
            freeRects.add(newRectBelow);
            System.out.println("Füge freien Bereich unten hinzu: Start(" + newRectBelow.x + ", " + newRectBelow.y + "), Breite=" + newRectBelow.width + "mm, Höhe=" + newRectBelow.height + "mm");
        }
        System.out.println("\nAktuelle freie Rechtecke:");
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangle r = freeRects.get(i);
            System.out.println("  FreeRect " + i + ": Start(" + r.x + ", " + r.y + "), Breite=" + r.width + "mm, Höhe=" + r.height + "mm");
        }
    }
}
