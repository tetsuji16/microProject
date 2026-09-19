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
package com.microproject.exchange;

import net.sf.mpxj.ProjectCalendar;


public class Context {
	private boolean xml = false;
	ProjectCalendar defaultMPXCalendar = null;
	boolean datesWithoutTimeZone = false;
	boolean skipExtraFields = false;
	boolean noAssignmentDelays =false;
	boolean mariner =false;
	boolean datesPinned =false;
	boolean useFixedDuration = false;
	boolean actualsProtected = false;
	boolean showedActualWarning = false;
	public boolean isUseFixedDuration() {
		return useFixedDuration;
	}
	public void setUseFixedDuration(boolean useFixedDuration) {
		this.useFixedDuration = useFixedDuration;
	}
	public boolean isDatesPinned() {
		return datesPinned;
	}
	public void setDatesPinned(boolean datesPinned) {
		this.datesPinned = datesPinned;
	}
	public Context() {
		
	}
	public final boolean isXml() {
		return xml;
	}
	public final void setXml(boolean xml) {
		this.xml = xml;
	}
	public final ProjectCalendar getDefaultMPXCalendar() {
		return defaultMPXCalendar;
	}
	public final void setDefaultMPXCalendar(ProjectCalendar defaultMPXCalendar) {
		this.defaultMPXCalendar = defaultMPXCalendar;
	}
	public boolean isDatesWithoutTimeZone() {
		return datesWithoutTimeZone;
	}
	public void setDatesWithoutTimeZone(boolean datesWithoutTimeZone) {
		this.datesWithoutTimeZone = datesWithoutTimeZone;
	}
	public boolean isSkipExtraFields() {
		return skipExtraFields;
	}
	public void setSkipExtraFields(boolean skipExtraFields) {
		this.skipExtraFields = skipExtraFields;
	}
	public boolean isNoAssignmentDelays() {
		return noAssignmentDelays;
	}
	public void setNoAssignmentDelays(boolean noAssignmentDelays) {
		this.noAssignmentDelays = noAssignmentDelays;
	}
	public boolean isMariner() {
		return mariner;
	}
	public void setMariner(boolean mariner) {
		this.mariner = mariner;
	}
	public boolean isActualsProtected() {
		return actualsProtected;
	}
	public void setActualsProtected(boolean actualsProtected) {
		this.actualsProtected = actualsProtected;
	}
	public boolean isShowedActualWarning() {
		return showedActualWarning;
	}
	public void setShowedActualWarning(boolean showedActualWarning) {
		this.showedActualWarning = showedActualWarning;
	}
}
