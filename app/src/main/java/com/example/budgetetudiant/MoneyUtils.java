package com.example.budgetetudiant;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formatage et validation des montants en dinars tunisiens.
 * Deux décimales minimum, une troisième si elle est nécessaire.
 */
public final class MoneyUtils {

    public static final String CURRENCY = "TND";

    private MoneyUtils() {
    }

    public static String formatAmount(double value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.FRANCE);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0.00#", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    public static String format(double value) {
        return formatAmount(value) + " " + CURRENCY;
    }

    public static String formatCurrency(double value, String currency) {
        return formatAmount(value) + " " + (currency == null ? "" : currency);
    }

    public static String formatPercent(double percent) {
        return Math.round(percent) + " %";
    }

    public static int percent(double used, double total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((used / total) * 100);
    }

    public static double parseAmount(String raw) {
        if (raw == null) {
            return Double.NaN;
        }
        String cleaned = raw.trim()
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(',', '.');
        if (cleaned.isEmpty()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public static boolean isValidAmount(double amount) {
        return !Double.isNaN(amount) && !Double.isInfinite(amount) && amount > 0;
    }

    /** Format compact pour les graphiques : 1,2 k / 3,4 M. */
    public static String compact(double value) {
        double absolute = Math.abs(value);
        if (absolute >= 1_000_000) {
            return String.format(Locale.FRANCE, "%.1f M", value / 1_000_000);
        }
        if (absolute >= 1_000) {
            return String.format(Locale.FRANCE, "%.1f k", value / 1_000);
        }
        return formatAmount(value);
    }
}
