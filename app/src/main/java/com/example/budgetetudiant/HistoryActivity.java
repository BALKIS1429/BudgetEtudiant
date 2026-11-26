package com.example.budgetetudiant;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Map;

/**
 * Historique mensuel : sélection du mois, total, budget, répartition par
 * catégorie, détail des dépenses et export PDF.
 */
public class HistoryActivity extends BaseActivity {

    private static final String STATE_MONTH = "state_month";

    private DBHelper db;
    private String month;

    private TextView tvMonth;
    private TextView tvTotal;
    private TextView tvCount;
    private TextView tvBudget;
    private TextView tvRemaining;
    private TextView tvComparison;
    private TextView tvEmpty;
    private LinearLayout layoutCategories;
    private LinearLayout layoutExpenses;
    private ImageButton btnNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        applyInsets(R.id.history_root);

        db = new DBHelper(this);
        month = savedInstanceState == null
                ? DateUtils.currentMonth()
                : savedInstanceState.getString(STATE_MONTH, DateUtils.currentMonth());

        bindViews();
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            month = DateUtils.previousMonth(month);
            render();
        });
        btnNext.setOnClickListener(v -> {
            if (month.compareTo(DateUtils.currentMonth()) < 0) {
                month = DateUtils.nextMonth(month);
                render();
            }
        });
        findViewById(R.id.btn_export_pdf).setOnClickListener(v ->
                PdfExporter.exportAndShare(this, month));

        render();
    }

    private void bindViews() {
        tvMonth = findViewById(R.id.tv_history_month);
        tvTotal = findViewById(R.id.tv_history_total);
        tvCount = findViewById(R.id.tv_history_count);
        tvBudget = findViewById(R.id.tv_history_budget);
        tvRemaining = findViewById(R.id.tv_history_remaining);
        tvComparison = findViewById(R.id.tv_history_comparison);
        tvEmpty = findViewById(R.id.tv_history_empty);
        layoutCategories = findViewById(R.id.layout_history_categories);
        layoutExpenses = findViewById(R.id.layout_history_expenses);
        btnNext = findViewById(R.id.btn_next_month);
    }

    private void render() {
        if (db == null) {
            return;
        }
        String currentMonth = DateUtils.currentMonth();
        tvMonth.setText(DateUtils.monthLabel(month));

        boolean atCurrentMonth = month.compareTo(currentMonth) >= 0;
        btnNext.setEnabled(!atCurrentMonth);
        btnNext.setAlpha(atCurrentMonth ? 0.35f : 1f);

        List<Expense> expenses = db.getExpensesForMonth(month);
        double total = db.totalForMonth(month);
        int count = db.countForMonth(month);
        double budget = db.getBudget(month);

        tvTotal.setText(MoneyUtils.format(total));
        tvCount.setText(getString(R.string.history_count, count));

        if (budget > 0) {
            tvBudget.setText(MoneyUtils.format(budget));
            if (total > budget) {
                tvRemaining.setText(getString(R.string.budget_exceeded_by,
                        MoneyUtils.format(total - budget)));
                tvRemaining.setTextColor(ContextCompat.getColor(this, R.color.danger));
            } else {
                tvRemaining.setText(MoneyUtils.format(budget - total));
                tvRemaining.setTextColor(ContextCompat.getColor(this, R.color.success));
            }
        } else {
            tvBudget.setText("—");
            tvRemaining.setText("—");
            tvRemaining.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }

        bindComparison(total, db.totalForMonth(DateUtils.previousMonth(month)));
        bindCategories(db.getCategoryTotalsForMonth(month), total);
        bindExpenses(expenses);
    }

    private void bindComparison(double total, double previousTotal) {
        if (previousTotal <= 0) {
            tvComparison.setText(R.string.history_comparison_none);
            return;
        }
        double difference = total - previousTotal;
        if (Math.abs(difference) < 0.005) {
            tvComparison.setText(R.string.history_comparison_same);
        } else if (difference > 0) {
            tvComparison.setText(getString(R.string.history_comparison_up,
                    MoneyUtils.format(difference)));
        } else {
            tvComparison.setText(getString(R.string.history_comparison_down,
                    MoneyUtils.format(-difference)));
        }
    }

    private void bindCategories(Map<String, Double> categories, double total) {
        layoutCategories.removeAllViews();
        if (categories.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.history_empty);
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            empty.setTextSize(12);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(10), 0, dp(4));
            layoutCategories.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, Double> entry : categories.entrySet()) {
            View row = inflater.inflate(R.layout.item_category_stat, layoutCategories, false);
            View dot = row.findViewById(R.id.view_category_dot);
            TextView name = row.findViewById(R.id.tv_category_name);
            TextView amount = row.findViewById(R.id.tv_category_amount);
            TextView percentLabel = row.findViewById(R.id.tv_category_percent);
            ProgressBar progress = row.findViewById(R.id.progress_category);

            int color = CategoryUtils.color(this, entry.getKey());
            int percent = MoneyUtils.percent(entry.getValue(), total);
            dot.setBackgroundTintList(ColorStateList.valueOf(color));
            name.setText(entry.getKey());
            amount.setText(MoneyUtils.format(entry.getValue()));
            percentLabel.setText(getString(R.string.percent_of_total, percent));
            progress.setProgress(Math.min(100, Math.max(0, percent)));
            progress.setProgressTintList(ColorStateList.valueOf(color));
            layoutCategories.addView(row);
        }
    }

    private void bindExpenses(List<Expense> expenses) {
        layoutExpenses.removeAllViews();
        tvEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Expense expense : expenses) {
            View row = inflater.inflate(R.layout.item_expense_compact, layoutExpenses, false);
            ImageView icon = row.findViewById(R.id.iv_category_icon);
            View iconContainer = row.findViewById(R.id.icon_container);
            TextView title = row.findViewById(R.id.tv_expense_title);
            TextView meta = row.findViewById(R.id.tv_expense_meta);
            TextView amount = row.findViewById(R.id.tv_expense_amount);

            int color = CategoryUtils.color(this, expense.category);
            icon.setImageResource(CategoryUtils.iconRes(expense.category));
            icon.setImageTintList(ColorStateList.valueOf(color));
            iconContainer.setBackgroundTintList(ColorStateList.valueOf(
                    CategoryUtils.softColor(this, expense.category)));
            title.setText(expense.title);
            meta.setText(expense.category + " · " + DateUtils.dateLabel(expense.date));
            amount.setText(MoneyUtils.format(expense.amount));

            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddEditExpenseActivity.class);
                intent.putExtra(AddEditExpenseActivity.EXTRA_EXPENSE_ID, expense.id);
                startActivity(intent);
            });
            layoutExpenses.addView(row);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) {
            render();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_MONTH, month);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (db != null) {
            db.close();
            db = null;
        }
        super.onDestroy();
    }
}
