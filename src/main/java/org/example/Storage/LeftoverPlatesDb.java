package org.example.Storage;

import java.sql.*;

public class LeftoverPlatesDb {
    private final String url;

    public LeftoverPlatesDb(String filePath) {
        this.url = "jdbc:sqlite:" + filePath;
        init();
    }

    private void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found on classpath. Please ensure sqlite-jdbc is a runtime dependency.", e);
        }
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS free_rects (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "solution_label TEXT," +
                    "plate_index INTEGER," +
                    "x REAL," +
                    "y REAL," +
                    "width REAL," +
                    "height REAL" +
                    ")");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init SQLite schema", e);
        }
    }

    public void clearAll() {
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM free_rects");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertFreeRect(String solutionLabel, int plateIndex, double x, double y, double width, double height) {
        String sql = "INSERT INTO free_rects(solution_label, plate_index, x, y, width, height) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, solutionLabel);
            ps.setInt(2, plateIndex);
            ps.setDouble(3, x);
            ps.setDouble(4, y);
            ps.setDouble(5, width);
            ps.setDouble(6, height);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countAll() {
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM free_rects")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
