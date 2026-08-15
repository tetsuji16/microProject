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
