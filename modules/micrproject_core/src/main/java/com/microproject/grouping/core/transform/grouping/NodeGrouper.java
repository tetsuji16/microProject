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
package com.microproject.grouping.core.transform.grouping;

import java.util.ArrayList;
import java.util.List;

import com.microproject.field.InvalidFormulaException;
import com.microproject.grouping.core.transform.CommonTransform;
import com.microproject.grouping.core.transform.CommonTransformFactory;

/**
 *
 */
public class NodeGrouper extends CommonTransformFactory{
//	static Log log = LogFactory.getLog(NodeGrouper.class);
	protected String type = null; 
	protected List groups=new ArrayList();
	
	private static int count=0;
	
	
	
	public CommonTransform getTransform() throws InvalidFormulaException{
	    if (groups.size()==0) return null;
		NodeGrouper copy = new NodeGrouper();
		copy.type = type;
		for (Object group : groups) {
			copy.groups.add(group instanceof NodeGroup nodeGroup ? nodeGroup.copyForSession() : group);
		}
		setProperties(copy);
		return copy;
	}
	
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    
    public void addGroup(Object group){
    	groups.add(group);
    }
    
    public List getGroups(){
    	return groups;
    }
    
   
}
