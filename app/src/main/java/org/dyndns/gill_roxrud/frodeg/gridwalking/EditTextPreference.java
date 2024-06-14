package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.content.Context;
import android.util.AttributeSet;

/* Based on https://enzam.wordpress.com/2013/09/29/android-preference-show-current-value-in-summary/ */
public class EditTextPreference extends androidx.preference.EditTextPreference {

    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        return this.getText();
    }
}
