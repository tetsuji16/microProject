/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.rtf.RTFEditorKit;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.contour.ContourTypes;
import com.microproject.strings.Messages;
import com.microproject.ui.util.PopupDialogSupport;

/** Assignment-scoped editing surface for usage views. Values are committed together on OK. */
public final class AssignmentInformationDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	private static final ContourTypes.Kind[] CONTOURS = ContourTypes.Kind.values();
	private final Assignment assignment;
	private final boolean readOnly;
	private final JSpinner units = new JSpinner(new SpinnerNumberModel(100.0, 0.0, 10000.0, 1.0));
	private final JComboBox<ContourTypes.Kind> contour = new JComboBox<>(CONTOURS);
	private final JTextPane notes = new JTextPane();
	private final String originalNotes;
	private boolean notesChanged;

	public AssignmentInformationDialog(Frame owner, Assignment assignment) {
		super(owner, Messages.getString("AssignmentInformationDialog.title"), false);
		this.assignment = java.util.Objects.requireNonNull(assignment);
		this.readOnly = assignment.isReadOnly();
		this.originalNotes = assignment.getNotes();
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
		readNotes(notes, assignment.getNotes());
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
		JPanel tools = new JPanel();
		JButton bold = new JButton("B");
		bold.setName("assignmentNotesBold");
		bold.addActionListener(event -> toggleNoteStyle(StyleConstants.Bold));
		JButton italic = new JButton("I");
		italic.setName("assignmentNotesItalic");
		italic.addActionListener(event -> toggleNoteStyle(StyleConstants.Italic));
		JButton underline = new JButton("U");
		underline.setName("assignmentNotesUnderline");
		underline.addActionListener(event -> toggleNoteStyle(StyleConstants.Underline));
		tools.add(bold); tools.add(italic); tools.add(underline);
		bold.setEnabled(!readOnly); italic.setEnabled(!readOnly); underline.setEnabled(!readOnly);
		notesPanel.add(tools, BorderLayout.NORTH);
		notesPanel.add(new JScrollPane(notes), BorderLayout.CENTER);
		builder.addLabel(Messages.getString("AssignmentInformationDialog.notes"), cc.xy(1, 5, CellConstraints.TOP, CellConstraints.DEFAULT));
		builder.add(notesPanel, cc.xy(3, 5));
		return builder.getPanel();
	}

	private void toggleNoteStyle(Object style) {
		var input = notes.getInputAttributes();
		boolean enabled = style == StyleConstants.Bold ? !StyleConstants.isBold(input)
			: style == StyleConstants.Italic ? !StyleConstants.isItalic(input) : !StyleConstants.isUnderline(input);
		SimpleAttributeSet attributes = new SimpleAttributeSet();
		if (style == StyleConstants.Bold) StyleConstants.setBold(attributes, enabled);
		else if (style == StyleConstants.Italic) StyleConstants.setItalic(attributes, enabled);
		else StyleConstants.setUnderline(attributes, enabled);
		notes.setCharacterAttributes(attributes, false);
	}

	@Override
	public void onOk() {
		if (readOnly) {
			super.onOk();
			return;
		}
		try {
			units.commitEdit();
			double selectedUnits = ((Number) units.getValue()).doubleValue() / 100.0;
			if (!Double.isFinite(selectedUnits) || selectedUnits < 0 || selectedUnits > 100.0)
				throw new IllegalArgumentException("Assignment units must be between 0 and 100 percent");
			String selectedNotes = notesChanged ? writeNotes(notes) : originalNotes;
			ContourTypes.Kind selectedContour = (ContourTypes.Kind) contour.getSelectedItem();
			if (selectedContour == null) throw new IllegalArgumentException("A work contour is required");
			AssignmentInformationEditService.apply(assignment, selectedUnits, selectedContour, selectedNotes, this);
			super.onOk();
		} catch (Exception failure) {
					PopupDialogSupport.showMessageDialog(this, failure.getMessage(),
						Messages.getString("AssignmentInformationDialog.title"), JOptionPane.ERROR_MESSAGE);
				}
	}

	static void readNotes(JTextPane pane, String value) {
		String raw = value == null ? "" : value;
		if (!raw.stripLeading().startsWith("{\\rtf")) {
			pane.setText(raw);
			return;
		}
		try {
			RTFEditorKit kit = new RTFEditorKit();
			var document = kit.createDefaultDocument();
			kit.read(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)), document, 0);
			pane.setDocument(document);
		} catch (Exception malformedRtf) {
			pane.setText(raw);
		}
	}

	static String writeNotes(JTextPane pane) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new RTFEditorKit().write(output, pane.getDocument(), 0, pane.getDocument().getLength());
		return output.toString(StandardCharsets.UTF_8);
	}
}
