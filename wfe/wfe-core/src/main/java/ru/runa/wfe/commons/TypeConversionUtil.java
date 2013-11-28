/*
 * This file is part of the RUNA WFE project.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation; version 2.1 
 * of the License. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wfe.commons;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.user.Group;
import ru.runa.wfe.user.IExecutorLoader;

import com.google.common.base.Defaults;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.primitives.Primitives;

@SuppressWarnings("unchecked")
public class TypeConversionUtil {

    public static <T> T convertTo(Class<T> classConvertTo, Object object) {
        return convertTo(classConvertTo, object, null, null);
    }

    public static <T> T convertTo(Class<T> classConvertTo, Object object, ITypeConvertor preConvertor, ITypeConvertor postConvertor) {
        try {
            Preconditions.checkNotNull(classConvertTo, "classConvertTo is null");
            if (preConvertor != null) {
                T result = preConvertor.convertTo(object, classConvertTo);
                if (result != null) {
                    return result;
                }
            }
            if (object == null) {
                return Defaults.defaultValue(classConvertTo);
            }
            if (classConvertTo.isPrimitive()) {
                classConvertTo = Primitives.wrap(classConvertTo);
            }
            if (classConvertTo.isInstance(object)) {
                return classConvertTo.cast(object);
            }
            if (object instanceof Actor) {
                // compatibility: client code expecting 'actorCode'
                Long actorCode = ((Actor) object).getCode();
                return convertTo(classConvertTo, actorCode, preConvertor, postConvertor);
            }
            if (object instanceof Group) {
                // compatibility: client code expecting 'groupCode'
                String groupCode = "G" + ((Group) object).getId();
                return convertTo(classConvertTo, groupCode, preConvertor, postConvertor);
            }
            if (classConvertTo == String.class) {
                if (object instanceof Date) {
                    return (T) CalendarUtil.formatDateTime((Date) object);
                }
                if (object instanceof Calendar) {
                    return (T) CalendarUtil.formatDateTime((Calendar) object);
                }
                return (T) object.toString();
            }
            if (object instanceof String) {
                String s = (String) object;
                if (s.length() == 0) {
                    // ??? treat as null
                    // return Defaults.defaultValue(classConvertTo);
                }
                // try to use 'valueOf(String)'
                try {
                    Method valueOfMethod = classConvertTo.getMethod("valueOf", String.class);
                    return (T) valueOfMethod.invoke(null, object);
                } catch (NoSuchMethodException e) {
                }
            }
            if (object instanceof Number && Number.class.isAssignableFrom(classConvertTo)) {
                Number n = (Number) object;
                if (classConvertTo == Long.class) {
                    return (T) new Long(n.longValue());
                }
                if (classConvertTo == Integer.class) {
                    return (T) new Integer(n.intValue());
                }
                if (classConvertTo == Byte.class) {
                    return (T) new Byte(n.byteValue());
                }
                if (classConvertTo == Double.class) {
                    return (T) new Double(n.doubleValue());
                }
                if (classConvertTo == Float.class) {
                    return (T) new Float(n.floatValue());
                }
            }
            if (classConvertTo.isArray()) {
                List<?> list = convertTo(List.class, object, preConvertor, postConvertor);
                Class<?> componentType = classConvertTo.getComponentType();
                Object array = Array.newInstance(componentType, list.size());
                for (int i = 0; i < list.size(); i++) {
                    Array.set(array, i, convertTo(componentType, list.get(i), preConvertor, postConvertor));
                }
                return (T) array;
            }
            if (List.class.isAssignableFrom(classConvertTo)) {
                List<Object> result = new ArrayList<Object>();
                if (object.getClass().isArray()) {
                    int len = Array.getLength(object);
                    for (int i = 0; i < len; i++) {
                        result.add(Array.get(object, i));
                    }
                } else if (object instanceof Collection<?>) {
                    result.addAll((Collection<?>) object);
                } else {
                    result.add(object);
                }
                return (T) result;
            }
            if (object instanceof Date && classConvertTo == Calendar.class) {
                return (T) CalendarUtil.dateToCalendar((Date) object);
            }
            if (object instanceof Calendar && classConvertTo == Date.class) {
                return (T) ((Calendar) object).getTime();
            }
            if (object instanceof String && (classConvertTo == Calendar.class || classConvertTo == Date.class)) {
                Date date;
                String formattedDate = (String) object;
                try {
                    date = CalendarUtil.convertToDate(formattedDate, CalendarUtil.DATE_WITH_HOUR_MINUTES_SECONDS_FORMAT);
                } catch (Exception e1) {
                    try {
                        date = CalendarUtil.convertToDate(formattedDate, CalendarUtil.DATE_WITH_HOUR_MINUTES_FORMAT);
                    } catch (Exception e2) {
                        try {
                            date = CalendarUtil.convertToDate(formattedDate, CalendarUtil.DATE_WITHOUT_TIME_FORMAT);
                        } catch (Exception e3) {
                            try {
                                date = CalendarUtil.convertToDate(formattedDate, CalendarUtil.HOURS_MINUTES_SECONDS_FORMAT);
                            } catch (Exception e4) {
                                try {
                                    date = CalendarUtil.convertToDate(formattedDate, CalendarUtil.HOURS_MINUTES_FORMAT);
                                } catch (Exception e5) {
                                    throw new InternalApplicationException("Unable to find datetime format for '" + formattedDate + "'");
                                }
                            }
                        }
                    }
                }
                if (classConvertTo == Calendar.class) {
                    return (T) CalendarUtil.dateToCalendar(date);
                }
                return (T) date;
            }
            if (Executor.class.isAssignableFrom(classConvertTo)) {
                return (T) convertToExecutor(object, ApplicationContextFactory.getExecutorDAO());
            }
            if (postConvertor != null) {
                T result = postConvertor.convertTo(object, classConvertTo);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        throw new InternalApplicationException("No conversion found between '" + object.getClass() + "' and '" + classConvertTo + "'");
    }

    public static int getArraySize(Object value) {
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        } else if (value instanceof List) {
            return ((List<?>) value).size();
        } else {
            throw new RuntimeException("Unsupported array type " + value.getClass());
        }
    }

    public static Object getArrayVariable(Object value, int index) {
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            if (array.length > index) {
                return array[index];
            } else {
                throw new RuntimeException("Array has insufficient length, index = " + index);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.size() > index) {
                return list.get(index);
            } else {
                throw new RuntimeException("List has insufficient size, index = " + index);
            }
        } else {
            throw new RuntimeException("Unsupported array type " + value.getClass());
        }
    }

    public static <T extends Executor> T convertToExecutor(Object object, IExecutorLoader executorLoader) {
        if (object == null || object instanceof Executor) {
            return (T) object;
        }
        try {
            String s = object.toString();
            if (s.length() == 0) {
                return null;
            }
            if (s.startsWith("ID")) {
                Long executorId = convertTo(Long.class, s.substring(2));
                return (T) executorLoader.getExecutor(executorId);
            } else if (s.startsWith("G")) {
                Long executorId = Long.parseLong(s.substring(1));
                return (T) executorLoader.getExecutor(executorId);
            } else {
                Long actorCode = Long.parseLong(s);
                return (T) executorLoader.getActorByCode(actorCode);
            }
        } catch (NumberFormatException nfe) {
            String executorIdentity = object.toString();
            return (T) executorLoader.getExecutor(executorIdentity);
        }
    }

    public static Map<String, String> toStringMap(Map<? extends Object, ? extends Object> map) {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
            result.put(convertTo(String.class, entry.getKey()), convertTo(String.class, entry.getValue()));
        }
        return result;
    }
}