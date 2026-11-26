package com.example.budgetetudiant;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
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
        tvTotal.setText(String.format("Total : %.2f TND", db.total()));
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
}