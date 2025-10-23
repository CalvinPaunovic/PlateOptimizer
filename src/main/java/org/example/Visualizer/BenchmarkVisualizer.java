package org.example.Visualizer;

import org.example.Algorithm.Controller;
import org.example.DataClasses.Plate;
import org.example.DataClasses.PlatePath;
import org.example.IOClasses.JsonOutputWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.List;
import org.example.Storage.LeftoverPlatesDb;

public class BenchmarkVisualizer extends JFrame {

    public static class BenchmarkResult {
        public String algorithmName;
        public Plate plate;
        public Object algorithm;
        public int placedJobs;
        public double coverageRate;
        public int totalJobs;
        public List<PlatePath.FreeRectangle> specificFreeRects;
        public java.util.List<Plate> platesRefs = new java.util.ArrayList<>();
        public java.util.List<String> platesNames = new java.util.ArrayList<>();
        public java.util.List<Double> platesCoverages = new java.util.ArrayList<>();
        public java.util.List<java.util.List<?>> platesFreeRects = new java.util.ArrayList<>();
        public String sortLabel = "-";
        public String rootSetId = "-";
        public String sortedBy = "-";
        public String jobSetLabel = "-";
        public java.util.List<String> perPlateSetLabels = new java.util.ArrayList<>();
        public boolean isSubRow = false;

        public BenchmarkResult(String algorithmName, Plate plate, int placedJobs, double coverageRate, int totalJobs) {
            this(algorithmName, plate, null, placedJobs, coverageRate, totalJobs, null, false);
        }

        public BenchmarkResult(String algorithmName, Plate plate, int placedJobs, double coverageRate, int totalJobs, List<PlatePath.FreeRectangle> specificFreeRects) {
            this(algorithmName, plate, null, placedJobs, coverageRate, totalJobs, specificFreeRects, false);
        }

        public BenchmarkResult(String algorithmName, Plate plate, Object algorithm, int placedJobs, double coverageRate, int totalJobs) {
            this(algorithmName, plate, algorithm, placedJobs, coverageRate, totalJobs, null, false);
        }

        public BenchmarkResult(String algorithmName, Plate plate, Object algorithm, int placedJobs, double coverageRate, int totalJobs, List<PlatePath.FreeRectangle> specificFreeRects) {
            this(algorithmName, plate, algorithm, placedJobs, coverageRate, totalJobs, specificFreeRects, false);
        }

        public BenchmarkResult(String algorithmName, Plate plate, Object algorithm, int placedJobs, double coverageRate, int totalJobs, List<PlatePath.FreeRectangle> specificFreeRects, boolean isSubRow) {
            this.algorithmName = algorithmName;
            this.plate = plate;
            this.algorithm = algorithm;
            this.placedJobs = placedJobs;
            this.coverageRate = coverageRate;
            this.totalJobs = totalJobs;
            this.specificFreeRects = specificFreeRects;
            this.isSubRow = isSubRow;
        }
    }

    private final List<BenchmarkResult> results;
    private JTable table;
    private JLabel statisticsLabel;
    private final String jobListInfo;

    public BenchmarkVisualizer(List<BenchmarkResult> results) { this(results, ""); }

    public BenchmarkVisualizer(List<BenchmarkResult> results, String jobListInfo) {
        this.results = results;
        this.jobListInfo = jobListInfo == null ? "" : jobListInfo;
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Benchmark Ergebnisse - " + jobListInfo);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createStatisticsPanel(), BorderLayout.SOUTH);
        add(createButtonPanel(), BorderLayout.EAST);
        setSize(1000, 600);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(54,103,53));
        panel.setPreferredSize(new Dimension(0,80));
        JLabel title = new JLabel("PlateOptimizer", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Benchmark Übersicht", JLabel.CENTER);
        sub.setFont(new Font("Arial", Font.PLAIN, 14));
        sub.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.CENTER);
        panel.add(sub, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Ergebnisse"));


        String[] columns = {"Rang","Algorithmus","Sortierung","Job-Set","Platzierte Jobs","Gesamt Jobs","Erfolgsrate","Deckungsrate"};
        DefaultTableModel model = new DefaultTableModel(columns,0){
            @Override public boolean isCellEditable(int r,int c){return false;}
            @Override public Class<?> getColumnClass(int col){
                if(col==0||col==4||col==5) return Integer.class;
                if(col==6||col==7) return Double.class;
                return String.class;
            }
        };

        boolean hasSubRows = false;
        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult r = results.get(i);
            if (r != null && r.isSubRow) { hasSubRows = true; break; }
        }

