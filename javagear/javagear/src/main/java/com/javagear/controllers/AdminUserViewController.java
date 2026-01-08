package com.javagear.controllers;

import com.javagear.App;
import com.javagear.models.DatabaseConnection;
import com.javagear.models.Peminjaman;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.FileOutputStream;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;

public class AdminUserViewController {

    @FXML
    private ComboBox<String> comboBulan;
    @FXML
    private ComboBox<String> comboTahun;
    @FXML
    private Label lblNamaAdmin;
    @FXML
    private Circle circleFotoAdmin;
    @FXML
    private Label lblPendapatan;
    @FXML
    private Label lblPendapatanCOD;
    @FXML
    private FlowPane gridUser;
    @FXML
    private Label lblTotalUser;

    private final String BASE_FOTO_PATH = System.getProperty("user.dir") + File.separator + "src"
            + File.separator + "main" + File.separator + "resources" + File.separator + "img"
            + File.separator + "fotouser" + File.separator;

    @FXML
    private TableView<Peminjaman> tabelPeminjaman;
    @FXML
    private Button btnFilterSemua, btnFilterMenunggu, btnFilterLunas, btnFilterDendaLunas,
            btnFilterTelat;
    @FXML
    private Button btnCetakLaporan;

    @FXML
    private TableColumn<Peminjaman, String> colId, colUser, colBarang, colTanggal, colKembali,
            colStatus, colStatusBayar;
    @FXML
    private TableColumn<Peminjaman, Double> colDenda, colBiayaSewa;
    @FXML
    private Button btnKonfirmasi, btnPeringatan, btnEditTanggal, btnArsipChat, btnTerima;

    public void initialize() {
        setupProfilAdmin();
        setupFilterWaktu();
        loadDataUser();
        setupTabel();
        loadDataPeminjaman();
        loadPendapatan();
    }

