/*******************************************************************************
 * MIT License
 *
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
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
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.Alert;
import com.microproject.util.BrowserControl;
import com.microproject.util.ClassLoaderUtils;
import com.microproject.util.VersionUtils;

public final class FindDialog extends AbstractDialog implements ObjectEvent.Listener,DocumentSelectedEvent.Listener{
	private static final long serialVersionUID = 1L;
	JComboBox<Field> combo;
	JTextField search  = new JTextField(30);
	JCheckBox caseSensitive = new JCheckBox(Messages.getString("FindDialog.MatchCase")); //$NON-NLS-1$
	DocumentFrame documentFrame;
	SearchContext context;
	JButton next;
	JComboBox<String> direction;
	public static FindDialog getInstance(DocumentFrame owner,Searchable searchable, Field current) {
		return new FindDialog(owner,searchable,current);
	}


	private Searchable searchable;

	private FindDialog(DocumentFrame documentFrame, Searchable searchable, Field field) {
		super(documentFrame.getGraphicManager().getFrame(), Messages.getString("FindDialog.Find"), false); //$NON-NLS-1$ //$NON-NLS-2$
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
		l.sort(null);
		ComboBoxModel<Field> m = new DefaultComboBoxModel<>(l.toArray(Field[]::new));
		if (combo == null)
			combo = new JComboBox<>(m);
		else
			combo.setModel(m);

		bind(true);
		updateFindButtonState();
		search.requestFocus();
	}

	public void onOk() {
		bind(false);
		if (!searchable.findNext(context))
			Alert.warn(Messages.getString("FindDialog.NoMatches"));
	}

	protected boolean bind(boolean get) {
		if (get) {
			if (context.getField() != null)
				combo.setSelectedItem(context.getField());
			if (search != null)
				search.setText("");
			caseSensitive.setSelected(context.isCaseSensitive());

		} else {
			context.setField((Field) combo.getSelectedItem());
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
		// Separating the component initialization and configuration
		// from the layout code makes both parts easier to read.
		FormLayout layout = new FormLayout("default, 3dlu, default, 3dlu, default", // cols //$NON-NLS-1$
				FlatUiSupport.preferredFormRows(7)); // rows //$NON-NLS-1$



		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();



		builder.append(Messages.getString("FindDialog.Find"),search); //$NON-NLS-1$
		builder.nextLine(2);
		builder.append(Messages.getString("Text.Field"),combo); //$NON-NLS-1$
		builder.nextLine(2);
		if (direction == null) {
			direction = new JComboBox<>(new String[] {
				Messages.getString("FindDialog.Down"), Messages.getString("FindDialog.Up") }); //$NON-NLS-1$
			direction.setName("findDialogDirection");
		}
		builder.append(Messages.getString("FindDialog.Search"),direction); //$NON-NLS-1$
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
	}

	@Override
	public ButtonPanel createButtonPanel() {
		ButtonPanel buttonPanel = new ButtonPanel();
		next = new JButton(Messages.getString("FindDialog.FindNext"));
		next.setName("findDialogFindNext");
		next.setToolTipText(Messages.getString("FindDialog.FindNext.tooltip"));
		FlatUiSupport.styleDialogButton(next, true);
		JButton close = new JButton(Messages.getString("ButtonText.Close"));
		close.setName("findDialogClose");
		FlatUiSupport.styleDialogButton(close, false);
		buttonPanel.add(next);
		buttonPanel.add(close);
		updateFindButtonState();
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				context.setForward(direction == null || direction.getSelectedIndex() == 0);
				onOk();
			}});
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}});
		search.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent event) { updateFindButtonState(); }
			@Override public void removeUpdate(DocumentEvent event) { updateFindButtonState(); }
			@Override public void changedUpdate(DocumentEvent event) { updateFindButtonState(); }
		});
		return buttonPanel;
	}

	private void updateFindButtonState() {
		if (next == null)
			return;
		next.setEnabled(search.getText().length() != 0);

	}
	@Override
	public void pack() {
		super.pack();
		if (next != null)
			getRootPane().setDefaultButton(next);
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

