package com.example.budgetetudiant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utilitaires de dates et de mois. Les formats de stockage utilisent Locale.US
 * pour éviter tout problème de chiffres ou de séparateurs selon la locale.
 */
public final class DateUtils {

    public static final Locale DISPLAY_LOCALE = Locale.FRANCE;

    private DateUtils() {
    }

    public static String today() {
        return format(new Date(), "yyyy-MM-dd");
    }

    public static String currentMonth() {
        return format(new Date(), "yyyy-MM");
    }

    public static String nowTimestamp() {
        return format(new Date(), "yyyy-MM-dd HH:mm:ss");
    }

    public static String format(Date date, String pattern) {
        return new SimpleDateFormat(pattern, Locale.US).format(date);
    }

    public static Date parse(String value, String pattern) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
            sdf.setLenient(false);
            return sdf.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String monthOf(String isoDate) {
        if (isoDate != null && isoDate.length() >= 7) {
            return isoDate.substring(0, 7);
        }
        return currentMonth();
    }

    /** Exemple : "Novembre 2026". */
    public static String monthLabel(String month) {
        Date date = parse(month, "yyyy-MM");
        if (date == null) {
            return month == null ? "" : month;
        }
        String label = new SimpleDateFormat("MMMM yyyy", DISPLAY_LOCALE).format(date);
        return capitalize(label);
    }

    /** Exemple : "Nov." pour les graphiques. */
    public static String monthShort(String month) {
        Date date = parse(month, "yyyy-MM");
        if (date == null) {
            return month == null ? "" : month;
        }
        String label = new SimpleDateFormat("MMM", DISPLAY_LOCALE).format(date);
        return capitalize(label);
    }

    /** Exemple : "27 nov. 2026". */
    public static String dateLabel(String isoDate) {
        Date date = parse(isoDate, "yyyy-MM-dd");
        if (date == null) {
            return isoDate == null ? "" : isoDate;
        }
        return new SimpleDateFormat("dd MMM yyyy", DISPLAY_LOCALE).format(date);
    }

    /** Exemple : "27/11/2026". */
    public static String dateNumeric(String isoDate) {
        Date date = parse(isoDate, "yyyy-MM-dd");
        if (date == null) {
            return isoDate == null ? "" : isoDate;
        }
        return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date);
    }

    /** Exemple : "Vendredi 27 novembre 2026". */
    public static String todayLongLabel() {
        String label = new SimpleDateFormat("EEEE d MMMM yyyy", DISPLAY_LOCALE).format(new Date());
        return capitalize(label);
    }

    public static String addMonths(String month, int delta) {
        Date date = parse(month, "yyyy-MM");
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, delta);
        return format(calendar.getTime(), "yyyy-MM");
    }

    public static String previousMonth(String month) {
        return addMonths(month, -1);
    }

    public static String nextMonth(String month) {
        return addMonths(month, 1);
    }

    /** Renvoie les mois du plus ancien au plus récent, en incluant le mois courant. */
    public static String[] lastMonthKeys(int count) {
        int safeCount = Math.max(1, count);
        String[] keys = new String[safeCount];
        String current = currentMonth();
        for (int i = 0; i < safeCount; i++) {
            keys[i] = addMonths(current, -(safeCount - 1 - i));
        }
        return keys;
    }

    public static boolean isFutureMonth(String month) {
        return month != null && month.compareTo(currentMonth()) > 0;
    }

    public static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
