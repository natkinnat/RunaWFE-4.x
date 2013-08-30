package ru.runa.wfe.commons.bc;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import ru.runa.wfe.commons.PropertyResources;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class DurationParser {
    private static final PropertyResources RESOURCES = new PropertyResources("business.calendar.properties");
    private static final long BUSINESS_DAY_IN_HOURS = RESOURCES.getIntegerProperty("business.day.expressed.in.hours", 8);
    private static final long BUSINESS_WEEK_IN_HOURS = RESOURCES.getIntegerProperty("business.week.expressed.in.hours", 40);
    private static final long BUSINESS_MONTH_IN_DAYS = RESOURCES.getIntegerProperty("business.month.expressed.in.business.days", 21);
    private static final long BUSINESS_YEAR_IN_DAYS = RESOURCES.getIntegerProperty("business.year.expressed.in.business.days", 220);

    private static final Map<String, Integer> calendarFields = Maps.newHashMap();
    static {
        calendarFields.put("seconds", Calendar.SECOND);
        calendarFields.put("minutes", Calendar.MINUTE);
        calendarFields.put("hours", Calendar.HOUR);
        calendarFields.put("days", Calendar.DAY_OF_YEAR);
        calendarFields.put("weeks", Calendar.WEEK_OF_YEAR);
        calendarFields.put("months", Calendar.MONTH);
        calendarFields.put("years", Calendar.YEAR);
    }

    /**
     * Creates a duration from a textual description. Syntax: &lt;quantity&gt;
     * [business] &lt;unit&gt; <br />
     * unit is one of
     * <ul>
     * <li>seconds</li>
     * <li>minutes</li>
     * <li>hours</li>
     * <li>days</li>
     * <li>weeks</li>
     * <li>months</li>
     * <li>years</li>
     * </ul>
     */
    public static Duration parse(String durationString) {
        Preconditions.checkNotNull(durationString, "duration is null");
        durationString = durationString.trim();
        int index = indexOfNonWhite(durationString, 0);
        char lead = durationString.charAt(index);
        if (lead == '+' || lead == '-') {
            index++;
        }
        // parse quantity
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        index = indexOfNonWhite(durationString, index);
        ParsePosition position = new ParsePosition(index);
        Number quantity = format.parse(durationString, position);
        if (quantity == null) {
            throw new IllegalArgumentException("improper format of duration '" + durationString + "'");
        }
        String unitText = durationString.substring(position.getIndex()).trim();
        boolean businessTime = false;
        if (unitText.startsWith("business ")) {
            businessTime = true;
            unitText = unitText.substring("business ".length());
        }
        // parse unit
        Integer unit = calendarFields.get(unitText);
        if (unit == null) {
            throw new IllegalArgumentException("improper format of duration '" + durationString + "'");
        }
        int calendarField = unit.intValue();
        int amount = quantity.intValue();
        if (lead == '-') {
            amount = -amount;
        }
        if (businessTime) {
            if (Calendar.YEAR == calendarField) {
                amount *= BUSINESS_YEAR_IN_DAYS;
                calendarField = Calendar.DAY_OF_YEAR;
            }
            if (Calendar.MONTH == calendarField) {
                amount *= BUSINESS_MONTH_IN_DAYS;
                calendarField = Calendar.DAY_OF_YEAR;
            }
            if (Calendar.WEEK_OF_YEAR == calendarField) {
                amount *= BUSINESS_WEEK_IN_HOURS;
                calendarField = Calendar.HOUR;
            }
            if (Calendar.DAY_OF_YEAR == calendarField) {
                amount *= BUSINESS_DAY_IN_HOURS;
                calendarField = Calendar.HOUR;
            }
            if (Calendar.HOUR == calendarField) {
                amount *= 60;
                calendarField = Calendar.MINUTE;
            }
        }
        return new Duration(calendarField, amount, businessTime);
    }

    private static int indexOfNonWhite(String str, int off) {
        while (off < str.length() && Character.isWhitespace(str.charAt(off))) {
            off++;
        }
        return off;
    }

}
