package com.javagear.controllers;

import com.javagear.models.DatabaseConnection;
import com.javagear.models.Peminjaman;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ResourceBundle;

public class KonfirmasiPembayaranController implements Initializable {

    @FXML
    private Label lblIdPeminjaman;
    @FXML
    private Label lblNamaUser;
    @FXML
    private Label lblNamaProduk;
    @FXML
    private Label lblTotalBayar;
    @FXML
    private Label lblMetodeBayar;
    @FXML
    private Label lblStatusSebelumnya;
    @FXML
    private TextArea txtCatatanAdmin;
    @FXML
    private Button btnConfirm;
    @FXML
    private Button btnCancel;

    private Peminjaman peminjaman;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();
    }

    public void setDataPeminjaman(Peminjaman peminjaman) {
        this.peminjaman = peminjaman;
        loadDataPeminjaman();
    }

    private void setupEventHandlers() {
        txtCatatanAdmin.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 200) {
                txtCatatanAdmin.setText(oldVal);
            }
        });
    }

    private void loadDataPeminjaman() {
        if (peminjaman != null) {
            lblIdPeminjaman.setText("#" + peminjaman.getId());
            lblNamaUser.setText(peminjaman.getNamaUser());
            lblNamaProduk.setText(peminjaman.getNamaProduk());

            // Tampilkan total bayar sesuai konteks (Denda atau Sewa)
            double nominal = peminjaman.getDenda() > 0 ? peminjaman.getDenda()
                    : getTotalSewaFromDB(peminjaman.getId());
            lblTotalBayar.setText("Rp " + String.format("%,.0f", nominal));

            lblMetodeBayar.setText("CASH (Manual)");
            lblStatusSebelumnya.setText(peminjaman.getStatusPembayaran());

            txtCatatanAdmin.setText("Pembayaran cash telah diterima dan dikonfirmasi oleh admin.");
        }
    }

    private double getTotalSewaFromDB(int id) {
        // Kita hitung ulang pake rumus biar akurat, jangan ambil total_bayar mentah yg isinya cuma
        // harga satuan
        String sql = "SELECT (prod.harga_sewa * p.jumlah_barang * p.durasi_hari) AS real_total "
                + "FROM peminjaman p " + "JOIN produk prod ON p.produk_id = prod.id "
                + "WHERE p.id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            var rs = stmt.executeQuery();
            if (rs.next())
                return rs.getDouble("real_total");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @FXML
    private void handleConfirm() {
        if (!validateInput())
            return;

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            // 🔍 LOGIKA DEWA: Cek Denda atau Sewa Biasa
            boolean isTelat = peminjaman.getDenda() > 0;

            String statusBaru;
            String statusBarang;
            String sqlUpdate;

            if (isTelat) {
                // KASUS BAYAR DENDA -> Update metode_bayar_denda
                statusBaru = "SUDAH_BAYAR";
                statusBarang = "Dikembalikan";
                // Set metode_bayar_denda = 'CASH'
                sqlUpdate =
                        "UPDATE peminjaman SET status_pembayaran = ?, status = ?, denda = ?, metode_bayar_denda = 'CASH' WHERE id = ?";
            } else {
                // KASUS BAYAR SEWA -> Update metode_bayar
                statusBaru = "LUNAS";
                statusBarang = "Dipinjam";
                // UPDATE PENTING: Set metode_bayar = 'CASH'
                sqlUpdate =
                        "UPDATE peminjaman SET status_pembayaran = ?, status = ?, denda = ?, metode_bayar = 'CASH' WHERE id = ?";
            }

            PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate);
            pstmtUpdate.setString(1, statusBaru);
            pstmtUpdate.setString(2, statusBarang);
            pstmtUpdate.setDouble(3, peminjaman.getDenda());
            pstmtUpdate.setInt(4, peminjaman.getId());

            int updated = pstmtUpdate.executeUpdate();

            if (updated > 0) {
                // INSERT NOTIFIKASI
                String sqlNotif =
                        "INSERT INTO notifikasi (user_id, pesan, tipe) VALUES (?, ?, 'admin_ke_user')";
                PreparedStatement pstmtNotif = conn.prepareStatement(sqlNotif);

                int userId = getUserIdByPeminjamanId(peminjaman.getId());
                String pesan = "✅ PEMBAYARAN DIKONFIRMASI (COD)\n\n"
                        + "Admin telah menerima pembayaran tunai Anda:\n" + "📦 Barang: "
                        + peminjaman.getNamaProduk() + "\n" + "💰 Status: " + statusBaru
                        + " (CASH)\n" + "Catatan: " + txtCatatanAdmin.getText();

                pstmtNotif.setInt(1, userId);
                pstmtNotif.setString(2, pesan);
                pstmtNotif.executeUpdate();

                conn.commit();
                showAlert("✅ BERHASIL", "Status berubah menjadi: " + statusBaru + " (COD)");
                closeWindow();
            } else {
                showAlert("❌ GAGAL", "Gagal update status pembayaran!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("❌ ERROR", "Error konfirmasi pembayaran: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private boolean validateInput() {
        if (txtCatatanAdmin.getText().trim().isEmpty()) {
            showAlert("⚠️ PERINGATAN", "Harap isi catatan konfirmasi!");
            return false;
        }
        return true;
    }

    private int getUserIdByPeminjamanId(int peminjamanId) {
        String sql = "SELECT user_id FROM peminjaman WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, peminjamanId);
            var rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt("user_id");
        } catch (Exception e) {
        }
        return -1;
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        ((Stage) btnConfirm.getScene().getWindow()).close();
    }
}
