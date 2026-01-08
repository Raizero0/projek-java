package com.javagear.controllers;

import com.javagear.App;
import com.javagear.models.DatabaseConnection;
import com.javagear.models.Produk;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class KatalogSewaController {

    @FXML
    private FlowPane gridProduk;
    @FXML
    private Label lblTotalItem;
    @FXML
    private Button btnPesan;
    @FXML
    private HBox boxKategori;

    private List<Produk> keranjangBelanja = new ArrayList<>();
    private String kategoriTerpilih = "SEMUA";

    public void initialize() {
        loadKategoriBar();
        loadKatalogProduk();
        updateTombolPesan();
    }

    private void loadKategoriBar() {
        boxKategori.getChildren().clear();
        Button btnSemua = createCategoryButton("SEMUA");
        btnSemua.getStyleClass().add("btn-category-selected");
        boxKategori.getChildren().add(btnSemua);

        String sql = "SELECT DISTINCT kategori FROM produk ORDER BY kategori";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next())
                boxKategori.getChildren().add(createCategoryButton(rs.getString("kategori")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Button createCategoryButton(String namaKategori) {
        Button btn = new Button(namaKategori);
        btn.getStyleClass().add("btn-category");
        btn.setOnAction(e -> {
            boxKategori.getChildren().forEach(node -> {
                node.getStyleClass().removeAll("btn-category-selected");
                node.getStyleClass().add("btn-category");
            });
            btn.getStyleClass().remove("btn-category");
            btn.getStyleClass().add("btn-category-selected");
            kategoriTerpilih = namaKategori;
            loadKatalogProduk();
        });
        return btn;
    }

    // --- LOGIC LOAD PRODUK (UPDATED: AMBIL SEMUA STOK) ---
    private void loadKatalogProduk() {
        gridProduk.getChildren().clear();
        String sql;

        // Hapus "WHERE stok > 0" biar barang abis tetep muncul
        if ("SEMUA".equals(kategoriTerpilih)) {
            sql = "SELECT * FROM produk ORDER BY nama";
        } else {
            sql = "SELECT * FROM produk WHERE kategori = ? ORDER BY nama";
        }

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (!"SEMUA".equals(kategoriTerpilih))
                stmt.setString(1, kategoriTerpilih);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Produk p = new Produk(rs.getInt("id"), rs.getString("nama"),
                        rs.getString("kategori"), rs.getString("deskripsi"),
                        rs.getDouble("harga_sewa"), rs.getInt("stok"));
                // Cek Stok di sini
                VBox card = createProductCard(p);
                gridProduk.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- LOGIC KARTU (UPDATED: STOCK AWARENESS) ---
    private VBox createProductCard(Produk p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        card.setAlignment(javafx.geometry.Pos.CENTER);

        // --- 1. CEK STOK HABIS ---
        boolean isHabis = p.getStok() <= 0;

        // Gambar
        String imagePath = "/img/produk/" + p.getNama() + ".jpg";
        Image image;
        try {
            if (getClass().getResource(imagePath) != null)
                image = new Image(getClass().getResourceAsStream(imagePath));
            else
                image = new Image(getClass().getResourceAsStream("/img/logo.png"));
        } catch (Exception e) {
            image = new Image(getClass().getResourceAsStream("/img/logo.png"));
        }

        ImageView img = new ImageView(image);
        img.setFitHeight(120);
        img.setFitWidth(150);
        img.setPreserveRatio(true);

        // Kalau habis, gambarnya agak pudar
        if (isHabis)
            img.setOpacity(0.5);

        Label lblNama = new Label(p.getNama());
        lblNama.getStyleClass().add("product-title");

        Label lblHarga = new Label("Rp " + String.format("%,.0f", p.getHargaSewa()) + " /hari");
        lblHarga.getStyleClass().add("product-price");

        // Label Stok (Merah kalau habis)
        Label lblStok = new Label(isHabis ? "❌ STOK HABIS" : "Stok: " + p.getStok());
        if (isHabis) {
            lblStok.setStyle(
                    "-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: #ffebee; -fx-padding: 3 8; -fx-background-radius: 5;");
        } else {
            lblStok.getStyleClass().add("product-stock");
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox boxQty = new HBox(10);
        boxQty.setAlignment(javafx.geometry.Pos.CENTER);

        Button btnMin = new Button("-");
        btnMin.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-min-width: 30;");

        Label lblQty = new Label("0");
        lblQty.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-min-width: 20; -fx-alignment: center;");

        Button btnPlus = new Button("+");
        btnPlus.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-min-width: 30;");

        // Logic Tombol
        if (isHabis) {
            btnMin.setDisable(true);
            btnPlus.setDisable(true);
            card.setStyle("-fx-background-color: #f5f5f5; -fx-opacity: 0.8;");
        } else {
            // Aksi Kurang
            btnMin.setOnAction(e -> {
                int current = p.getJumlahSewa();
                if (current > 0) {
                    p.setJumlahSewa(current - 1);
                    lblQty.setText(String.valueOf(p.getJumlahSewa()));
                    updateCardStyle(card, p.getJumlahSewa());
                    updateKeranjang(p);
                }
            });

            // Aksi Tambah
            btnPlus.setOnAction(e -> {
                int current = p.getJumlahSewa();
                if (current < p.getStok()) {
                    p.setJumlahSewa(current + 1);
                    lblQty.setText(String.valueOf(p.getJumlahSewa()));
                    updateCardStyle(card, p.getJumlahSewa());
                    updateKeranjang(p);
                }
            });
        }

        boxQty.getChildren().addAll(btnMin, lblQty, btnPlus);
        card.getChildren().addAll(img, lblNama, lblHarga, lblStok, spacer, boxQty);

        // Cek kondisi awal (kalau user balik dari kategori lain, qty harus tetep ada)
        if (keranjangBelanja.contains(p)) {
            // Cari item di keranjang yang ID-nya sama, ambil qty-nya
            for (Produk k : keranjangBelanja) {
                if (k.getId() == p.getId()) {
                    p.setJumlahSewa(k.getJumlahSewa());
                    lblQty.setText(String.valueOf(p.getJumlahSewa()));
                    updateCardStyle(card, p.getJumlahSewa());
                }
            }
        }

        return card;
    }

    // 🔥 HELPER: UBAH WARNA CARD (HIJAU / PUTIH)
    private void updateCardStyle(VBox card, int qty) {
        if (qty > 0) {
            card.getStyleClass().removeAll("product-card");
            if (!card.getStyleClass().contains("product-card-selected")) {
                card.getStyleClass().add("product-card-selected");
            }
        } else {
            card.getStyleClass().removeAll("product-card-selected");
            if (!card.getStyleClass().contains("product-card")) {
                card.getStyleClass().add("product-card");
            }
        }
    }

    // 🔥 HELPER: UPDATE LIST KERANJANG
    private void updateKeranjang(Produk p) {
        // Hapus dulu kalau ada (biar gak duplikat)
        keranjangBelanja.removeIf(item -> item.getId() == p.getId());

        // Kalau qty > 0, masukin lagi yang baru
        if (p.getJumlahSewa() > 0) {
            keranjangBelanja.add(p);
        }

        updateTombolPesan();
    }


    private void updateTombolPesan() {
        // 🔥 UPDATE LOGIC: Hitung total unit (Qty), bukan cuma jenis barang
        int totalUnit = 0;
        for (Produk p : keranjangBelanja) {
            totalUnit += p.getJumlahSewa();
        }

        // Update Label Total
        lblTotalItem.setText(totalUnit + " Item");

        // Update Tombol Pesan
        if (totalUnit == 0) {
            btnPesan.setDisable(true);
            btnPesan.setText("PILIH BARANG DULU");
            btnPesan.getStyleClass().removeAll("btn-checkout-active");
            btnPesan.getStyleClass().add("btn-checkout-disabled");
        } else {
            btnPesan.setDisable(false);
            btnPesan.setText("PESAN SEKARANG (" + totalUnit + ")");
            btnPesan.getStyleClass().removeAll("btn-checkout-disabled");
            btnPesan.getStyleClass().add("btn-checkout-active");
        }
    }

    @FXML
    private void handlePesanBarang() {
        try {
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/user/Checkout.fxml"));
            javafx.scene.Parent root = loader.load();
            CheckoutController controller = loader.getController();
            controller.setDataBelanja(keranjangBelanja);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Checkout - JavaGear");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadKatalogProduk();
            keranjangBelanja.clear();
            updateTombolPesan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        App.showUserDashboard();
    }
}
