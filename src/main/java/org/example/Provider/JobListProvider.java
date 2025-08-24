package org.example.Provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.example.DataClasses.Job;

public class JobListProvider {

    public static class NamedJobList {
        public final int id;
        public final String name;
        public final String description;
        public final List<Job> jobs;

        public NamedJobList(int id, String name, String description, List<Job> jobs) {
            this.id = id;
            this.name = name;
            this.description = description == null ? "" : description;
            this.jobs = jobs;
        }
    }

    public static NamedJobList getStandardJobList() {
        // Fläche Platte: 625950
        // Jobs: 402*480 + 305*222 + 220*573 + 205*153 + 243*188 + 243*188 + 205*153
        // = 192960 + 67610 + 126060 + 31365 + 45684 + 45684 + 31365 = 530728
        // Deckungsrate: 530728 / 625950 ≈ 84.79%
        // Kleinster Job Fläche: 31365 (Job 3 und 6), Größter Job Fläche: 192960 (Job 0)
        // Kleinste Kante: 153 (Job 3 und 6), Größte Kante: 573 (Job 2)
        // Nach Fläche sortiert (absteigend):
        // 0: 402*480 = 192960
        // 2: 220*573 = 126060
        // 1: 305*222 = 67610
        // 4: 243*188 = 45684
        // 5: 243*188 = 45684
        // 3: 205*153 = 31365
        // 6: 205*153 = 31365
        return new NamedJobList(1, "Standardliste", "Aus der Aufgabenstellung", Arrays.asList(
            new Job(0, 402, 480),
            new Job(1, 305, 222),
            new Job(2, 220, 573),
            new Job(3, 205, 153),
            new Job(4, 243, 188),
            new Job(5, 243, 188),
            new Job(6, 205, 153)
        ));
    }

    public static NamedJobList getSmallJobList() {
        // Fläche Platte: 963*650=625950
        // Jobs: (100*150)+(120*130)+(90*110) = 15000 + 15600 + 9900 = 40500
        // Deckungsrate: 40500 / 625950 ≈ 6.47%
        // Kleinster Job Fläche: 9900 (Job 2), Größter Job Fläche: 15600 (Job 1)
        // Kleinste Kante: 90 (Job 2), Größte Kante: 150 (Job 0)
        return new NamedJobList(2, "Kleine Liste", "3 Mini-Jobs zum schnellen Testen", Arrays.asList(
            new Job(0, 100, 150),
            new Job(1, 120, 130),
            new Job(2, 90, 110)
        ));
    }

    public static NamedJobList getMediumJobList() {
        // Fläche Platte: 625950
        // Jobs: 200*300 + 250*350 + 220*330 + 240*310 = 60000 + 87500 + 72600 + 74400 = 294500
        // Deckungsrate: 294500 / 625950 ≈ 47.06%
        // Kleinster Job Fläche: 60000 (Job 0), Größter Job Fläche: 87500 (Job 1)
        // Kleinste Kante: 200 (Job 0), Größte Kante: 350 (Job 1)
        return new NamedJobList(3, "Mittlere Liste", "4 mittelgroße Jobs, mittlere Auslastung", Arrays.asList(
            new Job(0, 200, 300),
            new Job(1, 250, 350),
            new Job(2, 220, 330),
            new Job(3, 240, 310)
        ));
    }

    public static NamedJobList getLargeJobList() {
        // Fläche Platte: 625950
        // Jobs: 400*500 + 450*550 + 420*530 + 440*510 + 460*520
        // = 200000 + 247500 + 222600 + 224400 + 239200 = 1137700
        // Deckungsrate: 1137700 / 625950 ≈ 181.74% (Jobs passen nicht zusammen)
        // Kleinster Job Fläche: 200000 (Job 0), Größter Job Fläche: 247500 (Job 1)
        // Kleinste Kante: 400 (Job 0), Größte Kante: 550 (Job 1)
        return new NamedJobList(4, "Große Liste", "5 große Paneele, deutlich zu groß für 1 Platte", Arrays.asList(
            new Job(0, 400, 500),
            new Job(1, 450, 550),
            new Job(2, 420, 530),
            new Job(3, 440, 510),
            new Job(4, 460, 520)
        ));
    }

