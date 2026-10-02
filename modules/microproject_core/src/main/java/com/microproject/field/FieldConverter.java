/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.field;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.datatype.Money;
import com.microproject.datatype.Work;
import com.microproject.options.EditOption;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;
/**
 * Converts field values while applying ProjectLibre specific types and validation.
 */
public class FieldConverter  {
	private static final Logger logger = Logger.getLogger(FieldConverter.class.getName());
	HashMap<FieldContext,HashMap<Class<?>,ValueConverter>> contextMaps = HashMap.newHashMap(1);
	private final HashMap<Class<?>, ValueConverter> converters = HashMap.newHashMap(8);
	private StringConverter stringConverter;
	private StringConverter compactStringConverter;
	
	public static String toString(Object value, Class<?> clazz, FieldContext context ) {
		return getInstance()._toString(value,clazz,context);
	}
	public static String toString(Object value ) {
		return getInstance()._toString(value,value.getClass(),null);
	}
	public static Object fromString(String value, Class<?> clazz) {
		return getInstance().convertValue(value, clazz);
	}

	
	/**
	 * Convert from an object, usually a string, into another object
	 * @param value.  Convert from this value
	 * @param clazz. Convert to this clazz type.
	 * @param context Converter context to use 
	 * @return object of type clazz.
	 * @throws FieldParseException
	 */
	public static Object convert(Object value, Class<?> clazz, FieldContext context) throws FieldParseException {
		return getInstance()._convert(value,clazz,context);
	}
        
	
	private static FieldConverter instance = null;
	public static FieldConverter getInstance() {
		if (instance == null)
			instance = new FieldConverter();
		return instance;
	}
	public static void reinitialize() {
		instance = null;
	}

	/**
	 * 
	 * @param value.  Convert from this value
	 * @param clazz. Convert to this clazz type.
	 * @return object of type clazz.
	 * @throws FieldParseException
	 */
	private Object _convert(Object value, Class<?> clazz, FieldContext context) throws FieldParseException {
		try {
			if (value instanceof String string) {
				ValueConverter contextConverter = null;
				HashMap<Class<?>, ValueConverter> contextMap = contextMaps.get(context);
				if (contextMap != null)
					contextConverter = contextMap.get(clazz);
				Object result;
				if (contextConverter != null)
					result = contextConverter.convert(clazz, value);
				else {
					if (context != null)
						logger.fine("no context converter found");
					result = convertValue(string, clazz);
				}
	//			if (result instanceof java.util.Date) { //  dates need to be normalized
	//				result = new Date(DateTime.gmt((Date) result));
	//			}
				if (result == null) {
					throw new FieldParseException("Invalid type");
				}
				return result;
			}	
	
			// Object-to-object conversions use the same typed registry as text conversion.
			return convertValue(value, clazz);
		} catch (ConversionException conversionException) {
			throw new FieldParseException(conversionException);
		}
	}

	private Object convertValue(Object value, Class<?> clazz) {
		if (clazz == null)
			throw new ConversionException("Target type must not be null");
		ValueConverter converter = converters.get(clazz);
		if (converter != null)
			return converter.convert(clazz, value);
		if (value == null)
			return defaultScalarValue(clazz);
		if (clazz.isInstance(value))
			return value;
		if (clazz.isEnum() && value instanceof String text) {
			try {
				@SuppressWarnings({"rawtypes", "unchecked"})
				Object enumValue = Enum.valueOf((Class<? extends Enum>) clazz, text);
				return enumValue;
			} catch (IllegalArgumentException exception) {
				throw new ConversionException(exception);
			}
		}
		PropertyEditor editor = PropertyEditorManager.findEditor(clazz);
		if (editor != null) {
			try {
				editor.setAsText(value.toString());
				return editor.getValue();
			} catch (IllegalArgumentException exception) {
				throw new ConversionException(exception);
			}
		}
		// Preserve ConvertUtils' legacy fallback for unregistered target types.
		return stringConverter.convert(clazz, value);
	}

	private static Object defaultScalarValue(Class<?> type) {
		if (type == Boolean.TYPE || type == Boolean.class)
			return Boolean.FALSE;
		if (type == Byte.TYPE || type == Byte.class)
			return Byte.valueOf((byte) 0);
		if (type == Short.TYPE || type == Short.class)
			return Short.valueOf((short) 0);
		if (type == Integer.TYPE || type == Integer.class)
			return Integer.valueOf(0);
		if (type == Float.TYPE || type == Float.class)
			return Float.valueOf(0.0f);
		return null;
	}
        
	
	
