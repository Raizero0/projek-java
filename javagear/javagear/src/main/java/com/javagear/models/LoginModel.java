package com.javagear.models;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginModel {

    private DatabaseConnection databaseConnection;

    public LoginModel() {

        this.databaseConnection = DatabaseConnection.getInstance();
    }

    public boolean cekLoginDanSimpanSesi(String username, String password) {
        String query =
                "SELECT id, password, role, username, nama FROM user WHERE username = ? OR email = ?";

        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, username); // Bisa login pake username atau email
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                String userRole = rs.getString("role");
                int userId = rs.getInt("id");
                String dbUsername = rs.getString("username");
                String nama = rs.getString("nama");

                System.out.println("=== DEBUG LOGIN ===");
                System.out.println("Input Username/Email: " + username);
                System.out.println("Found Username: " + dbUsername);
                System.out.println("Found Name: " + nama);
                System.out.println("Role: " + userRole);
                System.out.println("User ID: " + userId);
                System.out.println("Stored Password Length: " + dbPassword.length());
                System.out.println("Stored Password Prefix: "
                        + dbPassword.substring(0, Math.min(20, dbPassword.length())));
                System.out.println("Is BCrypt Hash: " + dbPassword.startsWith("$2a$"));

                boolean loginSuccess = false;
                String verificationMethod = "";

                if (dbPassword.startsWith("$2a$")) {
                    // Password sudah di-hash dengan BCrypt
                    loginSuccess = BCrypt.checkpw(password, dbPassword);
                    verificationMethod = "BCrypt";
                } else {
                    // Fallback: Password masih plain text (seharusnya tidak terjadi setelah fix)
                    System.out.println(" WARNING: Password stored as plain text!");
                    loginSuccess = password.equals(dbPassword);
                    verificationMethod = "Plain Text (Fallback)";

                    // Auto-update ke BCrypt
                    updatePasswordToBCrypt(dbUsername, password);
                }

                System.out.println("Verification Method: " + verificationMethod);
                System.out.println("Login Success: " + loginSuccess);
                System.out.println("=======================");

                if (loginSuccess) {
                    // ✅ SIMPAN SESI LENGKAP
                    SessionManager.setCurrentUserId(userId);
                    SessionManager.setCurrentUser(dbUsername, userRole);
                    SessionManager.setCurrentUserName(nama);

                    System.out.println(
                            "✅ Login successful! User: " + dbUsername + " | Role: " + userRole);
                    return true;
                } else {
                    System.out.println("❌ Password mismatch for user: " + dbUsername);
                }
            } else {
                System.out.println("❌ User not found: " + username);
            }
        } catch (SQLException e) {
            System.err.println("❌ Database error during login: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private void updatePasswordToBCrypt(String username, String plainPassword) {
        String sql = "UPDATE user SET password = ? WHERE username = ?";

        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✅ Auto-updated password to BCrypt for user: " + username);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to auto-update password: " + e.getMessage());
        }
    }

    public String getUserRole(String username) {
        String query = "SELECT role FROM user WHERE username = ? OR email = ?";
        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting user role: " + e.getMessage());
        }
        return null;
    }

    public boolean registerUser(String nama, String username, String email, String password,
            String telepon, String NoKtp) {
        if (isUsernameExist(username)) {
            System.out.println("❌ Username already exists: " + username);
            return false;
        }

        if (isEmailExist(email)) {
            System.out.println("❌ Email already exists: " + email);
            return false;
        }

        String sql =
                "INSERT INTO user (nama, username, email, password, telepon, no_ktp, role) VALUES (?, ?, ?, ?, ?, ?, 'user')";

        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // ✅ KONSISTEN: Hash password dengan BCrypt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            stmt.setString(1, nama);
            stmt.setString(2, username);
            stmt.setString(3, email);
            stmt.setString(4, hashedPassword);
            stmt.setString(5, telepon);
            stmt.setString(6, NoKtp);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✅ User registered successfully: " + username);
                System.out
                        .println("🔐 Password hashed: " + hashedPassword.substring(0, 20) + "...");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Registration error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean isUsernameExist(String username) {
        // ✅ FIX: Table name dari 'user' → 'users'
        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement("SELECT id FROM user WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("❌ Error checking username: " + e.getMessage());
            return true; // Error dianggap username exists untuk safety
        }
    }

    private boolean isEmailExist(String email) {
        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement("SELECT id FROM user WHERE email = ?")) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("❌ Error checking email: " + e.getMessage());
            return true; // Error dianggap email exists untuk safety
        }
    }

    public boolean resetPassword(String username, String email, String passwordBaru) {
        // Cek database cuma berdasarkan Username DAN Email
        String sql = "UPDATE user SET password = ? WHERE username = ? AND email = ?";

        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash password baru
            String hashedPassword = BCrypt.hashpw(passwordBaru, BCrypt.gensalt());

            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);
            stmt.setString(3, email);

            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;

            System.out.println(
                    " Password reset attempt for user: " + username + " | Success: " + success);
            return success;

        } catch (SQLException e) {
            System.err.println("❌ Password reset error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void debugUserPassword(String username) {
        String sql =
                "SELECT username, password, LENGTH(password) as pwd_length, role FROM user WHERE username = ?";

        try (Connection conn = databaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String pwd = rs.getString("password");
                int length = rs.getInt("pwd_length");
                String role = rs.getString("role");

                System.out.println(
                        "🔍 DEBUG USER: " + username + " | Role: " + role + " | Password Length: "
                                + length + " | Is BCrypt: " + pwd.startsWith("$2a$") + " | Prefix: "
                                + pwd.substring(0, Math.min(10, pwd.length())) + "...");
            } else {
                System.out.println("🔍 DEBUG: User not found - " + username);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error debugging user: " + e.getMessage());
        }
    }
}
