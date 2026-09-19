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

import com.microproject.field.InvalidFormulaException;

/** Encapsulation of formulas compiled together by {@link FormulaClass}. */
public class ScriptedFormula extends Formula{
	private String text = null;
	private String variableName = null;
	private FormulaClass formulaClass = null;
	/**
	 * @throws InvalidFormulaException
	 * 
	 */
	ScriptedFormula(String formulaName, String variableName, String text) {
		super(formulaName);
		this.variableName = variableName;
		this.text = text;
	}
	
	public Object evaluate(Object object) throws InvalidFormulaException {
		// lets call some method on an instance
		return formulaClass.evaluate(formulaName,object);
	}
	

	/**
	 * @return Returns the text.
	 */
	public String getText() {
		return text;
	}


	public final String getVariableName() {
		return variableName;
	}


	public final FormulaClass getFormulaClass() {
		return formulaClass;
	}


	public final void setFormulaClass(FormulaClass formulaClass) {
		this.formulaClass = formulaClass;
	}

}
