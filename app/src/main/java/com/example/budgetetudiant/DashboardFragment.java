package com.example.budgetetudiant;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/**
 * Tableau de bord : résumé du mois, budget, accès rapides et dernières dépenses.
 */
public class DashboardFragment extends Fragment {

    private DBHelper db;

    private TextView tvGreeting;
    private TextView tvDate;
    private TextView tvAvatarInitial;
    private TextView tvMonthSpent;
    private TextView tvHeroBudget;
    private TextView tvHeroRemaining;
    private TextView tvMonthCount;
    private TextView tvBudgetPercent;
    private TextView tvBudgetAmount;
    private TextView tvBudgetStatus;
    private ProgressBar pbBudget;
    private LinearLayout layoutRecent;
    private View layoutRecentEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new DBHelper(requireContext());

        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvDate = view.findViewById(R.id.tv_date);
        tvAvatarInitial = view.findViewById(R.id.tv_avatar_initial);
        tvMonthSpent = view.findViewById(R.id.tv_month_spent);
        tvHeroBudget = view.findViewById(R.id.tv_hero_budget);
        tvHeroRemaining = view.findViewById(R.id.tv_hero_remaining);
        tvMonthCount = view.findViewById(R.id.tv_month_count);
        tvBudgetPercent = view.findViewById(R.id.tv_budget_percent);
        tvBudgetAmount = view.findViewById(R.id.tv_budget_amount);
        tvBudgetStatus = view.findViewById(R.id.tv_budget_status);
        pbBudget = view.findViewById(R.id.pb_budget);
        layoutRecent = view.findViewById(R.id.layout_recent_expenses);
        layoutRecentEmpty = view.findViewById(R.id.layout_recent_empty);

        view.findViewById(R.id.btn_manage_budget).setOnClickListener(v -> showBudgetDialog());
        view.findViewById(R.id.btn_edit_budget).setOnClickListener(v -> showBudgetDialog());
        view.findViewById(R.id.btn_see_all).setOnClickListener(v -> openTab(R.id.nav_expenses));