	private String _toString(Object value, Class<?> clazz, FieldContext context) {
		if (context == COMPACT_CONVERTER_CONTEXT)
			return (String) compactStringConverter.convert(clazz, value);
		else
			return (String) stringConverter.convert(clazz, value);
	}
	

	public static final FieldContext COMPACT_CONVERTER_CONTEXT=new FieldContext();
	static {
		COMPACT_CONVERTER_CONTEXT.setCompact(true);
	}
	
	
	private FieldConverter() {
		instance = this;
		stringConverter = new StringConverter(false);
		compactStringConverter = new StringConverter(true);
		converters.put(String.class, stringConverter);
		converters.put(Date.class, new DateConverter());
		converters.put(GregorianCalendar.class, new CalendarConverter());
		converters.put(Duration.class, new DurationConverter());
		converters.put(Work.class, new WorkConverter());
		converters.put(Money.class, new MoneyConverter());
		ValueConverter booleanConverter = new BooleanConverter();
		converters.put(Boolean.TYPE, booleanConverter);
		converters.put(Boolean.class, booleanConverter);
		ValueConverter longConverter = new LongConverter();
		converters.put(Long.TYPE, longConverter);
		converters.put(Long.class, longConverter);
		ValueConverter doubleConverter = new DoubleConverter();
		converters.put(Double.TYPE, doubleConverter);
		converters.put(Double.class, doubleConverter);
		

		// short context converters
		HashMap<Class<?>, ValueConverter> compactMap = HashMap.newHashMap(1);
		contextMaps.put(COMPACT_CONVERTER_CONTEXT, compactMap);
		compactMap.put(String.class,compactStringConverter);
		// no need for duration or money as parsing is done in long form
		
	}
	private interface ValueConverter {
		Object convert(Class<?> type, Object value);
	}

	private static class ConversionException extends IllegalArgumentException {
		private static final long serialVersionUID = 1L;
		ConversionException(String message) { super(message); }
		ConversionException(Throwable cause) { super(cause); }
	}

	private static class StringConverter implements ValueConverter {
		private boolean compact = false;
		StringConverter(boolean compact) {
			this.compact = compact;
		}
		public Object convert(Class clazz, Object value) {
			if (value instanceof Work work) {
				if (compact) 
					return ((DurationFormat)DurationFormat.getWorkInstance()).formatCompact(work);
				else 
					return ((DurationFormat)DurationFormat.getWorkInstance()).format(work);
			} else if (value instanceof Duration duration) {
				if (compact) 
					return ((DurationFormat)DurationFormat.getInstance()).formatCompact(duration);
				else 
					return ((DurationFormat)DurationFormat.getInstance()).format(duration);
			} else if (value instanceof Money money) {
				return Money.formatCurrency(money.doubleValue(),compact);
			} else if (value instanceof Date date) {
				if (value.equals(DateTime.getZeroDate()))
					return null;
				return EditOption.getInstance().getDateFormat().format(date);
			} else {
				if (value == null)
					return null;
				else
					return value.toString();
			}
		}
	}
	// make a converter for long that can process dates and durations
	private static class LongConverter implements ValueConverter {
		public Object convert(Class<?> type, Object value) throws ConversionException {
			if (value == null)
				return null;
			if (value instanceof Date date) {
				return Long.valueOf(date.getTime());
			} else if (value instanceof GregorianCalendar calendar) {
				return Long.valueOf(calendar.getTimeInMillis());
			} else if (value instanceof Duration duration) {
				return Long.valueOf(duration.getEncodedMillis());
			}
			try {
				return Long.valueOf(value.toString().trim());
			} catch (NumberFormatException exception) {
				throw new ConversionException(exception);
			}
		}
	};
	
	private static class BooleanConverter implements ValueConverter {
		public Object convert(Class<?> type, Object value) {
			if (value == null)
				return Boolean.FALSE;
			if (value instanceof Boolean booleanValue)
				return booleanValue;
			String normalized = value.toString().trim().toLowerCase(Locale.ROOT);
			return switch (normalized) {
			case "true", "yes", "y", "on", "1" -> Boolean.TRUE;
			case "false", "no", "n", "off", "0" -> Boolean.FALSE;
			default -> Boolean.FALSE;
			};
		}
	};

