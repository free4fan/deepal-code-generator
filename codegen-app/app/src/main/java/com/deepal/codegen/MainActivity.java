package com.deepal.codegen;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String MIGRATION_LANG = "lang";

    private static final int THEME_SYSTEM = 0;
    private static final int THEME_LIGHT = 1;
    private static final int THEME_DARK = 2;

    private EditText vinInput;
    private TextView resultView;
    private MaterialButtonToggleGroup themeGroup;
    private MaterialButtonToggleGroup langGroup;
    private boolean restoringState = false;
    private boolean hasResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        migrateLanguageFromPrefs();
        applyThemeMode(getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_THEME, THEME_SYSTEM));
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        setContentView(R.layout.activity_main);

        vinInput = findViewById(R.id.vinInput);
        resultView = findViewById(R.id.result);
        themeGroup = findViewById(R.id.themeGroup);
        langGroup = findViewById(R.id.langGroup);

        findViewById(R.id.btnGenerate).setOnClickListener(v -> generate());

        vinInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                generate();
                return true;
            }
            return false;
        });

        findViewById(R.id.btnCopy).setOnClickListener(v -> copyResult());

        findViewById(R.id.btnClear).setOnClickListener(v -> clearAll());

        findViewById(R.id.btnHelp).setOnClickListener(v -> showHelp());

        findViewById(R.id.btnAbout).setOnClickListener(v -> showAbout());

        themeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || restoringState) {
                return;
            }
            int mode = THEME_SYSTEM;
            if (checkedId == R.id.themeLight) {
                mode = THEME_LIGHT;
            } else if (checkedId == R.id.themeDark) {
                mode = THEME_DARK;
            }
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            if (prefs.getInt(KEY_THEME, THEME_SYSTEM) != mode) {
                prefs.edit().putInt(KEY_THEME, mode).apply();
                applyThemeMode(mode);
            }
        });

        langGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || restoringState) {
                return;
            }
            String lang = checkedId == R.id.langRu ? "ru" : "en";
            if (!lang.equals(currentLanguage())) {
                setLanguage(lang);
            }
        });

        updateToggleStates();
    }

    private void migrateLanguageFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String legacy = prefs.getString(MIGRATION_LANG, null);
        if (legacy == null) {
            return;
        }
        if (!legacy.equals(currentLanguage())) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(legacy));
        }
        prefs.edit().remove(MIGRATION_LANG).apply();
    }

    private String currentLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        return locales.isEmpty()
                ? getResources().getConfiguration().locale.getLanguage()
                : locales.get(0).getLanguage();
    }

    private void setLanguage(String lang) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
    }

    private void applyThemeMode(int mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void updateToggleStates() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        int themeId = theme == THEME_LIGHT ? R.id.themeLight
                : theme == THEME_DARK ? R.id.themeDark : R.id.themeSystem;

        String lang = currentLanguage();
        restoringState = true;
        themeGroup.check(themeId);
        langGroup.check("ru".equals(lang) ? R.id.langRu : R.id.langEn);
        restoringState = false;
    }

    private void showHelp() {
        String[] steps = getResources().getStringArray(R.array.help_steps);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < steps.length; i++) {
            if (i > 0) {
                sb.append("\n\n");
            }
            sb.append(Html.fromHtml(steps[i], Html.FROM_HTML_MODE_LEGACY));
        }
        TextView tv = new TextView(this);
        tv.setPadding(32, 24, 32, 8);
        tv.setTextSize(15);
        tv.setText(sb);
        tv.setMovementMethod(android.text.method.ScrollingMovementMethod.getInstance());
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.help_title)
                .setView(tv)
                .setPositiveButton(R.string.help_dismiss, null)
                .show();
    }

    private void showAbout() {
        SpannableStringBuilder sb = new SpannableStringBuilder(
                Html.fromHtml(getString(R.string.about_body, BuildConfig.VERSION_NAME),
                        Html.FROM_HTML_MODE_LEGACY));
        TextView tv = new TextView(this);
        tv.setPadding(32, 24, 32, 8);
        tv.setTextSize(15);
        tv.setText(sb);
        tv.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setView(tv)
                .setPositiveButton(R.string.about_dismiss, null)
                .show();
    }

    private void generate() {
        String vin = vinInput.getText().toString().trim();
        if (vin.length() < 8) {
            Snackbar.make(resultView, R.string.error_short, Snackbar.LENGTH_LONG).show();
            return;
        }
        int badIndex = CodeGen.indexOfUnacceptable(vin);
        if (badIndex >= 0) {
            String msg = getString(R.string.error_chars, badIndex + 1);
            Snackbar.make(resultView, msg, Snackbar.LENGTH_LONG).show();
            return;
        }
        resultView.setText(CodeGen.genCode(vin));
        hasResult = true;
    }

    private void copyResult() {
        if (!hasResult) {
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("code", resultView.getText().toString()));
        Snackbar.make(resultView, R.string.copied, Snackbar.LENGTH_SHORT).show();
    }

    private void clearAll() {
        vinInput.setText("");
        resultView.setText(R.string.result_placeholder);
        hasResult = false;
    }
}
