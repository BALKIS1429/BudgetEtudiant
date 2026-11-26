package com.example.budgetetudiant;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

/**
 * Convertisseur TND vers EUR / USD / GBP, basé sur l'API open.er-api.com avec
 * un cache local du dernier taux connu.
 */
public class ConverterActivity extends BaseActivity {

    private static final String[] CURRENCIES = {"EUR", "USD", "GBP"};

    private TextInputLayout tilTnd;
    private EditText etTnd;
    private MaterialAutoCompleteTextView spCurrency;
    private MaterialButton btnConvert;
    private TextView tvResult;
    private TextView tvRateInfo;
    private TextView tvCacheWarning;

    private CurrencyApi.Rates rates;
    private boolean loading;
    private double pendingAmount = -1;
    private String pendingCurrency;
    private String convertButtonText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_converter);
        applyInsets(R.id.converterRoot);

        tilTnd = findViewById(R.id.til_tnd);
        etTnd = findViewById(R.id.etTnd);
        spCurrency = findViewById(R.id.spCurrency);
        btnConvert = findViewById(R.id.btnConvert);
        tvResult = findViewById(R.id.tvResult);
        tvRateInfo = findViewById(R.id.tv_rate_info);
        tvCacheWarning = findViewById(R.id.tv_cache_warning);
        convertButtonText = getString(R.string.convert);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, CURRENCIES);
        spCurrency.setAdapter(adapter);
        String savedCurrency = AppPreferences.getLastCurrency(this);
        spCurrency.setText(containsCurrency(savedCurrency) ? savedCurrency : "EUR", false);
        spCurrency.setOnItemClickListener((parent, view, position, id) -> {
            if (rates != null) {
                updateRateInfo();
            }
        });

        etTnd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilTnd.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnConvert.setOnClickListener(v -> convert());
        fetchRates();
    }

    private boolean containsCurrency(String currency) {
        for (String value : CURRENCIES) {
            if (value.equals(currency)) {
                return true;
            }
        }
        return false;
    }

    private String selectedCurrency() {
        String value = spCurrency.getText() == null ? "" : spCurrency.getText().toString().trim();
        return containsCurrency(value) ? value : "EUR";
    }

    private void fetchRates() {
        if (loading) {
            return;
        }
        loading = true;
        btnConvert.setEnabled(false);
        btnConvert.setText(R.string.converter_loading);
        tvCacheWarning.setVisibility(TextView.GONE);

        CurrencyApi.fetchRates(this, new CurrencyApi.Callback() {
            @Override
            public void onSuccess(CurrencyApi.Rates result, boolean fromCache) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                loading = false;
                rates = result;
                btnConvert.setEnabled(true);
                btnConvert.setText(convertButtonText);
                updateRateInfo();

                if (fromCache) {
                    showWarning(getString(R.string.converter_offline, result.updatedLabel),
                            R.color.warning);
                } else {
                    tvCacheWarning.setVisibility(TextView.GONE);
                }

                if (pendingAmount > 0) {
                    double amount = pendingAmount;
                    String currency = pendingCurrency == null
                            ? selectedCurrency() : pendingCurrency;
                    pendingAmount = -1;
                    pendingCurrency = null;
                    displayConversion(amount, currency);
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                loading = false;
                btnConvert.setEnabled(true);
                btnConvert.setText(convertButtonText);
                showWarning(message, R.color.danger);
            }
        });
    }

    private void convert() {
        double amount = MoneyUtils.parseAmount(etTnd.getText().toString());
        if (!MoneyUtils.isValidAmount(amount)) {
            tilTnd.setError(getString(R.string.converter_error_amount));
            return;
        }

        if (rates == null) {
            pendingAmount = amount;
            pendingCurrency = selectedCurrency();
            fetchRates();
            return;
        }
        displayConversion(amount, selectedCurrency());
    }

    private void displayConversion(double amount, String currency) {
        double rate = rates.rateFor(currency);
        double result = amount * rate;
        tvResult.setText(MoneyUtils.formatCurrency(result, currency));
        updateRateInfo();
        AppPreferences.setLastCurrency(this, currency);
    }

    private void updateRateInfo() {
        if (rates == null) {
            return;
        }
        String currency = selectedCurrency();
        double rate = rates.rateFor(currency);
        String rateLabel = getString(R.string.converter_rate_info,
                String.format(Locale.FRANCE, "%.4f", rate), currency);
        String updatedLabel = getString(R.string.converter_updated, rates.updatedLabel);
        tvRateInfo.setText(rateLabel + "\n" + updatedLabel);
    }

    private void showWarning(String message, int colorRes) {
        tvCacheWarning.setText(message);
        tvCacheWarning.setTextColor(ContextCompat.getColor(this, colorRes));
        tvCacheWarning.setVisibility(TextView.VISIBLE);
    }
}
