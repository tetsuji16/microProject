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
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.util.Alert;
import com.microproject.help.HelpUtil;
import com.microproject.util.PopupDialogSupport;

/** User-configurable task report with reusable presets, preview, print, and CSV export. */
public final class CustomReportDialogBox extends JDialog {
	private static final long serialVersionUID = 1L;
	enum ReportTemplate {
		BLANK("blank", "report.template.blank", List.of()),
		CHART("chart", "report.template.chart", List.of("Field.name", "Field.work", "Field.actualWork", "Field.remainingWork")),
		TABLE("table", "report.template.table", List.of("Field.name", "Field.start", "Field.finish", "Field.duration", "Field.percentComplete", "Field.resourceNames")),
		COMPARISON("comparison", "report.template.comparison", List.of("Field.name", "Field.start", "Field.finish", "Field.baselineStart", "Field.baselineFinish", "Field.percentComplete"));

		private final String code;
		private final String labelKey;
		private final List<String> fieldIds;

		ReportTemplate(String code, String labelKey, List<String> fieldIds) {
			this.code = code;
			this.labelKey = labelKey;
			this.fieldIds = fieldIds;
		}

		String code() { return code; }
		List<String> fieldIds() { return fieldIds; }
		@Override public String toString() { return t(labelKey); }

		static ReportTemplate fromCode(String code) {
			for (ReportTemplate template : values()) if (template.code.equals(code)) return template;
			return TABLE;
		}
	}
	private static final String[] FILTERS = { t("report.all"), t("report.incomplete"), t("report.late"), t("report.milestones"), t("report.critical"), t("report.manual"), t("report.inactive") };
	private static final String[] GROUPS = { t("report.none"), t("report.status"), t("common.resource") };
	private final Project project;
	private final List<Field> fields;
	private final JList<Field> columns;
	private final JComboBox<String> filter = new JComboBox<>(FILTERS);
	private final JComboBox<String> group = new JComboBox<>(GROUPS);
	private final JComboBox<Field> sort;
	private final JTextField contains = new JTextField(14);
	private final JCheckBox includeSummary = new JCheckBox(t("report.summary"));
	private final JCheckBox useDateRange = new JCheckBox(t("report.dateRange"));
	private final JSpinner from = new JSpinner(new SpinnerDateModel());
	private final JSpinner to = new JSpinner(new SpinnerDateModel());
	private final JComboBox<String> presets = new JComboBox<>();
	private final DefaultTableModel previewModel = new DefaultTableModel() {
		private static final long serialVersionUID = 1L;
		@Override public boolean isCellEditable(int row, int column) { return false; }
	};
	private final JTable preview = new JTable(previewModel);
	private final JPanel previewCards = new JPanel(new CardLayout());
	private final ChartPanel chartPreview = new ChartPanel(null);
	private final JLabel summary = new JLabel(" ");
	private ReportTemplate selectedTemplate = ReportTemplate.TABLE;

