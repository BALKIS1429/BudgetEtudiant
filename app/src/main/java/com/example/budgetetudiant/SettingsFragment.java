package com.example.budgetetudiant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Profil et paramètres : prénom, notifications, accès au convertisseur,
 * à l'historique et à l'export PDF.
 */
public class SettingsFragment extends Fragment {

    private TextView tvProfileInitial;
    private TextView tvProfileName;
    private TextView tvVersion;
    private MaterialSwitch switchNotifications;
    private boolean updatingSwitch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvProfileInitial = view.findViewById(R.id.tv_profile_initial);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvVersion = view.findViewById(R.id.tv_version);
        switchNotifications = view.findViewById(R.id.switch_notifications);

        view.findViewById(R.id.card_profile).setOnClickListener(v -> showNameDialog());
        view.findViewById(R.id.row_converter).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ConverterActivity.class)));
        view.findViewById(R.id.row_history).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HistoryActivity.class)));
        view.findViewById(R.id.row_pdf).setOnClickListener(v ->
                PdfExporter.exportAndShare(requireActivity(), DateUtils.currentMonth()));

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingSwitch) {
                return;
            }
            AppPreferences.setNotificationsEnabled(requireContext(), isChecked);
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && !hasNotificationPermission()) {
                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).requestNotificationPermissionIfNeeded();
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.notifications_enabled,
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), R.string.notifications_disabled,
                        Toast.LENGTH_SHORT).show();
            }
        });

        bindVersion();
        refreshProfile();
        refreshNotificationSwitch();
    }

    private void refreshProfile() {
        String name = AppPreferences.getUserName(requireContext());
        if (name.isEmpty()) {
            tvProfileName.setText(R.string.app_name);
            tvProfileInitial.setText("B");
        } else {
            tvProfileName.setText(name);
            tvProfileInitial.setText(name.substring(0, 1).toUpperCase());
        }
    }

    private void refreshNotificationSwitch() {
        updatingSwitch = true;
        switchNotifications.setChecked(notificationsAllowed());
        updatingSwitch = false;
    }

    private boolean notificationsAllowed() {
        return AppPreferences.areNotificationsEnabled(requireContext())
                && hasNotificationPermission();
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void bindVersion() {
        String version = "1.0";
        try {
            PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            if (info.versionName != null) {
                version = info.versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // On garde la version par défaut.
        }
        tvVersion.setText(version);
    }

    private void showNameDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_name, null);
        TextInputLayout tilName = view.findViewById(R.id.til_name);
        EditText etName = view.findViewById(R.id.etName);
        etName.setText(AppPreferences.getUserName(requireContext()));
        etName.setSelection(etName.getText().length());

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_name_title)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(
                        androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = etName.getText().toString().trim();
                    if (name.length() > 40) {
                        tilName.setError(getString(R.string.edit_name_hint));
                        return;
                    }
                    AppPreferences.setUserName(requireContext(), name);
                    refreshProfile();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.name_saved,
                            Toast.LENGTH_SHORT).show();
                }));
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (tvProfileName != null) {
            refreshProfile();
            refreshNotificationSwitch();
        }
    }
}
