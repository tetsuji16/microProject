/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class ClassUtilsAccessibleMethodTest {
	@Test
	void resolvesExactPublicMethodSignatures() throws Exception {
		Method method = ClassUtils.getAccessibleMethod(VisibleImplementation.class, "read", FieldContextFixture.class);

		assertNotNull(method);
		assertEquals(String.class, method.getReturnType());
		assertNull(ClassUtils.getAccessibleMethod(VisibleImplementation.class, "read", String.class));
	}

	@Test
	void resolvesPublicMethodFromAccessibleSuperclassWhenRuntimeTypeIsNotPublic() {
		Method method = ClassUtils.getAccessibleMethod(HiddenImplementation.class, "name");

		assertNotNull(method);
		assertEquals(VisibleBase.class, method.getDeclaringClass());
	}

	@Test
	void resolvesPublicMethodFromAccessibleInterfaceWhenRuntimeTypeIsNotPublic() {
		Method method = ClassUtils.getAccessibleMethod(HiddenInterfaceImplementation.class, "label");

		assertNotNull(method);
		assertEquals(VisibleContract.class, method.getDeclaringClass());
	}

	public static class VisibleBase {
		public String name() {
			return "name";
		}
	}

	public static class VisibleImplementation extends VisibleBase {
		public String read(FieldContextFixture context) {
			return context.toString();
		}
	}

	static class HiddenImplementation extends VisibleBase {
		@Override
		public String name() {
			return "hidden";
		}
	}

	public interface VisibleContract {
		String label();
	}

	static class HiddenInterfaceImplementation implements VisibleContract {
		@Override
		public String label() {
			return "label";
		}
	}

	public static class FieldContextFixture {
	}
}
