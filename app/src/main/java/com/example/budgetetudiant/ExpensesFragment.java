package com.example.budgetetudiant;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * Liste complète des dépenses : recherche, filtres par catégorie et par mois,
 * tri, modification et suppression avec confirmation.
 */
public class ExpensesFragment extends Fragment implements ExpenseAdapter.OnExpenseActionListener {

    private DBHelper db;
    private ExpenseAdapter adapter;
    private RecyclerView recyclerView;
    private EditText etSearch;
    private Spinner spCategory;
    private Spinner spMonth;
    private Spinner spSort;
    private TextView tvCount;
    private View layoutEmpty;

    private final List<String> monthKeys = new ArrayList<>();
    private final List<String> monthLabels = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::reloadExpenses;

    private boolean bindingFilters;
    private ArrayAdapter<String> monthAdapter;

    private final String[] sortLabels = {
            "Date récente", "Date ancienne", "Montant décroissant", "Montant croissant"
    };
    private final String[] sortValues = {
            DBHelper.SORT_DATE_DESC, DBHelper.SORT_DATE_ASC,
            DBHelper.SORT_AMOUNT_DESC, DBHelper.SORT_AMOUNT_ASC
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expenses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new DBHelper(requireContext());

        recyclerView = view.findViewById(R.id.rv_expenses);
        etSearch = view.findViewById(R.id.et_search);
        spCategory = view.findViewById(R.id.sp_filter_category);
        spMonth = view.findViewById(R.id.sp_filter_month);
        spSort = view.findViewById(R.id.sp_filter_sort);
        tvCount = view.findViewById(R.id.tv_expense_count);
        layoutEmpty = view.findViewById(R.id.layout_empty_expenses);

        adapter = new ExpenseAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        bindCategorySpinner();
        bindSortSpinner();
        rebuildMonthSpinner();
        bindListeners();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 220);
            }
        });

        reloadExpenses();
    }

    private void bindCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, CategoryUtils.categoriesWithAll());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
    }

    private void bindSortSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, sortLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSort.setAdapter(adapter);
    }

    /**
     * Reconstruit la liste des mois sans perdre le filtre courant. Le mois
     * courant est toujours proposé, même sans dépense.
     */
    private void rebuildMonthSpinner() {
        String selectedKey = getSelectedMonth();
        bindingFilters = true;

        TreeSet<String> months = new TreeSet<>(Collections.reverseOrder());
        months.add(DateUtils.currentMonth());
        months.addAll(db.getMonthsWithExpenses());

        monthKeys.clear();
        monthLabels.clear();
        monthKeys.add(null);
        monthLabels.add(getString(R.string.all_months));
        for (String month : months) {
            monthKeys.add(month);
            monthLabels.add(DateUtils.monthLabel(month));
        }

        if (monthAdapter == null) {
            monthAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, monthLabels);
            monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spMonth.setAdapter(monthAdapter);
        } else {
            monthAdapter.notifyDataSetChanged();
        }

        int selection = 0;
        if (selectedKey != null) {
            int index = monthKeys.indexOf(selectedKey);
            if (index > 0) {
                selection = index;
            }
        }
        spMonth.setSelection(selection);
        bindingFilters = false;
    }

    private void bindListeners() {
        AdapterView.OnItemSelectedListener reloadListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!bindingFilters) {
                    reloadExpenses();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spCategory.setOnItemSelectedListener(reloadListener);
        spMonth.setOnItemSelectedListener(reloadListener);
        spSort.setOnItemSelectedListener(reloadListener);

        requireView().findViewById(R.id.btn_clear_filters).setOnClickListener(v -> {
            etSearch.setText("");
            spCategory.setSelection(0);
            spSort.setSelection(0);
            spMonth.setSelection(0);
            reloadExpenses();
        });
    }

    private String getSelectedMonth() {
        if (spMonth == null || monthKeys.isEmpty()) {
            return null;
        }
        int position = spMonth.getSelectedItemPosition();
        if (position < 0 || position >= monthKeys.size()) {
            return null;
        }
        return monthKeys.get(position);
    }

    private String getSelectedCategory() {
        Object selected = spCategory.getSelectedItem();
        return selected == null ? null : selected.toString();
    }

    private String getSelectedSort() {
        int position = spSort.getSelectedItemPosition();
        if (position < 0 || position >= sortValues.length) {
            return DBHelper.SORT_DATE_DESC;
        }
        return sortValues[position];
    }

    private void reloadExpenses() {
        if (db == null || adapter == null) {
            return;
        }
        String query = etSearch.getText() == null ? null : etSearch.getText().toString();
        List<Expense> expenses = db.searchExpenses(query, getSelectedCategory(),
                getSelectedMonth(), getSelectedSort());
        adapter.submitList(expenses);

        int count = expenses.size();
        tvCount.setText(count == 1
                ? getString(R.string.expense_count, count)
                : getString(R.string.expense_count_plural, count));
        layoutEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (db != null) {
            rebuildMonthSpinner();
            reloadExpenses();
        }
    }

    @Override
    public void onEdit(Expense expense) {
        Intent intent = new Intent(requireContext(), AddEditExpenseActivity.class);
        intent.putExtra(AddEditExpenseActivity.EXTRA_EXPENSE_ID, expense.id);
        startActivity(intent);
    }

    @Override
    public void onDelete(Expense expense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_expense_title)
                .setMessage(R.string.confirm_delete_expense_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    db.deleteExpense(expense.id);
                    Toast.makeText(requireContext(), R.string.expense_deleted,
                            Toast.LENGTH_SHORT).show();
                    rebuildMonthSpinner();
                    reloadExpenses();
                    NotificationHelper.checkBudgetAlerts(requireContext());
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        searchHandler.removeCallbacks(searchRunnable);
        if (db != null) {
            db.close();
            db = null;
        }
        recyclerView = null;
        adapter = null;
        super.onDestroyView();
    }
}