    private void setupFilterWaktu() {
        // 1. Setup Bulan
        ObservableList<String> bulanList = FXCollections.observableArrayList("SEMUA", "Januari",
                "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September",
                "Oktober", "November", "Desember");
        comboBulan.setItems(bulanList);
        comboBulan.getSelectionModel().selectFirst();

        // 2. Setup Tahun (Ambil Dinamis dari DB)
        ObservableList<String> tahunList = FXCollections.observableArrayList("SEMUA");
        // Ambil tahun yang ada di database aja
        String sqlTahun = "SELECT DISTINCT YEAR(tanggal_pinjam) FROM peminjaman ORDER BY 1 DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sqlTahun);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tahunList.add(rs.getString(1));
            }
        } catch (Exception e) {
        }
        comboTahun.setItems(tahunList);
        comboTahun.getSelectionModel().selectFirst();

        // 3. Listener: Kalau Bulan/Tahun ganti -> Refresh Filter yang SEDANG AKTIF
        comboBulan.setOnAction(e -> reapplyCurrentFilter());
        comboTahun.setOnAction(e -> reapplyCurrentFilter());
    }

    @FXML
    private void handleResetFilter() {
        comboBulan.getSelectionModel().selectFirst();
        comboTahun.getSelectionModel().selectFirst();

        filterSemua();
    }

    private void reapplyCurrentFilter() {
        if (btnFilterLunas.getStyleClass().contains("btn-hijau-solid"))
            filterLunas();
        else if (btnFilterMenunggu.getStyleClass().contains("btn-kuning-solid"))
            filterMenunggu();
        else if (btnFilterDendaLunas.getStyleClass().contains("btn-biru-solid"))
            filterDendaLunas();
        else if (btnFilterTelat.getStyleClass().contains("btn-merah-solid"))
            filterTelat();
        else
            filterSemua();
    }

    private void setupProfilAdmin() {
        lblNamaAdmin.setText("Administrator");
        try {
            circleFotoAdmin.setFill(new ImagePattern(
                    new Image(getClass().getResourceAsStream("/img/foto_admin.jpg"))));
        } catch (Exception e) {
        }
    }

    @FXML
    private void handleRefresh() {
        System.out.println("🔄 Mereload semua data...");
        loadDataUser();
        loadPendapatan();

        String currentTahun = comboTahun.getValue();

        ObservableList<String> tahunList = FXCollections.observableArrayList("SEMUA");
        String sqlTahun = "SELECT DISTINCT YEAR(tanggal_pinjam) FROM peminjaman ORDER BY 1 DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sqlTahun);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tahunList.add(rs.getString(1));
            }
        } catch (Exception e) {
        }

        comboTahun.setItems(tahunList);

        if (currentTahun != null && tahunList.contains(currentTahun)) {
            comboTahun.setValue(currentTahun);
        } else {
            comboTahun.getSelectionModel().selectFirst();
        }

        loadDataPeminjaman();
    }

    @FXML
    private void handleBackToMenu() {
        App.showAdminDashboard();
    }

    private void loadPendapatan() {
        // 1. Hitung Saldo Aplikasi (Transfer)
        // Rumus: (Total Sewa Non-COD) + (Total Denda Non-COD)
        String sqlSaldoSewa = "SELECT SUM(prod.harga_sewa * p.jumlah_barang * p.durasi_hari) "
                + "FROM peminjaman p JOIN produk prod ON p.produk_id = prod.id "
                + "WHERE (p.status_pembayaran = 'SUDAH_BAYAR' OR p.status_pembayaran = 'LUNAS') "
                + "AND p.metode_bayar != 'CASH'";
        String sqlSaldoDenda = "SELECT SUM(p.denda) FROM peminjaman p "
                + "WHERE p.status_pembayaran = 'SUDAH_BAYAR' AND p.metode_bayar_denda != 'CASH'"; // Denda
                                                                                                  // via
                                                                                                  // Transfer

        // 2. Hitung COD (Uang Fisik)
        // Rumus: (Total Sewa COD) + (Total Denda COD)
        String sqlCODSewa = "SELECT SUM(prod.harga_sewa * p.jumlah_barang * p.durasi_hari) "
                + "FROM peminjaman p JOIN produk prod ON p.produk_id = prod.id "
                + "WHERE (p.status_pembayaran = 'SUDAH_BAYAR' OR p.status_pembayaran = 'LUNAS') "
                + "AND p.metode_bayar = 'CASH'";
        String sqlCODDenda = "SELECT SUM(p.denda) FROM peminjaman p "
                + "WHERE p.status_pembayaran = 'SUDAH_BAYAR' AND p.metode_bayar_denda = 'CASH'"; // Denda
                                                                                                 // via
                                                                                                 // COD

        // 3. Pengeluaran
        String sqlKeluar = "SELECT SUM(nominal) FROM notifikasi WHERE tipe = 'admin_bonus'";
        String sqlMasuk = "SELECT SUM(nominal) FROM notifikasi WHERE tipe = 'admin_potong'";

        double totalSaldoApp = 0;
        double totalUangFisik = 0;
        double totalBonus = 0;
        double totalPotong = 0;

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // -- HITUNG SALDO APP --
            try (PreparedStatement s1 = conn.prepareStatement(sqlSaldoSewa);
                    ResultSet r1 = s1.executeQuery()) {
                if (r1.next())
                    totalSaldoApp += r1.getDouble(1);
            }
            try (PreparedStatement s2 = conn.prepareStatement(sqlSaldoDenda);
                    ResultSet r2 = s2.executeQuery()) {
                if (r2.next())
                    totalSaldoApp += r2.getDouble(1);
            }

            // -- HITUNG UANG FISIK (COD) --
            try (PreparedStatement s3 = conn.prepareStatement(sqlCODSewa);
                    ResultSet r3 = s3.executeQuery()) {
                if (r3.next())
                    totalUangFisik += r3.getDouble(1);
            }
            try (PreparedStatement s4 = conn.prepareStatement(sqlCODDenda);
                    ResultSet r4 = s4.executeQuery()) {
                if (r4.next())
                    totalUangFisik += r4.getDouble(1);
            }

            // -- HITUNG PENGELUARAN --
            try (PreparedStatement s5 = conn.prepareStatement(sqlKeluar);
                    ResultSet r5 = s5.executeQuery()) {
                if (r5.next())
                    totalBonus = r5.getDouble(1);
            }
            try (PreparedStatement s6 = conn.prepareStatement(sqlMasuk);
                    ResultSet r6 = s6.executeQuery()) {
                if (r6.next())
                    totalPotong = r6.getDouble(1);
            }

            double nettSaldo = totalSaldoApp - totalBonus + totalPotong;

            // UPDATE LABEL
            lblPendapatan.setText("Rp " + String.format("%,.0f", nettSaldo));
            if (lblPendapatanCOD != null)
                lblPendapatanCOD.setText("Rp " + String.format("%,.0f", totalUangFisik));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDataUser() {
        gridUser.getChildren().clear();
        int totalUser = 0;
        String sql = "SELECT * FROM user WHERE role = 'user' ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int userId = rs.getInt("id");
                String nama = rs.getString("nama");
                String username = rs.getString("username");
                String email = rs.getString("email");
                String fotoPath = rs.getString("foto_profil");
                double saldo = rs.getDouble("saldo");
                String statusAkun = rs.getString("status");
                Timestamp lastLogin = rs.getTimestamp("last_login");

                int totalSewa = hitungTotalSewa(conn, userId);

                VBox card = createMemberCard(userId, nama, username, email, fotoPath, totalSewa,
                        saldo, statusAkun, lastLogin);
                gridUser.getChildren().add(card);
                totalUser++;
            }
            lblTotalUser.setText(totalUser + " Member Terdaftar");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int hitungTotalSewa(Connection conn, int userId) {
        try (PreparedStatement stmt =
                conn.prepareStatement("SELECT COUNT(*) FROM peminjaman WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (Exception e) {
        }
        return 0;
    }

    private String getTimeAgo(Timestamp lastLogin) {
        if (lastLogin == null)
            return "⚪ Belum pernah login";
        long diff = new Date().getTime() - lastLogin.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 30)
            return "🟢 Sedang Online";
        if (seconds < 60)
            return "⚪ Baru saja";
        if (minutes < 60)
            return "⚪ Aktif " + minutes + " menit lalu";
        if (hours < 24)
            return "⚪ Aktif " + hours + " jam lalu";
        return "⚪ Aktif " + days + " hari lalu";
    }

    private VBox createMemberCard(int id, String nama, String username, String email,
            String fotoPath, int totalSewa, double saldo, String statusAkun, Timestamp lastLogin) {
        VBox card = new VBox();
        card.setPrefWidth(280);
        card.setMinWidth(280);
        card.setMaxWidth(280);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 5);");

        String badgeFile = "badge_basic.png";
        String levelName = "Member";
        String bannerColor = "linear-gradient(to right, #b85900ff, #cb6900ff)";

        if (totalSewa >= 10) {
            badgeFile = "badge_gold.png";
            levelName = "Gold Member";
            bannerColor = "linear-gradient(to right, #f7971e, #ffd200)";
        } else if (totalSewa >= 5) {
            badgeFile = "badge_silver.png";
            levelName = "Silver Member";
            bannerColor = "linear-gradient(to right, #bdc3c7, #2c3e50)";
        } else {
            bannerColor = "linear-gradient(to right, #d35400, #e67e22)";
        }

        if ("nonaktif".equals(statusAkun)) {
            bannerColor = "linear-gradient(to right, #7f8c8d, #95a5a6)";
            levelName = "DIBEKUKAN";
        }

        boolean adaRequestAktivasi = cekRequestAktivasi(id);

        StackPane headerContainer = new StackPane();
        Region banner = new Region();
        banner.setStyle(
                "-fx-background-color: " + bannerColor + "; -fx-background-radius: 15 15 0 0;");
        banner.setPrefHeight(80);

        HBox profileBox = new HBox(10);
        profileBox.setAlignment(Pos.BOTTOM_CENTER);
        profileBox.setPadding(new javafx.geometry.Insets(40, 0, -30, 0));

        StackPane photoContainer = new StackPane();
        Circle circle = new Circle(35);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(4);
        circle.setEffect(new javafx.scene.effect.DropShadow(5, Color.rgb(0, 0, 0, 0.2)));
        loadUserImage(circle, fotoPath);
        photoContainer.getChildren().add(circle);

        if (adaRequestAktivasi && "nonaktif".equals(statusAkun)) {
            Circle redDot = new Circle(8, Color.RED);
            redDot.setStroke(Color.WHITE);
            redDot.setStrokeWidth(2);
            StackPane.setAlignment(redDot, Pos.TOP_RIGHT);
            photoContainer.getChildren().add(redDot);
            Tooltip.install(redDot, new Tooltip("User ini minta diaktifkan!"));
        }

        ImageView badge = new ImageView();
        badge.setFitWidth(75);
        badge.setFitHeight(75);
        badge.setPreserveRatio(true);
        try {
            badge.setImage(new Image(getClass().getResourceAsStream("/img/badge/" + badgeFile)));
        } catch (Exception e) {
        }
        badge.setEffect(new javafx.scene.effect.DropShadow(5, Color.rgb(0, 0, 0, 0.3)));

        profileBox.getChildren().addAll(photoContainer, badge);
        headerContainer.getChildren().addAll(banner, profileBox);

        VBox contentBox = new VBox(5);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setPadding(new javafx.geometry.Insets(35, 15, 15, 15));
        Label lblNama = new Label(nama);
        lblNama.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2C3E50;");
        Label lblUsername = new Label("@" + username);
        lblUsername.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        Label lblLastSeen = new Label(getTimeAgo(lastLogin));
        String lastSeenColor = lblLastSeen.getText().contains("Online") ? "#27ae60" : "#95a5a6";
        lblLastSeen.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: "
                + lastSeenColor + ";");

        Label lblLevel = new Label(levelName);
        String labelColor =
                (totalSewa >= 10) ? "#f1c40f" : (totalSewa >= 5) ? "#7f8c8d" : "#d35400";
        if ("nonaktif".equals(statusAkun))
            labelColor = "#7f8c8d";
        lblLevel.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: "
                        + labelColor + "; -fx-padding: 3 10; -fx-background-radius: 20;");

        HBox statsBox = new HBox(15);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setStyle("-fx-padding: 10 0 0 0;");
        VBox stat1 = createStatItem("TOTAL SEWA", totalSewa + "x", false);
        VBox stat2 = createStatItem("SALDO", "Rp " + String.format("%,.0f", saldo), true);
        statsBox.getChildren().addAll(stat1, stat2);

        contentBox.getChildren().addAll(lblNama, lblUsername, lblLastSeen, lblLevel, statsBox);

        VBox actions = new VBox(0);
        Button btnChat = new Button("📂 ARSIP CHAT");
        btnChat.setMaxWidth(Double.MAX_VALUE);
        btnChat.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 10;");
        btnChat.setOnAction(e -> showChatHistory(id, nama));

        Button btnTopUp = new Button("💸 ISI SALDO USER");
        btnTopUp.setMaxWidth(Double.MAX_VALUE);
        btnTopUp.setStyle(
                "-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 10;");
        btnTopUp.setOnAction(e -> handleAdminTopUp(id, nama));

        Button btnPotong = new Button("➖ POTONG SALDO");
        btnPotong.setMaxWidth(Double.MAX_VALUE);
        // Warna Oranye Gelap (#d35400) biar beda sama Delete (#e74c3c) dan TopUp
        btnPotong.setStyle(
                "-fx-background-color: #d35400; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 10;");
        // Kita kirim 'saldo' saat ini buat validasi biar gak minus
        btnPotong.setOnAction(e -> handleAdminPotongSaldo(id, nama, saldo));


        Button btnStatus = new Button();
        btnStatus.setMaxWidth(Double.MAX_VALUE);
        if ("aktif".equals(statusAkun)) {
            btnStatus.setText("❄️ NONAKTIFKAN AKUN");
            btnStatus.setStyle(
                    "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 10;");
            btnStatus.setOnAction(e -> handleToggleStatus(id, "nonaktif", nama));
        } else {
            btnStatus.setText("✅ AKTIFKAN AKUN");
            btnStatus.setStyle(
                    "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 10;");
            btnStatus.setOnAction(e -> {
                handleToggleStatus(id, "aktif", nama);
                tandaiRequestSudahDibaca(id);
            });
        }

        Button btnHapus = new Button("🗑️ HAPUS PERMANEN");
        btnHapus.setMaxWidth(Double.MAX_VALUE);
        btnHapus.setStyle(
                "-fx-background-color: white; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-border-color: #e74c3c; -fx-border-width: 1 0 0 0; -fx-background-radius: 0 0 15 15; -fx-cursor: hand; -fx-padding: 10;");
        btnHapus.setOnAction(e -> handleHapusUser(id, username, nama));

        actions.getChildren().addAll(btnChat, btnTopUp, btnPotong, btnStatus, btnHapus);
        card.getChildren().addAll(headerContainer, contentBox, new Region() {
            {
                setPrefHeight(10);
            }
        }, actions);

        card.setOnMouseEntered(e -> {
            card.setTranslateY(-5);
        });
        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
        });

        return card;
    }

    private VBox createStatItem(String title, String value, boolean isMoney) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 9px; -fx-text-fill: #95a5a6; -fx-font-weight: bold;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + (isMoney ? "#27ae60;" : "#2C3E50;"));
        box.getChildren().addAll(t, v);
        return box;
    }

    private void handleAdminPotongSaldo(int userId, String namaUser, double currentSaldo) {
        Dialog<javafx.util.Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Potong Saldo User");
        dialog.setHeaderText("Kurangi saldo milik " + namaUser + "\nSisa Saldo: Rp "
                + String.format("%,.0f", currentSaldo));

        ButtonType potongButtonType = new ButtonType("POTONG", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(potongButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nominalField = new TextField();
        nominalField.setPromptText("Contoh: 20000");
        TextField alasanField = new TextField();
        alasanField.setPromptText("Contoh: Denda kerusakan barang");

        grid.add(new Label("Nominal Potong (Rp):"), 0, 0);
        grid.add(nominalField, 1, 0);
        grid.add(new Label("Alasan/Pesan:"), 0, 1);
        grid.add(alasanField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == potongButtonType) {
                return new javafx.util.Pair<>(nominalField.getText(), alasanField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                double nominal = Double.parseDouble(result.getKey());
                String pesan = result.getValue();
                if (pesan.isEmpty())
                    pesan = "Koreksi saldo oleh Admin";

                if (nominal > currentSaldo) {
                    showAlert("❌ GAGAL", "Saldo user tidak cukup!\nSaldo saat ini: Rp "
                            + String.format("%,.0f", currentSaldo));
                    return;
                }

                Connection conn = DatabaseConnection.getInstance().getConnection();

                // 1. Update Saldo User (DIKURANGI)
                try (PreparedStatement stmt =
                        conn.prepareStatement("UPDATE user SET saldo = saldo - ? WHERE id = ?")) {
                    stmt.setDouble(1, nominal);
                    stmt.setInt(2, userId);
                    stmt.executeUpdate();
                }

                // 2. Insert Notifikasi (Tipe: admin_potong -> PENTING BUAT EXCEL SHEET 2)
                String sqlNotif =
                        "INSERT INTO notifikasi (user_id, pesan, tipe, is_read, nominal) VALUES (?, ?, 'admin_potong', 0, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlNotif)) {
                    stmt.setInt(1, userId);
                    stmt.setString(2, "🔴 SALDO DIPOTONG Rp " + String.format("%,.0f", nominal)
                            + "\nInfo: " + pesan);
                    stmt.setDouble(3, nominal); // Simpan nominal buat laporan
                    stmt.executeUpdate();
                }

                showAlert("✅ SUKSES", "Saldo " + namaUser + " berhasil dipotong Rp "
                        + String.format("%,.0f", nominal));

                // Refresh data biar angka di kartu update
                loadDataUser();
                loadPendapatan();

            } catch (NumberFormatException e) {
                showAlert("Error", "Nominal harus angka!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadUserImage(Circle circle, String dbPath) {
        if (dbPath == null || dbPath.isEmpty())
            dbPath = "default.png";
        try {
            File f = new File(BASE_FOTO_PATH + dbPath);
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    circle.setFill(new ImagePattern(new Image(fis)));
                    return;
                }
            }
            String resPath = "/img/fotouser/" + dbPath.replace("\\", "/");
            if (getClass().getResource(resPath) != null) {
                circle.setFill(
                        new ImagePattern(new Image(getClass().getResourceAsStream(resPath))));
            } else {
                circle.setFill(new ImagePattern(
                        new Image(getClass().getResourceAsStream("/img/fotouser/default.png"))));
            }
        } catch (Exception e) {
            circle.setFill(Color.LIGHTGRAY);
        }
    }

    private void handleToggleStatus(int userId, String newStatus, String nama) {
        String actionName = newStatus.equals("nonaktif") ? "MEMBEKUKAN" : "MENGAKTIFKAN";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Status");
        alert.setHeaderText("Yakin ingin " + actionName + " akun " + nama + "?");
        if (newStatus.equals("nonaktif")) {
            alert.setContentText("User tidak akan bisa login, tapi data transaksi tetap AMAN.");
        } else {
            alert.setContentText("User akan bisa login kembali.");
        }

        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt =
                            conn.prepareStatement("UPDATE user SET status = ? WHERE id = ?")) {
                stmt.setString(1, newStatus);
                stmt.setInt(2, userId);
                stmt.executeUpdate();

                showAlert("Sukses",
                        "Status akun berhasil diubah menjadi: " + newStatus.toUpperCase());
                loadDataUser();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Gagal ubah status: " + e.getMessage());
            }
        }
    }

    private void handleHapusUser(int id, String username, String nama) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Hapus User Permanen");
        alert.setHeaderText("Yakin HAPUS PERMANEN " + nama + "?");
        alert.setContentText(
                "⚠️ PERINGATAN KERAS:\n1. Data & Duit Riwayat HILANG SELAMANYA.\n2. Stok barang pinjaman akan dikembalikan.\n3. Lebih baik gunakan fitur NONAKTIFKAN jika masih ragu.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                deleteUserFolder(username);
                Connection conn = DatabaseConnection.getInstance().getConnection();

                String sqlCekBarang =
                        "SELECT produk_id, jumlah_barang FROM peminjaman WHERE user_id = ? AND status = 'Dipinjam'";
                try (PreparedStatement stmtCek = conn.prepareStatement(sqlCekBarang)) {
                    stmtCek.setInt(1, id);
                    ResultSet rs = stmtCek.executeQuery();
                    while (rs.next()) {
                        int produkId = rs.getInt("produk_id");
                        int qty = rs.getInt("jumlah_barang");
                        try (PreparedStatement stmtUpdateStok = conn.prepareStatement(
                                "UPDATE produk SET stok = stok + ? WHERE id = ?")) {
                            stmtUpdateStok.setInt(1, qty);
                            stmtUpdateStok.setInt(2, produkId);
                            stmtUpdateStok.executeUpdate();
                        }
                    }
                }

                try {
                    conn.prepareStatement("DELETE FROM notifikasi WHERE user_id=" + id)
                            .executeUpdate();
                    conn.prepareStatement("DELETE FROM peminjaman WHERE user_id=" + id)
                            .executeUpdate();
                    conn.prepareStatement("DELETE FROM user WHERE id=" + id).executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                showAlert("Sukses", "User dihapus permanen.");
                loadDataUser();
                loadPendapatan();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Gagal hapus user: " + e.getMessage());
            }
        }
    }

    private void showChatHistory(int userId, String namaUser) {
        StringBuilder chatLog = new StringBuilder();
        boolean adaPesan = false;
        String sql =
                "SELECT pesan, created_at, tipe FROM notifikasi WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                adaPesan = true;
                String waktu = rs.getString("created_at");
                String isi = rs.getString("pesan");
                String tipe = rs.getString("tipe");
                String pengirim = "admin_ke_user".equals(tipe) ? "👮‍♂️ ADMIN" : "👤 USER";
                chatLog.append("🕒 ").append(waktu).append("\n").append(pengirim).append(": ")
                        .append(isi).append("\n------------------------------------------------\n");
            }
            if (!adaPesan)
                chatLog.append("Belum ada riwayat chat.");
            TextArea area = new TextArea(chatLog.toString());
            area.setPrefSize(400, 300);
            area.setEditable(false);
            area.setWrapText(true);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Arsip Chat: " + namaUser);
            alert.setHeaderText("Riwayat Pesan");
            alert.getDialogPane().setContent(area);
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteUserFolder(String username) {
        try {
            File folder = new File(BASE_FOTO_PATH + username);
            if (folder.exists()) {
                for (File f : folder.listFiles())
                    f.delete();
                folder.delete();
            }
        } catch (Exception e) {
        }
    }

    private void setupTabel() {
        tabelPeminjaman.setPlaceholder(new Label("📭 Tidak ada data transaksi pada periode ini."));
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("namaUser"));
        colBarang.setCellValueFactory(new PropertyValueFactory<>("namaProduk"));
        colBarang.setCellFactory(col -> new TableCell<Peminjaman, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Peminjaman row = getTableView().getItems().get(getIndex());
                    int qty = row.getJumlahBarang();
                    if (qty > 1) {
                        setText(item + " (x" + qty + ")");
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #2C3E50;");
                    }
                }
            }
        });
        colTanggal.setCellValueFactory(new PropertyValueFactory<>("tanggalPinjam"));
        colKembali.setCellValueFactory(new PropertyValueFactory<>("tanggalKembali"));
        colBiayaSewa.setCellValueFactory(new PropertyValueFactory<>("totalBayar"));
        colBiayaSewa.setCellFactory(col -> new TableCell<Peminjaman, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Format Rp (Ribuan)
                    setText(String.format("Rp %,.0f", item));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
                }
            }
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colDenda.setCellValueFactory(new PropertyValueFactory<>("denda"));
        colDenda.setCellFactory(col -> new TableCell<Peminjaman, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else {
                    setText(String.format("Rp %,.0f", item));
                    setTextFill(item > 0 ? Color.RED : Color.GRAY);
                }
            }
        });

        colStatusBayar.setCellValueFactory(new PropertyValueFactory<>("statusPembayaran"));
        colStatusBayar.setCellFactory(col -> new TableCell<Peminjaman, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Peminjaman row = getTableView().getItems().get(getIndex());

                    // 1. Cek Status Sewa (Awal)
                    boolean sewaCOD =
                            row.getMetodeBayar() != null && row.getMetodeBayar().contains("CASH");
                    String teksSewa = sewaCOD ? "LUNAS (COD)" : "LUNAS";

                    // 2. Cek Status Denda
                    boolean dendaLunas = "SUDAH_BAYAR".equals(item); // Status SUDAH_BAYAR artinya
                                                                     // denda lunas
                    boolean dendaCOD = row.getMetodeBayarDenda() != null
                            && row.getMetodeBayarDenda().contains("CASH");

                    // 3. Cek Telat (Realtime)
                    LocalDate tglKembali = LocalDate.now();
                    try {
                        tglKembali = LocalDate.parse(row.getTanggalKembali());
                    } catch (Exception e) {
                    }
                    boolean isLate = "Dipinjam".equals(row.getStatus())
                            && LocalDate.now().isAfter(tglKembali);

                    // --- LOGIC GABUNGAN ---
                    if (dendaLunas) {
                        // Skenario: Sewa Lunas + Denda Lunas
                        String teksDenda = dendaCOD ? "Lunas Denda (COD)" : "Lunas Denda";
                        setText(teksSewa + ", " + teksDenda);
                        setStyle(
                                "-fx-text-fill: #2980b9; -fx-font-weight: bold; -fx-font-size: 10px;"); // Biru
                    } else if (isLate) {
                        // Skenario: Telat & Belum Bayar Denda -> "TELAT DENDA" (Merah Mutlak)
                        setText("TELAT DENDA");
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if ("MENUNGGU_PEMBAYARAN".equals(item)) {
                        setText("MENUNGGU KONFIRMASI");
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        // Skenario: Sewa Lunas, Gak ada denda
                        setText(teksSewa);
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tabelPeminjaman.setRowFactory(tv -> new TableRow<Peminjaman>() {
            @Override
            protected void updateItem(Peminjaman item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty)
                    setStyle("");
                else {
                    String statusBayar = item.getStatusPembayaran();
                    String statusBarang = item.getStatus();
                    LocalDate tglKembali = LocalDate.now();
                    try {
                        tglKembali = LocalDate.parse(item.getTanggalKembali());
                    } catch (Exception e) {
                    }
                    boolean isLate =
                            "Dipinjam".equals(statusBarang) && LocalDate.now().isAfter(tglKembali);
                    if ("SUDAH_BAYAR".equals(statusBayar))
                        setStyle("-fx-background-color: #cce5ff; -fx-font-weight: bold;");
                    else if ("MENUNGGU_PEMBAYARAN".equals(statusBayar))
                        setStyle("-fx-background-color: #fff2cc; -fx-font-weight: bold;");
                    else if (isLate)
                        setStyle("-fx-background-color: #ffcccc; -fx-font-weight: bold;");
                    else if ("LUNAS".equals(statusBayar))
                        setStyle("-fx-background-color: #ccffcc; -fx-font-weight: bold;");
                    else
                        setStyle("");
                }
            }
        });

        tabelPeminjaman.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tabelPeminjaman.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener.Change<? extends Peminjaman> c) -> {
                    ObservableList<Peminjaman> selectedItems =
                            tabelPeminjaman.getSelectionModel().getSelectedItems();
                    if (selectedItems.isEmpty()) {
                        disableButton(btnTerima);
                        disableButton(btnKonfirmasi);
                        disableButton(btnPeringatan);
                        disableButton(btnEditTanggal);
                        if (btnArsipChat != null)
                            disableButton(btnArsipChat);
                    } else {
                        boolean adaYangBisaDiterima = selectedItems.stream()
                                .anyMatch(p -> "Dipinjam".equals(p.getStatus()));
                        if (adaYangBisaDiterima)
                            enableButton(btnTerima, "#27ae60");
                        else
                            disableButton(btnTerima);
                        boolean adaYangMenunggu = selectedItems.stream().anyMatch(
                                p -> "MENUNGGU_PEMBAYARAN".equals(p.getStatusPembayaran()));
                        if (adaYangMenunggu) {
                            enableButton(btnKonfirmasi, "#F39C12");
                            disableButton(btnPeringatan);
                        } else {
                            disableButton(btnKonfirmasi);
                            boolean adaYangBelumLunasMerah = selectedItems.stream().anyMatch(p -> {
                                LocalDate tglKembali = LocalDate.now();
                                try {
                                    tglKembali = LocalDate.parse(p.getTanggalKembali());
                                } catch (Exception e) {
                                }
                                boolean isLate = "Dipinjam".equals(p.getStatus())
                                        && LocalDate.now().isAfter(tglKembali);
                                return isLate || (!"LUNAS".equals(p.getStatusPembayaran())
                                        && !"SUDAH_BAYAR".equals(p.getStatusPembayaran()));
                            });
                            if (adaYangBelumLunasMerah)
                                enableButton(btnPeringatan, "#e74c3c");
                            else
                                disableButton(btnPeringatan);
                        }
                        enableButton(btnEditTanggal, "#2C3E50");
                        if (btnArsipChat != null)
                            enableButton(btnArsipChat, "#3498db");
                    }
                });
    }

    private void enableButton(Button btn, String colorHex) {
        btn.setDisable(false);
        btn.setStyle(
                "-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-cursor: hand;");
    }

    private void disableButton(Button btn) {
        btn.setDisable(true);
        btn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #7f8c8d;");
    }

    private void loadDataPeminjaman() {
        ObservableList<Peminjaman> list = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder(
                "SELECT p.id, u.id AS user_id, u.nama AS nama_user, prod.nama AS nama_produk, "
                        + "p.tanggal_pinjam, DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY) AS tgl_kembali, "
                        + "p.status, p.status_pembayaran, p.jumlah_barang, p.metode_bayar, p.metode_bayar_denda, "
                        + "(prod.harga_sewa * p.jumlah_barang * p.durasi_hari) AS real_total_bayar, "
                        + // Ambil Total Sewa
                        "CASE WHEN p.status_pembayaran = 'SUDAH_BAYAR' OR p.status = 'Dikembalikan' THEN COALESCE(p.denda, 0) "
                        + "WHEN p.status = 'Dipinjam' AND CURRENT_DATE > DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY) "
                        + "THEN DATEDIFF(CURRENT_DATE, DATE_ADD(p.tanggal_pinjam, INTERVAL p.durasi_hari DAY)) * 15000 * p.jumlah_barang "
                        + "ELSE 0 END AS total_denda " + "FROM peminjaman p "
                        + "JOIN user u ON p.user_id = u.id "
                        + "JOIN produk prod ON p.produk_id = prod.id " + "WHERE 1=1 ");

        int blnIndex = comboBulan.getSelectionModel().getSelectedIndex();
        if (blnIndex > 0)
            sql.append(" AND MONTH(p.tanggal_pinjam) = ").append(blnIndex);

        String thn = comboTahun.getValue();
        if (thn != null && !"SEMUA".equals(thn))
            sql.append(" AND YEAR(p.tanggal_pinjam) = ").append(thn);

        sql.append(" ORDER BY p.tanggal_pinjam DESC");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString());
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                double totalBayarHitungan = rs.getDouble("real_total_bayar");

                Peminjaman p = new Peminjaman(rs.getInt("id"), rs.getInt("user_id"),
                        rs.getString("nama_user"), rs.getString("nama_produk"),
                        rs.getString("tanggal_pinjam"), rs.getString("tgl_kembali"),
                        rs.getString("status"), rs.getDouble("total_denda"),
                        rs.getString("status_pembayaran"), rs.getInt("jumlah_barang"),
                        rs.getString("metode_bayar"), rs.getString("metode_bayar_denda"),
                        totalBayarHitungan);

                // Variabel p sudah dideklarasikan, jadi aman buat dipanggil
                list.add(p);
            }

            tabelPeminjaman.setItems(list);

            if (btnCetakLaporan != null) {
                btnCetakLaporan.setDisable(list.isEmpty());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCetakLaporan() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Laporan Excel");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        // Nama File Otomatis: Laporan_JavaGear_[BULAN]_[TAHUN].xlsx
        String bln = comboBulan.getValue();
        String thn = comboTahun.getValue();
        if (bln == null)
            bln = "SEMUA";
        if (thn == null)
            thn = "SEMUA";

        fileChooser.setInitialFileName("Laporan_JavaGear_" + bln + "_" + thn + ".xlsx");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            exportToExcel(file);
        }
    }

    private void exportToExcel(File file) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet1 = workbook.createSheet("Data Transaksi");

            // --- STYLING (PASTIKAN HELPER STYLE ADA) ---
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook); // Style biasa + Border
            CellStyle currencyStyle = createCurrencyStyle(workbook); // Style duit + Border

            // Header Judul
            Row titleRow = sheet1.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("LAPORAN TRANSAKSI JAVAGEAR OUTDOOR");
            titleCell.setCellStyle(createTitleStyle(workbook));
            sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            Row periodRow = sheet1.createRow(1);
            periodRow.createCell(0).setCellValue(
                    "Periode: " + comboBulan.getValue() + " " + comboTahun.getValue());

            // Header Kolom
            String[] headers1 = {"ID", "Peminjam", "Barang", "Tgl Pinjam", "Tgl Kembali",
                    "Biaya Sewa", "Denda", "Status Barang", "Status Bayar", "Total Akhir"};
            Row headerRow1 = sheet1.createRow(3);
            for (int i = 0; i < headers1.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow1.createCell(i);
                cell.setCellValue(headers1[i]);
                cell.setCellStyle(headerStyle);
            }

            ObservableList<Peminjaman> data = tabelPeminjaman.getItems();
            int rowIdx = 4;

            // Variabel Penampung Duit
            double totalSewaTransfer = 0;
            double totalSewaCOD = 0;
            double totalDendaTransfer = 0;
            double totalDendaCOD = 0;

            // HITUNG JUMLAH UNIT BARANG (QTY), BUKAN JUMLAH BARIS
            int countDipinjam = 0;
            int countDikembalikan = 0;

            for (Peminjaman p : data) {
                Row row = sheet1.createRow(rowIdx++);

                // 1. Isi Data Utama dengan Border
                createCellWithBorder(row, 0, String.valueOf(p.getId()), dataStyle);
                createCellWithBorder(row, 1, p.getNamaUser(), dataStyle);
                createCellWithBorder(row, 2, p.getNamaProduk() + " (" + p.getJumlahBarang() + "x)",
                        dataStyle);
                createCellWithBorder(row, 3, p.getTanggalPinjam(), dataStyle);
                createCellWithBorder(row, 4, p.getTanggalKembali(), dataStyle);

                // 2. Biaya Sewa (Format Duit + Border)
                createCellCurrency(row, 5, p.getTotalBayar(), currencyStyle);

                // 3. Denda (Format Duit + Border)
                createCellCurrency(row, 6, p.getDenda(), currencyStyle);

                // 4. Status Barang
                createCellWithBorder(row, 7, p.getStatus(), dataStyle);

                // LOGIKA COUNTER BARANG (Qty)
                if ("Dipinjam".equals(p.getStatus())) {
                    countDipinjam += p.getJumlahBarang(); // Tambah sesuai Qty
                } else {
                    countDikembalikan += p.getJumlahBarang(); // Tambah sesuai Qty
                }

                // 5. Status Bayar (Warna-warni + Border)
                org.apache.poi.ss.usermodel.Cell statusCell = row.createCell(8);
                statusCell.setCellValue(formatStatusBayar(p)); // Helper teks

                // Bikin style baru biar border tetep ada tapi warna font berubah
                CellStyle coloredStyle = workbook.createCellStyle();
                coloredStyle.cloneStyleFrom(dataStyle);
                Font font = workbook.createFont();
                font.setBold(true);

                if (p.getStatusPembayaran().contains("LUNAS"))
                    font.setColor(IndexedColors.GREEN.getIndex());
                else if (p.getStatusPembayaran().contains("SUDAH_BAYAR"))
                    font.setColor(IndexedColors.BLUE.getIndex());
                else
                    font.setColor(IndexedColors.RED.getIndex());

                coloredStyle.setFont(font);
                statusCell.setCellStyle(coloredStyle);

                // 6. Total Akhir
                createCellCurrency(row, 9, p.getTotalBayar() + p.getDenda(), currencyStyle);

                // 7. Hitung Duit Footer (Logika Lama Dipertahankan)
                boolean sewaCOD = p.getMetodeBayar() != null && p.getMetodeBayar().contains("CASH");
                boolean dendaLunas = "SUDAH_BAYAR".equals(p.getStatusPembayaran());
                boolean dendaCOD =
                        p.getMetodeBayarDenda() != null && p.getMetodeBayarDenda().contains("CASH");

                if ("LUNAS".equals(p.getStatusPembayaran()) || dendaLunas) {
                    if (sewaCOD)
                        totalSewaCOD += p.getTotalBayar();
                    else
                        totalSewaTransfer += p.getTotalBayar();
                }
                if (dendaLunas) {
                    if (dendaCOD)
                        totalDendaCOD += p.getDenda();
                    else
                        totalDendaTransfer += p.getDenda();
                }
            }

            int footerRowIdx1 = rowIdx + 1;

            // Helper buat bikin baris footer ber-border
            createFooterRow(sheet1, footerRowIdx1, "TOTAL TRANSFER (APP)",
                    totalSewaTransfer + totalDendaTransfer, currencyStyle, headerStyle);
            createFooterRow(sheet1, footerRowIdx1 + 1, "TOTAL TUNAI (COD)",
                    totalSewaCOD + totalDendaCOD, currencyStyle, headerStyle);

            // Grand Total (Bold + Kuning)
            CellStyle grandStyle = workbook.createCellStyle();
            grandStyle.cloneStyleFrom(currencyStyle);
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            grandStyle.setFont(boldFont);
            grandStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            grandStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            createFooterRow(sheet1, footerRowIdx1 + 2, "GRAND TOTAL (SEMUA)",
                    (totalSewaTransfer + totalDendaTransfer) + (totalSewaCOD + totalDendaCOD),
                    grandStyle, headerStyle);

            // Statistik Barang (Border juga)
            // Menggunakan dataStyle untuk nilai barang agar ada border
            createFooterTextRow(sheet1, footerRowIdx1 + 4, "TOTAL BARANG DIPINJAM",
                    countDipinjam + " Item", dataStyle, headerStyle);
            createFooterTextRow(sheet1, footerRowIdx1 + 5, "TOTAL BARANG DIKEMBALIKAN",
                    countDikembalikan + " Item", dataStyle, headerStyle);

            for (int i = 0; i < headers1.length; i++)
                sheet1.autoSizeColumn(i);

            Sheet sheet2 = workbook.createSheet("Laporan Keuangan");
            Row titleRow2 = sheet2.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell2 = titleRow2.createCell(0);
            titleCell2.setCellValue("LAPORAN MUTASI SALDO ADMIN (MANUAL)");
            titleCell2.setCellStyle(createTitleStyle(workbook));
            sheet2.addMergedRegion(new CellRangeAddress(0, 0, 0, 6)); // Merge 7 Kolom

            String[] headers2 = {"Tanggal", "User ID", "Nama User", "Pesan", "Keterangan",
                    "Tipe Mutasi", "Nominal"};
            Row headerRow2 = sheet2.createRow(2);
            for (int i = 0; i < headers2.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow2.createCell(i);
                cell.setCellValue(headers2[i]);
                cell.setCellStyle(headerStyle);
            }

            // JOIN dengan tabel USER buat ambil NAMA
            String sqlMutasi = "SELECT n.created_at, n.user_id, u.nama, n.pesan, n.tipe, n.nominal "
                    + "FROM notifikasi n " + "JOIN user u ON n.user_id = u.id "
                    + "WHERE n.tipe IN ('admin_bonus', 'admin_potong')";

            int blnIndex = comboBulan.getSelectionModel().getSelectedIndex();
            String thnVal = comboTahun.getValue();
            if (blnIndex > 0)
                sqlMutasi += " AND MONTH(n.created_at) = " + blnIndex;
            if (thnVal != null && !"SEMUA".equals(thnVal))
                sqlMutasi += " AND YEAR(n.created_at) = " + thnVal;
            sqlMutasi += " ORDER BY n.created_at DESC";

            double totalPemasukan = 0;
            double totalPengeluaran = 0;

            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sqlMutasi);
                    ResultSet rs = stmt.executeQuery()) {
                int r2 = 3;
                while (rs.next()) {
                    Row row = sheet2.createRow(r2++);
                    row.createCell(0).setCellValue(rs.getString("created_at"));
                    row.createCell(1).setCellValue(rs.getInt("user_id"));
                    row.createCell(2).setCellValue(rs.getString("nama")); // 🔥 NAMA USER MUNCUL

                    // LOGIC SPLIT PESAN
                    String rawPesan = rs.getString("pesan");
                    String judulPesan = rawPesan;
                    String keterangan = "-";

                    if (rawPesan.contains("\n")) {
                        String[] parts = rawPesan.split("\n", 2);
                        judulPesan = parts[0]; // Baris 1
                        if (parts.length > 1) {
                            keterangan = parts[1].replace("Info: ", "").trim(); // Baris 2
                                                                                // (dibersihin)
                        }
                    }

                    row.createCell(3).setCellValue(judulPesan);
                    row.createCell(4).setCellValue(keterangan);

                    String tipe = rs.getString("tipe");
                    double nominal = rs.getDouble("nominal");

                    org.apache.poi.ss.usermodel.Cell tipeCell = row.createCell(5); // Geser ke index
                                                                                   // 5
                    org.apache.poi.ss.usermodel.Cell nominalCell = row.createCell(6); // Geser ke
                                                                                      // index 6
                    nominalCell.setCellStyle(currencyStyle);
                    nominalCell.setCellValue(nominal);

                    CellStyle colorStyle = workbook.createCellStyle();
                    colorStyle.cloneStyleFrom(dataStyle);
                    Font font = workbook.createFont();
                    font.setBold(true);

                    if ("admin_potong".equals(tipe)) {
                        tipeCell.setCellValue("PEMASUKAN (Potong Saldo)");
                        font.setColor(IndexedColors.GREEN.getIndex());
                        totalPemasukan += nominal;
                    } else {
                        tipeCell.setCellValue("PENGELUARAN (Top Up)");
                        font.setColor(IndexedColors.RED.getIndex());
                        totalPengeluaran += nominal;
                    }
                    colorStyle.setFont(font);
                    tipeCell.setCellStyle(colorStyle);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // FOOTER SHEET 2
            int footerRowIdx = sheet2.getLastRowNum() + 2;

            Row footerRow = sheet2.createRow(footerRowIdx);
            footerRow.createCell(5).setCellValue("TOTAL PEMASUKAN");
            footerRow.createCell(6).setCellValue(totalPemasukan);

            Row footerRow2 = sheet2.createRow(footerRowIdx + 1);
            footerRow2.createCell(5).setCellValue("TOTAL PENGELUARAN");
            footerRow2.createCell(6).setCellValue(totalPengeluaran);

            Row footerRow3 = sheet2.createRow(footerRowIdx + 2);
            footerRow3.createCell(5).setCellValue("GRAND TOTAL (NETTO)");
            org.apache.poi.ss.usermodel.Cell nettCell = footerRow3.createCell(6);
            double netto = totalPemasukan - totalPengeluaran;
            nettCell.setCellValue(netto);

            CellStyle netStyle = workbook.createCellStyle();
            netStyle.cloneStyleFrom(currencyStyle);
            netStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            if (netto >= 0)
                netStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            else
                netStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            nettCell.setCellStyle(netStyle);

            for (int i = 0; i < headers2.length; i++)
                sheet2.autoSizeColumn(i);

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
                showAlert("Sukses", "Laporan berhasil diexport ke:\n" + file.getAbsolutePath());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal export Excel: " + e.getMessage());
        }
    }

    // HELPER STYLES
    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    @FXML
    private void handleKonfirmasi() {
        Peminjaman selected = tabelPeminjaman.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/admin/KonfirmasiPembayaran.fxml"));
            Parent root = loader.load();
            KonfirmasiPembayaranController ctrl = loader.getController();
            ctrl.setDataPeminjaman(selected);
            Stage stage = new Stage();
            stage.setTitle("Konfirmasi");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadDataPeminjaman();
            loadPendapatan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTerimaBarang() {
        ObservableList<Peminjaman> selectedItems =
                tabelPeminjaman.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty())
            return;
        long validCount =
                selectedItems.stream().filter(p -> "Dipinjam".equals(p.getStatus())).count();
        if (validCount == 0)
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Terima Barang Massal");
        alert.setHeaderText("Terima " + validCount + " barang terpilih?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                String sqlUpdateStatus =
                        "UPDATE peminjaman SET status = 'Dikembalikan' WHERE id = ?";
                String sqlUpdateStok =
                        "UPDATE produk SET stok = stok + ? WHERE id = (SELECT produk_id FROM peminjaman WHERE id = ?)";
                try (PreparedStatement stmtStatus = conn.prepareStatement(sqlUpdateStatus);
                        PreparedStatement stmtStok = conn.prepareStatement(sqlUpdateStok)) {
                    for (Peminjaman p : selectedItems) {
                        if (!"Dipinjam".equals(p.getStatus()))
                            continue;
                        stmtStatus.setInt(1, p.getId());
                        stmtStatus.executeUpdate();
                        stmtStok.setInt(1, p.getJumlahBarang());
                        stmtStok.setInt(2, p.getId());
                        stmtStok.executeUpdate();
                    }
                }
                conn.commit();
                showAlert("Sukses", "Barang diterima!");
                loadDataPeminjaman();
                loadPendapatan();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handlePeringatan() {
        Peminjaman selected = tabelPeminjaman.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        String pesan = "⚠️ PERINGATAN\nHalo " + selected.getNamaUser() + ",\nBarang '"
                + selected.getNamaProduk() + "' belum dikembalikan.\nDenda: Rp "
                + String.format("%,.0f", selected.getDenda());
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO notifikasi (user_id, pesan, tipe, is_read) VALUES (?, ?, 'admin_ke_user', 0)")) {
            stmt.setInt(1, selected.getUserId());
            stmt.setString(2, pesan);
            stmt.executeUpdate();
            showAlert("Sukses", "Peringatan dikirim!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleArsipChat() {
        Peminjaman selected = tabelPeminjaman.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Info", "Pilih user dulu.");
            return;
        }
        showChatHistory(selected.getUserId(), selected.getNamaUser());
    }

    @FXML
    private void handleEditTanggal() {
        Peminjaman selected = tabelPeminjaman.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Edit Tanggal Pinjam");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DatePicker datePicker = new DatePicker(LocalDate.parse(selected.getTanggalPinjam()));
        dialog.getDialogPane().setContent(new VBox(10, new Label("Tanggal Baru:"), datePicker));
        dialog.setResultConverter(d -> d == ButtonType.OK ? datePicker.getValue() : null);
        dialog.showAndWait().ifPresent(date -> {
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE peminjaman SET tanggal_pinjam = ? WHERE id = ?")) {
                stmt.setDate(1, java.sql.Date.valueOf(date));
                stmt.setInt(2, selected.getId());
                stmt.executeUpdate();
                showAlert("Sukses", "Tanggal diubah!");
                loadDataPeminjaman();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void filterSemua() {
        setActiveButton(btnFilterSemua, "btn-ungu-solid", "btn-ungu-transparan");
        loadDataPeminjaman();
    }

    @FXML
    private void filterMenunggu() {
        setActiveButton(btnFilterMenunggu, "btn-kuning-solid", "btn-kuning-transparan");
        filterByStatus("MENUNGGU_PEMBAYARAN");
    }

    @FXML
    private void filterLunas() {
        setActiveButton(btnFilterLunas, "btn-hijau-solid", "btn-hijau-transparan");

        loadDataPeminjaman();
        ObservableList<Peminjaman> filtered = FXCollections.observableArrayList();
        for (Peminjaman p : tabelPeminjaman.getItems()) {
            LocalDate tglKembali = LocalDate.now();
            try {
                tglKembali = LocalDate.parse(p.getTanggalKembali());
            } catch (Exception e) {
            }
            boolean isLate =
                    "Dipinjam".equals(p.getStatus()) && LocalDate.now().isAfter(tglKembali);

            // Logic LUNAS (Hijau): Lunas & Tidak Telat
            if ("LUNAS".equals(p.getStatusPembayaran()) && !isLate) {
                filtered.add(p);
            }
        }
        tabelPeminjaman.setItems(filtered);
    }

    @FXML
    private void filterDendaLunas() {
        setActiveButton(btnFilterDendaLunas, "btn-biru-solid", "btn-biru-transparan");
        filterByStatus("SUDAH_BAYAR");
    }

    @FXML
    private void filterTelat() {
        setActiveButton(btnFilterTelat, "btn-merah-solid", "btn-merah-transparan");

        loadDataPeminjaman();
        ObservableList<Peminjaman> filtered = FXCollections.observableArrayList();
        for (Peminjaman p : tabelPeminjaman.getItems()) {
            // Logic TELAT (Merah): Ada Denda & Masih Dipinjam
            if (p.getDenda() > 0 && "Dipinjam".equals(p.getStatus())) {
                filtered.add(p);
            }
        }
        tabelPeminjaman.setItems(filtered);
    }

    private void filterByStatus(String status) {
        loadDataPeminjaman();
        ObservableList<Peminjaman> filtered = FXCollections.observableArrayList();
        for (Peminjaman p : tabelPeminjaman.getItems()) {
            if (status.equals(p.getStatusPembayaran())) {
                filtered.add(p);
            }
        }
        tabelPeminjaman.setItems(filtered);
    }

    private void setActiveButton(Button activeButton, String solidClass, String transparanClass) {
        // 1. Reset Semua ke Transparan Dulu
        if (btnFilterSemua != null)
            resetButtonStyle(btnFilterSemua, "btn-ungu-transparan", "btn-ungu-solid");
        if (btnFilterMenunggu != null)
            resetButtonStyle(btnFilterMenunggu, "btn-kuning-transparan", "btn-kuning-solid");
        if (btnFilterLunas != null)
            resetButtonStyle(btnFilterLunas, "btn-hijau-transparan", "btn-hijau-solid");
        if (btnFilterDendaLunas != null)
            resetButtonStyle(btnFilterDendaLunas, "btn-biru-transparan", "btn-biru-solid");
        if (btnFilterTelat != null)
            resetButtonStyle(btnFilterTelat, "btn-merah-transparan", "btn-merah-solid");

        // 2. Set Tombol yang Diklik jadi Solid (Terang)
        if (activeButton != null) {
            activeButton.getStyleClass().removeAll(transparanClass);
            if (!activeButton.getStyleClass().contains(solidClass)) {
                activeButton.getStyleClass().add(solidClass);
            }
        }
    }

    private void resetButtonStyle(Button btn, String transparanStyle, String solidStyle) {
        btn.getStyleClass().removeAll(solidStyle);
        if (!btn.getStyleClass().contains(transparanStyle)) {
            btn.getStyleClass().add(transparanStyle);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // HELPER: CEK RED DOT
    private boolean cekRequestAktivasi(int userId) {
        String sql =
                "SELECT COUNT(*) FROM notifikasi WHERE user_id = ? AND tipe = 'req_aktivasi' AND is_read = 0";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (Exception e) {
        }
        return false;
    }

    // HELPER: HAPUS RED DOT
    private void tandaiRequestSudahDibaca(int userId) {
        String sql =
                "UPDATE notifikasi SET is_read = 1 WHERE user_id = ? AND tipe = 'req_aktivasi'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
        }
    }

    // LOGIC SULTAN: ADMIN ISI SALDO
    private void handleAdminTopUp(int userId, String namaUser) {
        Dialog<javafx.util.Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Isi Saldo User");
        dialog.setHeaderText("Kirim saldo ke " + namaUser);

        ButtonType loginButtonType = new ButtonType("Kirim", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nominalField = new TextField();
        nominalField.setPromptText("Contoh: 50000");
        TextField alasanField = new TextField();
        alasanField.setPromptText("Contoh: Bonus Pelanggan Setia");

        grid.add(new Label("Nominal (Rp):"), 0, 0);
        grid.add(nominalField, 1, 0);
        grid.add(new Label("Pesan/Alasan:"), 0, 1);
        grid.add(alasanField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new javafx.util.Pair<>(nominalField.getText(), alasanField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                double nominal = Double.parseDouble(result.getKey());
                String pesan = result.getValue();
                if (pesan.isEmpty())
                    pesan = "Bonus dari Admin";

                Connection conn = DatabaseConnection.getInstance().getConnection();

                try (PreparedStatement stmt =
                        conn.prepareStatement("UPDATE user SET saldo = saldo + ? WHERE id = ?")) {
                    stmt.setDouble(1, nominal);
                    stmt.setInt(2, userId);
                    stmt.executeUpdate();
                }

                String sqlNotif =
                        "INSERT INTO notifikasi (user_id, pesan, tipe, is_read, nominal) VALUES (?, ?, 'admin_bonus', 0, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlNotif)) {
                    stmt.setInt(1, userId);
                    stmt.setString(2, "💰 SALDO MASUK Rp " + String.format("%,.0f", nominal)
                            + "\nInfo: " + pesan);
                    stmt.setDouble(3, nominal);
                    stmt.executeUpdate();
                }

                showAlert("Sukses",
                        "Saldo Rp " + String.format("%,.0f", nominal) + " terkirim ke " + namaUser);
                loadDataUser();
                loadPendapatan();

            } catch (NumberFormatException e) {
                showAlert("Error", "Nominal harus angka!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // 1. Bikin cell teks biasa dengan border
    private void createCellWithBorder(Row row, int col, String val, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(val != null ? val : "-");
        cell.setCellStyle(style);
    }

    // 2. Bikin cell duit dengan border
    private void createCellCurrency(Row row, int col, double val, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(val);
        cell.setCellStyle(style);
    }

    // 3. Bikin baris footer DUIT dengan border rapi
    private void createFooterRow(Sheet sheet, int rowIdx, String label, double amount,
            CellStyle valStyle, CellStyle labelStyle) {
        Row row = sheet.createRow(rowIdx);

        // Cell Label (Kolom 8)
        org.apache.poi.ss.usermodel.Cell cellLabel = row.createCell(8);
        cellLabel.setCellValue(label);
        cellLabel.setCellStyle(labelStyle); // Pake style header biar tebal & border

        // Cell Nilai (Kolom 9)
        org.apache.poi.ss.usermodel.Cell cellVal = row.createCell(9);
        cellVal.setCellValue(amount);
        cellVal.setCellStyle(valStyle);
    }

    // 4. Bikin baris footer TEKS (Counter Barang) dengan border rapi
    private void createFooterTextRow(Sheet sheet, int rowIdx, String label, String val,
            CellStyle valStyle, CellStyle labelStyle) {
        Row row = sheet.createRow(rowIdx);

        // Cell Label (Kolom 8)
        org.apache.poi.ss.usermodel.Cell cellLabel = row.createCell(8);
        cellLabel.setCellValue(label);
        cellLabel.setCellStyle(labelStyle);

        // Cell Nilai (Kolom 9)
        org.apache.poi.ss.usermodel.Cell cellVal = row.createCell(9);
        cellVal.setCellValue(val);
        cellVal.setCellStyle(valStyle);
    }

    // 5. Format teks status bayar (Helper Logic buat nentuin teks LUNAS/COD)
    private String formatStatusBayar(Peminjaman p) {
        String raw = p.getStatusPembayaran();
        boolean sewaCOD = p.getMetodeBayar() != null && p.getMetodeBayar().contains("CASH");
        boolean dendaLunas = "SUDAH_BAYAR".equals(raw);
        boolean dendaCOD =
                p.getMetodeBayarDenda() != null && p.getMetodeBayarDenda().contains("CASH");

        String teksSewa = sewaCOD ? "LUNAS (COD)" : "LUNAS";

        // Cek Telat
        LocalDate tglKembali = LocalDate.now();
        try {
            tglKembali = LocalDate.parse(p.getTanggalKembali());
        } catch (Exception e) {
        }
        boolean isLate = "Dipinjam".equals(p.getStatus()) && LocalDate.now().isAfter(tglKembali);

        if (dendaLunas) {
            String teksDenda = dendaCOD ? "Lunas Denda (COD)" : "Lunas Denda";
            return teksSewa + ", " + teksDenda;
        } else if (isLate) {
            return "TELAT DENDA";
        } else if ("MENUNGGU_PEMBAYARAN".equals(raw)) {
            return "MENUNGGU KONFIRMASI";
        } else if ("LUNAS".equals(raw)) {
            return teksSewa;
        }

        return raw != null ? raw : "-";
    }
}
