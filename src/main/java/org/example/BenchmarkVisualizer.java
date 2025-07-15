package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
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
    
    public BenchmarkVisualizer(List<BenchmarkResult> results) {
        this.results = results;
        initializeGUI();
    }
    
    private void initializeGUI() {
        setTitle("Benchmark Ergebnisse - Algorithmus Vergleich");
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
        
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(70, 130, 180));
        panel.setPreferredSize(new Dimension(0, 80));
        panel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("BENCHMARK ERGEBNISSE", JLabel.CENTER);
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
                "🥇 Beste Deckungsrate: %.2f%% (%s)<br/>" +
                "🥉 Schlechteste Deckungsrate: %.2f%% (%s)<br/>" +
                "📈 Verbesserung: %.2f Prozentpunkte<br/>" +
                "📋 Getestete Algorithmen: %d<br/>" +
                "⚡ Durchschnittliche Deckungsrate: %.2f%%<br/>" +
                "</div></html>",
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
        
        JButton exportButton = new JButton("💾 Ergebnisse exportieren");
        exportButton.setPreferredSize(new Dimension(180, 40));
        exportButton.addActionListener(e -> exportResults());
        
        JButton refreshButton = new JButton("🔄 Aktualisieren");
        refreshButton.setPreferredSize(new Dimension(180, 40));
        refreshButton.addActionListener(e -> refreshView());
        
        panel.add(Box.createVerticalStrut(10));
        panel.add(visualizeBestButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(visualizeSelectedButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(exportButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(refreshButton);
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
        String mode;
        if (result.algorithmName.contains("First Fit")) {
            mode = "1";
            PlateVisualizer.showPlate(result.plate, mode, result.algorithm);
        } else if (result.algorithmName.contains("Dynamic")) {
            mode = "3";
            PlateVisualizer.showPlate(result.plate, mode, result.algorithm);
        } else if (result.algorithmName.contains("MultiPath")) {
            mode = "4";
            // Für MultiPath: Verwende die gleiche Visualisierung wie im Einzelmodus
            if (result.specificFreeRects != null) {
                // Extrahiere Pfad-Nummer und Strategie-Code für Titel
                String titleFromName = "Pfad 1";
                if (result.algorithmName.contains("Pfad ")) {
                    int start = result.algorithmName.indexOf("Pfad ");
                    // Nimm alles ab "Pfad" bis zum Ende (enthält bereits Strategie-Code falls vorhanden)
                    titleFromName = result.algorithmName.substring(start);
                    // Entferne den Algorithmus-Prefix falls vorhanden
                    if (titleFromName.contains(" - ")) {
                        titleFromName = titleFromName.substring(titleFromName.lastIndexOf(" - ") + 3);
                    }
                }
                
                // Verwende die gespeicherten freien Rechtecke
                PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                    result.plate, 
                    mode, 
                    result.specificFreeRects, 
                    titleFromName, 
                    "Algorithmus: MultiPath (aus Benchmark)"
                );                } else {
                    // Fallback: Versuche den Pfad im Algorithmus zu finden
                    if (result.algorithm instanceof MaxRectBF_MultiPath) {
                        MaxRectBF_MultiPath multiPathAlgorithm = (MaxRectBF_MultiPath) result.algorithm;
                        
                        // Finde den entsprechenden Pfad basierend auf der Platte
                        MaxRectBF_MultiPath.AlgorithmPath matchingPath = null;
                        List<MaxRectBF_MultiPath.AlgorithmPath> allPaths = multiPathAlgorithm.getAllPaths();
                        for (int i = 0; i < allPaths.size(); i++) {
                            MaxRectBF_MultiPath.AlgorithmPath path = allPaths.get(i);
                            if (path.isActive && path.plate == result.plate) {
                                matchingPath = path;
                                break;
                            }
                        }
                        
                        if (matchingPath != null) {
                            // Extrahiere Pfad-Nummer für Titel
                            String pathNumber = "1";
                            if (matchingPath.pathDescription.contains("Pfad ")) {
                                int start = matchingPath.pathDescription.indexOf("Pfad ") + 5;
                                int end = matchingPath.pathDescription.indexOf(" ", start);
                                if (end == -1) end = matchingPath.pathDescription.length();
                                pathNumber = matchingPath.pathDescription.substring(start, end);
                            }
                            String simplifiedTitle = "Pfad " + pathNumber;
                            
                            // Verwende die spezifischen freien Rechtecke des Pfads
                            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                                matchingPath.plate, 
                                mode, 
                                matchingPath.freeRects, 
                                simplifiedTitle, 
                                "Algorithmus: MultiPath (aus Benchmark)"
                            );
                        } else {
                            // Fallback falls kein Pfad gefunden wird
                            PlateVisualizer.showPlate(result.plate, mode, result.algorithm);
                        }
                    } else {
                        PlateVisualizer.showPlate(result.plate, mode, result.algorithm);
                    }
                }
        } else {
            mode = "2";
            PlateVisualizer.showPlate(result.plate, mode, result.algorithm);
        }
    }
    
    private void exportResults() {
        StringBuilder sb = new StringBuilder();
        sb.append("BENCHMARK ERGEBNISSE\n");
        sb.append("===================\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult result = results.get(i);
            sb.append(String.format("%d. %s\n", i + 1, result.algorithmName));
            sb.append(String.format("   Platzierte Jobs: %d/%d\n", result.placedJobs, result.totalJobs));
            sb.append(String.format("   Deckungsrate: %.2f%%\n\n", result.coverageRate));
        }
        
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Benchmark Ergebnisse - Export", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void refreshView() {
        // Hier könnte man die Benchmarks erneut ausführen
        JOptionPane.showMessageDialog(this, "View aktualisiert!", "Aktualisierung", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void showBenchmarkResults(List<BenchmarkResult> results) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new BenchmarkVisualizer(results).setVisible(true);
            }
        });
    }
}