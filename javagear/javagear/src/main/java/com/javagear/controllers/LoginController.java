package com.javagear.controllers;

import com.javagear.App;
import com.javagear.models.DatabaseConnection; // Pastikan ini ada
import com.javagear.models.LoginModel;
import com.javagear.models.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import java.sql.Connection; // Import SQL
import java.sql.PreparedStatement; // Import SQL
import java.sql.ResultSet; // Import SQL

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordTextField;
    @FXML
    private CheckBox cbShowPassword;

    private boolean isPasswordVisible = false;
    private LoginModel loginModel = new LoginModel();

    public LoginController() {
        this.loginModel = new LoginModel();
    }

    @FXML
    private void initialize() {
        Platform.runLater(() -> usernameField.requestFocus());

        // Setup Enter Key
        passwordField.setOnAction(e -> handleLogin());
        passwordTextField.setOnAction(e -> handleLogin());

        // Sinkronisasi Teks (Biar isi passwordField & passwordTextField selalu sama)
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        passwordTextField.setVisible(false);
        passwordField.setVisible(true);

        cbShowPassword.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // Kalo Dicentang -> Tampilkan Teks, Sembunyikan Bintang
                passwordTextField.setVisible(true);
                passwordField.setVisible(false);

                passwordTextField.toFront();
                passwordTextField.requestFocus();
                passwordTextField.positionCaret(passwordTextField.getText().length());
            } else {
                // Kalo Gak Dicentang -> Tampilkan Bintang, Sembunyikan Teks
                passwordTextField.setVisible(false);
                passwordField.setVisible(true);

                passwordField.toFront();
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });
    }

    // BERSIHKAN FORM (Dipanggil dari App.java)
    public void clearForm() {
        usernameField.clear();
        passwordField.clear();
        passwordTextField.clear();
        usernameField.requestFocus();
        if (cbShowPassword != null)
            cbShowPassword.setSelected(false);

        isPasswordVisible = false;

        usernameField.requestFocus();

        System.out.println("🧹 Login form cleared!");
    }

    @FXML
    private void togglePasswordVisibility(MouseEvent event) {
        isPasswordVisible = !isPasswordVisible;

    }


    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Input Error", "Username dan password harus diisi!");
            return;
        }

        // LOGIC BARU: CEK STATUS BEKU & REDIRECT KE REQUEST AKTIVASI
        if (isUserFrozen(username)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("AKUN DIBEKUKAN");
            alert.setHeaderText("Akses Ditolak");
            alert.setContentText(
                    "Akun Anda telah dinonaktifkan oleh Admin.\nKlik OK untuk menghubungi Admin agar akun diaktifkan kembali.");

            // Redirect otomatis saat klik OK
            alert.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    handleRequestAktivasi(username); // Memanggil method baru (lihat poin 2)
                }
            });
            return;
        }

        // LOGIN NORMAL
        try {
            if (loginModel.cekLoginDanSimpanSesi(username, password)) {
                if (!SessionManager.validateSession())
                    return;

                updateLastLoginTime(username); // Catat waktu login

                String role = SessionManager.getUserRole();
                if ("admin".equalsIgnoreCase(role)) {
                    App.showAdminDashboard();
                } else {
                    App.showUserDashboard();
                }
            } else {
                // LOGIC BARU: PESAN "TERLALU TAMPAN" & REDIRECT REGISTER
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Gagal");
                alert.setHeaderText("Akun Tidak Ditemukan");
                alert.setContentText(
                        "Mungkin password salah, atau akun Anda sudah DIHAPUS Admin karena tindakan terlalu tampan.\n\nSilakan buat akun baru.");

                // Redirect ke Register saat klik OK
                alert.showAndWait().ifPresent(response -> {
                    if (response == javafx.scene.control.ButtonType.OK) {
                        App.showRegister();
                    }
                });

                passwordField.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("System Error", e.getMessage());
        }
    }

    // CEK APAKAH USER NONAKTIF?
    private boolean isUserFrozen(String username) {
        String sql = "SELECT status FROM user WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                // Kalau statusnya 'nonaktif', return true (Frozen)
                return "nonaktif".equalsIgnoreCase(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Default aman
    }

    // UPDATE JAM TERAKHIR LOGIN
    private void updateLastLoginTime(String username) {
        String sql = "UPDATE user SET last_login = NOW() WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
            System.out.println("⏱️ Last login updated for: " + username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegister() {
        App.showRegister();
    }

    @FXML
    private void goToLupaPassword() {
        App.showLupaPassword();
    }

    @FXML
    private void handleExit() {
        Platform.exit();
        System.exit(0);
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    //  FORM REQUEST AKTIVASI (TANPA LOGIN)
    private void handleRequestAktivasi(String username) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Hubungi Admin");
        dialog.setHeaderText("Permohonan Aktivasi Akun");
        dialog.setContentText("Tulis alasan kenapa akun harus diaktifkan:");

        dialog.showAndWait().ifPresent(pesan -> {
            if (pesan.trim().isEmpty())
                return;

            // Query Cerdas: Langsung ambil ID user dari username & Insert ke Notifikasi
            String sql =
                    "INSERT INTO notifikasi (user_id, pesan, tipe, is_read) SELECT id, ?, 'req_aktivasi', 0 FROM user WHERE username = ?";

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, "Mohon aktifkan akun saya (" + username + "). Alasan: " + pesan);
                stmt.setString(2, username);

                int rows = stmt.executeUpdate();

                if (rows > 0) {
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Terkirim");
                    info.setHeaderText(null);
                    info.setContentText("Permohonan berhasil dikirim! Tunggu Admin menyetujui.");
                    info.showAndWait();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Gagal mengirim request: " + e.getMessage());
            }
        });
    }
}
