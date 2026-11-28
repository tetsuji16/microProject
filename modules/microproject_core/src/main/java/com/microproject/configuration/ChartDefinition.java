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
package com.microproject.configuration;

import org.apache.commons.digester3.Digester;

/**
 * Defines a chart entry populated from the view configuration XML.
 */
public class ChartDefinition implements NamedItem {
	/** Identifies chart definitions in the configuration. */
	public static final String CATEGORY="Chart";

	private String name = null;
	private String title = null;
	private String type = null;
	private String x = null;
	private String y = null;
	private String z = null;
	boolean scaled=false;
	private boolean timeBased = false;

	/** Creates an empty chart definition for Digester property population. */
	public ChartDefinition() {
	}

	/**
	 * Registers the existing wildcard chart rule to create this class, bind its
	 * JavaBean properties, and append it to a parent through NamedItem.
	 *
	 * @param digester parser receiving the chart creation and property rules
	 */
	public static void addDigesterEvents(Digester digester) {
		digester.addObjectCreate("*/charts/chart", "com.microproject.configuration.ChartDefinition");
		digester.addSetProperties("*/charts/chart");
		digester.addSetNext("*/charts/chart", "add", "com.microproject.configuration.NamedItem");
	}

	/** @return configured chart name used by the parent chart collection */
	@Override
	public String getName() {
		return name;
	}

	/** @param name configured chart name from the XML name attribute */
	public void setName(String name) {
		this.name = name;
	}

	/** @return whether this chart is scaled in the view */
	public boolean isScaled() {
		return scaled;
	}

	/** @param scaled whether the configured chart uses scaling */
	public void setScaled(boolean scaled) {
		this.scaled = scaled;
	}

	/** @return whether this chart uses a time-based axis */
	public boolean isTimeBased() {
		return timeBased;
	}

	/** @param timeBased whether the configured chart uses a time-based axis */
	public void setTimeBased(boolean timeBased) {
		this.timeBased = timeBased;
	}

	/** @return configured chart type */
	public String getType() {
		return type;
	}

	/** @param type chart renderer type from the XML type attribute */
	public void setType(String type) {
		this.type = type;
	}

	/** @return field identifier configured for the x axis */
	public String getX() {
		return x;
	}

	/** @param x field identifier configured for the x axis */
	public void setX(String x) {
		this.x = x;
	}

	/** @return field identifier configured for the y axis */
	public String getY() {
		return y;
	}

	/** @param y field identifier configured for the y axis */
	public void setY(String y) {
		this.y = y;
	}

	/** @return field identifier configured for the z axis */
	public String getZ() {
		return z;
	}

	/** @param z field identifier configured for the z axis */
	public void setZ(String z) {
		this.z = z;
	}

	/** @return category constant used when registering this NamedItem */
	@Override
	public String getCategory() {
		return CATEGORY;
	}

	/** @return configured chart title */
	public String getTitle() {
		return title;
	}

	/** @param title chart title from the XML title attribute */
	public void setTitle(String title) {
		this.title = title;
	}
}
