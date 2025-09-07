package org.example.MultiPlateIndividual;

import org.example.DataClasses.Job;
import org.example.DataClasses.Plate;

public class CutLineCalculator {
    public static class CutLine {
        public final int id;
        public final boolean vertical; // true = vertical (x = const), false = horizontal (y = const)
        public final double coord;     // x for vertical, y for horizontal
        public final double start;     // start coordinate along the cut (y for vertical, x for horizontal)
        public final double end;       // end coordinate along the cut (y for vertical, x for horizontal)
        public CutLine(int id, boolean vertical, double coord, double start, double end) {
            this.id = id; this.vertical = vertical; this.coord = coord; this.start = start; this.end = end;
        }
    }

    private static final double EPS = 1e-6;

    public static java.util.List<CutLine> calculateCutLinesForPlate(Plate plate) {
        java.util.List<CutLine> cuts = new java.util.ArrayList<>();
        if (plate == null) return cuts;
        // Copy jobs into a local list for robustness
        java.util.List<Job> jobs = new java.util.ArrayList<>();
        for (Job j : plate.jobs) if (j != null && j.placedOn != null) jobs.add(j);
        if (jobs.isEmpty()) return cuts;

        Region root = new Region(0.0, 0.0, plate.width, plate.height, jobs);
        java.util.List<CutLine> seq = new java.util.ArrayList<>();
        computeGuillotineCutsRecursive(root, seq);
        // assign ids in order
        java.util.List<CutLine> finalCuts = new java.util.ArrayList<>();
        int id = 1;
        for (CutLine c : seq) finalCuts.add(new CutLine(id++, c.vertical, c.coord, c.start, c.end));
        return finalCuts;
    }

    private static void computeGuillotineCutsRecursive(Region region, java.util.List<CutLine> out) {
        if (region == null || region.jobs.isEmpty()) return;
        // Wenn genau ein Job in der Region liegt, entlang seiner Kanten voll-length Schnitte emittieren
        if (region.jobs.size() == 1) {
            emitIsolationCuts(region, out);
            return;
        }
        if (region.jobs.size() <= 1) return; // safety

        // 1) Prefer edge-peeling cuts: trim jobs (or strips) from the outside first
        SplitCandidate peelV = findEdgePeelVertical(region);
        SplitCandidate peelH = findEdgePeelHorizontal(region);
        SplitCandidate chosenPeel = null;
        if (peelV != null && peelH != null) {
            // choose the thinner strip to peel
            double widthV = Math.min(Math.abs(peelV.coord - region.x0), Math.abs(region.x1 - peelV.coord));
            double widthH = Math.min(Math.abs(peelH.coord - region.y0), Math.abs(region.y1 - peelH.coord));
            chosenPeel = (widthV <= widthH) ? peelV : peelH;
        } else if (peelV != null) {
            chosenPeel = peelV;
        } else if (peelH != null) {
            chosenPeel = peelH;
        }

        if (chosenPeel != null) {
            if (chosenPeel.vertical) {
                out.add(new CutLine(0, true, normalize(chosenPeel.coord), normalize(region.y0), normalize(region.y1)));
                Region left = region.subLeft(chosenPeel.coord); left.jobs = new java.util.ArrayList<>(chosenPeel.leftJobs);
                Region right = region.subRight(chosenPeel.coord); right.jobs = new java.util.ArrayList<>(chosenPeel.rightJobs);
                computeGuillotineCutsRecursive(left, out);
                computeGuillotineCutsRecursive(right, out);
            } else {
                out.add(new CutLine(0, false, normalize(chosenPeel.coord), normalize(region.x0), normalize(region.x1)));
                Region top = region.subTop(chosenPeel.coord); top.jobs = new java.util.ArrayList<>(chosenPeel.topJobs);
                Region bottom = region.subBottom(chosenPeel.coord); bottom.jobs = new java.util.ArrayList<>(chosenPeel.bottomJobs);
                computeGuillotineCutsRecursive(top, out);
                computeGuillotineCutsRecursive(bottom, out);
            }
            return;
        }

        // 2) Fallback: balanced split (previous behavior)
        SplitCandidate bestVert = findVerticalCut(region);
        SplitCandidate bestHoriz = findHorizontalCut(region);
        SplitCandidate chosen = chooseBetter(region, bestVert, bestHoriz);
        if (chosen == null) return; // cannot split

        if (chosen.vertical) {
            out.add(new CutLine(0, true, normalize(chosen.coord), normalize(region.y0), normalize(region.y1)));
            Region left = region.subLeft(chosen.coord); left.jobs = new java.util.ArrayList<>(chosen.leftJobs);
            Region right = region.subRight(chosen.coord); right.jobs = new java.util.ArrayList<>(chosen.rightJobs);
            computeGuillotineCutsRecursive(left, out);
            computeGuillotineCutsRecursive(right, out);
        } else {
            out.add(new CutLine(0, false, normalize(chosen.coord), normalize(region.x0), normalize(region.x1)));
            Region top = region.subTop(chosen.coord); top.jobs = new java.util.ArrayList<>(chosen.topJobs);
            Region bottom = region.subBottom(chosen.coord); bottom.jobs = new java.util.ArrayList<>(chosen.bottomJobs);
            computeGuillotineCutsRecursive(top, out);
            computeGuillotineCutsRecursive(bottom, out);
        }
    }

