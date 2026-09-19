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
package com.microproject.scripting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

class FormulaClassTest {
	@Test
	void resolvesAndReusesCompiledFormulaMethods() throws Exception {
		FormulaClass formulas = new FormulaClass("FormulaClassTestScript");
		formulas.add(new ScriptedFormula("doubleValue", "value", "value * 2"));
		formulas.add(new ScriptedFormula("label", "value", "'item-' + value"));

		assertEquals(42, formulas.evaluate("doubleValue", 21));
		assertEquals("item-7", formulas.evaluate("label", 7));
		assertEquals(84, formulas.evaluate("doubleValue", 42));
	}

	@Test
	void evaluationsDoNotShareAMutableArgumentArray() throws Exception {
		FormulaClass formulas = new FormulaClass("ConcurrentFormulaClassTestScript");
		formulas.add(new ScriptedFormula("identity", "value", "value"));
		formulas.compile();

		try (var executor = Executors.newFixedThreadPool(8)) {
			List<Callable<Void>> calls = new ArrayList<>();
			for (int thread = 0; thread < 8; thread++) {
				int offset = thread * 10_000;
				calls.add(() -> {
					for (int i = 0; i < 2_000; i++) {
						int value = offset + i;
						assertEquals(value, formulas.evaluate("identity", value));
					}
					return null;
				});
			}
			for (var result : executor.invokeAll(calls))
				result.get();
		}
	}

	@Test
	void addingTheSameFormulaNameReplacesItsDefinition() throws Exception {
		FormulaClass formulas = new FormulaClass("ReplaceFormulaClassTestScript");
		ScriptedFormula original = new ScriptedFormula("calculate", "value", "value * 2");
		formulas.add(original);
		assertEquals(10, original.evaluate(5));

		ScriptedFormula replacement = new ScriptedFormula("calculate", "value", "value * 3");
		formulas.add(replacement);

		assertEquals(15, replacement.evaluate(5));
		assertEquals(15, original.evaluate(5));
	}
}
