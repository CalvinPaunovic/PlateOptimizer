package org.example.Algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Comparator;

import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;


public class CutLineCalculator {
    public static class CutLine {
        public final int id;
        public final boolean vertical;
        public final double coord;
        public final double start;
        public final double end;
        public CutLine(int id, boolean vertical, double coord, double start, double end) {
            this.id = id; this.vertical = vertical; this.coord = coord; this.start = start; this.end = end;
        }
    }
    
    private static class SplitCandidate {
        boolean vertical;
        double coord;
        List<Job> leftJobs;
        List<Job> rightJobs;
        List<Job> topJobs;
        List<Job> bottomJobs;
    }

    private static final double EPS = 1e-6; // Tolerance for floating-point comparisons

    /**
     * Berechnet einen einzigen vollständigen Schnitt durch die gesamte Platte (entweder vertikal oder horizontal).
     * Gibt eine Liste mit genau einer CutLine zurück, die die Platte komplett teilt.
     * Der Schnitt wird mittig gesetzt oder an einer sinnvollen Stelle zwischen den Jobs.
     */
    public static List<CutLine> calculateCut(Plate plate) {
        List<CutLine> cuts = new ArrayList<>();
        if (plate == null) return cuts;
        // Collect placed jobs
        List<Job> jobs = new ArrayList<>();
        for (Job j : plate.jobs) if (j != null && j.placedOn != null) jobs.add(j);
        if (jobs.isEmpty()) return cuts;

        // Region for the whole plate
        Region root = new Region(0.0, 0.0, plate.width, plate.height, jobs);

        // 0) Try to cut out the largest free strip first
        SplitCandidate freeStrip = findLargestFreeStripCut(root);
        if (freeStrip != null) {
            if (freeStrip.vertical) {
                cuts.add(new CutLine(1, true, normalize(freeStrip.coord), 0.0, plate.height));
            } else {
                cuts.add(new CutLine(1, false, normalize(freeStrip.coord), 0.0, plate.width));
            }
            return cuts;
        }

        // 1) Edge-Peeling bevorzugen (dünnste Leiste vom Rand trennen)
        SplitCandidate peelV = findEdgePeelVertical(root);
        SplitCandidate peelH = findEdgePeelHorizontal(root);
        SplitCandidate chosen = null;
        if (peelV != null && peelH != null) {
            double widthV = Math.min(Math.abs(peelV.coord - root.x0), Math.abs(root.x1 - peelV.coord));
            double widthH = Math.min(Math.abs(peelH.coord - root.y0), Math.abs(root.y1 - peelH.coord));
            chosen = (widthV <= widthH) ? peelV : peelH;
        } else if (peelV != null) {
            chosen = peelV;
        } else if (peelH != null) {
            chosen = peelH;
        }

        // 2) Falls kein Edge-Peeling möglich, balancierten Schnitt wählen
        if (chosen == null) {
            SplitCandidate bestVert = findVerticalCut(root);
            SplitCandidate bestHoriz = findHorizontalCut(root);
            chosen = chooseBetter(root, bestVert, bestHoriz);
        }

        // 3) Wenn trotzdem keiner gefunden: keine Schnitte zurückgeben
        if (chosen == null) return cuts;

        // 4) Vollständige Schnittlinie erzeugen
        if (chosen.vertical) {
            cuts.add(new CutLine(1, true, normalize(chosen.coord), 0.0, plate.height));
        } else {
            cuts.add(new CutLine(1, false, normalize(chosen.coord), 0.0, plate.width));
        }
        return cuts;
    }

