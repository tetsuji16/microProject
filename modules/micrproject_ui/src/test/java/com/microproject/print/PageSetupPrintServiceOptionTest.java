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
package com.microproject.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.print.PrintService;

import org.junit.jupiter.api.Test;

/**
 * Issue #177: PageSetup.PrintServiceOption overrides equals() and now
 * implements hashCode() consistently (previously only Object identity hash).
 */
class PageSetupPrintServiceOptionTest {

	@Test
	void printServiceOptionEqualsImpliesSameHashCode() {
		PrintService a = fakePrintService("Printer A");
		PrintService b = fakePrintService("Printer A");

		PageSetup.PrintServiceOption same1 = new PageSetup.PrintServiceOption(a);
		PageSetup.PrintServiceOption same2 = new PageSetup.PrintServiceOption(a);
		PageSetup.PrintServiceOption other = new PageSetup.PrintServiceOption(b);

		assertEquals(same1, same2);
		assertEquals(same1.hashCode(), same2.hashCode());
		assertNotEquals(same1, other);
	}

	private static PrintService fakePrintService(String name) {
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) {
				String methodName = method.getName();
				if (methodName.equals("getName") && method.getParameterCount() == 0) {
					return name;
				}
				if (methodName.equals("equals") && method.getParameterCount() == 1) {
					return proxy == args[0];
				}
				if (methodName.equals("hashCode") && method.getParameterCount() == 0) {
					return System.identityHashCode(proxy);
				}
				Class<?> returnType = method.getReturnType();
				if (returnType == boolean.class) return false;
				if (returnType == int.class) return 0;
				if (returnType == long.class) return 0L;
				if (returnType == float.class) return 0.0f;
				if (returnType == double.class) return 0.0d;
				return null;
			}
		};
		return (PrintService) Proxy.newProxyInstance(PrintService.class.getClassLoader(),
				new Class<?>[] { PrintService.class }, handler);
	}
}
