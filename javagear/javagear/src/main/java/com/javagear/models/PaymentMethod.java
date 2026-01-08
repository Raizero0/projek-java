package com.javagear.models;

public interface PaymentMethod {

    boolean processPayment(double amount);

    String getPaymentDetails();

    default String getDefaultCurrency() {
        return "IDR";
    }

    default boolean validateAmount(double amount) {
        return amount > 0 && amount <= 100000000; 
    }

    static String getSupportedBanks() {
        return "BCA, BRI, BNI, Mandiri, CIMB";
    }

    static double calculateTax(double amount, double taxRate) {
        return amount * taxRate;
    }

    String BANK_BCA = "BCA";
    String BANK_BRI = "BRI";
    String BANK_BNI = "BNI";
    String BANK_MANDIRI = "Mandiri";

    double TAX_RATE = 0.11; 
}
