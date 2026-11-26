package com.example.budgetetudiant;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConverterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_converter);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.converterRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etTnd = findViewById(R.id.etTnd);
        Spinner sp = findViewById(R.id.spCurrency);
        TextView tvResult = findViewById(R.id.tvResult);

        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"EUR", "USD", "GBP"}));

        findViewById(R.id.btnConvert).setOnClickListener(v -> {
            String s = etTnd.getText().toString().trim();
            if (s.isEmpty()) {
                Toast.makeText(this, "Entrez un montant", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = Double.parseDouble(s);
            String currency = sp.getSelectedItem().toString();
            tvResult.setText("Chargement...");

            new Thread(() -> {
                try {
                    URL url = new URL("https://open.er-api.com/v6/latest/TND");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();

                    JSONObject rates = new JSONObject(sb.toString()).getJSONObject("rates");
                    double result = amount * rates.getDouble(currency);

                    runOnUiThread(() -> tvResult.setText(
                            String.format("%.2f TND = %.2f %s", amount, result, currency)));
                } catch (Exception e) {
                    runOnUiThread(() -> tvResult.setText("Erreur : vérifiez votre connexion"));
                }
            }).start();
        });
    }
}