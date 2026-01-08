package com.javagear.controllers;

import com.javagear.App;
import com.javagear.models.DatabaseConnection;
import com.javagear.models.Produk;
import com.javagear.models.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminBarangViewController {

    @FXML
    private Label lblNamaAdmin;
    @FXML
    private Circle circleFotoAdmin;

    @FXML
    private TableView<Produk> tabelBarang;
    @FXML
    private TableColumn<Produk, Integer> colId;
    @FXML
    private TableColumn<Produk, String> colNama;
    @FXML
    private TableColumn<Produk, String> colKategori;
    @FXML
    private TableColumn<Produk, Double> colHarga;
    @FXML
    private TableColumn<Produk, Integer> colStok;

    @FXML
    private TextField fieldNama;
    @FXML
    private TextField fieldHarga;
    @FXML
    private TextField fieldStok;
    @FXML
    private TextArea fieldDeskripsi;
    @FXML
    private ComboBox<String> comboKategori;

    @FXML
    private ImageView imgPreview;
    @FXML
    private Label lblFileInfo;

    private int selectedBarangId = 0;
    private File selectedImageFile;

    private String originalBarangName = "";

    //Visibility
    public void initialize() {
        setupProfilAdmin();
        setupTabel();
        loadDataBarang();
        loadKategori();
    }

    //visibility
    private void setupProfilAdmin() {
        lblNamaAdmin.setText(SessionManager.getCurrentUserName());
        try {
            circleFotoAdmin.setFill(new ImagePattern(
                    new Image(getClass().getResourceAsStream("/img/foto_admin.jpg"))));
        } catch (Exception e) {
        }
    }

    private void setupTabel() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        colKategori.setCellValueFactory(new PropertyValueFactory<>("kategori"));
        colHarga.setCellValueFactory(new PropertyValueFactory<>("hargaSewa"));
        colStok.setCellValueFactory(new PropertyValueFactory<>("stok"));

        colHarga.setCellFactory(col -> new TableCell<Produk, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("Rp %,.0f", item));
            }
        });

        tabelBarang.setRowFactory(tv -> new TableRow<Produk>() {
            @Override
            //Visibility
            protected void updateItem(Produk item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    int stok = item.getStok();
                    if (stok == 0) {
                        setStyle(
                                "-fx-background-color: #ffcdd2; -fx-text-background-color: #c62828;");
                    } else if (stok <= 3) {
                        setStyle(
                                "-fx-background-color: #fff9c4; -fx-text-background-color: #f57f17;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        tabelBarang.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null)
                        isiForm(newVal);
                });
    }

    private void isiForm(Produk p) {
        selectedBarangId = p.getId();
        fieldNama.setText(p.getNama());
        comboKategori.setValue(p.getKategori());
        fieldHarga.setText(String.valueOf((int) p.getHargaSewa()));
        fieldStok.setText(String.valueOf(p.getStok()));
        fieldDeskripsi.setText(p.getDeskripsi());

        originalBarangName = p.getNama();

        // untuk load gambar
        try {
            String imagePath = "/img/produk/" + p.getNama() + ".jpg";
            if (getClass().getResource(imagePath) != null) {
                String imageUrl = getClass().getResource(imagePath).toExternalForm();
                imgPreview.setImage(new Image(imageUrl + "?" + System.currentTimeMillis()));
                lblFileInfo.setText("Gambar: " + p.getNama() + ".jpg");
            } else {
                imgPreview.setImage(new Image(getClass().getResourceAsStream("/img/logo.png")));
                lblFileInfo.setText("Belum ada gambar.");
            }
        } catch (Exception e) {
            lblFileInfo.setText("Gagal load gambar.");
        }

        selectedImageFile = null;
    }

    private void loadDataBarang() {
        ObservableList<Produk> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM produk ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Produk(rs.getInt("id"), rs.getString("nama"), rs.getString("kategori"),
                        rs.getString("deskripsi"), rs.getDouble("harga_sewa"), rs.getInt("stok")));
            }
            tabelBarang.setItems(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadKategori() {
        ObservableList<String> listKat = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT kategori FROM produk ORDER BY kategori";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next())
                listKat.add(rs.getString("kategori"));
            comboKategori.setItems(listKat);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePilihGambar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Gambar Produk");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(lblNamaAdmin.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            imgPreview.setImage(new Image(file.toURI().toString()));
            lblFileInfo.setText("File baru: " + file.getName());
        }
    }

    // LOGIC SIMPAN + PROTEKSI FILE LAMA
    private void saveImageToResources(String namaBarang, boolean deleteOldFile) {
        try {
            String userDir = System.getProperty("user.dir");
            File destFolder = new File(userDir + "/src/main/resources/img/produk/");
            if (!destFolder.exists())
                destFolder.mkdirs();

            // 1. HAPUS FILE LAMA JIKA DIMINTA (Ganti Nama)
            if (deleteOldFile && originalBarangName != null && !originalBarangName.isEmpty()) {
                File oldFile = new File(destFolder, originalBarangName + ".jpg");
                if (oldFile.exists()) {
                    oldFile.delete();
                    System.out.println("🗑️ Gambar lama dihapus: " + oldFile.getName());
                }
            }

            // 2. SIMPAN FILE BARU (Jika ada upload)
            if (selectedImageFile != null) {
                File destFile = new File(destFolder, namaBarang + ".jpg");
                // Timpa kalau ada
                Files.copy(selectedImageFile.toPath(), destFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Gambar baru disimpan: " + destFile.getName());
            }
            // 3. JIKA GANTI NAMA TAPI GAMBAR SAMA (RENAME FILE)
            else if (deleteOldFile) { // Artinya cuma ganti nama doang
                File oldFile = new File(destFolder, originalBarangName + ".jpg");
                File newFile = new File(destFolder, namaBarang + ".jpg");
                if (oldFile.exists()) {
                    oldFile.renameTo(newFile);
                    System.out.println("🔄 Gambar di-rename: " + oldFile.getName() + " -> "
                            + newFile.getName());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Warning", "Gagal proses gambar: " + e.getMessage());
        }
    }

    @FXML
    private void handleSimpan() {
        if (!validateInput())
            return;
        String sql =
                "INSERT INTO produk (nama, kategori, harga_sewa, stok, deskripsi) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fieldNama.getText());
            stmt.setString(2, comboKategori.getValue());
            stmt.setDouble(3, Double.parseDouble(fieldHarga.getText()));
            stmt.setInt(4, Integer.parseInt(fieldStok.getText()));
            stmt.setString(5, fieldDeskripsi.getText());
            stmt.executeUpdate();

            if (selectedImageFile != null) {
                saveImageToResources(fieldNama.getText(), false);
            }

            showAlert("Sukses", "Barang berhasil ditambahkan!");
            handleRefresh();
        } catch (Exception e) {
            showAlert("Error", "Gagal simpan: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedBarangId == 0)
            return;
        if (!validateInput())
            return;

        String sql =
                "UPDATE produk SET nama=?, kategori=?, harga_sewa=?, stok=?, deskripsi=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            String namaBaru = fieldNama.getText();

            stmt.setString(1, namaBaru);
            stmt.setString(2, comboKategori.getValue());
            stmt.setDouble(3, Double.parseDouble(fieldHarga.getText()));
            stmt.setInt(4, Integer.parseInt(fieldStok.getText()));
            stmt.setString(5, fieldDeskripsi.getText());
            stmt.setInt(6, selectedBarangId);
            stmt.executeUpdate();

            boolean isNameChanged = !namaBaru.equals(originalBarangName);

            if (isNameChanged || selectedImageFile != null) {
                saveImageToResources(namaBaru, isNameChanged);
            }

            showAlert("Sukses", "Data barang berhasil diupdate!");
            handleRefresh();
        } catch (Exception e) {
            showAlert("Error", "Gagal update: " + e.getMessage());
        }
    }

    @FXML
    private void handleHapus() {
        if (selectedBarangId == 0)
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Hapus Barang");
        alert.setHeaderText("Hapus " + fieldNama.getText() + "?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt =
                            conn.prepareStatement("DELETE FROM produk WHERE id=?")) {
                stmt.setInt(1, selectedBarangId);
                stmt.executeUpdate();

                try {
                    String userDir = System.getProperty("user.dir");
                    File img = new File(userDir + "/src/main/resources/img/produk/"
                            + fieldNama.getText() + ".jpg");
                    if (img.exists())
                        img.delete();
                } catch (Exception ex) {
                }

                showAlert("Sukses", "Barang dihapus.");
                handleRefresh();
            } catch (Exception e) {
                showAlert("Gagal", "Barang tidak bisa dihapus (Masih ada di history).");
            }
        }
    }

    @FXML
    private void handleClear() {
        fieldNama.clear();
        fieldHarga.clear();
        fieldStok.clear();
        fieldDeskripsi.clear();
        comboKategori.setValue(null);
        comboKategori.setPromptText("Pilih / Ketik Baru");
        selectedBarangId = 0;
        tabelBarang.getSelectionModel().clearSelection();
        imgPreview.setImage(new Image(getClass().getResourceAsStream("/img/logo.png")));
        lblFileInfo.setText("Belum ada gambar");
        selectedImageFile = null;
        originalBarangName = ""; 
    }

    @FXML
    private void handleRefresh() {
        loadDataBarang();
        loadKategori();
        handleClear();
    }

    @FXML
    private void handleBackToMenu() {
        App.showAdminDashboard();
    }

    private boolean validateInput() {
        if (fieldNama.getText().isEmpty() || fieldHarga.getText().isEmpty()
                || fieldStok.getText().isEmpty() || comboKategori.getValue() == null) {
            showAlert("Error", "Data wajib belum lengkap!");
            return false;
        }
        return true;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
