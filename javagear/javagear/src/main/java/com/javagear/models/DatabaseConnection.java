package com.javagear.models;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; 

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private static HikariDataSource dataSource;
    private final String DB_NAME = "javagear_db";
    private static final int MAX_POOL_SIZE = 10;

    public static class DatabaseConfig {
        public static final int CONNECTION_TIMEOUT = 30000;
        public static final int IDLE_TIMEOUT = 600000;
        public static final int MAX_LIFETIME = 1800000;
        public static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
        public static final String URL_TEMPLATE = "jdbc:mysql://localhost:3306/";

        public static void displayConfig() {
            System.out.println("⚙️  DATABASE CONFIGURATION:");
            System.out.println("   Max Pool Size: " + MAX_POOL_SIZE);
            System.out.println("   Connection Timeout: " + CONNECTION_TIMEOUT + "ms");
            System.out.println("   Idle Timeout: " + IDLE_TIMEOUT + "ms");
            System.out.println("   Max Lifetime: " + MAX_LIFETIME + "ms");
        }

        public static boolean isValidConfig() {
            return MAX_POOL_SIZE > 0 && CONNECTION_TIMEOUT > 0;
        }

        public static String getConfigSummary() {
            return String.format("Pool:%d, Timeout:%ds, Idle:%dmin", MAX_POOL_SIZE,
                    CONNECTION_TIMEOUT / 1000, IDLE_TIMEOUT / 60000);
        }
    }

    public static class QueryBuilder {
        private String tableName;
        private StringBuilder whereClause = new StringBuilder();
        private StringBuilder orderByClause = new StringBuilder();
        private int limit = 0;

        public QueryBuilder(String tableName) {
            this.tableName = tableName;
        }

        public QueryBuilder where(String condition) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            } else {
                whereClause.append(" WHERE ");
            }
            whereClause.append(condition);
            return this;
        }

        public QueryBuilder orderBy(String column, boolean ascending) {
            if (orderByClause.length() > 0) {
                orderByClause.append(", ");
            } else {
                orderByClause.append(" ORDER BY ");
            }
            orderByClause.append(column).append(ascending ? " ASC" : " DESC");
            return this;
        }

        public QueryBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public String build() {
            StringBuilder query = new StringBuilder("SELECT * FROM " + tableName);
            query.append(whereClause);
            query.append(orderByClause);
            if (limit > 0) {
                query.append(" LIMIT ").append(limit);
            }
            return query.toString();
        }

        public static String getUserById(int userId) {
            return "SELECT * FROM user WHERE id = " + userId;
        }

        public static String getActivePeminjaman() {
            return "SELECT * FROM peminjaman WHERE status = 'Dipinjam'";
        }

        public static String getPeminjamanWithDenda() {
            return "SELECT * FROM peminjaman WHERE status = 'Dipinjam' AND CURRENT_DATE > DATE_ADD(tanggal_pinjam, INTERVAL durasi_hari DAY)";
        }
    }

    private DatabaseConnection() {
        try {
            System.out.println(" MEMBUAT KONEKSI DATABASE POOL (HIKARI)...");

            System.out.println("📊 " + DatabaseConfig.getConfigSummary());
            DatabaseConfig.displayConfig();

            createDatabaseIfNotExists();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DatabaseConfig.URL_TEMPLATE + DB_NAME);
            config.setUsername("root");
            config.setPassword("");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.setMaximumPoolSize(DatabaseConnection.MAX_POOL_SIZE);
            config.setConnectionTimeout(DatabaseConfig.CONNECTION_TIMEOUT);
            config.setIdleTimeout(DatabaseConfig.IDLE_TIMEOUT);
            config.setMaxLifetime(DatabaseConfig.MAX_LIFETIME);

            dataSource = new HikariDataSource(config);
            setupInitialTables();

            updateTableStructure();

            System.out.println("✅ Database pool berhasil dibuat!");

        } catch (SQLException e) {
            System.err.println("❌ Gagal total membuat HikariCP Pool!");
            e.printStackTrace();
            throw new RuntimeException("Gagal konek database", e);
        } catch (Exception e) {
            System.err.println("❌ Error tidak terduga: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error sistem", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public ResultSet executeQuery(String tableName, String whereCondition) throws SQLException {
        QueryBuilder qb = new QueryBuilder(tableName);
        if (whereCondition != null && !whereCondition.isEmpty()) {
            qb.where(whereCondition);
        }
        String query = qb.build();
        System.out.println("🔍 Executing query: " + query);

        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);
        return stmt.executeQuery();
    }

    public ResultSet executeQuery(String tableName, String whereCondition, String orderBy,
            boolean ascending) throws SQLException {
        QueryBuilder qb = new QueryBuilder(tableName);
        if (whereCondition != null && !whereCondition.isEmpty()) {
            qb.where(whereCondition);
        }
        if (orderBy != null && !orderBy.isEmpty()) {
            qb.orderBy(orderBy, ascending);
        }
        String query = qb.build();
        System.out.println("🔍 Executing query: " + query);

        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);
        return stmt.executeQuery();
    }

    public static void demoStaticNestedClass() {
        System.out.println("\n🎯 DEMO STATIC NESTED CLASS:");

        // Akses static nested class tanpa instance outer class
        DatabaseConnection.DatabaseConfig.displayConfig();
        System.out.println("✅ Config valid: " + DatabaseConnection.DatabaseConfig.isValidConfig());

        // Demo QueryBuilder
        DatabaseConnection.QueryBuilder qb = new DatabaseConnection.QueryBuilder("peminjaman");
        String query = qb.where("status = 'Dipinjam'").where("user_id = 1")
                .orderBy("tanggal_pinjam", false).limit(10).build();
        System.out.println("📝 Generated query: " + query);

        // Demo static methods
        System.out.println("👤 User query: " + DatabaseConnection.QueryBuilder.getUserById(1));
        System.out.println(
                "📦 Active peminjaman: " + DatabaseConnection.QueryBuilder.getActivePeminjaman());
    }

    private void updateTableStructure() {
        System.out.println("🔍 Memeriksa struktur tabel untuk pembaruan...");
        // Gunakan try-with-resources untuk Statement agar aman
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // 1. Cek & Tambah Kolom 'status' (Aktif/Nonaktif)
            if (!isColumnExist(conn, "user", "status")) {
                System.out.println("⚙️ Menambahkan kolom 'status' ke tabel user...");
                stmt.executeUpdate(
                        "ALTER TABLE user ADD COLUMN status ENUM('aktif', 'nonaktif') DEFAULT 'aktif'");
            }

            // 2. Cek & Tambah Kolom 'last_login' (Waktu Terakhir Online)
            if (!isColumnExist(conn, "user", "last_login")) {
                System.out.println("⚙️ Menambahkan kolom 'last_login' ke tabel user...");
                stmt.executeUpdate("ALTER TABLE user ADD COLUMN last_login DATETIME DEFAULT NULL");
            }

            if (!isColumnExist(conn, "notifikasi", "nominal")) {
                System.out.println("⚙️ Menambahkan kolom 'nominal' ke tabel notifikasi...");
                stmt.executeUpdate("ALTER TABLE notifikasi ADD COLUMN nominal DOUBLE DEFAULT 0");
            }

        } catch (SQLException e) {
            System.err.println("⚠️ Warning Auto-Migration: " + e.getMessage());
        }
    }

    private boolean isColumnExist(Connection conn, String tableName, String columnName) {
        String sql =
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ? AND column_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DB_NAME);
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
        }
        return false;
    }

    private void createDatabaseIfNotExists() {
        try {
            try (Connection conn =
                    DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "")) {
                ResultSet rs = conn.getMetaData().getCatalogs();
                boolean dbExists = false;
                while (rs.next()) {
                    String databaseName = rs.getString(1);
                    if (DB_NAME.equalsIgnoreCase(databaseName)) {
                        dbExists = true;
                        break;
                    }
                }
                rs.close();

                if (!dbExists) {
                    try (PreparedStatement stmt =
                            conn.prepareStatement("CREATE DATABASE " + DB_NAME)) {
                        stmt.executeUpdate();
                        System.out.println("✅ Database '" + DB_NAME + "' berhasil dibuat!");
                    }
                } else {
                    System.out.println("✅ Database '" + DB_NAME + "' sudah ada.");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Gagal buat/cek database: " + e.getMessage());
            throw new RuntimeException("Gagal buat database", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("🔌 Database connection pool ditutup.");
        }
    }

    private void setupInitialTables() throws SQLException {
        try (Connection connection = getConnection()) {
            System.out.println("🔗 Berhasil terhubung ke database '" + DB_NAME + "'!");

            if (!isTableExist(connection, "user")) {
                createUserTableAndDefaultUser(connection);
            }

            CreateTables.setupTables(connection);
            DataSeeder.addSeederData(connection);

            System.out.println("✅ Semua tabel berhasil disetup!");
        }
    }

    private boolean isTableExist(Connection connection, String tableName) {
        String sql =
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, DB_NAME);
            stmt.setString(2, tableName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void createUserTableAndDefaultUser(Connection connection) {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS user ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, " + "nama VARCHAR(255) NOT NULL, "
                + "username VARCHAR(50) NOT NULL UNIQUE, " + "email VARCHAR(255) NOT NULL, "
                + "telepon VARCHAR(15) NOT NULL, " + "no_ktp VARCHAR(20), "
                + "password VARCHAR(255) NOT NULL, " + "role VARCHAR(50) NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String insertUserSQL =
                "INSERT INTO user (nama, username, email, password, telepon, no_ktp, role) "
                        + "SELECT ?, ?, ?, ?, ?, ?, ? FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = ?)";

        try (PreparedStatement stmtCreate = connection.prepareStatement(createTableSQL);
                PreparedStatement stmtInsert = connection.prepareStatement(insertUserSQL)) {

            stmtCreate.executeUpdate();
            System.out.println("✅ Tabel 'user' berhasil dibuat.");

            String hashedAdmin = BCrypt.hashpw("admin123", BCrypt.gensalt());
            stmtInsert.setString(1, "Admin JavaGear");
            stmtInsert.setString(2, "admin");
            stmtInsert.setString(3, "admin@javagear.com");
            stmtInsert.setString(4, hashedAdmin);
            stmtInsert.setString(5, "08123456789");
            stmtInsert.setString(6, "-");
            stmtInsert.setString(7, "admin");
            stmtInsert.setString(8, "admin");

            int rowsAffected = stmtInsert.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Akun Admin default berhasil dibuat.");
                System.out.println("   👤 Username: admin");
                System.out.println("   🔑 Password: admin123");
            } else {
                System.out.println("ℹ️ Akun Admin sudah ada.");
            }

        } catch (SQLException e) {
            System.err.println("❌ Gagal buat tabel/user: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
