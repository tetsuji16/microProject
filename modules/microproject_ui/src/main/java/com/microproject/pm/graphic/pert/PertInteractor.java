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
package com.microproject.pm.graphic.pert;

import java.awt.geom.GeneralPath;

import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.network.NetworkInteractor;
import com.microproject.association.InvalidAssociationException;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.dependency.HasDependencies;
import com.microproject.util.Alert;

/**
 *
 */
public class PertInteractor extends NetworkInteractor {

	/**
	 * @param ui
	 */
	public PertInteractor(GraphUI ui) {
		super(ui);
	}

	
    protected boolean switchOnLinkCreation(double x, double y){
    	if (state!=BAR_SELECTION) return false;
		GraphicNode node=(GraphicNode)selected;
		GeneralPath shape=getShape(node);
		if (shape==null) return false;
		return (node.getNode().getImpl() instanceof HasDependencies&&!shape.contains(x,y));
    }
    public boolean executeAction(double x,double y){
    	if (super.executeAction(x,y)) return true;
    	if (selected==null) return false;
    	switch (state) {
		case LINK_CREATION:
			try {
					if (sourceNode!=null&&destinationNode!=null&&
							sourceNode.getNode().getImpl() instanceof HasDependencies &&
							destinationNode.getNode().getImpl() instanceof HasDependencies){
						DependencyService.getInstance().newDependency((HasDependencies)sourceNode.getNode().getImpl(),(HasDependencies)destinationNode.getNode().getImpl(),DependencyType.FS,0,this);
					}
				} catch (InvalidAssociationException e) {
					Alert.error(e.getMessage());
				}
			return true;
		case LINK_SELECTION:
			showDependencyPropertiesDialog((GraphicDependency)selected);
			return true;

    	}
    	return false;
    }

}

