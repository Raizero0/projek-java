package com.javagear.controllers;

import com.javagear.App;
import com.javagear.models.DatabaseConnection;
import com.javagear.models.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminMenuController {

    @FXML
    private Label lblNamaAdmin;
    @FXML
    private Circle circleFotoAdmin;
    @FXML
    private Label lblPesanCount;
    @FXML
    private Label lblPesanPreview;

    //Visibility
    public void initialize() {
        setupProfilAdmin();
        loadPreviewPesan();
    }

    //Visibility
    private void setupProfilAdmin() {
        lblNamaAdmin.setText(SessionManager.getCurrentUserName());

        // Load Foto Admin
        try {
            String imagePath = "/img/foto_admin.jpg";
            if (getClass().getResource(imagePath) != null) {
                Image img = new Image(getClass().getResourceAsStream(imagePath));
                circleFotoAdmin.setFill(new ImagePattern(img));
            } else {
                Image img = new Image(getClass().getResourceAsStream("/img/logo.png"));
                circleFotoAdmin.setFill(new ImagePattern(img));
            }
        } catch (Exception e) {
            System.out.println("⚠️ Foto admin belum di-set.");
        }
    }

    private void loadPreviewPesan() {
        String sql =
                "SELECT COUNT(*) as jumlah, (SELECT pesan FROM notifikasi WHERE tipe = 'user_ke_admin' ORDER BY created_at DESC LIMIT 1) as last_msg "
                        + "FROM notifikasi WHERE tipe = 'user_ke_admin' AND is_read = 0";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int jumlah = rs.getInt("jumlah");
                String lastMsg = rs.getString("last_msg");

                if (jumlah > 0) {
                    lblPesanCount.setText(jumlah + " Pesan Baru");
                    lblPesanCount.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    lblPesanPreview.setText("Terbaru: " + (lastMsg != null && lastMsg.length() > 30
                            ? lastMsg.substring(0, 30) + "..."
                            : lastMsg));
                } else {
                    lblPesanCount.setText("Tidak ada pesan baru");
                    lblPesanCount.setStyle("-fx-text-fill: #27ae60;");
                    lblPesanPreview.setText("Inbox aman terkendali.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBukaInbox() {
        System.out.println("📩 Membuka Inbox Admin...");

        StringBuilder daftarPesan = new StringBuilder();
        int count = 0;

        // 1. Ambil pesan yang BELUM DIBACA
        String sql = "SELECT u.nama, n.pesan, n.created_at " + "FROM notifikasi n "
                + "JOIN user u ON n.user_id = u.id "
                + "WHERE n.tipe = 'user_ke_admin' AND n.is_read = 0 "
                + "ORDER BY n.created_at DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                count++;
                String pengirim = rs.getString("nama");
                String pesan = rs.getString("pesan");
                String waktu = rs.getString("created_at");

                daftarPesan.append("👤 ").append(pengirim).append(" (").append(waktu).append(")\n");
                daftarPesan.append("💬 ").append(pesan).append("\n");
                daftarPesan.append("──────────────────────────\n\n");
            }

            if (count > 0) {
                showScrollableAlert("KOTAK MASUK (" + count + " Pesan Baru)",
                        daftarPesan.toString());

                markAllAsRead(conn);

                loadPreviewPesan();

            } else {
                showAlert("Inbox Kosong", "Tidak ada pesan baru yang belum dibaca.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markAllAsRead(Connection conn) {
        String sql =
                "UPDATE notifikasi SET is_read = 1 WHERE tipe = 'user_ke_admin' AND is_read = 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int updated = stmt.executeUpdate();
            System.out.println(
                    "✅ " + updated + " pesan telah ditandai 'Read' dan dihapus dari widget.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- NAVIGASI ---

    @FXML
    private void handleMenuUser() {
        System.out.println("📍 Masuk Menu User (Monitoring)...");
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/admin/AdminUserView.fxml"));
            Parent root = loader.load();

            // Ganti Scene
            Stage stage = (Stage) lblNamaAdmin.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMenuBarang() {
        System.out.println("📍 Masuk Menu Barang (Stok)...");
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/admin/AdminBarangView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) lblNamaAdmin.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        App.showLogin();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showScrollableAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(400, 300);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }
}
