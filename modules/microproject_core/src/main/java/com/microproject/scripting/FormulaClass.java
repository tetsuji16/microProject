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

import groovy.lang.GroovyObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.field.InvalidFormulaException;

public class FormulaClass {
	private static final Logger logger = Logger.getLogger(FormulaClass.class.getName());
	private final Map<String, ScriptedFormula> formulas = new LinkedHashMap<>();
	String className;
	private static final String imports="";
	private volatile Map<String, MethodHandle> invocationTargets;

	public FormulaClass(String className) {
		this.className = className;
	}
	
	synchronized void add(ScriptedFormula formula) {
		formulas.put(formula.getFormulaName(), formula);
		formula.setFormulaClass(this);
		invocationTargets = null;
		compileException = null;
	}
	
	private Exception compileException = null;
	public synchronized void compile() {
		if (invocationTargets == null && compileException == null) {
//					long x = System.currentTimeMillis();
			String classText = buildClassText();
			try {
				Class<? extends GroovyObject> groovyClass = GroovyClassCompiler.compile(classText, GroovyObject.class);
				GroovyObject groovyObject = groovyClass.getDeclaredConstructor().newInstance();
				Map<String, MethodHandle> targets = new HashMap<>();
				for (ScriptedFormula formula : formulas.values()) {
					MethodHandle target = MethodHandles.publicLookup()
						.unreflect(groovyClass.getMethod(formula.getFormulaName(), Object.class))
						.bindTo(groovyObject);
					targets.put(formula.getFormulaName(), target);
				}
				invocationTargets = Map.copyOf(targets);
			} catch (Exception e) {
				compileException = e;
				logger.log(Level.WARNING, "Failed to compile scripted formula class " + className, e);
			}
//					System.out.println("compiled class " + className + " in " + (System.currentTimeMillis()-x) + "ms");
		}
	}
	
	
	private String buildClassText() {
		StringBuilder text = new StringBuilder();
		text.append(imports);
		text.append("class " + className + " {");
		
		for (ScriptedFormula formula : formulas.values()) {
			text.append("\n\tObject ").append(formula.getFormulaName()).append("(Object ")
				.append(formula.getVariableName()).append(") {");
			text.append("\n\t\treturn ").append(formula.getText()).append(";\n\t}");
		}
		text.append("\n}"); // end of class body
		return text.toString();
	}
	
	public Object evaluate(String method, Object object) throws InvalidFormulaException {
		try {
			Map<String, MethodHandle> targets = invocationTargets;
			if (targets == null) {
				compile();
				targets = invocationTargets;
			}
			if (targets == null)
				throw compileException == null ? new IllegalStateException("Formula class was not compiled") : compileException;
			MethodHandle target = targets.get(method);
			if (target == null)
				throw new NoSuchMethodException(method);
			return target.invokeExact(object);
		} catch (Error e) {
			throw e;
		} catch (Throwable e) {
			logger.log(Level.WARNING, "Failed to evaluate scripted formula method " + method, e);
			throw new InvalidFormulaException(e);
		}
	}	
}
