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

import org.apache.commons.collections.Predicate;

import com.microproject.configuration.Dictionary;
import com.microproject.field.InvalidFormulaException;
import com.microproject.scripting.Formula;
import com.microproject.scripting.FormulaFactory;
import com.microproject.strings.Messages;

/**
 *
 */
public class BarStyle implements Predicate {
//	static Log log = LogFactory.getLog(BarStyle.class);
	public static final String FORMULA_PREFIX = "BarStyle";
	String name = null;
	String id = null;
	String formulaText = null;
	String formulaClass = null;
	String barFormatName = null;
	String type = null; // type is actually only used to construct formula
	String formatId = null;
	boolean link=false;
	boolean annotation=false;
	boolean calendar=false;
	boolean horizontalGrid = false;
	
	public boolean isHorizontalGrid() {
		return horizontalGrid;
	}

	public void setHorizontalGrid(boolean horizontalGrid) {
		if (this.horizontalGrid == horizontalGrid)
			return;
		this.horizontalGrid = horizontalGrid;
		invalidateStyleIndex();
	}

	private BarFormat barFormat = null;
	private Formula formula = null;
	BarStyles belongsTo;

	boolean active = true;
	public BarStyle() {}
	
	public boolean evaluate(Object object) {
		if (!active)
			return false;
		try {
			if (formula == null)
				return true;
			return ((Boolean) formula.evaluate(object)).booleanValue();
		} catch (InvalidFormulaException e) {

//			log.warn("Error evaluating formula in BarMappingRow" + name);
			return false;
		}
	}

	/**
	 * @return Returns the barFormat.
	 */
	public String getBarFormatName() {
		return barFormatName;
	}
	/**
	 * @param barFormat The barFormat to set.
	 */
	public void setFormatId(String formatId) {
		this.formatId = formatId;
		String name = Messages.getString(formatId);
		barFormat = (BarFormat) Dictionary.get(BarFormat.category,name);
	}
	/**
	 * @return Returns the formula.
	 */
	public String getFormulaText() {
		return formulaText;
	}
	
	void build() {
		if (formulaText == null&&formulaClass == null)
			formulaClass="com.microproject.scripting.formulas.TrueFormula";
		if (formulaClass!=null)
			formula=FormulaFactory.addNormal(formulaClass,name);
		else if (formulaText!=null)
			formula = FormulaFactory.addScripted(FORMULA_PREFIX + belongsTo.getName(),name,type,formulaText);
		String idName = Messages.getString(formatId);
		barFormat = (BarFormat) Dictionary.get(BarFormat.category,idName);
	}
	/**
	 * @param formula The formula to set.
	 */
	public void setFormulaText(String formulaText) {
		this.formulaText = formulaText;
	}
	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(String id) {
		this.id = id;
		setName(Messages.getString(id));
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type The type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return Returns the barFormat.
	 */
	public BarFormat getBarFormat() {
		return barFormat;
	}
	/**
	 * @return Returns the active.
	 */
	public boolean isActive() {
		return active;
	}
	/**
	 * @param active The active to set.
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	
	
	public boolean isLink() {
		return link;
	}
	public void setLink(boolean link) {
		if (this.link == link)
			return;
		this.link = link;
		invalidateStyleIndex();
	}

	public boolean isAnnotation() {
		return annotation;
	}

	public void setAnnotation(boolean annotation) {
		if (this.annotation == annotation)
			return;
		this.annotation = annotation;
		invalidateStyleIndex();
	}

	public boolean isCalendar() {
		return calendar;
	}

	public void setCalendar(boolean calendar) {
		if (this.calendar == calendar)
			return;
		this.calendar = calendar;
		invalidateStyleIndex();
	}

	private void invalidateStyleIndex() {
		if (belongsTo != null)
			belongsTo.invalidateStyleIndex();
	}

	public void setBelongsTo(BarStyles styles) {
		this.belongsTo = styles;
	}

	public String getFormulaClass() {
		return formulaClass;
	}

	public void setFormulaClass(String formulaClass) {
		this.formulaClass = formulaClass;
	}
	
	
	
}
