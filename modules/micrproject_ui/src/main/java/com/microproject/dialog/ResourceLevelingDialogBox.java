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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceLevelingService;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.help.HelpUtil;
import com.microproject.pm.task.Project;
import com.microproject.pm.graphic.views.CriticalChainBufferChartPanel;
import com.microproject.pm.graphic.views.CriticalChainGraphPanel;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.PopupDialogSupport;

/** Preview-first resource leveling workflow. */
public final class ResourceLevelingDialogBox extends JDialog {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private final CriticalChainService criticalChainService = new CriticalChainService();
	private final CriticalChainService.Settings workingSettings;
	private final JComboBox<ResourceLevelingService.Order> order = new JComboBox<>(ResourceLevelingService.Order.values());
	private final JCheckBox slackOnly = new JCheckBox(UsabilityStrings.text("leveling.slackOnly"));
	private final JCheckBox allowSplits = new JCheckBox(UsabilityStrings.text("leveling.splits"), true);
	private final JCheckBox ccpm = new JCheckBox(UsabilityStrings.text("ccpm.enabled"));
	private final JSpinner bufferPercent = new JSpinner(new javax.swing.SpinnerNumberModel(50, 0, 100, 5));
	private final JList<Resource> resources;
	private final ChangeTableModel changes = new ChangeTableModel();
	private final JTable table = new JTable(changes);
	private final JLabel status = new JLabel(" ");
	private final JButton apply = new JButton(UsabilityStrings.text("leveling.apply"));
	private final CriticalChainGraphPanel criticalChainGraph;
	private final CriticalChainBufferChartPanel criticalChainBufferChart;
	private ResourceLevelingService.Plan currentPlan;

	public static ResourceLevelingDialogBox getInstance(Frame owner, Project project) {
		return new ResourceLevelingDialogBox(owner, project, false);
	}

	/** Project > CCPM entry point; it shares the preview/apply controller with leveling. */
	public static ResourceLevelingDialogBox getCriticalChainInstance(Frame owner, Project project) {
		return new ResourceLevelingDialogBox(owner, project, true);
	}

	private ResourceLevelingDialogBox(Frame owner, Project project, boolean startInCriticalChainMode) {
		super(owner, UsabilityStrings.text(startInCriticalChainMode ? "ccpm.settingsTitle" : "leveling.title"), true);
		HelpUtil.addDocHelp(getRootPane(), "Resource_Leveling");
		getAccessibleContext().setAccessibleDescription(UsabilityStrings.text("leveling.slackOnly"));
		PopupDialogSupport.bindEscapeToDispose(this);
		this.project = project;
		criticalChainGraph = new CriticalChainGraphPanel(project);
		criticalChainBufferChart = new CriticalChainBufferChartPanel(project);
		CriticalChainService.Settings settings = criticalChainService.findSettings(project);
		workingSettings = settings == null ? new CriticalChainService.Settings() : settings.copy();
		ccpm.setSelected(startInCriticalChainMode || workingSettings.isEnabled());
		bufferPercent.setValue(Integer.valueOf((int) Math.round(workingSettings.getBufferFraction() * 100D)));
		order.setSelectedItem(workingSettings.getLevelingOrder());
		slackOnly.setSelected(workingSettings.isOnlyWithinAvailableSlack());
		allowSplits.setSelected(workingSettings.isAllowTaskSplits());
		List<Resource> resourceList = project.getResourcePool().getResourceList().stream()
			.filter(Resource::isLabor)
			.filter(value -> !value.getAssignments().isEmpty())
			.toList();
		resources = new JList<>(resourceList.toArray(Resource[]::new));
		resources.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		if (!resourceList.isEmpty()) {
			resources.setSelectionInterval(0, resourceList.size() - 1);
		}
		buildUi();
		preview();
	}

