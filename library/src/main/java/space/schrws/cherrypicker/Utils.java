package space.schrws.cherrypicker;

import android.support.v4.view.ViewCompat;
import android.view.View;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Schrws on 2018-10-26.
 */
public class Utils {
    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    public static boolean parseDate(String date, Calendar outDate) {
        if (date == null || date.isEmpty()) {
            return false;
        }
        try {
            final Date parsedDate = DATE_FORMATTER.parse(date);
            outDate.setTime(parsedDate);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static boolean isLayoutRtl(View view) {
        return (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL);
    }

    public static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }
    public static long constrain(long amount, long low, long high) {
        return amount < low ? low : (amount > high ? high : amount);
    }
    public static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public static final int VIEW_STATE_ENABLED = 1, VIEW_STATE_ACTIVATED = 1 << 1, VIEW_STATE_PRESSED = 1 << 2;

    private static final int[][] STATE_SETS = new int[8][];

    static {
        STATE_SETS[0] = new int[]{0};
        STATE_SETS[1] = new int[]{android.R.attr.state_enabled};
        STATE_SETS[2] = new int[]{android.R.attr.state_activated};
        STATE_SETS[3] = new int[]{android.R.attr.state_enabled, android.R.attr.state_activated};
        STATE_SETS[4] = new int[]{android.R.attr.state_pressed};
        STATE_SETS[5] = new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed};
        STATE_SETS[6] = new int[]{android.R.attr.state_activated, android.R.attr.state_pressed};
        STATE_SETS[7] = new int[]{android.R.attr.state_enabled, android.R.attr.state_activated, android.R.attr.state_pressed};
    }

    public static int[] getState(int mask) {
        return STATE_SETS[mask];
    }
}
