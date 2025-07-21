package org.example.Visualizer;

import org.example.Algorithms.MultiPlateMultiPath;
import org.example.Algorithms.MaxRectBF_MultiPath;
import org.example.DataClasses.Plate;

import java.util.List;

public class MultiPlateMultiPathVisualizer {

    /**
     * Visualisiert nur Pfad 1 jeder Platte und gibt die wichtigsten Kennzahlen auf der Konsole aus.
     */
    public static void showBenchmarkResults(MultiPlateMultiPath algo, String jobListInfo) {
        List<Plate> plates = algo.getPlates();
        int plateCount = plates.size();

        int pathIndex = org.example.Main.DEBUG_MultiPlateMultiPath_PathIndex;

        for (int i = 0; i < plateCount; i++) {
            MaxRectBF_MultiPath multiPathAlgo = algo.getMultiPathAlgorithms().get(i);
            List<MaxRectBF_MultiPath.AlgorithmPath> paths = multiPathAlgo.getAllPaths();

            if (paths.size() > pathIndex) {
                MaxRectBF_MultiPath.AlgorithmPath path = paths.get(pathIndex); // Visualisiere den ausgewählten Pfad jeder Platte
                String title = plates.get(i).name + " | Pfad: " + path.pathDescription;
                PlateVisualizer.showPlateWithSpecificFreeRectsAndTitleAndInfo(
                    path.plate, "5", path.freeRects, title, null, jobListInfo
                );
                double coverage = PlateVisualizer.calculateCoverageRate(path.plate);
                System.out.printf("Platte %d, Pfad %s: Deckungsrate: %.2f%%, Platzierte Jobs: %d\n",
                    (i + 1), path.pathDescription, coverage, path.plate.jobs.size());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}