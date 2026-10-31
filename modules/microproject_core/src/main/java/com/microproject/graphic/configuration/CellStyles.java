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
package com.microproject.graphic.configuration;


import org.apache.commons.digester3.Digester;

import com.microproject.configuration.Dictionary;

/**
 *
 */
public class CellStyles extends FormulaCatalog<CellStyle, CellStyleFactory> {
	public static final String category="CellStylesCategory";
	public String getCategory() {
		return category;
	}

	public CellStyles() {}

	public void addStyle(CellStyleFactory factory) {
		register(factory);
	}

	public CellStyle getStyle(String id) {
	    return resolve(id);
	}
	public CellStyle getDefaultStyle(){
	    return getStyle("CellStyle.default");
	}

	public static void addDigesterEvents(Digester digester){
		// main properties of bar
		digester.addObjectCreate("*/cellstyles", "com.microproject.graphic.configuration.CellStyles");
	    digester.addSetProperties("*/cellstyles");
		digester.addSetNext("*/cellstyles", "add", "com.microproject.configuration.NamedItem");

		// start section
		digester.addObjectCreate("*/cellstyles/style", "com.microproject.graphic.configuration.CellStyleFactory");
	    digester.addSetProperties("*/cellstyles/style");
	    digester.addCallMethod("*/cellstyles/style/formulaText","setFormulaText",0);
	    digester.addSetNext("*/cellstyles/style", "addStyle", "com.microproject.graphic.configuration.CellStyleFactory");
	    
	}
	
	
	
	
	
	
	protected static CellStyles instance=null;
	public static CellStyles getInstance(){
	    if (instance==null) instance=(CellStyles)Dictionary.get(category,"default");
	    return instance;
	}
}
