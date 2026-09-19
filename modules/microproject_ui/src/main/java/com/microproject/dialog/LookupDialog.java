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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.frames.MainFrameFactory;
import com.microproject.field.Field;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

public final class LookupDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(LookupDialog.class.getName());
	// use property utils to copy to project like struts
	Field field;
	JComboBox types;
	JList results;
	JScrollPane resultsPane;
	JTextField match;
	JButton find;
	JButton removeButton;
	LinkedHashMap<String, Object> resultMap;
	String key;
	String value;
	public static String[] getKeyAndValue(Field f) {
		LookupDialog dlg = new LookupDialog(f);
		dlg.initControls();
		if (dlg.doModal()) {
			return new String[] {dlg.key,dlg.value};
		}
		return null;
		
	}
	public ButtonPanel createButtonPanel() {
		AbstractAction action = new AbstractAction(Messages.getString("Text.Remove")) { //$NON-NLS-1$
			public void actionPerformed(ActionEvent e) {
				remove();
			}
		};
		removeButton = new JButton(action);
		
		createOkCancelButtons();
		ButtonPanel buttonPanel = new ButtonPanel();
		buttonPanel.addButton(removeButton);
		buttonPanel.addButton(ok);
		buttonPanel.addButton(cancel);
		return buttonPanel;
	}   	
	private void remove() {
		value= null;
		key = null;
		super.onOk();
	}

	@Override
	public void onOk() {
		int index = results.getSelectedIndex();
		if (index != -1) {
			value = (String) results.getSelectedValue();
			key = keyForValue(value);
		}
		super.onOk();
	}

	private String keyForValue(String value) {
		for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
			if (value == entry.getValue())
				return entry.getKey();
		}
		return null;
	}
	private LookupDialog(Field field) {
		super(MainFrameFactory.getMainFrame(), Messages.getString("LookupDialog.LookupAnObject"),true); //$NON-NLS-1$
		this.field = field;
	}
	protected boolean initialOkEnabledState() {
		return false;
	}

	/**
	 * Creates, intializes and configures the UI components. Real applications
	 * may further bind the components to underlying models.
	 */
	protected void initControls() {
		types = new JComboBox(field.getLookupTypes().split(";", -1)); //$NON-NLS-1$
		results = new JList();
		results.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
//		results.setVisibleRowCount(15);
		resultsPane = new JScrollPane(results);

		match = new JTextField();
		match.setToolTipText(Messages.getString("LookupDialog.EnterPartOfTheName")); //$NON-NLS-1$
		find = new JButton(Messages.getString("LookupDialog.Find")); //$NON-NLS-1$
		find.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Session session = SessionFactory.getInstance().getSession(false);
				if (session != null)
					try {
						resultMap = (LinkedHashMap<String, Object>)SessionFactory.call(session,"queryLike",new Class[]{String.class,String.class},new Object[]{ types.getSelectedItem(), match.getText()});
						results= new ActionJList(resultMap.values().toArray());
						((ActionJList)results).addActionListener(new ActionListener(){
							public void actionPerformed(ActionEvent e) {
								onOk();
							}});
						if (resultMap.isEmpty()) {
							resultsPane.getViewport().add(new JLabel(Messages.getString("LookupDialog.NoMatchesFound"))); //$NON-NLS-1$
							resultsPane.setEnabled(false);
							ok.setEnabled(false);
						} else {
							resultsPane.setEnabled(true);
							resultsPane.getViewport().add(results);
							ok.setEnabled(true);
						}
					} catch (Exception e1) {
						Alert.error(Messages.getString("LookupDialog.UnableToContactServer")); //$NON-NLS-1$
						logger.log(Level.WARNING, "Lookup dialog search failed", e1);
					}
			}});
	}


	// Component Creation and Initialization **********************************



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
		FormLayout layout = new FormLayout("p, 3dlu, p,20dlu,p,3dlu,160dlu:grow,3dlu,p", // cols //$NON-NLS-1$
				"p, 3dlu, p,3dlu,fill:default:grow"); // rows //$NON-NLS-1$

		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.append(Messages.getString("LookupDialog.Type"),types); //$NON-NLS-1$
		builder.append(Messages.format("Format.label", Messages.getString("LookupDialog.Find")),match); //$NON-NLS-1$
		builder.append(find);
		builder.nextLine(2);
		builder.append(Messages.getString("LookupDialog.Results")); //$NON-NLS-1$
		builder.nextLine(2);
		builder.add(resultsPane,cc.xyw(builder.getColumn(), builder.getRow(), 9));
		return builder.getPanel();
	}


}