	private static class DateConverter implements ValueConverter {
		public Object convert(Class<?> type, Object value) throws ConversionException {
			if (value == null)
				return null;
			if (value instanceof Long longValueObject) {
				long longValue = longValueObject.longValue();
				if (longValue == 0)
					return null;
				return new Date(longValue);
			} else if (value instanceof Date date) {
				return date;
			} else if (value instanceof Calendar calendar) {
				return calendar.getTime();
			} else if (value instanceof String dateText) {
				try {
					return EditOption.getInstance().getDateFormat().parse(dateText);
				} catch (ParseException e) {
					try {
						return DateTime.utcShortDateFormatInstance().parse(dateText); // try without time
					} catch (ParseException e1) {
						throw new ConversionException(Messages.getString("Message.invalidDate"));
					}
				}
			}

			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	};		
		
	// GregorianCalendar converter
	private static class CalendarConverter implements ValueConverter {
		private static DateConverter dateConverter = new DateConverter();
		public Object convert(Class<?> type, Object value) throws ConversionException {
			GregorianCalendar cal = DateTime.calendarInstance();
			if (value == null) {
				return null;
			} else if (value instanceof Long longValueObject) {
				long longValue = longValueObject.longValue();
				if (longValue == 0)
					return null;
		
				cal.setTimeInMillis(longValue);
				return cal;
			} else if (value instanceof Date date) {
				cal.setTime(date);
				return cal;
			} else if (value instanceof String dateText) {
				Date d = (Date) dateConverter.convert(Date.class,dateText);
				cal.setTime(d);
				return cal;
			}
			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	};		
	private static class DurationConverter implements ValueConverter {
			public Object convert(Class<?> type, Object value) throws ConversionException {
			if (value == null)
				return Duration.getInstanceFromDouble(null);
			
			if (value instanceof Number number) {
				return new Duration(number.longValue());
			} else if (value instanceof Work work) {
				return new Duration(work.longValue());
			} else if (value instanceof Duration duration) {
				return duration;
			} else if (value instanceof String durationText) {
				try {
					return DurationFormat.getInstance().parseObject(durationText);
				} catch (ParseException e) {
					throw new ConversionException(Messages.getString("Message.invalidDuration"));
				}
			}
			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	};		

	private static class WorkConverter implements ValueConverter {
		public Object convert(Class<?> type, Object value) throws ConversionException {
			if (value == null)
				return Duration.getInstanceFromDouble(null);
			
			if (value instanceof Number number) {
				return new Work(number.longValue());
			} else if (value instanceof Work work) {
				return new Work(work.longValue());
			} else if (value instanceof Duration duration) {
				return duration;
			} else if (value instanceof String workText) {
				try {
					return DurationFormat.getWorkInstance().parseObject(workText);
				} catch (ParseException e) {
					throw new ConversionException(Messages.getString("Message.invalidDuration"));
				}
			}
			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	};		
	private static class DoubleConverter implements ValueConverter {
		public Object convert(Class<?> type, Object value) throws ConversionException {
			if (value != null) {
				if (value instanceof Double doubleValue) {
					return doubleValue;
				} else if (value instanceof Money money) {
					double num = money.doubleValue();
				 	if (Double.isInfinite(num) || Double.isNaN(num)) {
				 		logger.log(Level.WARNING, "Error: number is invalid double in MoneyConverter {0}", value);
				 		num = 0.0;
				 	}
					return Double.valueOf(num);
				}
			}
			if (value == null)
				return Double.valueOf(0.0D);
			try {
				return Double.valueOf(value.toString().trim());
			} catch (NumberFormatException exception) {
				throw new ConversionException(exception);
			}
		}
	};

	/* TODO I have also experimented with the JADE library's Money class.  It is probably more useful
	 * for performing currency conversions than as a datatype.  A possible source for currency exchange rates is the 
	 * web service here: 
	 * http://www.bindingpoint.com/service.aspx?skey=377e6659-061f-4956-8edb-19b5023bc33b
	 *  
	 */
	private static class MoneyConverter implements ValueConverter {
		public Object convert(Class<?> type, Object value) throws ConversionException {
			if (value == null)
				return Money.getInstance(0);
			if (value instanceof Money money) {
				return money;
			} else if (value instanceof Number number) {
				double num = number.doubleValue();
			 	if (Double.isInfinite(num) || Double.isNaN(num)) {
			 		logger.log(Level.WARNING, "Error: number is invalid double in MoneyConverter {0}", value);
			 		num = 0.0;
			 	}
				return Money.getInstance(num);
			} else if (value instanceof String moneyText) {
				try {
					return Money.getFormat(false).parseObject(moneyText);
				} catch (ParseException e) {
					throw new ConversionException(Messages.getString("Message.invalidDuration"));
				}
			}
			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	}
}