	public CustomReportDialogBox(Frame owner, Project project) {
		super(owner, t("report.title"), false);
		HelpUtil.addDocHelp(getRootPane(), "Custom_Reports");
		getAccessibleContext().setAccessibleDescription(t("report.accessible"));
		PopupDialogSupport.bindEscapeToDispose(this);
		this.project = project;
		fields = reportFields();
		columns = new JList<>(fields.toArray(Field[]::new));
		columns.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		columns.setVisibleRowCount(12);
		sort = new JComboBox<>(fields.toArray(Field[]::new));
		selectDefaults();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(8, 8));
		add(buildSettings(), BorderLayout.NORTH);
		previewCards.add(new JScrollPane(preview), "table");
		previewCards.add(chartPreview, "chart");
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(columns), previewCards);
		split.setResizeWeight(0.2D); add(split, BorderLayout.CENTER);
		add(buildButtons(), BorderLayout.SOUTH);
		preview.setAutoCreateRowSorter(true); preview.setRowHeight(Math.max(22, preview.getRowHeight()));
		preview.getAccessibleContext().setAccessibleName(t("report.accessible"));
		refreshPresetNames(); generate();
		setMinimumSize(new Dimension(950, 600)); setSize(1200, 760); setLocationRelativeTo(owner);
	}

	private JPanel buildSettings() {
		JPanel result = new JPanel(new GridLayout(2, 1));
		JPanel row1 = new JPanel(); row1.add(new JLabel(t("report.preset"))); row1.add(presets);
		JButton create = new JButton(t("report.new")); create.addActionListener(event -> createTemplatePreset());
		JButton load = new JButton(t("report.load")); load.addActionListener(event -> loadPreset());
		JButton save = new JButton(t("report.saveAs")); save.addActionListener(event -> savePreset());
		JButton delete = new JButton(t("report.delete")); delete.addActionListener(event -> deletePreset());
		row1.add(create); row1.add(load); row1.add(save); row1.add(delete); row1.add(new JLabel(t("report.filter"))); row1.add(filter);
		row1.add(new JLabel(t("report.nameContains"))); row1.add(contains); row1.add(includeSummary);
		JPanel row2 = new JPanel(); row2.add(new JLabel(t("report.group"))); row2.add(group); row2.add(new JLabel(t("report.sort"))); row2.add(sort);
		row2.add(useDateRange); row2.add(new JLabel(t("report.from"))); row2.add(from); row2.add(new JLabel(t("report.to"))); row2.add(to);
		result.add(row1); result.add(row2); return result;
	}

	private JPanel buildButtons() {
		JPanel result = new JPanel(new BorderLayout()); result.add(summary, BorderLayout.WEST);
		JPanel buttons = new JPanel(); JButton generate = new JButton(t("report.generate")); generate.addActionListener(event -> generate());
		JButton export = new JButton(t("report.export")); export.addActionListener(event -> exportCsv());
		JButton print = new JButton(t("report.print")); print.addActionListener(event -> print());
		JButton close = new JButton(t("common.close")); close.addActionListener(event -> dispose());
		buttons.add(generate); buttons.add(export); buttons.add(print); buttons.add(close); result.add(buttons, BorderLayout.EAST); return result;
	}

	private List<Field> reportFields() {
		List<String> standard = List.of("Field.name", "Field.wbs", "Field.start", "Field.finish", "Field.duration",
			"Field.percentComplete", "Field.work", "Field.cost", "Field.resourceNames", "Field.priority",
			"Field.deadline", "Field.totalSlack", "Field.manuallyScheduled", "Field.inactiveTask");
		List<Field> result = new ArrayList<>();
		for (String id : standard) { Field field = Configuration.getFieldFromId(id); if (field != null) result.add(field); }
		Configuration.getInstance().getFieldDictionary().getTaskFields().stream().filter(Field::isCustom)
			.sorted(Comparator.comparing(Field::getName)).forEach(result::add);
		return List.copyOf(result);
	}

	private void selectDefaults() {
		List<String> defaults = List.of("Field.name", "Field.start", "Field.finish", "Field.duration", "Field.percentComplete", "Field.resourceNames");
		int[] indices = java.util.stream.IntStream.range(0, fields.size()).filter(index -> defaults.contains(fields.get(index).getId())).toArray();
		columns.setSelectedIndices(indices);
	}

	private void generate() {
		List<Field> selected = columns.getSelectedValuesList();
		if (selected.isEmpty()) {
			previewModel.setDataVector(new Object[0][0], new Object[0]);
			showTablePreview();
			summary.setText(t("report.blankPreview"));
			return;
		}
		List<Task> tasks = filteredTasks(); Field sortField = (Field) sort.getSelectedItem();
		if (sortField != null) tasks.sort(Comparator.comparing(task -> safeText(sortField, task), String.CASE_INSENSITIVE_ORDER));
		boolean grouped = group.getSelectedIndex() > 0;
		List<String> names = new ArrayList<>(); if (grouped) names.add(t("report.groupColumn")); selected.forEach(field -> names.add(field.getName()));
		previewModel.setDataVector(new Object[0][0], names.toArray());
		double totalCost = 0D; long totalWork = 0L;
		for (Task task : tasks) {
			List<Object> row = new ArrayList<>(); if (grouped) row.add(groupValue(task));
			for (Field field : selected) row.add(safeText(field, task));
			previewModel.addRow(row.toArray());
			if (task instanceof com.microproject.pm.task.NormalTask normal) {
				totalCost += normal.getCost(null); totalWork += normal.getWork(null);
			}
		}
		summary.setText(tasks.size() + " tasks  |  Cost: " + String.format(Locale.getDefault(), "%,.2f", totalCost) + "  |  Work: " + totalWork);
		if (selectedTemplate == ReportTemplate.CHART) {
			chartPreview.setChart(createWorkChart(tasks));
			showChartPreview();
		} else {
			showTablePreview();
		}
	}

	private void showTablePreview() {
		((CardLayout)previewCards.getLayout()).show(previewCards, "table");
	}

	private void showChartPreview() {
		((CardLayout)previewCards.getLayout()).show(previewCards, "chart");
	}

	static JFreeChart createWorkChart(List<? extends Task> tasks) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		double work = 0D;
		double actualWork = 0D;
		double remainingWork = 0D;
		for (Task task : tasks) {
			if (task instanceof com.microproject.pm.task.NormalTask normal) {
				work += hours(normal.getWork(null));
				actualWork += hours(normal.getActualWork(null));
				remainingWork += hours(normal.getRemainingWork(null));
			}
		}
		dataset.addValue(work, "Work", "Project");
		dataset.addValue(actualWork, "Actual Work", "Project");
		dataset.addValue(remainingWork, "Remaining Work", "Project");
		return ChartFactory.createBarChart("Work", "", "Hours", dataset,
			PlotOrientation.VERTICAL, true, true, false);
	}

	private static double hours(long milliseconds) {
		return milliseconds / 3_600_000D;
	}

	private List<Task> filteredTasks() {
		List<Task> result = new ArrayList<>(); String text = contains.getText().trim().toLowerCase(Locale.ROOT);
		long lower = ((Date) from.getValue()).getTime(), upper = ((Date) to.getValue()).getTime();
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next(); if (!includeSummary.isSelected() && task.isSummary()) continue;
			if (!text.isEmpty() && (task.getName() == null || !task.getName().toLowerCase(Locale.ROOT).contains(text))) continue;
			if (useDateRange.isSelected() && (task.getStart() < lower || task.getStart() > upper)) continue;
			String chosen = (String) filter.getSelectedItem();
			if (t("report.incomplete").equals(chosen) && task.isComplete()) continue;
			if (t("report.late").equals(chosen) && (task.isComplete() || task.getEnd() >= effectiveStatusDate())) continue;
			if (t("report.milestones").equals(chosen) && !task.isMilestone()) continue;
			if (t("report.critical").equals(chosen) && !task.isCritical()) continue;
			if (t("report.manual").equals(chosen) && !task.isManuallyScheduled()) continue;
			if (t("report.inactive").equals(chosen) && !task.isInactiveTask()) continue;
			result.add(task);
		}
		return result;
	}

	private long effectiveStatusDate() { long status = project.getStatusDate(); return status > 0L ? status : System.currentTimeMillis(); }
	private String groupValue(Task task) {
		if (group.getSelectedIndex() == 1) return task.isInactiveTask() ? t("report.inactive") : task.isComplete() ? t("report.complete") : task.inProgress() ? t("report.inProgress") : t("report.notStarted");
		if (!(task instanceof com.microproject.pm.task.NormalTask normal)) return t("report.unassigned");
		List<String> names = new ArrayList<>(); for (Object value : normal.getAssignments()) { Assignment assignment = (Assignment) value; if (assignment.getResource() != null) names.add(assignment.getResource().getName()); }
		return names.isEmpty() ? t("report.unassigned") : String.join(", ", names);
	}
	private String safeText(Field field, Task task) { try { String value = field.getText(task, null); return value == null ? "" : value; } catch (RuntimeException error) { return ""; } }

	private void savePreset() {
		String name = JOptionPane.showInputDialog(this, t("report.presetName"), t("report.savePreset"), JOptionPane.PLAIN_MESSAGE);
		if (name == null || name.isBlank()) return;
		String displayName = name.trim();
		project.getCustomReportPresets().put(displayName, encodePreset(currentPresetValues()));
		refreshPresetNames(); presets.setSelectedItem(displayName);
	}
	private void createTemplatePreset() {
		ReportTemplate template = (ReportTemplate) JOptionPane.showInputDialog(this, t("report.templatePrompt"),
			t("report.new"), JOptionPane.PLAIN_MESSAGE, null, ReportTemplate.values(), ReportTemplate.TABLE);
		if (template == null) return;
		String name = JOptionPane.showInputDialog(this, t("report.presetName"), t("report.new"), JOptionPane.PLAIN_MESSAGE);
		if (name == null || name.isBlank()) return;
		applyTemplate(template);
		String displayName = name.trim();
		project.getCustomReportPresets().put(displayName, encodePreset(currentPresetValues()));
		refreshPresetNames(); presets.setSelectedItem(displayName); generate();
	}

	private Map<String, String> currentPresetValues() {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("template", selectedTemplate.code());
		values.put("columns", String.join(",", columns.getSelectedValuesList().stream().map(Field::getId).toList()));
		values.put("filter", String.valueOf(filter.getSelectedItem()));
		values.put("group", String.valueOf(group.getSelectedItem()));
		Field sortField = (Field) sort.getSelectedItem(); values.put("sort", sortField == null ? "" : sortField.getId());
		values.put("summary", Boolean.toString(includeSummary.isSelected()));
		return values;
	}

	private void applyTemplate(ReportTemplate template) {
		selectedTemplate = template == null ? ReportTemplate.TABLE : template;
		List<String> ids = selectedTemplate.fieldIds();
		columns.setSelectedIndices(java.util.stream.IntStream.range(0, fields.size())
			.filter(index -> ids.contains(fields.get(index).getId())).toArray());
		includeSummary.setSelected(selectedTemplate == ReportTemplate.COMPARISON);
		group.setSelectedItem(GROUPS[0]); filter.setSelectedItem(FILTERS[0]); contains.setText("");
	}
	private void loadPreset() {
		String name = (String) presets.getSelectedItem(); if (name == null) return;
		Map<String, String> values = decodePreset(project.getCustomReportPresets().get(name)); if (values == null) return;
		selectedTemplate = ReportTemplate.fromCode(values.get("template"));
		List<String> ids = Arrays.asList(values.getOrDefault("columns", "").split(",", -1));
		columns.setSelectedIndices(java.util.stream.IntStream.range(0, fields.size()).filter(i -> ids.contains(fields.get(i).getId())).toArray());
		filter.setSelectedItem(values.getOrDefault("filter", FILTERS[0])); group.setSelectedItem(values.getOrDefault("group", GROUPS[0]));
		includeSummary.setSelected(Boolean.parseBoolean(values.getOrDefault("summary", "false")));
		String sortId = values.getOrDefault("sort", ""); fields.stream().filter(field -> field.getId().equals(sortId)).findFirst().ifPresent(sort::setSelectedItem); generate();
	}
	private void deletePreset() { String name = (String) presets.getSelectedItem(); if (name != null) { project.getCustomReportPresets().remove(name); refreshPresetNames(); } }
	private void refreshPresetNames() { presets.removeAllItems(); project.getCustomReportPresets().keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).forEach(presets::addItem); }
	static String encodePreset(Map<String, String> values) {
		return values.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue())).collect(java.util.stream.Collectors.joining("\n"));
	}
	static Map<String, String> decodePreset(String encoded) {
		if (encoded == null) return null;
		Map<String, String> values = new LinkedHashMap<>();
		try { for (String line : encoded.split("\\n")) { int separator = line.indexOf('='); if (separator <= 0) return null; values.put(decode(line.substring(0, separator)), decode(line.substring(separator + 1))); } return values.isEmpty() ? null : values; }
		catch (IllegalArgumentException error) { return null; }
	}
	private static String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
	private static String decode(String value) { return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8); }

	private void exportCsv() {
		generate();
		com.formdev.flatlaf.util.SystemFileChooser chooser = new com.formdev.flatlaf.util.SystemFileChooser();
		chooser.setSelectedFile(new java.io.File("project-report.csv"));
		chooser.setFileFilter(new com.formdev.flatlaf.util.SystemFileChooser.FileNameExtensionFilter("CSV (*.csv)", "csv"));
		if (chooser.showSaveDialog(this) != com.formdev.flatlaf.util.SystemFileChooser.APPROVE_OPTION) return;
		java.io.File selected = ensureCsvExtension(chooser.getSelectedFile());
		try (OutputStream out = Files.newOutputStream(selected.toPath())) { writeReportCsv(previewModel, out); }
		catch (Exception error) { Alert.error(t("report.exportError") + " " + error.getMessage()); }
	}

	static java.io.File ensureCsvExtension(java.io.File file) {
		String name = file.getName().toLowerCase(Locale.ROOT);
		return name.endsWith(".csv") ? file : new java.io.File(file.getParentFile(), file.getName() + ".csv");
	}

	/**
	 * Writes a table model as CSV (RFC 4180), prefixed with a UTF-8 BOM so Excel opens it correctly.
	 * Null cells are exported as empty fields.
	 */
	static void writeReportCsv(TableModel model, OutputStream out) throws IOException {
		try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
			writer.write('\uFEFF');
			try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.RFC4180)) {
				List<String> header = new ArrayList<>(); for (int column = 0; column < model.getColumnCount(); column++) header.add(model.getColumnName(column)); printer.printRecord(header);
				for (int row = 0; row < model.getRowCount(); row++) {
					List<String> values = new ArrayList<>(); for (int column = 0; column < model.getColumnCount(); column++) { Object value = model.getValueAt(row, column); values.add(value == null ? "" : String.valueOf(value)); }
					printer.printRecord(values);
				}
			}
		}
	}
	private static String t(String key) { return UsabilityStrings.text(key); }
	private void print() { try { preview.print(JTable.PrintMode.FIT_WIDTH, new java.text.MessageFormat(project.getName()), new java.text.MessageFormat("Page {0}")); } catch (PrinterException error) { Alert.error(error.getMessage()); } }
}
