package com.example.budgetetudiant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Graphique en barres des six derniers mois, dessiné avec Canvas.
 */
public class MonthlyChartView extends View {

    private String[] labels = new String[0];
    private double[] values = new double[0];
    private int highlightIndex = -1;

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public MonthlyChartView(Context context) {
        super(context);
        init();
    }

    public MonthlyChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MonthlyChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textPaint.setTextSize(sp(10));
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.card_stroke));
        gridPaint.setStrokeWidth(dp(1));
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_soft));
    }

    public void setData(String[] labels, double[] values, int highlightIndex) {
        this.labels = labels == null ? new String[0] : labels;
        this.values = values == null ? new double[0] : values;
        this.highlightIndex = highlightIndex;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = resolveSize((int) dp(220), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (labels.length == 0) {
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_muted));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Aucune donnée pour le moment", getWidth() / 2f,
                    getHeight() / 2f, textPaint);
            return;
        }

        double max = 0;
        for (double value : values) {
            max = Math.max(max, value);
        }
        if (max <= 0) {
            max = 1;
        }

        float left = dp(8);
        float right = getWidth() - dp(8);
        float chartTop = dp(34);
        float chartBottom = getHeight() - dp(42);
        float chartHeight = chartBottom - chartTop;
        float chartWidth = right - left;

        for (int i = 1; i <= 4; i++) {
            float lineY = chartBottom - (chartHeight * i / 4f);
            canvas.drawLine(left, lineY, right, lineY, gridPaint);
        }

        float slot = chartWidth / labels.length;
        float barWidth = Math.min(dp(30), slot * 0.48f);

        textPaint.setTextSize(sp(9));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_muted));
        canvas.drawText(MoneyUtils.compact(max), left, chartTop - dp(10), textPaint);

        for (int i = 0; i < labels.length; i++) {
            float centerX = left + slot * i + slot / 2f;
            double value = i < values.length ? values[i] : 0;
            float barHeight = value <= 0 ? 0 : (float) (chartHeight * (value / max));
            float barLeft = centerX - barWidth / 2f;
            float barRight = centerX + barWidth / 2f;

            canvas.drawRoundRect(new RectF(barLeft, chartTop, barRight, chartBottom),
                    barWidth / 2f, barWidth / 2f, trackPaint);

            if (barHeight > 0) {
                int color = i == highlightIndex
                        ? ContextCompat.getColor(getContext(), R.color.primary)
                        : ContextCompat.getColor(getContext(), R.color.lavender);
                barPaint.setColor(color);
                float barTop = chartBottom - Math.max(barHeight, dp(4));
                canvas.drawRoundRect(new RectF(barLeft, barTop, barRight, chartBottom),
                        barWidth / 2f, barWidth / 2f, barPaint);
            }

            if (value > 0) {
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
                textPaint.setTextSize(sp(8));
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(MoneyUtils.compact(value), centerX,
                        chartBottom - barHeight - dp(5), textPaint);
            }

            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            textPaint.setTextSize(sp(10));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(labels[i], centerX, getHeight() - dp(18), textPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
