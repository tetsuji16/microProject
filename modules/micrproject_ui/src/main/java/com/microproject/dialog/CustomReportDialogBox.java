package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.print.PrinterException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
	private static final Preferences PRESETS = Preferences.userNodeForPackage(CustomReportDialogBox.class).node("customReports");
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
	private final JLabel summary = new JLabel(" ");

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
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(columns), new JScrollPane(preview));
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
		JButton load = new JButton(t("report.load")); load.addActionListener(event -> loadPreset());
		JButton save = new JButton(t("report.saveAs")); save.addActionListener(event -> savePreset());
		JButton delete = new JButton(t("report.delete")); delete.addActionListener(event -> deletePreset());
		row1.add(load); row1.add(save); row1.add(delete); row1.add(new JLabel(t("report.filter"))); row1.add(filter);
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
		if (selected.isEmpty()) { Alert.warn(t("report.selectColumn")); return; }
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
		Preferences node = PRESETS.node(safeNode(name)); node.put("displayName", name.trim());
		node.put("columns", String.join(",", columns.getSelectedValuesList().stream().map(Field::getId).toList()));
		node.put("filter", String.valueOf(filter.getSelectedItem())); node.put("group", String.valueOf(group.getSelectedItem()));
		Field sortField = (Field) sort.getSelectedItem(); node.put("sort", sortField == null ? "" : sortField.getId());
		node.putBoolean("summary", includeSummary.isSelected()); refreshPresetNames(); presets.setSelectedItem(name.trim());
	}
	private void loadPreset() {
		String name = (String) presets.getSelectedItem(); if (name == null) return; Preferences node = presetByDisplayName(name); if (node == null) return;
		List<String> ids = Arrays.asList(node.get("columns", "").split(",")); columns.setSelectedIndices(java.util.stream.IntStream.range(0, fields.size()).filter(i -> ids.contains(fields.get(i).getId())).toArray());
		filter.setSelectedItem(node.get("filter", FILTERS[0])); group.setSelectedItem(node.get("group", GROUPS[0])); includeSummary.setSelected(node.getBoolean("summary", false));
		String sortId = node.get("sort", ""); fields.stream().filter(field -> field.getId().equals(sortId)).findFirst().ifPresent(sort::setSelectedItem); generate();
	}
	private void deletePreset() { String name = (String) presets.getSelectedItem(); Preferences node = name == null ? null : presetByDisplayName(name); if (node != null) try { node.removeNode(); refreshPresetNames(); } catch (BackingStoreException error) { Alert.error(error.getMessage()); } }
	private Preferences presetByDisplayName(String name) { try { for (String child : PRESETS.childrenNames()) { Preferences node = PRESETS.node(child); if (name.equals(node.get("displayName", child))) return node; } } catch (BackingStoreException ignored) { } return null; }
	private void refreshPresetNames() { presets.removeAllItems(); try { for (String child : PRESETS.childrenNames()) presets.addItem(PRESETS.node(child).get("displayName", child)); } catch (BackingStoreException ignored) { } }
	private static String safeNode(String name) { return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(name.trim().getBytes(StandardCharsets.UTF_8)); }

	private void exportCsv() {
		generate(); JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new java.io.File("project-report.csv")); if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
		StringBuilder csv = new StringBuilder(); for (int column = 0; column < previewModel.getColumnCount(); column++) { if (column > 0) csv.append(','); csv.append(csv(previewModel.getColumnName(column))); } csv.append("\r\n");
		for (int row = 0; row < previewModel.getRowCount(); row++) { for (int column = 0; column < previewModel.getColumnCount(); column++) { if (column > 0) csv.append(','); csv.append(csv(String.valueOf(previewModel.getValueAt(row, column)))); } csv.append("\r\n"); }
		try { Path path = chooser.getSelectedFile().toPath(); Files.writeString(path, "\uFEFF" + csv, StandardCharsets.UTF_8); }
		catch (Exception error) { Alert.error(t("report.exportError") + " " + error.getMessage()); }
	}
	static String csv(String value) { String safe = value == null ? "" : value; return '"' + safe.replace("\"", "\"\"") + '"'; }
	private static String t(String key) { return UsabilityStrings.text(key); }
	private void print() { try { preview.print(JTable.PrintMode.FIT_WIDTH, new java.text.MessageFormat(project.getName()), new java.text.MessageFormat("Page {0}")); } catch (PrinterException error) { Alert.error(error.getMessage()); } }
}
