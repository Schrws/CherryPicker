package space.schrws.cherrypicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;
import java.util.*;

/**
 * Created by Schrws on 2018-10-26.
 */
public class CherryDatePickerDelegate extends CherryDatePicker.AbstractDatePickerDelegate {
    private static final int USE_LOCALE = 0;
    private static final int UNINITIALIZED = -1;
    private static final int VIEW_MONTH_DAY = 0;
    private static final int VIEW_YEAR = 1;
    private static final int DEFAULT_START_YEAR = 1900;
    private static final int DEFAULT_END_YEAR = 2100;
    private static final int ANIMATION_DURATION = 300;
    private static final int[] ATTRS_TEXT_COLOR = new int[] {android.R.attr.textColor};
    private static final int[] ATTRS_DISABLED_ALPHA = new int[] {android.R.attr.disabledAlpha};
    private DateFormat mYearFormat;
    private DateFormat mMonthDayFormat;
    // Top-level container.
    private ViewGroup mContainer;
    // Header views.
    private TextView mHeaderYear;
    private TextView mHeaderMonthDay;
    // Picker views.
    private ViewAnimator mAnimator;
    private DayPickerView mDayPickerView;
    private YearPickerView mYearPickerView;
    // Accessibility strings.
    private String mSelectDay;
    private String mSelectYear;
    private int mCurrentView = UNINITIALIZED;
    private final Calendar mTempDate;
    private final Calendar mMinDate;
    private final Calendar mMaxDate;
    private int mFirstDayOfWeek = USE_LOCALE;
    public CherryDatePickerDelegate(CherryDatePicker delegator, Context context, AttributeSet attrs,
                                      int defStyleAttr, int defStyleRes) {
        super(delegator, context);
        final Locale locale = mCurrentLocale;
        mCurrentDate = Calendar.getInstance(locale);
        mTempDate = Calendar.getInstance(locale);
        mMinDate = Calendar.getInstance(locale);
        mMaxDate = Calendar.getInstance(locale);
        mMinDate.set(DEFAULT_START_YEAR, Calendar.JANUARY, 1);
        mMaxDate.set(DEFAULT_END_YEAR, Calendar.DECEMBER, 31);
        final Resources res = mDelegator.getResources();
        final TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.CherryDatePicker, defStyleAttr, defStyleRes);
        final LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final int layoutResourceId = a.getResourceId(
                R.styleable.CherryDatePicker_internalLayout, R.layout.date_picker_material);
        // Set up and attach container.
        mContainer = (ViewGroup) inflater.inflate(layoutResourceId, mDelegator, false);
        mContainer.setSaveFromParentEnabled(false);
        mDelegator.addView(mContainer);
        // Set up header views.
        final ViewGroup header = mContainer.findViewById(R.id.date_picker_header);
        mHeaderYear = header.findViewById(R.id.date_picker_header_year);
        mHeaderYear.setOnClickListener(mOnHeaderClickListener);
        mHeaderMonthDay = header.findViewById(R.id.date_picker_header_date);
        mHeaderMonthDay.setOnClickListener(mOnHeaderClickListener);
        // For the sake of backwards compatibility, attempt to extract the text
        // color from the header month text appearance. If it's set, we'll let
        // that override the "real" header text color.
        ColorStateList headerTextColor = null;
        @SuppressWarnings("deprecation")
        final int monthHeaderTextAppearance = a.getResourceId(
                R.styleable.CherryDatePicker_headerMonthTextAppearance, 0);
        if (monthHeaderTextAppearance != 0) {
            final TypedArray textAppearance = mContext.obtainStyledAttributes(null,
                    ATTRS_TEXT_COLOR, 0, monthHeaderTextAppearance);
            final ColorStateList legacyHeaderTextColor = textAppearance.getColorStateList(0);
            headerTextColor = legacyHeaderTextColor; //applyLegacyColorFixes(legacyHeaderTextColor);
            textAppearance.recycle();
        }
        if (headerTextColor == null) {
            headerTextColor = a.getColorStateList(R.styleable.CherryDatePicker_headerTextColor);
        }
        if (headerTextColor != null) {
            mHeaderYear.setTextColor(headerTextColor);
            mHeaderMonthDay.setTextColor(headerTextColor);
        }
        // Set up header background, if available.
        if (a.hasValueOrEmpty(R.styleable.CherryDatePicker_headerBackground)) {
            header.setBackground(a.getDrawable(R.styleable.CherryDatePicker_headerBackground));
        }
        a.recycle();
        // Set up picker container.
        mAnimator = mContainer.findViewById(R.id.animator);
        // Set up day picker view.
        mDayPickerView = mAnimator.findViewById(R.id.date_picker_day_picker);
        mDayPickerView.setFirstDayOfWeek(mFirstDayOfWeek);
        mDayPickerView.setMinDate(mMinDate.getTimeInMillis());
        mDayPickerView.setMaxDate(mMaxDate.getTimeInMillis());
        mDayPickerView.setDate(mCurrentDate.getTimeInMillis());
        mDayPickerView.setOnDaySelectedListener(mOnDaySelectedListener);
        // Set up year picker view.
        mYearPickerView = mAnimator.findViewById(R.id.date_picker_year_picker);
        mYearPickerView.setRange(mMinDate, mMaxDate);
        mYearPickerView.setYear(mCurrentDate.get(Calendar.YEAR));
        mYearPickerView.setOnYearSelectedListener(mOnYearSelectedListener);
        // Set up content descriptions.
        mSelectDay = "asd"; //res.getString(R.string.select_day);
        mSelectYear = "asd"; //res.getString(R.string.select_year);
        // Initialize for current locale. This also initializes the date, so no
        // need to call onDateChanged.
        onLocaleChanged(mCurrentLocale);
        setCurrentView(VIEW_MONTH_DAY);
    }
    /**
     * The legacy text color might have been poorly defined. Ensures that it
     * has an appropriate activated state, using the selected state if one
     * exists or modifying the default text color otherwise.
     *
     * @param color a legacy text color, or {@code null}
     * @return a color state list with an appropriate activated state, or
     *         {@code null} if a valid activated state could not be generated
     */
