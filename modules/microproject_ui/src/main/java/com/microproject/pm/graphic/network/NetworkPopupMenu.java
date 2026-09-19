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
package com.microproject.pm.graphic.network;

import com.microproject.util.DataUtils;
import java.util.function.Consumer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import org.apache.commons.collections.CollectionUtils;

import com.microproject.pm.graphic.graph.GraphInteractor;
import com.microproject.pm.graphic.graph.GraphModel;
import com.microproject.pm.graphic.graph.GraphPopupMenu;
import com.microproject.graphic.configuration.BarStyle;
import com.microproject.strings.Messages;


/**
 *
 */
public class NetworkPopupMenu extends GraphPopupMenu{
	private static final long serialVersionUID = -3722404084932322411L;


	private class BarMenuAction extends JRadioButtonMenuItem implements ActionListener {
		private static final long serialVersionUID = 8977876028186923345L;
		BarStyle style;
    	
    	BarMenuAction(final BarStyle style) {
    		super(style.getName());
    		this.style = style;
    		setSelected(style.isActive());
    		addActionListener(this);
    	}
    	public void actionPerformed(ActionEvent arg0) {
    	    style.setActive(isSelected());
    	    ((GraphModel)interactor.getGraph().getModel()).updateAll(true);
    	}
    }
    
    
    
    public NetworkPopupMenu(final GraphInteractor interactor) {
        super(interactor);
    }
    
	
/**
 * Because the styles may change, rebuild the menu each time
 *
 */
	protected void init() {
    	removeAll();
        final JMenu bars=new JMenu(Messages.getString("Network.Popup.barStylesMenu"));
		DataUtils.forAllDo(interactor.getGraph().getBarStyles().getRows().iterator(), new Consumer<Object>() { public void accept(Object arg0) {
				BarStyle barStyle = (BarStyle)arg0;
				BarMenuAction menuAction =new BarMenuAction(barStyle); 
				bars.add(menuAction);
				
			}
		});
        add(bars);
    	
    }

}