    // Wählt den Schnitt, der den größten freien Rand-Strip (links/rechts/oben/unten) abtrennt
    private static SplitCandidate findLargestFreeStripCut(Region r) {
        if (r.jobs == null || r.jobs.isEmpty()) return null;
        double minX = Double.POSITIVE_INFINITY, maxR = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxB = Double.NEGATIVE_INFINITY;
        // Find coordinates of each corner of all jobs
        for (Job j : r.jobs) {
            if (j.x < minX) minX = j.x;
            double right = j.x + j.width;
            if (right > maxR) maxR = right;
            if (j.y < minY) minY = j.y;
            double bottom = j.y + j.height;
            if (bottom > maxB) maxB = bottom;
        }

        SplitCandidate best = null;
        double bestArea = -1.0;

        // Left Strip: [r.x0, minX]
        if (minX > r.x0 + EPS && isVerticalLineClear(r.jobs, minX)) {
            double area = (minX - r.x0) * (r.y1 - r.y0);
            if (area > bestArea) {
                List<Job> left = new ArrayList<>(), right = new ArrayList<>();
                partitionVertical(r.jobs, minX, left, right);
                if (!right.isEmpty()) { // Restplatte enthält Jobs
                    SplitCandidate c = new SplitCandidate();
                    c.vertical = true; c.coord = minX; c.leftJobs = left; c.rightJobs = right;
                    best = c; bestArea = area;
                }
            }
        }

        // Rechter Strip: [maxR, r.x1]
        if (maxR < r.x1 - EPS && isVerticalLineClear(r.jobs, maxR)) {
            double area = (r.x1 - maxR) * (r.y1 - r.y0);
            if (area > bestArea) {
                List<Job> left = new ArrayList<>(), right = new ArrayList<>();
                partitionVertical(r.jobs, maxR, left, right);
                if (!left.isEmpty()) {
                    SplitCandidate c = new SplitCandidate();
                    c.vertical = true; c.coord = maxR; c.leftJobs = left; c.rightJobs = right;
                    best = c; bestArea = area;
                }
            }
        }

        // Oberer Strip: [r.y0, minY]
        if (minY > r.y0 + EPS && isHorizontalLineClear(r.jobs, minY)) {
            double area = (minY - r.y0) * (r.x1 - r.x0);
            if (area > bestArea) {
                List<Job> top = new ArrayList<>(), bottom = new ArrayList<>();
                partitionHorizontal(r.jobs, minY, top, bottom);
                if (!bottom.isEmpty()) {
                    SplitCandidate c = new SplitCandidate();
                    c.vertical = false; c.coord = minY; c.topJobs = top; c.bottomJobs = bottom;
                    best = c; bestArea = area;
                }
            }
        }

        // Unterer Strip: [maxB, r.y1]
        if (maxB < r.y1 - EPS && isHorizontalLineClear(r.jobs, maxB)) {
            double area = (r.y1 - maxB) * (r.x1 - r.x0);
            if (area > bestArea) {
                List<Job> top = new ArrayList<>(), bottom = new ArrayList<>();
                partitionHorizontal(r.jobs, maxB, top, bottom);
                if (!top.isEmpty()) {
                    SplitCandidate c = new SplitCandidate();
                    c.vertical = false; c.coord = maxB; c.topJobs = top; c.bottomJobs = bottom;
                    best = c; bestArea = area;
                }
            }
        }

        return best;
    }

    public static List<CutLine> calculateCutLinesForPlate(Plate plate) {
        return calculateCut(plate);
    }

    /**
     * Berechnet eine vollständige Sequenz an Schnitten, wobei jeder Schnitt einmal durch die gesamte Platte läuft
     * und keine Jobs schneidet. Es wird rekursiv gesplittet, bis Regionen nur noch 1 Job enthalten.
     */
    public static List<CutLine> calculateAllFullCuts(Plate plate) {
        List<CutLine> out = new ArrayList<>();
        if (plate == null) return out;
        List<Job> jobs = new ArrayList<>();
        for (Job j : plate.jobs) if (j != null && j.placedOn != null) jobs.add(j);
        if (jobs.isEmpty()) return out;

        Region root = new Region(0.0, 0.0, plate.width, plate.height, jobs);
        computeFullCutsRecursive(root, 0.0, 0.0, plate.width, plate.height, out);
        // IDs zuweisen
        List<CutLine> withIds = new ArrayList<>(out.size());
        int id = 1;
        for (CutLine c : out) withIds.add(new CutLine(id++, c.vertical, c.coord, c.start, c.end));
        return withIds;
    }

