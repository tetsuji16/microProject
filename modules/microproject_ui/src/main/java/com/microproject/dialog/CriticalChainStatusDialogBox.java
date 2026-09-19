/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.text.MessageFormat;

import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.ccpm.CriticalChainBufferHistory;
import com.microproject.pm.ccpm.CriticalChainBufferHistoryService;
import com.microproject.pm.ccpm.CriticalChainReportService;
import com.microproject.pm.graphic.views.CriticalChainBufferChartPanel;
import com.microproject.pm.graphic.views.CriticalChainGraphPanel;
import com.microproject.pm.task.Project;
import com.microproject.menu.MenuActionConstants;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.FlatLafDialog;
import com.microproject.util.PopupDialogSupport;

/** Read-only CCPM result surface used by the Report and View ribbon commands. */
public final class CriticalChainStatusDialogBox extends FlatLafDialog {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private CriticalChainBufferChartPanel bufferChart;
	private JButton retractObservation;
	private final JLabel feedback = new JLabel(" ");

	public enum Surface { BUFFER_STATUS, NETWORK }

	public static void show(Frame owner, Project project, Surface surface) {
		if (java.awt.GraphicsEnvironment.isHeadless()) return;
		new CriticalChainStatusDialogBox(owner, project, surface).setVisible(true);
	}

