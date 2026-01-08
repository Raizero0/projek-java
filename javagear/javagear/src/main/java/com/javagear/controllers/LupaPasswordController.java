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

public class LupaPasswordController {

    @FXML
    private TextField fieldUsername;
    @FXML
    private TextField fieldEmail;

    @FXML
    private PasswordField fieldPassBaru; // Password (****)
    @FXML
    private TextField fieldPassBaruText; // Password (Teks)
    @FXML
    private ImageView eyeIcon; // Ikon Mata

    private LoginModel loginModel;

    // Status Show/Hide
    private boolean isPasswordVisible = false;
    private Image iconOpen;
    private Image iconClose;

    public LupaPasswordController() {
        this.loginModel = new LoginModel();

        try {
            InputStream sOpen = getClass().getResourceAsStream("/img/icon_eye_open.png");
            if (sOpen != null)
                iconOpen = new Image(sOpen);

            InputStream sClose = getClass().getResourceAsStream("/img/icon_eye_close.png");
            if (sClose != null)
                iconClose = new Image(sClose);
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Ikon mata tidak ditemukan di LupaPassword.");
        }
    }

    @FXML
    private void initialize() {
        // Setup Focus
        Platform.runLater(() -> fieldUsername.requestFocus());

        // Sinkronisasi field password
        fieldPassBaruText.textProperty().bindBidirectional(fieldPassBaru.textProperty());

        // Setup Enter Key
        fieldPassBaru.setOnAction(e -> handleReset());
        fieldPassBaruText.setOnAction(e -> handleReset());

        updatePasswordUI();
    }

    @FXML
    private void togglePasswordVisibility(MouseEvent event) {
        isPasswordVisible = !isPasswordVisible;
        updatePasswordUI();
    }

    private void updatePasswordUI() {
        if (isPasswordVisible) {
            // Mode Buka
            fieldPassBaruText.setVisible(true);
            fieldPassBaruText.setManaged(true);
            fieldPassBaru.setVisible(false);
            fieldPassBaru.setManaged(false);
            if (iconOpen != null)
                eyeIcon.setImage(iconOpen);
        } else {
            // Mode Tutup
            fieldPassBaruText.setVisible(false);
            fieldPassBaruText.setManaged(false);
            fieldPassBaru.setVisible(true);
            fieldPassBaru.setManaged(true);
            if (iconClose != null)
                eyeIcon.setImage(iconClose);
        }
    }

    @FXML
    private void handleReset() {
        String user = fieldUsername.getText().trim();
        String email = fieldEmail.getText().trim();
        String passBaru = fieldPassBaru.getText(); // Ambil dari yg ter-bind

        if (user.isEmpty() || email.isEmpty() || passBaru.isEmpty()) {
            showAlert("Error", "Isi Username, Email, dan Password Baru!");
            return;
        }

        if (passBaru.length() < 6) {
            showAlert("Error", "Password minimal 6 karakter!");
            return;
        }

        // ✅ PANGGIL LOGIN MODEL (TANPA TELEPON)
        if (loginModel.resetPassword(user, email, passBaru)) {
            showAlert("Sukses", "Password berhasil diganti! Silakan login dengan password baru.");
            App.showLogin();
        } else {
            showAlert("Gagal",
                    "Data tidak cocok! Pastikan Username dan Email sesuai dengan akun Anda.");
        }
    }

    @FXML
    private void handleBack() {
        App.showLogin();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void clearForm() {
        fieldUsername.clear();
        fieldEmail.clear();

        // Reset Password Baru
        fieldPassBaru.clear();
        fieldPassBaruText.clear();

        // Reset Mata
        isPasswordVisible = false;
        updatePasswordUI();

        // Fokus balik ke awal
        fieldUsername.requestFocus();

        System.out.println("🧹 Form Lupa Password dibersihkan!");
    }
}
