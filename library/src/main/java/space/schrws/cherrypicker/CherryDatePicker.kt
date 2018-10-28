package space.schrws.cherrypicker

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.IntDef
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewStructure
import android.view.accessibility.AccessibilityEvent
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.CalendarView
import android.widget.DatePicker
import android.widget.FrameLayout
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by Schrws on 2018-10-23.
 */
class CherryDatePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = R.attr.datePickerStyle,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val mDelegate: CherryDatePickerDelegate
    val mode: Int
    /**
     * @return The selected year.
     */
    val year: Int
        get() = mDelegate.year
    /**
     * @return The selected month.
     */
    val month: Int
        get() = mDelegate.month
    /**
     * @return The selected day of month.
     */
    val dayOfMonth: Int
        get() = mDelegate.dayOfMonth
    /**
     * Gets the minimal date supported by this [CherryDatePicker] in
     * milliseconds since January 1, 1970 00:00:00 in
     * [TimeZone.getDefault] time zone.
     *
     *
     * Note: The default minimal date is 01/01/1900.
     *
     *
     *
     * @return The minimal supported date.
     */
    val minDate: Long
        get() = mDelegate.minDate.timeInMillis
    /**
     * Gets the maximal date supported by this [CherryDatePicker] in
     * milliseconds since January 1, 1970 00:00:00 in
     * [TimeZone.getDefault] time zone.
     *
     *
     * Note: The default maximal date is 12/31/2100.
     *
     *
     *
     * @return The maximal supported date.
     */
    /**
     * Sets the maximal date supported by this [CherryDatePicker] in
     * milliseconds since January 1, 1970 00:00:00 in
     * [TimeZone.getDefault] time zone.
     *
     * @param maxDate The maximal supported date.
     */
    var maxDate: Long
        get() = mDelegate.maxDate.timeInMillis
        set(maxDate) = mDelegate.setMaxDate(maxDate)
    /**
     * Gets the first day of week.
     *
     * @return The first day of the week conforming to the [CherryCalendarView]
     * APIs.
     * @see Calendar.SUNDAY
     *
     * @see Calendar.MONDAY
     *
     * @see Calendar.TUESDAY
     *
     * @see Calendar.WEDNESDAY
     *
     * @see Calendar.THURSDAY
     *
     * @see Calendar.FRIDAY
     *
     * @see Calendar.SATURDAY
     *
     *
     * @attr ref android.R.styleable#DatePicker_firstDayOfWeek
     */
    /**
     * Sets the first day of week.
     *
     * @param firstDayOfWeek The first day of the week conforming to the
     * [CherryCalendarView] APIs.
     * @see Calendar.SUNDAY
     *
     * @see Calendar.MONDAY
     *
     * @see Calendar.TUESDAY
     *
     * @see Calendar.WEDNESDAY
     *
     * @see Calendar.THURSDAY
     *
     * @see Calendar.FRIDAY
     *
     * @see Calendar.SATURDAY
     *
     *
     * @attr ref android.R.styleable#DatePicker_firstDayOfWeek
     */
    var firstDayOfWeek: Int
        get() = mDelegate.firstDayOfWeek
        set(firstDayOfWeek) {
            if (Calendar.SUNDAY <= firstDayOfWeek && firstDayOfWeek <= Calendar.SATURDAY)
                mDelegate.firstDayOfWeek = firstDayOfWeek
        }
    /**
     * Returns whether the [CherryCalendarView] is shown.
     *
     *
     * **Note:** This method returns `false` when the
     * [android.R.styleable.DatePicker_datePickerMode] attribute is set
     * to `calendar`.
     *
     * @return `true` if the calendar view is shown
     * @see .getCalendarView
     */
    /**
     * Sets whether the [CherryCalendarView] is shown.
     *
     *
     * **Note:** Calling this method has no effect when the
     * [android.R.styleable.DatePicker_datePickerMode] attribute is set
     * to `calendar`.
     *
     * @param shown `true` to show the calendar view, `false` to
     * hide it
     */
    var calendarViewShown: Boolean
        @Deprecated("Not supported by Material-style {@code calendar} mode")
        get() = mDelegate.calendarViewShown
        @Deprecated("Not supported by Material-style {@code calendar} mode")
        set(shown) {
            mDelegate.calendarViewShown = shown
        }
    /**
     * Returns the [CherryCalendarView] used by this picker.
     *
     *
     * **Note:** This method throws an
     * [UnsupportedOperationException] when the
     * [android.R.styleable.DatePicker_datePickerMode] attribute is set
     * to `calendar`.
     *
     * @return the calendar view
     * @see .getCalendarViewShown
     * @throws UnsupportedOperationException if called when the picker is
     * displayed in `calendar` mode
     */
    val calendarView: CalendarView
        @Deprecated(
            "Not supported by Material-style {@code calendar} mode\n" +
                    "      "
        )
        get() = mDelegate.calendarView

    /**
     * The callback used to indicate the user changed the date.
     */
    interface OnDateChangedListener {
        /**
         * Called upon a date change.
         *
         * @param view The view associated with this listener.
         * @param year The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         * with [java.util.Calendar].
         * @param dayOfMonth The day of the month that was set.
         */
        fun onDateChanged(view: CherryDatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int)
    }

    init {
        // DatePicker is important by default, unless app developer overrode attribute.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && importantForAutofill == View.IMPORTANT_FOR_AUTOFILL_AUTO) {
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        }
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.CherryDatePicker,
            defStyleAttr, defStyleRes
        )
        firstDayOfWeek = a.getInt(R.styleable.CherryDatePicker_firstDayOfWeek, 0)
        a.recycle()
        mode = MODE_CALENDAR
        mDelegate = createCalendarUIDelegate(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun createCalendarUIDelegate(
        context: Context, attrs: AttributeSet,
        defStyleAttr: Int, defStyleRes: Int
    ): CherryDatePickerDelegate {
        return CherryDatePickerDelegate(
            this, context, attrs, defStyleAttr, defStyleRes
        )
    }

    /**
     * Initialize the state. If the provided values designate an inconsistent
     * date the values are normalized before updating the spinners.
     *
     * @param year The initial year.
     * @param monthOfYear The initial month **starting from zero**.
     * @param dayOfMonth The initial day of the month.
     * @param onDateChangedListener How user is notified date is changed by
     * user, can be null.
     */
    fun init(
        year: Int, monthOfYear: Int, dayOfMonth: Int,
        onDateChangedListener: OnDateChangedListener
    ) {
        mDelegate.init(year, monthOfYear, dayOfMonth, onDateChangedListener)
    }

    /**
     * Set the callback that indicates the date has been adjusted by the user.
     *
     * @param onDateChangedListener How user is notified date is changed by
     * user, can be null.
     */
    fun setOnDateChangedListener(onDateChangedListener: OnDateChangedListener) {
        mDelegate.setOnDateChangedListener(onDateChangedListener)
    }

    /**
     * Update the current date.
     *
     * @param year The year.
     * @param month The month which is **starting from zero**.
     * @param dayOfMonth The day of the month.
     */
    fun updateDate(year: Int, month: Int, dayOfMonth: Int) {
        mDelegate.updateDate(year, month, dayOfMonth)
    }

    /**
     * Sets the callback that indicates the current date is valid.
     *
     * @param callback the callback, may be null
     * @hide
     */
    fun setValidationCallback(callback: ValidationCallback?) {
        mDelegate.setValidationCallback(callback)
    }

    override fun setEnabled(enabled: Boolean) {
        if (mDelegate.isEnabled == enabled) {
            return
        }
        super.setEnabled(enabled)
        mDelegate.isEnabled = enabled
    }

    override fun isEnabled(): Boolean {
        return mDelegate.isEnabled
    }

    /** @hide
     */
    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        return mDelegate.dispatchPopulateAccessibilityEvent(event)
    }

    /** @hide
     */
    override fun onPopulateAccessibilityEvent(event: AccessibilityEvent) {
        super.onPopulateAccessibilityEvent(event)
        mDelegate.onPopulateAccessibilityEvent(event)
    }

    override fun getAccessibilityClassName(): CharSequence {
        return CherryDatePicker::class.java!!.getName()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDelegate.onConfigurationChanged(newConfig)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        dispatchThawSelfOnly(container)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return mDelegate.onSaveInstanceState(superState)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as View.BaseSavedState
        super.onRestoreInstanceState(ss.superState)
        mDelegate.onRestoreInstanceState(ss)
    }

    /**
     * A delegate interface that defined the public API of the DatePicker. Allows different
     * DatePicker implementations. This would need to be implemented by the DatePicker delegates
     * for the real behavior.
     *
     * @hide
     */
    interface CherryDatePickerDelegate {
        val year: Int
        val month: Int
        val dayOfMonth: Int
        val autofillValue: AutofillValue
        var firstDayOfWeek: Int
        val minDate: Calendar
        val maxDate: Calendar
        var isEnabled: Boolean
        val calendarView: CalendarView
        var calendarViewShown: Boolean
        var spinnersShown: Boolean
        fun init(
            year: Int, monthOfYear: Int, dayOfMonth: Int,
            onDateChangedListener: OnDateChangedListener
        )

        fun setOnDateChangedListener(onDateChangedListener: OnDateChangedListener)
        fun setAutoFillChangeListener(onDateChangedListener: OnDateChangedListener)
        fun updateDate(year: Int, month: Int, dayOfMonth: Int)
        fun autofill(value: AutofillValue)
        fun setMinDate(minDate: Long)
        fun setMaxDate(maxDate: Long)
        fun setValidationCallback(callback: ValidationCallback?)
        fun onConfigurationChanged(newConfig: Configuration)
        fun onSaveInstanceState(superState: Parcelable?): Parcelable
        fun onRestoreInstanceState(state: Parcelable)
        fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean
        fun onPopulateAccessibilityEvent(event: AccessibilityEvent)
    }

    /**
     * An abstract class which can be used as a start for DatePicker implementations
     */
    abstract class AbstractDatePickerDelegate(// The delegator
        protected var mDelegator: CherryDatePicker, // The context
        protected var mContext: Context
    ) : CherryDatePickerDelegate {
        // NOTE: when subclasses change this variable, they must call resetAutofilledValue().
        protected var mCurrentDate: Calendar? = null
        // The current locale
        protected lateinit var mCurrentLocale: Locale
        // Callbacks
        protected lateinit var mOnDateChangedListener: OnDateChangedListener
        protected lateinit var mAutoFillChangeListener: OnDateChangedListener
        protected var mValidationCallback: ValidationCallback? = null
        // The value that was passed to autofill() - it must be stored because it getAutofillValue()
        // must return the exact same value that was autofilled, otherwise the widget will not be
        // properly highlighted after autofill().
        private var mAutofilledValue: Long = 0
        override val autofillValue: AutofillValue
            get() {
                val time = if (mAutofilledValue != 0L)
                    mAutofilledValue
                else
                    mCurrentDate!!.timeInMillis
                return AutofillValue.forDate(time)
            }
        protected val formattedCurrentDate: String
            get() = DateUtils.formatDateTime(
                mContext, mCurrentDate!!.timeInMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                        or DateUtils.FORMAT_SHOW_WEEKDAY
            )

        init {
            setCurrentLocale(Locale.getDefault())
        }

        protected fun setCurrentLocale(locale: Locale) {
            mCurrentLocale = locale
            onLocaleChanged(locale)
        }

        override fun setOnDateChangedListener(callback: OnDateChangedListener) {
            mOnDateChangedListener = callback
        }

        override fun setAutoFillChangeListener(callback: OnDateChangedListener) {
            mAutoFillChangeListener = callback
        }

        override fun setValidationCallback(callback: ValidationCallback?) {
            mValidationCallback = callback
        }

        override fun autofill(value: AutofillValue) {
            if (value == null || !value.isDate) {
                Log.w(LOG_TAG, value.toString() + " could not be autofilled into " + this)
                return
            }
            val time = value.dateValue
            val cal = Calendar.getInstance(mCurrentLocale)
            cal.timeInMillis = time
            updateDate(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            // Must set mAutofilledValue *after* calling subclass method to make sure the value
            // returned by getAutofillValue() matches it.
            mAutofilledValue = time
        }

        /**
         * This method must be called every time the value of the year, month, and/or day is
         * changed by a subclass method.
         */
        protected fun resetAutofilledValue() {
            mAutofilledValue = 0
        }

        protected fun onValidationChanged(valid: Boolean) {
            if (mValidationCallback != null) {
                mValidationCallback!!.onValidationChanged(valid)
            }
        }

        open fun onLocaleChanged(locale: Locale) {
            // Stub.
        }

        override fun onPopulateAccessibilityEvent(event: AccessibilityEvent) {
            event.text.add(formattedCurrentDate)
        }

        /**
         * Class for managing state storing/restoring.
         */
        internal class SavedState : View.BaseSavedState {
            val selectedYear: Int
            val selectedMonth: Int
            val selectedDay: Int
            val minDate: Long
            val maxDate: Long
            val currentView: Int
            val listPosition: Int
            val listPositionOffset: Int

            /**
             * Constructor called from [CherryDatePicker.onSaveInstanceState]
             */
            @JvmOverloads constructor(
                superState: Parcelable, year: Int, month: Int, day: Int, minDate: Long,
                maxDate: Long, currentView: Int = 0, listPosition: Int = 0, listPositionOffset: Int = 0
            ) : super(superState) {
                selectedYear = year
                selectedMonth = month
                selectedDay = day
                this.minDate = minDate
                this.maxDate = maxDate
                this.currentView = currentView
                this.listPosition = listPosition
                this.listPositionOffset = listPositionOffset
            }

            /**
             * Constructor called from [.CREATOR]
             */
            private constructor(`in`: Parcel) : super(`in`) {
                selectedYear = `in`.readInt()
                selectedMonth = `in`.readInt()
                selectedDay = `in`.readInt()
                minDate = `in`.readLong()
                maxDate = `in`.readLong()
                currentView = `in`.readInt()
                listPosition = `in`.readInt()
                listPositionOffset = `in`.readInt()
            }

            override fun writeToParcel(dest: Parcel, flags: Int) {
                super.writeToParcel(dest, flags)
                dest.writeInt(selectedYear)
                dest.writeInt(selectedMonth)
                dest.writeInt(selectedDay)
                dest.writeLong(minDate)
                dest.writeLong(maxDate)
                dest.writeInt(currentView)
                dest.writeInt(listPosition)
                dest.writeInt(listPositionOffset)
            }

            companion object {
                @SuppressWarnings("all")
                val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                    override fun createFromParcel(`in`: Parcel): SavedState {
                        return SavedState(`in`)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)
                    }
                }
            }
        }
    }

    /**
     * A callback interface for updating input validity when the date picker
     * when included into a dialog.
     *
     * @hide
     */
    interface ValidationCallback {
        fun onValidationChanged(valid: Boolean)
    }

    override fun dispatchProvideAutofillStructure(structure: ViewStructure, flags: Int) {
        // This view is self-sufficient for autofill, so it needs to call
        // onProvideAutoFillStructure() to fill itself, but it does not need to call
        // dispatchProvideAutoFillStructure() to fill its children.
        structure.autofillId = autofillId
        onProvideAutofillStructure(structure, flags)
    }

    override fun autofill(value: AutofillValue) {
        if (!isEnabled) return
        mDelegate.autofill(value)
    }

    override fun getAutofillType(): Int {
        return if (isEnabled) View.AUTOFILL_TYPE_DATE else View.AUTOFILL_TYPE_NONE
    }

    override fun getAutofillValue(): AutofillValue? {
        return if (isEnabled) mDelegate.autofillValue else null
    }

    companion object {
        private val LOG_TAG = CherryDatePicker::class.java.simpleName
        /**
         * Presentation mode for the Holo-style date picker that uses a set of
         * [android.widget.NumberPicker]s.
         *
         * @see .getMode
         * @hide Visible for testing only.
         */
        val MODE_SPINNER = 1
        /**
         * Presentation mode for the Material-style date picker that uses a
         * calendar.
         *
         * @see .getMode
         * @hide Visible for testing only.
         */
        val MODE_CALENDAR = 2
    }
}
