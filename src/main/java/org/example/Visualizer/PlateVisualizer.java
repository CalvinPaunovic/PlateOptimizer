package org.example.Visualizer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import org.example.Main;
import org.example.Algorithms.MaxRectBF;
import org.example.Algorithms.MaxRectBF_Dynamic;
import org.example.Algorithms.MaxRectBF_MultiPath;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;

public class PlateVisualizer extends JPanel {
    Plate plate;
    Object algorithm;
    private final String mode;
    private List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects; // Für Multi-Path spezifische freie Rechtecke
    private String customAlgorithmInfo; // Für benutzerdefinierte Algorithmus-Informationen
    private String jobListInfo; // Name der verwendeten Jobliste

    public PlateVisualizer(Plate plate, String mode, Object algorithm) {
        this(plate, mode, algorithm, null);
    }

    // Erweiterter Konstruktor, um Joblisten-Info zu übergeben
    public PlateVisualizer(Plate plate, String mode, Object algorithm, String jobListInfo) {
        this.plate = plate;
        this.mode = mode;
        this.algorithm = algorithm;
        this.jobListInfo = jobListInfo;
        int extraHeight;
        if ("4".equals(mode)) {
            extraHeight = 280;
        } else {
            extraHeight = 240;
        }
        setPreferredSize(new Dimension((int)Math.round(plate.width) + 100, (int)Math.round(plate.height) + extraHeight + 80));
    }

    // Bemalt das Panel
    // wird automatisch vom PlateVisualizer aufgerufen
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        int plateOffsetX = 50;
        int plateOffsetY = 50;
        g2d.translate(plateOffsetX, plateOffsetY);

