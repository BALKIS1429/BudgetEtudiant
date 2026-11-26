package com.example.budgetetudiant;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/**
 * Catégories de dépenses, icônes et couleurs associées.
 */
public final class CategoryUtils {

    public static final String ALL = "Toutes";
    public static final String OTHER = "Autre";

    public static final String[] CATEGORIES = {
            "Nourriture",
            "Transport",
            "Études",
            "Loisirs",
            "Santé",
            "Shopping",
            OTHER
    };

    private CategoryUtils() {
    }

    public static String[] categoriesWithAll() {
        String[] values = new String[CATEGORIES.length + 1];
        values[0] = ALL;
        System.arraycopy(CATEGORIES, 0, values, 1, CATEGORIES.length);
        return values;
    }

    public static String normalize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return OTHER;
        }
        String value = raw.trim();
        for (String category : CATEGORIES) {
            if (category.equalsIgnoreCase(value)) {
                return category;
            }
        }
        if ("etudes".equalsIgnoreCase(value)) {
            return "Études";
        }
        if ("sante".equalsIgnoreCase(value)) {
            return "Santé";
        }
        if ("loisir".equalsIgnoreCase(value)) {
            return "Loisirs";
        }
        return OTHER;
    }

    public static int indexOf(String category) {
        String normalized = normalize(category);
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(normalized)) {
                return i;
            }
        }
        return CATEGORIES.length - 1;
    }

    public static int iconRes(String category) {
        switch (normalize(category)) {
            case "Nourriture":
                return R.drawable.ic_food;
            case "Transport":
                return R.drawable.ic_transport;
            case "Études":
                return R.drawable.ic_studies;
            case "Loisirs":
                return R.drawable.ic_leisure;
            case "Santé":
                return R.drawable.ic_health;
            case "Shopping":
                return R.drawable.ic_shopping;
            default:
                return R.drawable.ic_other;
        }
    }

    public static int colorRes(String category) {
        switch (normalize(category)) {
            case "Nourriture":
                return R.color.category_food;
            case "Transport":
                return R.color.category_transport;
            case "Études":
                return R.color.category_studies;
            case "Loisirs":
                return R.color.category_leisure;
            case "Santé":
                return R.color.category_health;
            case "Shopping":
                return R.color.category_shopping;
            default:
                return R.color.category_other;
        }
    }

    public static int color(Context context, String category) {
        return ContextCompat.getColor(context, colorRes(category));
    }

    public static int softColor(Context context, String category) {
        return ColorUtils.setAlphaComponent(color(context, category), 32);
    }
}
