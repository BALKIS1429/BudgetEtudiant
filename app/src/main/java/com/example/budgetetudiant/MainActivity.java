package com.example.budgetetudiant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/**
 * Activité hôte : navigation principale à cinq sections et accès rapide à
 * l'ajout de dépense.
 */
public class MainActivity extends BaseActivity {

    public static final String EXTRA_OPEN_TAB = "open_tab";
    private static final int PERMISSION_REQUEST_NOTIFICATION = 201;
    private static final String STATE_TAB = "state_tab";

    private BottomNavigationView bottomNavigation;
    private ExtendedFloatingActionButton fabAdd;
    private int currentTab = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyInsets(R.id.main);

        NotificationHelper.createNotificationChannel(this);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        fabAdd = findViewById(R.id.fab_add);

        fabAdd.setOnClickListener(v -> openAddExpense());
        bottomNavigation.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt(STATE_TAB, R.id.nav_home);
        }
        bottomNavigation.setSelectedItemId(currentTab);

        handleIntent(getIntent());
        getWindow().getDecorView().post(this::requestNotificationPermissionIfNeeded);
    }

    private void showTab(int itemId) {
        currentTab = itemId;
        Fragment fragment;
        if (itemId == R.id.nav_expenses) {
            fragment = new ExpensesFragment();
        } else if (itemId == R.id.nav_savings) {
            fragment = new SavingsFragment();
        } else if (itemId == R.id.nav_stats) {
            fragment = new StatisticsFragment();
        } else if (itemId == R.id.nav_profile) {
            fragment = new SettingsFragment();
        } else {
            fragment = new DashboardFragment();
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        boolean showFab = itemId == R.id.nav_home || itemId == R.id.nav_expenses;
        fabAdd.setVisibility(showFab ? View.VISIBLE : View.GONE);
    }

    /** Permet aux fragments d'ouvrir un onglet précis. */
    public void selectTab(int itemId) {
        if (bottomNavigation.getSelectedItemId() == itemId) {
            showTab(itemId);
        } else {
            bottomNavigation.setSelectedItemId(itemId);
        }
    }

    public void openAddExpense() {
        startActivity(new Intent(this, AddEditExpenseActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationHelper.checkBudgetAlerts(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String tab = intent.getStringExtra(EXTRA_OPEN_TAB);
        if (tab == null) {
            return;
        }
        intent.removeExtra(EXTRA_OPEN_TAB);
        switch (tab) {
            case "expenses":
                selectTab(R.id.nav_expenses);
                break;
            case "savings":
                selectTab(R.id.nav_savings);
                break;
            case "stats":
                selectTab(R.id.nav_stats);
                break;
            case "profile":
                selectTab(R.id.nav_profile);
                break;
            default:
                selectTab(R.id.nav_home);
                break;
        }
    }

    public void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (!AppPreferences.areNotificationsEnabled(this)) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_NOTIFICATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_NOTIFICATION) {
            if (grantResults.length > 0
                    && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                AppPreferences.setNotificationsEnabled(this, false);
                Toast.makeText(this, R.string.permission_notifications_denied,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_TAB, currentTab);
        super.onSaveInstanceState(outState);
    }
}
