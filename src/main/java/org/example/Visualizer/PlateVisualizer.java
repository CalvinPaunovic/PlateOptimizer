package org.example.Visualizer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import org.example.Main;
import org.example.Algorithms.CutLineCalculator;
import org.example.Algorithms.Controller;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.DataClasses.PlatePath;

public class PlateVisualizer extends JPanel {
    private JButton cutButton;

    private Plate plate;
    private final String mode;
    private List<?> specificFreeRects;
    private String customAlgorithmInfo;
    private String jobListInfo;
    private java.util.List<CutLineCalculator.CutLine> externalCuts;

    public void setExternalCuts(java.util.List<CutLineCalculator.CutLine> cuts) { this.externalCuts = cuts; }


    public PlateVisualizer(Plate plate, String mode, Object ignoredAlgorithm) {
        this(plate, mode, null, null);
    }


    public PlateVisualizer(Plate plate, String mode, Object ignoredAlgorithm, String jobListInfo) {
        this(plate, mode, null, null, jobListInfo, null);
    }

    public PlateVisualizer(Plate plate, String mode, Object ignoredAlgorithm, List<?> specificFreeRects, String jobListInfo, String customAlgorithmInfo) {
        this.plate = plate;
        this.mode = mode;
        this.specificFreeRects = specificFreeRects;
        this.customAlgorithmInfo = customAlgorithmInfo;
        this.jobListInfo = jobListInfo;

        int extraHeight = 240;
        if ("4".equals(mode)) extraHeight = 280;

        setPreferredSize(new Dimension((int) Math.round(plate.width) + 100, (int) Math.round(plate.height) + extraHeight + 80));

        // Button für Schnitt 1 setzen (Hinzufügen ins Layout erfolgt in show*-Methode)
        cutButton = new JButton("Schnitt setzen");
        cutButton.addActionListener(e -> handleFirstCut());
    }

    // Methode, die beim Klick auf den Button ausgeführt wird
    private void handleFirstCut() {
        java.util.List<org.example.Algorithms.CutLineCalculator.CutLine> cuts = org.example.Algorithms.CutLineCalculator.calculateSingleFullCut(plate);
        if (cuts == null || cuts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kein gültiger vollständiger Schnitt mehr möglich.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Schnitt in aktueller Platte visuell darstellen
        this.setExternalCuts(cuts);
        this.repaint();
        org.example.Algorithms.CutLineCalculator.CutLine cut = cuts.get(0);
        // Zwei neue Platten erzeugen (links/rechts oder oben/unten)
        Plate left = clonePlateWithBounds(plate, 0, 0, cut.vertical ? cut.coord : plate.width, cut.vertical ? plate.height : cut.coord);
        Plate right = clonePlateWithBounds(plate, cut.vertical ? cut.coord : 0, cut.vertical ? 0 : cut.coord, plate.width, plate.height);
        // Zeige beide Teilplatten in neuen Fenstern, ohne Schnitte zu visualisieren
        java.util.List<CutLineCalculator.CutLine> noCuts = new java.util.ArrayList<>();
        showPlateWithCutsAndTitleAndInfo(left, "cut1", noCuts, null, plate.name, null, null);
        showPlateWithCutsAndTitleAndInfo(right, "cut1", noCuts, null, plate.name, null, null);
    }

    // Hilfsmethode: Erzeugt eine neue Platte mit Jobs, die im angegebenen Bereich liegen
    private Plate clonePlateWithBounds(Plate original, double x0, double y0, double x1, double y1) {
        Plate p = new Plate(original.name, x1 - x0, y1 - y0, original.plateId);
        for (Job j : original.jobs) {
            double cx = j.x + j.width / 2.0;
            double cy = j.y + j.height / 2.0;
            if (cx >= x0 && cx < x1 && cy >= y0 && cy < y1) {
                Job nj = new Job(j.id, j.x - x0, j.y - y0, j.width, j.height);
                nj.originalWidth = j.originalWidth;
                nj.originalHeight = j.originalHeight;
                nj.placementOrder = j.placementOrder;
                nj.splittingMethod = j.splittingMethod;
                nj.rotated = j.rotated;
                nj.placedOn = p;
                p.jobs.add(nj);
            }
        }
        return p;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        int plateOffsetX = 50;
        int plateOffsetY = 50;
        g2d.translate(plateOffsetX, plateOffsetY);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, (int) Math.round(plate.width), (int) Math.round(plate.height));
        for (Job job : plate.jobs) {
            Color fill = job.rotated ? new Color(0, 180, 0, 120) : new Color(0, 0, 255, 120);
            g2d.setColor(fill);
            g2d.fillRect((int) Math.round(job.x), (int) Math.round(job.y), (int) Math.round(job.width), (int) Math.round(job.height));
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawRect((int) Math.round(job.x), (int) Math.round(job.y), (int) Math.round(job.width), (int) Math.round(job.height));

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Job " + job.id, (int) Math.round(job.x) + 5, (int) Math.round(job.y) + 15);

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

        if (specificFreeRects != null && ("4".equals(mode) || "5".equals(mode) || "7".equals(mode) || "9".equals(mode) || "cut1".equals(mode))) {
            drawFreeRectangles(g2d, specificFreeRects);
        }

        java.util.List<CutLineCalculator.CutLine> cuts;
        if ("cut1".equals(mode)) {
            // In interaktivem Modus keine automatischen Schnitte berechnen
            cuts = externalCuts;
        } else {
            cuts = (externalCuts != null) ? externalCuts : Controller.computeCutLinesForPlate(plate);
        }
        if (cuts != null && !cuts.isEmpty()) {
            Stroke oldStroke = g2d.getStroke();
            g2d.setColor(new Color(0, 150, 0));
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8, 6}, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 12));

