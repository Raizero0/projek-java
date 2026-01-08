package com.javagear.models;

import java.sql.Timestamp;

//child Inheritance
public class Notifikasi extends BaseNotifikasi implements PaymentMethod {

    private int id;
    private boolean isRead;
    private double totalDenda;
    private String statusBayar;
    private String createdAtString; 

    //polimorfisme statis
    public Notifikasi(int id, int userId, String pesan, boolean isRead, String createdAt,
            String tipe) {
        super(userId, pesan, tipe);
        this.id = id;
        this.isRead = isRead;
        this.totalDenda = 0.0;
        this.statusBayar = "BELUM_BAYAR";
        this.createdAtString = createdAt; 

        setCreatedAtFromString(createdAt);

        System.out.println("📧 Notifikasi created - ID: " + id + ", User: " + userId);
    }

    //polimorfisme statis
    public Notifikasi(int id, int userId, String pesan, boolean isRead, String createdAt,
            String tipe, double totalDenda, String statusBayar) {
        super(userId, pesan, tipe);
        this.id = id;
        this.isRead = isRead;
        this.totalDenda = totalDenda;
        this.statusBayar = statusBayar;
        this.createdAtString = createdAt;

        setCreatedAtFromString(createdAt);

        System.out.println("💰 Payment Notifikasi created - Denda: " + totalDenda + ", Status: "
                + statusBayar);
    }

    //polimorfisme statis
    public Notifikasi(int userId, String pesan, String tipe) {
        super(userId, pesan, tipe);
        this.id = 0; // ID akan di-generate database
        this.isRead = false;
        this.totalDenda = 0.0;
        this.statusBayar = "BELUM_BAYAR";
        this.createdAtString = getCreatedAt().toString();
    }

    //polimorfisme dinamis child
    @Override
    public boolean send() {
        System.out.println("🚀 MENGIRIM NOTIFIKASI...");
        System.out.println("   ID: " + id);
        System.out.println("   User: " + getUserId());
        System.out.println("   Tipe: " + getTipe());
        System.out.println("   Read: " + isRead);
        boolean success = false;

        switch (getTipe()) {
            case "admin_ke_user":
                System.out.println("   📨 Admin → User - Priority High");
                success = true;
                break;
            case "user_ke_admin":
                System.out.println("   📤 User → Admin - Priority Medium");
                success = true;
                break;
            case "system":
                System.out.println("   ⚙️ System Notification - Priority High");
                success = true;
                break;
            default:
                System.out.println("   📝 General Notification - Priority Low");
                success = true;
        }

        if (success) {
            markAsSent();
            System.out.println("✅ Notifikasi berhasil dikirim!");
        } else {
            System.out.println("❌ Gagal mengirim notifikasi");
        }

        return success;
    }

    @Override
    public String getStatus() {
        if (isSent()) {
            return "TERKIRIM";
        } else if (isRead) {
            return "DIBACA";
        } else {
            return "MENUNGGU";
        }
    }

    @Override
    public String getPriority() {
        // TENTUKAN PRIORITY BERDASARKAN TIPE NOTIFIKASI
        switch (getTipe()) {
            case "admin_ke_user":
            case "system":
                return "HIGH";
            case "user_ke_admin":
                return "MEDIUM";
            default:
                return "LOW";
        }
    }

