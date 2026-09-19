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
package com.microproject.grouping.core.transform.transformer;

import java.util.function.Consumer;


import com.microproject.grouping.core.transform.CommonTransform;


/**
 *
 */
public abstract class NodeTransformer extends CommonTransform{
    public boolean isShowEmptyLines(){return false;}
    public void setShowEmptyLines(boolean showEmptyLines){}
    public boolean isShowEndEmptyLines(){return false;}
    public void setShowEndEmptyLines(boolean showEndEmptyLines){}
    public boolean isShowSummary(){return false;}
    public void setShowSummary(boolean showSummary){}
    public void setRedefinitionCallBack(Consumer<Object> callback){}
    public boolean isPreserveHierarchy() {return false;}
	public boolean isShowAssignments() {return true;}
	public boolean isShowEmptySummaries()  {return true;}
	public void setPreserveHierarchy(boolean preserveHierarchy) {}
	public void setShowAssignments(boolean showAssignments) {}
	public void setShowEmptySummaries(boolean showEmptySummaries) {}
	
	public abstract Object evaluate(Object node);
}
