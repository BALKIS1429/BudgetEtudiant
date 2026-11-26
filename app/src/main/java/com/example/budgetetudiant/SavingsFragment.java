package com.example.budgetetudiant;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Objectif d'épargne : création, modification, ajout de montant, remise à zéro
 * et suppression, avec confirmation pour les actions destructives.
 */
public class SavingsFragment extends Fragment {

    private DBHelper db;

    private View cardNoGoal;
    private View layoutContent;
    private TextView tvGoalName;
    private TextView tvGoalSavedHero;
    private TextView tvGoalTargetHero;
    private TextView tvGoalPercentHero;
    private TextView tvGoalTarget;
    private TextView tvGoalSaved;
    private TextView tvGoalRemaining;
    private ProgressBar pbGoal;
    private View cardReached;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_savings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new DBHelper(requireContext());

        cardNoGoal = view.findViewById(R.id.card_no_goal);
        layoutContent = view.findViewById(R.id.layout_goal_content);
        tvGoalName = view.findViewById(R.id.tv_goal_name);
        tvGoalSavedHero = view.findViewById(R.id.tv_goal_saved_hero);
        tvGoalTargetHero = view.findViewById(R.id.tv_goal_target_hero);
        tvGoalPercentHero = view.findViewById(R.id.tv_goal_percent_hero);
        tvGoalTarget = view.findViewById(R.id.tv_goal_target);
        tvGoalSaved = view.findViewById(R.id.tv_goal_saved);
        tvGoalRemaining = view.findViewById(R.id.tv_goal_remaining);
        pbGoal = view.findViewById(R.id.pb_goal);
        cardReached = view.findViewById(R.id.card_goal_reached);

        view.findViewById(R.id.btn_create_goal).setOnClickListener(v -> showGoalDialog(null));
        view.findViewById(R.id.btn_edit_goal).setOnClickListener(v -> {
            SavingsGoal goal = db.getSavingsGoal();
            if (goal != null) {
                showGoalDialog(goal);
            }
        });
        view.findViewById(R.id.btn_add_savings).setOnClickListener(v -> {
            SavingsGoal goal = db.getSavingsGoal();
            if (goal != null) {
                showAddSavingsDialog(goal);
            }
        });
        view.findViewById(R.id.btn_manage_goal).setOnClickListener(v -> {
            SavingsGoal goal = db.getSavingsGoal();
            if (goal != null) {
                showManageDialog(goal);
            }
        });

        loadGoal();
    }

    private void loadGoal() {
        if (db == null) {
            return;
        }
        SavingsGoal goal = db.getSavingsGoal();
        if (goal == null) {
            cardNoGoal.setVisibility(View.VISIBLE);
            layoutContent.setVisibility(View.GONE);
            return;
        }

        cardNoGoal.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        String name = goal.name == null || goal.name.trim().isEmpty()
                ? getString(R.string.goal_name_default) : goal.name;
        tvGoalName.setText(name);
        tvGoalSavedHero.setText(MoneyUtils.format(goal.savedAmount));
        tvGoalTargetHero.setText(getString(R.string.goal_target_of,
                MoneyUtils.format(goal.targetAmount)));
        tvGoalPercentHero.setText(getString(R.string.goal_progress, goal.progressPercent()));
        pbGoal.setProgress(Math.min(100, Math.max(0, goal.progressPercent())));
        cardReached.setVisibility(goal.isReached() ? View.VISIBLE : View.GONE);

        tvGoalTarget.setText(MoneyUtils.format(goal.targetAmount));
        tvGoalSaved.setText(MoneyUtils.format(goal.savedAmount));
        tvGoalRemaining.setText(goal.isReached()
                ? MoneyUtils.format(0) : MoneyUtils.format(goal.remainingAmount()));
    }

    private void showGoalDialog(@Nullable SavingsGoal goal) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_savings_goal, null);
        TextInputLayout tilName = view.findViewById(R.id.til_goal_name);
        TextInputLayout tilAmount = view.findViewById(R.id.til_goal_amount);
        EditText etName = view.findViewById(R.id.etGoalName);
        EditText etAmount = view.findViewById(R.id.etGoalAmount);

        if (goal != null) {
            etName.setText(goal.name);
            etAmount.setText(MoneyUtils.formatAmount(goal.targetAmount));
            etAmount.setSelection(etAmount.getText().length());
        }
        clearErrorOnEdit(etName, tilName);
        clearErrorOnEdit(etAmount, tilAmount);

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(goal == null ? R.string.create_savings_goal : R.string.edit_goal)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(
                        androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    double amount = MoneyUtils.parseAmount(etAmount.getText().toString());
                    if (!MoneyUtils.isValidAmount(amount)) {
                        tilAmount.setError(getString(R.string.error_goal_amount));
                        return;
                    }
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        name = getString(R.string.goal_name_default);
                    }

                    if (goal == null) {
                        db.createSavingsGoal(name, amount);
                    } else {
                        db.updateSavingsGoal(goal.id, name, amount, goal.savedAmount);
                    }
                    NotificationHelper.checkSavingsGoalAlerts(requireContext());
                    dialog.dismiss();
                    loadGoal();
                    Toast.makeText(requireContext(), R.string.goal_saved,
                            Toast.LENGTH_SHORT).show();
                }));
        dialog.show();
    }

    private void showAddSavingsDialog(@NonNull SavingsGoal goal) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_savings, null);
        TextView info = view.findViewById(R.id.tv_savings_dialog_info);
        TextInputLayout tilAmount = view.findViewById(R.id.til_savings_amount);
        EditText etAmount = view.findViewById(R.id.etSavingsAmount);

        info.setText(getString(R.string.savings_dialog_info,
                MoneyUtils.format(goal.targetAmount), MoneyUtils.format(goal.savedAmount)));
        clearErrorOnEdit(etAmount, tilAmount);

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_savings_title)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(
                        androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    double amount = MoneyUtils.parseAmount(etAmount.getText().toString());
                    if (!MoneyUtils.isValidAmount(amount)) {
                        tilAmount.setError(getString(R.string.error_savings_amount));
                        return;
                    }
                    db.addToSavingsGoal(goal.id, amount);
                    NotificationHelper.checkSavingsGoalAlerts(requireContext());
                    dialog.dismiss();
                    loadGoal();
                    Toast.makeText(requireContext(), R.string.savings_added,
                            Toast.LENGTH_SHORT).show();
                }));
        dialog.show();
    }

    private void showManageDialog(@NonNull SavingsGoal goal) {
        String[] actions = {
                getString(R.string.reset_goal),
                getString(R.string.delete_goal)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.manage_goal_title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        confirmReset(goal);
                    } else {
                        confirmDelete(goal);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmReset(@NonNull SavingsGoal goal) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reset_goal_confirm_title)
                .setMessage(R.string.reset_goal_confirm_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reset_goal, (dialog, which) -> {
                    db.resetSavingsGoal(goal.id);
                    loadGoal();
                    Toast.makeText(requireContext(), R.string.goal_reset,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void confirmDelete(@NonNull SavingsGoal goal) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_goal_confirm_title)
                .setMessage(R.string.delete_goal_confirm_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    db.deleteSavingsGoal(goal.id);
                    loadGoal();
                    Toast.makeText(requireContext(), R.string.goal_deleted,
                            Toast.LENGTH_SHORT).show();
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
    public void onResume() {
        super.onResume();
        if (db != null) {
            loadGoal();
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
