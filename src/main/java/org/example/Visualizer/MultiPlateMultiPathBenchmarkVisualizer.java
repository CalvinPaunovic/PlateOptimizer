package org.example.Visualizer;

import org.example.Algorithms.MultiPlateMultiPath;
import org.example.Algorithms.MaxRectBF_MultiPath;
import org.example.DataClasses.Plate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class MultiPlateMultiPathBenchmarkVisualizer extends JFrame {

    public static class MultiPlatePathResult {
        public String pathName;
        public int plateCount;
        public int placedJobs;
        public int totalJobs;
        public double successRate;
        public double totalCoverageRate;
        public List<Double> coverageRatesPerPlate;
        public List<Plate> plates;
        public List<List<MaxRectBF_MultiPath.FreeRectangle>> freeRectsPerPlate;

        public MultiPlatePathResult(String pathName, int plateCount, int placedJobs, int totalJobs,
                                    double totalCoverageRate, List<Double> coverageRatesPerPlate,
                                    List<Plate> plates, List<List<MaxRectBF_MultiPath.FreeRectangle>> freeRectsPerPlate) {
            this.pathName = pathName;
            this.plateCount = plateCount;
            this.placedJobs = placedJobs;
            this.totalJobs = totalJobs;
            this.successRate = totalJobs == 0 ? 0 : (double) placedJobs / totalJobs * 100.0;
            this.totalCoverageRate = totalCoverageRate;
            this.coverageRatesPerPlate = coverageRatesPerPlate;
            this.plates = plates;
            this.freeRectsPerPlate = freeRectsPerPlate;
        }
    }

    private List<MultiPlatePathResult> results;
    private JTable table;
    private String jobListInfo;

    // Neue Member für Sortierlabel
    private List<String> sortLabels;

    public MultiPlateMultiPathBenchmarkVisualizer(List<MultiPlatePathResult> results, List<String> sortLabels, String jobListInfo) {
        this.results = results;
        this.sortLabels = sortLabels;
        this.jobListInfo = jobListInfo;
        initializeGUI();
    }

    private void initializeGUI() {
        // Sortiere nach Deckungsrate absteigend
        List<Integer> sortedIndices = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) sortedIndices.add(i);
        sortedIndices.sort((a, b) -> Double.compare(results.get(b).totalCoverageRate, results.get(a).totalCoverageRate));

        // Mappe: originalIndex -> Rang in sortierter Tabelle
        int[] indexToRank = new int[results.size()];
        for (int rank = 0; rank < sortedIndices.size(); rank++) {
            indexToRank[sortedIndices.get(rank)] = rank;
        }

        // Parent-Infos für alle Ergebnisse (sortiert!)
        // Wir wollen den Parent-Rang (in der sortierten Tabelle) für jede Zeile anzeigen.
        List<String> parentPathInfos = new ArrayList<>();
        for (int rankIdx = 0; rankIdx < sortedIndices.size(); rankIdx++) {
            int origIdx = sortedIndices.get(rankIdx);
            String parentInfo = "";
            if (!results.get(origIdx).plates.isEmpty()) {
                Plate plate = results.get(origIdx).plates.get(0);
                if (plate.parentPathIndex != null && plate.parentPathIndex >= 0 && plate.parentPathIndex < results.size()) {
                    // Finde den Rang (in der sortierten Tabelle) des Parent-Index
                    int parentRank = -1;
                    for (int r = 0; r < sortedIndices.size(); r++) {
                        if (sortedIndices.get(r) == plate.parentPathIndex) {
                            parentRank = r;
                            break;
                        }
                    }
                    if (parentRank >= 0 && parentRank != rankIdx) {
                        parentInfo = "Pfad " + (rankIdx + 1) + " entstand durch Pfad " + (parentRank + 1);
                    }
                }
            }
            parentPathInfos.add(parentInfo);
        }

        // Titel mit Parent-Info für beste Lösung (Zeile 1)
        String parentInfoForTitle = "";
        if (!parentPathInfos.isEmpty() && !parentPathInfos.get(0).isEmpty()) {
            parentInfoForTitle = " (" + parentPathInfos.get(0) + ")";
        }
        setTitle("MultiPlateMultiPath Benchmark - " + (jobListInfo == null ? "" : jobListInfo) + parentInfoForTitle);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel headerPanel = createHeaderPanel(parentInfoForTitle);
        add(headerPanel, BorderLayout.NORTH);

        JPanel tablePanel = createTablePanel(parentPathInfos, sortedIndices);
        add(tablePanel, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.EAST);

        setSize(1200, 600);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
    }

    // Passe Header-Panel an, um Parent-Info anzuzeigen
    private JPanel createHeaderPanel(String parentInfoForTitle) {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(70, 130, 180));
        panel.setPreferredSize(new Dimension(0, 60));
        panel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("MULTIPLATE-MULTIPATH BENCHMARK", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Alle Pfade über alle Platten" + (parentInfoForTitle != null ? parentInfoForTitle : ""), JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setForeground(Color.WHITE);

        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(subtitleLabel, BorderLayout.SOUTH);

        return panel;
    }

    // Passe createTablePanel an, um sortierte Indices zu bekommen
    private JPanel createTablePanel(List<String> parentPathInfos, List<Integer> sortedIndices) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Pfade & Ergebnisse"));

        String[] columnNames = {
            "Rang", "Sortierung", "Pfad", "Parent-Pfad", "Platten", "Platzierte Jobs", "Gesamt Jobs",
            "Erfolgsrate", "Deckungsrate (gesamt)", "Deckungsrate je Platte"
        };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (int rankIdx = 0; rankIdx < sortedIndices.size(); rankIdx++) {
            int i = sortedIndices.get(rankIdx);
            MultiPlatePathResult r = results.get(i);
            String rank = String.valueOf(rankIdx + 1);
            String coveragePerPlate = "";
            for (int j = 0; j < r.coverageRatesPerPlate.size(); j++) {
                coveragePerPlate += String.format("%.2f%%", r.coverageRatesPerPlate.get(j));
                if (j < r.coverageRatesPerPlate.size() - 1) coveragePerPlate += " / ";
            }
            String pfadText = r.pathName;
            String parentInfo = parentPathInfos.get(rankIdx);
            Object[] row = {
                rank,
                sortLabels != null && i < sortLabels.size() ? sortLabels.get(i).replaceAll(".*\\((nach .*)\\)", "$1") : "",
                pfadText,
                parentInfo,
                r.plateCount,
                r.placedJobs,
                r.totalJobs,
                String.format("%.1f%%", r.successRate),
                String.format("%.2f%%", r.totalCoverageRate),
                coveragePerPlate
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
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 240, 240));

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
                // Zentriere bestimmte Spalten
                if (column == 0 || column == 2 || column == 3 || column == 4 || column == 5 || column == 6) {
                    label.setHorizontalAlignment(JLabel.CENTER);
                }
                return label;
            }
        });
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

    private void visualizeBestSolution() {
        if (!results.isEmpty()) {
            showMultiPlatePathVisualization(results.get(0));
        }
    }

    private void visualizeSelectedSolution() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < results.size()) {
            showMultiPlatePathVisualization(results.get(selectedRow));
        } else {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Zeile aus der Tabelle aus.", "Keine Auswahl", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showMultiPlatePathVisualization(MultiPlatePathResult result) {
        // Strategiepfade für alle Platten sammeln
        StringBuilder strategyCodes = new StringBuilder();
        List<MaxRectBF_MultiPath.AlgorithmPath> matchingPaths = new ArrayList<>();
        try {
            // lastMultiPlateMultiPath gibt es nicht mehr, daher muss MultiPlateMultiPath als Parameter übergeben werden!
            // Hier ist ein Fallback: Strategiecode kann nicht angezeigt werden, matchingPaths bleibt leer.
            // Alternativ: Diese Methode müsste angepasst werden, um MultiPlateMultiPath als Parameter zu bekommen.
        } catch (Exception ex) {
            // ignore
        }

        String strategyInfo = strategyCodes.length() > 0 ? "Strategiepfad: " + strategyCodes : null;

        for (int i = 0; i < result.plates.size(); i++) {
            Plate plate = result.plates.get(i);

            // Hole die wirklich passenden freien Rechtecke für diesen Pfad und diese Platte
            List<MaxRectBF_MultiPath.FreeRectangle> freeRects = null;
            MaxRectBF_MultiPath.AlgorithmPath matchingPath = (matchingPaths.size() > i) ? matchingPaths.get(i) : null;
            if (matchingPath != null) {
                freeRects = matchingPath.freeRects;
            }
            // Fallback: falls keine passenden freien Rechtecke gefunden, nimm die gespeicherten
            if (freeRects == null) {
                freeRects = result.freeRectsPerPlate.get(i);
            }

            // Debug-Ausgabe: Anzahl der freien Rechtecke für jede Platte
            System.out.println("Platte " + (i + 1) + " (" + plate.name + "): " +
                (freeRects != null ? freeRects.size() : "null") + " freie Rechtecke.");

            // --- NEU: Parent-Info für Visualisierungstitel ---
            String parentInfo = "";
            if (plate.parentPathIndex != null && plate.parentPathIndex >= 0) {
                parentInfo = "entstand durch Pfad " + (plate.parentPathIndex + 1);
            }

            String title;
            try {
                // lastMultiPlateMultiPath gibt es nicht mehr, daher kann strategyCode nicht angezeigt werden
                title = String.format("Platte %d: %s | Pfad: %s%s", i + 1, plate.name, result.pathName,
                    parentInfo.isEmpty() ? "" : " (" + parentInfo + ")");
            } catch (Exception ex) {
                title = String.format("Platte %d: %s | Pfad: %s%s", i + 1, plate.name, result.pathName,
                    parentInfo.isEmpty() ? "" : " (" + parentInfo + ")");
            }
            PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                plate, "5", freeRects, title, strategyInfo, jobListInfo
            );
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Erzeugt die Benchmark-Resultate für alle Pfade aus einer MultiPlateMultiPath-Instanz.
     * Jeder Pfad (gleicher Index auf allen Platten) wird als eine Zeile betrachtet.
     */
    public static List<MultiPlatePathResult> collectResults(MultiPlateMultiPath multiPlateAlgo, int totalJobs) {
        List<MultiPlatePathResult> results = new ArrayList<>();
        List<MaxRectBF_MultiPath> algos = multiPlateAlgo.getMultiPathAlgorithms();
        int maxPaths = 0;
        for (MaxRectBF_MultiPath algo : algos) {
            maxPaths = Math.max(maxPaths, algo.getAllPaths().size());
        }

        // Hilfsset für Duplikatserkennung
        // uniquePathSignatures speichert eine Zeichenkette ("Signature") für jede Benchmark-Zeile,
        // die alle platzierten Jobs (ID, x, y, rot) auf allen Platten enthält.
        // Damit kann man erkennen, ob eine identische Lösung (über alle Platten) schon einmal als Zeile erzeugt wurde.
        // Beispiel-Signatur: "|1@0.00,0.00,N;2@100.00,0.00,R;|..." (für jede Platte ein Block)
        java.util.HashSet<String> uniquePathSignatures = new java.util.HashSet<>();

        for (int pathIdx = 0; pathIdx < maxPaths; pathIdx++) {
            List<Plate> plates = new ArrayList<>();
            List<Double> coverageRates = new ArrayList<>();
            List<List<MaxRectBF_MultiPath.FreeRectangle>> freeRectsPerPlate = new ArrayList<>();
            int placedJobsSum = 0;
            StringBuilder pathName = new StringBuilder();

            // Signatur für diesen Multi-Platten-Pfad
            StringBuilder signature = new StringBuilder();

            for (int plateIdx = 0; plateIdx < algos.size(); plateIdx++) {
                MaxRectBF_MultiPath algo = algos.get(plateIdx);
                List<MaxRectBF_MultiPath.AlgorithmPath> paths = algo.getAllPaths();
                if (paths.size() > pathIdx) {
                    MaxRectBF_MultiPath.AlgorithmPath path = paths.get(pathIdx);
                    plates.add(path.plate);
                    double coverage = PlateVisualizer.calculateCoverageRate(path.plate);
                    coverageRates.add(coverage);
                    freeRectsPerPlate.add(path.freeRects);
                    placedJobsSum += path.plate.jobs.size();
                    if (plateIdx == 0) {
                        pathName.append(path.pathDescription != null ? path.pathDescription : "Pfad " + (pathIdx + 1));
                    }
                    // Signatur: alle platzierten Jobs (ID, x, y, rot)
                    signature.append("|");
                    for (org.example.DataClasses.Job job : path.plate.jobs) {
                        signature.append(job.id).append("@")
                                 .append(String.format("%.2f", job.x)).append(",")
                                 .append(String.format("%.2f", job.y)).append(",")
                                 .append(job.rotated ? "R" : "N").append(";");
                    }
                } else {
                    // Korrektur: Für abzweigende Pfade nimm den letzten existierenden Pfad dieser Platte
                    MaxRectBF_MultiPath.AlgorithmPath lastPath = null;
                    if (!paths.isEmpty()) {
                        lastPath = paths.get(paths.size() - 1);
                    }
                    if (lastPath != null) {
                        plates.add(lastPath.plate);
                        double coverage = PlateVisualizer.calculateCoverageRate(lastPath.plate);
                        coverageRates.add(coverage);
                        freeRectsPerPlate.add(lastPath.freeRects);
                        placedJobsSum += lastPath.plate.jobs.size();
                        // Signatur: alle platzierten Jobs (ID, x, y, rot)
                        signature.append("|");
                        for (org.example.DataClasses.Job job : lastPath.plate.jobs) {
                            signature.append(job.id).append("@")
                                     .append(String.format("%.2f", job.x)).append(",")
                                     .append(String.format("%.2f", job.y)).append(",")
                                     .append(job.rotated ? "R" : "N").append(";");
                        }
                    } else {
                        // Fallback: wirklich leere Platte
                        plates.add(new Plate("Platte" + (plateIdx + 1), 0, 0));
                        coverageRates.add(0.0);
                        freeRectsPerPlate.add(new ArrayList<>());
                        signature.append("|");
                    }
                }
            }
            // Prüfe, ob diese Signatur schon existiert
            // Duplikate werden NICHT mehr entfernt!
            uniquePathSignatures.add(signature.toString());

            double totalCoverage = 0.0;
            for (double c : coverageRates) totalCoverage += c;
            results.add(new MultiPlatePathResult(
                pathName.toString(),
                plates.size(),
                placedJobsSum,
                totalJobs,
                totalCoverage,
                coverageRates,
                plates,
                freeRectsPerPlate
            ));
        }
        return results;
    }

    // Neue Methode: Ergebnisse mehrerer MultiPlateMultiPath-Instanzen gemeinsam anzeigen
    public static void showBenchmarkResults(MultiPlateMultiPath[] algos, String[] sortNames, int totalJobs) {
        List<MultiPlatePathResult> allResults = new ArrayList<>();
        List<String> sortLabels = new ArrayList<>();
        for (int i = 0; i < algos.length; i++) {
            List<MultiPlatePathResult> results = collectResults(algos[i], totalJobs);
            allResults.addAll(results);
            // Für jede Zeile das passende Sortierlabel merken
            for (int j = 0; j < results.size(); j++) {
                sortLabels.add(sortNames[i]);
            }
        }
        SwingUtilities.invokeLater(() -> {
            MultiPlateMultiPathBenchmarkVisualizer visualizer = new MultiPlateMultiPathBenchmarkVisualizer(allResults, sortLabels, sortNames[0].replaceAll(" \\(nach.*\\)$", ""));
            visualizer.setVisible(true);
        });
    }

    // Bestehende showBenchmarkResults bleibt für Kompatibilität erhalten
    public static void showBenchmarkResults(MultiPlateMultiPath multiPlateAlgo, String jobListInfo, int totalJobs) {
        List<MultiPlatePathResult> results = collectResults(multiPlateAlgo, totalJobs);
        List<String> sortLabels = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) sortLabels.add(jobListInfo);
        SwingUtilities.invokeLater(() -> {
            MultiPlateMultiPathBenchmarkVisualizer visualizer = new MultiPlateMultiPathBenchmarkVisualizer(results, sortLabels, jobListInfo);
            visualizer.setVisible(true);
        });
    }
}