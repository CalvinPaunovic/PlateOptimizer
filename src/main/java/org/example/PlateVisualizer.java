package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PlateVisualizer extends JPanel {
    Plate plate;
    Object algorithm;  // Kann MaxRectBF oder MaxRectBFMerge sein
    private final String mode;
    private List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects; // Für Multi-Path spezifische freie Rechtecke
    private String customAlgorithmInfo; // Für benutzerdefinierte Algorithmus-Informationen

    public PlateVisualizer(Plate plate, String mode, Object algorithm) {
        this.plate = plate;
        this.mode = mode;
        this.algorithm = algorithm;
        // Panelgröße (Panel innerhalb des J-Frame-Fensters) initialisieren - mit extra Platz für Text
        // Für MultiPath-Mode extra Platz für zusätzliche Info-Zeile
        int extraHeight = "4".equals(mode) ? 140 : 100;
        setPreferredSize(new Dimension(plate.width + 50, plate.height + extraHeight));
    }

    // Bemalt das Panel
    // wird automatisch vom PlateVisualizer aufgerufen
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;  // Bessere Grafikfunktionen

        // Hintergrund
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, plate.width, plate.height);

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
            g2d.fillRect(job.x, job.y, job.width, job.height);
            // Umrandung
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3f)); // dicke schwarze Umrandung
            g2d.drawRect(job.x, job.y, job.width, job.height);
            
            // Beschriftung nur für Modi 1, 2, 3 - nicht für MultiPath Mode 4
            if (!"4".equals(mode)) {
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("Job " + job.id, job.x + 5, job.y + 15);
                g2d.drawString(job.width + "x" + job.height, job.x + 5, job.y + 30);
                if (job.rotated) g2d.drawString("(gedreht)", job.x + 5, job.y + 45);
                g2d.drawString("Order: " + job.placementOrder, job.x + 5, job.y + 60);
                if (job.splittingMethod != null) {
                    g2d.drawString("Split: " + job.splittingMethod, job.x + 5, job.y + 75);
                }
            }
        }

        // === Freie Rechtecke ===
        if ("2".equals(mode) && algorithm instanceof MaxRectBF) {
            // Strichlinie für die Umrandung definieren
            g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            // Holt die Liste der freien Rechtecke aus dem übergebenen Objekt algorithm
            List<MaxRectBF.FreeRectangle> freeRects = ((MaxRectBF) algorithm).freeRects;
            for (int i = 0; i < freeRects.size(); i++) {
                MaxRectBF.FreeRectangle rect = freeRects.get(i);
                // Füllung
                g2d.setColor(new Color(255, 0, 0, 50)); // hellrot
                g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
                // Umrandung
                g2d.setColor(Color.RED);
                g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
                // Beschriftung
                g2d.setColor(Color.RED.darker());
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("F" + i, rect.x + 5, rect.y + 15);
                g2d.drawString(rect.width + "x" + rect.height, rect.x + 5, rect.y + 30);
            }
        } else if ("3".equals(mode) && algorithm instanceof MaxRectBF_Dynamic) {
            // Strichlinie für die Umrandung definieren
            g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            // Holt die Liste der freien Rechtecke aus dem übergebenen Objekt algorithm
            List<MaxRectBF_Dynamic.FreeRectangle> freeRects = ((MaxRectBF_Dynamic) algorithm).freeRects;
            for (int i = 0; i < freeRects.size(); i++) {
                MaxRectBF_Dynamic.FreeRectangle rect = freeRects.get(i);
                // Füllung
                g2d.setColor(new Color(255, 0, 0, 50)); // hellrot
                g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
                // Umrandung
                g2d.setColor(Color.RED);
                g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
                // Beschriftung
                g2d.setColor(Color.RED.darker());
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("F" + i, rect.x + 5, rect.y + 15);
                g2d.drawString(rect.width + "x" + rect.height, rect.x + 5, rect.y + 30);
            }
        } else if ("4".equals(mode) && (algorithm instanceof MaxRectBF_MultiPath || specificFreeRects != null)) {
            // Strichlinie für die Umrandung definieren
            g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
            
            // Verwende spezifische freie Rechtecke wenn verfügbar, sonst die vom Algorithmus
            List<MaxRectBF_MultiPath.FreeRectangle> freeRects;
            if (specificFreeRects != null) {
                freeRects = specificFreeRects;
            } else {
                freeRects = ((MaxRectBF_MultiPath) algorithm).getFreeRects();
            }
            
            for (int i = 0; i < freeRects.size(); i++) {
                MaxRectBF_MultiPath.FreeRectangle rect = freeRects.get(i);
                // Füllung
                g2d.setColor(new Color(255, 0, 0, 50)); // hellrot
                g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
                // Umrandung
                g2d.setColor(Color.RED);
                g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
                // Keine Beschriftung für Mode 4 - saubere Darstellung
            }
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
                usedArea += job.width * job.height;
                placedJobsCount++;
            }
        }
        double coverageRate = calculateCoverageRate(plate);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        int textY = plate.height + 40;
        g2d.drawString("Platzierte Jobs: " + placedJobsCount, 10, textY);
        g2d.drawString(String.format("Deckungsrate: %.2f%%", coverageRate), 10, textY + 20);
        g2d.drawString("Belegte Fläche: " + usedArea + " mm²", 10, textY + 40);
        
        // Spezielle Informationen für MultiPath-Algorithmus
        if ("4".equals(mode) && customAlgorithmInfo != null) {
            g2d.drawString(customAlgorithmInfo, 10, textY + 60);
        }
    }

    // Statische Methode zur Berechnung der Deckungsrate
    public static double calculateCoverageRate(Plate plate) {
        int totalPlateArea = plate.width * plate.height;
        int usedArea = 0;
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn != null) {
                usedArea += job.width * job.height;
            }
        }
        return (double) usedArea / totalPlateArea * 100;
    }


    // Öffnet ein Swing-Fenster (Framework javax.swing), dass die aktuell definierten Zeichnungen visualisiert.
    // J-Frame ist eine Klasse innerhalb des Frameworks, die das Fenster erzeugt.
    public static void showPlate(Plate plate, String mode, Object algorithm) {
        // GUI-Thread starten
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Plate Visualizer - " + plate.name);  // Initialisiert das Fenster
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Beendet das Programm (auch die main), wenn das Fenster geschlossen wird
                frame.getContentPane().add(new PlateVisualizer(plate, mode, algorithm));  // Füge das Panel in das J-Frame-Fenster ein
                frame.pack();  // Fenster auf optimale Größe bringen. Entspricht setPreferredSize(new Dimension(plate.width + 50, plate.height + 50));
                frame.setLocationRelativeTo(null);  // Fenster zentrieren
                frame.setVisible(true);  // Hier ruft Swing automatisch die Methode paintComponent(Graphics g) auf
            }
        });
    }

    // Spezieller Visualizer für Multi-Path mit spezifischen freien Rechtecken
    public static void showPlateWithSpecificFreeRects(Plate plate, String mode, List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Plate Visualizer - " + plate.name);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                // Erstelle einen speziellen PlateVisualizer mit den spezifischen freien Rechtecken
                PlateVisualizer visualizer = new PlateVisualizer(plate, mode, null);
                visualizer.specificFreeRects = specificFreeRects;

                frame.getContentPane().add(visualizer);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    // Spezieller Visualizer für Multi-Path mit spezifischen freien Rechtecken und benutzerdefiniertem Titel
    public static void showPlateWithSpecificFreeRectsAndTitle(Plate plate, String mode, List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects, String customTitle) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(plate, mode, specificFreeRects, customTitle, null);
    }
    
    // Spezieller Visualizer für Multi-Path mit allen benutzerdefinierten Informationen
    public static void showPlateWithSpecificFreeRectsAndTitleAndInfo(Plate plate, String mode, List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects, String customTitle, String algorithmInfo) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Plate Visualizer - " + customTitle);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                // Erstelle einen speziellen PlateVisualizer mit den spezifischen freien Rechtecken
                PlateVisualizer visualizer = new PlateVisualizer(plate, mode, null);
                visualizer.specificFreeRects = specificFreeRects;
                visualizer.customAlgorithmInfo = algorithmInfo;
                
                // Stelle sicher, dass das Panel groß genug ist für alle Informationen
                int extraHeight = ("4".equals(mode) && algorithmInfo != null) ? 140 : 100;
                visualizer.setPreferredSize(new Dimension(plate.width + 50, plate.height + extraHeight));

                frame.getContentPane().add(visualizer);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
