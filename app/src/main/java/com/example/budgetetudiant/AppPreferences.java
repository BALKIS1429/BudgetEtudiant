package com.example.budgetetudiant;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Préférences locales. On conserve le nom "budget_prefs" utilisé par l'ancienne
 * version afin de ne pas perdre le budget déjà enregistré.
 */
public final class AppPreferences {

    public static final String PREFS_NAME = "budget_prefs";
    public static final String ALERTS_PREFS = "alert_prefs";
    public static final String MIGRATION_PREFS = "migration_prefs";

    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_LEGACY_BUDGET = "budget";
    private static final String KEY_LAST_CURRENCY = "last_currency";

    private AppPreferences() {
    }

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getAlerts(Context context) {
        return context.getSharedPreferences(ALERTS_PREFS, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getMigration(Context context) {
        return context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE);
    }

    public static String getUserName(Context context) {
        return get(context).getString(KEY_USER_NAME, "").trim();
    }

    public static void setUserName(Context context, String name) {
        get(context).edit().putString(KEY_USER_NAME, name == null ? "" : name.trim()).apply();
    }

    public static boolean areNotificationsEnabled(Context context) {
        return get(context).getBoolean(KEY_NOTIFICATIONS, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    /** Budget de l'ancienne version, conservé uniquement pour la migration. */
    public static float getLegacyBudget(Context context) {
        return get(context).getFloat(KEY_LEGACY_BUDGET, 0f);
    }

    public static String getLastCurrency(Context context) {
        return get(context).getString(KEY_LAST_CURRENCY, "EUR");
    }

    public static void setLastCurrency(Context context, String currency) {
        get(context).edit().putString(KEY_LAST_CURRENCY, currency).apply();
    }
}
