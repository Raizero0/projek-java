package com.javagear.models;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataSeeder {

    public static void addSeederData(Connection connection) {
        try {
            System.out.println(" Menambahkan data seeder...");

            addAdminUser(connection);

            // 1. Bikin User Biasa (bukan admin) - DIPASTIKAN PAKE BCRYPT
            addUserDummy(connection);

            // 2. Bikin Data Produk Outdoor
            addProdukDummy(connection);

            // 3. Bikin Data Peminjaman (buat tes tabel admin)
            addPeminjamanDummy(connection);

            System.out.println("✅ Data seeder berhasil ditambahkan!");

        } catch (SQLException e) {
            System.err.println("❌ Gagal masukin data dummy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addAdminUser(Connection connection) throws SQLException {
        // Cek dulu admin udah ada belum - PASTIKAN TABLE NAME BENAR
        String checkSql = "SELECT id FROM user WHERE username = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, "admin");
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("User admin 'admin' sudah ada.");

                String currentPassword = getCurrentPassword(connection, "admin");
                if (!currentPassword.startsWith("$2a$")) {
                    System.out.println("⚠️  Admin password tidak di-hash, updating...");
                    updatePasswordToBCrypt(connection, "admin", "admin123");
                }
                return;
            }
        }

        String sql =
                "INSERT INTO user (nama, username, email, password, telepon, no_ktp, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());

            stmt.setString(1, "Admin JavaGear");
            stmt.setString(2, "admin");
            stmt.setString(3, "admin@javagear.com");
            stmt.setString(4, hashedPassword); // ✅ HASHED
            stmt.setString(5, "081200000001");
            stmt.setString(6, "Jl. Admin No. 1");
            stmt.setString(7, "admin");

            stmt.executeUpdate();
            System.out
                    .println("✅ User ADMIN 'admin' berhasil dibuat (password: admin123 - HASHED)");
            System.out.println(" Hash: " + hashedPassword.substring(0, 20) + "...");
        }
    }

    private static void addUserDummy(Connection connection) throws SQLException {
        // Array user dummy untuk consistency
        String[][] dummyUser = {
                {"railene", "user123", "Railene Pendaki", "railene@email.com", "081299998888",
                        "Jl. User No. 1"},
                {"john_doe", "user123", "John Doe", "john@example.com", "081233445566",
                        "Jl. Contoh No. 2"},
                {"sarah_ayu", "user123", "Sarah Ayu", "sarah@example.com", "081244556677",
                        "Jl. Sample No. 3"}};

        for (String[] user : dummyUser) {
            String username = user[0];
            String password = user[1];

            // Cek dulu user udah ada belum
            String checkSql = "SELECT id FROM user WHERE username = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    System.out.println("ℹ️ User dummy '" + username + "' sudah ada.");

                    // 🆕 UPDATE: Cek password hash consistency
                    String currentPassword = getCurrentPassword(connection, username);
                    if (!currentPassword.startsWith("$2a$")) {
                        System.out.println(
                                "  User " + username + " password tidak di-hash, updating...");
                        updatePasswordToBCrypt(connection, username, password);
                    }
                    continue;
                }
            }

            String sql =
                    "INSERT INTO user (nama, username, email, password, telepon, no_ktp, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                // ✅ PASTIKAN PAKE BCRYPT - KONSISTEN
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                stmt.setString(1, user[2]); // nama_lengkap
                stmt.setString(2, username);
                stmt.setString(3, user[3]); // email
                stmt.setString(4, hashedPassword); // ✅ HASHED
                stmt.setString(5, user[4]); // telepon
                stmt.setString(6, user[5]); // alamat
                stmt.setString(7, "user");

                stmt.executeUpdate();
                System.out.println("✅ User dummy '" + username + "' berhasil dibuat (password: "
                        + password + " - HASHED)");
                System.out.println("🔐 Hash: " + hashedPassword.substring(0, 20) + "...");
            }
        }
    }

    private static String getCurrentPassword(Connection connection, String username)
            throws SQLException {
        String sql = "SELECT password FROM user WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password");
            }
        }
        return "";
    }

    private static void updatePasswordToBCrypt(Connection connection, String username,
            String plainPassword) throws SQLException {
        String sql = "UPDATE user SET password = ? WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);
            stmt.executeUpdate();
            System.out.println("✅ Password updated to BCrypt for user: " + username);
        }
    }

    private static void addProdukDummy(Connection connection) throws SQLException {
        // Cek dulu produk udah ada belum
        String checkSql = "SELECT COUNT(*) as jumlah FROM produk";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next() && rs.getInt("jumlah") > 0) {
                System.out.println("ℹ️ Data produk sudah ada.");
                return;
            }
        }

        String sql =
                "INSERT INTO produk (nama, kategori, deskripsi, harga_sewa, stok) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Barang 1: Tenda
            stmt.setString(1, "Tenda Dome 4 Person");
            stmt.setString(2, "Tenda");
            stmt.setString(3,
                    "Tenda kapasitas 4 orang, double layer, waterproof. Cocok untuk keluarga atau kelompok kecil.");
            stmt.setDouble(4, 25000); // Harga sewa per hari
            stmt.setInt(5, 8);
            stmt.executeUpdate();

            // Barang 2: Carrier
            stmt.setString(1, "Carrier Eiger 60L");
            stmt.setString(2, "Tas");
            stmt.setString(3,
                    "Tas gunung kapasitas 60 liter, include rain cover. Nyaman untuk pendakian 2-3 hari.");
            stmt.setDouble(4, 30000);
            stmt.setInt(5, 5);
            stmt.executeUpdate();

            // Barang 3: Sleeping Bag
            stmt.setString(1, "Sleeping Bag -5°C");
            stmt.setString(2, "Peralatan Tidur");
            stmt.setString(3,
                    "Sleeping bag tahan hingga -5 derajat, bahan waterproof, ringan dan mudah dibawa.");
            stmt.setDouble(4, 15000);
            stmt.setInt(5, 10);
            stmt.executeUpdate();

            // Barang 4: Kompor Portable
            stmt.setString(1, "Kompor Portable Gas");
            stmt.setString(2, "Peralatan Masak");
            stmt.setString(3,
                    "Kompor portable praktis, menggunakan gas kaleng, cocok untuk camping dan pendakian.");
            stmt.setDouble(4, 12000);
            stmt.setInt(5, 6);
            stmt.executeUpdate();

            // Barang 5: Headlamp
            stmt.setString(1, "Headlamp LED");
            stmt.setString(2, "Penerangan");
            stmt.setString(3,
                    "Headlamp LED terang, tahan air, battery tahan lama. Essential untuk camping malam.");
            stmt.setDouble(4, 8000);
            stmt.setInt(5, 15);
            stmt.executeUpdate();

            // Barang 6: Matras Lipat
            stmt.setString(1, "Matras Camping Lipat");
            stmt.setString(2, "Peralatan Tidur");
            stmt.setString(3, "Matras camping tebal dan nyaman, mudah dilipat, anti bocor.");
            stmt.setDouble(4, 10000);
            stmt.setInt(5, 12);
            stmt.executeUpdate();

            // Barang 7: Nesting Set
            stmt.setString(1, "Nesting Set Aluminum");
            stmt.setString(2, "Peralatan Masak");
            stmt.setString(3,
                    "Set panci dan wajan nesting, praktis untuk masak outdoor, bahan aluminum ringan.");
            stmt.setDouble(4, 15000);
            stmt.setInt(5, 8);
            stmt.executeUpdate();

            // Barang 8: Trekking Pole
            stmt.setString(1, "Trekking Pole Adjustable");
            stmt.setString(2, "Aksesoris");
            stmt.setString(3,
                    "Tongkat hiking adjustable, bahan aluminium ringan, support beban hingga 120kg.");
            stmt.setDouble(4, 12000);
            stmt.setInt(5, 10);
            stmt.executeUpdate();

            // Barang 9: Tenda 2 Orang
            stmt.setString(1, "Tenda Dome 2 Person");
            stmt.setString(2, "Tenda");
            stmt.setString(3,
                    "Tenda compact untuk 2 orang, ultra lightweight, cocok untuk backpacking.");
            stmt.setDouble(4, 20000);
            stmt.setInt(5, 6);
            stmt.executeUpdate();

            // Barang 10: Hammock
            stmt.setString(1, "Hammock Outdoor");
            stmt.setString(2, "Peralatan Tidur");
            stmt.setString(3,
                    "Hammock nyaman untuk camping, bahan parachute nylon, include carabiner.");
            stmt.setDouble(4, 18000);
            stmt.setInt(5, 7);
            stmt.executeUpdate();

            System.out.println("✅ 10 data produk outdoor berhasil ditambahkan.");
        }
    }

    private static void addPeminjamanDummy(Connection connection) throws SQLException {
        // Cek dulu peminjaman udah ada belum
        String checkSql = "SELECT COUNT(*) as jumlah FROM peminjaman";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                ResultSet rs = checkStmt.executeQuery()) {
            if (rs.next() && rs.getInt("jumlah") > 0) {
                System.out.println("ℹ️ Data peminjaman sudah ada.");
                return;
            }
        }

        // Cari ID user railene dan ID produk
        int userId = 0;
        int produkId = 0;

        // Table name dari 'users' → 'user'
        String getUserSql = "SELECT id FROM user WHERE username = 'railene'";
        try (PreparedStatement stmt = connection.prepareStatement(getUserSql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next())
                userId = rs.getInt("id");
        }

        String getProdukSql = "SELECT id FROM produk WHERE nama = 'Tenda Dome 4 Person'";
        try (PreparedStatement stmt = connection.prepareStatement(getProdukSql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next())
                produkId = rs.getInt("id");
        }

        if (userId != 0 && produkId != 0) {
            String sql =
                    "INSERT INTO peminjaman (user_id, produk_id, tanggal_pinjam, durasi_hari, total_bayar, status, status_pembayaran) VALUES (?, ?, DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, produkId);
                stmt.setInt(3, 3); // Minjem 3 hari
                stmt.setDouble(4, 75000); // 25rb x 3
                stmt.setString(5, "Dipinjam");
                stmt.setString(6, "BELUM_BAYAR"); // Status pembayaran
                stmt.executeUpdate();
                System.out.println(
                        "✅ Data peminjaman dummy berhasil ditambahkan (Status: Telat 2 hari).");
            }
        } else {
            System.out.println(
                    "⚠️  Gagal tambah peminjaman dummy: User atau produk tidak ditemukan.");
        }
    }
}
