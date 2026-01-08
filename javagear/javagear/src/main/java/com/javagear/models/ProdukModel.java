package com.javagear.models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProdukModel {

    private DatabaseConnection dbConnection;

    public ProdukModel() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public List<Produk> getAllProduk() {
        List<Produk> list = new ArrayList<>();
        String query = "SELECT * FROM produk ORDER BY nama";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Produk p = new Produk(rs.getInt("id"), rs.getString("nama"),
                        rs.getString("kategori"), rs.getString("deskripsi"),
                        rs.getDouble("harga_sewa"), rs.getInt("stok"));
                list.add(p);
            }

            System.out.println("✅ Load " + list.size() + " produk dari database.");

        } catch (Exception e) {
            System.err.println("❌ Error load produk: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public boolean isStokTersedia(int produkId, int jumlah) {
        String query = "SELECT stok FROM produk WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, produkId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int stok = rs.getInt("stok");
                return stok >= jumlah;
            }
        } catch (Exception e) {
            System.err.println("❌ Error cek stok: " + e.getMessage());
        }
        return false;
    }
}