        // Hintergrund
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, (int)Math.round(plate.width), (int)Math.round(plate.height));

        // === Jobs ===
        List<Job> jobs = plate.jobs;
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            if (job.rotated) {
                g2d.setColor(new Color(0, 180, 0, 120));
            } else {
                g2d.setColor(new Color(0, 0, 255, 120));
            }
            // Füllung
            g2d.fillRect((int)Math.round(job.x), (int)Math.round(job.y), (int)Math.round(job.width), (int)Math.round(job.height));
            // Umrandung
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawRect((int)Math.round(job.x), (int)Math.round(job.y), (int)Math.round(job.width), (int)Math.round(job.height));

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Job " + job.id, (int)Math.round(job.x) + 5, (int)Math.round(job.y) + 15);

            String dimensionText;
            if (job.originalWidth > 0 && job.originalHeight > 0) {
                dimensionText = String.format("%.2fx%.2f (+%dmm)", job.originalWidth, job.originalHeight, Main.KERF_WIDTH);
            } else {
                dimensionText = String.format("%.2fx%.2f", job.width, job.height);
            }
            g2d.drawString(dimensionText, (int)Math.round(job.x) + 5, (int)Math.round(job.y) + 30);

            g2d.drawString("Order: " + job.placementOrder, (int)Math.round(job.x) + 5, (int)Math.round(job.y) + 45);
            if (job.splittingMethod != null) {
                g2d.drawString("Split: " + job.splittingMethod, (int)Math.round(job.x) + 5, (int)Math.round(job.y) + 60);
            }
            if (job.rotated) g2d.drawString("(gedreht)", (int)Math.round(job.x) + 5, (int)Math.round(job.y) + 75);
        }

        // === Freie Rechtecke ===
        if ("2".equals(mode) && algorithm instanceof MaxRectBF) {
            List<MaxRectBF.FreeRectangle> freeRects = ((MaxRectBF) algorithm).freeRects;
            drawFreeRectangles(g2d, freeRects);
        } else if ("3".equals(mode) && algorithm instanceof MaxRectBF_Dynamic) {
            List<MaxRectBF_Dynamic.FreeRectangle> freeRects = ((MaxRectBF_Dynamic) algorithm).freeRects;
            drawFreeRectangles(g2d, freeRects);
        } else if (("4".equals(mode) || "5".equals(mode)) && (algorithm instanceof MaxRectBF_MultiPath || specificFreeRects != null)) {
            // Für MultiPath und MultiPlateMultiPath: Mode 4 oder 5
            List<MaxRectBF_MultiPath.FreeRectangle> freeRects;
            if (specificFreeRects != null) {
                freeRects = specificFreeRects;
            } else {
                freeRects = ((MaxRectBF_MultiPath) algorithm).getFreeRects();
            }
            drawFreeRectangles(g2d, freeRects);
        } else {
            if (!"1".equals(mode)) System.err.println("Algorithmus-Typ passt nicht zum Mode oder unbekannt.");
        }

        // Reset Stroke (für eventuelle spätere Zeichnungen)
        g2d.setStroke(new BasicStroke(1f));

        // === Statistiken am unteren Rand ===
        int usedArea = 0;
        int placedJobsCount = 0;
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn != null) {
                if (job.originalWidth > 0 && job.originalHeight > 0) {
                    usedArea += job.originalWidth * job.originalHeight;
                } else {
                    usedArea += job.width * job.height;
                }
                placedJobsCount++;
            }
        }

        double coverageRate = calculateCoverageRate(plate);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        int textY = (int)Math.round(plate.height) + 120;
        g2d.drawString("Platzierte Jobs: " + placedJobsCount, 10, textY);
        g2d.drawString(String.format("Deckungsrate: %.2f%%", coverageRate), 10, textY + 25);
        g2d.drawString("Belegte Fläche: " + usedArea + " mm²", 10, textY + 50);

        // Plattenformat anzeigen (Name und Größe exakt wie vom PlateProvider übergeben, auch im Benchmark)
        String plateFormat = "Plattenformat: " + plate.name + " (" +
                String.format("%.1f", plate.width) + " x " +
                String.format("%.1f", plate.height) + " mm)";
        g2d.drawString(plateFormat, 10, textY + 75);

        // Joblisten-Info anzeigen (falls vorhanden)
        if (jobListInfo != null && !jobListInfo.isEmpty()) {
            g2d.drawString("Jobliste: " + jobListInfo, 10, textY + 100);
        }

        // Spezielle Informationen für MultiPath-Algorithmus
        if ("4".equals(mode) && customAlgorithmInfo != null) {
            g2d.drawString(customAlgorithmInfo, 10, textY + 125);
        }
    }

    // Hilfsmethode zum Zeichnen von freien Rechtecken
    private void drawFreeRectangles(Graphics2D g2d, java.util.List<?> freeRects) {
        g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));

        for (int i = 0; i < freeRects.size(); i++) {
            Object rectObj = freeRects.get(i);
            double x, y, width, height;

            if (rectObj instanceof MaxRectBF.FreeRectangle) {
                MaxRectBF.FreeRectangle rect = (MaxRectBF.FreeRectangle) rectObj;
                x = rect.x; y = rect.y; width = rect.width; height = rect.height;
            } else if (rectObj instanceof MaxRectBF_Dynamic.FreeRectangle) {
                MaxRectBF_Dynamic.FreeRectangle rect = (MaxRectBF_Dynamic.FreeRectangle) rectObj;
                x = rect.x; y = rect.y; width = rect.width; height = rect.height;
            } else if (rectObj instanceof MaxRectBF_MultiPath.FreeRectangle) {
                MaxRectBF_MultiPath.FreeRectangle rect = (MaxRectBF_MultiPath.FreeRectangle) rectObj;
                x = rect.x; y = rect.y; width = rect.width; height = rect.height;
            } else {
                continue;
            }

            g2d.setColor(new Color(255, 0, 0, 50));
            g2d.fillRect((int)Math.round(x), (int)Math.round(y), (int)Math.round(width), (int)Math.round(height));
            g2d.setColor(Color.RED);
            g2d.drawRect((int)Math.round(x), (int)Math.round(y), (int)Math.round(width), (int)Math.round(height));
            g2d.setColor(Color.RED.darker());
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("F" + i, (int)Math.round(x) + 5, (int)Math.round(y) + 15);
            g2d.drawString(String.format("%.2fx%.2f", width, height), (int)Math.round(x) + 5, (int)Math.round(y) + 30);
        }
    }

    // Statische Methode zur Berechnung der Deckungsrate
    public static double calculateCoverageRate(Plate plate) {
        double totalPlateArea = plate.width * plate.height;
        double usedArea = 0;
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn != null) {
                if (job.originalWidth > 0 && job.originalHeight > 0) {
                    usedArea += job.originalWidth * job.originalHeight;
                } else {
                    usedArea += job.width * job.height;
                }
            }
        }
        return usedArea / totalPlateArea * 100;
    }

    // Öffnet ein Swing-Fenster (Framework javax.swing), dass die aktuell definierten Zeichnungen visualisiert.
    // J-Frame ist eine Klasse innerhalb des Frameworks, die das Fenster erzeugt.
    public static void showPlate(Plate plate, String mode, Object algorithm) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(plate, mode, null, plate.name, null, null);
    }

    // Hauptmethode für alle Visualizer-Varianten
    public static void showPlateWithSpecificFreeRectsAndTitleAndInfo(
            Plate plate, String mode, List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects,
            String customTitle, String algorithmInfo, String jobListInfo) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Plate Visualizer - " + customTitle);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                // Erstelle PlateVisualizer - für showPlate() brauchen wir einen gültigen Algorithmus
                Object algorithmToUse = null;
                if (specificFreeRects == null) {
                    // Für normale showPlate-Aufrufe - verwende Dummy-Algorithmus für Visualisierung
                    if ("2".equals(mode)) {
                        algorithmToUse = new MaxRectBF(plate, false);
                    } else if ("3".equals(mode)) {
                        algorithmToUse = new MaxRectBF_Dynamic(plate);
                    } else if ("4".equals(mode)) {
                        algorithmToUse = new MaxRectBF_MultiPath(plate);
                    }
                }

                PlateVisualizer visualizer = new PlateVisualizer(plate, mode, algorithmToUse, jobListInfo);
                visualizer.specificFreeRects = specificFreeRects;
                visualizer.customAlgorithmInfo = algorithmInfo;

                // Stelle sicher, dass das Panel groß genug ist für alle Informationen
                int extraHeight;
                if ("4".equals(mode) && algorithmInfo != null) {
                    extraHeight = 280;
                } else {
                    extraHeight = 240;
                }
                visualizer.setPreferredSize(new Dimension((int)Math.round(plate.width) + 100, (int)Math.round(plate.height) + extraHeight + 80));

                frame.getContentPane().add(visualizer);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
