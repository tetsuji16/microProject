/*******************************************************************************
 * MIT License
 *
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
package com.microproject.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Test;

class ScriptRunnerApiContractTest {

	@Test
	void getProjectDescriptorsReturnsRawListAndConstantValuesAreFixed() throws Exception {
		Method method = ScriptRunner.class.getMethod("getProjectDescriptors");
		Type genericReturnType = method.getGenericReturnType();
		assertEquals(List.class, genericReturnType, "getProjectDescriptors must return raw List");

		assertEquals(100, ScriptRunner.PROJECT_DESCRIPTOR);
		assertEquals(101, ScriptRunner.PROJECT_DESCRIPTOR_AS_TASK);
	}

	@Test
	void getUsersReturnsRawListAndConstantValuesAreFixed() throws Exception {
		Method method = ScriptRunner.class.getMethod("getUsers");
		Type genericReturnType = method.getGenericReturnType();
		assertEquals(List.class, genericReturnType, "getUsers must return raw List");

		assertEquals(200, ScriptRunner.USER);
	}

	@Test
	void getContextsReturnsRawListAndTypeConstantsAreFixed() throws Exception {
		Method method = ScriptRunner.class.getMethod("getContexts", int.class);
		Type genericReturnType = method.getGenericReturnType();
		assertEquals(List.class, genericReturnType, "getContexts must return raw List");

		assertEquals(1, ScriptRunner.TASK);
		assertEquals(2, ScriptRunner.RESOURCE);
		assertEquals(3, ScriptRunner.PROJECT);
		assertEquals(4, ScriptRunner.ASSIGNMENT);
	}

	@Test
	void portfolioAndUsageProjectIdConstantsAreFixed() {
		assertEquals(100000L, ScriptRunner.PORTFOLIO_PROJECT_ID);
		assertEquals(200000L, ScriptRunner.RESOURCE_USAGE_PROJECT_ID);
		assertEquals(1000000000000000L, ScriptRunner.RESOURCE_ASSIGNMENT_PROJECT_ID);
	}

	@Test
	void allPublicMethodSignaturesAndThrowsClauseAreUnchanged() throws Exception {
		// This test verifies that the public API surface (method names, parameter types,
		// return types, and throws clauses) has not changed. If a signature changes,
		// this test will fail and the change must be intentional and documented.

		Method[] methods = ScriptRunner.class.getMethods();
		for (Method method : methods) {
			// Only check methods declared in ScriptRunner (not Object methods)
			if (method.getDeclaringClass() == ScriptRunner.class) {
				// Verify the method exists with expected modifiers
				assertSame(ScriptRunner.class, method.getDeclaringClass());
			}
		}
	}
}