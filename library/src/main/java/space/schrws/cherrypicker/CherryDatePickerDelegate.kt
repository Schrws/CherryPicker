package space.schrws.cherrypicker

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.icu.text.DateFormat
import android.icu.text.DisplayContext
import android.os.Parcelable
import android.support.annotation.Nullable
import android.util.AttributeSet
import android.util.StateSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ViewAnimator
import java.util.*

/**
 * Created by Schrws on 2018-10-26.
 */
class CherryDatePickerDelegate(
    delegator: CherryDatePicker, context: Context, attrs: AttributeSet,
    defStyleAttr: Int, defStyleRes: Int
) : CherryDatePicker.AbstractDatePickerDelegate(delegator, context) {
    private var mYearFormat: DateFormat? = null
    private var mMonthDayFormat: DateFormat? = null
    // Top-level container.
    private val mContainer: ViewGroup
    // Header views.
    private var mHeaderYear: TextView? = null
    private val mHeaderMonthDay: TextView
    // Picker views.
    private val mAnimator: ViewAnimator
    private val mDayPickerView: DayPickerView
    private val mYearPickerView: YearPickerView
    // Accessibility strings.
    private val mSelectDay: String
    private val mSelectYear: String
    private var mCurrentView = UNINITIALIZED
    private val mTempDate: Calendar
    override val minDate: Calendar
    override val maxDate: Calendar
    private var mFirstDayOfWeek = USE_LOCALE
    /**
     * Listener called when the user selects a day in the day picker view.
     */
    private val mOnDaySelectedListener = DayPickerView.OnDaySelectedListener { view, day ->
        mCurrentDate!!.timeInMillis = day.timeInMillis
        onDateChanged(true, true)
    }
    /**
     * Listener called when the user selects a year in the year picker view.
     */
    private val mOnYearSelectedListener = YearPickerView.OnYearSelectedListener { view, year ->
        // If the newly selected month / year does not contain the
        // currently selected day number, change the selected day number
        // to the last day of the selected month or year.
        // e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
        // e.g. Switching from 2012 to 2013 when Feb 29, 2012 is selected -> Feb 28, 2013
        val day = mCurrentDate!!.get(Calendar.DAY_OF_MONTH)
        val month = mCurrentDate!!.get(Calendar.MONTH)
        val daysInMonth = getDaysInMonth(month, year)
        if (day > daysInMonth) {
            mCurrentDate!!.set(Calendar.DAY_OF_MONTH, daysInMonth)
        }
        mCurrentDate!!.set(Calendar.YEAR, year)
        onDateChanged(true, true)
        // Automatically switch to day picker.
        setCurrentView(VIEW_MONTH_DAY)
        // Switch focus back to the year text.
        mHeaderYear!!.requestFocus()
    }
    /**
     * Listener called when the user clicks on a header item.
     */
    private val mOnHeaderClickListener = OnClickListener {
        tryVibrate()
        when (it.id) {
            R.id.date_picker_header_year -> setCurrentView(VIEW_YEAR)
            R.id.date_picker_header_date -> setCurrentView(VIEW_MONTH_DAY)
        }
    }
    override val year: Int
        get() = mCurrentDate!!.get(Calendar.YEAR)
    override val month: Int
        get() = mCurrentDate!!.get(Calendar.MONTH)
    override val dayOfMonth: Int
        get() = mCurrentDate!!.get(Calendar.DAY_OF_MONTH)
    override var firstDayOfWeek: Int
        get() = if (mFirstDayOfWeek != USE_LOCALE) {
            mFirstDayOfWeek
        } else mCurrentDate!!.firstDayOfWeek
        set(firstDayOfWeek) {
            mFirstDayOfWeek = firstDayOfWeek
            mDayPickerView.setFirstDayOfWeek(firstDayOfWeek)
        }
    override var isEnabled: Boolean
        get() = mContainer.isEnabled
        set(enabled) {
            mContainer.isEnabled = enabled
            mDayPickerView.setEnabled(enabled)
            mYearPickerView.setEnabled(enabled)
            mHeaderYear!!.setEnabled(enabled)
            mHeaderMonthDay.setEnabled(enabled)
        }
    override val calendarView: CalendarView
        get() = throw UnsupportedOperationException("Not supported by calendar-mode DatePicker")
    override// No-op for compatibility with the old DatePicker.
    var calendarViewShown: Boolean
        get() = false
        set(shown) {}
    override// No-op for compatibility with the old DatePicker.
    var spinnersShown: Boolean
        get() = false
        set(shown) {}
    val accessibilityClassName: CharSequence
        get() = CherryDatePicker::class.java.getName()

    init {
        val locale = mCurrentLocale
        mCurrentDate = Calendar.getInstance(locale)
        mTempDate = Calendar.getInstance(locale)
        minDate = Calendar.getInstance(locale)
        maxDate = Calendar.getInstance(locale)
        minDate.set(DEFAULT_START_YEAR, Calendar.JANUARY, 1)
        maxDate.set(DEFAULT_END_YEAR, Calendar.DECEMBER, 31)
        val res = mDelegator.resources
        val a = mContext.obtainStyledAttributes(
            attrs,
            R.styleable.CherryDatePicker, defStyleAttr, defStyleRes
        )
        val inflater = mContext.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater
        val layoutResourceId = a.getResourceId(
            R.styleable.CherryDatePicker_internalLayout, R.layout.date_picker_material
        )
        // Set up and attach container.
        mContainer = inflater.inflate(layoutResourceId, mDelegator, false) as ViewGroup
        mContainer.isSaveFromParentEnabled = false
        mDelegator.addView(mContainer)
        // Set up header views.
        val header = mContainer.findViewById<LinearLayout>(R.id.date_picker_header)
        mHeaderYear = header.findViewById(R.id.date_picker_header_year)
        mHeaderYear!!.setOnClickListener(mOnHeaderClickListener)
        mHeaderMonthDay = header.findViewById(R.id.date_picker_header_date)
        mHeaderMonthDay.setOnClickListener(mOnHeaderClickListener)
        // For the sake of backwards compatibility, attempt to extract the text
        // color from the header month text appearance. If it's set, we'll let
        // that override the "real" header text color.
        var headerTextColor: ColorStateList? = null
        val monthHeaderTextAppearance = a.getResourceId(
            R.styleable.CherryDatePicker_headerMonthTextAppearance, 0
        )
        if (monthHeaderTextAppearance != 0) {
            val textAppearance = mContext.obtainStyledAttributes(
                null,
                ATTRS_TEXT_COLOR, 0, monthHeaderTextAppearance
            )
            val legacyHeaderTextColor = textAppearance.getColorStateList(0)
            headerTextColor = legacyHeaderTextColor // applyLegacyColorFixes(legacyHeaderTextColor)
            textAppearance.recycle()
        }
        if (headerTextColor == null) {
            headerTextColor = a.getColorStateList(R.styleable.CherryDatePicker_headerTextColor)
        }
        if (headerTextColor != null) {
            mHeaderYear!!.setTextColor(headerTextColor)
            mHeaderMonthDay.setTextColor(headerTextColor)
        }
        // Set up header background, if available.
        if (a.hasValueOrEmpty(R.styleable.CherryDatePicker_headerBackground)) {
            header.setBackground(a.getDrawable(R.styleable.CherryDatePicker_headerBackground))
        }
        a.recycle()
        // Set up picker container.
        mAnimator = mContainer.findViewById(R.id.animator)
        // Set up day picker view.
        mDayPickerView = mAnimator.findViewById(R.id.date_picker_day_picker)
        mDayPickerView.setFirstDayOfWeek(mFirstDayOfWeek)
        mDayPickerView.setMinDate(minDate.timeInMillis)
        mDayPickerView.setMaxDate(maxDate.timeInMillis)
        mDayPickerView.setDate(mCurrentDate!!.timeInMillis)
        mDayPickerView.setOnDaySelectedListener(mOnDaySelectedListener)
        // Set up year picker view.
        mYearPickerView = mAnimator.findViewById(R.id.date_picker_year_picker)
        mYearPickerView.setRange(minDate, maxDate)
        mYearPickerView.setYear(mCurrentDate!!.get(Calendar.YEAR))
        mYearPickerView.setOnYearSelectedListener(mOnYearSelectedListener)
        // Set up content descriptions.
        mSelectDay = "asd" // res.getString(R.string.select_day)
        mSelectYear = "asda" // res.getString(R.string.select_year)
        // Initialize for current locale. This also initializes the date, so no
        // need to call onDateChanged.
        onLocaleChanged(mCurrentLocale)
        setCurrentView(VIEW_MONTH_DAY)
    }

    /**
     * The legacy text color might have been poorly defined. Ensures that it
     * has an appropriate activated state, using the selected state if one
     * exists or modifying the default text color otherwise.
     *
     * @param color a legacy text color, or `null`
     * @return a color state list with an appropriate activated state, or
     * `null` if a valid activated state could not be generated
     */
//    @Nullable
//    private fun applyLegacyColorFixes(@Nullable color: ColorStateList?): ColorStateList? {
//        if (color == null || color.hasState(R.attr.state_activated)) {
//            return color
//        }
//        val activatedColor: Int
//        val defaultColor: Int
//        if (color.hasState(R.attr.state_selected)) {
//            activatedColor = color.getColorForState(
//                StateSet.get(
//                    StateSet.VIEW_STATE_ENABLED or StateSet.VI
//                ), 0
//            )
//            defaultColor = color.getColorForState(
//                StateSet.get(
//                    StateSet.VIEW_STATE_ENABLED
//                ), 0
//            )
//        } else {
//            activatedColor = color.defaultColor
//            // Generate a non-activated color using the disabled alpha.
//            val ta = mContext.obtainStyledAttributes(ATTRS_DISABLED_ALPHA)
//            val disabledAlpha = ta.getFloat(0, 0.30f)
//            defaultColor = multiplyAlphaComponent(activatedColor, disabledAlpha)
//        }
//        if (activatedColor == 0 || defaultColor == 0) {
//            // We somehow failed to obtain the colors.
//            return null
//        }
//        val stateSet = arrayOf(intArrayOf(R.attr.state_activated), intArrayOf())
//        val colors = intArrayOf(activatedColor, defaultColor)
//        return ColorStateList(stateSet, colors)
//    }

    private fun multiplyAlphaComponent(color: Int, alphaMod: Float): Int {
        val srcRgb = color and 0xFFFFFF
        val srcAlpha = color shr 24 and 0xFF
        val dstAlpha = (srcAlpha * alphaMod + 0.5f).toInt()
        return srcRgb or (dstAlpha shl 24)
    }

    override fun onLocaleChanged(locale: Locale) {
        val headerYear = mHeaderYear
            ?: // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return
// Update the date formatter.
        mMonthDayFormat = DateFormat.getInstanceForSkeleton("EMMMd", locale)
        mMonthDayFormat!!.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE)
        mYearFormat = DateFormat.getInstanceForSkeleton("y", locale)
        // Update the header text.
        onCurrentDateChanged(false)
    }

    private fun onCurrentDateChanged(announce: Boolean) {
        if (mHeaderYear == null) {
            // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return
        }
        val year = mYearFormat!!.format(mCurrentDate!!.time)
        mHeaderYear!!.setText(year)
        val monthDay = mMonthDayFormat!!.format(mCurrentDate!!.time)
        mHeaderMonthDay.setText(monthDay)
        // TODO: This should use live regions.
        if (announce) {
            mAnimator.announceForAccessibility(formattedCurrentDate)
        }
    }

    private fun setCurrentView(viewIndex: Int) {
        when (viewIndex) {
            VIEW_MONTH_DAY -> {
                mDayPickerView.setDate(mCurrentDate!!.timeInMillis)
                if (mCurrentView != viewIndex) {
                    mHeaderMonthDay.setActivated(true)
                    mHeaderYear!!.setActivated(false)
                    mAnimator.setDisplayedChild(VIEW_MONTH_DAY)
                    mCurrentView = viewIndex
                }
                mAnimator.announceForAccessibility(mSelectDay)
            }
            VIEW_YEAR -> {
                val year = mCurrentDate!!.get(Calendar.YEAR)
                mYearPickerView.setYear(year)
                mYearPickerView.post({
                    mYearPickerView.requestFocus()
                    val selected = mYearPickerView.getSelectedView()
                    if (selected != null) {
                        selected!!.requestFocus()
                    }
                })
                if (mCurrentView != viewIndex) {
                    mHeaderMonthDay.setActivated(false)
                    mHeaderYear!!.setActivated(true)
                    mAnimator.setDisplayedChild(VIEW_YEAR)
                    mCurrentView = viewIndex
                }
                mAnimator.announceForAccessibility(mSelectYear)
            }
        }
    }

    override fun init(
        year: Int, month: Int, dayOfMonth: Int,
        callBack: CherryDatePicker.OnDateChangedListener
    ) {
        setDate(year, month, dayOfMonth)
        onDateChanged(false, false)
        mOnDateChangedListener = callBack
    }

    override fun updateDate(year: Int, month: Int, dayOfMonth: Int) {
        setDate(year, month, dayOfMonth)
        onDateChanged(false, true)
    }

    private fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        mCurrentDate!!.set(Calendar.YEAR, year)
        mCurrentDate!!.set(Calendar.MONTH, month)
        mCurrentDate!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        resetAutofilledValue()
    }

    private fun onDateChanged(fromUser: Boolean, callbackToClient: Boolean) {
        val year = mCurrentDate!!.get(Calendar.YEAR)
        if (callbackToClient && (mOnDateChangedListener != null || mAutoFillChangeListener != null)) {
            val monthOfYear = mCurrentDate!!.get(Calendar.MONTH)
            val dayOfMonth = mCurrentDate!!.get(Calendar.DAY_OF_MONTH)
            if (mOnDateChangedListener != null) {
                mOnDateChangedListener.onDateChanged(mDelegator, year, monthOfYear, dayOfMonth)
            }
            if (mAutoFillChangeListener != null) {
                mAutoFillChangeListener.onDateChanged(mDelegator, year, monthOfYear, dayOfMonth)
            }
        }
        mDayPickerView.setDate(mCurrentDate!!.timeInMillis)
        mYearPickerView.setYear(year)
        onCurrentDateChanged(fromUser)
        if (fromUser) {
            tryVibrate()
        }
    }

    override fun setMinDate(minDate: Long) {
        mTempDate.timeInMillis = minDate
        if (mTempDate.get(Calendar.YEAR) == this.minDate.get(Calendar.YEAR) && mTempDate.get(Calendar.DAY_OF_YEAR) == this.minDate.get(
                Calendar.DAY_OF_YEAR
            )
        ) {
            // Same day, no-op.
            return
        }
        if (mCurrentDate!!.before(mTempDate)) {
            mCurrentDate!!.timeInMillis = minDate
            onDateChanged(false, true)
        }
        this.minDate.timeInMillis = minDate
        mDayPickerView.setMinDate(minDate)
        mYearPickerView.setRange(this.minDate, maxDate)
    }

    override fun setMaxDate(maxDate: Long) {
        mTempDate.timeInMillis = maxDate
        if (mTempDate.get(Calendar.YEAR) == this.maxDate.get(Calendar.YEAR) && mTempDate.get(Calendar.DAY_OF_YEAR) == this.maxDate.get(
                Calendar.DAY_OF_YEAR
            )
        ) {
            // Same day, no-op.
            return
        }
        if (mCurrentDate!!.after(mTempDate)) {
            mCurrentDate!!.timeInMillis = maxDate
            onDateChanged(false, true)
        }
        this.maxDate.timeInMillis = maxDate
        mDayPickerView.setMaxDate(maxDate)
        mYearPickerView.setRange(minDate, this.maxDate)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        setCurrentLocale(newConfig.locale)
    }

    override fun onSaveInstanceState(superState: Parcelable?): Parcelable {
        val year = mCurrentDate!!.get(Calendar.YEAR)
        val month = mCurrentDate!!.get(Calendar.MONTH)
        val day = mCurrentDate!!.get(Calendar.DAY_OF_MONTH)
        var listPosition = -1
        var listPositionOffset = -1
        if (mCurrentView == VIEW_MONTH_DAY) {
            listPosition = mDayPickerView.getMostVisiblePosition()
        } else if (mCurrentView == VIEW_YEAR) {
            listPosition = mYearPickerView.getFirstVisiblePosition()
            listPositionOffset = mYearPickerView.getFirstPositionOffset()
        }
        return CherryDatePicker.AbstractDatePickerDelegate.SavedState(
            superState!!, year, month, day, minDate.timeInMillis,
            maxDate.timeInMillis, mCurrentView, listPosition, listPositionOffset
        )
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is CherryDatePicker.AbstractDatePickerDelegate.SavedState) {
// TODO: Move instance state into DayPickerView, YearPickerView.
            mCurrentDate!!.set(state.selectedYear, state.selectedMonth, state.selectedDay)
            minDate.timeInMillis = state.minDate
            maxDate.timeInMillis = state.maxDate
            onCurrentDateChanged(false)
            val currentView = state.currentView
            setCurrentView(currentView)
            val listPosition = state.listPosition
            if (listPosition != -1) {
                if (currentView == VIEW_MONTH_DAY) {
                    mDayPickerView.setPosition(listPosition)
                } else if (currentView == VIEW_YEAR) {
                    val listPositionOffset = state.listPositionOffset
                    mYearPickerView.setSelectionFromTop(listPosition, listPositionOffset)
                }
            }
        }
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        onPopulateAccessibilityEvent(event)
        return true
    }

    private fun tryVibrate() {
        mDelegator.performHapticFeedback(5)//HapticFeedbackConstants.CALENDAR_DATE)
    }

    companion object {
        private val USE_LOCALE = 0
        private val UNINITIALIZED = -1
        private val VIEW_MONTH_DAY = 0
        private val VIEW_YEAR = 1
        private val DEFAULT_START_YEAR = 1900
        private val DEFAULT_END_YEAR = 2100
        private val ANIMATION_DURATION = 300
        private val ATTRS_TEXT_COLOR = intArrayOf(R.attr.textColor)
        private val ATTRS_DISABLED_ALPHA = intArrayOf(R.attr.disabledAlpha)
        fun getDaysInMonth(month: Int, year: Int): Int {
            when (month) {
                Calendar.JANUARY, Calendar.MARCH, Calendar.MAY, Calendar.JULY, Calendar.AUGUST, Calendar.OCTOBER, Calendar.DECEMBER -> return 31
                Calendar.APRIL, Calendar.JUNE, Calendar.SEPTEMBER, Calendar.NOVEMBER -> return 30
                Calendar.FEBRUARY -> return if (year % 4 == 0) 29 else 28
                else -> throw IllegalArgumentException("Invalid Month")
            }
        }
    }
}
