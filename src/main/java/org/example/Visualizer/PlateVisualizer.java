package org.example.Visualizer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import org.example.Main;
import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;
import org.example.DataClasses.MultiPlate_DataClasses; // neu
import org.example.SinglePlate.MaxRectBF_MultiPath;
import org.example.SinglePlate.MultiPlateMultiPath;

public class PlateVisualizer extends JPanel {
    Plate plate;
    Object algorithm;
    private final String mode;
    private List<?> specificFreeRects; // verallgemeinert (kann MaxRect oder MultiPlate enthalten)
    private String customAlgorithmInfo;
    private String jobListInfo;

    public PlateVisualizer(Plate plate, String mode, Object algorithm) { this(plate, mode, algorithm, null); }
    public PlateVisualizer(Plate plate, String mode, Object algorithm, String jobListInfo) { this(plate, mode, algorithm, null, jobListInfo, null); }
    public PlateVisualizer(Plate plate, String mode, Object algorithm, List<?> specificFreeRects, String jobListInfo, String customAlgorithmInfo) {
        this.plate = plate; this.mode = mode; this.algorithm = algorithm; this.specificFreeRects = specificFreeRects; this.customAlgorithmInfo = customAlgorithmInfo; this.jobListInfo = jobListInfo;
        int extraHeight = 240; if ("4".equals(mode)) extraHeight = 280; setPreferredSize(new Dimension((int)Math.round(plate.width) + 100, (int)Math.round(plate.height) + extraHeight + 80)); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g); Graphics2D g2d = (Graphics2D) g; int plateOffsetX = 50; int plateOffsetY = 50; g2d.translate(plateOffsetX, plateOffsetY);
        g2d.setColor(Color.LIGHT_GRAY); g2d.fillRect(0, 0, (int)Math.round(plate.width), (int)Math.round(plate.height));
        // Jobs
        for (Job job : plate.jobs) {
            g2d.setColor(job.rotated ? new Color(0,180,0,120) : new Color(0,0,255,120));
            g2d.fillRect((int)Math.round(job.x),(int)Math.round(job.y),(int)Math.round(job.width),(int)Math.round(job.height));
            g2d.setColor(Color.BLACK); g2d.setStroke(new BasicStroke(3f)); g2d.drawRect((int)Math.round(job.x),(int)Math.round(job.y),(int)Math.round(job.width),(int)Math.round(job.height));
            g2d.setColor(Color.BLACK); g2d.setFont(new Font("Arial", Font.BOLD, 16)); g2d.drawString("Job " + job.id, (int)Math.round(job.x)+5, (int)Math.round(job.y)+15);
            String dimensionText = (job.originalWidth>0 && job.originalHeight>0)? String.format("%.2fx%.2f (+%dmm)", job.originalWidth, job.originalHeight, Main.KERF_WIDTH): String.format("%.2fx%.2f", job.width, job.height);
            g2d.drawString(dimensionText,(int)Math.round(job.x)+5,(int)Math.round(job.y)+30); g2d.drawString("Order: " + job.placementOrder,(int)Math.round(job.x)+5,(int)Math.round(job.y)+45); if (job.splittingMethod!=null) g2d.drawString("Split: " + job.splittingMethod,(int)Math.round(job.x)+5,(int)Math.round(job.y)+60); if (job.rotated) g2d.drawString("(gedreht)",(int)Math.round(job.x)+5,(int)Math.round(job.y)+75);
        }
        // Freie Rechtecke (Modes 4,5,7)
        if ("4".equals(mode) || "5".equals(mode) || "7".equals(mode)) {
            if (specificFreeRects != null) { drawFreeRectangles(g2d, specificFreeRects); }
            else if (algorithm instanceof MaxRectBF_MultiPath && ("4".equals(mode) || "5".equals(mode))) { drawFreeRectangles(g2d, ((MaxRectBF_MultiPath)algorithm).getFreeRects()); }
        }
        g2d.setStroke(new BasicStroke(1f));
        int usedArea = 0; int placedJobsCount = 0; for (Job job : plate.jobs) if (job.placedOn != null) { usedArea += (job.originalWidth>0 && job.originalHeight>0)? job.originalWidth*job.originalHeight : job.width*job.height; placedJobsCount++; }
        double coverageRate = calculateCoverageRate(plate); g2d.setColor(Color.BLACK); g2d.setFont(new Font("Arial", Font.BOLD, 16)); int textY = (int)Math.round(plate.height) + 120;
        g2d.drawString("Platzierte Jobs: " + placedJobsCount,10,textY); g2d.drawString(String.format("Deckungsrate: %.2f%%", coverageRate),10,textY+25); g2d.drawString("Belegte Fläche: " + usedArea + " mm²",10,textY+50);
        String plateFormat = "Plattenformat: " + plate.name + " (" + String.format("%.1f", plate.width) + " x " + String.format("%.1f", plate.height) + " mm)"; g2d.drawString(plateFormat,10,textY+75);
        if (jobListInfo!=null && !jobListInfo.isEmpty()) g2d.drawString("Jobliste: " + jobListInfo,10,textY+100);
        if (("4".equals(mode) || "5".equals(mode) || "7".equals(mode)) && customAlgorithmInfo!=null) g2d.drawString(customAlgorithmInfo,10,textY+125);
    }

    private void drawFreeRectangles(Graphics2D g2d, java.util.List<?> freeRects) {
        g2d.setStroke(new BasicStroke(3f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{6},0)); g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        for (int i=0;i<freeRects.size();i++) {
            Object rectObj = freeRects.get(i); double x,y,width,height; boolean match=false;
            if (rectObj instanceof MaxRectBF_MultiPath.FreeRectangle) { MaxRectBF_MultiPath.FreeRectangle r=(MaxRectBF_MultiPath.FreeRectangle)rectObj; x=r.x;y=r.y;width=r.width;height=r.height; match=true; }
            else if (rectObj instanceof MultiPlate_DataClasses.FreeRectangle) { MultiPlate_DataClasses.FreeRectangle r=(MultiPlate_DataClasses.FreeRectangle)rectObj; x=r.x;y=r.y;width=r.width;height=r.height; match=true; }
            else continue; if (!match) continue;
            g2d.setColor(new Color(255,0,0,50)); g2d.fillRect((int)Math.round(x),(int)Math.round(y),(int)Math.round(width),(int)Math.round(height));
            g2d.setColor(Color.RED); g2d.drawRect((int)Math.round(x),(int)Math.round(y),(int)Math.round(width),(int)Math.round(height)); g2d.setColor(Color.RED.darker()); g2d.setFont(new Font("Arial", Font.BOLD, 16)); g2d.drawString("F"+i,(int)Math.round(x)+5,(int)Math.round(y)+15); g2d.drawString(String.format("%.2fx%.2f",width,height),(int)Math.round(x)+5,(int)Math.round(y)+30);
        }
    }

    public static double calculateCoverageRate(Plate plate) { double totalPlateArea = plate.width*plate.height; double usedArea=0; for (Job job: plate.jobs) if (job.placedOn!=null) usedArea += (job.originalWidth>0 && job.originalHeight>0)? job.originalWidth*job.originalHeight: job.width*job.height; return usedArea/totalPlateArea*100; }

    public static void showPlate(Plate plate, String mode, Object algorithm) { showPlateWithSpecificFreeRectsAndTitleAndInfo(plate, mode, null, plate.name, null, null); }

    public static void showPlateWithSpecificFreeRectsAndTitleAndInfo(Plate plate, String mode, List<?> specificFreeRects, String customTitle, String algorithmInfo, String jobListInfo) {
        SwingUtilities.invokeLater(() -> { JFrame frame = new JFrame("Plate Visualizer - " + customTitle); frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            Object algorithmToUse = null; if (specificFreeRects == null) { if ("4".equals(mode)) algorithmToUse = new MaxRectBF_MultiPath(plate); }
            PlateVisualizer visualizer = new PlateVisualizer(plate, mode, algorithmToUse, specificFreeRects, jobListInfo, algorithmInfo);
            int extraHeight = 240; if ("4".equals(mode) && algorithmInfo != null) extraHeight = 280; visualizer.setPreferredSize(new Dimension((int)Math.round(plate.width)+100,(int)Math.round(plate.height)+extraHeight+80));
            frame.getContentPane().add(visualizer); frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true); }); }

    public static void showBenchmarkResults(MultiPlateMultiPath algo, String jobListInfo) { List<Plate> plates = algo.getPlates(); int plateCount = plates.size(); int pathIndex = 1; for (int i=0;i<plateCount;i++) { MaxRectBF_MultiPath multiPathAlgo = algo.getMultiPathAlgorithms().get(i); List<MaxRectBF_MultiPath.AlgorithmPath> paths = multiPathAlgo.getAllPaths(); if (paths.size()>pathIndex) { MaxRectBF_MultiPath.AlgorithmPath path = paths.get(pathIndex); String strategyCode = multiPathAlgo.getStrategyCodeForPath(path); String title = (strategyCode!=null && !strategyCode.isEmpty())? plates.get(i).name + " | Pfad: " + path.pathDescription + " ["+strategyCode+"]" : plates.get(i).name + " | Pfad: " + path.pathDescription; showPlateWithSpecificFreeRectsAndTitleAndInfo(path.plate, "5", path.freeRects, title, null, jobListInfo); double coverage = calculateCoverageRate(path.plate); System.out.printf("Platte %d, Pfad %s: Deckungsrate: %.2f%%, Platzierte Jobs: %d\n", (i+1), path.pathDescription, coverage, path.plate.jobs.size()); try { Thread.sleep(1000);} catch (InterruptedException e){ Thread.currentThread().interrupt(); } } } }

    // NEU: Visualisierung einzelner MultiPlate-Pfade
    public static void showMultiPlatePath(MultiPlate_DataClasses path, String titleSuffix) {
        showPlateWithSpecificFreeRectsAndTitleAndInfo(path.plate, "7", path.freeRects, "MultiPlate Pfad " + path.pathId + (titleSuffix==null?"":" "+titleSuffix), null, null);
    }
}