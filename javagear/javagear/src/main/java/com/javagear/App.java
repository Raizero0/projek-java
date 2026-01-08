package com.javagear;

import com.javagear.controllers.LoginController;
import com.javagear.controllers.LupaPasswordController;
import com.javagear.controllers.RegisterController;
import com.javagear.models.DatabaseConnection;
import com.javagear.models.CreateTables;
import com.javagear.models.DataSeeder;
import com.javagear.models.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.Connection;

public class App extends Application {

    private static Stage mainStage;
    private static Scene loginScene;
    private static Scene registerScene;
    private static Scene lupaPasswordScene;
    private static LoginController loginController;
    private static RegisterController registerController;
    private static LupaPasswordController lupaPasswordController;

    @Override
    public void start(Stage stage) {
        try {
            mainStage = stage;

            System.out.println("🚀 =========================================");
            System.out.println("🚀 JAVA GEAR - OUTDOOR RENTAL SYSTEM");
            System.out.println("🚀 =========================================");

            initializeDatabase();

            //  Pre-load SEMUA halaman dengan ERROR HANDLING
            System.out.println("📁 Pre-loading semua halaman...");

            try {
                FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
                Parent loginRoot = loginLoader.load();
                loginController = loginLoader.getController(); 
                loginScene = new Scene(loginRoot);
                System.out.println("✅ Loaded: Login");
            } catch (Exception e) {
                System.err.println("❌ Gagal Load Login: " + e.getMessage());
                loginScene = createEmergencyScene("Login Gagal");
            }

            try {
                FXMLLoader regLoader =
                        new FXMLLoader(getClass().getResource("/fxml/auth/Register.fxml"));
                Parent regRoot = regLoader.load();
                registerController = regLoader.getController(); 
                registerScene = new Scene(regRoot);
                System.out.println("✅ Loaded: Register");
            } catch (Exception e) {
                System.err.println("❌ Gagal Load Register: " + e.getMessage());
                registerScene = createEmergencyScene("Register Gagal");
            }
            try {
                FXMLLoader lupaLoader =
                        new FXMLLoader(getClass().getResource("/fxml/auth/LupaPassword.fxml"));
                Parent lupaRoot = lupaLoader.load();
                lupaPasswordController = lupaLoader.getController(); 
                lupaPasswordScene = new Scene(lupaRoot);
                System.out.println("✅ Loaded: Lupa Password");
            } catch (Exception e) {
                System.err.println("❌ Gagal Load Lupa Password: " + e.getMessage());
                lupaPasswordScene = createEmergencyScene("Lupa Password Gagal");
            }

            System.out.println("✅ Semua halaman siap!");

            // 3. Setup Jendela - WINDOW MANAGEMENT FIX
            stage.setTitle("JavaGear - Outdoor Rental System");
            try {
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/logo.png")));
                System.out.println("🖼️ Logo aplikasi loaded");
            } catch (Exception e) {
                System.out.println("⚠️ Logo tidak ditemukan, lanjut tanpa logo...");
            }

            stage.setMinWidth(1200);
            stage.setMinHeight(700);
            stage.setWidth(1200);
            stage.setHeight(700);

            SessionManager.clearSession();

            showLogin();

            stage.show();

            System.out.println("🎉 Aplikasi berhasil di-start!");
            System.out.println("🔐 Authentication System: READY");
            System.out.println("💾 Database System: READY");
            System.out.println("🎨 UI System: READY");

        } catch (Exception e) {
            System.err.println("💥 CRITICAL ERROR DURING STARTUP: " + e.getMessage());
            e.printStackTrace();
            // EMERGENCY FALLBACK - kalo semua gagal
            showEmergencyScene(stage);
        }
    }

