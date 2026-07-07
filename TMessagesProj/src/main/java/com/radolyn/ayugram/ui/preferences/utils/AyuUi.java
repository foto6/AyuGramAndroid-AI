/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.ui.preferences.utils;

import android.app.Activity;
import android.widget.LinearLayout;
import com.radolyn.ayugram.AyuConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.EditTextSettingsCell;
import org.telegram.ui.Cells.TextCell;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class AyuUi {
    public static void spawnEditBox(Activity parent, TextCell view, String title, Supplier<String> getter, String configField, String defaultValue) {
        spawnEditBox(parent,
                view,
                title,
                getter,
                configField,
                defaultValue,
                (s) -> {
                },
                (s) -> s
        );
    }

    public static void spawnEditBox(Activity parent, TextCell view, String title, Supplier<String> getter, String configField, String defaultValue, Consumer<String> callback) {
        spawnEditBox(parent,
                view,
                title,
                getter,
                configField,
                defaultValue,
                callback,
                (s) -> s
        );
    }

    public static void spawnEditBox(Activity parent, TextCell view, String title, Supplier<String> getter, String configField, String defaultValue, Consumer<String> callback, Function<String, String> map) {
        var builder = new AlertDialog.Builder(parent);
        builder.setTitle(title);
        var layout = new LinearLayout(parent);
        var input = new EditTextSettingsCell(parent);
        input.setText(getter.get(), true);

        layout.setGravity(LinearLayout.VERTICAL);
        layout.addView(input);
        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            var s = map.apply(input.getText());

            AyuConfig.editor.putString(configField, s).apply();
            view.setTextAndValue(title, s, true);
            callback.accept(s);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, which) -> dialog.cancel());
        builder.setNeutralButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
            var s = map.apply(defaultValue);

            AyuConfig.editor.putString(configField, s).apply();
            view.setTextAndValue(title, s, true);
            callback.accept(s);
        });

        builder.show();
    }

    public static void spawnIntBox(Activity parent, TextCell view, String title, IntSupplier getter, String configField, int defaultValue, int minValue, int maxValue, IntConsumer callback) {
        var builder = new AlertDialog.Builder(parent);
        builder.setTitle(title);
        var layout = new LinearLayout(parent);
        var input = new EditTextSettingsCell(parent);
        input.setText(String.valueOf(getter.getAsInt()), true);

        layout.setGravity(LinearLayout.VERTICAL);
        layout.addView(input);
        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            int value = parseInt(input.getText(), defaultValue, minValue, maxValue);

            AyuConfig.editor.putInt(configField, value).apply();
            view.setTextAndValue(title, String.valueOf(value), true);
            callback.accept(value);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, which) -> dialog.cancel());
        builder.setNeutralButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
            int value = parseInt(String.valueOf(defaultValue), defaultValue, minValue, maxValue);

            AyuConfig.editor.putInt(configField, value).apply();
            view.setTextAndValue(title, String.valueOf(value), true);
            callback.accept(value);
        });

        builder.show();
    }

    private static int parseInt(String value, int defaultValue, int minValue, int maxValue) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(minValue, Math.min(maxValue, parsed));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
