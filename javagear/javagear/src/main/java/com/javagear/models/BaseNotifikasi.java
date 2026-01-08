package com.javagear.models;

import java.sql.Timestamp;

// parent Inheritance
public abstract class BaseNotifikasi {

    protected int userId;
    protected String pesan;
    protected String tipe;
    protected Timestamp createdAt;
    protected boolean isSent;

    public BaseNotifikasi(int userId, String pesan, String tipe) {
        this.userId = userId;
        this.pesan = pesan;
        this.tipe = tipe;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.isSent = false;

        System.out.println("📝 BaseNotifikasi created for user: " + userId);
    }

    // polimorfisme dinamis parent
    public abstract boolean send();

    public abstract String getStatus();

    public abstract String getPriority();

    public String getPreview() {
        if (pesan == null || pesan.isEmpty()) {
            return "[Pesan kosong]";
        }
        return pesan.length() > 50 ? pesan.substring(0, 50) + "..." : pesan;
    }

    public void markAsSent() {
        this.isSent = true;
        System.out.println("✅ Notifikasi marked as sent for user: " + userId);
    }

    public void markAsRead() {
        System.out.println("👀 Notifikasi dibaca oleh user: " + userId);
    }

    public void updatePesan(String newPesan) {
        this.pesan = newPesan;
        System.out.println("✏️ Pesan diperbarui: " + getPreview());
    }

    public void updatePesan(String newPesan, boolean addTimestamp) {
        if (addTimestamp) {
            this.pesan = newPesan + " [Updated: " + new java.util.Date() + "]";
        } else {
            this.pesan = newPesan;
        }
        System.out.println("✏️ Pesan diperbarui dengan timestamp: " + getPreview());
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPesan() {
        return pesan;
    }

    public void setPesan(String pesan) {
        this.pesan = pesan;
    }

    public String getTipe() {
        return tipe;
    }

    public void setTipe(String tipe) {
        this.tipe = tipe;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSent() {
        return isSent;
    }

    public void displayInfo() {
        interface InfoNotif {
            void displayInfo(); 
        }

        // Anonymous Class
        InfoNotif tampil = new InfoNotif() {
            @Override
            public void displayInfo() {
                System.out.println("\n📋 INFORMASI NOTIFIKASI (VIA ANONYMOUS):");
                System.out.println("   User ID: " + userId);
                System.out.println("   Tipe: " + tipe);
                System.out.println("   Preview: " + getPreview());
                System.out.println("   Created: " + createdAt);
                System.out.println("   Status: " + getStatus());
                System.out.println("   Priority: " + getPriority());
                System.out.println("   Sent: " + (isSent ? "Yes" : "No"));
            }
        }; 

        tampil.displayInfo(); 
    }

    public static int getTotalNotificationTypes() {
        return 4; // admin_ke_user, user_ke_admin, system, reminder
    }
}
