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
package com.microproject.util;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.datatype.Duration;
import com.microproject.datatype.Money;
import com.microproject.datatype.Rate;
import com.microproject.datatype.TimeUnit;
import com.microproject.datatype.Work;
import com.microproject.field.Field;


/**
 * Utility functions for manipulating primitive types
 */
public class ClassUtils {
	private static final Logger logger = Logger.getLogger(ClassUtils.class.getName());
	private static final Map<String, java.lang.reflect.Field> STATIC_FIELDS = new ConcurrentHashMap<>();
	private static final Map<String, Method> STATIC_METHODS = new ConcurrentHashMap<>();

	public static final Long defaultLong = Long.valueOf(0L);
	public static final Double defaultDouble = Double.valueOf(0.0);
	public static final Integer defaultInteger = Integer.valueOf(0);
	public static final Float defaultFloat = Float.valueOf(0.0f);
	public static final Boolean defaultBoolean = Boolean.valueOf(false);
	public static final String defaultString = "";
	public static final Rate defaultRate = new Rate(1.0D);
	public static final Rate defaultUnitlessRate = new Rate(1, TimeUnit.NON_TEMPORAL);


	/**
	 * Given a type, return its default value.  If type is unknown, a new one is constructed
	 * @param clazz
	 * @return
	 */
	public static Object getDefaultValueForType(Class<?> clazz) {
		if (clazz == String.class)
			return defaultString;
		else if (clazz == Double.class || clazz == Double.TYPE)
			return defaultDouble;
		else if (clazz == Integer.class || clazz == Integer.TYPE)
			return defaultInteger;
		else if (clazz == Long.class || clazz == Long.TYPE)
			return defaultLong;
		else if (clazz == Float.class || clazz == Float.TYPE)
			return defaultFloat;
		else if (clazz == Boolean.class)
			return defaultBoolean;
		else if (clazz == Rate.class)
			return defaultRate;
		else {
			try {
				logger.info("making default for class " + clazz);
				return clazz.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				logger.log(Level.WARNING, "Failed to instantiate default value for " + clazz, e);
			}
			return null;
		}
	}
	
	public static boolean isDefaultValue(Object value) {
		return (value == defaultLong
			|| value == defaultDouble
			|| value == defaultInteger
			|| value == defaultFloat
			|| value == defaultString
			|| value == Duration.ZERO
			|| value == defaultRate
			|| value == DateTime.getZeroDate());
	}

	public static final Long LONG_MULTIPLE_VALUES = Long.valueOf(0L);
	public static final Double DOUBLE_MULTIPLE_VALUES = Double.valueOf(0.0);
	public static final Integer INTEGER_MULTIPLE_VALUES = Integer.valueOf(0);
	public static final Float FLOAT_MULTIPLE_VALUES = Float.valueOf(0.0f);
	public static final Boolean BOOLEAN_MULTIPLE_VALUES = Boolean.valueOf(false);
	public static final String STRING_MULTIPLE_VALUES = "";
	public static final Double PERCENT_MULTIPLE_VALUES = Double.valueOf(-9876543.21); // a never used value used as flag to indicate multiple values
	public static final Rate RATE_MULTIPLE_VALUES = new Rate();
	/**
	 * Given a type, return a value that signifies that there are multiple values.  This can occur in a dialog which works on multile objects at once.  If type is unknown, a new one is constructed
	 * @param clazz
	 * @return
	 */
	public static Object getMultipleValueForType(Class<?> clazz) {
		if (clazz == String.class)
			return STRING_MULTIPLE_VALUES;
		else if (clazz == Double.class || clazz == Double.TYPE)
			return DOUBLE_MULTIPLE_VALUES;
		else if (clazz == Integer.class || clazz == Integer.TYPE)
			return INTEGER_MULTIPLE_VALUES;
		else if (clazz == Long.class || clazz == Long.TYPE)
			return LONG_MULTIPLE_VALUES;
		else if (clazz == Float.class || clazz == Float.TYPE)
			return FLOAT_MULTIPLE_VALUES;
		else if (clazz == Boolean.class)
			return BOOLEAN_MULTIPLE_VALUES;
		else if (clazz == Rate.class)
			return RATE_MULTIPLE_VALUES;
		else {
			try {
				return clazz.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
			}
			return null;
		}
	}
	
	public static boolean isMultipleValue(Object value) {
		if (value == null)
			return false;
		return (value == LONG_MULTIPLE_VALUES
			|| value == DOUBLE_MULTIPLE_VALUES
			|| value == INTEGER_MULTIPLE_VALUES
			|| value == FLOAT_MULTIPLE_VALUES
			|| value == STRING_MULTIPLE_VALUES
			|| value.equals(PERCENT_MULTIPLE_VALUES)
			|| value == RATE_MULTIPLE_VALUES
			|| value == Duration.ZERO
			|| value == DateTime.getZeroDate());
	}
	
	
	