    private void initializeDatabase() {
        try {
            System.out.println("🔧 ===== DATABASE INITIALIZATION =====");

            // 1. Cek koneksi database
            System.out.println("📡 Checking database connection...");
            DatabaseConnection dbConnection = DatabaseConnection.getInstance();
            Connection conn = dbConnection.getConnection();

            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Database connection: SUCCESS");
            } else {
                throw new Exception("Database connection failed");
            }

            // 2. Create tables jika belum ada
            System.out.println("🗃️ Creating database tables...");
            CreateTables.setupTables(conn);
            System.out.println("✅ Database tables: READY");

            // 3. Seed initial data
            System.out.println("🌱 Seeding initial data...");
            DataSeeder.addSeederData(conn);
            System.out.println("✅ Initial data: SEEDED");

            // 4. Verify critical data
            System.out.println("🔍 Verifying critical users...");
            verifyCriticalUsers(conn);

            System.out.println("✅ Database initialization: COMPLETE");
            System.out.println("=====================================");

        } catch (Exception e) {
            System.err.println("❌ DATABASE INITIALIZATION FAILED: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    // 🆕 METHOD BARU: Verify critical users exist
    private void verifyCriticalUsers(Connection conn) throws Exception {
        String checkSql = "SELECT username, role FROM user WHERE username IN ('admin', 'railene')";
        var stmt = conn.createStatement();
        var rs = stmt.executeQuery(checkSql);

        int userCount = 0;
        while (rs.next()) {
            String username = rs.getString("username");
            String role = rs.getString("role");
            System.out.println("👤 Found user: " + username + " | Role: " + role);
            userCount++;
        }

        if (userCount >= 2) {
            System.out.println("✅ Critical users verification: PASSED");
        } else {
            System.out.println("⚠️ Critical users verification: SOME USERS MISSING");
        }
    }
    private Scene loadSceneSafely(String fxmlPath, String sceneName) {
        try {
            System.out.println("📂 Loading: " + fxmlPath);
            URL fxmlUrl = getClass().getResource(fxmlPath);

            if (fxmlUrl == null) {
                throw new Exception("FXML file not found: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            System.out.println("✅ Loaded: " + sceneName);
            return new Scene(root);

        } catch (Exception e) {
            System.err.println("❌ GAGAL LOAD: " + fxmlPath);
            System.err.println("Error: " + e.getMessage());

            return createEmergencyScene(sceneName + " (Gagal Load)");
        }
    }

    private Scene createEmergencyScene(String title) {
        VBox emergencyRoot = new VBox(20);
        emergencyRoot
                .setStyle("-fx-alignment: center; -fx-padding: 50; -fx-background-color: #f8f9fa;");

        Label titleLabel = new Label("⚠️ " + title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        Label infoLabel =
                new Label("Halaman tidak bisa dimuat\nPeriksa file FXML di resources folder");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-text-alignment: center; -fx-font-size: 14px;");

        // Tombol navigasi emergency
        javafx.scene.control.Button btnLogin = new javafx.scene.control.Button("🔐 Ke Login");
        btnLogin.setOnAction(e -> showLogin());
        btnLogin.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

        javafx.scene.control.Button btnAdmin = new javafx.scene.control.Button("👨‍💼 Ke Admin");
        btnAdmin.setOnAction(e -> showAdminDashboard());
        btnAdmin.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

        javafx.scene.control.Button btnUser = new javafx.scene.control.Button("👤 Ke User");
        btnUser.setOnAction(e -> showUserDashboard());
        btnUser.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

        emergencyRoot.getChildren().addAll(titleLabel, infoLabel, btnLogin, btnAdmin, btnUser);
        return new Scene(emergencyRoot, 1200, 700);
    }

    // METHOD BARU: Fallback ultimate kalo semua gagal
    private void showEmergencyScene(Stage stage) {
        VBox root = new VBox(20);
        root.setStyle("-fx-alignment: center; -fx-padding: 50; -fx-background-color: #2c3e50;");

        Label label = new Label("🚨 APLIKASI JAVA GEAR 🚨\nTerjadi Error Saat Startup");
        label.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-text-alignment: center;");

        Label infoLabel = new Label(
                "Periksa:\n• Struktur folder resources\n• File FXML\n• Database connection\n• Console untuk detail error");
        infoLabel.setStyle(
                "-fx-text-fill: #ecf0f1; -fx-text-alignment: center; -fx-font-size: 14px;");

        javafx.scene.control.Button btnClose = new javafx.scene.control.Button("❌ Tutup Aplikasi");
        btnClose.setOnAction(e -> stage.close());
        btnClose.setStyle(
                "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

        root.getChildren().addAll(label, infoLabel, btnClose);
        Scene emergencyScene = new Scene(root, 800, 400);
        stage.setScene(emergencyScene);
        stage.show();
    }

    public static void showLogin() {
        System.out.println("📍 Navigating to Login...");
        mainStage.setTitle("JavaGear - Login");
        mainStage.setScene(loginScene);
        mainStage.setMaximized(true);

        if (loginController != null) {
            loginController.clearForm();
        }
    }

    public static void showAdminDashboard() {
        try {
            System.out.println("🎬 Loading Admin Dashboard...");

            // 🆕 SESSION VALIDATION
            if (!SessionManager.isLoggedIn() || !SessionManager.isAdmin()) {
                System.out.println("❌ Access denied - Not admin or not logged in");
                showLogin();
                return;
            }

            String fxmlPath = "/fxml/admin/AdminMenu.fxml";
            URL fxmlUrl = App.class.getResource(fxmlPath);

            if (fxmlUrl == null) {
                throw new Exception("Admin Dashboard FXML not found");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene adminScene = new Scene(root);
            mainStage.setScene(adminScene);
            mainStage.setTitle(
                    "Admin Dashboard - JavaGear | User: " + SessionManager.getCurrentUser());
            mainStage.setMaximized(true);

            System.out.println("✅ Admin Dashboard loaded successfully");
            System.out.println("👤 Admin: " + SessionManager.getCurrentUser());

        } catch (Exception e) {
            System.err.println("❌ ERROR loading Admin Dashboard: " + e.getMessage());
            e.printStackTrace();
            showLogin(); 
        }
    }

    public static void showUserDashboard() { 
        try {
            System.out.println("🎬 Loading User Menu...");
            if (!SessionManager.isLoggedIn()) {
                showLogin();
                return;
            }

            // 🟢 UPDATE PATH KE FILE BARU
            String fxmlPath = "/fxml/user/UserMenu.fxml";

            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene userScene = new Scene(root);
            mainStage.setScene(userScene);
            mainStage.setTitle("Main Menu - JavaGear | " + SessionManager.getCurrentUserName());
            mainStage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showRegister() {
        System.out.println("📍 Navigating to Register...");
        mainStage.setTitle("Daftar Akun - JavaGear");
        mainStage.setScene(registerScene);
        mainStage.setMaximized(true);

        if (registerController != null) {
            registerController.clearForm();
        }
    }

    public static void showLupaPassword() {
        System.out.println("📍 Navigating to Lupa Password...");
        mainStage.setTitle("Reset Password - JavaGear");
        mainStage.setScene(lupaPasswordScene);
        mainStage.setMaximized(true);

        if (lupaPasswordController != null) {
            lupaPasswordController.clearForm();
        }
    }

    public static void shutdown() {
        System.out.println("🔴 Shutting down JavaGear application...");
        SessionManager.clearSession();
        DatabaseConnection.getInstance().close();
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            System.out.println("🎯 Starting JavaGear Application...");
            launch();
        } catch (Exception e) {
            System.err.println("💥 FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
