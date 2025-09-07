package org.example.Visualizer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.DataClasses.MultiPlate_DataClasses; // neu
import org.example.SinglePlate.MaxRectBF_MultiPath;
import org.example.SinglePlate.MultiPlateMultiPath;
import org.example.MultiPlateIndividual.CutLineCalculator;
import org.example.MultiPlateIndividual.MultiPlateIndividual_Controller;

/**
 * PlateVisualizer
 *
 * Zeigt eine grafische Darstellung einer Platte mit allen platzierten Jobs
 * sowie optionalen freien Rechtecken (FreeRects) und Algorithmus-Info an.
 *
 * Diese Version ist ausführlich dokumentiert und formatiert, um die
 * verschiedenen Anzeigefälle (Single Plate, MultiPath, MultiPlate) klar zu
 * trennen und lesbar zu halten.
 */
public class PlateVisualizer extends JPanel {

    // ------------------------------------------------------
    // Instanzvariablen
    // ------------------------------------------------------

    /** Referenz auf die dargestellte Platte */
    private Plate plate;

    /** Optional: Algorithmus-Objekt, z.B. MaxRectBF_MultiPath */
    private Object algorithm;

    /** Modus-String: steuert welche Infos/FreeRects gezeichnet werden */
    private final String mode;

    /** Liste mit spezifischen freien Rechtecken (kann verschiedene Typen enthalten) */
    private List<?> specificFreeRects;

    /** Zusätzlicher Algorithmus-Info-Text, z.B. "Sortierung: Fläche" */
    private String customAlgorithmInfo;

    /** Info zur Jobliste, wird im Footer angezeigt */
    private String jobListInfo;


    /** Optional: vordefinierte, nummerierte Schnittlinien, von außen gesetzt */
    private java.util.List<CutLineCalculator.CutLine> externalCuts;

    public void setExternalCuts(java.util.List<CutLineCalculator.CutLine> cuts) { this.externalCuts = cuts; }

    // ------------------------------------------------------
    // Konstruktoren
    // ------------------------------------------------------

    public PlateVisualizer(Plate plate, String mode, Object algorithm) {
        this(plate, mode, algorithm, null);
    }

    public PlateVisualizer(Plate plate, String mode, Object algorithm, String jobListInfo) {
        this(plate, mode, algorithm, null, jobListInfo, null);
    }

    public PlateVisualizer(Plate plate, String mode, Object algorithm, List<?> specificFreeRects, String jobListInfo, String customAlgorithmInfo) {
        this.plate = plate;
        this.mode = mode;
        this.algorithm = algorithm;
        this.specificFreeRects = specificFreeRects;
        this.customAlgorithmInfo = customAlgorithmInfo;
        this.jobListInfo = jobListInfo;

        // Berechne bevorzugte Panel-Größe (Platte + Platz für Footer/Info)
        int extraHeight = 240;
        if ("4".equals(mode)) extraHeight = 280;

        setPreferredSize(new Dimension((int) Math.round(plate.width) + 100, (int) Math.round(plate.height) + extraHeight + 80));
    }


    // ------------------------------------------------------
    // Painting
    // ------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // Leichter Rand um die Platte
        int plateOffsetX = 50;
        int plateOffsetY = 50;
        g2d.translate(plateOffsetX, plateOffsetY);

