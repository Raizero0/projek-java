package com.javagear.controllers;

import com.javagear.controllers.BayarDendaController.DendaData;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.print.PrinterJob;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class StrukDendaController implements Initializable {

    @FXML
    private Label lblIdStruk;
    @FXML
    private Label lblTanggal;
    @FXML
    private Label lblIdPembayaran;
    @FXML
    private Label lblTotalDenda;
    @FXML
    private Label lblSudahDibayar;
    @FXML
    private Label lblSisaDenda;
    @FXML
    private Label lblNominalBayar;
    @FXML
    private Label lblMetodeBayar;
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

    private DendaData dendaData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        generateIdStruk();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        lblTanggal.setText(sdf.format(new Date()));
    }

    public void setDataDenda(DendaData dendaData) {
        this.dendaData = dendaData;
        loadDataDenda();
    }

    private void generateIdStruk() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String idStruk = "STRUK-DENDA-" + sdf.format(new Date());
        lblIdStruk.setText(idStruk);
    }

    private void loadDataDenda() {
        if (dendaData != null) {
            lblIdPembayaran.setText(dendaData.getIdPembayaran());
            lblTotalDenda.setText("Rp " + String.format("%,d", (int) dendaData.getTotalDenda()));
            lblSudahDibayar
                    .setText("Rp " + String.format("%,d", (int) dendaData.getSudahDibayar()));
            lblSisaDenda.setText("Rp " + String.format("%,d", (int) dendaData.getSisaDenda()));
            lblNominalBayar
                    .setText("Rp " + String.format("%,d", (int) dendaData.getNominalBayar()));

            String rawMetode = dendaData.getMetode();
            if (rawMetode != null && rawMetode.toUpperCase().contains("CASH")) {
                lblMetodeBayar.setText("CASH (COD)");
            } else {
                lblMetodeBayar.setText("SALDO APP");
            }

            double kembalian = dendaData.getNominalBayar() - dendaData.getSisaDenda();
            lblKembalian.setText("Rp " + String.format("%,d", (int) kembalian));

            String status = dendaData.getStatus();
            lblStatusPembayaran.setText(status);
            if ("LUNAS".equals(status)) {
                lblStatusPembayaran.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else if ("MENUNGGU_PEMBAYARAN".equals(status)) {
                lblStatusPembayaran.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            } else {
                lblStatusPembayaran.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }

            System.out.println("✅ Data struk denda loaded: " + dendaData.getIdPembayaran());
        }
    }

    @FXML
    private void handlePrint() {
        System.out.println("🖨️ Mencetak struk denda...");
        try {
            PrinterJob printerJob = PrinterJob.createPrinterJob();
            if (printerJob != null && printerJob.showPrintDialog(btnPrint.getScene().getWindow())) {
                boolean success = printerJob.printPage(boxStruk);
                if (success) {
                    printerJob.endJob();
                    showAlert("✅ BERHASIL", "Struk denda berhasil dicetak!");
                } else {
                    showAlert("❌ GAGAL", "Gagal mencetak struk denda!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("❌ ERROR", "Error saat mencetak: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveAsText() {
        System.out.println("💾 Menyimpan struk denda sebagai text...");
        try {
            StringBuilder strukText = new StringBuilder();
            strukText.append("========================================\n");
            strukText.append("        STRUK PEMBAYARAN DENDA\n");
            strukText.append("           JAVA GEAR OUTDOOR\n");
            strukText.append("========================================\n");
            strukText.append("ID Struk        : ").append(lblIdStruk.getText()).append("\n");
            strukText.append("Tanggal         : ").append(lblTanggal.getText()).append("\n");
            strukText.append("ID Pembayaran   : ").append(lblIdPembayaran.getText()).append("\n");
            strukText.append("----------------------------------------\n");
            strukText.append("Total Denda     : ").append(lblTotalDenda.getText()).append("\n");
            strukText.append("Sudah Dibayar   : ").append(lblSudahDibayar.getText()).append("\n");
            strukText.append("Sisa Denda      : ").append(lblSisaDenda.getText()).append("\n");
            strukText.append("Nominal Bayar   : ").append(lblNominalBayar.getText()).append("\n");
            // Menggunakan text dari label yang sudah diformat
            strukText.append("Metode Bayar    : ").append(lblMetodeBayar.getText()).append("\n");
            strukText.append("Kembalian       : ").append(lblKembalian.getText()).append("\n");
            strukText.append("Status          : ").append(lblStatusPembayaran.getText())
                    .append("\n");
            strukText.append("========================================\n");
            strukText.append("Terima kasih telah melunasi denda!\n");
            strukText.append("www.javagear-outdoor.com\n");
            strukText.append("========================================\n");

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Simpan Struk Denda (.txt)");
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            fileChooser.setInitialFileName(lblIdStruk.getText() + ".txt");

            File file = fileChooser.showSaveDialog(lblIdStruk.getScene().getWindow());

            if (file != null) {
                try (PrintWriter writer = new PrintWriter(file)) {
                    writer.println(strukText.toString());
                    showAlert("✅ BERHASIL", "Struk denda disimpan di:\n" + file.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("❌ ERROR", "Error saat menyimpan struk: " + e.getMessage());
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