    // Emittiert Vollschnitte entlang der Job-Kanten, um das Einzelteil vollständig herauszutrennen
    private static void emitIsolationCuts(Region region, java.util.List<CutLine> out) {
        Job j = region.jobs.get(0);
        double left = j.x;
        double right = j.x + j.width;
        double top = j.y;
        double bottom = j.y + j.height;

        // Vertikale Schnitte links/rechts wenn noch Rand vorhanden ist
        if (left > region.x0 + EPS) {
            out.add(new CutLine(0, true, normalize(left), normalize(region.y0), normalize(region.y1)));
        }
        if (right < region.x1 - EPS) {
            out.add(new CutLine(0, true, normalize(right), normalize(region.y0), normalize(region.y1)));
        }
        // Horizontale Schnitte oben/unten wenn noch Rand vorhanden ist
        if (top > region.y0 + EPS) {
            out.add(new CutLine(0, false, normalize(top), normalize(region.x0), normalize(region.x1)));
        }
        if (bottom < region.y1 - EPS) {
            out.add(new CutLine(0, false, normalize(bottom), normalize(region.x0), normalize(region.x1)));
        }
    }

    // Prefer edge-peeling vertical cut: if a job touches left (x0) cut at its right edge; if touches right (x1) cut at its left edge
    private static SplitCandidate findEdgePeelVertical(Region r) {
        java.util.List<SplitCandidate> candidates = new java.util.ArrayList<>();
        for (Job j : r.jobs) {
            boolean touchesLeft = Math.abs(j.x - r.x0) < EPS;
            boolean touchesRight = Math.abs((j.x + j.width) - r.x1) < EPS;
            if (!touchesLeft && !touchesRight) continue;
            double xCut = touchesLeft ? j.x + j.width : j.x;
            if (xCut <= r.x0 + EPS || xCut >= r.x1 - EPS) continue;
            if (!isVerticalLineClear(r.jobs, xCut)) continue;
            java.util.List<Job> left = new java.util.ArrayList<>();
            java.util.List<Job> right = new java.util.ArrayList<>();
            partitionVertical(r.jobs, xCut, left, right);
            if (left.isEmpty() || right.isEmpty()) continue; // must split
            SplitCandidate sc = new SplitCandidate();
            sc.vertical = true; sc.coord = xCut; sc.leftJobs = left; sc.rightJobs = right;
            candidates.add(sc);
        }
        if (candidates.isEmpty()) return null;
        // choose the minimum strip thickness relative to region borders
        candidates.sort((a,b)->{
            double wa = Math.min(Math.abs(a.coord - r.x0), Math.abs(r.x1 - a.coord));
            double wb = Math.min(Math.abs(b.coord - r.x0), Math.abs(r.x1 - b.coord));
            int cmp = Double.compare(wa, wb);
            if (cmp!=0) return cmp;
            int diffA = Math.abs(a.leftJobs.size() - a.rightJobs.size());
            int diffB = Math.abs(b.leftJobs.size() - b.rightJobs.size());
            return Integer.compare(diffA, diffB); // tie-breaker: more balanced
        });
        return candidates.get(0);
    }

