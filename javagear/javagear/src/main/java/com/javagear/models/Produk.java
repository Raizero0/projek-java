package com.javagear.models;

public class Produk {
    private int id;
    private String nama;
    private String kategori; // Fitur Kategori lu
    private String deskripsi;
    private double hargaSewa;
    private int stok;
    private int jumlahSewa = 0;

    public Produk(int id, String nama, String kategori, String deskripsi, double hargaSewa,
            int stok) {
        this.id = id;
        this.nama = nama;
        this.kategori = kategori;
        this.deskripsi = deskripsi;
        this.hargaSewa = hargaSewa;
        this.stok = stok;
        this.jumlahSewa = 0;
    }

    // Getter 
    public int getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public String getKategori() {
        return kategori;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public double getHargaSewa() {
        return hargaSewa;
    }

    public int getStok() {
        return stok;
    }

    public int getJumlahSewa() {
        return jumlahSewa;
    }

    public void setJumlahSewa(int jumlahSewa) {
        this.jumlahSewa = jumlahSewa;
    }
}
