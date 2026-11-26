package com.example.budgetetudiant;

public class Expense {
    public int id;
    public String title;
    public double amount;
    public String category;

    public Expense(int id, String title, double amount, String category) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
    }

    @Override
    public String toString() {
        return title + "  (" + category + ")\n" + String.format("%.2f", amount) + " TND";
    }
}