            int over = 30;
            final double EPS = 1e-6;

            for (CutLineCalculator.CutLine cl : cuts) {
                int idx = cl.id;
                if (cl.vertical) {
                    int x = (int) Math.round(cl.coord);
                    int yStart = (int) Math.round(cl.start);
                    int yEnd = (int) Math.round(cl.end);

                    g2d.drawLine(x, yStart, x, yEnd);
                    if (Math.abs(cl.start) < EPS) {
                        g2d.drawLine(x, yStart - over, x, yStart);
                    }
                    if (Math.abs(cl.end - plate.height) < EPS) {
                        g2d.drawLine(x, yEnd, x, yEnd + over);
                    }
                    g2d.drawString("C" + idx, x - 10, yStart - 12);
                } else {
                    int y = (int) Math.round(cl.coord);
                    int xStart = (int) Math.round(cl.start);
                    int xEnd = (int) Math.round(cl.end);

                    g2d.drawLine(xStart, y, xEnd, y);
                    if (Math.abs(cl.start) < EPS) {
                        g2d.drawLine(xStart - over, y, xStart, y);
                    }
                    if (Math.abs(cl.end - plate.width) < EPS) {
                        g2d.drawLine(xEnd, y, xEnd + over, y);
                    }
                    g2d.drawString("C" + idx, xStart - 28, y - 4);
                }
            }
            g2d.setStroke(oldStroke);
        }
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


    private void drawFreeRectangles(Graphics2D g2d, java.util.List<?> freeRects) {
        g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));

        for (int i = 0; i < freeRects.size(); i++) {
            Object rectObj = freeRects.get(i);

            double x = 0, y = 0, width = 0, height = 0;
            boolean match = false;

            if (rectObj instanceof PlatePath.FreeRectangle) {
                PlatePath.FreeRectangle r = (PlatePath.FreeRectangle) rectObj;
                x = r.x; y = r.y; width = r.width; height = r.height; match = true;
            } else continue;

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


    public static void showPlate(Plate plate, String mode, Object ignoredAlgorithm) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(plate, mode, null, plate.name, null, null);
    }

    public static void showPlateWithSpecificFreeRectsAndTitleAndInfo(Plate plate, String mode, List<?> specificFreeRects, String customTitle, String algorithmInfo, String jobListInfo) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Plate Visualizer - " + customTitle);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            PlateVisualizer visualizer = new PlateVisualizer(plate, mode, null, specificFreeRects, jobListInfo, algorithmInfo);

            // In interaktivem Modus den geplanten ersten Schnitt sofort als Vorschau anzeigen
            if ("cut1".equals(mode)) {
                // Gesamte Sequenz visualisieren
                java.util.List<CutLineCalculator.CutLine> preview = CutLineCalculator.calculateAllFullCuts(plate);
                visualizer.setExternalCuts(preview);
            }

            int extraHeight = 240;
            if ("4".equals(mode) && algorithmInfo != null) extraHeight = 280;
            visualizer.setPreferredSize(new Dimension((int) Math.round(plate.width) + 100, (int) Math.round(plate.height) + extraHeight + 80));

            // Wenn Modus "cut1", dann Button unterhalb anzeigen
            if ("cut1".equals(mode)) {
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(visualizer, BorderLayout.CENTER);
                JButton cutButton = visualizer.cutButton;
                if (cutButton != null) panel.add(cutButton, BorderLayout.SOUTH);
                frame.getContentPane().add(panel);
            } else {
                frame.getContentPane().add(visualizer);
            }
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void showPlateWithCutsAndTitleAndInfo(Plate plate, String mode, java.util.List<CutLineCalculator.CutLine> cuts, List<?> specificFreeRects, String customTitle, String algorithmInfo, String jobListInfo) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Plate Visualizer - " + customTitle);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            PlateVisualizer visualizer = new PlateVisualizer(plate, mode, null, specificFreeRects, jobListInfo, algorithmInfo);
            visualizer.setExternalCuts(cuts);
            // In interaktivem Modus komplette Sequenz als Vorschau anzeigen, falls keine expliziten Cuts übergeben
            if ("cut1".equals(mode) && (cuts == null || cuts.isEmpty())) {
                java.util.List<CutLineCalculator.CutLine> preview = CutLineCalculator.calculateAllFullCuts(plate);
                visualizer.setExternalCuts(preview);
            }

            int extraHeight = 240;
            if ("4".equals(mode) && algorithmInfo != null) extraHeight = 280;
            visualizer.setPreferredSize(new Dimension((int) Math.round(plate.width) + 100, (int) Math.round(plate.height) + extraHeight + 80));

            // Wenn Modus "cut1", dann Button unterhalb anzeigen
            if ("cut1".equals(mode)) {
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(visualizer, BorderLayout.CENTER);
                JButton cutButton = visualizer.cutButton;
                if (cutButton != null) panel.add(cutButton, BorderLayout.SOUTH);
                frame.getContentPane().add(panel);
            } else {
                frame.getContentPane().add(visualizer);
            }
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }


    public static void showMultiPlatePath(PlatePath path, String titleSuffix) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(path.plate, "7", path.freeRects, "MultiPlate Pfad " + path.pathId + (titleSuffix == null ? "" : " " + titleSuffix), null, null);
    }

}