    private static void computeFullCutsRecursive(Region region, double gX0, double gY0, double gX1, double gY1, List<CutLine> out) {
        if (region == null || region.jobs == null || region.jobs.size() <= 1) return;

        // Priorität: größter freier Rand-Strip, dann Edge-Peeling, dann balanciert
        SplitCandidate chosen = findLargestFreeStripCut(region);
        if (chosen == null) {
            SplitCandidate peelV = findEdgePeelVertical(region);
            SplitCandidate peelH = findEdgePeelHorizontal(region);
            chosen = chooseBetter(region, peelV, peelH);
            if (chosen == null) {
                SplitCandidate bestV = findVerticalCut(region);
                SplitCandidate bestH = findHorizontalCut(region);
                chosen = chooseBetter(region, bestV, bestH);
            }
        }
        if (chosen == null) return;

        if (chosen.vertical) {
            out.add(new CutLine(0, true, normalize(chosen.coord), normalize(gY0), normalize(gY1)));
            Region left = region.subLeft(chosen.coord); left.jobs = new ArrayList<>(chosen.leftJobs);
            Region right = region.subRight(chosen.coord); right.jobs = new ArrayList<>(chosen.rightJobs);
            computeFullCutsRecursive(left, gX0, gY0, chosen.coord, gY1, out);
            computeFullCutsRecursive(right, chosen.coord, gY0, gX1, gY1, out);
        } else {
            out.add(new CutLine(0, false, normalize(chosen.coord), normalize(gX0), normalize(gX1)));
            Region top = region.subTop(chosen.coord); top.jobs = new ArrayList<>(chosen.topJobs);
            Region bottom = region.subBottom(chosen.coord); bottom.jobs = new ArrayList<>(chosen.bottomJobs);
            computeFullCutsRecursive(top, gX0, gY0, gX1, chosen.coord, out);
            computeFullCutsRecursive(bottom, gX0, chosen.coord, gX1, gY1, out);
        }
    }

    // Findet einen vertikalen Schnitt am Rand (Edge-Peeling)
    // Ein Job, der den linken oder rechten Rand berührt, wird abgeschnitten
    private static SplitCandidate findEdgePeelVertical(Region r) {
        List<SplitCandidate> kandidaten = new ArrayList<>();
        
        // Gehe durch alle Jobs in der Region
        for (int i = 0; i < r.jobs.size(); i++) {
            Job j = r.jobs.get(i);
            
            // Prüfe, ob der Job den linken Rand berührt
            boolean beruehrtLinks = Math.abs(j.x - r.x0) < EPS;
            // Prüfe, ob der Job den rechten Rand berührt
            boolean beruehrtRechts = Math.abs((j.x + j.width) - r.x1) < EPS;
            
            // Wenn der Job keinen Rand berührt, überspringe ihn
            if (!beruehrtLinks && !beruehrtRechts) {
                continue;
            }
            
            // Berechne die Schnittposition: rechter Rand des Jobs wenn links, linker Rand wenn rechts
            double xSchnitt = beruehrtLinks ? j.x + j.width : j.x;
            
            // Prüfe, ob der Schnitt innerhalb der Region liegt
            if (xSchnitt <= r.x0 + EPS || xSchnitt >= r.x1 - EPS) {
                continue;
            }
            
            // Prüfe, ob an dieser Position ein Schnitt möglich ist (keine anderen Jobs durchkreuzen)
            if (!isVerticalLineClear(r.jobs, xSchnitt)) {
                continue;
            }
            
            // Teile die Jobs in links und rechts
            List<Job> links = new ArrayList<>();
            List<Job> rechts = new ArrayList<>();
            partitionVertical(r.jobs, xSchnitt, links, rechts);
            
            // Beide Seiten müssen Jobs enthalten
            if (links.isEmpty() || rechts.isEmpty()) {
                continue;
            }
            
            // Erstelle einen Schnitt-Kandidaten
            SplitCandidate kandidat = new SplitCandidate();
            kandidat.vertical = true;
            kandidat.coord = xSchnitt;
            kandidat.leftJobs = links;
            kandidat.rightJobs = rechts;
            kandidaten.add(kandidat);
        }
        
        // Wenn keine Kandidaten gefunden wurden
        if (kandidaten.isEmpty()) {
            return null;
        }
        
        // Sortiere die Kandidaten: bevorzuge den dünnsten Streifen
        kandidaten.sort(new Comparator<SplitCandidate>() {
            public int compare(SplitCandidate a, SplitCandidate b) {
                // Berechne die Breite des abgeschnittenen Streifens
                double breiteA = Math.min(Math.abs(a.coord - r.x0), Math.abs(r.x1 - a.coord));
                double breiteB = Math.min(Math.abs(b.coord - r.x0), Math.abs(r.x1 - b.coord));
                
                // Vergleiche die Breiten
                int vergleich = Double.compare(breiteA, breiteB);
                if (vergleich != 0) {
                    return vergleich;
                }
                
                // Bei gleicher Breite: bevorzuge ausgewogenere Aufteilung
                int unterschiedA = Math.abs(a.leftJobs.size() - a.rightJobs.size());
                int unterschiedB = Math.abs(b.leftJobs.size() - b.rightJobs.size());
                return Integer.compare(unterschiedA, unterschiedB);
            }
        });
        
        // Gib den besten Kandidaten zurück
        return kandidaten.get(0);
    }

