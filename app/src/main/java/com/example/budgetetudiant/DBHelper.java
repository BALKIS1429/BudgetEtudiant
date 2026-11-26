package com.example.budgetetudiant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "budget.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, amount REAL, category TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS expenses");
        onCreate(db);
    }

    public void add(String title, double amount, String category) {
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("amount", amount);
        v.put("category", category);
        getWritableDatabase().insert("expenses", null, v);
    }

    public List<Expense> getAll() {
        List<Expense> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, title, amount, category FROM expenses ORDER BY id DESC", null);
        while (c.moveToNext()) {
            list.add(new Expense(c.getInt(0), c.getString(1), c.getDouble(2), c.getString(3)));
        }
        c.close();
        return list;
    }

    public void delete(int id) {
        getWritableDatabase().delete("expenses", "id=?", new String[]{String.valueOf(id)});
    }

    public double total() {
        Cursor c = getReadableDatabase().rawQuery("SELECT SUM(amount) FROM expenses", null);
        double t = 0;
        if (c.moveToFirst()) t = c.getDouble(0);
        c.close();
        return t;
    }
}