package com.example.budgetetudiant;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Génère un rapport PDF mensuel avec les dépenses, les catégories, le budget
 * et l'objectif d'épargne, puis permet de le partager.
 */
public final class PdfExporter {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final float MARGIN = 40f;

    private PdfExporter() {
    }

    public static void exportAndShare(final Activity activity, final String month) {
        Toast.makeText(activity, R.string.pdf_generating, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                File file = exportMonth(activity.getApplicationContext(), month);
                activity.runOnUiThread(() -> share(activity, file));
            } catch (Exception e) {
                activity.runOnUiThread(() -> Toast.makeText(activity,
                        R.string.pdf_error, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public static File exportMonth(Context context, String month) throws IOException {
        DBHelper db = new DBHelper(context);
        List<Expense> expenses;
        Map<String, Double> categories;
        double total;
        double budget;
        SavingsGoal goal;
        try {
            expenses = db.getExpensesForMonth(month);
            categories = db.getCategoryTotalsForMonth(month);
            total = db.totalForMonth(month);
            budget = db.getBudget(month);
            goal = db.getSavingsGoal();
        } finally {
            db.close();
        }

        File directory = new File(context.getCacheDir(), "reports");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Impossible de créer le dossier des rapports");
        }
        File file = new File(directory, "Rapport_Budget_" + month + ".pdf");

        PdfDocument document = new PdfDocument();
        try {
            Writer writer = new Writer(document, context);
            writer.startPage(true);
            writer.drawReportHeader(month);
            writer.drawSummary(total, budget, goal);
            writer.drawCategories(categories, total);
            writer.drawExpenses(expenses);
            writer.finish();

            try (FileOutputStream output = new FileOutputStream(file)) {
                document.writeTo(output);
                output.flush();
            }
        } finally {
            document.close();
        }
        return file;
    }

    public static void share(Context context, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Rapport Budget Étudiant");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent,
                    context.getString(R.string.share_pdf)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.pdf_error, Toast.LENGTH_LONG).show();
        }
    }

    private static final class Writer {

        private final PdfDocument document;
        private final Context context;
        private final int colorPrimary;
        private final int colorPrimaryDark;
        private final int colorText;
        private final int colorMuted;
        private final int colorLine;
        private final int colorSoft;
        private final int colorSuccess;

        private PdfDocument.Page page;
        private Canvas canvas;
        private int pageNumber;
        private float y;

        Writer(PdfDocument document, Context context) {
            this.document = document;
            this.context = context;
            this.colorPrimary = ContextCompat.getColor(context, R.color.primary);
            this.colorPrimaryDark = ContextCompat.getColor(context, R.color.primary_dark);
            this.colorText = ContextCompat.getColor(context, R.color.text_primary);
            this.colorMuted = ContextCompat.getColor(context, R.color.text_secondary);
            this.colorLine = ContextCompat.getColor(context, R.color.card_stroke);
            this.colorSoft = ContextCompat.getColor(context, R.color.primary_soft);
            this.colorSuccess = ContextCompat.getColor(context, R.color.success);
        }

        void startPage(boolean first) {
            pageNumber++;
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
            page = document.startPage(info);
            canvas = page.getCanvas();
            y = MARGIN;
            if (first) {
                Paint header = paint(colorPrimary);
                canvas.drawRect(0, 0, PAGE_WIDTH, 112, header);
                text("Budget Étudiant", MARGIN, 48, 24, Color.WHITE, true);
                text("Rapport mensuel", MARGIN, 76, 12, Color.WHITE, false);
                y = 140;
            } else {
                text("Budget Étudiant — Rapport mensuel", MARGIN, 38, 10, colorMuted, false);
                line(MARGIN, 50, PAGE_WIDTH - MARGIN, 50, colorLine);
                y = 72;
            }
        }

        void drawReportHeader(String month) {
            text(DateUtils.monthLabel(month), MARGIN, y, 20, colorPrimaryDark, true);
            y += 24;
            text("Généré le " + DateUtils.dateNumeric(DateUtils.today()), MARGIN, y, 10, colorMuted, false);
            y += 30;
        }

        void drawSummary(double total, double budget, SavingsGoal goal) {
            section("Résumé du mois");
            Paint box = paint(colorSoft);
            float boxHeight = goal == null ? 104 : 132;
            ensureSpace(boxHeight + 10);
            canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxHeight, 16, 16, box);
            float innerX = MARGIN + 18;
            float innerY = y + 24;
            text("Total des dépenses", innerX, innerY, 10, colorMuted, false);
            text(MoneyUtils.format(total), innerX, innerY + 24, 20, colorPrimaryDark, true);
            innerY += 52;
            text("Budget mensuel", innerX, innerY, 10, colorMuted, false);
            text(budget > 0 ? MoneyUtils.format(budget) : "Non défini",
                    innerX + 130, innerY, 11, colorText, true);
            text("Restant", innerX + 300, innerY, 10, colorMuted, false);
            String remaining = budget > 0
                    ? MoneyUtils.format(Math.max(0, budget - total)) : "—";
            text(remaining, innerX + 370, innerY, 11, colorText, true);
            if (goal != null) {
                innerY += 26;
                text("Objectif d’épargne", innerX, innerY, 10, colorMuted, false);
                text(goal.name == null ? "Objectif" : goal.name, innerX + 130, innerY, 11, colorText, true);
                text("Économisé : " + MoneyUtils.format(goal.savedAmount) +
                                " / " + MoneyUtils.format(goal.targetAmount),
                        innerX + 270, innerY, 10, colorText, false);
            }
            y += boxHeight + 20;
        }

        void drawCategories(Map<String, Double> categories, double total) {
            section("Dépenses par catégorie");
            if (categories.isEmpty()) {
                text("Aucune dépense enregistrée pour ce mois.", MARGIN, y, 11, colorMuted, false);
                y += 24;
                return;
            }
            for (Map.Entry<String, Double> entry : categories.entrySet()) {
                ensureSpace(24);
                double percent = total > 0 ? (entry.getValue() / total) * 100 : 0;
                text(entry.getKey(), MARGIN, y, 11, colorText, false);
                String value = MoneyUtils.format(entry.getValue()) +
                        "  (" + Math.round(percent) + " %)";
                textRight(value, PAGE_WIDTH - MARGIN, y, 11, colorPrimaryDark, true);
                y += 22;
            }
            y += 10;
        }

        void drawExpenses(List<Expense> expenses) {
            section("Détail des dépenses");
            if (expenses.isEmpty()) {
                text("Aucune dépense pour ce mois.", MARGIN, y, 11, colorMuted, false);
                y += 24;
                return;
            }
            tableHeader();
            int index = 0;
            for (Expense expense : expenses) {
                ensureSpace(24);
                if (index % 2 == 0) {
                    Paint rowPaint = paint(colorSoft);
                    canvas.drawRect(MARGIN, y - 11, PAGE_WIDTH - MARGIN, y + 8, rowPaint);
                }
                text(DateUtils.dateNumeric(expense.date), MARGIN + 4, y, 9, colorMuted, false);
                text(truncate(expense.title, 180), MARGIN + 75, y, 9, colorText, false);
                text(truncate(expense.category, 85), MARGIN + 285, y, 9, colorMuted, false);
                textRight(MoneyUtils.formatAmount(expense.amount) + " TND",
                        PAGE_WIDTH - MARGIN - 4, y, 9, colorPrimaryDark, true);
                y += 22;
                index++;
            }
            y += 12;
        }

        private void tableHeader() {
            ensureSpace(26);
            Paint header = paint(colorPrimary);
            canvas.drawRoundRect(MARGIN, y - 13, PAGE_WIDTH - MARGIN, y + 9, 6, 6, header);
            text("Date", MARGIN + 4, y, 9, Color.WHITE, true);
            text("Titre", MARGIN + 75, y, 9, Color.WHITE, true);
            text("Catégorie", MARGIN + 285, y, 9, Color.WHITE, true);
            textRight("Montant", PAGE_WIDTH - MARGIN - 4, y, 9, Color.WHITE, true);
            y += 24;
        }

        void finish() {
            if (page != null) {
                Paint footer = paint(colorMuted);
                footer.setTextSize(8);
                footer.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Budget Étudiant · page " + pageNumber,
                        PAGE_WIDTH / 2f, PAGE_HEIGHT - 22, footer);
                document.finishPage(page);
                page = null;
            }
        }

