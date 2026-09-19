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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FormulaFactory {
	private static final Logger logger = Logger.getLogger(FormulaFactory.class.getName());
	private static final Map<String, FormulaClass> scriptClassMap = new ConcurrentHashMap<>();
	private static final Map<String, Formula> formulaMap = new ConcurrentHashMap<>();
	public FormulaFactory() {
		super();
	}
	//	public Formula(String category,String formulaName, String variableName, String text) throws InvalidFormulaException {

	public static Formula addNormal(String className,String formulaName) {
		return formulaMap.computeIfAbsent(className, key -> {
			try {
				Formula formula = Class.forName(key).asSubclass(Formula.class)
					.getDeclaredConstructor().newInstance();
				formula.setFormulaName(formulaName);
				return formula;
			} catch (ReflectiveOperationException | ClassCastException e) {
				logger.log(Level.WARNING, "Formula class not found " + key, e);
				return null;
			}
		});
	}
	public static ScriptedFormula addScripted(String className,String formulaName, String variableName, String text) {
		String validFormulaName = formulaName.replace(' ', '_');
		
		FormulaClass formulaClass = scriptClassMap.computeIfAbsent(className, FormulaClass::new);
		ScriptedFormula formula = new ScriptedFormula(validFormulaName,variableName,text);
		formulaClass.add(formula);
		return formula;
		
	}
	
	public static void precompileClass(String className) {
		FormulaClass formulaClass = scriptClassMap.get(className);
		if (formulaClass!=null) formulaClass.compile();
	}
	
	

}
