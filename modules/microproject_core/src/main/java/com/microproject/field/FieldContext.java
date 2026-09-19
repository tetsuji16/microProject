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
package com.microproject.field;

import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.util.DateTime;
/**
 * This class holds context specific information necessary for interacting with field data.
 */
public class FieldContext implements HasStartAndEnd {
	public static final long defaultStart = 0;
	public static final long defaultEnd = DateTime.getMaxDate().getTime();
	
	private boolean parseOnly;
	private boolean noUpdate;
	private boolean noDirty = false;
	private HasStartAndEnd interval = null;
	private boolean leftAssociation = true;
	private boolean round=false; //for start date when pasting from a string clipboard
	private boolean scripting = false;
	private boolean compact = false;
	private boolean forceValue = false;
	private boolean taskSheetUpdate = false;
	
	private static FieldContext noUpdateInstance = null;
	private static FieldContext scriptingInstance = null;
	
	public static FieldContext DEFAULT_CONTEXT= new FieldContext();
	public static FieldContext getNoUpdateInstance() {
		if (noUpdateInstance == null) {
			noUpdateInstance = new FieldContext();
			noUpdateInstance.setNoUpdate(true);
		}
		return noUpdateInstance;	
	}
	public static FieldContext getScriptingInstance() {
		if (scriptingInstance == null) {
			scriptingInstance = new FieldContext();
			scriptingInstance.setScripting(true);
		}
		return scriptingInstance;	
	}
	private static FieldContext noDirtyInstance = null;
	
	public static FieldContext getNoDirtyInstance() {
		if (noDirtyInstance == null) {
			noDirtyInstance = new FieldContext();
			noDirtyInstance.setNoDirty(true);
		}
		return noDirtyInstance;	
	}

	/**
	 * @return Returns parseOnly flag which indicates that the text should be parsed, errors thrown if necessary, but never set values
	 */
	public boolean isParseOnly() {
		return parseOnly;
	}
	/**
	 * @param parseOnly The parseOnly to set.
	 */
	public void setParseOnly(boolean parseOnly) {
		this.parseOnly = parseOnly;
	}
	/**
	 * @return Returns noUpdate flag which indicates that the field should be set but no update message sent
	 */
	public boolean isNoUpdate() {
		return noUpdate;
	}
	/**
	 * @param noUpdate The noUpdate to set.
	 */
	public void setNoUpdate(boolean noUpdate) {
		this.noUpdate = noUpdate;
	}
	
	/**
	 * @return Returns the interval.
	 */
	public HasStartAndEnd getInterval() {
		return interval;
	}
	/**
	 * @param interval The interval to set.
	 */
	public void setInterval(HasStartAndEnd interval) {
		this.interval = interval;
	}
	
	public static boolean hasInterval(FieldContext context) {
		if (context == null)
			return false;
		if (context.getInterval() ==  null)
			return false;
		return true;
	}
	
	
	public static boolean isParseOnly(FieldContext context) {
		if (context == null)
			return false;
		return context.isParseOnly();
	}

	public static boolean isNoUpdate(FieldContext context) {
		if (context == null)
			return false;
		return context.isNoUpdate();
	}
	
	public static boolean isScripting(FieldContext context) {
		if (context == null)
			return false;
		return context.isScripting();
	}
	public static boolean isForceValue(FieldContext context) {
		if (context == null)
			return false;
		return context.isForceValue();
	}	
	/**
	 * @return
	 */
	public long getEnd() {
		if (interval == null)
			return defaultEnd;
		return interval.getEnd();
	}
	/**
	 * @return
	 */
	public long getStart() {
		if (interval == null)
			return defaultStart;
		return interval.getStart();
	}
	
	public static long start(FieldContext context) {
		if (context == null)
			return defaultStart;
		return context.getStart();
	}
	public static long end(FieldContext context) {
		if (context == null)
			return defaultEnd;
		return context.getEnd();
	}
	
	public static boolean isScalar(long start, long end) { // see if range is all time
		return start == defaultStart && end == defaultEnd;
	}
	/**
	 * @return Returns the leftAssociation.
	 */
	public boolean isLeftAssociation() {
		return leftAssociation;
	}
	/**
	 * @param leftAssociation The leftAssociation to set.
	 */
	public void setLeftAssociation(boolean leftAssociation) {
		this.leftAssociation = leftAssociation;
	}

	public final boolean isNoDirty() {
		return noDirty;
	}

	public final void setNoDirty(boolean noDirty) {
		this.noDirty = noDirty;
	}

	public boolean isRound() {
		return round;
	}

	public void setRound(boolean round) {
		this.round = round;
	}
	public boolean isScripting() {
		return scripting;
	}
	public void setScripting(boolean scripting) {
		this.scripting = scripting;
	}
	public boolean isCompact() {
		return compact;
	}
	public void setCompact(boolean compact) {
		this.compact = compact;
	}
	public boolean isForceValue() {
		return forceValue;
	}
	public void setForceValue(boolean forceValue) {
		this.forceValue = forceValue;
	}
	public boolean isTaskSheetUpdate() {
		return taskSheetUpdate;
	}
	public void setTaskSheetUpdate(boolean taskSheetUpdate) {
		this.taskSheetUpdate = taskSheetUpdate;
	}
	public static boolean isTaskSheetUpdate(FieldContext context) {
		if (context == null)
			return false;
		return context.isTaskSheetUpdate();
	}
	
	
}
