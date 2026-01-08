package com.javagear.controllers;

import com.javagear.models.DatabaseConnection;
import com.javagear.models.Produk;
import com.javagear.models.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

public class CheckoutController {

    @FXML
    private ListView<String> listBarang;
    @FXML
    private DatePicker datePinjam;
    @FXML
    private Spinner<Integer> spinnerDurasi;
    @FXML
    private ComboBox<String> comboMetode;

    @FXML
    private Label lblTotalBayar;
    @FXML
    private Label lblInfoDiskon; 

    private List<Produk> produkDipilih;
    private double totalPerHari = 0;
    private double grandTotal = 0;
    private double diskonPersen = 0; 

    public void initialize() {
        // Setup Spinner (1-30 hari)
        spinnerDurasi.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 1));
        datePinjam.setValue(LocalDate.now());

        // OPSI METODE BAYAR
        comboMetode.setItems(FXCollections.observableArrayList("💳 Saldo Aplikasi (Instan)",
                "📦 Bayar di Tempat (COD)"));

        // Listener buat update total harga realtime
        spinnerDurasi.valueProperty().addListener((obs, oldVal, newVal) -> hitungTotal());

        cekLevelDanDiskon();
    }

    private void cekLevelDanDiskon() {
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

        if (totalSewa >= 10) {
            diskonPersen = 0.10; // Gold 10%
        } else if (totalSewa >= 5) {
            diskonPersen = 0.05; // Silver 5%
        } else {
            diskonPersen = 0.0;
        }
    }

    public void setDataBelanja(List<Produk> items) {
        this.produkDipilih = items;

        for (Produk p : items) {
            listBarang.getItems().add("• " + p.getNama() + " (" + p.getJumlahSewa() + ") @ Rp "
                    + String.format("%,.0f", p.getHargaSewa()));

            totalPerHari += (p.getHargaSewa() * p.getJumlahSewa());
        }

        hitungTotal();
    }

    private void hitungTotal() {
        int durasi = spinnerDurasi.getValue();
        double hargaNormal = totalPerHari * durasi;

        // Hitung Potongan
        double nominalDiskon = hargaNormal * diskonPersen;
        grandTotal = hargaNormal - nominalDiskon;

        // Tampilkan Harga
        if (diskonPersen > 0) {
            String level = (diskonPersen == 0.10) ? "GOLD" : "SILVER";

            lblTotalBayar.setText("Rp " + String.format("%,.0f", grandTotal) + " (Diskon "
                    + (int) (diskonPersen * 100) + "%)");
            lblTotalBayar.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); // Hijau Cuan
        } else {
            lblTotalBayar.setText("Rp " + String.format("%,.0f", grandTotal));
            lblTotalBayar.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleBayar() {
        if (comboMetode.getValue() == null) {
            showAlert("Pilih Metode", "Mau bayar pakai apa bro?");
            return;
        }

        if (datePinjam.getValue().isBefore(LocalDate.now())) {
            showAlert("Tanggal Salah",
                    "Gak bisa sewa di masa lalu bro, yang masa lalu biar mantan aja!");
            return;
        }

        if (comboMetode.getValue().contains("Saldo")) {
            if (!checkSaldoCukup())
                return;
        }

        processTransaction();
    }

    private boolean checkSaldoCukup() {
        double saldoUser = 0;
        int userId = SessionManager.getCurrentUserId();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement("SELECT saldo FROM user WHERE id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                saldoUser = rs.getDouble("saldo");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Cek pake grandTotal yang udah didiskon
        if (saldoUser < grandTotal) {
            showAlert("Saldo Kurang",
                    "Saldo lu cuma Rp " + String.format("%,.0f", saldoUser) + ". Top up dulu gih!");
            return false;
        }
        return true;
    }

    private void processTransaction() {
        int userId = SessionManager.getCurrentUserId();

        boolean isSaldo = comboMetode.getValue().contains("Saldo");
        String metode = isSaldo ? "TRANSFER" : "CASH";
        String statusBayar = isSaldo ? "LUNAS" : "MENUNGGU_PEMBAYARAN";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Kurangi Saldo (Pakai Harga Diskon)
                if (isSaldo) {
                    if (!checkSaldoCukup())
                        return;

                    String sqlSaldo = "UPDATE user SET saldo = saldo - ? WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlSaldo)) {
                        stmt.setDouble(1, grandTotal); // 🔥 POTONG SESUAI HARGA DISKON
                        stmt.setInt(2, userId);
                        stmt.executeUpdate();
                    }
                }

                // 2. Insert Peminjaman
                String sqlSewa =
                        "INSERT INTO peminjaman (user_id, produk_id, tanggal_pinjam, durasi_hari, total_bayar, metode_bayar, status, status_pembayaran, jumlah_barang) VALUES (?, ?, ?, ?, ?, ?, 'Dipinjam', ?, ?)";
                String sqlStok = "UPDATE produk SET stok = stok - ? WHERE id = ?";

                try (PreparedStatement stmtSewa = conn.prepareStatement(sqlSewa);
                        PreparedStatement stmtStok = conn.prepareStatement(sqlStok)) {

                    for (Produk p : produkDipilih) {
                        // Hitung harga per item setelah diskon (proporsional)
                        double hargaItemAsli = p.getHargaSewa() * spinnerDurasi.getValue();
                        double hargaItemDiskon = hargaItemAsli - (hargaItemAsli * diskonPersen);

                        stmtSewa.setInt(1, userId);
                        stmtSewa.setInt(2, p.getId());
                        stmtSewa.setDate(3, java.sql.Date.valueOf(datePinjam.getValue()));
                        stmtSewa.setInt(4, spinnerDurasi.getValue());
                        stmtSewa.setDouble(5, hargaItemDiskon); // 🔥 MASUKIN HARGA DISKON KE DB
                        stmtSewa.setString(6, metode);
                        stmtSewa.setString(7, statusBayar);
                        stmtSewa.setInt(8, p.getJumlahSewa());
                        stmtSewa.executeUpdate();

                        stmtStok.setInt(1, p.getJumlahSewa());
                        stmtStok.setInt(2, p.getId());
                        stmtStok.executeUpdate();
                    }
                }

                conn.commit();
                showAlert("Berhasil", "Pesanan berhasil! "
                        + (diskonPersen > 0 ? "Kamu dapet diskon member! 🎉" : ""));
                ((Stage) lblTotalBayar.getScene().getWindow()).close();

            } catch (Exception e) {
                conn.rollback();
                showAlert("Gagal", "Transaksi error: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBatal() {
        ((Stage) lblTotalBayar.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
