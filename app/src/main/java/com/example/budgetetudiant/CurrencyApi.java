package com.example.budgetetudiant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client de l'API de change existante (open.er-api.com).
 * Conserve un cache local des derniers taux pour rester utile hors ligne.
 */
public final class CurrencyApi {

    private static final String API_URL = "https://open.er-api.com/v6/latest/TND";
    private static final String CACHE_PREFS = "currency_cache";
    private static final String KEY_RATES = "rates_json";
    private static final String KEY_UPDATED = "updated_label";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private CurrencyApi() {
    }

    public interface Callback {
        void onSuccess(Rates rates, boolean fromCache);

        void onError(String message);
    }

    public static final class Rates {
        public final double eur;
        public final double usd;
        public final double gbp;
        public final String updatedLabel;

        Rates(double eur, double usd, double gbp, String updatedLabel) {
            this.eur = eur;
            this.usd = usd;
            this.gbp = gbp;
            this.updatedLabel = updatedLabel;
        }

        public double rateFor(String currency) {
            if ("USD".equalsIgnoreCase(currency)) {
                return usd;
            }
            if ("GBP".equalsIgnoreCase(currency)) {
                return gbp;
            }
            return eur;
        }
    }

    public static void fetchRates(final Context context, final Callback callback) {
        final Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                Rates rates = fetchFromNetwork();
                saveCache(appContext, rates);
                MAIN_HANDLER.post(() -> callback.onSuccess(rates, false));
            } catch (Exception networkError) {
                Rates cached = readCache(appContext);
                if (cached != null) {
                    MAIN_HANDLER.post(() -> callback.onSuccess(cached, true));
                } else {
                    MAIN_HANDLER.post(() -> callback.onError(
                            context.getString(R.string.converter_network_error)));
                }
            }
        });
    }

    private static Rates fetchFromNetwork() throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setUseCaches(false);

            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String body = readStream(stream);
            if (responseCode != 200) {
                throw new IllegalStateException("HTTP " + responseCode);
            }

            JSONObject root = new JSONObject(body);
            if (!"success".equalsIgnoreCase(root.optString("result"))) {
                throw new IllegalStateException("API result not success");
            }
            JSONObject rates = root.getJSONObject("rates");
            return new Rates(
                    rates.getDouble("EUR"),
                    rates.getDouble("USD"),
                    rates.getDouble("GBP"),
                    formatUpdated(root.optString("time_last_update_utc")));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static String formatUpdated(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "—";
        }
        try {
            SimpleDateFormat source = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
            Date date = source.parse(raw);
            if (date == null) {
                return raw;
            }
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(date);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private static void saveCache(Context context, Rates rates) {
        try {
            JSONObject json = new JSONObject();
            json.put("EUR", rates.eur);
            json.put("USD", rates.usd);
            json.put("GBP", rates.gbp);
            SharedPreferences prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_RATES, json.toString())
                    .putString(KEY_UPDATED, rates.updatedLabel)
                    .apply();
        } catch (Exception ignored) {
            // Le cache est un bonus ; une erreur de cache ne doit pas bloquer l'appel.
        }
    }

    private static Rates readCache(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_RATES, null);
            if (json == null) {
                return null;
            }
            JSONObject rates = new JSONObject(json);
            String updated = prefs.getString(KEY_UPDATED, "—");
            return new Rates(
                    rates.getDouble("EUR"),
                    rates.getDouble("USD"),
                    rates.getDouble("GBP"),
                    updated);
        } catch (Exception ignored) {
            return null;
        }
    }
}