    public static NamedJobList getAllSameSizeList() {
        // Fläche Platte: 625950
        // Jobs: 100 * (100*100) = 100 * 10000 = 1,000,000
        // Deckungsrate: 1,000,000 / 625950 ≈ 159.8%
        // Kleinster Job Fläche = Größter Job Fläche = 10000 (alle)
        // Kleinste Kante = Größte Kante = 100 (alle)
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            jobs.add(new Job(i, 100, 100));
        }
        return new NamedJobList(5, "Alle Jobs gleich groß (100x)", "100 identische Quadrate (100x100)", jobs);
    }

    public static NamedJobList getDuplicateJobsList() {
        // Fläche Platte: 625950
        // Jobs: 5 * (200*200) = 5 * 40000 = 200000
        // Deckungsrate: 200000 / 625950 ≈ 31.96%
        // Kleinster Job Fläche = Größter Job Fläche = 40000 (alle)
        // Kleinste Kante = Größte Kante = 200 (alle)
        return new NamedJobList(6, "Doppelte Jobs", "5 identische Quadrate (200x200)", Arrays.asList(
            new Job(0, 200, 200),
            new Job(1, 200, 200),
            new Job(2, 200, 200),
            new Job(3, 200, 200),
            new Job(4, 200, 200)
        ));
    }

    public static NamedJobList getTooLargeJobsList() {
        // Fläche Platte: 625950
        // Jobs: 2000*2000 + 963*700 + 1200*650 + 964*651
        // = 4,000,000 + 674,100 + 780,000 + 627,564 = 6,081,664
        // Deckungsrate: 6,081,664 / 625,950 ≈ 971.94% (viel zu groß)
        // Kleinster Job Fläche: 627,564 (Job 3), Größter Job Fläche: 4,000,000 (Job 0)
        // Kleinste Kante: 651 (Job 3), Größte Kante: 2000 (Job 0)
        return new NamedJobList(7, "Zu große Jobs", "4 Jobs, die die Platte(n) massiv überfüllen", Arrays.asList(
            new Job(0, 2000, 2000),
            new Job(1, 963, 700),
            new Job(2, 1200, 650),
            new Job(3, 964, 651)
        ));
    }

    public static NamedJobList getVerySmallJobsList() {
        // Fläche Platte: 625950
        // Jobs: 1*1 + 2*2 + 3*3 + 4*4 + 5*5 + 1*1 + 2*2
        // = 1 + 4 + 9 + 16 + 25 + 1 + 4 = 60
        // Deckungsrate: 60 / 625950 ≈ 0.0096%
        // Kleinster Job Fläche: 1 (Job 0 and 5), Größter Job Fläche: 25 (Job 4)
        // Kleinste Kante: 1 (Job 0 and 5), Größte Kante: 5 (Job 4)
        return new NamedJobList(8, "Sehr kleine Jobs", "Viele winzige Quadrate zum Stresstest", Arrays.asList(
            new Job(0, 1, 1),
            new Job(1, 2, 2),
            new Job(2, 3, 3),
            new Job(3, 4, 4),
            new Job(4, 5, 5),
            new Job(5, 1, 1),
            new Job(6, 2, 2)
        ));
    }

    public static NamedJobList getMixedExtremeList() {
        // Fläche Platte: 625950
        // Jobs:
        // 963*650=625950
        // 1*1=1
        // 963*649=624387
        // 1000*700=700000
        // 2*2=4
        // 963*1=963
        // 1*650=650
        // 963*650=625950
        // 963*650=625950
        // Summe: 625950 + 1 + 624387 + 700000 + 4 + 963 + 650 + 625950 + 625950 = 3,828,805
        // Deckungsrate: 3,828,805 / 625,950 ≈ 611.8% (viel zu groß)
        // Kleinster Job Fläche: 1 (Job 1), Größter Job Fläche: 700,000 (Job 3)
        // Kleinste Kante: 1 (Job 1 and 5 and 6), Größte Kante: 963 (Jobs 0,2,5,7,8)
        return new NamedJobList(9, "Gemischte Extreme", "Extreme Größen von winzig bis Plattengröße", Arrays.asList(
            new Job(0, 963, 650),   // exakt Plattengröße
            new Job(1, 1, 1),       // sehr klein
            new Job(2, 963, 649),   // fast Plattengröße
            new Job(3, 1000, 700),  // zu groß
            new Job(4, 2, 2),
            new Job(5, 963, 1),     // sehr lang und schmal
            new Job(6, 1, 650),     // sehr hoch und schmal
            new Job(7, 963, 650),   // exakt Plattengröße (doppelt)
            new Job(8, 963, 650)    // exakt Plattengröße (doppelt)
        ));
    }

    public static NamedJobList getSingleJobFitsExactly() {
        // Fläche Platte: 625950
        // Job: 963*650 = 625950
        // Deckungsrate: 100%
        // Kleinster Job = Größter Job = Fläche 625950, Kanten 963 und 650
        return new NamedJobList(10, "Ein Job passt exakt", "Ein einziges Paneel exakt Plattengröße", Arrays.asList(
            new Job(0, 963, 650)
        ));
    }

    public static NamedJobList getSingleJobTooLarge() {
        // Fläche Platte: 625950
        // Job: 2000*2000 = 4,000,000
        // Deckungsrate: 640% (viel zu groß)
        // Kleinster Job = Größter Job = 4,000,000, Kanten 2000 und 2000
        return new NamedJobList(11, "Ein Job zu groß", "Ein einziges Paneel, deutlich zu groß", Arrays.asList(
            new Job(0, 2000, 2000)
        ));
    }

    public static NamedJobList getManyTinyJobs() {
        // Fläche Platte: 625950
        // Jobs: 100 * (5*5) = 100 * 25 = 2500
        // Deckungsrate: 2500 / 625950 ≈ 0.4%
        // Kleinster Job = Größter Job = 25 (alle)
        // Kleinste Kante = Größte Kante = 5 (alle)
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            jobs.add(new Job(i, 5, 5));
        }
        return new NamedJobList(12, "Viele winzige Jobs (100x 5x5)", "100 sehr kleine Quadrate (5x5)", jobs);
    }

    public static NamedJobList getAlternatingSizesList() {
        // Platte: 963 x 650 = 625950 (Fläche Platte)
        // Jobs Flächen (Breite x Höhe = Fläche):
        // Summe aller Job-Flächen: 200000 + 25 + 247500 + 100 + 222600 + 225 + 224400 + 400 = 895250
        // Deckungsrate (Summe Job-Flächen / Fläche Platte): 895250 / 625950 ≈ 1.431 (143.1%) → Jobs brauchen 43.1% mehr Fläche als Platte bietet
        // Kleinste Fläche: 25 (Job 1)
        // Größte Fläche: 247500 (Job 2)
        // Kleinste Kante (Breite oder Höhe): 5 (Job 1)
        // Größte Kante (Breite oder Höhe): 550 (Job 2)
        return new NamedJobList(13, "Abwechselnd große und kleine Jobs", "Mix aus sehr kleinen und sehr großen Elementen", Arrays.asList(
            new Job(0, 400, 500),
            new Job(1, 5, 5),
            new Job(2, 450, 550),
            new Job(3, 10, 10),
            new Job(4, 420, 530),
            new Job(5, 15, 15),
            new Job(6, 440, 510),
            new Job(7, 20, 20)
        ));
    }

    public static NamedJobList getDecimalJobsList() {
        // Fläche Platte: 625950
        // Jobs: 100.5*150.2 + 120.7*130.3 + 90.9*110.1 = 15109.05 + 15723.21 + 9999.09 = 40831.35
        // Deckungsrate: 40831.35 / 625950 ≈ 6.53%
        // Kleinster Job Fläche: 9999.09 (Job 2), Größter Job Fläche: 15723.21 (Job 1)
        // Kleinste Kante: 90.9 (Job 2), Größte Kante: 150.2 (Job 0)
        List<Job> jobs = Arrays.asList(
            new Job(0, 100.5, 150.2),
            new Job(1, 120.7, 130.3),
            new Job(2, 90.9, 110.1)
        );
        return new NamedJobList(14, "Jobs mit Kommazahlen", "3 Jobs mit Dezimalmaßen", jobs);
    }

    // Erweiterte StandardJobList für zwei Platten
    public static NamedJobList getExtendedStandardJobList() {
        // Fläche pro Platte: 963*650=625950, zwei Platten: 1.251.900
        // Jobs: Summe Flächen ca. 1.100.000
        return new NamedJobList(15, "Erweiterte Standardliste", "Standardliste + Ergänzungen für 2 Platten", Arrays.asList(
            new Job(0, 402, 480),
            new Job(1, 305, 222),
            new Job(2, 220, 573),
            new Job(3, 205, 153),
            new Job(4, 243, 188),
            new Job(5, 243, 188),
            new Job(6, 205, 153),
            new Job(7, 402, 480),
            new Job(8, 305, 222)
            //new Job(9, 220, 573),
            //new Job(10, 205, 153),
            //new Job(11, 243, 188),
            //new Job(12, 243, 188),
            //new Job(13, 205, 153)
        ));
    }

    // Erweiterte StandardJobList für zwei Platten
    public static NamedJobList getExtendedStandardJobListTwo() {
        // Fläche pro Platte: 963*650=625950, zwei Platten: 1.251.900
        // Jobs: Summe Flächen ca. 1.100.000
        return new NamedJobList(16, "Erweiterte Standardliste Zwei", "Standardliste + Ergänzungen für 3 Platten", Arrays.asList(
            new Job(0, 402, 480),
            new Job(1, 305, 222),
            new Job(2, 220, 573),
            new Job(3, 205, 153),
            new Job(4, 243, 188),
            new Job(5, 243, 188),
            new Job(6, 205, 153),
            new Job(7, 402, 480),
            new Job(8, 305, 222),
            new Job(9, 220, 573),
            new Job(10, 205, 153),
            new Job(11, 243, 188)
            //new Job(12, 243, 188),
            //new Job(13, 205, 153)
        ));
    }


    // Menü in fester Reihenfolge (IDs aufsteigend)
    public static java.util.List<NamedJobList> getAllListsInMenuOrder() {
        return java.util.Arrays.asList(
            getStandardJobList(),
            getSmallJobList(),
            getMediumJobList(),
            getLargeJobList(),
            getAllSameSizeList(),
            getDuplicateJobsList(),
            getTooLargeJobsList(),
            getVerySmallJobsList(),
            getMixedExtremeList(),
            getSingleJobFitsExactly(),
            getSingleJobTooLarge(),
            getManyTinyJobs(),
            getAlternatingSizesList(),
            getDecimalJobsList(),
            getExtendedStandardJobList(),
            getExtendedStandardJobListTwo()
        );
    }

}