//    @Nullable
//    private ColorStateList applyLegacyColorFixes(@Nullable ColorStateList color) {
//        if (color == null || color.hasState(R.attr.state_activated)) {
//            return color;
//        }
//        final int activatedColor;
//        final int defaultColor;
//        if (color.hasState(R.attr.state_selected)) {
//            activatedColor = color.getColorForState(Utils.getState(Utils.VIEW_STATE_ENABLED | Utils.VIEW_STATE_SELECTED), 0);
//            defaultColor = color.getColorForState(Utils.getState(Utils.VIEW_STATE_ENABLED), 0);
//        } else {
//            activatedColor = color.getDefaultColor();
//            // Generate a non-activated color using the disabled alpha.
//            final TypedArray ta = mContext.obtainStyledAttributes(ATTRS_DISABLED_ALPHA);
//            final float disabledAlpha = ta.getFloat(0, 0.30f);
//            defaultColor = multiplyAlphaComponent(activatedColor, disabledAlpha);
//        }
//        if (activatedColor == 0 || defaultColor == 0) {
//            // We somehow failed to obtain the colors.
//            return null;
//        }
//        final int[][] stateSet = new int[][] {{ R.attr.state_activated }, {}};
//        final int[] colors = new int[] { activatedColor, defaultColor };
//        return new ColorStateList(stateSet, colors);
//    }
    private int multiplyAlphaComponent(int color, float alphaMod) {
        final int srcRgb = color & 0xFFFFFF;
        final int srcAlpha = (color >> 24) & 0xFF;
        final int dstAlpha = (int) (srcAlpha * alphaMod + 0.5f);
        return srcRgb | (dstAlpha << 24);
    }
    /**
     * Listener called when the user selects a day in the day picker view.
     */
    private final DayPickerView.OnDaySelectedListener mOnDaySelectedListener = new DayPickerView.OnDaySelectedListener() {
        @Override
        public void onDaySelected(DayPickerView view, Calendar day) {
            mCurrentDate.setTimeInMillis(day.getTimeInMillis());
            onDateChanged(true, true);
        }
    };
    /**
     * Listener called when the user selects a year in the year picker view.
     */
    private final YearPickerView.OnYearSelectedListener mOnYearSelectedListener = new YearPickerView.OnYearSelectedListener() {
        @Override
        public void onYearChanged(YearPickerView view, int year) {
            // If the newly selected month / year does not contain the
            // currently selected day number, change the selected day number
            // to the last day of the selected month or year.
            // e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
            // e.g. Switching from 2012 to 2013 when Feb 29, 2012 is selected -> Feb 28, 2013
            final int day = mCurrentDate.get(Calendar.DAY_OF_MONTH);
            final int month = mCurrentDate.get(Calendar.MONTH);
            final int daysInMonth = getDaysInMonth(month, year);
            if (day > daysInMonth) {
                mCurrentDate.set(Calendar.DAY_OF_MONTH, daysInMonth);
            }
            mCurrentDate.set(Calendar.YEAR, year);
            onDateChanged(true, true);
            // Automatically switch to day picker.
            setCurrentView(VIEW_MONTH_DAY);
            // Switch focus back to the year text.
            mHeaderYear.requestFocus();
        }
    };
    /**
     * Listener called when the user clicks on a header item.
     */
    private final OnClickListener mOnHeaderClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            tryVibrate();
            int i = v.getId();
            if (i == R.id.date_picker_header_year) {
                setCurrentView(VIEW_YEAR);

            } else if (i == R.id.date_picker_header_date) {
                setCurrentView(VIEW_MONTH_DAY);

            }
        }
    };
    @Override
    protected void onLocaleChanged(Locale locale) {
        final TextView headerYear = mHeaderYear;
        if (headerYear == null) {
            // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return;
        }
        // Update the date formatter.
        mMonthDayFormat = DateFormat.getInstanceForSkeleton("EMMMd", locale);
        mMonthDayFormat.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
        mYearFormat = DateFormat.getInstanceForSkeleton("y", locale);
        // Update the header text.
        onCurrentDateChanged(false);
    }
    private void onCurrentDateChanged(boolean announce) {
        if (mHeaderYear == null) {
            // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return;
        }
        final String year = mYearFormat.format(mCurrentDate.getTime());
        mHeaderYear.setText(year);
        final String monthDay = mMonthDayFormat.format(mCurrentDate.getTime());
        mHeaderMonthDay.setText(monthDay);
        // TODO: This should use live regions.
        if (announce) {
            mAnimator.announceForAccessibility(getFormattedCurrentDate());
        }
    }
    private void setCurrentView(final int viewIndex) {
        switch (viewIndex) {
            case VIEW_MONTH_DAY:
                mDayPickerView.setDate(mCurrentDate.getTimeInMillis());
                if (mCurrentView != viewIndex) {
                    mHeaderMonthDay.setActivated(true);
                    mHeaderYear.setActivated(false);
                    mAnimator.setDisplayedChild(VIEW_MONTH_DAY);
                    mCurrentView = viewIndex;
                }
                mAnimator.announceForAccessibility(mSelectDay);
                break;
            case VIEW_YEAR:
                final int year = mCurrentDate.get(Calendar.YEAR);
                mYearPickerView.setYear(year);
                mYearPickerView.post(new Runnable() {
                    @Override
                    public void run() {
                        mYearPickerView.requestFocus();
                        final View selected = mYearPickerView.getSelectedView();
                        if (selected != null) {
                            selected.requestFocus();
                        }
                    }
                });
                if (mCurrentView != viewIndex) {
                    mHeaderMonthDay.setActivated(false);
                    mHeaderYear.setActivated(true);
                    mAnimator.setDisplayedChild(VIEW_YEAR);
                    mCurrentView = viewIndex;
                }
                mAnimator.announceForAccessibility(mSelectYear);
                break;
        }
    }
    @Override
    public void init(int year, int month, int dayOfMonth,
                     CherryDatePicker.OnDateChangedListener callBack) {
        setDate(year, month, dayOfMonth);
        onDateChanged(false, false);
        mOnDateChangedListener = callBack;
    }
    @Override
    public void updateDate(int year, int month, int dayOfMonth) {
        setDate(year, month, dayOfMonth);
        onDateChanged(false, true);
    }
    private void setDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.MONTH, month);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        resetAutofilledValue();
    }
    private void onDateChanged(boolean fromUser, boolean callbackToClient) {
        final int year = mCurrentDate.get(Calendar.YEAR);
        if (callbackToClient
                && (mOnDateChangedListener != null || mAutoFillChangeListener != null)) {
            final int monthOfYear = mCurrentDate.get(Calendar.MONTH);
            final int dayOfMonth = mCurrentDate.get(Calendar.DAY_OF_MONTH);
            if (mOnDateChangedListener != null) {
                mOnDateChangedListener.onDateChanged(mDelegator, year, monthOfYear, dayOfMonth);
            }
            if (mAutoFillChangeListener != null) {
                mAutoFillChangeListener.onDateChanged(mDelegator, year, monthOfYear, dayOfMonth);
            }
        }
        mDayPickerView.setDate(mCurrentDate.getTimeInMillis());
        mYearPickerView.setYear(year);
        onCurrentDateChanged(fromUser);
        if (fromUser) {
            tryVibrate();
        }
    }
    @Override
    public int getYear() {
        return mCurrentDate.get(Calendar.YEAR);
    }
    @Override
    public int getMonth() {
        return mCurrentDate.get(Calendar.MONTH);
    }
    @Override
    public int getDayOfMonth() {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }
    @Override
    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (mTempDate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) == mMinDate.get(Calendar.DAY_OF_YEAR)) {
            // Same day, no-op.
            return;
        }
        if (mCurrentDate.before(mTempDate)) {
            mCurrentDate.setTimeInMillis(minDate);
            onDateChanged(false, true);
        }
        mMinDate.setTimeInMillis(minDate);
        mDayPickerView.setMinDate(minDate);
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }
    @Override
    public Calendar getMinDate() {
        return mMinDate;
    }
    @Override
    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (mTempDate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) == mMaxDate.get(Calendar.DAY_OF_YEAR)) {
            // Same day, no-op.
            return;
        }
        if (mCurrentDate.after(mTempDate)) {
            mCurrentDate.setTimeInMillis(maxDate);
            onDateChanged(false, true);
        }
        mMaxDate.setTimeInMillis(maxDate);
        mDayPickerView.setMaxDate(maxDate);
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }
    @Override
    public Calendar getMaxDate() {
        return mMaxDate;
    }
    @Override
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        mFirstDayOfWeek = firstDayOfWeek;
        mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
    }
    @Override
    public int getFirstDayOfWeek() {
        if (mFirstDayOfWeek != USE_LOCALE) {
            return mFirstDayOfWeek;
        }
        return mCurrentDate.getFirstDayOfWeek();
    }
    @Override
    public void setEnabled(boolean enabled) {
        mContainer.setEnabled(enabled);
        mDayPickerView.setEnabled(enabled);
        mYearPickerView.setEnabled(enabled);
        mHeaderYear.setEnabled(enabled);
        mHeaderMonthDay.setEnabled(enabled);
    }
    @Override
    public boolean isEnabled() {
        return mContainer.isEnabled();
    }
    @Override
    public CalendarView getCalendarView() {
        throw new UnsupportedOperationException("Not supported by calendar-mode DatePicker");
    }
    @Override
    public void setCalendarViewShown(boolean shown) {
        // No-op for compatibility with the old DatePicker.
    }
    @Override
    public boolean getCalendarViewShown() {
        return false;
    }
    @Override
    public void setSpinnersShown(boolean shown) {
        // No-op for compatibility with the old DatePicker.
    }
    @Override
    public boolean getSpinnersShown() {
        return false;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setCurrentLocale(newConfig.locale);
    }
    @Override
    public Parcelable onSaveInstanceState(Parcelable superState) {
        final int year = mCurrentDate.get(Calendar.YEAR);
        final int month = mCurrentDate.get(Calendar.MONTH);
        final int day = mCurrentDate.get(Calendar.DAY_OF_MONTH);
        int listPosition = -1;
        int listPositionOffset = -1;
        if (mCurrentView == VIEW_MONTH_DAY) {
            listPosition = mDayPickerView.getMostVisiblePosition();
        } else if (mCurrentView == VIEW_YEAR) {
            listPosition = mYearPickerView.getFirstVisiblePosition();
            listPositionOffset = mYearPickerView.getFirstPositionOffset();
        }
        return new SavedState(superState, year, month, day, mMinDate.getTimeInMillis(),
                mMaxDate.getTimeInMillis(), mCurrentView, listPosition, listPositionOffset);
    }
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            final SavedState ss = (SavedState) state;
            // TODO: Move instance state into DayPickerView, YearPickerView.
            mCurrentDate.set(ss.getSelectedYear(), ss.getSelectedMonth(), ss.getSelectedDay());
            mMinDate.setTimeInMillis(ss.getMinDate());
            mMaxDate.setTimeInMillis(ss.getMaxDate());
            onCurrentDateChanged(false);
            final int currentView = ss.getCurrentView();
            setCurrentView(currentView);
            final int listPosition = ss.getListPosition();
            if (listPosition != -1) {
                if (currentView == VIEW_MONTH_DAY) {
                    mDayPickerView.setPosition(listPosition);
                } else if (currentView == VIEW_YEAR) {
                    final int listPositionOffset = ss.getListPositionOffset();
                    mYearPickerView.setSelectionFromTop(listPosition, listPositionOffset);
                }
            }
        }
    }
    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }
    public CharSequence getAccessibilityClassName() {
        return CherryDatePicker.class.getName();
    }
    public static int getDaysInMonth(int month, int year) {
        switch (month) {
            case Calendar.JANUARY:
            case Calendar.MARCH:
            case Calendar.MAY:
            case Calendar.JULY:
            case Calendar.AUGUST:
            case Calendar.OCTOBER:
            case Calendar.DECEMBER:
                return 31;
            case Calendar.APRIL:
            case Calendar.JUNE:
            case Calendar.SEPTEMBER:
            case Calendar.NOVEMBER:
                return 30;
            case Calendar.FEBRUARY:
                return (year % 4 == 0) ? 29 : 28;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
    }
    private void tryVibrate() {
        mDelegator.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }
}