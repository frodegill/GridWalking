package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.content.Context;
import android.util.AttributeSet;

/* https://enzam.wordpress.com/2013/09/29/android-preference-show-current-value-in-summary/ */
public class EditTextPreference extends android.preference.EditTextPreference {

    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        return this.getText();
    }
}
