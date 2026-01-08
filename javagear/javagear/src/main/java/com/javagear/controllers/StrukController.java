package com.javagear.controllers;

import com.javagear.models.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.print.PrinterJob;
import javafx.print.PageLayout;
import javafx.scene.transform.Translate;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class StrukController implements Initializable {

    @FXML
    private Label lblIdStruk;
    @FXML
    private Label lblTanggal;
    @FXML
    private Label lblIdPembayaran;
    @FXML
    private Label lblNamaUser;
    @FXML
    private Label lblNamaProduk;
    @FXML
    private Label lblJumlahBarang;
    @FXML
    private Label lblDurasi;
    @FXML
    private Label lblHargaPerHari;
    @FXML
    private Label lblTotalBayar;
    @FXML
    private Label lblMetodeBayar; // Kita akan manipulasi isi label ini
    @FXML
    private Label lblNominalBayar;
    @FXML
    private Label lblKembalian;
    @FXML
    private Label lblStatusPembayaran;
    @FXML
    private VBox boxStruk;
    @FXML
    private Button btnPrint;
    @FXML
    private Button btnClose;

    private int peminjamanId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        generateIdStruk();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        lblTanggal.setText(sdf.format(new Date()));
    }

    public void setDataPeminjaman(int peminjamanId) {
        this.peminjamanId = peminjamanId;
        loadDataPeminjaman();
    }

    private void generateIdStruk() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String idStruk = "STRUK-" + sdf.format(new Date());
        lblIdStruk.setText(idStruk);
    }

    private void loadDataPeminjaman() {
        // Tambah p.metode_bayar_denda, p.denda
        String query = "SELECT "
                + "p.id_pembayaran, u.nama as nama_user, prod.nama as nama_produk, "
                + "p.jumlah_barang, p.durasi_hari, prod.harga_sewa, (prod.harga_sewa * p.jumlah_barang * p.durasi_hari) as real_total_bayar,   "
                + "p.metode_bayar, p.metode_bayar_denda, p.denda, " + // <-- Tambahan
                "p.nominal_bayar, p.kembalian, p.status_pembayaran, " + "p.tanggal_pinjam "
                + "FROM peminjaman p " + "JOIN user u ON p.user_id = u.id "
                + "JOIN produk prod ON p.produk_id = prod.id " + "WHERE p.id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, peminjamanId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                lblIdPembayaran.setText(rs.getString("id_pembayaran"));
                lblNamaUser.setText(rs.getString("nama_user"));
                lblNamaProduk.setText(rs.getString("nama_produk"));
                lblJumlahBarang.setText(rs.getString("jumlah_barang") + " unit");
                lblDurasi.setText(rs.getString("durasi_hari") + " hari");
                lblHargaPerHari.setText(
                        "Rp " + String.format("%,d", (int) rs.getDouble("harga_sewa")) + " /hari");

                // --- LOGIC GABUNGAN METODE BAYAR ---
                String metodeSewa = rs.getString("metode_bayar");
                String metodeDenda = rs.getString("metode_bayar_denda");
                double denda = rs.getDouble("denda");
                double totalBayar = rs.getDouble("real_total_bayar");

                // Format Teks Metode (Biar Rapi)
                String teksSewa = (metodeSewa != null && metodeSewa.contains("CASH")) ? "CASH (COD)"
                        : "SALDO APP";
                String teksDenda =
                        (metodeDenda != null && metodeDenda.contains("CASH")) ? "CASH (COD)"
                                : "SALDO APP";

                if (denda > 0) {
                    // Kalo ada denda, tampilin total gabungan & metode gabungan
                    lblTotalBayar.setText("Rp " + String.format("%,d", (int) (totalBayar + denda)));
                    // Format: "CASH (COD) + SALDO APP (Denda)"
                    lblMetodeBayar.setText(teksSewa + " (Sewa) + " + teksDenda + " (Denda)");
                    // Kecilin font dikit biar muat
                    lblMetodeBayar.setStyle("-fx-font-size: 10px;");
                } else {
                    lblTotalBayar.setText("Rp " + String.format("%,d", (int) totalBayar));
                    lblMetodeBayar.setText(teksSewa);
                }

                lblNominalBayar
                        .setText("Rp " + String.format("%,d", (int) rs.getDouble("nominal_bayar")));
                lblKembalian.setText("Rp " + String.format("%,d", (int) rs.getDouble("kembalian")));

                String status = rs.getString("status_pembayaran");
                lblStatusPembayaran.setText(status);
                if ("LUNAS".equals(status)) {
                    lblStatusPembayaran.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else if ("MENUNGGU_PEMBAYARAN".equals(status)) {
                    lblStatusPembayaran.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                } else {
                    lblStatusPembayaran.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }

                System.out.println("✅ Data struk loaded ID: " + peminjamanId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrint() {
        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob == null) {
            showAlert("❌ ERROR", "Tidak bisa membuat print job.");
            return;
        }
        if (!printerJob.showPrintDialog(btnPrint.getScene().getWindow()))
            return;

        PageLayout pageLayout = printerJob.getJobSettings().getPageLayout();
        double scale = Math.min(pageLayout.getPrintableWidth() / boxStruk.getWidth(),
                pageLayout.getPrintableHeight() / boxStruk.getHeight());
        Translate printTransform =
                new Translate((pageLayout.getPrintableWidth() - (boxStruk.getWidth() * scale)) / 2,
                        (pageLayout.getPrintableHeight() - (boxStruk.getHeight() * scale)) / 2);
        boxStruk.getTransforms().add(printTransform);

        if (printerJob.printPage(boxStruk)) {
            printerJob.endJob();
            showAlert("✅ BERHASIL", "Struk terkirim ke printer!");
        } else {
            showAlert("❌ GAGAL", "Gagal mencetak struk!");
        }

        boxStruk.getTransforms().remove(printTransform);
    }

    @FXML
    private void handleSaveAsText() {
        try {
            StringBuilder strukText = new StringBuilder();
            strukText.append("========================================\n");
            strukText.append("           STRUK PEMBAYARAN\n");
            strukText.append("           JAVA GEAR OUTDOOR\n");
            strukText.append("========================================\n");
            strukText.append("ID Struk      : ").append(lblIdStruk.getText()).append("\n");
            strukText.append("Tanggal       : ").append(lblTanggal.getText()).append("\n");
            strukText.append("ID Pembayaran : ").append(lblIdPembayaran.getText()).append("\n");
            strukText.append("----------------------------------------\n");
            strukText.append("Nama Customer : ").append(lblNamaUser.getText()).append("\n");
            strukText.append("Produk        : ").append(lblNamaProduk.getText()).append("\n");
            strukText.append("Jumlah        : ").append(lblJumlahBarang.getText()).append("\n");
            strukText.append("Durasi        : ").append(lblDurasi.getText()).append("\n");
            strukText.append("Harga/Hari    : ").append(lblHargaPerHari.getText()).append("\n");
            strukText.append("----------------------------------------\n");
            strukText.append("Total Bayar   : ").append(lblTotalBayar.getText()).append("\n");
            // Mengambil teks dari label yang sudah kita format di atas
            strukText.append("Metode Bayar  : ").append(lblMetodeBayar.getText()).append("\n");
            strukText.append("Nominal Bayar : ").append(lblNominalBayar.getText()).append("\n");
            strukText.append("Kembalian     : ").append(lblKembalian.getText()).append("\n");
            strukText.append("Status        : ").append(lblStatusPembayaran.getText()).append("\n");
            strukText.append("========================================\n");
            strukText.append("Terima kasih atas kepercayaan Anda!\n");
            strukText.append("www.javagear-outdoor.com\n");
            strukText.append("========================================\n");

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Simpan Struk (.txt)");
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            fileChooser.setInitialFileName(lblIdStruk.getText() + ".txt");

            File file = fileChooser.showSaveDialog(lblIdStruk.getScene().getWindow());

            if (file != null) {
                try (PrintWriter writer = new PrintWriter(file)) {
                    writer.println(strukText.toString());
                    showAlert("✅ BERHASIL",
                            "Struk berhasil disimpan di:\n" + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) btnClose.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
