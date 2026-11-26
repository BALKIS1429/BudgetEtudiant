package com.example.budgetetudiant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Alertes locales pour le budget et l'objectif d'épargne.
 * Les drapeaux sont mémorisés afin d'éviter les notifications répétitives.
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "budget_alerts";

    private static final int ID_BUDGET_80 = 2001;
    private static final int ID_BUDGET_OVER = 2002;
    private static final int ID_SAVINGS_REACHED = 2003;

    private NotificationHelper() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.channel_description));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Vérifie le budget du mois courant. Une alerte à 80 % et une alerte de
     * dépassement sont envoyées au maximum une fois par mois et par seuil.
     */
    public static void checkBudgetAlerts(Context context) {
        if (!canNotify(context)) {
            return;
        }
        DBHelper db = new DBHelper(context);
        try {
            String month = DateUtils.currentMonth();
            double budget = db.getBudget(month);
            if (budget <= 0) {
                return;
            }
            double spent = db.totalForMonth(month);
            SharedPreferences prefs = AppPreferences.getAlerts(context);
            String key80 = "budget_80_" + month;
            String keyOver = "budget_over_" + month;

            if (spent >= budget) {
                if (!prefs.getBoolean(keyOver, false)) {
                    String message;
                    if (spent > budget) {
                        message = "Vous avez dépassé votre budget de " +
                                MoneyUtils.format(spent - budget) + " ce mois.";
                    } else {
                        message = "Votre budget de " + MoneyUtils.format(budget) +
                                " est entièrement utilisé.";
                    }
                    notify(context, ID_BUDGET_OVER,
                            context.getString(R.string.notification_budget_over_title),
                            message, "expenses");
                    prefs.edit().putBoolean(keyOver, true).putBoolean(key80, true).apply();
                }
            } else {
                int percent = MoneyUtils.percent(spent, budget);
                if (percent >= 80) {
                    if (!prefs.getBoolean(key80, false)) {
                        notify(context, ID_BUDGET_80,
                                context.getString(R.string.notification_budget_80_title),
                                "Il vous reste " + MoneyUtils.format(budget - spent) +
                                        " sur un budget de " + MoneyUtils.format(budget) + ".",
                                "expenses");
                        prefs.edit().putBoolean(key80, true).apply();
                    }
                } else {
                    // Redescendre sous 80 % réautorise une alerte si le seuil est
                    // franchi à nouveau plus tard.
                    prefs.edit().remove(key80).remove(keyOver).apply();
                }
            }
        } finally {
            db.close();
        }
    }

    /**
     * Une seule notification par objectif atteint. La signature change si
     * l'objectif est modifié, ce qui permet une nouvelle alerte légitime.
     */
    public static void checkSavingsGoalAlerts(Context context) {
        if (!canNotify(context)) {
            return;
        }
        DBHelper db = new DBHelper(context);
        try {
            SavingsGoal goal = db.getSavingsGoal();
            if (goal == null) {
                return;
            }
            SharedPreferences prefs = AppPreferences.getAlerts(context);
            String signature = goal.id + ":" + Math.round(goal.targetAmount * 100);
            String lastSignature = prefs.getString("savings_reached_signature", "");
            if (goal.isReached()) {
                if (!signature.equals(lastSignature)) {
                    String goalName = goal.name == null || goal.name.trim().isEmpty()
                            ? "objectif" : "« " + goal.name + " »";
                    notify(context, ID_SAVINGS_REACHED,
                            context.getString(R.string.notification_savings_title),
                            "Bravo ! Votre " + goalName + " est atteint avec " +
                                    MoneyUtils.format(goal.savedAmount) + " économisés.",
                            "savings");
                    prefs.edit().putString("savings_reached_signature", signature).apply();
                }
            } else {
                prefs.edit().remove("savings_reached_signature").apply();
            }
        } finally {
            db.close();
        }
    }

    public static void resetBudgetFlags(Context context, String month) {
        AppPreferences.getAlerts(context).edit()
                .remove("budget_80_" + month)
                .remove("budget_over_" + month)
                .apply();
    }

    private static boolean canNotify(Context context) {
        if (!AppPreferences.areNotificationsEnabled(context)) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    @SuppressLint("MissingPermission")
    private static void notify(Context context, int id, String title, String message, String tab) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, tab);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}
