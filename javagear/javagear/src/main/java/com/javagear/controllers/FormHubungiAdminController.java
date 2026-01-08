package com.javagear.controllers;

import com.javagear.models.DatabaseConnection;
import com.javagear.models.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class FormHubungiAdminController {

    @FXML
    private TextArea fieldPesan;

    @FXML
    private void handleKirimPesan() {
        String pesan = fieldPesan.getText().trim();

        if (pesan.isEmpty()) {
            showAlert("Error", "Pesan tidak boleh kosong!");
            return;
        }

        // Kirim ke Database
        if (kirimPesanKeDatabase(pesan)) {
            showAlert("Sukses", "Pesan berhasil dikirim ke Admin!");
            closeWindow();
        } else {
            showAlert("Error", "Gagal mengirim pesan. Coba lagi.");
        }
    }

    private boolean kirimPesanKeDatabase(String pesan) {
        int userId = SessionManager.getCurrentUserId();
        // Tipe: 'user_ke_admin', status_bayar: null (karena cuma chat biasa)
        String sql =
                "INSERT INTO notifikasi (user_id, pesan, tipe, is_read, created_at) VALUES (?, ?, 'user_ke_admin', 0, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Tambahin label "KONSULTASI" biar admin tau ini bukan bayar denda
            String finalPesan = "💬 KONSULTASI USER:\n" + pesan;

            stmt.setInt(1, userId);
            stmt.setString(2, finalPesan);

            int result = stmt.executeUpdate();
            return result > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void handleBatal() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) fieldPesan.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
