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

import java.util.Date;

import com.microproject.field.FieldContext;

/**
 * 
 */
public class DataObjectImpl extends CommonDataObject{
	static final long serialVersionUID = 2789999282666L;
   protected String name;
    protected long id=-1;
    protected Date created;
    
    public DataObjectImpl() {
    }
    
    public DataObjectImpl(String name) { //id is set by ejbs
        this.name=name;
    }
    

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean equals(Object o){
        if (o instanceof DataObjectImpl){
            DataObjectImpl dob=(DataObjectImpl)o;
            if (id!=dob.getUniqueId()) return false;
            //if ((name==null&&dob.getName()!=null)||(name!=null&&dob.getName()==null)) return false;
            //if (!name.equals(dob.getName())) return false;
            return true;
        } else return false;
        
    }

    public String toString() {
    	return getName();
    }
	public String getName(FieldContext context) {
		return getName();
	}

	public Date getCreated() {
		return created;
	}


	public void setCreated(Date created) {
		this.created = created;
	}
}
