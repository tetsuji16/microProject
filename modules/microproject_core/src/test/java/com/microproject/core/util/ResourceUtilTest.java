/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.core.util;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResourceUtilTest {
	@Test
	void createObjectUsesNoArgumentConstructor() {
		assertInstanceOf(Constructible.class,
				ResourceUtil.createObject("ResourceUtilTest$Constructible", "com.microproject.core.util"));
	}

	@Test
	void createObjectPropagatesConstructorFailure() {
		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> ResourceUtil.createObject("ResourceUtilTest$Failing", "com.microproject.core.util"));

		assertSame(Failing.FAILURE, failure);
	}

	public static class Constructible {
		public Constructible() {
		}
	}

	public static class Failing {
		private static final IllegalStateException FAILURE = new IllegalStateException("constructor failed");

		public Failing() {
			throw FAILURE;
		}
	}
}