        // Platte zeichnen (Hintergrund)
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, (int) Math.round(plate.width), (int) Math.round(plate.height));

        // --------------------------------------------------
        // Jobs: gefüllte Rechtecke + Label
        // --------------------------------------------------
        for (Job job : plate.jobs) {
            // Farbe: blau = normal, grün = rotiert
            Color fill = job.rotated ? new Color(0, 180, 0, 120) : new Color(0, 0, 255, 120);
            g2d.setColor(fill);
            g2d.fillRect((int) Math.round(job.x), (int) Math.round(job.y), (int) Math.round(job.width), (int) Math.round(job.height));

            // Rahmen und Beschriftung
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawRect((int) Math.round(job.x), (int) Math.round(job.y), (int) Math.round(job.width), (int) Math.round(job.height));

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Job " + job.id, (int) Math.round(job.x) + 5, (int) Math.round(job.y) + 15);

            // Dimensionstext, Order, Split-Info
            String dimensionText = (job.originalWidth > 0 && job.originalHeight > 0)
                    ? String.format("%.2fx%.2f (+%dmm)", job.originalWidth, job.originalHeight, Main.KERF_WIDTH)
                    : String.format("%.2fx%.2f", job.width, job.height);

            g2d.drawString(dimensionText, (int) Math.round(job.x) + 5, (int) Math.round(job.y) + 30);
            g2d.drawString("Order: " + job.placementOrder, (int) Math.round(job.x) + 5, (int) Math.round(job.y) + 45);

            if (job.splittingMethod != null) {
                g2d.drawString("Split: " + job.splittingMethod, (int) Math.round(job.x) + 5, (int) Math.round(job.y) + 60);
            }

            if (job.rotated) {
                g2d.drawString("(gedreht)", (int) Math.round(job.x) + 5, (int) Math.round(job.y) + 75);
            }
        }

        // --------------------------------------------------
        // Freie Rechtecke zeichnen (nur für unterstützte Modi)
        // --------------------------------------------------
        if ("4".equals(mode) || "5".equals(mode) || "7".equals(mode)) {
            if (specificFreeRects != null) {
                drawFreeRectangles(g2d, specificFreeRects);
            } else if (algorithm instanceof MaxRectBF_MultiPath && ("4".equals(mode) || "5".equals(mode))) {
                drawFreeRectangles(g2d, ((MaxRectBF_MultiPath) algorithm).getFreeRects());
            }
        }

        // --------------------------------------------------
        // Schnittlinien zeichnen und nummerieren (grün gestrichelt)
        // --------------------------------------------------
        java.util.List<CutLineCalculator.CutLine> cuts = (externalCuts != null) ? externalCuts : MultiPlateIndividual_Controller.computeCutLinesForPlate(plate);
        if (cuts != null && !cuts.isEmpty()) {
            Stroke oldStroke = g2d.getStroke();
            g2d.setColor(new Color(0, 150, 0));
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8, 6}, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 12));

            int over = 30; // Überhang-Länge außerhalb der Platte
            final double EPS = 1e-6;

            for (CutLineCalculator.CutLine cl : cuts) {
                int idx = cl.id; // Verwende die von computeCutLinesForPlate vergebene ID
                if (cl.vertical) {
                    int x = (int) Math.round(cl.coord);
                    int yStart = (int) Math.round(cl.start);
                    int yEnd = (int) Math.round(cl.end);

                    // Inneres Segment zeichnen (durch freie Bereiche)
                    g2d.drawLine(x, yStart, x, yEnd);

                    // Optionale Überhänge, nur wenn Segment am Rand anliegt
                    if (Math.abs(cl.start) < EPS) { // Am oberen Rand (y=0)
                        g2d.drawLine(x, yStart - over, x, yStart);
                    }
                    if (Math.abs(cl.end - plate.height) < EPS) { // Am unteren Rand
                        g2d.drawLine(x, yEnd, x, yEnd + over);
                    }

                    // Nummer oberhalb des Segments (z. B. mittig oder am Start)
                    g2d.drawString("C" + idx, x - 10, yStart - 12);
                } else {
                    int y = (int) Math.round(cl.coord);
                    int xStart = (int) Math.round(cl.start);
                    int xEnd = (int) Math.round(cl.end);

                    // Inneres Segment zeichnen (durch freie Bereiche)
                    g2d.drawLine(xStart, y, xEnd, y);

                    // Optionale Überhänge, nur wenn Segment am Rand anliegt
                    if (Math.abs(cl.start) < EPS) { // Am linken Rand (x=0)
                        g2d.drawLine(xStart - over, y, xStart, y);
                    }
                    if (Math.abs(cl.end - plate.width) < EPS) { // Am rechten Rand
                        g2d.drawLine(xEnd, y, xEnd + over, y);
                    }

                    // Nummer links vom Segment (z. B. mittig oder am Start)
                    g2d.drawString("C" + idx, xStart - 28, y - 4);
                }
            }
            g2d.setStroke(oldStroke);
        }

        // --------------------------------------------------
        // Footer / Statistik-Text
        // --------------------------------------------------
        g2d.setStroke(new BasicStroke(1f));

        int usedArea = 0;
        int placedJobsCount = 0;
        for (Job job : plate.jobs) {
            if (job.placedOn != null) {
                usedArea += (job.originalWidth > 0 && job.originalHeight > 0) ? job.originalWidth * job.originalHeight : job.width * job.height;
                placedJobsCount++;
            }
        }

        double coverageRate = calculateCoverageRate(plate);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));

        int textY = (int) Math.round(plate.height) + 120;

        g2d.drawString("Platzierte Jobs: " + placedJobsCount, 10, textY);
        g2d.drawString(String.format("Deckungsrate: %.2f%%", coverageRate), 10, textY + 25);
        g2d.drawString("Belegte Fläche: " + usedArea + " mm²", 10, textY + 50);

        String plateFormat = "Plattenformat: " + plate.name + " (" + String.format("%.1f", plate.width) + " x " + String.format("%.1f", plate.height) + " mm)";
        g2d.drawString(plateFormat, 10, textY + 75);

        if (jobListInfo != null && !jobListInfo.isEmpty()) {
            g2d.drawString("Jobliste: " + jobListInfo, 10, textY + 100);
        }

        if (("4".equals(mode) || "5".equals(mode) || "7".equals(mode)) && customAlgorithmInfo != null) {
            g2d.drawString(customAlgorithmInfo, 10, textY + 125);
        }
    }


    // ------------------------------------------------------
    // Hilfsfunktionen zum Zeichnen
    // ------------------------------------------------------

    /** Zeichnet eine Liste beliebiger FreeRect-Typen (MaxRect oder MultiPlate) */
    private void drawFreeRectangles(Graphics2D g2d, java.util.List<?> freeRects) {
        g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));

        for (int i = 0; i < freeRects.size(); i++) {
            Object rectObj = freeRects.get(i);

            double x = 0, y = 0, width = 0, height = 0;
            boolean match = false;

            if (rectObj instanceof MaxRectBF_MultiPath.FreeRectangle) {
                MaxRectBF_MultiPath.FreeRectangle r = (MaxRectBF_MultiPath.FreeRectangle) rectObj;
                x = r.x; y = r.y; width = r.width; height = r.height; match = true;
            } else if (rectObj instanceof MultiPlate_DataClasses.FreeRectangle) {
                MultiPlate_DataClasses.FreeRectangle r = (MultiPlate_DataClasses.FreeRectangle) rectObj;
                x = r.x; y = r.y; width = r.width; height = r.height; match = true;
            } else {
                // unbekannter Typ -> überspringen
                continue;
            }

            if (!match) continue;

            g2d.setColor(new Color(255, 0, 0, 50));
            g2d.fillRect((int) Math.round(x), (int) Math.round(y), (int) Math.round(width), (int) Math.round(height));

            g2d.setColor(Color.RED);
            g2d.drawRect((int) Math.round(x), (int) Math.round(y), (int) Math.round(width), (int) Math.round(height));

            g2d.setColor(Color.RED.darker());
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("F" + i, (int) Math.round(x) + 5, (int) Math.round(y) + 15);
            g2d.drawString(String.format("%.2fx%.2f", width, height), (int) Math.round(x) + 5, (int) Math.round(y) + 30);
        }
    }


    // ------------------------------------------------------
    // Utility / Statics
    // ------------------------------------------------------

    public static double calculateCoverageRate(Plate plate) {
        double totalPlateArea = plate.width * plate.height;
        double usedArea = 0;
        for (Job job : plate.jobs) {
            if (job.placedOn != null) {
                usedArea += (job.originalWidth > 0 && job.originalHeight > 0) ? job.originalWidth * job.originalHeight : job.width * job.height;
            }
        }
        return usedArea / totalPlateArea * 100;
    }


    // ------------------------------------------------------
    // Öffentliche Helfer zum Starten von Visualisierungen
    // ------------------------------------------------------

    public static void showPlate(Plate plate, String mode, Object algorithm) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(plate, mode, null, plate.name, null, null);
    }

    /**
     * Erzeugt ein Fenster und zeigt die gegebene Platte an.
     *
     * Parameter:
     *  - specificFreeRects: Liste mit freien Rechtecken (optional)
     *  - customTitle: Titel für das Fenster
     *  - algorithmInfo: zusätzlicher Info-String, wird im Footer angezeigt
     *  - jobListInfo: Joblisten-Beschreibung
     */
    public static void showPlateWithSpecificFreeRectsAndTitleAndInfo(Plate plate, String mode, List<?> specificFreeRects, String customTitle, String algorithmInfo, String jobListInfo) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Plate Visualizer - " + customTitle);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            Object algorithmToUse = null;
            if (specificFreeRects == null) {
                if ("4".equals(mode)) algorithmToUse = new MaxRectBF_MultiPath(plate);
            }

            PlateVisualizer visualizer = new PlateVisualizer(plate, mode, algorithmToUse, specificFreeRects, jobListInfo, algorithmInfo);

            int extraHeight = 240;
            if ("4".equals(mode) && algorithmInfo != null) extraHeight = 280;
            visualizer.setPreferredSize(new Dimension((int) Math.round(plate.width) + 100, (int) Math.round(plate.height) + extraHeight + 80));

            frame.getContentPane().add(visualizer);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void showPlateWithCutsAndTitleAndInfo(Plate plate, String mode, java.util.List<CutLineCalculator.CutLine> cuts, List<?> specificFreeRects, String customTitle, String algorithmInfo, String jobListInfo) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Plate Visualizer - " + customTitle);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            Object algorithmToUse = null;
            if (specificFreeRects == null) {
                if ("4".equals(mode)) algorithmToUse = new MaxRectBF_MultiPath(plate);
            }

            PlateVisualizer visualizer = new PlateVisualizer(plate, mode, algorithmToUse, specificFreeRects, jobListInfo, algorithmInfo);
            visualizer.setExternalCuts(cuts);

            int extraHeight = 240;
            if ("4".equals(mode) && algorithmInfo != null) extraHeight = 280;
            visualizer.setPreferredSize(new Dimension((int) Math.round(plate.width) + 100, (int) Math.round(plate.height) + extraHeight + 80));

            frame.getContentPane().add(visualizer);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }


    /**
     * Hilfs-Funktion für das alte MultiPlateMultiPath UI: zeichnet für jeden
     * Pfad eine Platte und gibt einfache Konsoleninfos aus. Wird von der
     * historischen GUI-Aufruferseite weiterhin genutzt.
     */
    public static void showBenchmarkResults(MultiPlateMultiPath algo, String jobListInfo) {

        List<Plate> plates = algo.getPlates();
        int plateCount = plates.size();
        int pathIndex = 1; // Standardauswahl

        for (int i = 0; i < plateCount; i++) {
            MaxRectBF_MultiPath multiPathAlgo = algo.getMultiPathAlgorithms().get(i);
            List<MaxRectBF_MultiPath.AlgorithmPath> paths = multiPathAlgo.getAllPaths();

            if (paths.size() > pathIndex) {
                MaxRectBF_MultiPath.AlgorithmPath path = paths.get(pathIndex);

                String strategyCode = multiPathAlgo.getStrategyCodeForPath(path);

                String title;
                if (strategyCode != null && !strategyCode.isEmpty()) {
                    title = plates.get(i).name + " | Pfad: " + path.pathDescription + " [" + strategyCode + "]";
                } else {
                    title = plates.get(i).name + " | Pfad: " + path.pathDescription;
                }

                showPlateWithSpecificFreeRectsAndTitleAndInfo(path.plate, "5", path.freeRects, title, null, jobListInfo);

                double coverage = calculateCoverageRate(path.plate);
                System.out.printf("Platte %d, Pfad %s: Deckungsrate: %.2f%%, Platzierte Jobs: %d\n", (i + 1), path.pathDescription, coverage, path.plate.jobs.size());

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    // ------------------------------------------------------
    // MultiPlate-spezifische Helfer
    // ------------------------------------------------------

    /**
     * Zeigt eine einzelne MultiPlate_DataClasses-Pfad-Platte an.
     */
    public static void showMultiPlatePath(MultiPlate_DataClasses path, String titleSuffix) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(path.plate, "7", path.freeRects, "MultiPlate Pfad " + path.pathId + (titleSuffix == null ? "" : " " + titleSuffix), null, null);
    }

}