    @Override
    public boolean processPayment(double amount) {
        System.out.println("💳 PROSES PEMBAYARAN VIA NOTIFIKASI...");
        System.out.println("   Notifikasi ID: " + id);
        System.out.println("   User ID: " + getUserId());
        System.out.println("   Amount: Rp " + String.format("%,.0f", amount));

        if (!validateAmount(amount)) {
            System.out.println("❌ Validasi amount gagal!");
            this.statusBayar = "GAGAL";
            return false;
        }

        if (totalDenda > 0 && amount >= totalDenda) {
            System.out.println("✅ Pembayaran mencukupi untuk melunasi denda");
        } else if (totalDenda > 0 && amount < totalDenda) {
            System.out.println("⚠️ Pembayaran kurang dari total denda");
        }

        try {
            Thread.sleep(300); 

            double tax = PaymentMethod.calculateTax(amount, TAX_RATE);
            double totalAmount = amount + tax;

            System.out.println("   Tax (11%): Rp " + String.format("%,.0f", tax));
            System.out.println("   Total: Rp " + String.format("%,.0f", totalAmount));

            this.statusBayar = "LUNAS";

            String paymentInfo = "\n\n--- PAYMENT CONFIRMATION ---" + "\n💰 Amount: Rp "
                    + String.format("%,.0f", amount) + "\n📊 Tax (11%): Rp "
                    + String.format("%,.0f", tax) + "\n💵 Total: Rp "
                    + String.format("%,.0f", totalAmount) + "\n✅ Status: " + statusBayar
                    + "\n💳 Method: NOTIFIKASI_PAYMENT" + "\n🏦 Currency: " + getDefaultCurrency()
                    + "\n📅 Time: " + new java.util.Date();

            updatePesan(getPesan() + paymentInfo, true);

            System.out.println("✅ Pembayaran berhasil diproses via notifikasi!");
            return true;

        } catch (InterruptedException e) {
            System.err.println("❌ Gagal proses pembayaran: " + e.getMessage());
            this.statusBayar = "GAGAL";
            return false;
        }
    }

    @Override
    public String getPaymentDetails() {
        return "🔐 PAYMENT DETAILS - NOTIFIKASI SYSTEM\n" + "────────────────────────────\n"
                + "📧 Notifikasi ID: " + id + "\n" + "👤 User ID: " + getUserId() + "\n"
                + "📝 Tipe: " + getTipe() + "\n" + "💰 Total Denda: Rp "
                + String.format("%,.0f", totalDenda) + "\n" + "✅ Payment Status: " + statusBayar
                + "\n" + "🎯 Priority: " + getPriority() + "\n" + "🏦 Supported Banks: "
                + PaymentMethod.getSupportedBanks() + "\n" + "💸 Tax Rate: " + (TAX_RATE * 100)
                + "%\n" + "📅 Created: " + getCreatedAtString() + "\n" + "📤 Notification Status: "
                + getStatus();
    }

    @Override
    public String getPreview() {
        String basePreview = super.getPreview();
        if (totalDenda > 0) {
            return "💰 " + basePreview + " [Denda: Rp " + String.format("%,.0f", totalDenda) + "]";
        }
        if ("LUNAS".equals(statusBayar)) {
            return "✅ " + basePreview + " [LUNAS]";
        }
        return basePreview;
    }

    @Override
    public void markAsRead() {
        this.isRead = true;
        System.out.println("👀 Notifikasi ID " + id + " ditandai sebagai dibaca");
    }

    @Override
    public void displayInfo() {
        System.out.println("\n📋 INFORMASI NOTIFIKASI LENGKAP:");
        System.out.println("   📧 ID: " + id);
        System.out.println("   👤 User ID: " + getUserId());
        System.out.println("   📝 Tipe: " + getTipe());
        System.out.println("   🎯 Priority: " + getPriority());
        System.out.println("   👀 Is Read: " + (isRead ? "Yes" : "No"));
        System.out.println("   💰 Total Denda: Rp " + String.format("%,.0f", totalDenda));
        System.out.println("   ✅ Payment Status: " + statusBayar);
        System.out.println("   📨 Status: " + getStatus());
        System.out.println("   📅 Created: " + getCreatedAtString());
        System.out.println("   🔍 Preview: " + getPreview());

        // 🎯 TAMPILKAN PAYMENT DETAILS JIKA ADA PEMBAYARAN
        if (totalDenda > 0 && !"BELUM_BAYAR".equals(statusBayar)) {
            System.out.println("\n💳 PAYMENT INFORMATION:");
            System.out.println(getPaymentDetails().replace("\n", "\n   "));
        }
    }

