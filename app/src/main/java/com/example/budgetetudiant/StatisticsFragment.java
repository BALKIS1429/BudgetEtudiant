package com.example.budgetetudiant;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Map;

/**
 * Statistiques calculées à partir des données SQLite : répartition par
 * catégorie, évolution mensuelle, moyenne et comparaison au mois précédent.
 */
public class StatisticsFragment extends Fragment {

    private DBHelper db;

    private TextView tvTotal;
    private TextView tvMonth;
    private TextView tvAverage;
    private TextView tvTopCategory;
    private TextView tvComparisonTitle;
    private TextView tvComparisonDetail;
    private TextView tvAverageMonth;
    private ImageView ivComparisonIcon;
    private CategoryChartView categoryChart;
    private MonthlyChartView monthlyChart;
    private LinearLayout layoutBreakdown;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new DBHelper(requireContext());

        tvTotal = view.findViewById(R.id.tv_stats_total);
        tvMonth = view.findViewById(R.id.tv_stats_month);
        tvAverage = view.findViewById(R.id.tv_stats_average);
        tvTopCategory = view.findViewById(R.id.tv_stats_top_category);
        tvComparisonTitle = view.findViewById(R.id.tv_comparison_title);
        tvComparisonDetail = view.findViewById(R.id.tv_comparison_detail);
        tvAverageMonth = view.findViewById(R.id.tv_average_month);
        ivComparisonIcon = view.findViewById(R.id.iv_comparison_icon);
        categoryChart = view.findViewById(R.id.category_chart);
        monthlyChart = view.findViewById(R.id.monthly_chart);
        layoutBreakdown = view.findViewById(R.id.layout_category_breakdown);

        loadStatistics();
    }

    private void loadStatistics() {
        if (db == null) {
            return;
        }

        String currentMonth = DateUtils.currentMonth();
        String previousMonth = DateUtils.previousMonth(currentMonth);

        double totalAll = db.totalAll();
        int countAll = db.countAll();
        double totalMonth = db.totalForMonth(currentMonth);
        double totalPrevious = db.totalForMonth(previousMonth);

        tvTotal.setText(MoneyUtils.format(totalAll));
        tvMonth.setText(MoneyUtils.format(totalMonth));
        tvAverage.setText(MoneyUtils.format(countAll > 0 ? totalAll / countAll : 0));

        Map<String, Double> categories = db.getCategoryTotalsAll();
        String topCategory = "—";
        double topAmount = 0;
        for (Map.Entry<String, Double> entry : categories.entrySet()) {
            topCategory = entry.getKey();
            topAmount = entry.getValue();
            break;
        }
        if (topAmount > 0) {
            tvTopCategory.setText(topCategory + "\n" + MoneyUtils.format(topAmount));
        } else {
            tvTopCategory.setText("—");
        }

        bindCategoryChart(categories, totalAll);
        bindMonthlyChart();
        bindComparison(currentMonth, previousMonth, totalMonth, totalPrevious);

        double averageMonth = db.getAverageMonthlyExpense();
        tvAverageMonth.setText(getString(R.string.average_per_month,
                MoneyUtils.format(averageMonth)));
    }

    private void bindCategoryChart(Map<String, Double> categories, double totalAll) {
        layoutBreakdown.removeAllViews();

        String[] labels = new String[categories.size()];
        double[] values = new double[categories.size()];
        int[] colors = new int[categories.size()];

        int index = 0;
        for (Map.Entry<String, Double> entry : categories.entrySet()) {
            labels[index] = entry.getKey();
            values[index] = entry.getValue();
            colors[index] = CategoryUtils.color(requireContext(), entry.getKey());
            index++;
        }
        categoryChart.setData(labels, values, colors);

        if (categories.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.no_data_yet);
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            empty.setTextSize(12);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(12), 0, dp(4));
            layoutBreakdown.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Map.Entry<String, Double> entry : categories.entrySet()) {
            View row = inflater.inflate(R.layout.item_category_stat, layoutBreakdown, false);
            View dot = row.findViewById(R.id.view_category_dot);
            TextView name = row.findViewById(R.id.tv_category_name);
            TextView amount = row.findViewById(R.id.tv_category_amount);
            TextView percentLabel = row.findViewById(R.id.tv_category_percent);
            ProgressBar progress = row.findViewById(R.id.progress_category);

            int color = CategoryUtils.color(requireContext(), entry.getKey());
            int percent = MoneyUtils.percent(entry.getValue(), totalAll);
            dot.setBackgroundTintList(ColorStateList.valueOf(color));
            name.setText(entry.getKey());
            amount.setText(MoneyUtils.format(entry.getValue()));
            percentLabel.setText(getString(R.string.percent_of_total, percent));
            progress.setProgress(Math.min(100, Math.max(0, percent)));
            progress.setProgressTintList(ColorStateList.valueOf(color));
            layoutBreakdown.addView(row);
        }
    }

    private void bindMonthlyChart() {
        Map<String, Double> monthly = db.getMonthlyTotals(6);
        String[] labels = new String[monthly.size()];
        double[] values = new double[monthly.size()];
        int index = 0;
        for (Map.Entry<String, Double> entry : monthly.entrySet()) {
            labels[index] = DateUtils.monthShort(entry.getKey());
            values[index] = entry.getValue();
            index++;
        }
        monthlyChart.setData(labels, values, monthly.size() - 1);
    }

    private void bindComparison(String currentMonth, String previousMonth,
                                double totalMonth, double totalPrevious) {
        tvComparisonDetail.setText(getString(R.string.comparison_detail,
                MoneyUtils.format(totalMonth), MoneyUtils.format(totalPrevious)));

        int iconRes;
        int colorRes;
        String title;

        if (totalPrevious <= 0 && totalMonth <= 0) {
            title = getString(R.string.no_comparison);
            iconRes = R.drawable.ic_trending_up;
            colorRes = R.color.text_muted;
        } else if (totalPrevious <= 0) {
            title = getString(R.string.no_comparison);
            iconRes = R.drawable.ic_trending_up;
            colorRes = R.color.text_muted;
        } else {
            double difference = totalMonth - totalPrevious;
            if (Math.abs(difference) < 0.005) {
                title = getString(R.string.comparison_same);
                iconRes = R.drawable.ic_trending_up;
                colorRes = R.color.text_muted;
            } else if (difference > 0) {
                title = getString(R.string.comparison_up, MoneyUtils.format(difference));
                iconRes = R.drawable.ic_trending_up;
                colorRes = R.color.danger;
            } else {
                title = getString(R.string.comparison_down, MoneyUtils.format(-difference));
                iconRes = R.drawable.ic_trending_down;
                colorRes = R.color.success;
            }
        }

        tvComparisonTitle.setText(title);
        ivComparisonIcon.setImageResource(iconRes);
        ivComparisonIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), colorRes)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (db != null) {
            loadStatistics();
        }
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
