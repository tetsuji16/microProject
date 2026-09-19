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
package com.microproject.dialog;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;


import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.ComponentFactory;
import com.microproject.dialog.util.ExtDateField;
import com.microproject.grouping.core.transform.CommonTransform;
import com.microproject.grouping.core.transform.TransformParameter;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.DateTime;

public final class TransformParameterDialog extends AbstractDialog implements Consumer<Object>{


	List<String> labels=new ArrayList<>();
	List<ExtDateField> valueComponents=new ArrayList<>();
	CommonTransform transform;

	public TransformParameterDialog() {
		super();
		setModal(true);
	}
	
	public void accept(Object obj) {
	    labels.clear();
	    valueComponents.clear();
	    transform=(CommonTransform)obj;
	    for (TransformParameter param : parameters()){
	        labels.add(/*new JLabel(*/Messages.getString(param.getId())/*)*/);
	        ExtDateField dateChooser= new ExtDateField(); //ComponentFactory.createDateField(); 
	        
	        Date date = param.getValue() == null ? new Date(DateTime.midnightToday()) : new Date((Long)param.getValue());
	        dateChooser.setValue(date);
	        valueComponents.add(dateChooser);
	    }
	    clearComponents();

	    pack();
		bind(true);
		setLocationRelativeTo(getParent());//to center on screen
		setVisible(true);
		if (getDialogResult() != JOptionPane.CANCEL_OPTION){
			bind(false);
		}
	}
	

	protected void initControls() {
		bind(true);
	}

	protected boolean bind(boolean get) {
		if (get) {
		} else {
	    for (int i = 0; i < parameters().size(); i++) {
	        TransformParameter param = parameters().get(i);
	        ExtDateField comp = valueComponents.get(i);
		    long d = DateTime.gmt(comp.getDateValue());
		    param.setValue(d);
		    transform.setParameter(param);
		}
		}
		return true;
	}

	// Building *************************************************************

	/**
	 * Builds the panel. Initializes and configures components first, then
	 * creates a FormLayout, configures the layout, creates a builder, sets a
	 * border, and finally adds the components.
	 * 
	 * @return the built panel
	 */

	public JComponent createContentPanel() {
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		initControls();
		FormLayout layout = new FormLayout("default, 3dlu, default", // cols
				FlatUiSupport.preferredFormRows(10)); // rows
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		
		for (int i = 0; i < labels.size(); i++){
		    String name = labels.get(i);
		    JComponent comp = valueComponents.get(i);
		    builder.append(name,comp);
		    builder.nextLine(2);
		}
		return builder.getPanel();
	}

	@SuppressWarnings("unchecked")
	private List<TransformParameter> parameters() {
		return (List<TransformParameter>) transform.getParameters();
	}

}
