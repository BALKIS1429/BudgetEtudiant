package com.example.budgetetudiant;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestionnaire SQLite de Budget Étudiant.
 *
 * IMPORTANT : les migrations sont non destructives. Aucune table contenant les
 * dépenses de l'utilisateur n'est supprimée. Les colonnes et tables manquantes
 * sont ajoutées avec ALTER TABLE / CREATE TABLE IF NOT EXISTS.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "budget.db";
    public static final int DATABASE_VERSION = 2;

    public static final String TABLE_EXPENSES = "expenses";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_CREATED_AT = "created_at";

    public static final String TABLE_BUDGETS = "budgets";
    public static final String COLUMN_BUDGET_MONTH = "month";
    public static final String COLUMN_BUDGET_AMOUNT = "amount";
    public static final String COLUMN_UPDATED_AT = "updated_at";

    public static final String TABLE_SAVINGS = "savings_goal";
    public static final String COLUMN_SAVINGS_ID = "id";
    public static final String COLUMN_SAVINGS_NAME = "name";
    public static final String COLUMN_SAVINGS_TARGET = "target_amount";
    public static final String COLUMN_SAVINGS_SAVED = "saved_amount";
    public static final String COLUMN_SAVINGS_CREATED_AT = "created_at";
    public static final String COLUMN_SAVINGS_UPDATED_AT = "updated_at";

    public static final String SORT_DATE_DESC = "date_desc";
    public static final String SORT_DATE_ASC = "date_asc";
    public static final String SORT_AMOUNT_DESC = "amount_desc";
    public static final String SORT_AMOUNT_ASC = "amount_asc";

    private static final String ALL_MONTHS = "Tous les mois";

    private final Context appContext;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        ensureSchema(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migration compatible : on ajoute uniquement ce qui manque.
        ensureSchema(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Ne jamais supprimer les données de l'utilisateur en cas de downgrade.
        ensureSchema(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        ensureSchema(db);
        migrateLegacyBudget(db);
    }

    /**
     * Crée ou complète le schéma. Cette méthode est idempotente et peut être
     * appelée à chaque ouverture de la base.
     */
    private void ensureSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXPENSES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_CREATED_AT + " TEXT)");

        // Ancienne base v1 : la table expenses n'avait pas de date.
        addColumnIfMissing(db, TABLE_EXPENSES, COLUMN_DATE, "TEXT");
        addColumnIfMissing(db, TABLE_EXPENSES, COLUMN_CREATED_AT, "TEXT");

        // Les anciennes dépenses n'ont pas de date connue : on les rattache au
        // mois courant pour qu'elles restent visibles et ne soient jamais perdues.
        db.execSQL("UPDATE " + TABLE_EXPENSES + " SET " + COLUMN_DATE + " = ? " +
                        "WHERE " + COLUMN_DATE + " IS NULL OR TRIM(" + COLUMN_DATE + ") = ''",
                new Object[]{DateUtils.today()});
        db.execSQL("UPDATE " + TABLE_EXPENSES + " SET " + COLUMN_CREATED_AT + " = ? " +
                        "WHERE " + COLUMN_CREATED_AT + " IS NULL OR TRIM(" + COLUMN_CREATED_AT + ") = ''",
                new Object[]{DateUtils.nowTimestamp()});

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BUDGETS + " (" +
                COLUMN_BUDGET_MONTH + " TEXT PRIMARY KEY, " +
                COLUMN_BUDGET_AMOUNT + " REAL NOT NULL, " +
                COLUMN_UPDATED_AT + " TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SAVINGS + " (" +
                COLUMN_SAVINGS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SAVINGS_NAME + " TEXT, " +
                COLUMN_SAVINGS_TARGET + " REAL NOT NULL, " +
                COLUMN_SAVINGS_SAVED + " REAL NOT NULL DEFAULT 0, " +
                COLUMN_SAVINGS_CREATED_AT + " TEXT, " +
                COLUMN_SAVINGS_UPDATED_AT + " TEXT)");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_date ON " +
                TABLE_EXPENSES + "(" + COLUMN_DATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_category ON " +
                TABLE_EXPENSES + "(" + COLUMN_CATEGORY + ")");
    }

    private void addColumnIfMissing(SQLiteDatabase db, String table, String column, String type) {
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && column.equalsIgnoreCase(cursor.getString(nameIndex))) {
                    exists = true;
                    break;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (!exists) {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }

    /**
     * L'ancienne version stockait un budget unique dans SharedPreferences.
     * On le transfère une seule fois vers la table budgets, pour le mois courant.
     */
    private void migrateLegacyBudget(SQLiteDatabase db) {
        SharedPreferences migration = AppPreferences.getMigration(appContext);
        if (migration.getBoolean("legacy_budget_migrated", false)) {
            return;
        }

        float legacyBudget = AppPreferences.getLegacyBudget(appContext);
        if (legacyBudget > 0) {
            String month = DateUtils.currentMonth();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_BUDGETS +
                        " WHERE " + COLUMN_BUDGET_MONTH + " = ?", new String[]{month});
                boolean alreadyExists = cursor.moveToFirst() && cursor.getInt(0) > 0;
                if (!alreadyExists) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_BUDGET_MONTH, month);
                    values.put(COLUMN_BUDGET_AMOUNT, legacyBudget);
                    values.put(COLUMN_UPDATED_AT, DateUtils.nowTimestamp());
                    db.insert(TABLE_BUDGETS, null, values);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        migration.edit().putBoolean("legacy_budget_migrated", true).apply();
    }

    // ---------------------------------------------------------------------
    // DÉPENSES
    // ---------------------------------------------------------------------

    public long addExpense(String title, double amount, String category, String date) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title == null ? "" : title.trim());
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_CATEGORY, CategoryUtils.normalize(category));
        values.put(COLUMN_DATE, date == null || date.trim().isEmpty() ? DateUtils.today() : date);
        values.put(COLUMN_CREATED_AT, DateUtils.nowTimestamp());
        return getWritableDatabase().insert(TABLE_EXPENSES, null, values);
    }

    /** Compatibilité avec l'ancienne interface. */
    public void add(String title, double amount, String category) {
        addExpense(title, amount, category, DateUtils.today());
    }

    public int updateExpense(Expense expense) {
        if (expense == null) {
            return 0;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, expense.title == null ? "" : expense.title.trim());
        values.put(COLUMN_AMOUNT, expense.amount);
        values.put(COLUMN_CATEGORY, CategoryUtils.normalize(expense.category));
        values.put(COLUMN_DATE, expense.date == null || expense.date.trim().isEmpty()
                ? DateUtils.today() : expense.date);
        return getWritableDatabase().update(TABLE_EXPENSES, values,
                COLUMN_ID + " = ?", new String[]{String.valueOf(expense.id)});
    }

    public int deleteExpense(int id) {
        return getWritableDatabase().delete(TABLE_EXPENSES,
                COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /** Compatibilité avec l'ancienne interface. */
    public void delete(int id) {
        deleteExpense(id);
    }

    public Expense getExpenseById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_EXPENSES, null, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)}, null, null, null);
            if (cursor.moveToFirst()) {
                return readExpense(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<Expense> getAll() {
        return searchExpenses(null, null, null, SORT_DATE_DESC);
    }

    public List<Expense> getExpensesForMonth(String month) {
        return searchExpenses(null, null, month, SORT_DATE_DESC);
    }

    public List<Expense> getRecentExpenses(int limit) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_EXPENSES +
                            " ORDER BY " + COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1, limit))});
            while (cursor.moveToNext()) {
                list.add(readExpense(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    /**
     * Recherche et filtres. Les valeurs de tri sont limitées à une liste blanche
     * afin de ne jamais concaténer une valeur utilisateur dans le SQL.
     */
    public List<Expense> searchExpenses(String query, String category, String month, String sort) {
        List<Expense> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_EXPENSES + " WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND LOWER(").append(COLUMN_TITLE).append(") LIKE LOWER(?)");
            args.add("%" + query.trim() + "%");
        }
        if (category != null && !category.trim().isEmpty()
                && !CategoryUtils.ALL.equalsIgnoreCase(category.trim())) {
            sql.append(" AND ").append(COLUMN_CATEGORY).append(" = ?");
            args.add(CategoryUtils.normalize(category));
        }
        if (month != null && month.matches("\\d{4}-\\d{2}")) {
            sql.append(" AND substr(").append(COLUMN_DATE).append(", 1, 7) = ?");
            args.add(month);
        }
        sql.append(" ORDER BY ").append(orderBy(sort));

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
            while (cursor.moveToNext()) {
                list.add(readExpense(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    private String orderBy(String sort) {
        if (sort == null) {
            sort = SORT_DATE_DESC;
        }
        switch (sort) {
            case SORT_DATE_ASC:
                return COLUMN_DATE + " ASC, " + COLUMN_ID + " ASC";
            case SORT_AMOUNT_DESC:
                return COLUMN_AMOUNT + " DESC, " + COLUMN_DATE + " DESC";
            case SORT_AMOUNT_ASC:
                return COLUMN_AMOUNT + " ASC, " + COLUMN_DATE + " DESC";
            case SORT_DATE_DESC:
            default:
                return COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC";
        }
    }

    private Expense readExpense(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
        double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));
        String category = CategoryUtils.normalize(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
        String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
        return new Expense(id, title, amount, category, date);
    }

    public double total() {
        return totalAll();
    }

    public double totalAll() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COALESCE(SUM(" + COLUMN_AMOUNT + "), 0) FROM " + TABLE_EXPENSES, null);
            return cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public double totalForMonth(String month) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COALESCE(SUM(" + COLUMN_AMOUNT + "), 0) FROM " +
                            TABLE_EXPENSES + " WHERE substr(" + COLUMN_DATE + ", 1, 7) = ?",
                    new String[]{month});
            return cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public int countAll() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSES, null);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public int countForMonth(String month) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSES +
                            " WHERE substr(" + COLUMN_DATE + ", 1, 7) = ?",
                    new String[]{month});
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ---------------------------------------------------------------------
    // STATISTIQUES
    // ---------------------------------------------------------------------

    public Map<String, Double> getCategoryTotalsAll() {
        return categoryTotals(null);
    }

    public Map<String, Double> getCategoryTotalsForMonth(String month) {
        return categoryTotals(month);
    }

    private Map<String, Double> categoryTotals(String month) {
        Map<String, Double> merged = new HashMap<>();
        StringBuilder sql = new StringBuilder("SELECT " + COLUMN_CATEGORY + ", SUM(" +
                COLUMN_AMOUNT + ") FROM " + TABLE_EXPENSES);
        List<String> args = new ArrayList<>();
        if (month != null && month.matches("\\d{4}-\\d{2}")) {
            sql.append(" WHERE substr(").append(COLUMN_DATE).append(", 1, 7) = ?");
            args.add(month);
        }
        sql.append(" GROUP BY ").append(COLUMN_CATEGORY);

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
            while (cursor.moveToNext()) {
                String category = CategoryUtils.normalize(cursor.getString(0));
                double amount = cursor.getDouble(1);
                merged.put(category, merged.getOrDefault(category, 0.0) + amount);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(merged.entrySet());
        entries.sort((left, right) -> Double.compare(right.getValue(), left.getValue()));
        Map<String, Double> ordered = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : entries) {
            ordered.put(entry.getKey(), entry.getValue());
        }
        return ordered;
    }

    public List<String> getMonthsWithExpenses() {
        List<String> months = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT DISTINCT substr(" + COLUMN_DATE + ", 1, 7) AS m FROM " +
                    TABLE_EXPENSES + " WHERE " + COLUMN_DATE + " IS NOT NULL AND " +
                    COLUMN_DATE + " <> '' ORDER BY m DESC", null);
            while (cursor.moveToNext()) {
                String month = cursor.getString(0);
                if (month != null && !month.isEmpty()) {
                    months.add(month);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return months;
    }

    /** Mois du plus ancien au plus récent, avec 0 pour les mois sans dépense. */
    public Map<String, Double> getMonthlyTotals(int numberOfMonths) {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (String month : DateUtils.lastMonthKeys(numberOfMonths)) {
            totals.put(month, totalForMonth(month));
        }
        return totals;
    }

    public double getAverageMonthlyExpense() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT AVG(month_total) FROM (" +
                    "SELECT SUM(" + COLUMN_AMOUNT + ") AS month_total FROM " + TABLE_EXPENSES +
                    " GROUP BY substr(" + COLUMN_DATE + ", 1, 7))", null);
            return cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ---------------------------------------------------------------------
    // BUDGET MENSUEL
    // ---------------------------------------------------------------------

    public double getBudget(String month) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BUDGETS, new String[]{COLUMN_BUDGET_AMOUNT},
                    COLUMN_BUDGET_MONTH + " = ?", new String[]{month}, null, null, null);
            return cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void setBudget(String month, double amount) {
        SQLiteDatabase db = getWritableDatabase();
        if (amount <= 0) {
            db.delete(TABLE_BUDGETS, COLUMN_BUDGET_MONTH + " = ?", new String[]{month});
            return;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_BUDGET_MONTH, month);
        values.put(COLUMN_BUDGET_AMOUNT, amount);
        values.put(COLUMN_UPDATED_AT, DateUtils.nowTimestamp());
        db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ---------------------------------------------------------------------
    // OBJECTIF D'ÉPARGNE
    // ---------------------------------------------------------------------

    public SavingsGoal getSavingsGoal() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SAVINGS, null, null, null, null, null,
                    COLUMN_SAVINGS_ID + " ASC", "1");
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SAVINGS_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVINGS_NAME));
                double target = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SAVINGS_TARGET));
                double saved = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SAVINGS_SAVED));
                return new SavingsGoal(id, name, target, saved);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long createSavingsGoal(String name, double targetAmount) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_SAVINGS_NAME, name == null || name.trim().isEmpty()
                ? "Mon objectif" : name.trim());
        values.put(COLUMN_SAVINGS_TARGET, targetAmount);
        values.put(COLUMN_SAVINGS_SAVED, 0);
        values.put(COLUMN_SAVINGS_CREATED_AT, DateUtils.nowTimestamp());
        values.put(COLUMN_SAVINGS_UPDATED_AT, DateUtils.nowTimestamp());
        return getWritableDatabase().insert(TABLE_SAVINGS, null, values);
    }

    public int updateSavingsGoal(int id, String name, double targetAmount, double savedAmount) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_SAVINGS_NAME, name == null || name.trim().isEmpty()
                ? "Mon objectif" : name.trim());
        values.put(COLUMN_SAVINGS_TARGET, targetAmount);
        values.put(COLUMN_SAVINGS_SAVED, Math.max(0, savedAmount));
        values.put(COLUMN_SAVINGS_UPDATED_AT, DateUtils.nowTimestamp());
        return getWritableDatabase().update(TABLE_SAVINGS, values,
                COLUMN_SAVINGS_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void addToSavingsGoal(int id, double amount) {
        getWritableDatabase().execSQL("UPDATE " + TABLE_SAVINGS + " SET " +
                        COLUMN_SAVINGS_SAVED + " = " + COLUMN_SAVINGS_SAVED + " + ?, " +
                        COLUMN_SAVINGS_UPDATED_AT + " = ? WHERE " + COLUMN_SAVINGS_ID + " = ?",
                new Object[]{amount, DateUtils.nowTimestamp(), id});
    }

    public void resetSavingsGoal(int id) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_SAVINGS_SAVED, 0);
        values.put(COLUMN_SAVINGS_UPDATED_AT, DateUtils.nowTimestamp());
        getWritableDatabase().update(TABLE_SAVINGS, values,
                COLUMN_SAVINGS_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int deleteSavingsGoal(int id) {
        return getWritableDatabase().delete(TABLE_SAVINGS,
                COLUMN_SAVINGS_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
