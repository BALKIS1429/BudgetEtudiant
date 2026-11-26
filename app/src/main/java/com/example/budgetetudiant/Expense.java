package com.example.budgetetudiant;

import java.util.Locale;

/**
 * Modèle d'une dépense. La date est stockée au format ISO yyyy-MM-dd.
 */
public class Expense {

    public int id;
    public String title;
    public double amount;
    public String category;
    public String date;

    public Expense(int id, String title, double amount, String category) {
        this(id, title, amount, category, null);
    }

    public Expense(int id, String title, double amount, String category, String date) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    @Override
    public String toString() {
        return title + "  (" + category + ")\n" + String.format(Locale.FRANCE, "%.2f TND", amount);
    }
}
