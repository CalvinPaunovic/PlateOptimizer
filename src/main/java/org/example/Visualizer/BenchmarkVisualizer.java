package org.example.Visualizer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.example.Algorithms.MaxRectBF_MultiPath;
import org.example.DataClasses.Plate;

import java.awt.*;
import java.util.List;

public class BenchmarkVisualizer extends JFrame {
    
    public static class BenchmarkResult {
        public String algorithmName;
        public Plate plate;
        public Object algorithm;  // MaxRectBF, MaxRectBF_Dynamic, oder MaxRectBF_MultiPath
        public int placedJobs;
        public double coverageRate;
        public int totalJobs;
        public List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects; // Für MultiPath-Pfade

        // Standardkonstruktor
        public BenchmarkResult(String algorithmName, Plate plate, Object algorithm, int placedJobs, double coverageRate, int totalJobs) {
            this.algorithmName = algorithmName;
            this.plate = plate;
            this.algorithm = algorithm;
            this.placedJobs = placedJobs;
            this.coverageRate = coverageRate;
            this.totalJobs = totalJobs;
            this.specificFreeRects = null;
        }

        // Erweiteter Konstruktor mit freien Rechtecken
        public BenchmarkResult(String algorithmName, Plate plate, Object algorithm, int placedJobs, double coverageRate, int totalJobs, List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects) {
            this.algorithmName = algorithmName;
            this.plate = plate;
            this.algorithm = algorithm;
            this.placedJobs = placedJobs;
            this.coverageRate = coverageRate;
            this.totalJobs = totalJobs;
            this.specificFreeRects = specificFreeRects;
        }
    }
    
    private List<BenchmarkResult> results;
    private JTable table;
    private JLabel statisticsLabel;
    private String jobListInfo = "";
    
    public BenchmarkVisualizer(List<BenchmarkResult> results) {
        this(results, "");
    }

    public BenchmarkVisualizer(List<BenchmarkResult> results, String jobListInfo) {
        this.results = results;
        this.jobListInfo = jobListInfo;
        initializeGUI();
    }
    
    private void initializeGUI() {
        setTitle("Benchmark Ergebnisse - " + (jobListInfo == null ? "" : jobListInfo));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Table Panel
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);
        
        // Statistics Panel
        JPanel statisticsPanel = createStatisticsPanel();
        add(statisticsPanel, BorderLayout.SOUTH);
        
        // Button Panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.EAST);
        
