package org.example.Visualizer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.example.DataClasses.Plate;
import org.example.SinglePlate.MaxRectBF_MultiPath;

import java.awt.*;
import java.util.List;

public class BenchmarkVisualizer extends JFrame {
    
    public static class BenchmarkResult {
        public String algorithmName;
        public Plate plate;
        public Object algorithm;
        public int placedJobs;
        public double coverageRate;
        public int totalJobs;
        public List<MaxRectBF_MultiPath.FreeRectangle> specificFreeRects;
        public int platesUsed = 1;
        public int platesTotal = 1;
        public String plate1Name;
        public Double coveragePlate1;
        public String plate2Name;
        public Double coveragePlate2;
        public Plate plate1Ref;
        public Plate plate2Ref;
        public List<?> plate1FreeRects;
        public List<?> plate2FreeRects;
        public java.util.List<Plate> platesRefs;          
        public java.util.List<String> platesNames;        
        public java.util.List<Double> platesCoverages;    
        public java.util.List<java.util.List<?>> platesFreeRects; 
        public String sortLabel;
        public String jobSetLabel;
        public String sortedBy;
        public String rootSetId;
        public java.util.List<String> perPlateSetLabels;

        // Standardkonstruktor
        public BenchmarkResult(String algorithmName, Plate plate, Object algorithm, int placedJobs, double coverageRate, int totalJobs) {
            this.algorithmName = algorithmName;
            this.plate = plate;
            this.algorithm = algorithm;
            this.placedJobs = placedJobs;
            this.coverageRate = coverageRate;
            this.totalJobs = totalJobs;
            this.specificFreeRects = null;
            this.plate1Name = plate != null ? plate.name : null;
            this.coveragePlate1 = coverageRate;
            this.plate1Ref = plate;
            this.plate2Ref = null;
            this.plate1FreeRects = null;
            this.plate2FreeRects = null;
            this.platesRefs = new java.util.ArrayList<>();
            this.platesNames = new java.util.ArrayList<>();
            this.platesCoverages = new java.util.ArrayList<>();
            this.platesFreeRects = new java.util.ArrayList<>();
            this.sortLabel = "-";
            this.jobSetLabel = "-";
            this.sortedBy = "-";
            this.rootSetId = "-";
            this.perPlateSetLabels = new java.util.ArrayList<>();
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
            this.plate1Name = plate != null ? plate.name : null;
            this.coveragePlate1 = coverageRate;
            this.plate1Ref = plate;
            this.plate2Ref = null;
            this.plate1FreeRects = null;
            this.plate2FreeRects = null;
            this.platesRefs = new java.util.ArrayList<>();
            this.platesNames = new java.util.ArrayList<>();
            this.platesCoverages = new java.util.ArrayList<>();
            this.platesFreeRects = new java.util.ArrayList<>();
            this.sortLabel = "-";
            this.jobSetLabel = "-";
            this.sortedBy = "-";
            this.rootSetId = "-";
            this.perPlateSetLabels = new java.util.ArrayList<>();
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
        
        JLabel titleLabel = new JLabel("PlateOptimizer", JLabel.CENTER);
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
        
        // Ermittele maximale Anzahl genutzter Platten über alle Ergebnisse
        int maxPlates = 0;
        for (BenchmarkResult r : results) {
            int used = r.platesRefs != null && !r.platesRefs.isEmpty() ? r.platesRefs.size() : r.platesUsed;
            if (used > maxPlates) maxPlates = used;
        }
        
        // Table Model mit dynamischen Spalten
        java.util.List<String> colNames = new java.util.ArrayList<>();
        colNames.add("Rang");
        colNames.add("Pfad");
        colNames.add("Sortiert nach");
        colNames.add("Root-Set (P1)");
        colNames.add("Platzierte Jobs");
        colNames.add("Gesamt Jobs");
        colNames.add("Erfolgsrate");
        for (int i = 1; i <= maxPlates; i++) { colNames.add("Platte " + i); colNames.add("Cov" + i); }
        DefaultTableModel model = new DefaultTableModel(colNames.toArray(new String[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                String name = getColumnName(columnIndex);
                if ("Rang".equals(name)) return Integer.class;
                if ("Pfad".equals(name)) return String.class;
                if ("Sortiert nach".equals(name)) return String.class;
                if ("Root-Set (P1)".equals(name)) return String.class;
                if ("Platzierte Jobs".equals(name)) return Integer.class;
                if ("Gesamt Jobs".equals(name)) return Integer.class;
                if ("Erfolgsrate".equals(name)) return Double.class;
                if (name.startsWith("Platte ")) return String.class;
                if (name.startsWith("Cov")) return Double.class;
                return Object.class;
            }
        };
        
        // Fülle die Tabelle mit Daten
        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult result = results.get(i);
            Integer rank = i + 1;
            double successRate = result.totalJobs == 0 ? 0.0 : (double) result.placedJobs / result.totalJobs * 100.0;
            String sortedBy = "-";
            if (result.sortedBy != null && !result.sortedBy.trim().isEmpty() && !"-".equals(result.sortedBy)) {
                sortedBy = result.sortedBy;
            } else if (result.sortLabel != null) {
                String sl = result.sortLabel.toLowerCase();
                if (sl.contains("fläche")) sortedBy = "Fläche";
                else if (sl.contains("kante")) sortedBy = "Kante";
            }
            java.util.List<Object> row = new java.util.ArrayList<>();
            row.add(rank);
            row.add(result.algorithmName);
            row.add(sortedBy);
            String root = result.rootSetId == null ? "-" : result.rootSetId;
            row.add(root);
            row.add(result.placedJobs);
            row.add(result.totalJobs);
            row.add(successRate);
            for (int pi = 0; pi < maxPlates; pi++) {
                String name = null; Double cov = null;
                if (result.platesNames != null && pi < result.platesNames.size()) name = result.platesNames.get(pi);
                if (result.platesCoverages != null && pi < result.platesCoverages.size()) cov = result.platesCoverages.get(pi);
                if (name == null) name = "-";
                row.add(name);
                row.add(cov);
            }
            model.addRow(row.toArray());
         }
         
         table = new JTable(model);
         javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(model);
         sorter.setSortable(0, false);
         // Comparator für Pfad-ID (Spalte "Pfad")
         sorter.setComparator(model.findColumn("Pfad"), (Object a, Object b) -> {
             String sa = a == null ? "" : a.toString();
             String sb = b == null ? "" : b.toString();
             String ida = sa.replaceAll("[^0-9.]", "");
             String idb = sb.replaceAll("[^0-9.]", "");
             String[] pa = ida.split("\\.");
             String[] pb = idb.split("\\.");
             int len = Math.max(pa.length, pb.length);
             for (int i2 = 0; i2 < len; i2++) {
                 int va = (i2 < pa.length && !pa[i2].isEmpty()) ? Integer.parseInt(pa[i2]) : 0;
                 int vb = (i2 < pb.length && !pb[i2].isEmpty()) ? Integer.parseInt(pb[i2]) : 0;
                 if (va != vb) return Integer.compare(va, vb);
             }
             return 0;
         });
         java.util.Comparator<Double> doubleComparator = (a, b) -> { if (a == b) return 0; if (a == null) return -1; if (b == null) return 1; return Double.compare(a, b); };
         sorter.setComparator(model.findColumn("Erfolgsrate"), doubleComparator);
         for (int ci = 0; ci < model.getColumnCount(); ci++) if (model.getColumnName(ci).startsWith("Cov")) sorter.setComparator(ci, doubleComparator);

        // Standard-Sortierung: Coverage-Rate (absteigend), dann Erfolgsrate (absteigend)
        int covCol = -1;
        for (int ci = 0; ci < model.getColumnCount(); ci++) {
            if (model.getColumnName(ci).startsWith("Cov")) { covCol = ci; break; }
        }
        java.util.List<javax.swing.RowSorter.SortKey> sortKeys = new java.util.ArrayList<>();
        if (covCol >= 0) sortKeys.add(new javax.swing.RowSorter.SortKey(covCol, javax.swing.SortOrder.DESCENDING));
        sortKeys.add(new javax.swing.RowSorter.SortKey(model.findColumn("Erfolgsrate"), javax.swing.SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);
        table.setRowSorter(sorter);
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
        
        // Automatische Spaltenbreite für "Pfad"-Spalte
        adjustAlgorithmColumnWidth();
        
        table.setDefaultRenderer(Object.class, new TableCellRenderer() {
             @Override
             public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String name = table.getColumnName(column);
                String text;
                if ("Rang".equals(name)) {
                    text = String.valueOf(row + 1);
                } else if ("Erfolgsrate".equals(name) || "Deckungsrate (gesamt)".equals(name) || name.startsWith("Cov")) {
                    if (value instanceof Number) text = String.format("%.2f%%", ((Number) value).doubleValue());
                    else if (value == null) text = "-"; else text = value.toString();
                } else if (value == null) {
                    text = "-";
                } else {
                    text = value.toString();
                }
                JLabel label = new JLabel(text);
                 label.setOpaque(true);
                 label.setFont(table.getFont());
                 label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                 
                 if (isSelected) {
                     label.setBackground(new Color(184, 207, 229));
                 } else {
                     if (row == 0) {
                         label.setBackground(new Color(255, 215, 0, 50));
                     } else if (row == 1) {
                         label.setBackground(new Color(192, 192, 192, 50));
                     } else if (row == 2) {
                         label.setBackground(new Color(205, 127, 50, 50));
                     } else {
                         label.setBackground(Color.WHITE);
                     }
                 }
                 
                 if ("Rang".equals(name) || "Platzierte Jobs".equals(name) || "Gesamt Jobs".equals(name) || "Erfolgsrate".equals(name) || "Deckungsrate (gesamt)".equals(name) || "Platten genutzt".equals(name) || name.startsWith("Cov")) {
                     label.setHorizontalAlignment(JLabel.CENTER);
                 } else {
                     label.setHorizontalAlignment(JLabel.LEFT);
                 }
 
                 return label;
             }
         });
     }
 
