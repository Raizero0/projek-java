package com.javagear.controllers;

import com.javagear.App;
import com.javagear.models.DatabaseConnection;
import com.javagear.models.Peminjaman;
import com.javagear.models.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class InfoAkunController {

    @FXML
    private Label lblNama, lblUsername, lblEmail, lblTelepon, lblSaldo, lblTotalDenda;
    @FXML
    private TextField fieldTopUp;
    @FXML
    private Button btnBayarDenda;
    @FXML
    private Circle circleProfil;
    @FXML
    private ImageView imgBadgeBig;
    @FXML
    private VBox cardProfil;
    @FXML
    private Label lblStatusMember;

    @FXML
    private TableView<Peminjaman> tabelRiwayat;
    @FXML
    private TableColumn<Peminjaman, String> colBarang, colTanggal, colStatus, colStatusBayar;
    @FXML
    private TableColumn<Peminjaman, Double> colBiayaSewa, colDenda;

    private final String BASE_FOTO_PATH = System.getProperty("user.dir") + File.separator + "src"
            + File.separator + "main" + File.separator + "resources" + File.separator + "img"
            + File.separator + "fotouser" + File.separator;

    public void initialize() {
        loadProfilUser();
        setupTabelRiwayat();
        loadRiwayatSewa();
        cekTagihanDenda();
        loadBadgeAndBorder();
    }

    private void loadProfilUser() {
        int userId = SessionManager.getCurrentUserId();
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                lblNama.setText(rs.getString("nama"));
                lblUsername.setText("@" + rs.getString("username"));
                lblEmail.setText(rs.getString("email"));
                lblTelepon.setText(rs.getString("telepon"));
                lblSaldo.setText("Rp " + String.format("%,.0f", rs.getDouble("saldo")));

                String fotoPathDB = rs.getString("foto_profil");
                if (fotoPathDB == null || fotoPathDB.isEmpty())
                    fotoPathDB = "default.png";

                SessionManager.setCurrentUserFoto(fotoPathDB);
                setCircleImage(fotoPathDB);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBadgeAndBorder() {
        int userId = SessionManager.getCurrentUserId();
        String sql = "SELECT COUNT(*) FROM peminjaman WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int totalSewa = rs.getInt(1);

                // Default Level (Basic)
                String badgeFile = "badge_basic.png";
                Color ringColor = Color.web("#a84100ff");
                String cardStyle =
                        "-fx-background-color: linear-gradient(to bottom right, #8e6932ff, #ffffff); -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(216, 161, 88, 0.4), 20, 0, 0, 0); -fx-padding: 40 30;";
                String statusText = "Member";
                String statusStyle =
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f1930fc7; -fx-background-color: #fffde7; -fx-padding: 5 15; -fx-background-radius: 15; -fx-border-color: #fd6100b1; -fx-border-radius: 15;";

                if (totalSewa >= 10) {
                    // 🥇 GOLD
                    badgeFile = "badge_gold.png";
                    ringColor = Color.web("#f1c40f");
                    cardStyle =
                            "-fx-background-color: linear-gradient(to bottom right, #fff9c4, #ffffff); -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(241, 196, 15, 0.4), 20, 0, 0, 0); -fx-padding: 40 30;";
                    statusText = "GOLD MEMBER";
                    statusStyle =
                            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f1c40f; -fx-background-color: #fffde7; -fx-padding: 5 15; -fx-background-radius: 15; -fx-border-color: #f1c40f; -fx-border-radius: 15;";
                } else if (totalSewa >= 5) {
                    // 🥈 SILVER
                    badgeFile = "badge_silver.png";
                    ringColor = Color.web("#8c8c8cc2");
                    cardStyle =
                            "-fx-background-color: linear-gradient(to bottom right, #b3b3b3ff, #ffffff); -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(128, 128, 128, 0.4), 20, 0, 0, 0); -fx-padding: 40 30;";
                    statusText = "SILVER MEMBER";
                    statusStyle =
                            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d; -fx-background-color: #ecf0f1; -fx-padding: 5 15; -fx-background-radius: 15; -fx-border-color: #bdc3c7a2; -fx-border-radius: 15;";
                }

                try {
                    if (imgBadgeBig != null)
                        imgBadgeBig.setImage(new Image(
                                getClass().getResourceAsStream("/img/badge/" + badgeFile)));
                } catch (Exception e) {
                }

                circleProfil.setStroke(ringColor);
                if (cardProfil != null)
                    cardProfil.setStyle(cardStyle);
                if (lblStatusMember != null) {
                    lblStatusMember.setText(statusText);
                    lblStatusMember.setStyle(statusStyle);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCircleImage(String imagePathDB) {
        circleProfil.setFill(Color.LIGHTGRAY);
        if (imagePathDB == null || imagePathDB.isEmpty())
            imagePathDB = "default.png";
        try {
            Image img = null;
            File f = new File(BASE_FOTO_PATH + imagePathDB);
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    img = new Image(fis);
                }
            }
            if (img == null) {
                String resourcePath = "/img/fotouser/" + imagePathDB.replace("\\", "/");
                if (getClass().getResource(resourcePath) != null) {
                    img = new Image(getClass().getResourceAsStream(resourcePath));
                } else {
                    img = new Image(getClass().getResourceAsStream("/img/fotouser/default.png"));
                }
            }
            if (img != null && !img.isError()) {
                circleProfil.setFill(new ImagePattern(img));
            }
        } catch (Exception e) {
        }
    }

    @FXML
    private void handleUbahProfil() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Galeri Foto Profil");
        dialog.setHeaderText("Pilih, Upload, atau Hapus (Klik Kanan)");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);

        FlowPane flow = new FlowPane();
        flow.setHgap(15);
        flow.setVgap(15);
        flow.setPrefWrapLength(400);
        flow.setStyle("-fx-padding: 20; -fx-alignment: center;");

        String[] defaults = {"default.png", "boy.png", "girl.png"};
        for (String def : defaults) {
            flow.getChildren().add(createAvatarCard(def, true, dialog));
        }

        List<String> userPhotos = getUserPhotos();
        for (String photoName : userPhotos) {
            String relativePath = SessionManager.getCurrentUser() + File.separator + photoName;
            flow.getChildren().add(createAvatarCard(relativePath, false, dialog));
        }

        Button btnUpload = new Button("➕\nUpload");
        btnUpload.setStyle(
                "-fx-pref-width: 80; -fx-pref-height: 80; -fx-background-radius: 10; -fx-cursor: hand; -fx-base: #ecf0f1;");
        btnUpload.setOnAction(e -> {
            handleUploadFoto(dialog);
            dialog.close();
            handleUbahProfil();
        });
        flow.getChildren().add(btnUpload);

        ScrollPane scroll = new ScrollPane(flow);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);
        scroll.setStyle("-fx-background-color: transparent;");

        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    private List<String> getUserPhotos() {
        List<String> photos = new ArrayList<>();
        String username = SessionManager.getCurrentUser();
        File userFolder = new File(BASE_FOTO_PATH + username);
        if (userFolder.exists() && userFolder.isDirectory()) {
            File[] files = userFolder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        photos.add(f.getName());
                    }
                }
            }
        }
        return photos;
    }

    private VBox createAvatarCard(String relativePath, boolean isDefault, Dialog<String> dialog) {
        VBox card = new VBox();
        card.setStyle("-fx-alignment: center; -fx-cursor: hand;");
        ImageView img = new ImageView();
        img.setFitWidth(70);
        img.setFitHeight(70);

        try {
            File f = new File(BASE_FOTO_PATH + relativePath);
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    img.setImage(new Image(fis));
                }
            } else {
                String resPath = "/img/fotouser/" + relativePath.replace("\\", "/");
                if (getClass().getResource(resPath) != null) {
                    img.setImage(new Image(getClass().getResourceAsStream(resPath)));
                } else {
                    img.setImage(
                            new Image(getClass().getResourceAsStream("/img/fotouser/default.png")));
                }
            }
        } catch (Exception e) {
        }

        img.setClip(new Circle(35, 35, 35));
        img.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                updateFotoDatabase(relativePath);
                dialog.close();
            }
        });

        if (!isDefault) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("🗑️ Hapus Foto Ini");
            deleteItem.setOnAction(e -> {
                if (hapusFotoFisik(relativePath)) {
                    dialog.close();
                    handleUbahProfil();
                }
            });
            contextMenu.getItems().add(deleteItem);
            img.setOnContextMenuRequested(
                    e -> contextMenu.show(img, e.getScreenX(), e.getScreenY()));
            Tooltip.install(img, new Tooltip("Klik Kiri: Pilih\nKlik Kanan: Hapus"));
        } else {
            Tooltip.install(img, new Tooltip("Foto Bawaan"));
        }

        card.getChildren().add(img);
        return card;
    }

    private boolean hapusFotoFisik(String relativePath) {
        try {
            File f = new File(BASE_FOTO_PATH + relativePath);
            if (f.exists() && f.delete()) {
                String current = SessionManager.getCurrentUserFoto().replace("\\", "/");
                String deleted = relativePath.replace("\\", "/");
                if (current.equals(deleted)) {
                    updateFotoDatabase("default.png");
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void handleUploadFoto(Dialog<String> dialog) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Foto Profil");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(lblNama.getScene().getWindow());
        if (file != null) {
            try {
                String username = SessionManager.getCurrentUser();
                String ext = file.getName().substring(file.getName().lastIndexOf("."));
                String newFileName = "foto_" + System.currentTimeMillis() + ext;
                File userFolder = new File(BASE_FOTO_PATH + username);
                if (!userFolder.exists()) {
                    userFolder.mkdirs();
                }
                File destFile = new File(userFolder, newFileName);
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                String relativePath = username + File.separator + newFileName;
                updateFotoDatabase(relativePath);
            } catch (Exception e) {
                showAlert("Error", "Gagal upload: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void updateFotoDatabase(String path) {
        int userId = SessionManager.getCurrentUserId();
        String dbPath = path.replace("\\", "/");
        String sql = "UPDATE user SET foto_profil = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dbPath);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            SessionManager.setCurrentUserFoto(dbPath);
            setCircleImage(dbPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTopUp() {
        String nominalStr = fieldTopUp.getText();
        if (nominalStr.isEmpty() || !nominalStr.matches("\\d+")) {
            showAlert("Error", "Angka saja!");
            return;
        }
        double nominal = Double.parseDouble(nominalStr);
        if (nominal < 10000) {
            showAlert("Error", "Min 10.000!");
            return;
        }
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement("UPDATE user SET saldo = saldo + ? WHERE id = ?")) {
            stmt.setDouble(1, nominal);
            stmt.setInt(2, SessionManager.getCurrentUserId());
            if (stmt.executeUpdate() > 0) {
                showAlert("Sukses", "Top Up Berhasil!");
                fieldTopUp.clear();
                loadProfilUser();
            }
        } catch (Exception e) {
        }
    }

    private void cekTagihanDenda() {
        int userId = SessionManager.getCurrentUserId();
        double totalDenda = 0;
        String sql =
                "SELECT p.jumlah_barang, DATEDIFF(CURRENT_DATE, DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY)) as hari_telat FROM peminjaman p  WHERE p.user_id = ? AND p.status = 'Dipinjam' AND CURRENT_DATE > DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY) AND (p.status_pembayaran IS NULL OR p.status_pembayaran != 'SUDAH_BAYAR')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int telat = rs.getInt("hari_telat");
                int qty = rs.getInt("jumlah_barang");
                if (telat > 0)
                    totalDenda += (telat * 15000 * qty);
            }
        } catch (Exception e) {
        }
        lblTotalDenda.setText("Rp " + String.format("%,.0f", totalDenda));
        if (totalDenda > 0) {
            btnBayarDenda.setDisable(false);
            btnBayarDenda.setText("🚨 BAYAR DENDA (Rp " + String.format("%,.0f", totalDenda) + ")");
        } else {
            btnBayarDenda.setDisable(true);
            btnBayarDenda.setText("✅ AMAN");
        }
    }

    @FXML
    private void handleBayarDenda() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/user/BayarDenda.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Bayar Denda");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadProfilUser();
            cekTagihanDenda();
            loadRiwayatSewa();
        } catch (Exception e) {
        }
    }

    @FXML
    private void handleCetakStruk() {
        Peminjaman selected = tabelRiwayat.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Info", "Pilih data dulu!");
            return;
        }
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/user/StrukPembayaran.fxml"));
            Parent root = loader.load();
            StrukController controller = loader.getController();
            controller.setDataPeminjaman(selected.getId());
            Stage stage = new Stage();
            stage.setTitle("Struk");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
        }
    }

    private void setupTabelRiwayat() {
        // 1. Kolom Barang (Nama + Qty)
        colBarang.setCellValueFactory(new PropertyValueFactory<>("namaProduk"));
        colBarang.setCellFactory(column -> new TableCell<Peminjaman, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Peminjaman row = getTableView().getItems().get(getIndex());
                    int qty = row.getJumlahBarang();
                    setText(qty > 1 ? item + " (x" + qty + ")" : item);
                    setStyle("-fx-font-weight: bold; -fx-alignment: center-left;");
                }
            }
        });

        // 2. Kolom Tanggal
        colTanggal.setCellValueFactory(new PropertyValueFactory<>("tanggalPinjam"));
        colTanggal.setStyle("-fx-alignment: center;");

        // 3. KOLOM BIAYA SEWA (BARU)
        colBiayaSewa.setCellValueFactory(new PropertyValueFactory<>("totalBayar")); // Ambil
                                                                                    // real_total_bayar
        colBiayaSewa.setCellFactory(column -> new TableCell<Peminjaman, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Rp " + String.format("%,.0f", item));
                    setStyle("-fx-alignment: center-right; -fx-text-fill: #2C3E50;");
                }
            }
        });

        // 4. KOLOM DENDA (BARU)
        colDenda.setCellValueFactory(new PropertyValueFactory<>("denda"));
        colDenda.setCellFactory(column -> new TableCell<Peminjaman, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item > 0) {
                        setText("Rp " + String.format("%,.0f", item));
                        setStyle(
                                "-fx-alignment: center-right; -fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setText("-");
                        setStyle("-fx-alignment: center; -fx-text-fill: #95a5a6;");
                    }
                }
            }
        });

        // 5. Kolom Status Barang
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setStyle("-fx-alignment: center;");

        // 6. Kolom Status Pembayaran (Logic Warna-Warni)
        colStatusBayar.setCellValueFactory(new PropertyValueFactory<>("statusPembayaran"));
        colStatusBayar.setCellFactory(column -> new TableCell<Peminjaman, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Peminjaman row = getTableView().getItems().get(getIndex());

                    // Logic Data
                    boolean sewaCOD =
                            row.getMetodeBayar() != null && row.getMetodeBayar().contains("CASH");
                    String teksSewa = sewaCOD ? "LUNAS (COD)" : "LUNAS";
                    boolean dendaLunas = "SUDAH_BAYAR".equals(item);
                    boolean dendaCOD = row.getMetodeBayarDenda() != null
                            && row.getMetodeBayarDenda().contains("CASH");

                    // Cek Telat Realtime
                    java.time.LocalDate tglKembali = java.time.LocalDate.now();
                    boolean isLate = false;
                    try {
                        if (row.getTanggalKembali() != null) {
                            tglKembali = java.time.LocalDate.parse(row.getTanggalKembali());
                            if (java.time.LocalDate.now().isAfter(tglKembali)
                                    && "Dipinjam".equals(row.getStatus()))
                                isLate = true;
                        }
                    } catch (Exception e) {
                    }

                    // Tampilan Teks
                    if (dendaLunas) {
                        String teksDenda = dendaCOD ? "Lunas Denda (COD)" : "Lunas Denda";
                        setText(teksSewa + ", " + teksDenda);
                        setStyle(
                                "-fx-text-fill: #2980b9; -fx-font-weight: bold; -fx-font-size: 10px; -fx-alignment: center;");
                    } else if (isLate) {
                        setText("TELAT DENDA");
                        setStyle(
                                "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-alignment: center;");
                    } else if ("MENUNGGU_PEMBAYARAN".equals(item)) {
                        setText("MENUNGGU KONFIRMASI");
                        setStyle(
                                "-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-alignment: center;");
                    } else {
                        setText(teksSewa);
                        setStyle(
                                "-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-alignment: center;");
                    }
                }
            }
        });

        // Row Coloring (Merah kalau telat)
        tabelRiwayat.setRowFactory(tv -> new TableRow<Peminjaman>() {
            @Override
            protected void updateItem(Peminjaman item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    java.time.LocalDate tglKembali = java.time.LocalDate.now();
                    boolean isTelat = false;
                    try {
                        if (item.getTanggalKembali() != null) {
                            tglKembali = java.time.LocalDate.parse(item.getTanggalKembali());
                            if (java.time.LocalDate.now().isAfter(tglKembali)
                                    && "Dipinjam".equals(item.getStatus()))
                                isTelat = true;
                        }
                    } catch (Exception e) {
                    }

                    if (isTelat)
                        setStyle("-fx-background-color: #ffebee;");
                    else
                        setStyle("");
                }
            }
        });
    }

    // FETCH DATA LENGKAP (Termasuk metode_bayar_denda)
    private void loadRiwayatSewa() {
        ObservableList<Peminjaman> list = FXCollections.observableArrayList();
        int userId = SessionManager.getCurrentUserId();
        // Tambah kolom metode_bayar_denda
        String sql = "SELECT p.id, u.id AS user_id, u.nama AS nama_user, prod.nama AS nama_produk, "
                + "p.tanggal_pinjam, DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY) AS tgl_kembali, "
                + "p.status, p.status_pembayaran, p.jumlah_barang, p.metode_bayar, p.metode_bayar_denda, "
                + "(prod.harga_sewa * p.jumlah_barang * p.durasi_hari) AS real_total_bayar, " +

                //  RUMUS DENDA BARU (SAMA KAYAK ADMIN)
                "CASE WHEN p.status_pembayaran = 'SUDAH_BAYAR' OR p.status = 'Dikembalikan' THEN COALESCE(p.denda, 0) "
                + "WHEN p.status = 'Dipinjam' AND CURRENT_DATE > DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY) "
                + "THEN DATEDIFF(CURRENT_DATE, DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY)) * 15000 * p.jumlah_barang "
                + // <-- DIKALI JUMLAH BARANG
                "ELSE 0 END AS total_denda " +

                "FROM peminjaman p " + "JOIN user u ON p.user_id = u.id "
                + "JOIN produk prod ON p.produk_id = prod.id "
                + "WHERE p.user_id = ? ORDER BY p.tanggal_pinjam DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Peminjaman(rs.getInt("id"), rs.getInt("user_id"),
                        rs.getString("nama_user"), rs.getString("nama_produk"),
                        rs.getString("tanggal_pinjam"), rs.getString("tgl_kembali"),
                        rs.getString("status"), rs.getDouble("total_denda"),
                        rs.getString("status_pembayaran"), rs.getInt("jumlah_barang"),
                        rs.getString("metode_bayar"), rs.getString("metode_bayar_denda"),
                        rs.getDouble("real_total_bayar")));
            }
            tabelRiwayat.setItems(list);
        } catch (Exception e) {
        }
    }

    @FXML
    private void handleBack() {
        App.showUserDashboard();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
