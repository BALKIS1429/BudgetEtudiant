package com.example.budgetetudiant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DBHelper db;
    private ListView listView;
    private TextView tvTotal;
    private List<Expense> expenses;
    private ProgressBar progressBudget;
    private TextView tvBudget;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = new DBHelper(this);
        listView = findViewById(R.id.listExpenses);
        tvTotal = findViewById(R.id.tvTotal);
        progressBudget = findViewById(R.id.progressBudget);
        tvBudget = findViewById(R.id.tvBudget);
        prefs = getSharedPreferences("budget_prefs", MODE_PRIVATE);

        findViewById(R.id.btnBudget).setOnClickListener(v -> showBudgetDialog());
        findViewById(R.id.btnAdd).setOnClickListener(v -> showAddDialog());
        findViewById(R.id.btnConverter).setOnClickListener(v ->
                startActivity(new Intent(this, ConverterActivity.class)));

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Expense e = expenses.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Supprimer ?")
                    .setMessage(e.title)
                    .setPositiveButton("Oui", (d, w) -> {
                        db.delete(e.id);
                        refresh();
                    })
                    .setNegativeButton("Non", null)
                    .show();
            return true;
        });

        refresh();
    }

    private void refresh() {
        expenses = db.getAll();
        listView.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, expenses));

        double total = db.total();
        float budget = prefs.getFloat("budget", 0);
        tvTotal.setText(String.format("Total : %.2f TND", total));

        if (budget <= 0) {
            tvBudget.setText("Budget non défini");
            tvBudget.setTextColor(Color.GRAY);
            tvTotal.setTextColor(Color.BLACK);
            progressBudget.setProgress(0);
        } else {
            int percent = (int) Math.min(100, (total / budget) * 100);
            progressBudget.setProgress(percent);
            if (total > budget) {
                tvTotal.setTextColor(Color.RED);
                tvBudget.setTextColor(Color.RED);
                tvBudget.setText(String.format("⚠ Budget dépassé de %.2f TND (budget : %.2f)",
                        total - budget, budget));
            } else {
                tvTotal.setTextColor(Color.BLACK);
                tvBudget.setTextColor(Color.DKGRAY);
                tvBudget.setText(String.format("Reste : %.2f TND sur %.2f (%d%%)",
                        budget - total, budget, percent));
            }
        }
    }

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add, null);
        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etAmount = view.findViewById(R.id.etAmount);
        Spinner sp = view.findViewById(R.id.spCategory);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Nourriture", "Transport", "Études", "Loisirs", "Autre"}));

        new AlertDialog.Builder(this)
                .setTitle("Nouvelle dépense")
                .setView(view)
                .setPositiveButton("Ajouter", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();
                    if (title.isEmpty() || amountStr.isEmpty()) {
                        Toast.makeText(this, "Remplissez tous les champs", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.add(title, Double.parseDouble(amountStr), sp.getSelectedItem().toString());
                    refresh();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showBudgetDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Ex : 500");

        new AlertDialog.Builder(this)
                .setTitle("Budget mensuel (TND)")
                .setView(input)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    String s = input.getText().toString().trim();
                    if (s.isEmpty()) return;
                    prefs.edit().putFloat("budget", Float.parseFloat(s)).apply();
                    refresh();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}