        view.findViewById(R.id.card_quick_add).setOnClickListener(v -> openAddExpense());
        view.findViewById(R.id.card_quick_savings).setOnClickListener(v -> openTab(R.id.nav_savings));
        view.findViewById(R.id.card_quick_stats).setOnClickListener(v -> openTab(R.id.nav_stats));
        view.findViewById(R.id.card_quick_converter).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ConverterActivity.class)));
        view.findViewById(R.id.card_quick_history).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HistoryActivity.class)));
        view.findViewById(R.id.card_quick_pdf).setOnClickListener(v ->
                PdfExporter.exportAndShare(requireActivity(), DateUtils.currentMonth()));

        loadDashboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (db != null) {
            loadDashboard();
        }
    }

    private void openTab(int tabId) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).selectTab(tabId);
        }
    }

    private void openAddExpense() {
        startActivity(new Intent(requireContext(), AddEditExpenseActivity.class));
    }

    private void loadDashboard() {
        String name = AppPreferences.getUserName(requireContext());
        if (name.isEmpty()) {
            tvGreeting.setText(getString(R.string.hello) + " 👋");
            tvAvatarInitial.setText("B");
        } else {
            tvGreeting.setText(getString(R.string.hello) + ", " + name + " 👋");
            tvAvatarInitial.setText(name.substring(0, 1).toUpperCase());
        }
        tvDate.setText(DateUtils.todayLongLabel());

        String month = DateUtils.currentMonth();
        double total = db.totalForMonth(month);
        int count = db.countForMonth(month);
        double budget = db.getBudget(month);

        tvMonthSpent.setText(MoneyUtils.format(total));
        tvMonthCount.setText(count + (count == 1 ? " dépense ce mois" : " dépenses ce mois"));
        tvHeroBudget.setText(budget > 0 ? MoneyUtils.format(budget) : "—");
        tvHeroRemaining.setText(budget > 0
                ? MoneyUtils.format(Math.max(0, budget - total)) : "—");

        if (budget <= 0) {
            pbBudget.setProgress(0);
            pbBudget.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.primary)));
            tvBudgetPercent.setText("—");
            tvBudgetPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            tvBudgetAmount.setText(R.string.no_budget);
            tvBudgetStatus.setText(R.string.define_budget_hint);
            tvBudgetStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            int percent = MoneyUtils.percent(total, budget);
            pbBudget.setProgress(Math.min(100, percent));
            tvBudgetPercent.setText(percent + " %");

            int statusColor;
            int progressColor;
            String status;
            if (total > budget) {
                statusColor = R.color.danger;
                progressColor = R.color.danger;
                status = getString(R.string.budget_exceeded_by,
                        MoneyUtils.format(total - budget));
            } else if (percent >= 80) {
                statusColor = R.color.warning;
                progressColor = R.color.warning;
                status = getString(R.string.budget_remaining,
                        MoneyUtils.format(budget - total), MoneyUtils.format(budget));
            } else {
                statusColor = R.color.text_secondary;
                progressColor = R.color.primary;
                status = getString(R.string.budget_remaining,
                        MoneyUtils.format(budget - total), MoneyUtils.format(budget));
            }
            tvBudgetPercent.setTextColor(ContextCompat.getColor(requireContext(), statusColor));
            tvBudgetAmount.setText("Budget : " + MoneyUtils.format(budget));
            tvBudgetStatus.setText(status);
            tvBudgetStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor));
            pbBudget.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), progressColor)));
        }

        layoutRecent.removeAllViews();
        List<Expense> recent = db.getRecentExpenses(5);
        if (recent.isEmpty()) {
            layoutRecentEmpty.setVisibility(View.VISIBLE);
        } else {
            layoutRecentEmpty.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (Expense expense : recent) {
                View row = inflater.inflate(R.layout.item_expense_compact, layoutRecent, false);
                bindRecentRow(row, expense);
                layoutRecent.addView(row);
            }
        }
    }

    private void bindRecentRow(View row, Expense expense) {
        ImageView icon = row.findViewById(R.id.iv_category_icon);
        View iconContainer = row.findViewById(R.id.icon_container);
        TextView title = row.findViewById(R.id.tv_expense_title);
        TextView meta = row.findViewById(R.id.tv_expense_meta);
        TextView amount = row.findViewById(R.id.tv_expense_amount);

        int color = CategoryUtils.color(requireContext(), expense.category);
        icon.setImageResource(CategoryUtils.iconRes(expense.category));
        icon.setImageTintList(ColorStateList.valueOf(color));
        iconContainer.setBackgroundTintList(
                ColorStateList.valueOf(CategoryUtils.softColor(requireContext(), expense.category)));
        title.setText(expense.title);
        meta.setText(expense.category + " · " + DateUtils.dateLabel(expense.date));
        amount.setText(MoneyUtils.format(expense.amount));

        row.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditExpenseActivity.class);
            intent.putExtra(AddEditExpenseActivity.EXTRA_EXPENSE_ID, expense.id);
            startActivity(intent);
        });
    }

    private void showBudgetDialog() {
        String month = DateUtils.currentMonth();
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget, null);
        TextView monthLabel = view.findViewById(R.id.tv_budget_dialog_month);
        TextInputLayout tilAmount = view.findViewById(R.id.til_budget_amount);
        EditText etAmount = view.findViewById(R.id.etBudget);

        monthLabel.setText(DateUtils.monthLabel(month));
        double currentBudget = db.getBudget(month);
        if (currentBudget > 0) {
            etAmount.setText(MoneyUtils.formatAmount(currentBudget));
            etAmount.setSelection(etAmount.getText().length());
        }
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilAmount.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.budget_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(
                        androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    double amount = MoneyUtils.parseAmount(etAmount.getText().toString());
                    if (!MoneyUtils.isValidAmount(amount)) {
                        tilAmount.setError(getString(R.string.error_budget_invalid));
                        return;
                    }
                    db.setBudget(month, amount);
                    NotificationHelper.resetBudgetFlags(requireContext(), month);
                    NotificationHelper.checkBudgetAlerts(requireContext());
                    dialog.dismiss();
                    loadDashboard();
                    Toast.makeText(requireContext(), R.string.budget_saved, Toast.LENGTH_SHORT).show();
                }));
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        if (db != null) {
            db.close();
            db = null;
        }
        super.onDestroyView();
    }
}
