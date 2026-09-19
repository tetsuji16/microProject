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
package com.microproject.server.data;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;

public class DistributionData implements Serializable{
	static final long serialVersionUID = 79362873984L;
	public final static int UPDATE=1;
	public final static int INSERT=4;
	public final static int REMOVE=8;

	protected long projectId,resourceId,taskId;
	protected int timeId;
	protected short type;
	protected double cost,work;
	protected int status;

	public double getCost() {
		return cost;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
	public long getProjectId() {
		return projectId;
	}
	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}
	public long getResourceId() {
		return resourceId;
	}
	public void setResourceId(long resourceId) {
		this.resourceId = resourceId;
	}
	public long getTaskId() {
		return taskId;
	}
	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}
	public int getTimeId() {
		return timeId;
	}
	public void setTimeId(int timeId) {
		this.timeId = timeId;
	}
	public short getType() {
		return type;
	}
	public void setType(short type) {
		this.type = type;
	}
	public double getWork() {
		return work;
	}
	public void setWork(double work) {
		this.work = work;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	public int hashCode(){
		int _hashCode = 0;
		_hashCode += (int)this.resourceId;
		_hashCode += (int)this.taskId;
		_hashCode += (int)this.timeId;
		_hashCode += (int)this.type;

		return _hashCode;
	}

	public boolean equals(Object obj){
		if( !(obj instanceof DistributionData) )
			return false;

		DistributionData d = (DistributionData)obj;
		boolean eq = true;

		if( obj == null ){
			eq = false;
		}
		else{
			eq = eq && this.resourceId == d.resourceId;
			eq = eq && this.taskId == d.taskId;
			eq = eq && this.timeId == d.timeId;
			eq = eq && this.type == d.type;
		}

		return eq;
	}

}