     private void adjustAlgorithmColumnWidth() {
         int maxWidth = 0;
         FontMetrics fm = table.getFontMetrics(table.getFont());
         
         String headerText = "Pfad";
         int headerWidth = fm.stringWidth(headerText);
         maxWidth = Math.max(maxWidth, headerWidth);
         
         for (int i = 0; i < results.size(); i++) {
             BenchmarkResult result = results.get(i);
             int textWidth = fm.stringWidth(result.algorithmName);
             maxWidth = Math.max(maxWidth, textWidth);
         }
         
         maxWidth += 40;
         
         table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("Pfad")).setPreferredWidth(maxWidth);
         table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("Pfad")).setMinWidth(maxWidth);
        try {
            int platesUsedIdx = table.getColumnModel().getColumnIndex("Platten genutzt");
            table.getColumnModel().getColumn(platesUsedIdx).setPreferredWidth(110);
        } catch (IllegalArgumentException ignored) {
        }
        // Dynamisch: setze Breite für Name/Cov Paare ab "Platte 1"
        int start = -1;
        try { start = table.getColumnModel().getColumnIndex("Platte 1"); } catch(Exception ignored) {}
        if (start >= 0) {
            for (int c = start; c < table.getColumnModel().getColumnCount(); c++) {
                String name = table.getColumnName(c);
                if (name.startsWith("Platte ")) table.getColumnModel().getColumn(c).setPreferredWidth(140);
                else if (name.startsWith("Cov")) table.getColumnModel().getColumn(c).setPreferredWidth(80);
            }
        }
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
        
