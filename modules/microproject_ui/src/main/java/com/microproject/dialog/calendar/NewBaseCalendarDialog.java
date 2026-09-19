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
package com.microproject.dialog.calendar;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.AbstractDialog;
import com.microproject.help.HelpUtil;
import com.microproject.configuration.Settings;
import com.microproject.document.Document;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;

public final class NewBaseCalendarDialog extends AbstractDialog {

	JTextField name;
	JRadioButton createNewBase;
	JRadioButton makeACopy;
	JComboBox calendarToCopy;
	ButtonGroup options;
	Document document;
	WorkingCalendar newCalendar = null;
	

	public static NewBaseCalendarDialog getInstance(Frame documentFrame, Document document) {
		return new NewBaseCalendarDialog(documentFrame, document);
	}

	private NewBaseCalendarDialog(Frame owner, Document document) {
		super(owner, Messages.getString("NewBaseCalendarDialog.NewBaseCalendar"), true); //$NON-NLS-1$
		this.document = document;
		addDocHelp("New_Base_Calendar");
	}

	protected void initControls() {
		name = new JTextField(); 
		createNewBase = new JRadioButton(Messages.getString("NewBaseCalendarDialog.CreateANewBaseCalendar")); //$NON-NLS-1$
		makeACopy = new JRadioButton(Messages.getString("NewBaseCalendarDialog.CreateACopyOfCalendar")); //$NON-NLS-1$
		calendarToCopy = new JComboBox();
		calendarToCopy.setEnabled(false);
		options = new ButtonGroup();
		options.add(createNewBase);
		options.add(makeACopy);
		createNewBase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
				calendarToCopy.setEnabled(false);
	    }});
		makeACopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
				calendarToCopy.setEnabled(true);
	    }});
		createNewBase.setSelected(true);
		bind(true);
	}
	
	protected boolean bind(boolean get) {
		CalendarService service = CalendarService.getInstance();
		if (get) {
			ComboBoxModel calModel = new DefaultComboBoxModel(service.getBaseCalendars().toArray());
			calendarToCopy.setModel(calModel);
		} else {
			WorkingCalendar toCopy;
			if (makeACopy.isSelected()) 
				toCopy = (WorkingCalendar) calendarToCopy.getSelectedItem();
			else
				toCopy = CalendarService.getInstance().getDefaultInstance();
			newCalendar = CalendarService.getInstance().makeScratchCopy(toCopy);
				
			newCalendar.setName(name.getText());
			service.add(newCalendar);
		}
		return super.bind(get);
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
		initControls();

		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		FormLayout layout = new FormLayout("p, 3dlu, max(160dlu;pref):grow", // cols //$NON-NLS-1$
				FlatUiSupport.preferredFormRows(9)); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(Messages.getString("NewBaseCalendarDialog.Name"), name); //$NON-NLS-1$
		builder.nextLine(2);
		builder.add(createNewBase, cc.xyw(builder.getColumn(), builder.getRow(), 3));
		builder.nextLine(2);
		builder.add(makeACopy, cc.xy(builder.getColumn(), builder.getRow()));
		builder.add(calendarToCopy, cc.xy(3, builder.getRow()));
		return builder.getPanel();
	}

	public final WorkingCalendar getNewCalendar() {
		return newCalendar;
	}
}