        setSize(1000, 600); // Feste Fenstergröße
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(54, 103, 53)); // RAL 6001
        panel.setPreferredSize(new Dimension(0, 80));
        panel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("CodiPaq PlateOptimizer", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("Vergleich aller Platzierungsalgorithmen", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.WHITE);
        
        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(subtitleLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Algorithmus Ranking"));
        
        // Table Model
        String[] columnNames = {"Rang", "Algorithmus", "Platzierte Jobs", "Gesamt Jobs", "Erfolgsrate", "Deckungsrate"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Fülle die Tabelle mit Daten
        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult result = results.get(i);
            String rank = String.valueOf(i + 1);
            String successRate = String.format("%.1f%%", (double) result.placedJobs / result.totalJobs * 100);
            String coverageRate = String.format("%.2f%%", result.coverageRate);
            
            Object[] row = {
                rank,
                result.algorithmName,
                result.placedJobs,
                result.totalJobs,
                successRate,
                coverageRate
            };
            model.addRow(row);
        }
        
        table = new JTable(model);
        customizeTable();
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void customizeTable() {
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 240, 240));
        
        // Automatische Spaltenbreite für "Algorithmus"-Spalte (Index 1)
        adjustAlgorithmColumnWidth();
        
        // Custom cell renderer für farbige Zeilen
        table.setDefaultRenderer(Object.class, new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value.toString());
                label.setOpaque(true);
                label.setFont(table.getFont());
                label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
                if (isSelected) {
                    label.setBackground(new Color(184, 207, 229));
                } else {
                    // Verschiedene Farben je nach Rang
                    if (row == 0) {
                        label.setBackground(new Color(255, 215, 0, 50)); // Gold
                    } else if (row == 1) {
                        label.setBackground(new Color(192, 192, 192, 50)); // Silber
                    } else if (row == 2) {
                        label.setBackground(new Color(205, 127, 50, 50)); // Bronze
                    } else {
                        label.setBackground(Color.WHITE);
                    }
                }
                
                // Zentriere bestimmte Spalten
                if (column == 0 || column == 2 || column == 3 || column == 4 || column == 5) {
                    label.setHorizontalAlignment(JLabel.CENTER);
                }
                
                return label;
            }
        });
    }
    
    private void adjustAlgorithmColumnWidth() {
        // Finde die längste Algorithmus-Name
        int maxWidth = 0;
        FontMetrics fm = table.getFontMetrics(table.getFont());
        
        // Prüfe den Header-Text
        String headerText = "Algorithmus";
        int headerWidth = fm.stringWidth(headerText);
        maxWidth = Math.max(maxWidth, headerWidth);
        
        // Prüfe alle Algorithmus-Namen in den Daten
        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult result = results.get(i);
            int textWidth = fm.stringWidth(result.algorithmName);
            maxWidth = Math.max(maxWidth, textWidth);
        }
        
        // Füge Padding hinzu (für Borders und etwas Luft)
        maxWidth += 40;
        
        // Setze die Spaltenbreite
        table.getColumnModel().getColumn(1).setPreferredWidth(maxWidth);
        table.getColumnModel().getColumn(1).setMinWidth(maxWidth);
    }
    
    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Statistiken"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        if (!results.isEmpty()) {
            BenchmarkResult best = results.get(0);
            BenchmarkResult worst = results.get(results.size() - 1);
            double improvement = best.coverageRate - worst.coverageRate;
            
            statisticsLabel = new JLabel();
            statisticsLabel.setText(String.format(
                "<html><div style='padding: 10px;'>" +
                "<b>📊 DETAILLIERTE STATISTIKEN:</b><br/>" +
                "Plattenformat: %s (%.1f x %.1f mm)<br/>" + 
                "🥇 Beste Deckungsrate: %.2f%% (%s)<br/>" +
                "🥉 Schlechteste Deckungsrate: %.2f%% (%s)<br/>" +
                "📈 Verbesserung: %.2f Prozentpunkte<br/>" +
                "📋 Getestete Algorithmen: %d<br/>" +
                "⚡ Durchschnittliche Deckungsrate: %.2f%%<br/>" +
                "</div></html>",
                best.plate.name, best.plate.width, best.plate.height,
                best.coverageRate, best.algorithmName,
                worst.coverageRate, worst.algorithmName,
                improvement,
                results.size(),
                getAverageCoverageRate()
            ));
            
            panel.add(statisticsLabel);
        }
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Aktionen"));
        
        JButton visualizeBestButton = new JButton("🏆 Beste Lösung anzeigen");
        visualizeBestButton.setPreferredSize(new Dimension(180, 40));
        visualizeBestButton.addActionListener(e -> visualizeBestSolution());
        
        JButton visualizeSelectedButton = new JButton("👁️ Ausgewählte anzeigen");
        visualizeSelectedButton.setPreferredSize(new Dimension(180, 40));
        visualizeSelectedButton.addActionListener(e -> visualizeSelectedSolution());
        
        panel.add(Box.createVerticalStrut(10));
        panel.add(visualizeBestButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(visualizeSelectedButton);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private double getAverageCoverageRate() {
        double sum = 0;
        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult result = results.get(i);
            sum += result.coverageRate;
        }
        return sum / results.size();
    }
    
    private void visualizeBestSolution() {
        if (!results.isEmpty()) {
            BenchmarkResult best = results.get(0);
            showPlateVisualization(best);
        }
    }
    
    private void visualizeSelectedSolution() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < results.size()) {
            BenchmarkResult selected = results.get(selectedRow);
            showPlateVisualization(selected);
        } else {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Zeile aus der Tabelle aus.", "Keine Auswahl", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void showPlateVisualization(BenchmarkResult result) {
        // Print job coordinates before visualization
        printJobCoordinates(result.plate);

        String mode;
        if (result.algorithmName.contains("First Fit")) {
            mode = "1";
            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                result.plate, mode, null, result.plate.name, null, jobListInfo);
        } else if (result.algorithmName.contains("Dynamic")) {
            mode = "3";
            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                result.plate, mode, null, result.plate.name, null, jobListInfo);
        } else if (result.algorithmName.contains("MultiPath")) {
            mode = "4";
            if (result.specificFreeRects != null) {
                String titleFromName = "Pfad 1";
                if (result.algorithmName.contains("Pfad ")) {
                    int start = result.algorithmName.indexOf("Pfad ");
                    titleFromName = result.algorithmName.substring(start);
                    if (titleFromName.contains(" - ")) {
                        titleFromName = titleFromName.substring(titleFromName.lastIndexOf(" - ") + 3);
                    }
                }
                PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                    result.plate, 
                    mode, 
                    result.specificFreeRects, 
                    titleFromName, 
                    "Algorithmus: MultiPath (aus Benchmark)",
                    jobListInfo // <-- Joblisten-Info weitergeben
                );
            } else {
                if (result.algorithm instanceof MaxRectBF_MultiPath) {
                    MaxRectBF_MultiPath multiPathAlgorithm = (MaxRectBF_MultiPath) result.algorithm;
                    Object matchingPath = null;
                    List<?> allPaths = multiPathAlgorithm.getAllPaths();
                    for (int i = 0; i < allPaths.size(); i++) {
                        Object path = allPaths.get(i);
                        try {
                            java.lang.reflect.Field isActiveField = path.getClass().getDeclaredField("isActive");
                            java.lang.reflect.Field plateField = path.getClass().getDeclaredField("plate");
                            isActiveField.setAccessible(true);
                            plateField.setAccessible(true);
                            boolean isActive = isActiveField.getBoolean(path);
                            Object plateObj = plateField.get(path);
                            if (isActive && plateObj == result.plate) {
                                matchingPath = path;
                                break;
                            }
                        } catch (Exception ex) {
                            // Reflection failed, skip this path
                        }
                    }
                    if (matchingPath != null) {
                        String pathNumber = "1";
                        try {
                            java.lang.reflect.Field pathDescriptionField = matchingPath.getClass().getDeclaredField("pathDescription");
                            pathDescriptionField.setAccessible(true);
                            String pathDescription = (String) pathDescriptionField.get(matchingPath);
                            if (pathDescription != null && pathDescription.contains("Pfad ")) {
                                int start = pathDescription.indexOf("Pfad ") + 5;
                                int end = pathDescription.indexOf(" ", start);
                                if (end == -1) end = pathDescription.length();
                                pathNumber = pathDescription.substring(start, end);
                            }
                            String simplifiedTitle = "Pfad " + pathNumber;
                            java.lang.reflect.Field plateField = matchingPath.getClass().getDeclaredField("plate");
                            java.lang.reflect.Field freeRectsField = matchingPath.getClass().getDeclaredField("freeRects");
                            plateField.setAccessible(true);
                            freeRectsField.setAccessible(true);
                            Object plateObj = plateField.get(matchingPath);
                            Object freeRectsObj = freeRectsField.get(matchingPath);
                            List<MaxRectBF_MultiPath.FreeRectangle> freeRectsList = null;
                            if (freeRectsObj instanceof List<?>) {
                                freeRectsList = new java.util.ArrayList<>();
                                for (Object obj : (List<?>) freeRectsObj) {
                                    if (obj instanceof MaxRectBF_MultiPath.FreeRectangle) {
                                        freeRectsList.add((MaxRectBF_MultiPath.FreeRectangle) obj);
                                    }
                                }
                            }
                            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                                (Plate) plateObj, 
                                mode, 
                                freeRectsList, 
                                simplifiedTitle, 
                                "Algorithmus: MultiPath (aus Benchmark)",
                                jobListInfo // <-- Joblisten-Info weitergeben
                            );
                        } catch (Exception ex) {
                            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                                result.plate, mode, null, result.plate.name, null, jobListInfo);
                        }
                    } else {
                        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                            result.plate, mode, null, result.plate.name, null, jobListInfo);
                    }
                    PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                        result.plate, mode, null, result.plate.name, null, jobListInfo);
                }
            }
        } else {
            mode = "2";
            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                result.plate, mode, null, result.plate.name, null, jobListInfo);
        }
    }
    
    // Helper method to print job coordinates
    private void printJobCoordinates(org.example.DataClasses.Plate plate) {
        System.out.println("Job-Koordinaten auf Platte '" + plate.name + "':");
        for (org.example.DataClasses.Job job : plate.jobs) {
            System.out.printf("Job %d: x=%.2f, y=%.2f, w=%.2f, h=%.2f, rotated=%s\n",
                job.id, job.x, job.y, job.width, job.height, job.rotated ? "ja" : "nein");
        }
        System.out.println();
    }
    
    public static void showBenchmarkResults(List<BenchmarkResult> results, String jobListInfo) {
        // Kein Popup mehr, Info nur im Fenstertitel
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                BenchmarkVisualizer visualizer = new BenchmarkVisualizer(results, jobListInfo);
                visualizer.setVisible(true);
            }
        });
    }

    // Bestehende Methode für Rückwärtskompatibilität (ohne Info)
    public static void showBenchmarkResults(List<BenchmarkResult> results) {
        showBenchmarkResults(results, "Jobliste: unbekannt");
    }
}