        JButton visualizeSelectedButton = new JButton("👁️ Ausgewählte anzeigen");
        visualizeSelectedButton.setPreferredSize(new Dimension(180, 40));
        visualizeSelectedButton.addActionListener(e -> visualizeSelectedSolution());
        
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
    
    private void visualizeSelectedSolution() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            // View-Index -> Model-Index abbilden (wegen Sortierung/Filterung)
            int modelRow = table.convertRowIndexToModel(selectedRow);
            if (modelRow >= 0 && modelRow < results.size()) {
                BenchmarkResult selected = results.get(modelRow);
                showPlateVisualization(selected);
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Zeile aus der Tabelle aus.", "Keine Auswahl", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showPlateVisualization(BenchmarkResult result) {
        // Vor jeder Fenster-Visualisierung: bedingte Konsolenausgabe für ausgewählte Pfade
        // Falls BenchmarkResult einen explicit pathName enthält, nutze diesen; ansonsten versuche, den Pfad aus algorithmName zu extrahieren.
        String detectedPathId = null;
        if (result != null) {
            if (result.algorithmName != null && result.algorithmName.contains("Pfad ")) {
                int idx = result.algorithmName.indexOf("Pfad ");
                int start = idx + 5;
                int end = start;
                while (end < result.algorithmName.length() && (Character.isDigit(result.algorithmName.charAt(end)) || result.algorithmName.charAt(end) == '.')) end++;
                if (end > start) detectedPathId = result.algorithmName.substring(start, end);
            }
            // Falls platesRefs vorhanden ist, keine explizite Pfad-ID, aber wir haben eventuell plate names -> prüfe diese
            if (detectedPathId == null && result.platesNames != null && !result.platesNames.isEmpty()) {
                // versuche ersten Eintrag
                String name = result.platesNames.get(0);
                if (name != null && name.startsWith("Pfad ")) {
                    detectedPathId = name.substring(5).trim();
                }
            }
        }
        org.example.MultiPlate.MultiPlate_Controller.printPlateInfoIfSelected(result.plate, detectedPathId);

         // Optionaler Zusatztext zur Sortierung
         String globalAlgoInfo = (result.sortLabel == null || result.sortLabel.isEmpty() || "-".equals(result.sortLabel)) ? null : ("Sortierung: " + result.sortLabel);

         // Dynamische Mehrplatten-Visualisierung: öffne für alle genutzten Platten ein Fenster
         if (result.platesRefs != null && !result.platesRefs.isEmpty()) {
             for (int i = 0; i < result.platesRefs.size(); i++) {
                 Plate p = result.platesRefs.get(i);
                // Druck der Job-Koordinaten + Cuts für diese Platte
                org.example.MultiPlate.MultiPlate_Controller.printPlateInfo(p);
                 java.util.List<?> freeRects = (result.platesFreeRects != null && i < result.platesFreeRects.size()) ? result.platesFreeRects.get(i) : null;
                 String name = (result.platesNames != null && i < result.platesNames.size() && result.platesNames.get(i) != null) ? result.platesNames.get(i) : ("Platte " + (i + 1));
                 String title = result.algorithmName + " | " + name;
                 PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(p, "7", freeRects, title, globalAlgoInfo, jobListInfo);
             }
             return;
         }

         // Wenn zwei Platten referenziert sind, öffne beide Fenster inkl. freier Rechtecke
         if (result.plate1Ref != null && result.plate2Ref != null) {
            // Druck der Job-Koordinaten + Cuts für beide Platten
            org.example.MultiPlate.MultiPlate_Controller.printPlateInfo(result.plate1Ref);
            org.example.MultiPlate.MultiPlate_Controller.printPlateInfo(result.plate2Ref);

             String baseTitle = result.algorithmName;
             int idx = baseTitle.indexOf(" - ");
             if (idx > 0) baseTitle = baseTitle.substring(0, idx);
             String title1 = baseTitle + " | " + (result.plate1Name == null ? "Platte 1" : result.plate1Name);
             String title2 = baseTitle + " | " + (result.plate2Name == null ? "Platte 2" : result.plate2Name);
             PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(result.plate1Ref, "7", result.plate1FreeRects, title1, globalAlgoInfo, jobListInfo);
             PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(result.plate2Ref, "7", result.plate2FreeRects, title2, globalAlgoInfo, jobListInfo);
             return;
         }

        String mode;
        if (result.algorithmName.contains("First Fit")) {
            mode = "1";
            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(result.plate, mode, null, result.plate.name, globalAlgoInfo, jobListInfo);
        } else if (result.algorithmName.contains("Dynamic")) {
            mode = "3";
            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(result.plate, mode, null, result.plate.name, globalAlgoInfo, jobListInfo);
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
                String algoInfo = "Algorithmus: MultiPath (aus Benchmark)" + (globalAlgoInfo == null ? "" : " | " + globalAlgoInfo);
                PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(result.plate, mode, result.specificFreeRects, titleFromName, algoInfo, jobListInfo);
            } else if (result.algorithm instanceof MaxRectBF_MultiPath) {
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
                        String algoInfo = "Algorithmus: MultiPath (aus Benchmark)" + (globalAlgoInfo == null ? "" : " | " + globalAlgoInfo);
                        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo((Plate) plateObj, mode, freeRectsList, simplifiedTitle, algoInfo, jobListInfo);
                    } catch (Exception ex) {
                        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(result.plate, mode, null, result.plate.name, globalAlgoInfo, jobListInfo);
                    }
                } else {
                    PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(result.plate, mode, null, result.plate.name, globalAlgoInfo, jobListInfo);
                }
            } else {
                PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(result.plate, mode, null, result.plate.name, globalAlgoInfo, jobListInfo);
            }
        } else {
            mode = "2";
            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(result.plate, mode, null, result.plate.name, globalAlgoInfo, jobListInfo);
        }
    }

    public static void showBenchmarkResults(java.util.List<BenchmarkResult> results, String jobListInfo) {
        SwingUtilities.invokeLater(() -> {
            BenchmarkVisualizer visualizer = new BenchmarkVisualizer(results, jobListInfo);
            visualizer.setVisible(true);
        });
    }

}