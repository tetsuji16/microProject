/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/** Test-only support for invoking dialog body builders without constructing a top-level window. */
public final class HeadlessDialogPanels {
	private static final Unsafe UNSAFE = unsafe();

	private HeadlessDialogPanels() {
	}

	public static <T> T allocate(Class<T> type) {
		try {
			return type.cast(UNSAFE.allocateInstance(type));
		} catch (InstantiationException exception) {
			throw new AssertionError("Could not allocate headless dialog body fixture " + type.getName(), exception);
		}
	}

	public static void set(Object target, String fieldName, Object value) {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(target, value);
				return;
			} catch (NoSuchFieldException ignored) {
				// Continue through the dialog's superclass hierarchy.
			} catch (ReflectiveOperationException exception) {
				throw new AssertionError("Could not set headless dialog fixture field " + fieldName, exception);
			}
		}
		throw new AssertionError("Dialog fixture field not found: " + fieldName);
	}

	public static Object get(Object target, String fieldName) {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field.get(target);
			} catch (NoSuchFieldException ignored) {
				// Continue through the object's superclass hierarchy.
			} catch (ReflectiveOperationException exception) {
				throw new AssertionError("Could not read headless dialog fixture field " + fieldName, exception);
			}
		}
		throw new AssertionError("Dialog fixture field not found: " + fieldName);
	}

	public static Object getStatic(Class<?> type, String fieldName) {
		try {
			Field field = type.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(null);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Could not read headless dialog fixture field " + fieldName, exception);
		}
	}

	private static Unsafe unsafe() {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			return (Unsafe) field.get(null);
		} catch (ReflectiveOperationException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}
}
