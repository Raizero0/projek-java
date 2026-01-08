package com.javagear.models;

public class Peminjaman {
    private int id;
    private int userId; 
    private String namaUser;
    private String namaProduk;
    private String tanggalPinjam;
    private String tanggalKembali;
    private String status;
    private double denda;
    private String statusPembayaran;
    private String metodeBayar;
    private int jumlahBarang;
    private String metodeBayarDenda;
    private double totalBayar;

    public Peminjaman(int id, int userId, String namaUser, String namaProduk, String tanggalPinjam,
            String tanggalKembali, String status, double denda, String statusPembayaran,
            int jumlahBarang, String metodeBayar, String metodeBayarDenda, double totalBayar) {
        this.id = id;
        this.userId = userId; 
        this.namaUser = namaUser;
        this.namaProduk = namaProduk;
        this.tanggalPinjam = tanggalPinjam;
        this.tanggalKembali = tanggalKembali;
        this.status = status;
        this.denda = denda;
        this.statusPembayaran = statusPembayaran;
        this.jumlahBarang = jumlahBarang;
        this.metodeBayar = metodeBayar;
        this.metodeBayarDenda = metodeBayarDenda;
        this.totalBayar = totalBayar;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public String getNamaUser() {
        return namaUser;
    }

    public String getNamaProduk() {
        return namaProduk;
    }

    public String getTanggalPinjam() {
        return tanggalPinjam;
    }

    public String getTanggalKembali() {
        return tanggalKembali;
    }

    public String getStatus() {
        return status;
    }

    public double getTotalBayar() {
        return totalBayar;
    }

    public double getDenda() {
        return denda;
    }

    public String getStatusPembayaran() {
        return statusPembayaran != null ? statusPembayaran : "BELUM_BAYAR";
    }

    public int getJumlahBarang() {
        return jumlahBarang;
    }

    public String getMetodeBayar() {
        return metodeBayar;
    }

    public String getMetodeBayarDenda() {
        return metodeBayarDenda;
    }

    // SETTER
    public void setId(int id) {
        this.id = id;
    }

    public void setNamaUser(String namaUser) {
        this.namaUser = namaUser;
    }

    public void setNamaProduk(String namaProduk) {
        this.namaProduk = namaProduk;
    }

    public void setTanggalPinjam(String tanggalPinjam) {
        this.tanggalPinjam = tanggalPinjam;
    }

    public void setTanggalKembali(String tanggalKembali) {
        this.tanggalKembali = tanggalKembali;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDenda(double denda) {
        this.denda = denda;
    }

    public void setStatusPembayaran(String statusPembayaran) {
        this.statusPembayaran = statusPembayaran;
    }

    public void setJumlahBarang(int jumlahBarang) {
        this.jumlahBarang = jumlahBarang;
    }
}
