package com.javagear.controllers;

import com.javagear.App;
import com.javagear.models.LoginModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import java.io.InputStream;
import java.util.regex.Pattern;

public class RegisterController {

    @FXML
    private TextField fieldNama;
    @FXML
    private TextField fieldUsername;
    @FXML
    private TextField fieldEmail;
    @FXML
    private TextField fieldTelepon;
    @FXML
    private TextField fieldKtp;
    @FXML
    private PasswordField fieldPassword; // Password (****)
    @FXML
    private TextField fieldPasswordText; // Password (Teks biasa)
    @FXML
    private ImageView eyeIcon; // Ikon Mata

    private LoginModel loginModel;

    // Status Show/Hide
    private boolean isPasswordVisible = false;
    private Image iconOpen;
    private Image iconClose;

    // REGEX PATTERNS
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,13}$");

    public RegisterController() {
        this.loginModel = new LoginModel();
        System.out.println("🔧 RegisterController initialized");

        try {
            InputStream sOpen = getClass().getResourceAsStream("/img/icon_eye_open.png");
            if (sOpen != null)
                iconOpen = new Image(sOpen);

            InputStream sClose = getClass().getResourceAsStream("/img/icon_eye_close.png");
            if (sClose != null)
                iconClose = new Image(sClose);
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Ikon mata tidak ditemukan di RegisterController.");
        }
    }

    @FXML
    private void initialize() {
        System.out.println("🎬 RegisterController FXML initialized");

        fieldPassword.setOnAction(e -> handleRegister());
        fieldPasswordText.setOnAction(e -> handleRegister()); // Bisa enter di mode show password
        fieldKtp.setOnAction(e -> handleRegister());

        Platform.runLater(() -> fieldNama.requestFocus());

        fieldPasswordText.textProperty().bindBidirectional(fieldPassword.textProperty());
        updatePasswordUI();
    }

    @FXML
    private void togglePasswordVisibility(MouseEvent event) {
        isPasswordVisible = !isPasswordVisible;
        updatePasswordUI();
    }

    private void updatePasswordUI() {
        if (isPasswordVisible) {
            // Mode Buka: Tampilkan Teks Biasa
            fieldPasswordText.setVisible(true);
            fieldPasswordText.setManaged(true);
            fieldPassword.setVisible(false);
            fieldPassword.setManaged(false);
            if (iconOpen != null)
                eyeIcon.setImage(iconOpen);
        } else {
            // Mode Tutup: Tampilkan Bintang-bintang
            fieldPasswordText.setVisible(false);
            fieldPasswordText.setManaged(false);
            fieldPassword.setVisible(true);
            fieldPassword.setManaged(true);
            if (iconClose != null)
                eyeIcon.setImage(iconClose);
        }
    }

    @FXML
    private void handleRegister() {
        String nama = fieldNama.getText().trim();
        String username = fieldUsername.getText().trim();
        String email = fieldEmail.getText().trim();
        String password = fieldPassword.getText(); // Ambil dari sini (karena sudah di-bind)
        String telepon = fieldTelepon.getText().trim();
        String noKtp = fieldKtp.getText().trim();

        System.out.println("=== 📝 REGISTRATION ATTEMPT ===");

        // VALIDASI INPUT
        String validationError = validateInput(nama, username, email, password, telepon, noKtp);
        if (validationError != null) {
            showAlert("Input Error", validationError);
            return;
        }

        try {
            boolean registrationSuccess =
                    loginModel.registerUser(nama, username, email, password, telepon, noKtp);

            if (registrationSuccess) {
                showAlert("Registrasi Berhasil",
                        "Akun berhasil dibuat!\nSilakan login dengan username dan password Anda.");
                App.showLogin();
            } else {
                showAlert("Registrasi Gagal", "Username atau email sudah digunakan!");
                fieldUsername.clear();
                fieldEmail.clear();
                fieldUsername.requestFocus();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("System Error", "Terjadi kesalahan sistem: " + e.getMessage());
        }
    }

    private String validateInput(String nama, String username, String email, String password,
            String telepon, String noKtp) {
        if (nama.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()
                || noKtp.isEmpty()) {
            return "Semua field wajib diisi!";
        }
        if (nama.length() < 3 || nama.length() > 50)
            return "Nama harus 3-50 karakter!";
        if (!USERNAME_PATTERN.matcher(username).matches())
            return "Username tidak valid (hanya huruf, angka, _)!";
        if (!EMAIL_PATTERN.matcher(email).matches())
            return "Format email tidak valid!";
        if (password.length() < 6)
            return "Password minimal 6 karakter!";
        if (!telepon.isEmpty() && !PHONE_PATTERN.matcher(telepon).matches())
            return "Format telepon tidak valid!";
        if (noKtp.length() < 10)
            return "No. KTP terlalu pendek!";
        return null;
    }

    @FXML
    private void handleBack() {
        App.showLogin();
    }

    @FXML
    private void handleClear() {
        fieldNama.clear();
        fieldUsername.clear();
        fieldEmail.clear();
        fieldPassword.clear();
        fieldTelepon.clear();
        fieldKtp.clear();
        fieldNama.requestFocus();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(400, 200);
        alert.showAndWait();
    }

    public void clearForm() {
        fieldNama.clear();
        fieldUsername.clear();
        fieldEmail.clear();
        fieldTelepon.clear();
        fieldKtp.clear();

        // Reset Password (Baik yang bintang-bintang maupun teks biasa)
        fieldPassword.clear();
        fieldPasswordText.clear();

        // Reset Mata (Sembunyikan Password)
        isPasswordVisible = false;
        updatePasswordUI();

        // Fokus balik ke field pertama
        fieldNama.requestFocus();

        System.out.println("🧹 Form Register dibersihkan!");
    }
}
