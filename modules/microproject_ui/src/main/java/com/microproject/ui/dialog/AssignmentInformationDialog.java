/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.application.AssignmentInformationEditService;
import com.microproject.model.assignment.Assignment;
import com.microproject.model.assignment.contour.ContourTypes;
import com.microproject.i18n.Messages;
import com.microproject.ui.util.PopupDialogSupport;

/** Assignment-scoped editing surface for usage views. Values are committed together on OK. */
public final class AssignmentInformationDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	private static final ContourTypes.Kind[] CONTOURS = ContourTypes.Kind.values();
	private final Assignment assignment;
	private final boolean readOnly;
	private final JSpinner units = new JSpinner(new SpinnerNumberModel(100.0, 0.0, 10000.0, 1.0));
	private final JComboBox<ContourTypes.Kind> contour = new JComboBox<>(CONTOURS);
	private final JTextArea notes = new JTextArea();
	private final String originalNotes;
	private final double originalUnitsPercent;
	private boolean notesChanged;

	public AssignmentInformationDialog(Frame owner, Assignment assignment) {
		super(owner, Messages.getString("AssignmentInformationDialog.title"), false);
		this.assignment = java.util.Objects.requireNonNull(assignment);
		this.readOnly = assignment.isReadOnly();
		this.originalNotes = assignment.getNotes();
		this.originalUnitsPercent = assignment.getUnits() * 100.0;
		setName("assignmentInformationDialog");
		setObjectValues();
		notes.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent event) { notesChanged = true; }
			@Override public void removeUpdate(DocumentEvent event) { notesChanged = true; }
			@Override public void changedUpdate(DocumentEvent event) { notesChanged = true; }
		});
		String taskName = assignment.getTask() == null ? "" : assignment.getTask().getName();
		String resourceName = assignment.getResource() == null ? "" : assignment.getResource().getName();
		setTitle(Messages.getString("AssignmentInformationDialog.title") + " - " + taskName + " / " + resourceName);
		addDocHelp("Assignment_Information_Dialog");
	}

	private void setObjectValues() {
		units.setValue(assignment.getUnits() * 100.0);
		contour.setSelectedItem(assignment.getWorkContourTypeKind());
		notes.setText(assignment.getNotes() == null ? "" : assignment.getNotes());
		units.setName("assignmentUnits");
		contour.setName("assignmentWorkContour");
		notes.setName("assignmentNotes");
		contour.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
				int index, boolean selected, boolean focused) {
				super.getListCellRendererComponent(list, value, index, selected, focused);
				setText(value instanceof ContourTypes.Kind kind
					? Messages.getString("AssignmentInformationDialog.contour." + kind.name()) : "");
				return this;
			}
		});
		units.setEnabled(!readOnly);
		contour.setEnabled(!readOnly);
		notes.setEditable(!readOnly);
	}

	@Override
	public JComponent createContentPanel() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("right:pref, 8dlu, 280dlu:grow", "pref, 6dlu, pref, 6dlu, fill:180dlu:grow"));
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addLabel(Messages.getString("AssignmentInformationDialog.units"), cc.xy(1, 1));
		builder.add(units, cc.xy(3, 1));
		builder.addLabel(Messages.getString("AssignmentInformationDialog.workContour"), cc.xy(1, 3));
		builder.add(contour, cc.xy(3, 3));
		JPanel notesPanel = new JPanel(new BorderLayout(0, 4));
		notesPanel.add(new JScrollPane(notes), BorderLayout.CENTER);
		builder.addLabel(Messages.getString("AssignmentInformationDialog.notes"), cc.xy(1, 5, CellConstraints.TOP, CellConstraints.DEFAULT));
		builder.add(notesPanel, cc.xy(3, 5));
		return builder.getPanel();
	}

	@Override
	public void onOk() {
		if (readOnly) {
			super.onOk();
			return;
		}
		try {
			double selectedUnits = originalUnitsPercent;
			if (!Objects.equals(units.getValue(), originalUnitsPercent)) {
				units.commitEdit();
				selectedUnits = ((Number) units.getValue()).doubleValue();
			}
			selectedUnits /= 100.0;
			if (!Double.isFinite(selectedUnits) || selectedUnits < 0 || selectedUnits > 100.0)
				throw new IllegalArgumentException(Messages.getString("AssignmentInformationDialog.validation.units"));
			String selectedNotes = notesChanged ? notes.getText() : originalNotes;
			ContourTypes.Kind selectedContour = (ContourTypes.Kind) contour.getSelectedItem();
			if (selectedContour == null) throw new IllegalArgumentException(Messages.getString("AssignmentInformationDialog.validation.contour"));
			AssignmentInformationEditService.apply(assignment, selectedUnits, selectedContour, selectedNotes, this);
			super.onOk();
		} catch (Exception failure) {
			String message = failure instanceof AssignmentInformationEditService.AssignmentReadOnlyException
				? Messages.getString("AssignmentInformationDialog.readOnly") : failure.getMessage();
			PopupDialogSupport.showMessageDialog(this, message == null ? failure.toString() : message,
				Messages.getString("AssignmentInformationDialog.title"), javax.swing.JOptionPane.ERROR_MESSAGE);
		}
	}
}
