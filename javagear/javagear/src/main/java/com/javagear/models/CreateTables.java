package com.javagear.models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CreateTables {

    public static void setupTables(Connection connection) {
        try {
            System.out.println("🛠️  Setup tabel database...");

            createTableUser(connection);
            createTableProduk(connection);
            createTablePeminjamanEnhanced(connection);
            createTableNotifikasiEnhanced(connection);
            updateTableStructure(connection);
            updateDatabaseStructure(connection);

            System.out.println("✅ Semua tabel berhasil disetup!");
        } catch (SQLException e) {
            System.err.println("❌ Error setting up tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void updateTableStructure(Connection connection) {
        try {
            System.out.println("🔄 Checking table structure updates...");
            java.sql.DatabaseMetaData meta = connection.getMetaData();
            java.sql.ResultSet rs;

            // 1. CEK KOLOM DENDA (Peminjaman)
            rs = meta.getColumns(null, null, "peminjaman", "denda");
            if (!rs.next()) {
                String sql = "ALTER TABLE peminjaman ADD COLUMN denda DOUBLE DEFAULT 0";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.executeUpdate();
                    System.out.println("✅ Kolom 'denda' ditambahkan.");
                }
            }

            // 2. CEK KOLOM SALDO (User)
            rs = meta.getColumns(null, null, "user", "saldo");
            if (!rs.next()) {
                String sql = "ALTER TABLE user ADD COLUMN saldo DOUBLE DEFAULT 0";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.executeUpdate();
                    System.out.println("✅ Kolom 'saldo' ditambahkan.");
                }
            }

            // 3. CEK KOLOM FOTO PROFIL (User) 
            rs = meta.getColumns(null, null, "user", "foto_profil");
            if (!rs.next()) {
                // Default pake 'default.png'
                String sql =
                        "ALTER TABLE user ADD COLUMN foto_profil VARCHAR(255) DEFAULT 'default.png'";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.executeUpdate();
                    System.out.println("✅ Kolom 'foto_profil' ditambahkan!");
                }
            }

            System.out.println("✅ Struktur tabel sudah up-to-date.");

        } catch (SQLException e) {
            System.err.println("❌ Gagal update struktur tabel: " + e.getMessage());
        }
    }

    // --- (SISA KODE BAWAH TIDAK BERUBAH, TAPI TETAP DISERTAKAN BIAR FULL) ---
    private static void createTableUser(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS user (" + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "nama VARCHAR(255) NOT NULL, " + "username VARCHAR(50) NOT NULL UNIQUE, "
                + "email VARCHAR(255) NOT NULL, " + "telepon VARCHAR(15) NOT NULL, "
                + "no_ktp VARCHAR(20), " + "password VARCHAR(255) NOT NULL, "
                + "role VARCHAR(50) NOT NULL, " + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private static void createTableProduk(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS produk (" + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "nama VARCHAR(100) NOT NULL, " + "kategori VARCHAR(50) NOT NULL, "
                + "deskripsi TEXT, " + "harga_sewa DOUBLE NOT NULL, " + "stok INT NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private static void createTablePeminjamanEnhanced(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS peminjaman ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, " + "user_id INT NOT NULL, "
                + "produk_id INT NOT NULL, " + "tanggal_pinjam DATE NOT NULL, "
                + "durasi_hari INT NOT NULL, " + "jumlah_barang INT NOT NULL DEFAULT 1, "
                + "total_bayar DOUBLE NOT NULL, " + "metode_bayar VARCHAR(50) DEFAULT 'TRANSFER', "
                + "nominal_bayar DOUBLE DEFAULT 0, " + "kembalian DOUBLE DEFAULT 0, "
                + "id_pembayaran VARCHAR(100), " + "status VARCHAR(20) DEFAULT 'Dipinjam', "
                + "status_pembayaran VARCHAR(20) DEFAULT 'BELUM_BAYAR', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES user(id), "
                + "FOREIGN KEY (produk_id) REFERENCES produk(id))";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private static void createTableNotifikasiEnhanced(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS notifikasi ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, " + "user_id INT NOT NULL, "
                + "pesan TEXT NOT NULL, " + "tipe VARCHAR(50) DEFAULT 'admin_ke_user', "
                + "total_denda DOUBLE DEFAULT 0, "
                + "status_bayar VARCHAR(50) DEFAULT 'BELUM_BAYAR', "
                + "is_read BOOLEAN DEFAULT FALSE, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES user(id))";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public static void updateDatabaseStructure(Connection conn) {
        try (java.sql.Statement stmt = conn.createStatement()) {
            String sql =
                    "ALTER TABLE peminjaman ADD COLUMN metode_bayar_denda VARCHAR(20) DEFAULT NULL";
            stmt.executeUpdate(sql);
            System.out.println(
                    "✅ Sukses update tabel: Kolom 'metode_bayar_denda' berhasil ditambahkan.");
        } catch (java.sql.SQLException e) {
            // Kalau error "Duplicate column name", berarti kolom udah ada. Biarin aja.
            if (e.getMessage().contains("Duplicate") || e.getMessage().contains("exists")) {
                System.out.println("ℹ️ Info: Kolom 'metode_bayar_denda' sudah ada (Aman).");
            } else {
                e.printStackTrace();
            }
        }
    }
}
