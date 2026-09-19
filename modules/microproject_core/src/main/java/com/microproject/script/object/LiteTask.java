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
import java.util.List;

import com.microproject.script.ScriptRunner;

public class LiteTask  implements Serializable{
	static final long serialVersionUID = 172830333211283L;
	protected String txt;
	protected long parentId;
	protected List field;
	protected List<Link> links;
	protected boolean critical,subproject,parent;
	protected long id,s,e,c;
	protected double w,ct;
	protected List<Bar> bars;
	protected List<Bar> dist;
	public List<Bar> getBars() {
		return bars;
	}
	public void setBars(List<Bar> bars) {
		this.bars = bars;
	}
	public List<Bar> getDist() {
		return dist;
	}
	public void setDist(List<Bar> dist) {
		this.dist = dist;
	}
	public long getC() {
		return c;
	}
	public void setC(long c) {
		this.c = c;
	}
	public boolean isCritical() {
		return critical;
	}
	public void setCritical(boolean critical) {
		this.critical = critical;
	}
	public long getE() {
		return e;
	}
	public void setE(long e) {
		this.e = e;
	}
	public List getField() {
		return field;
	}
	public void setField(List field) {
		this.field = field;
	}
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	public boolean isParent() {
		return parent;
	}
	public void setParent(boolean parent) {
		this.parent = parent;
	}
	public long getParentId() {
		return parentId;
	}
	public void setParentId(long parentId) {
		this.parentId = parentId;
	}
	public long getS() {
		return s;
	}
	public void setS(long s) {
		this.s = s;
	}
	public boolean isSubproject() {
		return subproject;
	}
	public void setSubproject(boolean subproject) {
		this.subproject = subproject;
	}
	public String getTxt() {
		return txt;
	}
	public void setTxt(String txt) {
		this.txt = txt;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getType() {
		return ScriptRunner.TASK;
	}
	public void setType() {
		// in case jsonrpc wants a method
	}
	public double getCt() {
		return ct;
	}
	public void setCt(double ct) {
		this.ct = ct;
	}
	public double getW() {
		return w;
	}
	public void setW(double w) {
		this.w = w;
	}
	
	
}
