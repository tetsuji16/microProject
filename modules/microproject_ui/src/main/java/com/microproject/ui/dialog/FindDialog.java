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
package com.microproject.ui.dialog;

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
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.frame.DocumentSelectedEvent;
import com.microproject.ui.view.spreadsheet.common.CommonSpreadSheet;
import com.microproject.ui.view.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.ui.view.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.ui.view.views.SearchContext;
import com.microproject.ui.view.views.Searchable;
import com.microproject.configuration.Configuration;
import com.microproject.document.ObjectEvent;
import com.microproject.field.Field;
import com.microproject.i18n.Messages;
import com.microproject.ui.util.FlatUiSupport;
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
	/** Replace mode (Find and Replace); everything below is created lazily and null-guarded. */
	boolean replaceMode;
	JTextField replaceWith;
	JLabel replaceLabel;
	JButton replace;
	JButton replaceAll;
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

	/** Switches between plain Find and Find and Replace; may be called before or after the dialog is built. */
	public void setReplaceMode(boolean replaceMode) {
		this.replaceMode = replaceMode;
		setTitle(Messages.getString(replaceMode ? "FindDialog.Replace" : "FindDialog.Find")); //$NON-NLS-1$ //$NON-NLS-2$
		if (replaceLabel != null)
			replaceLabel.setVisible(replaceMode);
		if (replaceWith != null)
			replaceWith.setVisible(replaceMode);
		if (replace != null)
			replace.setVisible(replaceMode);
		if (replaceAll != null)
			replaceAll.setVisible(replaceMode);
		updateFindButtonState();
	}

	public boolean isReplaceMode() {
		return replaceMode;
	}

	/** Replaces the current match, then moves to the next one. */
	void onReplace() {
		bind(false);
		context.setForward(true);
		searchable.replace(context, replaceWith == null ? "" : replaceWith.getText(), false);
		if (!searchable.findNext(context))
			Alert.warn(Messages.getString("FindDialog.NoMatches")); //$NON-NLS-1$
	}

	void onReplaceAll() {
		bind(false);
		int count = searchable.replace(context, replaceWith == null ? "" : replaceWith.getText(), true);
		Alert.warn(count == 0 ? Messages.getString("FindDialog.NoMatches") //$NON-NLS-1$
			: Messages.getString("FindDialog.Replaced") + " " + count); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void onOk() {
		bind(false);
		context.setForward(direction == null || direction.getSelectedIndex() == 0);
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
				FlatUiSupport.preferredFormRows(9)); // rows //$NON-NLS-1$



		// Create a builder that assists in adding components to the container.
		// Wrap the panel with a standardized border.
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();



		builder.append(Messages.getString("FindDialog.Find"),search); //$NON-NLS-1$
		builder.nextLine(2);
		if (replaceWith == null) {
			replaceWith = new JTextField(30);
			replaceWith.setName("findDialogReplaceWith");
			replaceLabel = new JLabel(Messages.getString("FindDialog.ReplaceWith")); //$NON-NLS-1$
		}
		replaceLabel.setVisible(replaceMode);
		replaceWith.setVisible(replaceMode);
		builder.append(replaceLabel, replaceWith);
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
		replace = new JButton(Messages.getString("FindDialog.Replace")); //$NON-NLS-1$
		replace.setName("findDialogReplace");
		FlatUiSupport.styleDialogButton(replace, false);
		replace.setVisible(replaceMode);
		replace.addActionListener(e -> onReplace());
		replaceAll = new JButton(Messages.getString("FindDialog.ReplaceAll")); //$NON-NLS-1$
		replaceAll.setName("findDialogReplaceAll");
		FlatUiSupport.styleDialogButton(replaceAll, false);
		replaceAll.setVisible(replaceMode);
		replaceAll.addActionListener(e -> onReplaceAll());
		buttonPanel.add(next);
		buttonPanel.add(replace);
		buttonPanel.add(replaceAll);
		buttonPanel.add(close);
		updateFindButtonState();
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
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
		boolean hasQuery = search.getText().length() != 0;
		next.setEnabled(hasQuery);
		if (replace != null)
			replace.setEnabled(hasQuery);
		if (replaceAll != null)
			replaceAll.setEnabled(hasQuery);

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