    // Findet einen horizontalen Schnitt am Rand (Edge-Peeling)
    // Ein Job, der den oberen oder unteren Rand berührt, wird abgeschnitten
    private static SplitCandidate findEdgePeelHorizontal(Region r) {
        List<SplitCandidate> kandidaten = new ArrayList<>();
        
        // Gehe durch alle Jobs in der Region
        for (int i = 0; i < r.jobs.size(); i++) {
            Job j = r.jobs.get(i);
            
            // Prüfe, ob der Job den oberen Rand berührt
            boolean beruehrtOben = Math.abs(j.y - r.y0) < EPS;
            // Prüfe, ob der Job den unteren Rand berührt
            boolean beruehrtUnten = Math.abs((j.y + j.height) - r.y1) < EPS;
            
            // Wenn der Job keinen Rand berührt, überspringe ihn
            if (!beruehrtOben && !beruehrtUnten) {
                continue;
            }
            
            // Berechne die Schnittposition: unterer Rand des Jobs wenn oben, oberer Rand wenn unten
            double ySchnitt = beruehrtOben ? j.y + j.height : j.y;
            
            // Prüfe, ob der Schnitt innerhalb der Region liegt
            if (ySchnitt <= r.y0 + EPS || ySchnitt >= r.y1 - EPS) {
                continue;
            }
            
            // Prüfe, ob an dieser Position ein Schnitt möglich ist (keine anderen Jobs durchkreuzen)
            if (!isHorizontalLineClear(r.jobs, ySchnitt)) {
                continue;
            }
            
            // Teile die Jobs in oben und unten
            List<Job> oben = new ArrayList<>();
            List<Job> unten = new ArrayList<>();
            partitionHorizontal(r.jobs, ySchnitt, oben, unten);
            
            // Beide Seiten müssen Jobs enthalten
            if (oben.isEmpty() || unten.isEmpty()) {
                continue;
            }
            
            // Erstelle einen Schnitt-Kandidaten
            SplitCandidate kandidat = new SplitCandidate();
            kandidat.vertical = false;
            kandidat.coord = ySchnitt;
            kandidat.topJobs = oben;
            kandidat.bottomJobs = unten;
            kandidaten.add(kandidat);
        }
        
        // Wenn keine Kandidaten gefunden wurden
        if (kandidaten.isEmpty()) {
            return null;
        }
        
        // Sortiere die Kandidaten: bevorzuge den dünnsten Streifen
        kandidaten.sort(new Comparator<SplitCandidate>() {
            public int compare(SplitCandidate a, SplitCandidate b) {
                // Berechne die Höhe des abgeschnittenen Streifens
                double hoeheA = Math.min(Math.abs(a.coord - r.y0), Math.abs(r.y1 - a.coord));
                double hoeheB = Math.min(Math.abs(b.coord - r.y0), Math.abs(r.y1 - b.coord));
                
                // Vergleiche die Höhen
                int vergleich = Double.compare(hoeheA, hoeheB);
                if (vergleich != 0) {
                    return vergleich;
                }
                
                // Bei gleicher Höhe: bevorzuge ausgewogenere Aufteilung
                int unterschiedA = Math.abs(a.topJobs.size() - a.bottomJobs.size());
                int unterschiedB = Math.abs(b.topJobs.size() - b.bottomJobs.size());
                return Integer.compare(unterschiedA, unterschiedB);
            }
        });
        
        // Gib den besten Kandidaten zurück
        return kandidaten.get(0);
    }