	/**
	 * Get the corresponding object class from a primitive class
	 * @param clazz primitive class
	 * @return Object class.
	 * @throws ClassCastException if class is unknown primitive
	 */
	public static Class<?> primitiveToObjectClass(Class<?> clazz) {
		if (clazz == Boolean.TYPE)
			return Boolean.class;
		else if (clazz == Character.TYPE)
			return Character.class;
		else if (clazz == Byte.TYPE)
			return Byte.class;
		else if (clazz == Short.TYPE)
			return Short.class;
		else if (clazz == Integer.TYPE)
			return Integer.class;
		else if (clazz == Long.TYPE)
			return Long.class;
		else if (clazz == Float.TYPE)
			return Float.class;
		else if (clazz == Double.TYPE)
			return Double.class;
		throw new ClassCastException("Cannot convert class" + clazz + " to an object class");
	}
	
/**
 * Convert a Double to an Object of a given class
 * @param value Double value to convert
 * @param clazz Class the class to convert to
 * @return an object of the given class
 * @throws IllegalArgumentException if the value is not convertible to the class
 */	public static Object doubleToObject(Double value, Class<?> clazz) {
		if (clazz == Boolean.class)
			return Boolean.valueOf(value.doubleValue() != 0.0);
		else if (clazz == Byte.class)
			return Byte.valueOf(value.byteValue());
		else if (clazz == Short.class)
			return Short.valueOf(value.shortValue());
		else if (clazz == Integer.class)
			return Integer.valueOf(value.intValue());
		else if (clazz == Long.class)
			return Long.valueOf(value.longValue());
		else if (clazz == Float.class)
			return Float.valueOf(value.floatValue());			
		else if (clazz == Double.class)
			return value;
		else if (clazz == Money.class)
			return Money.getInstance(value.doubleValue());
		else if (clazz == Duration.class)
			return Duration.getInstanceFromDouble(value);
		else if (clazz == Work.class)
			return Work.getWorkInstanceFromDouble(value);
		

		
		throw new IllegalArgumentException("Class " + clazz + " cannot be converted from a Double");
	}
 
