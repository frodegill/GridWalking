package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.WindowInsetsCompat.Type;

public class InsetHelper {

    /**
     * Applies system bar insets as padding to the provided view (status & nav bars).
     *
     * @param view         The view to apply insets to.
     * @param applyLeft    Whether to apply left inset as padding.
     * @param applyTop     Whether to apply top inset as padding.
     * @param applyRight   Whether to apply right inset as padding.
     * @param applyBottom  Whether to apply bottom inset as padding.
     */
    public static void applySystemBarInsets(final View view, boolean applyLeft, boolean applyTop, boolean applyRight, boolean applyBottom) {
        if (view == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @Override
            @NonNull
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                int left = applyLeft ? insets.getInsets(Type.systemBars()).left : 0;
                int top = applyTop ? insets.getInsets(Type.systemBars()).top : 0;
                int right = applyRight ? insets.getInsets(Type.systemBars()).right : 0;
                int bottom = applyBottom ? insets.getInsets(Type.systemBars()).bottom : 0;

                v.setPadding(left, top, right, bottom);
                return insets;
            }
        });
    }

    /**
     * Shortcut for applying all sides.
     */
    public static void applySystemBarInsets(final View view) {
        applySystemBarInsets(view, true, true, true, true);
    }

    /**
     * Shortcut for applying only vertical (top and bottom) insets.
     */
    public static void applyVerticalInsets(final View view) {
        applySystemBarInsets(view, false, true, false, true);
    }

}