	private CriticalChainStatusDialogBox(Frame owner, Project project, Surface surface) {
		super(owner, UsabilityStrings.text(surface == Surface.NETWORK ? "ccpm.networkTitle" : "ccpm.bufferTitle"), true);
		this.project = project;
		FlatUiSupport.styleDialogRoot(getRootPane());
		PopupDialogSupport.bindEscapeToDispose(this);
		setLayout(new BorderLayout());
		installUndoRedoBindings();

		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.findSettings(project);
		if (settings == null || !settings.isEnabled() || service.findBaseline(project) == null) {
			add(new JLabel(UsabilityStrings.text("ccpm.noAppliedPlan")), BorderLayout.CENTER);
		} else {
			JPanel content = new JPanel(new BorderLayout());
			content.add(new JLabel(UsabilityStrings.text("ccpm.loading")), BorderLayout.CENTER);
			add(content, BorderLayout.CENTER);
			loadAnalysis(service, project, surface, content);
		}

		JButton close = new JButton(UsabilityStrings.text("common.close"));
		close.addActionListener(event -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		boolean hasAppliedPlan = settings != null && settings.isEnabled() && service.findBaseline(project) != null;
		if (!hasAppliedPlan) {
			JButton configure = new JButton(UsabilityStrings.text("ccpm.configure"));
			configure.setToolTipText(UsabilityStrings.text("ccpm.configureTooltip"));
			configure.getAccessibleContext().setAccessibleDescription(
				UsabilityStrings.text("ccpm.configureTooltip"));
			configure.addActionListener(event -> openSettingsAndReturn(owner, project, surface));
			buttons.add(configure);
		}
		if (hasAppliedPlan) {
			JButton csv = new JButton("CSV");
			csv.addActionListener(event -> exportReport(project, false));
			JButton html = new JButton("HTML");
			html.addActionListener(event -> exportReport(project, true));
			buttons.add(csv); buttons.add(html);
			if (surface == Surface.BUFFER_STATUS && project != null && !project.isReadOnly()) {
				retractObservation = new JButton(UsabilityStrings.text("ccpm.retractObservation"));
				retractObservation.setActionCommand(MenuActionConstants.ACTION_CCPM_BUFFER_OBSERVATION_RETRACT);
				retractObservation.setToolTipText(UsabilityStrings.text("ccpm.retractObservationTooltip"));
				retractObservation.setEnabled(false);
				retractObservation.addActionListener(event -> retractSelectedObservation());
				buttons.add(retractObservation);
			}
		}
		buttons.add(close);
		JPanel south = new JPanel(new BorderLayout());
		south.add(feedback, BorderLayout.CENTER);
		south.add(buttons, BorderLayout.EAST);
		add(south, BorderLayout.SOUTH);
		setPreferredSize(new Dimension(820, 510));
		pack();
		setLocationRelativeTo(owner);
	}

	private void installUndoRedoBindings() {
		JComponent root = getRootPane();
		var input = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		var actions = root.getActionMap();
		input.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "ccpm-undo");
		input.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "ccpm-redo");
		actions.put("ccpm-undo", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent event) {
				if (project != null && project.getUndoController().canUndo()) {
					project.getUndoController().undo();
					if (bufferChart != null) bufferChart.reloadHistory();
				}
			}
		});
		actions.put("ccpm-redo", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent event) {
				if (project != null && project.getUndoController().canRedo()) {
					project.getUndoController().redo();
					if (bufferChart != null) bufferChart.reloadHistory();
				}
			}
		});
	}

	private void openSettingsAndReturn(Frame owner, Project project, Surface surface) {
		dispose();
		// Do not open the next modal dialog from the action that is still closing
		// this one.  That nested modal loop can consume the physical button release
		// under FlatLaf and leave the settings surface undisplayed.
		SwingUtilities.invokeLater(() -> {
			ResourceLevelingDialogBox.getCriticalChainInstance(owner, project).setVisible(true);
			if (project != null) {
				new CriticalChainStatusDialogBox(owner, project, surface).setVisible(true);
			}
		});
	}

	private void loadAnalysis(CriticalChainService service, Project project, Surface surface, JPanel content) {
		// The project model is edited on the EDT and is not safe to traverse while
		// spreadsheet edits are in progress. Defer one event so the loading label
		// paints, then analyze on the same thread that owns the model.
		SwingUtilities.invokeLater(() -> {
				if (content == null) return;
				try {
					CriticalChainService.Analysis analysis = service.analysis(project);
					content.removeAll();
					if (analysis == null) content.add(new JLabel(UsabilityStrings.text("ccpm.noAppliedPlan")), BorderLayout.CENTER);
					else if (surface == Surface.NETWORK) {
						CriticalChainGraphPanel graph = new CriticalChainGraphPanel(project);
						graph.setAnalysis(analysis);
						content.add(new JScrollPane(graph), BorderLayout.CENTER);
					} else {
						bufferChart = new CriticalChainBufferChartPanel(project);
						bufferChart.setSelectionListener(ignored -> updateRetractionEnabled());
						bufferChart.setAnalysis(analysis, true);
						content.add(bufferChart, BorderLayout.CENTER);
						updateRetractionEnabled();
					}
				} catch (Exception exception) {
					content.removeAll();
					content.add(new JLabel(UsabilityStrings.text("ccpm.analysisFailed")), BorderLayout.CENTER);
				}
				content.revalidate();
				content.repaint();
		});
	}

	private void updateRetractionEnabled() {
		if (retractObservation == null) return;
		retractObservation.setEnabled(bufferChart != null && bufferChart.selectedPoint() != null
			&& project != null && !project.isReadOnly());
	}

	private void retractSelectedObservation() {
		if (bufferChart == null || bufferChart.selectedPoint() == null || project == null || project.isReadOnly()) {
			updateRetractionEnabled();
			return;
		}
		CriticalChainBufferHistory.Point point = bufferChart.selectedPoint();
		JTextField reason = new JTextField(28);
		JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
		form.add(new JLabel(UsabilityStrings.text("ccpm.observationTime")));
		form.add(new JLabel(DateTimeFormatter.ISO_INSTANT.format(point.observedAt())));
		form.add(new JLabel(UsabilityStrings.text("ccpm.observationValues")));
		form.add(new JLabel(Math.round(point.progressPercent()) + "% / " + Math.round(point.consumptionPercent()) + "% (" + point.zone() + ")"));
		form.add(new JLabel(UsabilityStrings.text("ccpm.retractionReason")));
		form.add(reason);
		boolean latest = false;
		CriticalChainBufferHistory history = project.findTransientDocumentState(CriticalChainBufferHistory.class);
		if (history != null) latest = history.points().stream().max(java.util.Comparator.comparing(CriticalChainBufferHistory.Point::observedAt))
			.map(value -> value.observationId().equals(point.observationId())).orElse(false);
		String message = latest ? UsabilityStrings.text("ccpm.retractLatestWarning") : UsabilityStrings.text("ccpm.retractPrompt");
		int choice = javax.swing.JOptionPane.showConfirmDialog(this, new Object[] { message, form },
			UsabilityStrings.text("ccpm.retractTitle"), javax.swing.JOptionPane.OK_CANCEL_OPTION,
			javax.swing.JOptionPane.WARNING_MESSAGE);
		if (choice != javax.swing.JOptionPane.OK_OPTION) return;
		CriticalChainBufferHistoryService.Outcome outcome = new CriticalChainBufferHistoryService().retract(project,
			point.observationId(), reason.getText(), "local", "Local user");
		if (!outcome.changed()) {
			feedback.setText(retractionFailureMessage(outcome));
			updateRetractionEnabled();
			return;
		}
		bufferChart.reloadHistory();
		feedback.setText(MessageFormat.format(UsabilityStrings.text("ccpm.retractSuccess"),
			reason.getText().trim(), DateTimeFormatter.ISO_INSTANT.format(Instant.now())));
		updateRetractionEnabled();
	}

	static String retractionFailureMessage(CriticalChainBufferHistoryService.Outcome outcome) {
		if (outcome == null)
			return UsabilityStrings.text("ccpm.retractRejected");
		if (outcome.status() == CriticalChainBufferHistoryService.Status.FAILED)
			return UsabilityStrings.text("ccpm.retractFailed");
		String reason = outcome.reason() == null ? "" : outcome.reason();
		return switch (reason) {
			case "reason-required" -> UsabilityStrings.text("ccpm.retractReasonRequired");
			case "read-only" -> UsabilityStrings.text("ccpm.retractReadOnly");
			case "history-empty" -> UsabilityStrings.text("ccpm.retractHistoryEmpty");
			case "observation-not-found" -> UsabilityStrings.text("ccpm.retractNotFound");
			default -> UsabilityStrings.text("ccpm.retractRejected");
		};
	}

	private void exportReport(Project project, boolean html) {
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new java.io.File(html ? "ccpm-report.html" : "ccpm-report.csv"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
		try {
			CriticalChainBufferHistory history = project.findTransientDocumentState(CriticalChainBufferHistory.class);
			CriticalChainReportService reports = new CriticalChainReportService();
			if (html) reports.writeHtml(chooser.getSelectedFile().toPath(), project.getName(), history);
			else reports.writeCsv(chooser.getSelectedFile().toPath(), history);
		} catch (java.io.IOException exception) {
			javax.swing.JOptionPane.showMessageDialog(this, exception.getMessage(), "CCPM report", javax.swing.JOptionPane.ERROR_MESSAGE);
		}
	}
}
