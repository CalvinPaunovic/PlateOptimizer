package org.example.IOClasses;

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
        return new NamedJobList(2, "Kleine Liste", "3 Mini-Jobs zum schnellen Testen", Arrays.asList(
            new Job(0, 100, 150),
            new Job(1, 120, 130),
            new Job(2, 90, 110)
        ));
    }

    public static NamedJobList getMediumJobList() {
        return new NamedJobList(3, "Mittlere Liste", "4 mittelgroße Jobs, mittlere Auslastung", Arrays.asList(
            new Job(0, 200, 300),
            new Job(1, 250, 350),
            new Job(2, 220, 330),
            new Job(3, 240, 310)
        ));
    }

    public static NamedJobList getLargeJobList() {
        return new NamedJobList(4, "Große Liste", "5 große Paneele, deutlich zu groß für 1 Platte", Arrays.asList(
            new Job(0, 400, 500),
            new Job(1, 450, 550),
            new Job(2, 420, 530),
            new Job(3, 440, 510),
            new Job(4, 460, 520)
        ));
    }

    public static NamedJobList getAllSameSizeList() {
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            jobs.add(new Job(i, 100, 100));
        }
        return new NamedJobList(5, "Alle Jobs gleich groß (100x)", "100 identische Quadrate (100x100)", jobs);
    }

    public static NamedJobList getDuplicateJobsList() {
        return new NamedJobList(6, "Doppelte Jobs", "5 identische Quadrate (200x200)", Arrays.asList(
            new Job(0, 200, 200),
            new Job(1, 200, 200),
            new Job(2, 200, 200),
            new Job(3, 200, 200),
            new Job(4, 200, 200)
        ));
    }

    public static NamedJobList getTooLargeJobsList() {
        return new NamedJobList(7, "Zu große Jobs", "4 Jobs, die die Platte(n) massiv überfüllen", Arrays.asList(
            new Job(0, 2000, 2000),
            new Job(1, 963, 700),
            new Job(2, 1200, 650),
            new Job(3, 964, 651)
        ));
    }

    public static NamedJobList getVerySmallJobsList() {
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
        return new NamedJobList(10, "Ein Job passt exakt", "Ein einziges Paneel exakt Plattengröße", Arrays.asList(
            new Job(0, 963, 650)
        ));
    }

    public static NamedJobList getSingleJobTooLarge() {
        return new NamedJobList(11, "Ein Job zu groß", "Ein einziges Paneel, deutlich zu groß", Arrays.asList(
            new Job(0, 2000, 2000)
        ));
    }

    public static NamedJobList getManyTinyJobs() {
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            jobs.add(new Job(i, 5, 5));
        }
        return new NamedJobList(12, "Viele winzige Jobs (100x 5x5)", "100 sehr kleine Quadrate (5x5)", jobs);
    }

    public static NamedJobList getAlternatingSizesList() {
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
        List<Job> jobs = Arrays.asList(
            new Job(0, 100.5, 150.2),
            new Job(1, 120.7, 130.3),
            new Job(2, 90.9, 110.1)
        );
        return new NamedJobList(14, "Jobs mit Kommazahlen", "3 Jobs mit Dezimalmaßen", jobs);
    }

    public static NamedJobList getExtendedStandardJobListTwo() {
        return new NamedJobList(15, "Erweiterte Standardliste", "Standardliste + Ergänzungen für 2 Platten", Arrays.asList(
            new Job(2, 220, 573),
            new Job(0, 402, 480),
            new Job(7, 402, 480),
            new Job(1, 305, 222),
            new Job(8, 305, 222),
            new Job(4, 243, 188),
            new Job(5, 243, 188),
            new Job(3, 205, 153),
            new Job(6, 205, 153)
        ));
    }

    public static NamedJobList getExtendedStandardJobListThree() {
        // Fläche pro Platte: 963*650=625950, zwei Platten: 1.251.900
        // Jobs: Summe Flächen ca. 1.100.000
        return new NamedJobList(16, "Erweiterte Standardliste Drei", "Standardliste + Ergänzungen für 3 Platten", Arrays.asList(
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
        ));
    }

    public static NamedJobList getExtendedStandardJobListFour() {
        // Fläche pro Platte: 963*650=625950
        // Ziel: genug Jobs, damit 3 Platten nicht ausreichen und eine vierte benötigt wird
        return new NamedJobList(17, "Erweiterte Standardliste Vier", "Standardliste + Ergänzungen für 4 Platten", Arrays.asList(
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
            new Job(11, 243, 188),
            new Job(12, 243, 188),
            new Job(13, 205, 153),
            new Job(14, 402, 480),
            new Job(15, 402, 480),
            new Job(16, 450, 550),
            new Job(17, 305, 222),
            new Job(18, 300, 320)
        ));
    }

    public static NamedJobList getExtendedA1JobListTwoTypes() {
        // Fläche pro Platte: 841 * 594 = 499_854 mm²
        // Zwei Job-Typen, mehrfach vorkommend, Gesamtfläche > 1 Platte
        return new NamedJobList(18, "A1 Zweityp-Liste", "Jobs für A1-Platte", Arrays.asList(
            new Job(0, 400, 300),
            new Job(1, 400, 300),
            new Job(2, 400, 300),
            new Job(4, 250, 200),
            new Job(5, 250, 200),
            new Job(6, 250, 200),
            new Job(7, 250, 200),
            new Job(8, 250, 200)
        ));
    }


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
            getExtendedStandardJobListTwo(),
            getExtendedStandardJobListThree(),
            getExtendedStandardJobListFour(),
            getExtendedA1JobListTwoTypes()
        );
    }

}