	public static java.lang.reflect.Field staticFieldFromFullName(String nameAndField) {
		java.lang.reflect.Field cached = STATIC_FIELDS.get(nameAndField);
		if (cached != null)
			return cached;
		int lastDot = nameAndField.lastIndexOf(".");
		String className = nameAndField.substring(0,lastDot);
		String fieldName = nameAndField.substring(lastDot+1);
		try {
			java.lang.reflect.Field field = ClassUtils.forName(className).getDeclaredField(fieldName);
			STATIC_FIELDS.putIfAbsent(nameAndField, field);
			return field;
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "Failed to resolve static field " + nameAndField, e);
		} catch (NoSuchFieldException e) {
			logger.log(Level.WARNING, "Failed to resolve static field " + nameAndField, e);
		} catch (ClassNotFoundException e) {
			logger.log(Level.WARNING, "Failed to resolve static field " + nameAndField, e);
		}
		return null;
	}
	
	public static Method staticVoidMethodFromFullName(String nameAndField) {
		return staticMethodFromFullName(nameAndField,null);
	}
	
	public static Method staticMethodFromFullName(String nameAndField, Class<?>[] args) {
		String cacheKey = nameAndField + Arrays.toString(args == null ? new Class<?>[0] : args);
		Method cached = STATIC_METHODS.get(cacheKey);
		if (cached != null)
			return cached;
		int lastDot = nameAndField.lastIndexOf(".");
		String className = nameAndField.substring(0,lastDot);
		String methodName = nameAndField.substring(lastDot+1);
		try {
			Method method = ClassUtils.forName(className).getDeclaredMethod(methodName, args);
			STATIC_METHODS.putIfAbsent(cacheKey, method);
			return method;
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "Failed to resolve static method " + nameAndField, e);
		} catch (ClassNotFoundException e) {
			logger.log(Level.WARNING, "Failed to resolve static method " + nameAndField, e);
		} catch (NoSuchMethodException e) {
			logger.log(Level.WARNING, "Failed to resolve static method " + nameAndField, e);
		}
		return null;
	}	
	/**
	 * Set the array size of the custom field this applies to
	 * @param boundsField
	 */	
		public static void setStaticField(String field, int value) {
			try {
				staticFieldFromFullName(field).setInt(null,value);
			} catch (IllegalArgumentException e) {
				logger.log(Level.WARNING, "Failed to set static int field " + field, e);
			} catch (IllegalAccessException e) {
				logger.log(Level.WARNING, "Failed to set static int field " + field, e);
			}
		}
		
		public static void setStaticField(String field, String value) {
			try {
				staticFieldFromFullName(field).set(null,value);
			} catch (IllegalArgumentException e) {
				logger.log(Level.WARNING, "Failed to set static field " + field, e);
			} catch (IllegalAccessException e) {
				logger.log(Level.WARNING, "Failed to set static field " + field, e);
			}
		}		

		/**
		 * Safe Class.forName.  See http://radio.weblogs.com/0112098/stories/2003/02/12/classfornameIsEvil.html
		 * @param className
		 * @return
		 * @throws ClassNotFoundException
		 */
		public static Class<?> forName(String className) throws ClassNotFoundException {
			Class<?> theClass = null;
			try {
			    theClass = Class.forName( className, true, Thread.currentThread().getContextClassLoader() );
			}
			catch (ClassNotFoundException e) {
			    theClass = Class.forName( className );
			}
			return theClass;
		}
		
		public static boolean setSimpleProperty(Object bean, String name, Object value) {
			try {
				if (bean == null || name == null || name.isEmpty())
					throw new IllegalArgumentException("Bean and simple property name are required");
				for (PropertyDescriptor descriptor : Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors()) {
					if (name.equals(descriptor.getName()) && descriptor.getWriteMethod() != null) {
						descriptor.getWriteMethod().invoke(bean, value);
						return true;
					}
				}
				throw new IllegalArgumentException("No writable JavaBean property named " + name);
			} catch (Exception e) { //claur
				logger.log(Level.WARNING, "Failed to set property " + name + " on " + bean, e);
			}
			return false;
		}

		/** Resolves a public method through an accessible type in the class hierarchy. */
		public static Method getAccessibleMethod(Class<?> type, String name, Class<?>... parameterTypes) {
			if (type == null || name == null)
				return null;
			try {
				Method method = type.getMethod(name, parameterTypes);
				if (Modifier.isPublic(method.getDeclaringClass().getModifiers()))
					return method;
			} catch (NoSuchMethodException e) {
				// Search accessible interfaces and superclasses below.
			}
			for (Class<?> interfaceType : type.getInterfaces()) {
				Method method = getAccessibleMethod(interfaceType, name, parameterTypes);
				if (method != null)
					return method;
			}
			return getAccessibleMethod(type.getSuperclass(), name, parameterTypes);
		}

		/** Reads a simple JavaBean property without accepting nested or indexed paths. */
		public static Object getSimpleProperty(Object bean, String name)
				throws java.beans.IntrospectionException, ReflectiveOperationException {
			if (bean == null || name == null || name.isEmpty() || name.indexOf('.') >= 0
					|| name.indexOf('[') >= 0 || name.indexOf('(') >= 0)
				throw new IllegalArgumentException("Bean and simple property name are required");
			for (PropertyDescriptor descriptor : Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors()) {
				if (name.equals(descriptor.getName()) && descriptor.getReadMethod() != null)
					return descriptor.getReadMethod().invoke(bean);
			}
			throw new IllegalArgumentException("No readable JavaBean property named " + name);
		}

		public static boolean isObjectReadOnly(Object object){
			return invokeReadOnly(object, new Class<?>[0]);
		}
		public static boolean isObjectFieldReadOnly(Object object,Field field){
			return invokeReadOnly(object, new Class<?>[] {Field.class}, field);
		}

		private static boolean invokeReadOnly(Object object, Class<?>[] parameterTypes, Object... arguments) {
			if (object == null)
				return false;
			try {
				Method method = object.getClass().getMethod("isReadOnly", parameterTypes);
				return Boolean.TRUE.equals(method.invoke(object, arguments));
			} catch (NoSuchMethodException e) {
				return false;
			} catch (ReflectiveOperationException | IllegalArgumentException | ClassCastException e) {
				logger.log(Level.FINE, "Failed to evaluate read-only state for " + object.getClass().getName(), e);
				return false;
			}
		}
		
		private static Map<Class<?>, Comparator<Object>> comparatorMap = null;
		private static final Comparator<Object> defaultTextComparator =
				(o1, o2) -> ("" + o1).compareTo("" + o2);

		private static Comparator<Object> nullSafeNaturalComparator() {
			return Comparator.nullsFirst(naturalComparator());
		}

		@SuppressWarnings("unchecked")
		public static Comparator<Object> naturalComparator() {
			return (Comparator<Object>) (Comparator<?>) Comparator.naturalOrder();
		}

		private static void registerComparator(Class<?> type, Comparator<Object> comparator) {
			comparatorMap.put(type, comparator);
		}

		@SuppressWarnings("unchecked")
		public static Comparator<Object> getComparator(Class<?> clazz) {
			if (comparatorMap == null) {
				comparatorMap = HashMap.newHashMap(13);
				registerComparator(String.class, nullSafeNaturalComparator());
				registerComparator(Date.class, nullSafeNaturalComparator());
				registerComparator(Integer.class, nullSafeNaturalComparator());
				registerComparator(Long.class, nullSafeNaturalComparator());
				registerComparator(Short.class, nullSafeNaturalComparator());
				registerComparator(Float.class, nullSafeNaturalComparator());
				registerComparator(Double.class, nullSafeNaturalComparator());
				registerComparator(Byte.class, nullSafeNaturalComparator());
				registerComparator(Boolean.class, nullSafeNaturalComparator());
				registerComparator(Money.class, nullSafeNaturalComparator());
				registerComparator(Duration.class, nullSafeNaturalComparator());
				registerComparator(Work.class, nullSafeNaturalComparator());
				registerComparator(Rate.class, nullSafeNaturalComparator());
			}
			Comparator<Object> result = comparatorMap.get(clazz);
			if (result == null) {
				if (clazz != null && Comparable.class.isAssignableFrom(clazz))
					return naturalComparator();
				return defaultTextComparator;
			}
			return result;
		}


}