    private static SplitCandidate chooseBetter(Region r, SplitCandidate v, SplitCandidate h) {
        if (v == null) return h;
        if (h == null) return v;
        int diffV = Math.abs(v.leftJobs.size() - v.rightJobs.size());
        int diffH = Math.abs(h.topJobs.size() - h.bottomJobs.size());
        // prefer the more balanced split; tie-breaker: prefer vertical
        if (diffV < diffH) return v;
        if (diffH < diffV) return h;
        return v;
    }

    private static SplitCandidate findVerticalCut(Region r) {
        Set<Double> candidates = new TreeSet<>();
        for (Job j : r.jobs) {
            double xL = clampToRegion(j.x, r.x0, r.x1);
            double xR = clampToRegion(j.x + j.width, r.x0, r.x1);
            if (xL > r.x0 + EPS && xL < r.x1 - EPS) candidates.add(xL);
            if (xR > r.x0 + EPS && xR < r.x1 - EPS) candidates.add(xR);
        }
        SplitCandidate best = null;
        for (double x : candidates) {
            if (!isVerticalLineClear(r.jobs, x)) continue;
            List<Job> left = new ArrayList<>();
            List<Job> right = new ArrayList<>();
            partitionVertical(r.jobs, x, left, right);
            if (left.isEmpty() || right.isEmpty()) continue; // must split into two non-empty sets
            SplitCandidate cand = new SplitCandidate();
            cand.vertical = true; cand.coord = x; cand.leftJobs = left; cand.rightJobs = right;
            if (best == null || isMoreBalanced(best.leftJobs.size(), best.rightJobs.size(), left.size(), right.size())) best = cand;
        }
        return best;
    }

    private static SplitCandidate findHorizontalCut(Region r) {
        Set<Double> candidates = new TreeSet<>();
        for (Job j : r.jobs) {
            double yT = clampToRegion(j.y, r.y0, r.y1);
            double yB = clampToRegion(j.y + j.height, r.y0, r.y1);
            if (yT > r.y0 + EPS && yT < r.y1 - EPS) candidates.add(yT);
            if (yB > r.y0 + EPS && yB < r.y1 - EPS) candidates.add(yB);
        }
        SplitCandidate best = null;
        for (double y : candidates) {
            if (!isHorizontalLineClear(r.jobs, y)) continue;
            List<Job> top = new ArrayList<>();
            List<Job> bottom = new ArrayList<>();
            partitionHorizontal(r.jobs, y, top, bottom);
            if (top.isEmpty() || bottom.isEmpty()) continue;
            SplitCandidate cand = new SplitCandidate();
            cand.vertical = false; cand.coord = y; cand.topJobs = top; cand.bottomJobs = bottom;
            if (best == null || isMoreBalanced(best.topJobs.size(), best.bottomJobs.size(), top.size(), bottom.size())) best = cand;
        }
        return best;
    }

    private static boolean isMoreBalanced(int a, int b, int c, int d) {
        return Math.abs(c - d) < Math.abs(a - b);
    }

    private static void partitionVertical(List<Job> jobs, double x, List<Job> left, List<Job> right) {
        for (Job j : jobs) {
            double center = j.x + j.width * 0.5;
            if (center <= x) left.add(j); else right.add(j);
        }
    }

    private static void partitionHorizontal(List<Job> jobs, double y, List<Job> top, List<Job> bottom) {
        for (Job j : jobs) {
            double center = j.y + j.height * 0.5;
            if (center <= y) top.add(j); else bottom.add(j);
        }
    }

    private static boolean isVerticalLineClear(List<Job> jobs, double x) {
        for (Job j : jobs) {
            if (j.x + EPS < x && x < j.x + j.width - EPS) return false; // would cross interior
        }
        return true;
    }

    private static boolean isHorizontalLineClear(List<Job> jobs, double y) {
        for (Job j : jobs) {
            if (j.y + EPS < y && y < j.y + j.height - EPS) return false; // would cross interior
        }
        return true;
    }

