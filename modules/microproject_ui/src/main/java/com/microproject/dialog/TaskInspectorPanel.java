/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.pm.task.ScheduleDiagnosticsService;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;

/** Read-only, selection-following schedule facts for the selected task. */
public final class TaskInspectorPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final JPanel content = new JPanel(new GridLayout(0, 1, 0, 8));
	private final JLabel title = new JLabel(Messages.getString("TaskInspector.SelectTask"), SwingConstants.LEADING);
	private final Consumer<Task> navigate;
	private final Consumer<Task> makeAutomatic;
	private final Predicate<Task> canMakeAutomatic;
	private Task task;

	public TaskInspectorPanel(Consumer<Task> navigate, Consumer<Task> makeAutomatic, Predicate<Task> canMakeAutomatic) {
		this.navigate = navigate;
		this.makeAutomatic = makeAutomatic;
		this.canMakeAutomatic = canMakeAutomatic;
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(title, BorderLayout.NORTH);
		add(new JScrollPane(content), BorderLayout.CENTER);
		setPreferredSize(new java.awt.Dimension(300, 400));
		setMinimumSize(new java.awt.Dimension(220, 120));
	}

	public Task getTask() { return task; }
	public String getTitleText() { return title.getText(); }
	public void showFeedback(String message) { title.setText(message); }

	public void showTask(Task selected) {
		task = selected;
		content.removeAll();
		if (selected == null) {
			title.setText(Messages.getString("TaskInspector.SelectTask"));
			content.revalidate();
			content.repaint();
			return;
		}
		title.setText(selected.getName());
		title.setToolTipText(selected.getName());
		section(Messages.getString("TaskInspector.TaskDates"), date(selected.getStart()) + " – " + date(selected.getEnd()));
		section(Messages.getString("TaskInspector.ActualStartAssignments"), format("TaskInspector.ActualStartValue", date(selected.getActualStart())) + assignmentSummary(selected));
		section(Messages.getString("TaskInspector.LevelingDelay"), selected.getLevelingDelay() == 0
			? Messages.getString("TaskInspector.None") : formatDuration(selected, selected.getLevelingDelay()));
		section(Messages.getString("TaskInspector.Constraint"), format("TaskInspector.ConstraintValue",
			selected.getConstraintTypeKind(), date(selected.getConstraintDate())));
		section(Messages.getString("TaskInspector.SummaryConstraint"), summaryConstraint(selected));
		section(Messages.getString("TaskInspector.Predecessors"), predecessorPanel(selected));
		section(Messages.getString("TaskInspector.Subtasks"), subtasks(selected));
		section(Messages.getString("TaskInspector.Calendar"), calendar(selected));
		JPanel mode = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
			mode.add(new JLabel(Messages.getString(selected.isManuallyScheduled()
				? "TaskInspector.ManualMode" : "TaskInspector.AutomaticMode")));
		if (selected.isManuallyScheduled()) {
			JButton automatic = new JButton(Messages.getString("TaskInspector.Automatic"));
			automatic.setEnabled(canMakeAutomatic.test(selected));
			automatic.addActionListener(event -> makeAutomatic.accept(selected));
			mode.add(automatic);
		}
		section(Messages.getString("TaskInspector.SchedulingMode"), mode);
		section(Messages.getString("TaskInspector.Refresh"), Messages.getString("TaskInspector.RefreshGuidance"));
		String diagnostics = new ScheduleDiagnosticsService().diagnose(selected).stream()
			.map(issue -> issue.severity() + ": " + issue.summary() + "\n" + issue.recommendation())
			.collect(java.util.stream.Collectors.joining("\n\n"));
		section(Messages.getString("TaskInformationDialog.Diagnostics"), diagnostics);
		content.revalidate();
		content.repaint();
	}

	private String assignmentSummary(Task selected) {
		if (!(selected instanceof com.microproject.pm.task.NormalTask normal) || normal.getAssignments().isEmpty())
			return "\n" + Messages.getString("TaskInspector.Assignments") + ": " + Messages.getString("TaskInspector.None");
		StringBuilder value = new StringBuilder("\n").append(Messages.getString("TaskInspector.Assignments")).append(':');
		for (var association : normal.getAssignments()) {
			if (association instanceof Assignment assignment)
				value.append("\n• ").append(assignment.getResourceName()).append(" — ")
					.append(format("TaskInspector.AssignmentActualStart", date(assignment.getActualStart())));
		}
		return value.toString();
	}

	private JPanel predecessorPanel(Task selected) {
		JPanel rows = new JPanel(new GridLayout(0, 1, 0, 3));
		for (Object value : selected.getPredecessorList()) {
			Dependency dependency = (Dependency) value;
			Task predecessor = (Task) dependency.getPredecessor();
			JButton link = new JButton(format("TaskInspector.PredecessorValue", predecessor.getName(),
				dependency.getDependencyKind(), formatDuration(selected, dependency.getLeadValue())));
			link.setHorizontalAlignment(SwingConstants.LEADING);
			link.addActionListener(event -> navigate.accept(predecessor));
			rows.add(link);
		}
		if (rows.getComponentCount() == 0)
			rows.add(new JLabel(Messages.getString("TaskInspector.None")));
		return rows;
	}

	private String subtasks(Task selected) {
		Collection<com.microproject.grouping.core.Node> children = selected.getWbsChildrenNodes();
		if (children == null || children.isEmpty()) return Messages.getString("TaskInspector.None");
		List<String> names = new ArrayList<>();
		for (var child : children) {
			if (child.getImpl() instanceof Task task) {
				String matchingBounds = "";
				if (selected.isWbsParent()) {
					if (task.getStart() == selected.getStart()) matchingBounds += " — " + Messages.getString("TaskInspector.MatchesSummaryStart");
					if (task.getEnd() == selected.getEnd()) matchingBounds += " — " + Messages.getString("TaskInspector.MatchesSummaryFinish");
				}
				names.add(task.getName() + " (" + date(task.getStart()) + " – " + date(task.getEnd()) + ")" + matchingBounds);
			}
		}
		return String.join("\n", names);
	}

	private String calendar(Task selected) {
		var calendar = selected.getEffectiveWorkCalendar();
		if (calendar == null) return Messages.getString("TaskInspector.ProjectCalendar");
		String[] days = new DateFormatSymbols().getShortWeekdays();
		StringBuilder details = new StringBuilder(calendar.getName());
		if (calendar instanceof WorkingCalendar workingCalendar) {
			for (int day = 0; day < 7; day++) {
				var workDay = workingCalendar.getDerivedWeekDay(day);
				String hours = workDay == null ? Messages.getString("TaskInspector.Inherit")
					: workDay.getWorkingHours() == null || !workDay.getWorkingHours().hasHours()
						? Messages.getString("TaskInspector.Nonworking") : workDay.getWorkingHours().toString();
				details.append("\n").append(days[day + 1]).append(": ").append(hours);
			}
		}
		return details.toString();
	}

	private static String summaryConstraint(Task selected) {
		for (Task parent = selected.getWbsParentTask(); parent != null; parent = parent.getWbsParentTask()) {
			if (isFixedConstraint(parent))
				return parent.getName() + ": " + parent.getConstraintTypeKind() + " — " + date(parent.getConstraintDate());
		}
		return Messages.getString("TaskInspector.NoneDetected");
	}

	private static boolean isFixedConstraint(Task task) {
		return task.getConstraintTypeKind() != com.microproject.pm.scheduling.ConstraintType.Kind.ASAP
			&& task.getConstraintTypeKind() != com.microproject.pm.scheduling.ConstraintType.Kind.ALAP;
	}

	private static String date(long value) {
		return value == 0 ? "—" : DateFormat.getDateTimeInstance().format(new java.util.Date(value));
	}

	private static String formatDuration(Task task, long encodedMillis) {
		return ((DurationFormat) DurationFormat.getSignedInstance())
			.format(new Duration(encodedMillis), task.getProject().getDurationDefaults());
	}

	private static String format(String key, Object... values) {
		return MessageFormat.format(Messages.getString(key), values);
	}

	private void section(String heading, String text) {
		JTextArea body = new JTextArea(text);
		body.setEditable(false);
		body.setLineWrap(true);
		body.setWrapStyleWord(true);
		body.setOpaque(false);
		section(heading, body);
	}

	private void section(String heading, java.awt.Component body) {
		JPanel block = new JPanel(new BorderLayout(0, 3));
		block.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		block.add(new JLabel(heading), BorderLayout.NORTH);
		block.add(body, BorderLayout.CENTER);
		content.add(block);
	}
}
