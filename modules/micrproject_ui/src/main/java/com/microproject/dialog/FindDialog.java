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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.DocumentSelectedEvent;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.pm.graphic.views.SearchContext;
import com.microproject.pm.graphic.views.Searchable;
import com.microproject.configuration.Configuration;
import com.microproject.document.ObjectEvent;
import com.microproject.field.Field;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.BrowserControl;
import com.microproject.util.ClassLoaderUtils;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.VersionUtils;

public final class FindDialog extends AbstractDialog implements ObjectEvent.Listener,DocumentSelectedEvent.Listener{
	private static final long serialVersionUID = 1L;
	JComboBox combo;
	JTextField search  = new JTextField(30);
	JCheckBox caseSensitive = new JCheckBox(Messages.getString("FindDialog.MatchCase")); //$NON-NLS-1$
	DocumentFrame documentFrame;
	SearchContext context;
	JButton next;
	JButton previous;
	public static FindDialog getInstance(DocumentFrame owner,Searchable searchable, Field current) {
		return new FindDialog(owner,searchable,current);
	}


	private Searchable searchable;

	private FindDialog(DocumentFrame documentFrame, Searchable searchable, Field field) {
		super(documentFrame.getGraphicManager().getFrame(), Messages.getString("LookupDialog.Find"), false); //$NON-NLS-1$ //$NON-NLS-2$
		DocumentSelectedEvent.addListener(this);
		init(searchable,field);

	}

	public void init(Searchable searchable, Field field) {
		this.searchable=searchable;
		context = searchable.createSearchContext();
		if (field != null)
			context.setField(field);
		Collection<Field> availableFields = searchable.getAvailableFields();
		ArrayList<Field> l = new ArrayList<>(availableFields.size());
		l.addAll(availableFields);
		Collections.sort(l);
		ComboBoxModel m = new DefaultComboBoxModel(l.toArray());
		if (combo == null)
			combo = new JComboBox(m);
		else
			combo.setModel(m);

		bind(true);
		updateFindButtonState();
		search.requestFocus();
		//combo.invalidate();

	}

	public void onOk() {
		bind(false);
		if (!searchable.findNext(context))
			Alert.warn("No more matches");
	}

	protected boolean bind(boolean get) {
		if (get) {
			if (context.getField() != null)
				combo.setSelectedItem(context.getField());
			if (search != null)
				search.setText("");
			caseSensitive.setSelected(context.isCaseSensitive());

		} else {
			context.setField((Field)combo.getSelectedItem());
			context.setSearchValue(search.getText());
			context.setCaseSensitive(caseSensitive.isSelected());

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
//		initControls();
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		FormLayout layout = new FormLayout("default, 3dlu, default, 3dlu, default", // cols //$NON-NLS-1$
				"p, 3dlu, p, 3dlu, p"); // rows //$NON-NLS-1$



		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();



		builder.append(Messages.getString("LookupDialog.Find"),search); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(Messages.getString("Text.Field"),combo); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(caseSensitive); //$NON-NLS-1$
		return builder.getPanel();
	}

	public void setDocumentFrame(DocumentFrame documentFrame) {
		if (this.documentFrame != null)
		    this.documentFrame.getProject().removeObjectListener(this);
		this.documentFrame = documentFrame;
		documentFrame.getProject().addObjectListener(this);
	}

	public void documentSelected(DocumentSelectedEvent evt) {
		setVisible(false);
	}
	public void objectChanged(ObjectEvent objectEvent) {
//		if (objectEvent.getObject() instanceof Project)
//			setVisible(false);
	}

	@Override
	public ButtonPanel createButtonPanel() {
		ButtonPanel buttonPanel = new ButtonPanel();
		next = new JButton(Messages.getString("LookupDialog.Find"),IconManager.getIcon("image.down"));
		previous = new JButton(Messages.getString("LookupDialog.Find"),IconManager.getIcon("image.up"));
		FlatUiSupport.styleDialogButton(next, false);
		FlatUiSupport.styleDialogButton(previous, false);
		buttonPanel.add(next);
		buttonPanel.add(previous);
		updateFindButtonState();
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				context.setForward(true);
				onOk();
			}});
		previous.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				context.setForward(false);
				onOk();
			}});
		search.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				updateFindButtonState();			}

			public void keyTyped(KeyEvent e) {
			}});
		return buttonPanel;
	}

	private void updateFindButtonState() {
		if (next == null)
			return;
		boolean empty = search.getText().length() == 0;
		next.setEnabled(!empty);
		previous.setEnabled(!empty);

	}
	@Override
	protected boolean hasOkAndCancelButtons() {
		return false;
	}

	/** Initializes the dialog from the application-wide search box and selects the first match. */
	public void init(Searchable searchable, Field field, String initialQuery) {
		init(searchable, field);
		if (initialQuery == null || initialQuery.isBlank())
			return;
		search.setText(initialQuery);
		context.setForward(true);
		bind(false);
		updateFindButtonState();
		if (!searchable.findNext(context))
			Alert.warn(Messages.getString("FindDialog.NoMatches")); //$NON-NLS-1$
	}

	@Override
	public void dispose() {
		DocumentSelectedEvent.removeListener(this);
		if (documentFrame != null) {
			documentFrame.getProject().removeObjectListener(this);
			documentFrame = null;
		}
		super.dispose();
	}

}

