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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;

import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.datatype.Money;
import com.microproject.datatype.Work;
import com.microproject.options.EditOption;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;
/**
 * This class decorates ConvertUtils to use ProjectLibre specific types and validation
 */
public class FieldConverter  {
	private static final Logger logger = Logger.getLogger(FieldConverter.class.getName());
	HashMap<FieldContext,HashMap<Class,Converter>> contextMaps = new HashMap<FieldContext, HashMap<Class,Converter>>();
	private StringConverter stringConverter;
	private StringConverter compactStringConverter;
	
	public static String toString(Object value, Class clazz, FieldContext context ) {
		return getInstance()._toString(value,clazz,context);
	}
	public static String toString(Object value ) {
		return getInstance()._toString(value,value.getClass(),null);
	}
	public static Object fromString(String value, Class clazz) {
		return ConvertUtils.convert(value, clazz);
	}

	
	/**
	 * Convert from an object, usually a string, into another object
	 * @param value.  Convert from this value
	 * @param clazz. Convert to this clazz type.
	 * @param context Converter context to use 
	 * @return object of type clazz.
	 * @throws FieldParseException
	 */
	public static Object convert(Object value, Class clazz, FieldContext context) throws FieldParseException {
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
	private Object _convert(Object value, Class clazz, FieldContext context) throws FieldParseException {
		try {
			if (value instanceof String) { 
				Object result = null;
				if (context == null)
					result = ConvertUtils.convert((String) value,clazz);
				else {
					Converter contextConverter = null;
					HashMap<Class, Converter> contextMap = contextMaps.get(context);
					if (contextMap != null)
						contextConverter = contextMap.get(clazz);
					if (contextConverter != null) {
						contextConverter.convert(clazz,value);
					} else {
						logger.fine("no context converter found");
						result = ConvertUtils.convert((String) value,clazz);
					}
				}
	//			if (result instanceof java.util.Date) { //  dates need to be normalized
	//				result = new Date(DateTime.gmt((Date) result));
	//			}
				if (result == null) {
					throw new FieldParseException("Invalid type");
				}
				return result;
			}	
	
			// Because of stupidity of beanutils which assumes type string, I implement this by hand
			Converter converter = ConvertUtils.lookup(clazz);                       
			if (converter == null) {                         
				logger.log(Level.WARNING, "converter is null for class {0} instance {1} resetting", new Object[] {clazz, instance.hashCode()});
				instance = new FieldConverter();
				converter = ConvertUtils.lookup(String.class);  
			} 
			return converter.convert(clazz, value);
		} catch (ConversionException conversionException) {
			throw new FieldParseException(conversionException);
		}
	}
        
	
	
	private String _toString(Object value, Class clazz, FieldContext context) {
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
		ConvertUtils.register(stringConverter, String.class);   // Wrapper class
		ConvertUtils.register(new DateConverter(), Date.class);   // Wrapper class
		ConvertUtils.register(new CalendarConverter(), GregorianCalendar.class);   // Wrapper class
		ConvertUtils.register(new DurationConverter(), Duration.class);   // Wrapper class
		ConvertUtils.register(new WorkConverter(), Work.class);   // Wrapper class
		ConvertUtils.register(new MoneyConverter(), Money.class);   // Wrapper class
		Converter longConverter = new LongConverter();
		ConvertUtils.register(longConverter, Long.TYPE);    // Native type
		ConvertUtils.register(longConverter, Long.class);   // Wrapper class
		Converter doubleConverter = new DoubleConverter();
		ConvertUtils.register(doubleConverter, Double.TYPE);    // Native type
		ConvertUtils.register(doubleConverter, Double.class);   // Wrapper class
		

		// short context converters
		HashMap<Class, Converter> compactMap = new HashMap<Class, Converter>();
		contextMaps.put(COMPACT_CONVERTER_CONTEXT, compactMap);
		compactMap.put(String.class,compactStringConverter);
		// no need for duration or money as parsing is done in long form
		
	}
	private static class StringConverter implements Converter {
		private boolean compact = false;
		StringConverter(boolean compact) {
			this.compact = compact;
		}
		public Object convert(Class clazz, Object value) {
			if (value instanceof Work) {
				if (compact) 
					return ((DurationFormat)DurationFormat.getWorkInstance()).formatCompact(value);
				else 
					return ((DurationFormat)DurationFormat.getWorkInstance()).format(value);
			} else if (value instanceof Duration) {
				if (compact) 
					return ((DurationFormat)DurationFormat.getInstance()).formatCompact(value);
				else 
					return ((DurationFormat)DurationFormat.getInstance()).format(value);
			} else if (value instanceof Money) {
				return Money.formatCurrency(((Money)value).doubleValue(),compact);
			} else if (value instanceof Date) {
				if (value.equals(DateTime.getZeroDate()))
					return null;
				return EditOption.getInstance().getDateFormat().format(value);
			} else {
				if (value == null)
					return null;
				else
					return value.toString();
			}
		}
	}
	// make a converter for long that can process dates and durations
	private static class LongConverter implements Converter {
		Converter baseConverter = new org.apache.commons.beanutils.converters.LongConverter(); 
		public Object convert(Class type, Object value) throws ConversionException {
			if (value == null)
				return null;
			if (value != null) {
				if (value instanceof Date) {
					return Long.valueOf(((Date)value).getTime());
				} else if (value instanceof GregorianCalendar) {
					return Long.valueOf(((GregorianCalendar)value).getTimeInMillis());
				} else if (value instanceof Duration || value instanceof Work) {
					return Long.valueOf(((Duration)value).getEncodedMillis());
				}
			}
			return baseConverter.convert(type,value);
		}
	};
	
	private static class DateConverter implements Converter {
		public Object convert(Class type, Object value) throws ConversionException {
			if (value == null)
				return null;
			if (value instanceof Long) {
				long longValue =  ((Long)value).longValue();
				if (longValue == 0)
					return null;
				return new Date(longValue);
			} else if (value instanceof Date) {
				return value;
			} else if (value instanceof Calendar) {
				return ((Calendar)value).getTime();
			} else if (value instanceof String) {
				try {
					return EditOption.getInstance().getDateFormat().parse((String)value);
				} catch (ParseException e) {
					try {
						return DateTime.utcShortDateFormatInstance().parse((String)value); // try without time
					} catch (ParseException e1) {
						throw new ConversionException(Messages.getString("Message.invalidDate"));
					}
				}
			}

			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	};		
		
	// GregorianCalendar converter
	private static class CalendarConverter implements Converter {
		private static DateConverter dateConverter = new DateConverter();
		public Object convert(Class type, Object value) throws ConversionException {
			GregorianCalendar cal = DateTime.calendarInstance();
			if (value == null) {
				return null;
			} else if (value instanceof Long) {
				long longValue =  ((Long)value).longValue();
				if (longValue == 0)
					return null;
		
				cal.setTimeInMillis(longValue);
				return cal;
			} else if (value instanceof Date) {
				cal.setTime((Date)value);
				return cal;
			} else if (value instanceof String) {
				Date d = (Date) dateConverter.convert(Date.class,value);
				cal.setTime(d);
				return cal;
			}
			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	};		
	private static class DurationConverter implements Converter {
		public Object convert(Class type, Object value) throws ConversionException {
			if (value == null)
				return Duration.getInstanceFromDouble(null);
			
			if (value instanceof Number) {
				return new Duration(((Number)value).longValue());
			} else if (value instanceof Work) {
				return new Duration(((Work)value).longValue());
			} else if (value instanceof Duration) {
				return value;
			} else if (value instanceof String) {
				try {
					return DurationFormat.getInstance().parseObject((String) value);
				} catch (ParseException e) {
					throw new ConversionException(Messages.getString("Message.invalidDuration"));
				}
			}
			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	};		

	private static class WorkConverter implements Converter {
		public Object convert(Class type, Object value) throws ConversionException {
			if (value == null)
				return Duration.getInstanceFromDouble(null);
			
			if (value instanceof Number) {
				return new Work(((Number)value).longValue());
			} else if (value instanceof Work) {
				return new Work(((Work)value).longValue());
			} else if (value instanceof Duration) {
				return value;
			} else if (value instanceof String) {
				try {
					return DurationFormat.getWorkInstance().parseObject((String) value);
				} catch (ParseException e) {
					throw new ConversionException(Messages.getString("Message.invalidDuration"));
				}
			}
			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	};		
	private static class DoubleConverter implements Converter {
		Converter baseConverter = new org.apache.commons.beanutils.converters.DoubleConverter(); 
		public Object convert(Class type, Object value) throws ConversionException {
			if (value != null) {
				if (value instanceof Double) {
					return value;
				} else if (value instanceof Money) {
					double num = ((Number)value).doubleValue();
				 	if (Double.isInfinite(num) || Double.isNaN(num)) {
				 		logger.log(Level.WARNING, "Error: number is invalid double in MoneyConverter {0}", value);
				 		num = 0.0;
				 	}
					return Double.valueOf(num);
				}
			}
			return baseConverter.convert(type,value);
		}
	};

	/* TODO I have also experimented with the JADE library's Money class.  It is probably more useful
	 * for performing currency conversions than as a datatype.  A possible source for currency exchange rates is the 
	 * web service here: 
	 * http://www.bindingpoint.com/service.aspx?skey=377e6659-061f-4956-8edb-19b5023bc33b
	 *  
	 */
	private static class MoneyConverter implements Converter {
		public Object convert(Class type, Object value) throws ConversionException {
			if (value == null)
				return Money.getInstance(0);
			if (value instanceof Money) {
				return value;
			} else if (value instanceof Number) {
				double num = ((Number)value).doubleValue();
			 	if (Double.isInfinite(num) || Double.isNaN(num)) {
			 		logger.log(Level.WARNING, "Error: number is invalid double in MoneyConverter {0}", value);
			 		num = 0.0;
			 	}
				return Money.getInstance(num);
			} else if (value instanceof String) {
				try {
					return Money.getFormat(false).parseObject((String) value);
				} catch (ParseException e) {
					throw new ConversionException(Messages.getString("Message.invalidDuration"));
				}
			}
			throw new ConversionException("Error: no conversion from " + value.getClass().getName() + " to " + type.getName() + " for value" + value);
		}
	}
}