	private void buildUi() {
		FlatUiSupport.styleDialogRoot(getRootPane());
		JPanel options = new JPanel(new GridBagLayout());
		options.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(3, 3, 3, 8);
		options.add(new JLabel(UsabilityStrings.text("leveling.order")), constraints);
		constraints.gridx = 1;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		options.add(order, constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		constraints.gridwidth = 2;
		options.add(slackOnly, constraints);
		constraints.gridy++;
		options.add(allowSplits, constraints);
		constraints.gridy++;
		options.add(ccpm, constraints);
		constraints.gridy++;
		constraints.gridwidth = 1;
		options.add(new JLabel(UsabilityStrings.text("ccpm.buffer")), constraints);
		constraints.gridx = 1;
		options.add(bufferPercent, constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		constraints.gridwidth = 2;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;
		options.add(new JScrollPane(resources), constraints);

		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, options, new JScrollPane(table));
		split.setResizeWeight(0.25D);
		split.setDividerLocation(230);

		JButton preview = new JButton(UsabilityStrings.text("leveling.preview"));
		preview.addActionListener(event -> preview());
		apply.addActionListener(event -> apply());
		JButton clear = new JButton(UsabilityStrings.text("leveling.clear"));
		clear.addActionListener(event -> clear());
		JButton close = new JButton(UsabilityStrings.text("common.close"));
		close.addActionListener(event -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(preview);
		buttons.add(clear);
		buttons.add(apply);
		buttons.add(close);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(UsabilityStrings.text("leveling.title"), split);
		tabs.addTab(UsabilityStrings.text("ccpm.networkTab"), new JScrollPane(criticalChainGraph));
		tabs.addTab(UsabilityStrings.text("ccpm.bufferChartTab"), criticalChainBufferChart);
		setLayout(new BorderLayout());
		add(tabs, BorderLayout.CENTER);
		JPanel south = new JPanel(new BorderLayout());
		// The CCPM status text is wider than the dialog; clip it in the center
		// instead of letting it push the action buttons off the dialog edge.
		JPanel statusArea = new JPanel(new BorderLayout());
		status.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		statusArea.add(status, BorderLayout.CENTER);
		south.add(statusArea, BorderLayout.CENTER);
		south.add(buttons, BorderLayout.EAST);
		add(south, BorderLayout.SOUTH);
		setPreferredSize(new Dimension(900, 520));
		pack();
		setLocationRelativeTo(getOwner());
	}

	private void preview() {
		List<Resource> selected = resources.getSelectedValuesList();
		workingSettings.setEnabled(ccpm.isSelected());
		workingSettings.setBufferFraction(((Number) bufferPercent.getValue()).doubleValue() / 100D);
		workingSettings.setLevelingOrder((ResourceLevelingService.Order) order.getSelectedItem());
		workingSettings.setOnlyWithinAvailableSlack(slackOnly.isSelected());
		workingSettings.setAllowTaskSplits(allowSplits.isSelected());
		CriticalChainService.Analysis analysis = criticalChainService.preview(project, selected.isEmpty() ? null : selected, workingSettings);
		criticalChainGraph.setAnalysis(analysis);
		criticalChainBufferChart.setAnalysis(analysis, ccpm.isSelected());
		currentPlan = analysis.levelingPlan();
		changes.setPlan(currentPlan);
		// CCPM application also captures the critical-chain baseline.  It must
		// remain available when leveling finds no date changes; otherwise a
		// conflict-free project can never produce the baseline used by the
		// buffer graph/status view.
		apply.setEnabled(ccpm.isSelected() || !currentPlan.changes().isEmpty());
		status.setText(java.text.MessageFormat.format(UsabilityStrings.text("leveling.status"),
			currentPlan.changes().size(), currentPlan.splits().size(), currentPlan.unresolved().size())
			+ (ccpm.isSelected() ? " " + java.text.MessageFormat.format(UsabilityStrings.text("ccpm.status"),
				analysis.criticalTaskIds().size(), ChangeTableModel.humanDuration(analysis.projectBufferMillis()))
				+ " " + java.text.MessageFormat.format(UsabilityStrings.text("ccpm.bufferStatus"),
					ChangeTableModel.humanDuration(analysis.projectBuffer().remainingMillis()),
					ChangeTableModel.humanDuration(analysis.projectBuffer().plannedMillis()), analysis.projectBuffer().status()) : ""));
	}

	private void apply() {
		if (currentPlan == null || (currentPlan.changes().isEmpty() && !ccpm.isSelected())) {
			return;
		}
		if (!currentPlan.isComplete()) {
			int answer = PopupDialogSupport.showConfirmDialog(this,
				UsabilityStrings.text("leveling.partial"),
				UsabilityStrings.text("leveling.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer != JOptionPane.YES_OPTION) {
				return;
			}
		}
		if (ccpm.isSelected()) {
			criticalChainService.apply(project, resources.getSelectedValuesList(), workingSettings);
		} else {
			javax.swing.undo.UndoableEditSupport edits = project.getUndoController().getEditSupport();
			if (edits != null) edits.beginUpdate();
			try {
				criticalChainService.forget(project, edits);
				currentPlan.apply(edits);
			} finally {
				if (edits != null) edits.endUpdate();
			}
		}
		preview();
	}

	private void clear() {
		criticalChainService.clear(project);
		ccpm.setSelected(false);
		workingSettings.setEnabled(false);
		preview();
	}

	private static final class ChangeTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		private static final String[] COLUMNS = { UsabilityStrings.text("common.task"), UsabilityStrings.text("leveling.currentStart"), UsabilityStrings.text("leveling.leveledStart"), UsabilityStrings.text("leveling.delay"), UsabilityStrings.text("common.resource") };
		private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
			.withZone(ZoneId.systemDefault());
		private List<ResourceLevelingService.Change> rows = List.of();

		void setPlan(ResourceLevelingService.Plan plan) {
			rows = plan == null ? List.of() : plan.changes();
			fireTableDataChanged();
		}

		public int getRowCount() {
			return rows.size();
		}

		public int getColumnCount() {
			return COLUMNS.length;
		}

		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			ResourceLevelingService.Change change = rows.get(rowIndex);
			return switch (columnIndex) {
				case 0 -> change.task().getName();
				case 1 -> formatter.format(Instant.ofEpochMilli(change.oldStart()));
				case 2 -> formatter.format(Instant.ofEpochMilli(change.projectedStart()));
				case 3 -> humanDuration(change.addedDelayMillis());
				case 4 -> change.limitingResource();
				default -> "";
			};
		}

		static String humanDuration(long milliseconds) {
			long hours = milliseconds / (60L * 60L * 1000L);
			return hours % 8L == 0L ? (hours / 8L) + "d" : hours + "h";
		}
	}
}