        private void section(String title) {
            ensureSpace(44);
            text(title, MARGIN, y, 14, colorPrimaryDark, true);
            y += 8;
            line(MARGIN, y, PAGE_WIDTH - MARGIN, y, colorLine);
            y += 24;
        }

        private void ensureSpace(float needed) {
            if (y + needed > PAGE_HEIGHT - MARGIN - 20) {
                finishPage();
                startPage(false);
            }
        }

        private void finishPage() {
            if (page != null) {
                Paint footer = paint(colorMuted);
                footer.setTextSize(8);
                footer.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Budget Étudiant · page " + pageNumber,
                        PAGE_WIDTH / 2f, PAGE_HEIGHT - 22, footer);
                document.finishPage(page);
                page = null;
            }
        }

        private void text(String value, float x, float baseline, float size,
                          int color, boolean bold) {
            Paint paint = paint(color);
            paint.setTextSize(size);
            paint.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(value == null ? "" : value, x, baseline, paint);
        }

        private void textRight(String value, float right, float baseline, float size,
                               int color, boolean bold) {
            Paint paint = paint(color);
            paint.setTextSize(size);
            paint.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(value == null ? "" : value, right, baseline, paint);
        }

        private void line(float startX, float startY, float endX, float endY, int color) {
            Paint paint = paint(color);
            paint.setStrokeWidth(0.8f);
            canvas.drawLine(startX, startY, endX, endY, paint);
        }

        private Paint paint(int color) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
            return paint;
        }

        private String truncate(String value, float maxWidth) {
            if (value == null) {
                return "";
            }
            Paint paint = paint(colorText);
            paint.setTextSize(9);
            if (paint.measureText(value) <= maxWidth) {
                return value;
            }
            String ellipsis = "…";
            int end = value.length();
            while (end > 1 && paint.measureText(value.substring(0, end) + ellipsis) > maxWidth) {
                end--;
            }
            return value.substring(0, end) + ellipsis;
        }
    }
}
