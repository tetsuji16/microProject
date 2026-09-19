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
package com.microproject.pm.graphic.model.cache;

import java.awt.geom.GeneralPath;

import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyType;

/**
 *
 */
public class GraphicDependency /*extends GraphicNode*/{
	protected GraphicNode predecessor=null;
	protected GraphicNode successor=null;
	protected Dependency dependency;
	protected boolean dirty;

	protected GeneralPath path=null;



	/**
	 *
	 */
	public GraphicDependency(GraphicNode predecessor,GraphicNode successor,Dependency dependency) {
		//super(new NodeBridge(dependency));
		this.predecessor=predecessor;
		this.successor=successor;
		this.dependency=dependency;
		dirty=false;
	}



	/**
	 * @return Returns the predecessor.
	 */
	public GraphicNode getPredecessor() {
		return predecessor;
	}
	/**
	 * @return Returns the successor.
	 */
	public GraphicNode getSuccessor() {
		return successor;
	}

	public Dependency getDependency() {
		return dependency;
	}

	public int getType(){
		return (dependency==null)?DependencyType.FS:dependency.getDependencyType();
	}


	public String toString(){
	    return dependency.toString();
	}






	public GeneralPath getPath() {
		if (path==null) path=new GeneralPath();
		return path;
	}


    public boolean isDirty() {
        return dirty;
    }
    public void setDirty(boolean dirty) {
//		System.out.println("GraphicDependency _setDirty");
        this.dirty = dirty;
    }
}

