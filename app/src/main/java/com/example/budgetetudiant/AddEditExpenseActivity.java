package com.example.budgetetudiant;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Date;

/**
 * Formulaire d'ajout et de modification d'une dépense.
 */
public class AddEditExpenseActivity extends BaseActivity {

    public static final String EXTRA_EXPENSE_ID = "expense_id";

    private DBHelper db;
    private int expenseId = -1;
    private String selectedDate;

    private TextInputLayout tilTitle;
    private TextInputLayout tilAmount;
    private TextInputLayout tilCategory;
    private TextInputLayout tilDate;
    private EditText etTitle;
    private EditText etAmount;
    private EditText etDate;
    private MaterialAutoCompleteTextView spCategory;
    private TextView tvPageTitle;
    private MaterialButton btnDelete;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_expense);
        applyInsets(R.id.add_expense_root);

        db = new DBHelper(this);
        expenseId = getIntent().getIntExtra(EXTRA_EXPENSE_ID, -1);

        bindViews();
        bindCategoryDropdown();
        bindDateField();
        applyAmountFilter();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_expense).setOnClickListener(v -> saveExpense());
        btnDelete.setOnClickListener(v -> confirmDelete());

        if (expenseId > 0) {
            loadExpense();
        } else {
            selectedDate = DateUtils.today();
            etDate.setText(DateUtils.dateNumeric(selectedDate));
            spCategory.setText(CategoryUtils.CATEGORIES[0], false);
        }
    }

    private void bindViews() {
        tilTitle = findViewById(R.id.til_title);
        tilAmount = findViewById(R.id.til_amount);
        tilCategory = findViewById(R.id.til_category);
        tilDate = findViewById(R.id.til_date);
        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        etDate = findViewById(R.id.etDate);
        spCategory = findViewById(R.id.spCategory);
        tvPageTitle = findViewById(R.id.tv_page_title);
        btnDelete = findViewById(R.id.btn_delete_expense_form);

        clearErrorOnEdit(etTitle, tilTitle);
        clearErrorOnEdit(etAmount, tilAmount);
        clearErrorOnEdit(etDate, tilDate);
    }

    private void bindCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, CategoryUtils.CATEGORIES);
        spCategory.setAdapter(adapter);
        spCategory.setOnItemClickListener((parent, view, position, id) -> tilCategory.setError(null));
    }

    private void bindDateField() {
        etDate.setOnClickListener(v -> showDatePicker());
        tilDate.setEndIconOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        Date current = DateUtils.parse(selectedDate, "yyyy-MM-dd");
        if (current != null) {
            calendar.setTime(current);
        }

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth);
            selectedDate = DateUtils.format(picked.getTime(), "yyyy-MM-dd");
            etDate.setText(DateUtils.dateNumeric(selectedDate));
            tilDate.setError(null);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void applyAmountFilter() {
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            String result = dest.toString().substring(0, dstart)
                    + source.subSequence(start, end)
                    + dest.toString().substring(dend);
            if (result.isEmpty()) {
                return null;
            }
            if (result.matches("\\d{0,9}([.,]\\d{0,3})?")) {
                return null;
            }
            return "";
        };
        etAmount.setFilters(new InputFilter[]{filter});
    }

    private void loadExpense() {
        Expense expense = db.getExpenseById(expenseId);
        if (expense == null) {
            Toast.makeText(this, R.string.no_expenses, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvPageTitle.setText(R.string.edit_expense);
        btnDelete.setVisibility(View.VISIBLE);
        etTitle.setText(expense.title);
        etAmount.setText(MoneyUtils.formatAmount(expense.amount));
        spCategory.setText(CategoryUtils.normalize(expense.category), false);
        selectedDate = expense.date == null || expense.date.trim().isEmpty()
                ? DateUtils.today() : expense.date;
        etDate.setText(DateUtils.dateNumeric(selectedDate));
    }

    private void saveExpense() {
        tilTitle.setError(null);
        tilAmount.setError(null);
        tilCategory.setError(null);
        tilDate.setError(null);

        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            tilTitle.setError(getString(R.string.error_title_empty));
            etTitle.requestFocus();
            return;
        }

        double amount = MoneyUtils.parseAmount(etAmount.getText().toString());
        if (!MoneyUtils.isValidAmount(amount)) {
            tilAmount.setError(getString(R.string.error_amount_invalid));
            etAmount.requestFocus();
            return;
        }

        String category = spCategory.getText().toString().trim();
        if (category.isEmpty()) {
            tilCategory.setError(getString(R.string.error_category_empty));
            return;
        }
        category = CategoryUtils.normalize(category);

        if (selectedDate == null || selectedDate.trim().isEmpty()) {
            tilDate.setError(getString(R.string.error_date_empty));
            return;
        }

        if (expenseId > 0) {
            Expense expense = new Expense(expenseId, title, amount, category, selectedDate);
            db.updateExpense(expense);
            Toast.makeText(this, R.string.expense_updated, Toast.LENGTH_SHORT).show();
        } else {
            db.addExpense(title, amount, category, selectedDate);
            Toast.makeText(this, R.string.expense_saved, Toast.LENGTH_SHORT).show();
        }

        NotificationHelper.checkBudgetAlerts(this);
        NotificationHelper.checkSavingsGoalAlerts(this);
        setResult(RESULT_OK);
        finish();
    }

    private void confirmDelete() {
        if (expenseId <= 0) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_expense_title)
                .setMessage(R.string.confirm_delete_expense_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    db.deleteExpense(expenseId);
                    Toast.makeText(this, R.string.expense_deleted, Toast.LENGTH_SHORT).show();
                    NotificationHelper.checkBudgetAlerts(this);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private void clearErrorOnEdit(EditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
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
