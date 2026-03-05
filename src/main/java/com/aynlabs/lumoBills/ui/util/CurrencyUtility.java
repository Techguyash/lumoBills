package com.aynlabs.lumoBills.ui.util;

public class CurrencyUtility {
    public static String getCurrencySymbol(String currencyCode) {
        if (currencyCode == null)
            return "$";
        return switch (currencyCode) {
            case "INR" -> "₹";
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "JPY" -> "¥";
            default -> currencyCode + " ";
        };
    }
}
