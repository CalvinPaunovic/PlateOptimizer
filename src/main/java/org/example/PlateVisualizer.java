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
        // Panelgröße mit mehr Platz für Beschriftungen
        int extraHeight;
        if ("4".equals(mode)) {
            extraHeight = 280;  // Mehr Platz für MultiPath
        } else {
            extraHeight = 240;  // Mehr Platz für alle Modi
        }
        // Mehr Platz links und oben für Schnitt-Beschriftungen
        setPreferredSize(new Dimension(plate.width + 100, plate.height + extraHeight + 80));
    }

    // Bemalt das Panel
    // wird automatisch vom PlateVisualizer aufgerufen
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;  // Bessere Grafikfunktionen

        // Platte 50px von links und 50px von oben verschieben
        int plateOffsetX = 50;
        int plateOffsetY = 50;
        
        // Transformiere alle Koordinaten
        g2d.translate(plateOffsetX, plateOffsetY);

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
            
            // Beschriftung für alle Modi
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Job " + job.id, job.x + 5, job.y + 15);
            
            // Zeige ursprüngliche Dimensionen falls verfügbar, sonst aktuelle
            String dimensionText;
            if (job.originalWidth > 0 && job.originalHeight > 0) {
                dimensionText = job.originalWidth + "x" + job.originalHeight + " (+" + Main.KERF_WIDTH + "mm)";
            } else {
                dimensionText = job.width + "x" + job.height;
            }
            g2d.drawString(dimensionText, job.x + 5, job.y + 30);
            
            g2d.drawString("Order: " + job.placementOrder, job.x + 5, job.y + 45);
            if (job.splittingMethod != null) {
                g2d.drawString("Split: " + job.splittingMethod, job.x + 5, job.y + 60);
            }
            if (job.rotated) g2d.drawString("(gedreht)", job.x + 5, job.y + 75);
        }

        // === Freie Rechtecke ===
        if ("2".equals(mode) && algorithm instanceof MaxRectBF) {
            List<MaxRectBF.FreeRectangle> freeRects = ((MaxRectBF) algorithm).freeRects;
            drawFreeRectangles(g2d, freeRects);
        } else if ("3".equals(mode) && algorithm instanceof MaxRectBF_Dynamic) {
            List<MaxRectBF_Dynamic.FreeRectangle> freeRects = ((MaxRectBF_Dynamic) algorithm).freeRects;
            drawFreeRectangles(g2d, freeRects);
        } else if ("4".equals(mode) && (algorithm instanceof MaxRectBF_MultiPath || specificFreeRects != null)) {
            // Verwende spezifische freie Rechtecke wenn verfügbar, sonst die vom Algorithmus
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

        // === Guillotine-Schnittlinien einzeichnen ===
        drawGuillotineCutLines(g2d, plate);

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
        
        // Berechne optimale Guillotine-Schnitte
        int totalCuts = calculateMinimalGuillotineCuts(plate);
        
        double coverageRate = calculateCoverageRate(plate);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        int textY = plate.height + 120;
        g2d.drawString("Platzierte Jobs: " + placedJobsCount, 10, textY);
        g2d.drawString(String.format("Deckungsrate: %.2f%%", coverageRate), 10, textY + 25);
        g2d.drawString("Belegte Fläche: " + usedArea + " mm²", 10, textY + 50);
        g2d.drawString("Guillotine-Schnitte: " + totalCuts, 10, textY + 75);
        
        // Spezielle Informationen für MultiPath-Algorithmus
        if ("4".equals(mode) && customAlgorithmInfo != null) {
            g2d.drawString(customAlgorithmInfo, 10, textY + 100);
        }
    }

    // Hilfsmethode zum Zeichnen von freien Rechtecken
    private void drawFreeRectangles(Graphics2D g2d, java.util.List<?> freeRects) {
        // Strichlinie für die Umrandung definieren
        g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        
        for (int i = 0; i < freeRects.size(); i++) {
            Object rectObj = freeRects.get(i);
            int x, y, width, height;
            
            // Extrahiere Eigenschaften je nach Typ
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
                continue; // Unbekannter Typ, überspringe
            }
            
            // Füllung
            g2d.setColor(new Color(255, 0, 0, 50)); // hellrot
            g2d.fillRect(x, y, width, height);
            // Umrandung
            g2d.setColor(Color.RED);
            g2d.drawRect(x, y, width, height);
            // Beschriftung
            g2d.setColor(Color.RED.darker());
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("F" + i, x + 5, y + 15);
            g2d.drawString(width + "x" + height, x + 5, y + 30);
        }
    }

    // Statische Methode zur Berechnung der Deckungsrate
    public static double calculateCoverageRate(Plate plate) {
        int totalPlateArea = plate.width * plate.height;
        int usedArea = 0;
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn != null) {
                // Verwende ursprüngliche Dimensionen für die Deckungsrate-Berechnung
                if (job.originalWidth > 0 && job.originalHeight > 0) {
                    usedArea += job.originalWidth * job.originalHeight;
                } else {
                    usedArea += job.width * job.height;
                }
            }
        }
        return (double) usedArea / totalPlateArea * 100;
    }


    // Öffnet ein Swing-Fenster (Framework javax.swing), dass die aktuell definierten Zeichnungen visualisiert.
    // J-Frame ist eine Klasse innerhalb des Frameworks, die das Fenster erzeugt.
    public static void showPlate(Plate plate, String mode, Object algorithm) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(plate, mode, null, plate.name, null);
    }

    // Spezieller Visualizer für Multi-Path mit spezifischen freien Rechtecken
    public static void showPlateWithSpecificFreeRects(Plate plate, String mode, List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(plate, mode, specificFreeRects, plate.name, null);
    }

    // Spezieller Visualizer für Multi-Path mit spezifischen freien Rechtecken und benutzerdefiniertem Titel
    public static void showPlateWithSpecificFreeRectsAndTitle(Plate plate, String mode, List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects, String customTitle) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(plate, mode, specificFreeRects, customTitle, null);
    }
    
    // Hauptmethode für alle Visualizer-Varianten
    public static void showPlateWithSpecificFreeRectsAndTitleAndInfo(Plate plate, String mode, List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects, String customTitle, String algorithmInfo) {
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
                
                PlateVisualizer visualizer = new PlateVisualizer(plate, mode, algorithmToUse);
                visualizer.specificFreeRects = specificFreeRects;
                visualizer.customAlgorithmInfo = algorithmInfo;
                
                // Stelle sicher, dass das Panel groß genug ist für alle Informationen
                int extraHeight;
                if ("4".equals(mode) && algorithmInfo != null) {
                    extraHeight = 280;
                } else {
                    extraHeight = 240;
                }
                visualizer.setPreferredSize(new Dimension(plate.width + 100, plate.height + extraHeight + 80));

                frame.getContentPane().add(visualizer);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
    
    /**
     * Berechnet die minimalen intelligenten Schnitte, um jeden Job individuell herauszuschneiden.
     * Sammelt ALLE vier Seiten jedes Jobs für komplettes Herausschneiden.
     */
    public static int calculateMinimalGuillotineCuts(Plate plate) {
        if (plate.jobs.isEmpty()) {
            return 0;
        }
        
        java.util.Set<Integer> allVerticalCuts = new java.util.HashSet<>();
        java.util.Set<Integer> allHorizontalCuts = new java.util.HashSet<>();
        
        // Für jeden Job ALLE vier Seiten sammeln
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn == null) continue;
            
            // LINKE Seite: Vertikaler Schnitt bei job.x (wenn nicht am Rand)
            if (job.x > 0) {
                allVerticalCuts.add(job.x);
            }
            
            // RECHTE Seite: Vertikaler Schnitt bei job.x + job.width (wenn nicht am Rand)
            if (job.x + job.width < plate.width) {
                allVerticalCuts.add(job.x + job.width);
            }
            
            // OBERE Seite: Horizontaler Schnitt bei job.y (wenn nicht am Rand)
            if (job.y > 0) {
                allHorizontalCuts.add(job.y);
            }
            
            // UNTERE Seite: Horizontaler Schnitt bei job.y + job.height (wenn nicht am Rand)
            if (job.y + job.height < plate.height) {
                allHorizontalCuts.add(job.y + job.height);
            }
        }
        
        return allVerticalCuts.size() + allHorizontalCuts.size();
    }
    
    /**
     * Zeichnet nur die gültigen Schnittlinien, die nicht durch Jobs gehen.
     * Sammelt ALLE vier Seiten jedes Jobs für komplettes Herausschneiden.
     */
    private void drawGuillotineCutLines(Graphics2D g2d, Plate plate) {
        if (plate.jobs.isEmpty()) {
            return;
        }
        
        // Debug-Ausgabe für Mode "1" oder für Pfad 1 in Mode "4"
        boolean isDebugMode = "1".equals(mode) || 
                             ("4".equals(mode) && plate.name.contains("Pfad 1"));
        
        if (isDebugMode) {
            System.out.println("\n" + "=".repeat(100));
            System.out.println("SCHNITTLINIEN-HERLEITUNG FÜR PLATTE: " + plate.name);
            System.out.println("=".repeat(100));
            System.out.println("Plattengröße: " + plate.width + "x" + plate.height + " mm");
            System.out.println("Anzahl Jobs: " + plate.jobs.size());
        }
        
        java.util.Set<Integer> allVerticalCuts = new java.util.TreeSet<>();
        java.util.Set<Integer> allHorizontalCuts = new java.util.TreeSet<>();
        
        if (isDebugMode) {
            System.out.println("\n--- SCHRITT 1: SAMMELN ALLER POTENTIELLEN SCHNITTLINIEN ---");
        }
        
        // Für jeden Job ALLE vier Seiten sammeln
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn == null) continue;
            
            if (isDebugMode) {
                System.out.println("\nJob " + job.id + " (" + job.width + "x" + job.height + " mm):");
                System.out.println("  Position: x=" + job.x + ", y=" + job.y);
                System.out.println("  Grenzen: links=" + job.x + ", rechts=" + (job.x + job.width) + 
                                 ", oben=" + job.y + ", unten=" + (job.y + job.height));
            }
            
            // LINKE Seite: Vertikaler Schnitt bei job.x (wenn nicht am Rand)
            if (job.x > 0) {
                allVerticalCuts.add(job.x);
                if (isDebugMode) {
                    System.out.println("  [OK] Vertikaler Schnitt LINKS bei X=" + job.x + " (Job " + job.id + " Position: " + job.x + "," + job.y + ", Abstand zum linken Rand: " + job.x + "mm)");
                }
            } else {
                if (isDebugMode) {
                    System.out.println("  [NO] Vertikaler Schnitt LINKS nicht möglich (Job " + job.id + " Position: " + job.x + "," + job.y + " - direkt am linken Rand X=0)");
                }
            }
            
            // RECHTE Seite: Vertikaler Schnitt bei job.x + job.width (wenn nicht am Rand)
            if (job.x + job.width < plate.width) {
                allVerticalCuts.add(job.x + job.width);
                if (isDebugMode) {
                    int distanceToRight = plate.width - (job.x + job.width);
                    System.out.println("  [OK] Vertikaler Schnitt RECHTS bei X=" + (job.x + job.width) + " (Job " + job.id + " Position: " + job.x + "," + job.y + ", Abstand zum rechten Rand: " + distanceToRight + "mm)");
                }
            } else {
                if (isDebugMode) {
                    System.out.println("  [NO] Vertikaler Schnitt RECHTS nicht möglich (Job " + job.id + " Position: " + job.x + "," + job.y + " - reicht bis rechten Rand X=" + plate.width + ")");
                }
            }
            
            // OBERE Seite: Horizontaler Schnitt bei job.y (wenn nicht am Rand)
            if (job.y > 0) {
                allHorizontalCuts.add(job.y);
                if (isDebugMode) {
                    System.out.println("  [OK] Horizontaler Schnitt OBEN bei Y=" + job.y + " (Job " + job.id + " Position: " + job.x + "," + job.y + ", Abstand zum oberen Rand: " + job.y + "mm)");
                }
            } else {
                if (isDebugMode) {
                    System.out.println("  [NO] Horizontaler Schnitt OBEN nicht möglich (Job " + job.id + " Position: " + job.x + "," + job.y + " - direkt am oberen Rand Y=0)");
                }
            }
            
            // UNTERE Seite: Horizontaler Schnitt bei job.y + job.height (wenn nicht am Rand)
            if (job.y + job.height < plate.height) {
                allHorizontalCuts.add(job.y + job.height);
                if (isDebugMode) {
                    int distanceToBottom = plate.height - (job.y + job.height);
                    System.out.println("  [OK] Horizontaler Schnitt UNTEN bei Y=" + (job.y + job.height) + " (Job " + job.id + " Position: " + job.x + "," + job.y + ", Abstand zum unteren Rand: " + distanceToBottom + "mm)");
                }
            } else {
                if (isDebugMode) {
                    System.out.println("  [NO] Horizontaler Schnitt UNTEN nicht möglich (Job " + job.id + " Position: " + job.x + "," + job.y + " - reicht bis unteren Rand Y=" + plate.height + ")");
                }
            }
        }
        
        if (isDebugMode) {
            System.out.println("\n--- SCHRITT 2: ZUSAMMENFASSUNG ALLER POTENTIELLEN SCHNITTE ---");
            System.out.println("Vertikale Schnitte (X-Koordinaten): " + allVerticalCuts);
            System.out.println("Horizontale Schnitte (Y-Koordinaten): " + allHorizontalCuts);
            System.out.println("Potentielle Schnitte gesamt: " + (allVerticalCuts.size() + allHorizontalCuts.size()));
        }
        
        // Zeichne ALLE gesammelten Schnittlinien
        g2d.setColor(new Color(255, 165, 0, 255)); // Orange ohne Transparenz
        g2d.setStroke(new BasicStroke(4f)); // Dicke durchgehende Linie
        
        int extensionLength = 20;
        
        if (isDebugMode) {
            System.out.println("\n--- SCHRITT 3: VALIDIERUNG VERTIKALER SCHNITTE ---");
        }
        
        // Zeichne ALLE vertikalen Schnittlinien
        int vIndex = 1;
        int validVerticalCuts = 0;
        for (Integer x : allVerticalCuts) {
            if (isDebugMode) {
                System.out.println("\nPrüfe vertikalen Schnitt bei X=" + x + ":");
            }
            
            // Prüfe ob Schnitt sinnvoll ist (nicht sofort auf Job stößt)
            if (isVerticalCutUseful(x, plate, isDebugMode)) {
                if (isDebugMode) {
                    System.out.println("  [GÜLTIG] - Schnitt wird gezeichnet als V" + vIndex);
                }
                validVerticalCuts++;
                
                // Zeichne Segmente, die nicht durch Jobs gehen
                drawVerticalCutSegments(g2d, x, plate, extensionLength);
                
                // Beschriftung oberhalb der Platte
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.setColor(new Color(255, 140, 0));
                g2d.drawString("V" + vIndex, x + 2, -5);
                
                // Beschriftung auf der Platte (falls Platz vorhanden)
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.setColor(new Color(255, 165, 0, 200)); // Leicht transparent
                g2d.drawString("" + vIndex, x - 8, 25);
                
                vIndex++;
            } else {
                if (isDebugMode) {
                    System.out.println("  [UNGÜLTIG] - Schnitt wird verworfen (nicht lang genug)");
                }
            }
        }
        
        if (isDebugMode) {
            System.out.println("\n--- SCHRITT 4: VALIDIERUNG HORIZONTALER SCHNITTE ---");
        }
        
        // Zeichne ALLE horizontalen Schnittlinien
        int hIndex = 1;
        int validHorizontalCuts = 0;
        for (Integer y : allHorizontalCuts) {
            if (isDebugMode) {
                System.out.println("\nPrüfe horizontalen Schnitt bei Y=" + y + ":");
            }
            
            // Prüfe ob Schnitt sinnvoll ist (nicht sofort auf Job stößt)
            if (isHorizontalCutUseful(y, plate, isDebugMode)) {
                if (isDebugMode) {
                    System.out.println("  [GÜLTIG] - Schnitt wird gezeichnet als H" + hIndex);
                }
                validHorizontalCuts++;
                
                // Zeichne Segmente, die nicht durch Jobs gehen
                drawHorizontalCutSegments(g2d, y, plate, extensionLength);
                
                // Beschriftung links von der Platte
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.setColor(new Color(255, 140, 0));
                g2d.drawString("H" + hIndex, -25, y - 2);
                
                // Beschriftung auf der Platte (falls Platz vorhanden)
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.setColor(new Color(255, 165, 0, 200)); // Leicht transparent
                g2d.drawString("" + (vIndex - 1 + hIndex), 5, y - 5);
                
                hIndex++;
            } else {
                if (isDebugMode) {
                    System.out.println("  [UNGÜLTIG] - Schnitt wird verworfen (nicht lang genug)");
                }
            }
        }
        
        if (isDebugMode) {
            System.out.println("\n--- SCHRITT 5: ENDRESULTAT ---");
            System.out.println("Gültige vertikale Schnitte: " + validVerticalCuts + " von " + allVerticalCuts.size());
            System.out.println("Gültige horizontale Schnitte: " + validHorizontalCuts + " von " + allHorizontalCuts.size());
            System.out.println("TOTAL BENÖTIGTE SCHNITTE: " + (validVerticalCuts + validHorizontalCuts));
            System.out.println("=".repeat(100) + "\n");
        }
    }
    
    /**
     * Zeichnet vertikale Schnittlinien nur bis zu anderen Jobs.
     */
    private void drawVerticalCutSegments(Graphics2D g2d, int x, Plate plate, int extension) {
        java.util.List<Integer> stopPoints = new java.util.ArrayList<>();
        
        // Sammle alle Y-Punkte, wo Jobs den Schnitt stoppen würden
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn != null && x >= job.x && x <= job.x + job.width) {
                stopPoints.add(job.y);          // Oberer Rand des Jobs
                stopPoints.add(job.y + job.height); // Unterer Rand des Jobs
            }
        }
        
        // Füge Plattenränder hinzu
        stopPoints.add(-extension);
        stopPoints.add(plate.height + extension);
        
        // Sortiere alle Punkte
        stopPoints.sort(Integer::compareTo);
        
        // Zeichne Segmente zwischen aufeinanderfolgenden Punkten
        for (int i = 0; i < stopPoints.size() - 1; i++) {
            int startY = stopPoints.get(i);
            int endY = stopPoints.get(i + 1);
            
            // Prüfe ob dieser Bereich frei von Jobs ist
            boolean isFree = true;
            for (int j = 0; j < plate.jobs.size(); j++) {
                Job job = plate.jobs.get(j);
                if (job.placedOn != null && x > job.x && x < job.x + job.width) {
                    // Schnitt geht durch Job - prüfe Y-Überschneidung
                    if (!(endY <= job.y || startY >= job.y + job.height)) {
                        isFree = false;
                        break;
                    }
                }
            }
            
            // Zeichne nur wenn der Bereich frei ist
            if (isFree && endY > startY) {
                g2d.drawLine(x, startY, x, endY);
            }
        }
    }
    
    /**
     * Zeichnet horizontale Schnittlinien nur bis zu anderen Jobs.
     */
    private void drawHorizontalCutSegments(Graphics2D g2d, int y, Plate plate, int extension) {
        java.util.List<Integer> stopPoints = new java.util.ArrayList<>();
        
        // Sammle alle X-Punkte, wo Jobs den Schnitt stoppen würden
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn != null && y >= job.y && y <= job.y + job.height) {
                stopPoints.add(job.x);          // Linker Rand des Jobs
                stopPoints.add(job.x + job.width); // Rechter Rand des Jobs
            }
        }
        
        // Füge Plattenränder hinzu
        stopPoints.add(-extension);
        stopPoints.add(plate.width + extension);
        
        // Sortiere alle Punkte
        stopPoints.sort(Integer::compareTo);
        
        // Zeichne Segmente zwischen aufeinanderfolgenden Punkten
        for (int i = 0; i < stopPoints.size() - 1; i++) {
            int startX = stopPoints.get(i);
            int endX = stopPoints.get(i + 1);
            
            // Prüfe ob dieser Bereich frei von Jobs ist
            boolean isFree = true;
            for (int j = 0; j < plate.jobs.size(); j++) {
                Job job = plate.jobs.get(j);
                if (job.placedOn != null && y > job.y && y < job.y + job.height) {
                    // Schnitt geht durch Job - prüfe X-Überschneidung
                    if (!(endX <= job.x || startX >= job.x + job.width)) {
                        isFree = false;
                        break;
                    }
                }
            }
            
            // Zeichne nur wenn der Bereich frei ist
            if (isFree && endX > startX) {
                g2d.drawLine(startX, y, endX, y);
            }
        }
    }
    
    /**
     * Optimierte Schnittplanung: Berechnet die Schneidereihenfolge für minimale Materialbewegung.
     */
    public static java.util.List<String> calculateOptimalCuttingSequence(Plate plate) {
        java.util.List<String> sequence = new java.util.ArrayList<>();
        
        if (plate.jobs.isEmpty()) {
            return sequence;
        }
        
        // Sammle alle Koordinaten
        java.util.Set<Integer> xCoords = new java.util.TreeSet<>();
        java.util.Set<Integer> yCoords = new java.util.TreeSet<>();
        
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn != null) {
                xCoords.add(job.x);
                xCoords.add(job.x + job.width);
                yCoords.add(job.y);
                yCoords.add(job.y + job.height);
            }
        }
        
        // Strategie: Erst alle vertikalen, dann alle horizontalen Schnitte
        // (oder umgekehrt - je nach Maschine)
        
        int cutIndex = 1;
        
        // Vertikale Schnitte (von links nach rechts)
        for (Integer x : xCoords) {
            if (x > 0 && x < plate.width) {
                sequence.add("Schnitt " + cutIndex + ": Vertikal bei X=" + x + "mm");
                cutIndex++;
            }
        }
        
        // Horizontale Schnitte (von oben nach unten)
        for (Integer y : yCoords) {
            if (y > 0 && y < plate.height) {
                sequence.add("Schnitt " + cutIndex + ": Horizontal bei Y=" + y + "mm");
                cutIndex++;
            }
        }
        
        return sequence;
    }
    
    /**
     * Prüft, ob ein vertikaler Schnitt sinnvoll ist (mindestens so lang wie der Job).
     */
    private boolean isVerticalCutUseful(int x, Plate plate) {
        return isVerticalCutUseful(x, plate, false);
    }
    
    /**
     * Prüft, ob ein vertikaler Schnitt sinnvoll ist (mindestens so lang wie der Job).
     */
    private boolean isVerticalCutUseful(int x, Plate plate, boolean debug) {
        if (debug) {
            System.out.println("    Suche Job für vertikalen Schnitt bei X=" + x);
        }
        
        // Finde den Job, für den dieser Schnitt gedacht ist
        Job targetJob = null;
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn != null && (x == job.x || x == job.x + job.width)) {
                targetJob = job;
                if (debug) {
                    System.out.println("    Gefunden! Job " + job.id + " hat Kante bei X=" + x);
                    if (x == job.x) {
                        System.out.println("    X=" + x + " ist die LINKE Kante von Job " + job.id);
                    } else {
                        System.out.println("    X=" + x + " ist die RECHTE Kante von Job " + job.id);
                    }
                }
                break;
            }
        }
        
        if (targetJob == null) {
            if (debug) {
                System.out.println("    Kein Job gefunden für X=" + x + " - Schnitt wird verworfen");
            }
            return false;
        }
        
        // Berechne verfügbare Schnittlänge von beiden Seiten
        int availableFromTop = targetJob.y;           // Platz von oben bis Job
        int availableFromBottom = plate.height - (targetJob.y + targetJob.height); // Platz von Job bis unten
        
        // Schnitt ist nur sinnvoll wenn er mindestens so lang ist wie der Job
        int requiredLength = targetJob.height;
        
        if (debug) {
            System.out.println("    Job " + targetJob.id + " Dimensionen: " + targetJob.width + "x" + targetJob.height + " mm");
            System.out.println("    Job " + targetJob.id + " Position: x=" + targetJob.x + ", y=" + targetJob.y);
            System.out.println("    Benötigte Schnittlänge: " + requiredLength + " mm (= Job-Höhe)");
            System.out.println("    Verfügbarer Platz von OBEN: " + availableFromTop + " mm (von Y=0 bis Y=" + targetJob.y + ")");
            System.out.println("    Verfügbarer Platz von UNTEN: " + availableFromBottom + " mm (von Y=" + (targetJob.y + targetJob.height) + " bis Y=" + plate.height + ")");
        }
        
        // Prüfe ob von oben oder unten genug Platz ist
        boolean canCutFromTop = availableFromTop >= requiredLength;
        boolean canCutFromBottom = availableFromBottom >= requiredLength;
        
        if (debug) {
            System.out.println("    Schnitt von OBEN möglich? " + canCutFromTop + " (" + availableFromTop + " >= " + requiredLength + ")");
            System.out.println("    Schnitt von UNTEN möglich? " + canCutFromBottom + " (" + availableFromBottom + " >= " + requiredLength + ")");
        }
        
        boolean result = canCutFromTop || canCutFromBottom;
        if (debug) {
            System.out.println("    ENDRESULTAT: " + (result ? "GÜLTIG" : "UNGÜLTIG") + " (mindestens eine Richtung muss >= " + requiredLength + "mm sein)");
        }
        
        return result;
    }
    
    /**
     * Prüft, ob ein horizontaler Schnitt sinnvoll ist (mindestens so lang wie der Job).
     */
    private boolean isHorizontalCutUseful(int y, Plate plate) {
        return isHorizontalCutUseful(y, plate, false);
    }
    
    /**
     * Prüft, ob ein horizontaler Schnitt sinnvoll ist (mindestens so lang wie der Job).
     */
    private boolean isHorizontalCutUseful(int y, Plate plate, boolean debug) {
        if (debug) {
            System.out.println("    Suche Job für horizontalen Schnitt bei Y=" + y);
        }
        
        // Finde den Job, für den dieser Schnitt gedacht ist
        Job targetJob = null;
        for (int i = 0; i < plate.jobs.size(); i++) {
            Job job = plate.jobs.get(i);
            if (job.placedOn != null && (y == job.y || y == job.y + job.height)) {
                targetJob = job;
                if (debug) {
                    System.out.println("    Gefunden! Job " + job.id + " hat Kante bei Y=" + y);
                    if (y == job.y) {
                        System.out.println("    Y=" + y + " ist die OBERE Kante von Job " + job.id);
                    } else {
                        System.out.println("    Y=" + y + " ist die UNTERE Kante von Job " + job.id);
                    }
                }
                break;
            }
        }
        
        if (targetJob == null) {
            if (debug) {
                System.out.println("    Kein Job gefunden für Y=" + y + " - Schnitt wird verworfen");
            }
            return false;
        }
        
        // Berechne verfügbare Schnittlänge von beiden Seiten
        int availableFromLeft = targetJob.x;           // Platz von links bis Job
        int availableFromRight = plate.width - (targetJob.x + targetJob.width); // Platz von Job bis rechts
        
        // Schnitt ist nur sinnvoll wenn er mindestens so lang ist wie der Job
        int requiredLength = targetJob.width;
        
        if (debug) {
            System.out.println("    Job " + targetJob.id + " Dimensionen: " + targetJob.width + "x" + targetJob.height + " mm");
            System.out.println("    Job " + targetJob.id + " Position: x=" + targetJob.x + ", y=" + targetJob.y);
            System.out.println("    Benötigte Schnittlänge: " + requiredLength + " mm (= Job-Breite)");
            System.out.println("    Verfügbarer Platz von LINKS: " + availableFromLeft + " mm (von X=0 bis X=" + targetJob.x + ")");
            System.out.println("    Verfügbarer Platz von RECHTS: " + availableFromRight + " mm (von X=" + (targetJob.x + targetJob.width) + " bis X=" + plate.width + ")");
        }
        
        // Prüfe ob von links oder rechts genug Platz ist
        boolean canCutFromLeft = availableFromLeft >= requiredLength;
        boolean canCutFromRight = availableFromRight >= requiredLength;
        
        if (debug) {
            System.out.println("    Schnitt von LINKS möglich? " + canCutFromLeft + " (" + availableFromLeft + " >= " + requiredLength + ")");
            System.out.println("    Schnitt von RECHTS möglich? " + canCutFromRight + " (" + availableFromRight + " >= " + requiredLength + ")");
        }
        
        boolean result = canCutFromLeft || canCutFromRight;
        if (debug) {
            System.out.println("    ENDRESULTAT: " + (result ? "GÜLTIG" : "UNGÜLTIG") + " (mindestens eine Richtung muss >= " + requiredLength + "mm sein)");
        }
        
        return result;
    }
}
