package com.javagear.controllers;

import com.javagear.models.DatabaseConnection;
import com.javagear.models.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.UUID;

public class BayarDendaController implements Initializable {

    @FXML
    private Label lblTotalDenda, lblSisaDenda, lblKembalian, lblError, lblIdPembayaran,
            lblInfoDenda;
    @FXML
    private ComboBox<String> comboMetodeBayar;

    @FXML
    private ListView<String> listDetailDenda;

    @FXML
    private TextField txtNominalBayar;
    @FXML
    private ProgressBar progressDenda;

    private double totalDenda, sudahDibayar, sisaDenda;
    private String infoBarangTelat = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupComboBox();
        setupListeners();
        generateIdPembayaran();
        loadDataDenda();
    }

    private void setupComboBox() {
        ObservableList<String> metodeBayar = FXCollections
                .observableArrayList("TRANSFER - Saldo Aplikasi", "CASH - Bayar di Tempat");
        comboMetodeBayar.setItems(metodeBayar);
        comboMetodeBayar.getSelectionModel().selectFirst();
    }

    private void setupListeners() {
        txtNominalBayar.textProperty()
                .addListener((obs, oldVal, newVal) -> calculateKembalianDenda());
        comboMetodeBayar.valueProperty()
                .addListener((obs, oldVal, newVal) -> onMetodeBayarChangedDenda());
    }

    private void generateIdPembayaran() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String idStruk = "DND-" + sdf.format(new Date()) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        lblIdPembayaran.setText(idStruk);
    }

    private void loadDataDenda() {
        int userId = SessionManager.getCurrentUserId();

        // 1. Hitung Total & Isi List
        totalDenda = hitungTotalDenda(userId);

        // 2. Setup Variable
        sudahDibayar = 0;
        sisaDenda = totalDenda;

        // 3. Update UI
        updateUIDenda();
    }

    private double hitungTotalDenda(int userId) {
        double total = 0;
        StringBuilder rincianBarang = new StringBuilder();
        ObservableList<String> itemsList = FXCollections.observableArrayList();

        String sql =
                "SELECT p.jumlah_barang, prod.nama, DATEDIFF(CURRENT_DATE, DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY)) as hari_telat "
                        + "FROM peminjaman p " + "JOIN produk prod ON p.produk_id = prod.id "
                        + "WHERE p.user_id = ? " + "AND p.status = 'Dipinjam' "
                        + "AND CURRENT_DATE > DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY) "
                        + "AND (p.status_pembayaran IS NULL OR p.status_pembayaran != 'SUDAH_BAYAR')";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                int hariTelat = rs.getInt("hari_telat");
                String namaBarang = rs.getString("nama");
                int qty = rs.getInt("jumlah_barang");

                if (hariTelat > 0) {
                    double dendaItem = hariTelat * 15000 * qty;
                    total += dendaItem;

                    // 1. Masukin ke Label Kuning (Logic Lama)
                    if (count < 2) {
                        rincianBarang.append("• ").append(namaBarang).append(" (Telat ")
                                .append(hariTelat).append(" hari)\n");
                    } else if (count == 2) {
                        rincianBarang.append("• Dan lainnya...\n");
                    }
                    count++;

                    String detailList = String.format("• %s (Telat %d hari) - Rp %,.0f", namaBarang,
                            hariTelat, dendaItem);
                    itemsList.add(detailList);
                }
            }

            this.infoBarangTelat = rincianBarang.toString();

            // Set Items ke ListView (Cek null dulu biar aman)
            if (listDetailDenda != null) {
                listDetailDenda.setItems(itemsList);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    private void updateUIDenda() {
        lblTotalDenda.setText(String.format("Rp %,d", (int) totalDenda));
        lblSisaDenda.setText(String.format("Rp %,d", (int) sisaDenda));

        double progress = (totalDenda > 0) ? (sudahDibayar / totalDenda) : 0;
        progressDenda.setProgress(progress);

        if (sisaDenda <= 0) {
            lblInfoDenda.setText("✅ TIDAK ADA DENDA.");
            txtNominalBayar.setDisable(true);
            txtNominalBayar.setText("0");
        } else {
            lblInfoDenda.setText("⚠️ Keterlambatan pada:\n" + infoBarangTelat);
            txtNominalBayar.setText(String.valueOf((int) sisaDenda));
        }
        calculateKembalianDenda();
    }

    private void calculateKembalianDenda() {
        try {
            double nominal = Double.parseDouble(txtNominalBayar.getText().replace(",", ""));
            double kembalian = nominal - sisaDenda;
            lblKembalian
                    .setText(kembalian >= 0 ? String.format("Rp %,d", (int) kembalian) : "Rp 0");
            lblKembalian.setTextFill(kembalian >= 0 ? Color.GREEN : Color.RED);
        } catch (Exception e) {
            lblKembalian.setText("Rp 0");
        }
    }

    private void onMetodeBayarChangedDenda() {
        if (comboMetodeBayar.getValue() != null
                && comboMetodeBayar.getValue().contains("TRANSFER")) {
            txtNominalBayar.setText(String.valueOf((int) sisaDenda));
            txtNominalBayar.setDisable(true);
        } else {
            txtNominalBayar.setDisable(false);
        }
    }

    @FXML
    private void handleBayarDenda() {
        lblError.setVisible(false);
        if (sisaDenda <= 0) {
            lblError.setText("Tidak ada tagihan!");
            lblError.setVisible(true);
            return;
        }

        try {
            double nominalBayar = Double.parseDouble(txtNominalBayar.getText().replace(",", ""));
            String metode = comboMetodeBayar.getValue().contains("TRANSFER") ? "TRANSFER" : "CASH";
            int userId = SessionManager.getCurrentUserId();

            if ("TRANSFER".equals(metode)) {
                double saldoUser = getSaldoUser(userId);
                if (saldoUser < nominalBayar) {
                    lblError.setText("❌ SALDO TIDAK CUKUP! (Sa ldo: Rp "
                            + String.format("%,.0f", saldoUser) + ")");
                    lblError.setVisible(true);
                    return;
                }
            }

            // TRANSAKSI
            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                conn.setAutoCommit(false); // Start Transaction

                try {
                    String statusPembayaran =
                            "TRANSFER".equals(metode) ? "SUDAH_BAYAR" : "MENUNGGU_PEMBAYARAN";

                    // A. POTONG SALDO (Jika Transfer)
                    if ("TRANSFER".equals(metode)) {
                        potongSaldoUser(conn, userId, nominalBayar);
                    }

                    // B. UPDATE STATUS BARANG & DENDA (Bulk Update)
                    updateStatusPeminjaman(conn, userId, statusPembayaran, metode);

                    // C. CATAT RIWAYAT NOTIFIKASI
                    insertNotifikasi(conn, userId, nominalBayar, metode, statusPembayaran);

                    conn.commit(); // COMMIT SUKSES

                    // Tampilkan Struk
                    showStrukDenda(nominalBayar, metode, statusPembayaran);

                } catch (Exception e) {
                    conn.rollback(); // ROLLBACK KALAU ERROR
                    throw e;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Error: " + e.getMessage());
            lblError.setVisible(true);
        }
    }

    // --- HELPER METHODS ---

    private double getSaldoUser(int userId) {
        String sql = "SELECT saldo FROM user WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getDouble("saldo");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    //  LOGIC POTONG SALDO
    private void potongSaldoUser(Connection conn, int userId, double amount) throws SQLException {
        String sql = "UPDATE user SET saldo = saldo - ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            System.out.println("💰 Saldo terpotong: Rp " + amount);
        }
    }

    private void updateStatusPeminjaman(Connection conn, int userId, String statusPembayaran,
            String metodeBayar) throws SQLException {

        String sqlCekBarang = "SELECT produk_id, jumlah_barang FROM peminjaman "
                + "WHERE user_id = ? AND status = 'Dipinjam' "
                + "AND CURRENT_DATE > DATE_ADD(tanggal_pinjam, INTERVAL durasi_hari DAY)";

        try (PreparedStatement stmtCek = conn.prepareStatement(sqlCekBarang)) {
            stmtCek.setInt(1, userId);
            ResultSet rs = stmtCek.executeQuery();

            // Siapkan query buat balikin stok ke gudang
            String sqlRestock = "UPDATE produk SET stok = stok + ? WHERE id = ?";

            try (PreparedStatement stmtRestock = conn.prepareStatement(sqlRestock)) {
                while (rs.next()) {
                    int pId = rs.getInt("produk_id");
                    int qty = rs.getInt("jumlah_barang");

                    // Eksekusi balikin stok
                    stmtRestock.setInt(1, qty);
                    stmtRestock.setInt(2, pId);
                    stmtRestock.executeUpdate();
                }
            }
        }

        String sqlUpdate = "UPDATE peminjaman SET " + "status_pembayaran = ?, "
                + "status = 'Dikembalikan', " + "metode_bayar_denda = ?, " +

                // Hitung & Simpan nominal denda final biar tercatat di history
                "denda = DATEDIFF(CURRENT_DATE, DATE_ADD(tanggal_pinjam, INTERVAL durasi_hari DAY)) * 15000 * jumlah_barang "
                +

                "WHERE user_id = ? " + "AND status = 'Dipinjam' "
                + "AND CURRENT_DATE > DATE_ADD(tanggal_pinjam, INTERVAL durasi_hari DAY)";

        try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
            stmt.setString(1, statusPembayaran);

            // Tentukan metode bayar (Transfer/Cash)
            String metodeDB = "TRANSFER".equals(metodeBayar) ? "TRANSFER" : "CASH";
            stmt.setString(2, metodeDB);

            stmt.setInt(3, userId);

            int updated = stmt.executeUpdate();
            System.out.println("✅ Sukses: " + updated
                    + " transaksi diselesaikan (Stok Kembali + Denda Lunas + Metode: " + metodeDB
                    + ")");
        }
    }

    private void insertNotifikasi(Connection conn, int userId, double nominal, String metode,
            String status) throws SQLException {
        String sql =
                "INSERT INTO notifikasi (user_id, pesan, tipe, total_denda, status_bayar) VALUES (?, ?, 'user_ke_admin', ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            String pesan = "💰 PEMBAYARAN DENDA\nID: " + lblIdPembayaran.getText() + "\nBayar: Rp "
                    + nominal;
            stmt.setInt(1, userId);
            stmt.setString(2, pesan);
            stmt.setDouble(3, nominal);
            stmt.setString(4, status);
            stmt.executeUpdate();
        }
    }

    private void showStrukDenda(double nominal, String metode, String status) {
        try {
            DendaData data = new DendaData(lblIdPembayaran.getText(), totalDenda, 0, 0, nominal,
                    metode, status);
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/user/StrukDenda.fxml"));
            Parent root = loader.load();
            StrukDendaController controller = loader.getController();
            controller.setDataDenda(data);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
            ((Stage) lblTotalDenda.getScene().getWindow()).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBatal() {
        ((Stage) lblTotalDenda.getScene().getWindow()).close();
    }

    // Inner class DendaData (sama kayak sebelumnya)
    public static class DendaData {
        private String idPembayaran, metode, status;
        private double totalDenda, sudahDibayar, sisaDenda, nominalBayar;

        public DendaData(String id, double tot, double sdh, double sis, double nom, String met,
                String stat) {
            this.idPembayaran = id;
            this.totalDenda = tot;
            this.sudahDibayar = sdh;
            this.sisaDenda = sis;
            this.nominalBayar = nom;
            this.metode = met;
            this.status = stat;
        }

        public String getIdPembayaran() {
            return idPembayaran;
        }

        public double getTotalDenda() {
            return totalDenda;
        }

        public double getSudahDibayar() {
            return sudahDibayar;
        }

        public double getSisaDenda() {
            return sisaDenda;
        }

        public double getNominalBayar() {
            return nominalBayar;
        }

        public String getMetode() {
            return metode;
        }

        public String getStatus() {
            return status;
        }
    }
}
