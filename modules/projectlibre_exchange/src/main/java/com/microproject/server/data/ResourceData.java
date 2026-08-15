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
public class ResourceData extends SerializedDataObject {
	static final long serialVersionUID = 2637888382782L;
    protected EnterpriseResourceData enterpriseResource;
    protected ResourceData parentResource;
    protected long childPosition;
    protected long parentResourceId=-1;
    protected int role;
    
    public static final SerializedDataObjectFactory FACTORY=new SerializedDataObjectFactory(){
        public SerializedDataObject createSerializedDataObject(){
            return new ResourceData();
        }
    };
    
    public EnterpriseResourceData getEnterpriseResource() {
        return enterpriseResource;
    }
    public void setEnterpriseResource(EnterpriseResourceData enterpriseResource) {
        this.enterpriseResource = enterpriseResource;
        setUniqueId((enterpriseResource==null)?-1L:enterpriseResource.getUniqueId());
    }
    
    public long getChildPosition() {
        return (enterpriseResource==null)?childPosition:enterpriseResource.getChildPosition();
    }
    public void setChildPosition(long childPosition) {
        this.childPosition = childPosition;
    }
    public ResourceData getParentResource() {
        return parentResource;
    }
    public void setParentResource(ResourceData parentResource) {
        this.parentResource = parentResource;
        setParentResourceId((parentResource==null)?-1L:parentResource.getUniqueId());
    }
    public int getType(){
        return DataObjectConstants.RESOURCE_TYPE;
    }
    
    public long getParentResourceId() {
		return parentResourceId;
	}
	public void setParentResourceId(long parentResourceId) {
		this.parentResourceId = parentResourceId;
	}

	public int getRole() {
		return role;
	}
	public void setRole(int role) {
		this.role = role;
	}
	
	
	
	public void emtpy(){
    	super.emtpy();
    	parentResource=null;
    	enterpriseResource=null;
    }
	public String toString() {
		return name + "/" +getUniqueId() + " hash " + this.hashCode();
	}
}
