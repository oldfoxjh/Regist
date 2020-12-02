package kr.co.enord.dji.utils;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

/**
 * TextView의 입력값을 필터링
 */
public class InputFilterMinMax implements InputFilter {

    private int m_min, m_max;

    public InputFilterMinMax(int min, int max){
        m_min = min;
        m_max = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            int input = Integer.parseInt(dest.toString() + source.toString());
            if (inRange(input)) {
                return null;
            }
        } catch (NumberFormatException nfe) { }

        return "";
    }

    private boolean inRange(int input){
        return input >= m_min && input <= m_max;
    }
}
