package com.example.budgetetudiant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Petit graphique horizontal des dépenses par catégorie, dessiné sans
 * bibliothèque externe.
 */
public class CategoryChartView extends View {

    private String[] labels = new String[0];
    private double[] values = new double[0];
    private int[] colors = new int[0];

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CategoryChartView(Context context) {
        super(context);
        init();
    }

    public CategoryChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textPaint.setTextSize(sp(12));
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_soft));
        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(String[] labels, double[] values, int[] colors) {
        this.labels = labels == null ? new String[0] : labels;
        this.values = values == null ? new double[0] : values;
        this.colors = colors == null ? new int[0] : colors;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int rows = Math.max(1, labels.length);
        int desiredHeight = (int) (dp(18) + rows * dp(46));
        int height = resolveSize(desiredHeight, heightMeasureSpec);
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

        float left = dp(2);
        float right = getWidth() - dp(2);
        float rowHeight = dp(46);
        float top = dp(10);

        for (int i = 0; i < labels.length; i++) {
            float rowTop = top + i * rowHeight;
            String label = labels[i] == null ? "" : labels[i];
            double value = i < values.length ? values[i] : 0;

            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            textPaint.setTextSize(sp(12));
            textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(label, left, rowTop + dp(13), textPaint);

            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_dark));
            textPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(MoneyUtils.formatAmount(value), right, rowTop + dp(13), textPaint);

            float trackTop = rowTop + dp(23);
            float trackHeight = dp(8);
            canvas.drawRoundRect(left, trackTop, right, trackTop + trackHeight,
                    trackHeight / 2, trackHeight / 2, trackPaint);

            float fillWidth = (float) (right - left) * (float) (value / max);
            if (fillWidth > 0) {
                int color = i < colors.length ? colors[i]
                        : ContextCompat.getColor(getContext(), R.color.primary);
                barPaint.setColor(color);
                canvas.drawRoundRect(left, trackTop, left + Math.max(fillWidth, dp(4)),
                        trackTop + trackHeight, trackHeight / 2, trackHeight / 2, barPaint);
            }
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
