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

package com.projectlibre1.server.data;

/**
 *
 */
public class CalendarData extends SerializedDataObject{
	static final long serialVersionUID = 192735738339L;

    protected CalendarData baseCalendar;
    protected long baseCalendarId=-1;
    
    public static final SerializedDataObjectFactory FACTORY=new SerializedDataObjectFactory(){
        public SerializedDataObject createSerializedDataObject(){
            return new CalendarData();
        }
    };
    
    public CalendarData getBaseCalendar() {
        return baseCalendar;
    }
    public void setBaseCalendar(CalendarData baseCalendar) {
        this.baseCalendar = baseCalendar;
        setBaseCalendarId((baseCalendar==null)?-1L:baseCalendar.getUniqueId());
    }
    
    public long getBaseCalendarId() {
		return baseCalendarId;
	}
	public void setBaseCalendarId(long baseCalendarId) {
		this.baseCalendarId = baseCalendarId;
	}
	public int getType(){
        return DataObjectConstants.CALENDAR_TYPE;
    }
    
    public void emtpy(){
    	baseCalendar=null;
    }

}