        int rankCounter = 0; // rank only main rows
        for(int i=0;i<results.size();i++){
            BenchmarkResult r=results.get(i);
            double success = r.totalJobs==0?0.0:(double)r.placedJobs/r.totalJobs*100.0;
            String jobSet = (r.jobSetLabel != null && !"-".equals(r.jobSetLabel)) ? r.jobSetLabel : "-";
            Integer rankVal = r.isSubRow ? null : (++rankCounter);
            model.addRow(new Object[]{rankVal,r.algorithmName,r.sortedBy,jobSet,r.placedJobs,r.totalJobs,success,r.coverageRate});
        }

        table = new JTable(model);
        // Preserve insertion order if we detect grouped job-set subrows or the title signals a job-set summary view
        boolean preserveOrder = hasSubRows || (jobListInfo != null && jobListInfo.toLowerCase().contains("job-set"));
        if (!preserveOrder) {
            javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(model);
            table.setRowSorter(sorter);
            sorter.toggleSortOrder(6); // sort by Erfolgsrate
        }
        customizeTable();

        // Verhindere Auswahl von Unterzeilen: wenn eine Unterzeile angeklickt wird, springe auf die Hauptzeile
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                int viewRow = table.getSelectedRow();
                if (viewRow < 0) return;
                int modelRow = table.getRowSorter()==null ? viewRow : table.convertRowIndexToModel(viewRow);
                if (modelRow < 0 || modelRow >= results.size()) return;
                BenchmarkResult r = results.get(modelRow);
                if (r != null && r.isSubRow) {
                    // finde vorherige Hauptzeile
                    int prev = modelRow - 1;
                    while (prev >= 0 && results.get(prev) != null && results.get(prev).isSubRow) prev--;
                    if (prev >= 0) {
                        int viewPrev = table.getRowSorter()==null ? prev : table.convertRowIndexToView(prev);
                        table.getSelectionModel().setSelectionInterval(viewPrev, viewPrev);
                    } else {
                        table.clearSelection();
                    }
                }
            }
        });
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void customizeTable(){
        table.setFont(new Font("Arial",Font.PLAIN,12));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Arial",Font.BOLD,12));
        table.setDefaultRenderer(Object.class, new TableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column){
                // Resolve model row (for styling even when a sorter is active)
                int modelRow = table.getRowSorter()==null ? row : table.convertRowIndexToModel(row);
                BenchmarkResult r = (modelRow>=0 && modelRow<results.size()) ? results.get(modelRow) : null;
                boolean isSub = r != null && r.isSubRow;

                String text;
                if(value instanceof Number && (column==6||column==7)) text=String.format("%.2f%%", ((Number)value).doubleValue());
                else text=value==null?"-":value.toString();

                // Prettify subrow algorithm text with an arrow indicator
                if (isSub && column==1) {
                    String trimmed = text == null ? "" : text.trim();
                    if (!trimmed.startsWith("↳")) text = "↳ " + trimmed;
                }

                JLabel l=new JLabel(text);
                l.setOpaque(true);
                l.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
                if(isSelected) {
                    l.setBackground(new Color(184,207,229));
                } else if (isSub) {
                    l.setBackground(new Color(245,245,245));
                    l.setFont(l.getFont().deriveFont(Font.ITALIC));
                } else if (row==0 && table.getRowSorter()==null) {
                    // Only highlight first row when preserving order (summary view)
                    l.setBackground(new Color(255,215,0,60));
                } else {
                    l.setBackground(Color.WHITE);
                }
                l.setHorizontalAlignment(column==0||column>=2?JLabel.CENTER:JLabel.LEFT);
                return l;
            }
        });
    }

    private JPanel createStatisticsPanel(){
        JPanel p=new JPanel();
        p.setBorder(BorderFactory.createTitledBorder("Statistiken"));
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        if(!results.isEmpty()){
            BenchmarkResult best=results.get(0);
            BenchmarkResult worst=results.get(results.size()-1);
            double improvement=best.coverageRate-worst.coverageRate;
            statisticsLabel=new JLabel(String.format("<html><b>Beste Deckung:</b> %.2f%% (%s) | <b>Schlechteste:</b> %.2f%% (%s) | Δ %.2f Pp | Läufe: %d</html>",
                    best.coverageRate,best.algorithmName,worst.coverageRate,worst.algorithmName,improvement,results.size()));
            p.add(Box.createVerticalStrut(4));
            p.add(statisticsLabel);
            if(!jobListInfo.isEmpty()) p.add(new JLabel("Jobliste: "+jobListInfo));
        }
        return p;
    }

    private JPanel createButtonPanel(){
        JPanel p=new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Aktionen"));
        JButton btn=new JButton("Visualisieren");
        btn.addActionListener(e->visualizeSelectedSolution());
        // Nur in der Zusammenfassungsansicht: Button zum Akzeptieren der Lösung anzeigen
        boolean isBestPerSetView = jobListInfo != null && jobListInfo.contains("Die besten Ergebnisse pro Job-Set");
        JButton btnAccept = null;
        if (isBestPerSetView) {
            btnAccept = new JButton("Lösung akzeptieren");
            btnAccept.addActionListener(e -> acceptSelectedSolution());
        }
        p.add(Box.createVerticalStrut(8));
        p.add(btn);
        p.add(Box.createVerticalStrut(8));
        if (btnAccept != null) p.add(btnAccept);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void visualizeSelectedSolution(){
        int sel=table.getSelectedRow();
        if(sel<0){JOptionPane.showMessageDialog(this,"Bitte Zeile auswählen","Hinweis",JOptionPane.INFORMATION_MESSAGE);return;}
        int modelRow=table.convertRowIndexToModel(sel);
        if(modelRow<0 || modelRow>=results.size()) return;
        
        BenchmarkResult selected = results.get(modelRow);
        if(selected == null) return;
        
        // Prüfe, ob es eine Unterzeile ist - wenn ja, springe zur Hauptzeile
        if(selected.isSubRow) {
            int mainRow = modelRow - 1;
            while (mainRow >= 0 && results.get(mainRow) != null && results.get(mainRow).isSubRow) {
                mainRow--;
            }
            if(mainRow >= 0) {
                modelRow = mainRow;
                selected = results.get(modelRow);
            }
        }
        
        // Sammle Hauptzeile und alle zugehörigen Unterzeilen
        java.util.List<BenchmarkResult> bundle = new java.util.ArrayList<>();
        bundle.add(selected);
        int idx = modelRow + 1;
        while (idx < results.size()) {
            BenchmarkResult r = results.get(idx);
            if (r == null || !r.isSubRow) break;
            bundle.add(r);
            idx++;
        }
        
        // Visualisiere alle gesammelten Ergebnisse (Hauptzeile + Unterzeilen)
        for(BenchmarkResult br : bundle) {
            showPlateVisualization(br);
        }
    }

    private void showPlateVisualization(BenchmarkResult r){
        String algoInfo = r.sortLabel!=null && !"-".equals(r.sortLabel)?("Sortierung: "+r.sortLabel):null;
        String mode = "cut1"; // Modus für interaktiven Schnitt-Button
        if(r.platesRefs!=null && !r.platesRefs.isEmpty()){
            for(int i=0;i<r.platesRefs.size();i++){
                Plate p=r.platesRefs.get(i);
                java.util.List<?> free = (r.platesFreeRects!=null && i<r.platesFreeRects.size())? r.platesFreeRects.get(i):null;
                String title=r.algorithmName+" | Platte "+(i+1);
                // Konsole: Schnitte und Restplatten für diese Ursprungplatte ausgeben
                try { Controller.printCutsAndIntersections(p, true); } catch (Throwable t) { t.printStackTrace(); }
                PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(p,mode,free,title,algoInfo,jobListInfo);
            }
            return;
        }
        // Einzelfall: direkte Platte
        try { Controller.printCutsAndIntersections(r.plate, true); } catch (Throwable t) { t.printStackTrace(); }
        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(r.plate,mode,r.specificFreeRects,r.algorithmName,algoInfo,jobListInfo);
    }

    // Akzeptiert die ausgewählte Hauptzeile (mitsamt ihren Unterzeilen) als Lösung und informiert den Nutzer.
    private void acceptSelectedSolution() {
        try {
            acceptSelectedSolutionInternal();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Fehler beim Erstellen der JSON-Datei: " + ex.getMessage(), 
                "Fehler", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void acceptSelectedSolutionInternal() throws java.io.IOException {
        int viewRow = table.getSelectedRow();
        if(viewRow<0){
            JOptionPane.showMessageDialog(this,"Bitte Zeile auswählen","Hinweis",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.getRowSorter()==null ? viewRow : table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= results.size()) return;
        BenchmarkResult main = results.get(modelRow);
        if (main == null) return;
        if (main.isSubRow) {
            JOptionPane.showMessageDialog(this,"Unterzeilen dürfen nicht ausgewählt werden.","Hinweis",JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Sammle Unterzeilen bis zur nächsten Hauptzeile
        java.util.List<BenchmarkResult> bundle = new java.util.ArrayList<>();
        bundle.add(main);
        int idx = modelRow + 1;
        while (idx < results.size()) {
            BenchmarkResult r = results.get(idx);
            if (r == null || !r.isSubRow) break;
            bundle.add(r);
            idx++;
        }

        // Sammle alle Platten für die JSON-Ausgabe
        java.util.List<Plate> allPlates = new java.util.ArrayList<>();
        for (BenchmarkResult br : bundle) {
            if (br.platesRefs != null && !br.platesRefs.isEmpty()) {
                allPlates.addAll(br.platesRefs);
            } else if (br.plate != null) {
                allPlates.add(br.plate);
            }
        }

        // JSON-Datei erstellen mit den Konsolenausgaben (Schnitte und Restplatten)
        String solutionLabel = (main.algorithmName == null ? "Lösung" : main.algorithmName);
        String jsonPath = "src/main/IOFiles/output.json";
        JsonOutputWriter.writePlatesToJson(solutionLabel, allPlates, jsonPath);
        System.out.println("\nLösung wurde in JSON-Datei gespeichert: " + jsonPath);

        // BMP-Export: Alle Platten als Bilder speichern
        String exportDir = "src/main/IOFiles";
        java.io.File dir = new java.io.File(exportDir);
        if (!dir.exists()) dir.mkdirs();

        int plateCounter = 0;
        for (BenchmarkResult br : bundle) {
            if (br.platesRefs != null && !br.platesRefs.isEmpty()) {
                for (int i = 0; i < br.platesRefs.size(); i++) {
                    Plate p = br.platesRefs.get(i);
                    java.util.List<?> free = (br.platesFreeRects!=null && i<br.platesFreeRects.size())? br.platesFreeRects.get(i):null;
                    String fileName = sanitize(solutionLabel) + "_plate" + (++plateCounter) + ".bmp";
                    String outPath = exportDir + "/" + fileName;
                    PlateVisualizer.savePlateAsBmp(p, "cut1", free, br.algorithmName, jobListInfo, outPath);
                }
            } else if (br.plate != null) {
                String fileName = sanitize(solutionLabel) + "_plate" + (++plateCounter) + ".bmp";
                String outPath = exportDir + "/" + fileName;
                PlateVisualizer.savePlateAsBmp(br.plate, "cut1", br.specificFreeRects, br.algorithmName, jobListInfo, outPath);
            }
        }
        System.out.println("BMP-Export abgeschlossen: " + plateCounter + " Datei(en) unter " + exportDir);

        // In SQLite speichern (alle freien Rechtecke als einzelne Zeilen)
       try {
            String dbPath = "C:\\Users\\cpaun\\VisualStudioProjects\\PlateOptimizer\\src\\main\\java\\org\\example\\Storage\\leftover_plates.sqlite";
            LeftoverPlatesDb db = new LeftoverPlatesDb(dbPath);
            for (BenchmarkResult br : bundle) {
                String sqliteSolutionLabel = (br.algorithmName==null?"(ohne Name)":br.algorithmName);
                if (br.platesRefs != null && !br.platesRefs.isEmpty()) {
                    for (int i = 0; i < br.platesRefs.size(); i++) {
                        java.util.List<?> frList = (br.platesFreeRects!=null && i<br.platesFreeRects.size()) ? br.platesFreeRects.get(i) : null;
                        if (frList == null) continue;
                        for (Object o : frList) {
                            if (o instanceof PlatePath.FreeRectangle) {
                                PlatePath.FreeRectangle fr = (PlatePath.FreeRectangle)o;
                                db.insertFreeRect(sqliteSolutionLabel, i, fr.x, fr.y, fr.width, fr.height);
                            } else {
                                Double x = tryGetDoubleField(o, "x");
                                Double y = tryGetDoubleField(o, "y");
                                Double w = tryGetDoubleField(o, "width");
                                Double h = tryGetDoubleField(o, "height");
                                if (x!=null && y!=null && w!=null && h!=null) db.insertFreeRect(sqliteSolutionLabel, i, x, y, w, h);
                            }
                        }
                    }
                } else if (br.plate != null && br.specificFreeRects != null) {
                    int i = 0;
                    for (PlatePath.FreeRectangle fr : br.specificFreeRects) {
                        db.insertFreeRect(sqliteSolutionLabel, i, fr.x, fr.y, fr.width, fr.height);
                    }
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Fehler beim Speichern in SQLite: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Meldung an den Nutzer
    String message = String.format("Die Lösung '%s' (inkl. %d Unterzeile(n)) wurde gespeichert:\n- JSON: output.json\n- BMP: IOFiles/exports/*.bmp\n- SQLite: leftover_plates.sqlite",
                main.algorithmName, Math.max(0, bundle.size()-1));
        JOptionPane.showMessageDialog(this, message, "Lösung akzeptiert", JOptionPane.INFORMATION_MESSAGE);
        
    }

    private static Double tryGetDoubleField(Object obj, String fieldName) {
        try {
            if (obj == null || fieldName == null || fieldName.isEmpty()) return null;
            java.lang.reflect.Field f = null;
            Class<?> c = obj.getClass();
            while (c != null) {
                try { f = c.getDeclaredField(fieldName); break; } catch (NoSuchFieldException ignore) { c = c.getSuperclass(); }
            }
            if (f == null) return null;
            f.setAccessible(true);
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number)v).doubleValue();
            if (v != null) return Double.parseDouble(String.valueOf(v));
        } catch (Exception ignore) {}
        return null;
    }

    // Dateinamen von unerlaubten Zeichen säubern
    private static String sanitize(String s) {
        if (s == null) return "solution";
    return s.replaceAll("[^a-zA-Z0-9-_\\. ]", "_").trim().replaceAll(" +", "_");
    }

    public static void showBenchmarkResults(java.util.List<BenchmarkResult> results,String jobListInfo){
        SwingUtilities.invokeLater(()->{
            BenchmarkVisualizer v=new BenchmarkVisualizer(results,jobListInfo);
            v.setVisible(true);
        });
    }
}