    public void markAsRead(boolean updateTimestamp) {
        this.isRead = true;
        if (updateTimestamp) {
            System.out.println("👀 Notifikasi dibaca dengan timestamp - ID: " + id);
        } else {
            System.out.println("👀 Notifikasi dibaca - ID: " + id);
        }
    }

    public void updateStatusBayar(String newStatus) {
        String oldStatus = this.statusBayar;
        this.statusBayar = newStatus;
        System.out.println("🔄 Status bayar updated: " + oldStatus + " → " + newStatus);
    }

    public void updateStatusBayar(String newStatus, String reason) {
        String oldStatus = this.statusBayar;
        this.statusBayar = newStatus;
        System.out.println("🔄 Status bayar updated: " + oldStatus + " → " + newStatus
                + " [Reason: " + reason + "]");
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public String getCreatedAtString() {
        return this.createdAtString != null ? this.createdAtString
                : (getCreatedAt() != null ? getCreatedAt().toString() : "");
    }

    private void setCreatedAtFromString(String createdAtStr) {
        this.createdAtString = createdAtStr;
        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            try {
                // Format: "2024-01-01 10:00:00" atau "2024-01-01T10:00:00"
                String formatted = createdAtStr.replace('T', ' ');
                setCreatedAt(Timestamp.valueOf(formatted));
            } catch (Exception e) {
                System.err.println(
                        "⚠️ Gagal parse createdAt: " + createdAtStr + ", using current time");
                setCreatedAt(new Timestamp(System.currentTimeMillis()));
                this.createdAtString = getCreatedAt().toString();
            }
        }
    }

    public double getTotalDenda() {
        return totalDenda;
    }

    public void setTotalDenda(double totalDenda) {
        this.totalDenda = totalDenda;
    }

    public String getStatusBayar() {
        return statusBayar;
    }

    public void setStatusBayar(String statusBayar) {
        this.statusBayar = statusBayar;
    }

    public boolean isPaymentNotification() {
        return totalDenda > 0 || !"BELUM_BAYAR".equals(statusBayar);
    }

    public String getSummary() {
        return String.format("Notifikasi[ID:%d, User:%d, Type:%s, Read:%s, Payment:%s]", id,
                getUserId(), getTipe(), isRead ? "YES" : "NO",
                isPaymentNotification() ? "YES" : "NO");
    }

    public static void demoPolymorphism() {
        System.out.println("\n🎯 DEMO POLYMORPHISM - NOTIFIKASI SYSTEM");

        Notifikasi notif1 = new Notifikasi(1, 101, "Pesan biasa dari admin", false,
                "2024-01-01 10:00:00", "admin_ke_user");
        Notifikasi notif2 = new Notifikasi(2, 102, "Penting: Keterlambatan pengembalian", false,
                "2024-01-01 11:00:00", "system");
        Notifikasi notif3 = new Notifikasi(3, 103, "Konfirmasi pembayaran denda", false,
                "2024-01-01 12:00:00", "user_ke_admin", 150000, "BELUM_BAYAR");

        BaseNotifikasi[] notifikasiArray = {notif1, notif2, notif3};

        for (BaseNotifikasi notif : notifikasiArray) {
            System.out.println("\n🔹 Processing: " + notif.getPreview());
            System.out.println("   Status: " + notif.getStatus());
            System.out.println("   Priority: " + notif.getPriority());

            if (notif instanceof Notifikasi) {
                Notifikasi n = (Notifikasi) notif;
                if (n.isPaymentNotification()) {
                    System.out.println("   💰 Payment Amount: Rp "
                            + String.format("%,.0f", n.getTotalDenda()));
                }
            }
        }

        System.out.println("\n💳 DEMO PAYMENT PROCESSING:");
        PaymentMethod paymentNotif = notif3;
        boolean success = paymentNotif.processPayment(150000);
        System.out.println("   Payment Result: " + (success ? "SUCCESS" : "FAILED"));
        if (success) {
            System.out.println("   Payment Details:\n" + paymentNotif.getPaymentDetails());
        }
    }
}
