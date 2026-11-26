package com.example.budgetetudiant;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptateur de la liste des dépenses avec actions modifier / supprimer.
 */
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnExpenseActionListener {
        void onEdit(Expense expense);

        void onDelete(Expense expense);
    }

    private final List<Expense> expenses = new ArrayList<>();
    private final OnExpenseActionListener listener;

    public ExpenseAdapter(OnExpenseActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Expense> newExpenses) {
        expenses.clear();
        if (newExpenses != null) {
            expenses.addAll(newExpenses);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        Context context = holder.itemView.getContext();
        int categoryColor = CategoryUtils.color(context, expense.category);

        holder.title.setText(expense.title);
        holder.meta.setText(expense.category + " · " + DateUtils.dateLabel(expense.date));
        holder.amount.setText(MoneyUtils.format(expense.amount));
        holder.icon.setImageResource(CategoryUtils.iconRes(expense.category));
        holder.icon.setImageTintList(ColorStateList.valueOf(categoryColor));
        holder.iconContainer.setBackgroundTintList(
                ColorStateList.valueOf(CategoryUtils.softColor(context, expense.category)));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(expense);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDelete(expense);
            }
            return true;
        });
        holder.edit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(expense);
            }
        });
        holder.delete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(expense);
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        final View iconContainer;
        final ImageView icon;
        final TextView title;
        final TextView meta;
        final TextView amount;
        final ImageButton edit;
        final ImageButton delete;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.icon_container);
            icon = itemView.findViewById(R.id.iv_category_icon);
            title = itemView.findViewById(R.id.tv_expense_title);
            meta = itemView.findViewById(R.id.tv_expense_meta);
            amount = itemView.findViewById(R.id.tv_expense_amount);
            edit = itemView.findViewById(R.id.btn_edit_expense);
            delete = itemView.findViewById(R.id.btn_delete_expense);
        }
    }
}
