package org.example.Visualizer;

import org.example.DataClasses.Plate;
import org.example.DataClasses.PlatePath;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
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

        public BenchmarkResult(String algorithmName, Plate plate, int placedJobs, double coverageRate, int totalJobs) {
            this(algorithmName, plate, null, placedJobs, coverageRate, totalJobs, null);
        }

        public BenchmarkResult(String algorithmName, Plate plate, int placedJobs, double coverageRate, int totalJobs, List<PlatePath.FreeRectangle> specificFreeRects) {
            this(algorithmName, plate, null, placedJobs, coverageRate, totalJobs, specificFreeRects);
        }

        public BenchmarkResult(String algorithmName, Plate plate, Object algorithm, int placedJobs, double coverageRate, int totalJobs) {
            this(algorithmName, plate, algorithm, placedJobs, coverageRate, totalJobs, null);
        }

        public BenchmarkResult(String algorithmName, Plate plate, Object algorithm, int placedJobs, double coverageRate, int totalJobs, List<PlatePath.FreeRectangle> specificFreeRects) {
            this.algorithmName = algorithmName;
            this.plate = plate;
            this.algorithm = algorithm;
            this.placedJobs = placedJobs;
            this.coverageRate = coverageRate;
            this.totalJobs = totalJobs;
            this.specificFreeRects = specificFreeRects;
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

        String[] columns = {"Rang","Algorithmus","Platzierte Jobs","Gesamt Jobs","Erfolgsrate","Deckungsrate"};
        DefaultTableModel model = new DefaultTableModel(columns,0){
            @Override public boolean isCellEditable(int r,int c){return false;}
            @Override public Class<?> getColumnClass(int col){
                if(col==0||col==2||col==3) return Integer.class;
                if(col==4||col==5) return Double.class;
                return String.class;
            }
        };

        for(int i=0;i<results.size();i++){
            BenchmarkResult r=results.get(i);
            double success = r.totalJobs==0?0.0:(double)r.placedJobs/r.totalJobs*100.0;
            model.addRow(new Object[]{i+1,r.algorithmName,r.placedJobs,r.totalJobs,success,r.coverageRate});
        }

        table = new JTable(model);
        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.toggleSortOrder(4); // sort by Erfolgsrate
        customizeTable();
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void customizeTable(){
        table.setFont(new Font("Arial",Font.PLAIN,12));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Arial",Font.BOLD,12));
        table.setDefaultRenderer(Object.class, new TableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column){
                String text;
                if(value instanceof Number && (column==4||column==5)) text=String.format("%.2f%%", ((Number)value).doubleValue());
                else text=value==null?"-":value.toString();
                JLabel l=new JLabel(text);
                l.setOpaque(true);
                l.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
                if(isSelected) l.setBackground(new Color(184,207,229)); else if(row==0) l.setBackground(new Color(255,215,0,60)); else l.setBackground(Color.WHITE);
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
        p.add(Box.createVerticalStrut(8));
        p.add(btn);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void visualizeSelectedSolution(){
        int sel=table.getSelectedRow();
        if(sel<0){JOptionPane.showMessageDialog(this,"Bitte Zeile auswählen","Hinweis",JOptionPane.INFORMATION_MESSAGE);return;}
        int modelRow=table.convertRowIndexToModel(sel);
        if(modelRow>=0 && modelRow<results.size()) showPlateVisualization(results.get(modelRow));
    }

    private void showPlateVisualization(BenchmarkResult r){
        String algoInfo = r.sortLabel!=null && !"-".equals(r.sortLabel)?("Sortierung: "+r.sortLabel):null;
        if(r.platesRefs!=null && !r.platesRefs.isEmpty()){
            for(int i=0;i<r.platesRefs.size();i++){
                Plate p=r.platesRefs.get(i);
                java.util.List<?> free = (r.platesFreeRects!=null && i<r.platesFreeRects.size())? r.platesFreeRects.get(i):null;
                String title=r.algorithmName+" | Platte "+(i+1);
                PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(p,"9",free,title,algoInfo,jobListInfo);
            }
            return;
        }
        PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(r.plate,"9",r.specificFreeRects,r.algorithmName,algoInfo,jobListInfo);
    }

    public static void showBenchmarkResults(java.util.List<BenchmarkResult> results,String jobListInfo){
        SwingUtilities.invokeLater(()->{
            BenchmarkVisualizer v=new BenchmarkVisualizer(results,jobListInfo);
            v.setVisible(true);
        });
    }
}