    private static double clampToRegion(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    private static double normalize(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static class Region {
        final double x0, y0, x1, y1;
        List<Job> jobs;
        Region(double x0, double y0, double x1, double y1, List<Job> jobs) {
            this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1; this.jobs = new ArrayList<>(jobs);
        }
        Region subLeft(double x) { return new Region(x0, y0, x, y1, Collections.emptyList()); }
        Region subRight(double x) { return new Region(x, y0, x1, y1, Collections.emptyList()); }
        Region subTop(double y) { return new Region(x0, y0, x1, y, Collections.emptyList()); }
        Region subBottom(double y) { return new Region(x0, y, x1, y1, Collections.emptyList()); }
    }

    // Beschreibt eine Restplatte (Teilregion) nach vollständiger Schnittsequenz
    public static class ResidualPlate {
        public final double x0, y0, x1, y1;
        public final int jobCount; // Anzahl Jobs in dieser Region (0 oder 1 nach unserer Abbruchbedingung)
        public ResidualPlate(double x0, double y0, double x1, double y1, int jobCount) {
            this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1; this.jobCount = jobCount;
        }
        public double width() { return x1 - x0; }
        public double height() { return y1 - y0; }
        public double area() { return width() * height(); }
    }

    /**
     * Liefert die endgültigen Restplatten (rechteckige Teilbereiche), die nach Anwendung aller vollständigen Schnitte
     * entstehen würden. Schneidet rekursiv, bis Regionen <= 1 Job enthalten, und sammelt dann deren Bounds.
     */
    public static List<ResidualPlate> calculateResidualPlates(Plate plate) {
        List<ResidualPlate> out = new ArrayList<>();
        if (plate == null) return out;
        List<Job> jobs = new ArrayList<>();
        for (Job j : plate.jobs) if (j != null && j.placedOn != null) jobs.add(j);
        if (jobs.isEmpty()) {
            out.add(new ResidualPlate(0.0, 0.0, plate.width, plate.height, 0));
            return out;
        }
        Region root = new Region(0.0, 0.0, plate.width, plate.height, jobs);
        collectResidualRegions(root, 0.0, 0.0, plate.width, plate.height, out);
        return out;
    }

    private static void collectResidualRegions(Region region, double gX0, double gY0, double gX1, double gY1,
                                               List<ResidualPlate> out) {
        if (region == null || region.jobs == null || region.jobs.size() <= 1) {
            int count = (region == null || region.jobs == null) ? 0 : region.jobs.size();
            out.add(new ResidualPlate(normalize(gX0), normalize(gY0), normalize(gX1), normalize(gY1), count));
            return;
        }

        // gleiche Heuristik wie bei computeFullCutsRecursive
        SplitCandidate chosen = findLargestFreeStripCut(region);
        if (chosen == null) {
            SplitCandidate peelV = findEdgePeelVertical(region);
            SplitCandidate peelH = findEdgePeelHorizontal(region);
            chosen = chooseBetter(region, peelV, peelH);
            if (chosen == null) {
                SplitCandidate bestV = findVerticalCut(region);
                SplitCandidate bestH = findHorizontalCut(region);
                chosen = chooseBetter(region, bestV, bestH);
            }
        }
        if (chosen == null) {
            out.add(new ResidualPlate(normalize(gX0), normalize(gY0), normalize(gX1), normalize(gY1), region.jobs.size()));
            return;
        }

        if (chosen.vertical) {
            Region left = region.subLeft(chosen.coord); left.jobs = new ArrayList<>(chosen.leftJobs);
            Region right = region.subRight(chosen.coord); right.jobs = new ArrayList<>(chosen.rightJobs);
            collectResidualRegions(left, gX0, gY0, chosen.coord, gY1, out);
            collectResidualRegions(right, chosen.coord, gY0, gX1, gY1, out);
        } else {
            Region top = region.subTop(chosen.coord); top.jobs = new ArrayList<>(chosen.topJobs);
            Region bottom = region.subBottom(chosen.coord); bottom.jobs = new ArrayList<>(chosen.bottomJobs);
            collectResidualRegions(top, gX0, gY0, gX1, chosen.coord, out);
            collectResidualRegions(bottom, gX0, chosen.coord, gX1, gY1, out);
        }
    }
}
