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
package com.microproject.script.object;

import java.io.Serializable;

import com.microproject.timescale.CalendarUtil;

public class ValueBar extends Bar implements Serializable{
	static final long serialVersionUID = 172830273911283L;
	protected double w,ct;
	public ValueBar() {
		super();
	}
	public ValueBar(ValueBar v) {
		this(v.getS(),v.getE(),v.getW(),v.getCt());
	}
	public ValueBar(long s, long e, double w, double ct) {
		super(s,e);
		this.w = w;
		this.ct = ct;
	}
	public double getW() {
		return w;
	}
	public void setW(double w) {
		this.w = w;
	}
	public double getCt() {
		return ct;
	}
	public void setCt(double ct) {
		this.ct = ct;
	}

	public void reset(){
		s=e=0;
		w=ct=0.0d;
	}

	public String toString(){
		return "ValueBar{s="+CalendarUtil.toString(s)+", e="+CalendarUtil.toString(e)+"w="+w+", ct="+ct+"}";
	}

}