    // Prefer edge-peeling horizontal cut: if a job touches top (y0) cut at its bottom; if touches bottom (y1) cut at its top
    private static SplitCandidate findEdgePeelHorizontal(Region r) {
        java.util.List<SplitCandidate> candidates = new java.util.ArrayList<>();
        for (Job j : r.jobs) {
            boolean touchesTop = Math.abs(j.y - r.y0) < EPS;
            boolean touchesBottom = Math.abs((j.y + j.height) - r.y1) < EPS;
            if (!touchesTop && !touchesBottom) continue;
            double yCut = touchesTop ? j.y + j.height : j.y;
            if (yCut <= r.y0 + EPS || yCut >= r.y1 - EPS) continue;
            if (!isHorizontalLineClear(r.jobs, yCut)) continue;
            java.util.List<Job> top = new java.util.ArrayList<>();
            java.util.List<Job> bottom = new java.util.ArrayList<>();
            partitionHorizontal(r.jobs, yCut, top, bottom);
            if (top.isEmpty() || bottom.isEmpty()) continue;
            SplitCandidate sc = new SplitCandidate();
            sc.vertical = false; sc.coord = yCut; sc.topJobs = top; sc.bottomJobs = bottom;
            candidates.add(sc);
        }
        if (candidates.isEmpty()) return null;
        candidates.sort((a,b)->{
            double ha = Math.min(Math.abs(a.coord - r.y0), Math.abs(r.y1 - a.coord));
            double hb = Math.min(Math.abs(b.coord - r.y0), Math.abs(r.y1 - b.coord));
            int cmp = Double.compare(ha, hb);
            if (cmp!=0) return cmp;
            int diffA = Math.abs(a.topJobs.size() - a.bottomJobs.size());
            int diffB = Math.abs(b.topJobs.size() - b.bottomJobs.size());
            return Integer.compare(diffA, diffB);
        });
        return candidates.get(0);
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
        java.util.Set<Double> candidates = new java.util.TreeSet<>();
        for (Job j : r.jobs) {
            double xL = clampToRegion(j.x, r.x0, r.x1);
            double xR = clampToRegion(j.x + j.width, r.x0, r.x1);
            if (xL > r.x0 + EPS && xL < r.x1 - EPS) candidates.add(xL);
            if (xR > r.x0 + EPS && xR < r.x1 - EPS) candidates.add(xR);
        }
        SplitCandidate best = null;
        for (double x : candidates) {
            if (!isVerticalLineClear(r.jobs, x)) continue;
            java.util.List<Job> left = new java.util.ArrayList<>();
            java.util.List<Job> right = new java.util.ArrayList<>();
            partitionVertical(r.jobs, x, left, right);
            if (left.isEmpty() || right.isEmpty()) continue; // must split into two non-empty sets
            SplitCandidate cand = new SplitCandidate();
            cand.vertical = true; cand.coord = x; cand.leftJobs = left; cand.rightJobs = right;
            if (best == null || isMoreBalanced(best.leftJobs.size(), best.rightJobs.size(), left.size(), right.size())) best = cand;
        }
        return best;
    }

    private static SplitCandidate findHorizontalCut(Region r) {
        java.util.Set<Double> candidates = new java.util.TreeSet<>();
        for (Job j : r.jobs) {
            double yT = clampToRegion(j.y, r.y0, r.y1);
            double yB = clampToRegion(j.y + j.height, r.y0, r.y1);
            if (yT > r.y0 + EPS && yT < r.y1 - EPS) candidates.add(yT);
            if (yB > r.y0 + EPS && yB < r.y1 - EPS) candidates.add(yB);
        }
        SplitCandidate best = null;
        for (double y : candidates) {
            if (!isHorizontalLineClear(r.jobs, y)) continue;
            java.util.List<Job> top = new java.util.ArrayList<>();
            java.util.List<Job> bottom = new java.util.ArrayList<>();
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

    private static void partitionVertical(java.util.List<Job> jobs, double x, java.util.List<Job> left, java.util.List<Job> right) {
        for (Job j : jobs) {
            double center = j.x + j.width * 0.5;
            if (center <= x) left.add(j); else right.add(j);
        }
    }

    private static void partitionHorizontal(java.util.List<Job> jobs, double y, java.util.List<Job> top, java.util.List<Job> bottom) {
        for (Job j : jobs) {
            double center = j.y + j.height * 0.5;
            if (center <= y) top.add(j); else bottom.add(j);
        }
    }

    private static boolean isVerticalLineClear(java.util.List<Job> jobs, double x) {
        for (Job j : jobs) {
            if (j.x + EPS < x && x < j.x + j.width - EPS) return false; // would cross interior
        }
        return true;
    }

    private static boolean isHorizontalLineClear(java.util.List<Job> jobs, double y) {
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
        java.util.List<Job> jobs;
        Region(double x0, double y0, double x1, double y1, java.util.List<Job> jobs) {
            this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1; this.jobs = new java.util.ArrayList<>(jobs);
        }
        Region subLeft(double x) { return new Region(x0, y0, x, y1, java.util.Collections.emptyList()); }
        Region subRight(double x) { return new Region(x, y0, x1, y1, java.util.Collections.emptyList()); }
        Region subTop(double y) { return new Region(x0, y0, x1, y, java.util.Collections.emptyList()); }
        Region subBottom(double y) { return new Region(x0, y, x1, y1, java.util.Collections.emptyList()); }
    }

    private static class SplitCandidate {
        boolean vertical;
        double coord;
        java.util.List<Job> leftJobs;
        java.util.List<Job> rightJobs;
        java.util.List<Job> topJobs;
        java.util.List<Job> bottomJobs;
    }
}
