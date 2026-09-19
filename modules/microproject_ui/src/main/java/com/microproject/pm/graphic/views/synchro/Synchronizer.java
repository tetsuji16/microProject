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
package com.microproject.pm.graphic.views.synchro;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JScrollPane;

/**
 *
 */
public class Synchronizer {
	protected ArrayList scrollPaneSynchronizers;
	/**
	 * 
	 */
	public Synchronizer() {
	    scrollPaneSynchronizers=new ArrayList();
	}

	
	public void addSynchro(JScrollPane scrollPane1,
			JScrollPane scrollPane2, int orientation,boolean bottomBarActivated,boolean bottomBarEnabled){
		if (scrollPane1 == null || scrollPane2 == null) {
			return;
		}
		for (Iterator existing = scrollPaneSynchronizers.iterator(); existing.hasNext();) {
			ScrollPaneSynchronizer synchronizer = (ScrollPaneSynchronizer) existing.next();
			if (synchronizer.getScrollPane1() == scrollPane1
					&& synchronizer.getScrollPane2() == scrollPane2
					&& synchronizer.getOrientation() == orientation) {
				return;
			}
		}
	    ScrollPaneSynchronizer s=new ScrollPaneSynchronizer(scrollPane1,scrollPane2,orientation);
	    s.setBottomBarActivated(bottomBarActivated);
	    s.setBottomBarEnabled(bottomBarEnabled);
	    s.activateSynchro();
	    scrollPaneSynchronizers.add(s);
	}
	public void addSynchro(JScrollPane scrollPane1,
			JScrollPane scrollPane2, int orientation){
		addSynchro(scrollPane1,scrollPane2,orientation,true,false);
	}
	public void removeSynchro(JScrollPane scrollPane1,
			JScrollPane scrollPane2, int orientation){
	    for (Iterator i=scrollPaneSynchronizers.iterator();i.hasNext();){
	        ScrollPaneSynchronizer s=(ScrollPaneSynchronizer)i.next();
	        if ((s.getScrollPane1() == scrollPane1)&&(s.getScrollPane2() == scrollPane2)&&(s.getOrientation()==orientation)){
	            i.remove();
	            s.deactivateSynchro();
	        }
	    }
	}
	
}

