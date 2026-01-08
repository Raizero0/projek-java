package com.javagear.controllers;

import com.javagear.App;
import com.javagear.models.DatabaseConnection;
import com.javagear.models.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene; // <--- INI YANG TADI KURANG
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage; // <--- BIAR LEBIH RAPI

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserMenuController {

    // --- HEADER COMPONENT ---
    @FXML
    private Label lblNamaUser;
    @FXML
    private Label lblSaldo;
    @FXML
    private Button btnPesan;
    @FXML
    private Circle circleProfil;
    @FXML
    private ImageView imgBadgeSmall;
    @FXML
    private Label lblStatusMember;
    @FXML
    private ProgressBar progressJourney;
    @FXML
    private ImageView imgLevel1, imgLevel2, imgLevel3;
    @FXML
    private Label lblLevel2, lblLevel3;
    @FXML
    private Label lblBenefit2, lblBenefit3;
    @FXML
    private Label lblNextGoal;

    public void initialize() {
        lblNamaUser.setText("Hai, " + SessionManager.getCurrentUserName() + "!");
        loadDataUserLengkap();
        cekPesanBaru();
        cekKeterlambatan();
        loadJourneyData();
    }

    // --- LOAD DATA USER ---
    private void loadDataUserLengkap() {
        int userId = SessionManager.getCurrentUserId();
        String sql = "SELECT saldo, foto_profil FROM user WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                lblSaldo.setText("Rp " + String.format("%,.0f", rs.getDouble("saldo")));
                String fotoName = rs.getString("foto_profil");
                if (fotoName == null || fotoName.isEmpty())
                    fotoName = "default.png";
                SessionManager.setCurrentUserFoto(fotoName);

                Image img = null;
                try {
                    String path = "/img/fotouser/" + fotoName;
                    if (getClass().getResource(path) != null)
                        img = new Image(getClass().getResourceAsStream(path));
                    else
                        img = new Image(
                                getClass().getResourceAsStream("/img/fotouser/default.png"));
                } catch (Exception e) {
                }

                if (img != null && !img.isError())
                    circleProfil.setFill(new ImagePattern(img));
                else
                    circleProfil.setFill(javafx.scene.paint.Color.LIGHTGRAY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadJourneyData() {
        int userId = SessionManager.getCurrentUserId();
        int totalSewa = 0;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn
                        .prepareStatement("SELECT COUNT(*) FROM peminjaman WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                totalSewa = rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ColorAdjust grayscale = new ColorAdjust();
        grayscale.setSaturation(-1.0);
        grayscale.setBrightness(-0.2);

        String statusText = "Member";
        String badgeFile = "badge_basic.png";

        // --- LEVEL 1 ---
        imgLevel1.setEffect(null);

        // --- LEVEL 2 ---
        if (totalSewa >= 5) {
            imgLevel2.setEffect(null);
            lblLevel2.setText("SILVER");
            lblLevel2.getStyleClass().removeAll("text-locked");
            lblLevel2.getStyleClass().add("text-unlocked");
            lblBenefit2.getStyleClass().removeAll("benefit-badge");
            lblBenefit2.getStyleClass().add("benefit-badge-active");
            statusText = "Silver Member";
            badgeFile = "badge_silver.png";
        } else {
            imgLevel2.setEffect(grayscale);
            lblLevel2.setText("LOCKED");
            lblLevel2.getStyleClass().add("text-locked");
            lblBenefit2.getStyleClass().add("benefit-badge");
        }

        // --- LEVEL 3 ---
        if (totalSewa >= 10) {
            imgLevel3.setEffect(null);
            lblLevel3.setText("GOLD");
            lblLevel3.getStyleClass().removeAll("text-locked");
            lblLevel3.getStyleClass().add("text-unlocked");
            lblBenefit3.getStyleClass().removeAll("benefit-badge");
            lblBenefit3.getStyleClass().add("benefit-badge-active");
            statusText = "Gold Member";
            badgeFile = "badge_gold.png";
        } else {
            imgLevel3.setEffect(grayscale);
            lblLevel3.setText("LOCKED");
            lblLevel3.getStyleClass().add("text-locked");
            lblBenefit3.getStyleClass().add("benefit-badge");
        }

        try {
            if (imgBadgeSmall != null)
                imgBadgeSmall.setImage(
                        new Image(getClass().getResourceAsStream("/img/badge/" + badgeFile)));
            if (lblStatusMember != null) {
                lblStatusMember.setText(statusText);
                if (totalSewa >= 10)
                    lblStatusMember.setStyle(
                            "-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 11px;");
                else if (totalSewa >= 5)
                    lblStatusMember.setStyle(
                            "-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 11px;");
                else
                    lblStatusMember.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
            }
        } catch (Exception e) {
        }

        double progress = (double) totalSewa / 10.0;
        if (progress > 1.0)
            progress = 1.0;
        progressJourney.setProgress(progress);

        if (totalSewa < 5) {
            int sisa = 5 - totalSewa;
            lblNextGoal.setText("Sewa " + sisa + "x lagi untuk buka SILVER (Diskon 5%)");
        } else if (totalSewa < 10) {
            int sisa = 10 - totalSewa;
            lblNextGoal.setText("Sewa " + sisa + "x lagi untuk buka GOLD (Diskon 10%)");
        } else {
            lblNextGoal.setText("🎉 SELAMAT! Anda adalah member SULTAN JavaGear!");
            lblNextGoal.setStyle(
                    "-fx-text-fill: #d35400; -fx-font-weight: bold; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 1);");
        }
    }

    // --- NOTIFIKASI & PESAN ---
    private void cekPesanBaru() {
        int userId = SessionManager.getCurrentUserId();

        // UPDATE QUERY: Sekarang ngecek 'admin_ke_user' DAN 'admin_bonus'
        String sql =
                "SELECT COUNT(*) as jumlah FROM notifikasi WHERE user_id = ? AND is_read = 0 AND tipe IN ('admin_ke_user', 'admin_bonus', 'admin_potong')";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("jumlah") > 0) {
                btnPesan.setText("💬 PESAN BARU!"); // Tombol Merah
                btnPesan.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            } else {
                btnPesan.setText("💬 Pesan"); // Tombol Oranye Biasa
                btnPesan.setStyle(
                        "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCekPesan() {
        int userId = SessionManager.getCurrentUserId();

        // UPDATE QUERY: Ambil pesan tipe 'admin_ke_user' ATAU 'admin_bonus'
        String sqlPesan =
                "SELECT id, pesan FROM notifikasi WHERE user_id = ? AND tipe IN ('admin_ke_user', 'admin_bonus', 'admin_potong') AND is_read = 0 ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sqlPesan)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String pesan = rs.getString("pesan");
                int notifId = rs.getInt("id");

                // Tampilkan Alert
                showAlert("Pesan dari Admin", pesan);

                // Tandai sudah dibaca
                if (!cekApakahMasihAdaUtang(conn, userId)) {
                    try (PreparedStatement upStmt = conn
                            .prepareStatement("UPDATE notifikasi SET is_read = 1 WHERE id = ?")) {
                        upStmt.setInt(1, notifId);
                        upStmt.executeUpdate();
                    }
                    cekPesanBaru(); // Refresh tombol jadi oranye lagi
                    loadDataUserLengkap(); // 🔥 TAMBAHAN: Refresh Saldo di Header biar langsung
                                           // nambah!
                }
            } else {
                showAlert("Info", "Tidak ada pesan baru yang belum dibaca.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean cekApakahMasihAdaUtang(Connection conn, int userId) {
        String sql =
                "SELECT COUNT(*) as jumlah FROM peminjaman WHERE user_id = ? AND status = 'Dipinjam' AND CURRENT_DATE > DATE_ADD(tanggal_pinjam, INTERVAL durasi_hari DAY) AND (status_pembayaran IS NULL OR status_pembayaran != 'SUDAH_BAYAR')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt("jumlah") > 0;
        } catch (Exception e) {
        }
        return false;
    }

    private void cekKeterlambatan() {
        int userId = SessionManager.getCurrentUserId();
        String sql =
                "SELECT COUNT(*) as telat FROM peminjaman WHERE user_id = ? AND status = 'Dipinjam' AND CURRENT_DATE > DATE_ADD(tanggal_pinjam, INTERVAL durasi_hari DAY) AND (status_pembayaran IS NULL OR status_pembayaran != 'SUDAH_BAYAR')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("telat") > 0) {
                showAlert("⚠️ PERINGATAN KETERLAMBATAN!",
                        "Anda memiliki tagihan denda aktif.\nMohon segera lunasi di menu Info Akun.");
            }
        } catch (Exception e) {
        }
    }

    // --- NAVIGATION ---
    @FXML
    private void handleInfoAkun() {
        goToPage("/fxml/user/InfoAkun.fxml");
    }

    @FXML
    private void handleSewaBarang() {
        goToPage("/fxml/user/KatalogSewa.fxml");
    }

    @FXML
    private void handleTopUp() {
        handleInfoAkun();
    }

    @FXML
    private void handleLogout() {
        App.showLogin();
    }

    @FXML
    private void handleHubungiAdmin() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/user/FormHubungiAdmin.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage(); // Sekarang udah bersih karena Stage di-import
            stage.setTitle("Hubungi Admin");
            stage.setScene(new Scene(root)); // Scene aman sekarang
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToPage(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            lblNamaUser.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
