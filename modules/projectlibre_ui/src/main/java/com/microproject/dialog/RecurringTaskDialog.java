package com.microproject.dialog;

import java.awt.Frame;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.dialog.util.ComponentFactory;
import com.microproject.dialog.util.ExtDateField;
import com.microproject.options.CalendarOption;
import com.microproject.pm.task.RecurringTaskSpec;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.DateTime;

public final class RecurringTaskDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;

	private final JTextField nameField = new JTextField();
	private final ExtDateField startDateField = ComponentFactory.createDateField();
	private final JTextField durationField = new JTextField();
	private final JComboBox<PatternOption> patternBox = new JComboBox<PatternOption>();
	private final JCheckBox[] weekdayBoxes = new JCheckBox[7];
	private final JRadioButton endByDateButton = new JRadioButton(Messages.getString("RecurringTaskDialog.EndByDate"));
	private final JRadioButton endAfterOccurrencesButton = new JRadioButton(Messages.getString("RecurringTaskDialog.EndAfterOccurrences"));
	private final ExtDateField endDateField = ComponentFactory.createDateField();
	private final JSpinner occurrenceSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 999, 1));
	private final JLabel monthlyHint = new JLabel(Messages.getString("RecurringTaskDialog.MonthlyHint"));
	private RecurringTaskSpec spec;

	public static RecurringTaskDialog getInstance(Frame owner) {
		return new RecurringTaskDialog(owner);
	}

	private RecurringTaskDialog(Frame owner) {
		super(owner, Messages.getString("RecurringTaskDialog.Title"), true);
		durationField.setText(DurationFormat.format(CalendarOption.getInstance().getDefaultDuration()));
		startDateField.setValue(new Date(CalendarOption.getInstance().makeValidStart(DateTime.midnightToday(), true)));
		endDateField.setValue(new Date(CalendarOption.getInstance().makeValidEnd(DateTime.midnightToday(), true)));
		patternBox.addItem(new PatternOption(RecurringTaskSpec.PatternType.DAILY, Messages.getString("RecurringTaskDialog.PatternDaily")));
		patternBox.addItem(new PatternOption(RecurringTaskSpec.PatternType.WEEKLY, Messages.getString("RecurringTaskDialog.PatternWeekly")));
		patternBox.addItem(new PatternOption(RecurringTaskSpec.PatternType.MONTHLY, Messages.getString("RecurringTaskDialog.PatternMonthly")));
		patternBox.addActionListener(e -> updateEnabledState());
		ButtonGroup group = new ButtonGroup();
		group.add(endByDateButton);
		group.add(endAfterOccurrencesButton);
		endAfterOccurrencesButton.setSelected(true);
		endByDateButton.addActionListener(e -> updateEnabledState());
		endAfterOccurrencesButton.addActionListener(e -> updateEnabledState());
		initializeWeekdayBoxes();
		updateEnabledState();
	}

	public RecurringTaskSpec getSpec() {
		return spec;
	}

	@Override
	public JComponent createContentPanel() {
		FormLayout layout = new FormLayout(
			"max(70dlu;pref), 3dlu, 180dlu:grow",
			"p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(Messages.getString("RecurringTaskDialog.Name"), nameField);
		builder.nextLine(2);
		builder.append(Messages.getString("RecurringTaskDialog.Start"), startDateField);
		builder.nextLine(2);
		builder.append(Messages.getString("RecurringTaskDialog.Duration"), durationField);
		builder.nextLine(2);
		builder.append(Messages.getString("RecurringTaskDialog.Pattern"), patternBox);
		builder.nextLine(2);
		builder.append(Messages.getString("RecurringTaskDialog.Weekdays"), createWeekdayPanel());
		builder.nextLine(2);
		builder.append(Messages.getString("RecurringTaskDialog.End"), createRangePanel());
		builder.nextLine(2);
		builder.append("", monthlyHint);
		return builder.getPanel();
	}

	@Override
	protected boolean bind(boolean get) {
		if (get)
			return true;
		ValidationResult result = validateAndBuildSpec(readSpecInput());
		if (!result.isValid()) {
			Alert.error(Messages.getString(result.getErrorKey()), this);
			return false;
		}
		spec = result.getSpec();
		return true;
	}

	@Override
	public ButtonPanel createButtonPanel() {
		createOkCancelButtons(Messages.getString("RecurringTaskDialog.Create"), Messages.getString("ButtonText.Cancel"));
		ButtonPanel panel = new ButtonPanel();
		panel.addButton(ok);
		panel.addButton(cancel);
		return panel;
	}

	private void initializeWeekdayBoxes() {
		String[] weekdays = new SimpleDateFormat("EEE", Locale.getDefault()).getDateFormatSymbols().getShortWeekdays();
		for (int i = 0; i < weekdayBoxes.length; i++) {
			int day = i + 1;
			String label = weekdays[day];
			if (label == null || label.trim().length() == 0)
				label = Integer.toString(day);
			weekdayBoxes[i] = new JCheckBox(label);
			if (day == Calendar.MONDAY)
				weekdayBoxes[i].setSelected(true);
		}
	}

	private JComponent createWeekdayPanel() {
		JPanel panel = new JPanel();
		for (JCheckBox box : weekdayBoxes)
			panel.add(box);
		return panel;
	}

	private JComponent createRangePanel() {
		JPanel panel = new JPanel();
		panel.add(endByDateButton);
		panel.add(endDateField);
		panel.add(endAfterOccurrencesButton);
		panel.add(occurrenceSpinner);
		return panel;
	}

	private Set<Integer> readWeeklyDays() {
		LinkedHashSet<Integer> result = new LinkedHashSet<Integer>();
		for (int i = 0; i < weekdayBoxes.length; i++) {
			if (weekdayBoxes[i].isSelected())
				result.add(Integer.valueOf(i + 1));
		}
		return result;
	}

	private void updateEnabledState() {
		PatternOption option = (PatternOption) patternBox.getSelectedItem();
		boolean weekly = option != null && option.type == RecurringTaskSpec.PatternType.WEEKLY;
		boolean monthly = option != null && option.type == RecurringTaskSpec.PatternType.MONTHLY;
		for (JCheckBox box : weekdayBoxes)
			box.setEnabled(weekly);
		endDateField.setEnabled(endByDateButton.isSelected());
		occurrenceSpinner.setEnabled(endAfterOccurrencesButton.isSelected());
		monthlyHint.setVisible(monthly);
	}

	private SpecInput readSpecInput() {
		PatternOption option = (PatternOption) patternBox.getSelectedItem();
		return new SpecInput(
			readTrimmedName(),
			startDateField.getDateValue(),
			durationField.getText(),
			option == null ? null : option.type,
			readWeeklyDays(),
			endByDateButton.isSelected(),
			endDateField.getDateValue(),
			((Number) occurrenceSpinner.getValue()).intValue());
	}

	private String readTrimmedName() {
		return nameField.getText() == null ? "" : nameField.getText().trim();
	}

	static ValidationResult validateAndBuildSpec(SpecInput input) {
		if (input.name.length() == 0)
			return ValidationResult.error("RecurringTaskDialog.ErrorName");
		if (input.startDate == null)
			return ValidationResult.error("RecurringTaskDialog.ErrorStart");
		Duration duration = parseDuration(input.durationText);
		if (duration == null)
			return ValidationResult.error("RecurringTaskDialog.ErrorDuration");
		if (input.patternType == RecurringTaskSpec.PatternType.WEEKLY && input.weeklyDays.isEmpty())
			return ValidationResult.error("RecurringTaskDialog.ErrorWeekdays");

		long start = CalendarOption.getInstance().makeValidStart(DateTime.gmt(input.startDate), true);
		RangeSelection range = resolveRangeSelection(input, start);
		if (!range.isValid())
			return ValidationResult.error(range.getErrorKey());

		try {
			return ValidationResult.success(new RecurringTaskSpec(
				input.name,
				start,
				duration.getEncodedMillis(),
				input.patternType,
				range.getRangeType(),
				range.getEndDate(),
				range.getOccurrences(),
				input.weeklyDays));
		} catch (IllegalArgumentException e) {
			return ValidationResult.error(mapSpecValidationError(e.getMessage()));
		}
	}

	private static Duration parseDuration(String durationText) {
		try {
			return (Duration) DurationFormat.getInstance().parseObject(durationText);
		} catch (ParseException e) {
			return null;
		}
	}

	private static RangeSelection resolveRangeSelection(SpecInput input, long start) {
		if (!input.endByDate) {
			if (input.occurrences <= 0)
				return RangeSelection.error("RecurringTaskDialog.ErrorOccurrences");
			return RangeSelection.success(RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES, 0L, input.occurrences);
		}
		if (input.endDate == null)
			return RangeSelection.error("RecurringTaskDialog.ErrorEndDate");
		long endDate = CalendarOption.getInstance().makeValidEnd(DateTime.gmt(input.endDate), true);
		if (endDate < start)
			return RangeSelection.error("RecurringTaskDialog.ErrorEndBeforeStart");
		return RangeSelection.success(RecurringTaskSpec.RangeType.END_BY_DATE, endDate, input.occurrences);
	}

	private static String mapSpecValidationError(String message) {
		if ("occurrenceCount must be positive".equals(message))
			return "RecurringTaskDialog.ErrorOccurrences";
		if ("endDate must not be before start".equals(message))
			return "RecurringTaskDialog.ErrorEndBeforeStart";
		if ("weeklyDays must not be empty".equals(message))
			return "RecurringTaskDialog.ErrorWeekdays";
		if ("patternType is required".equals(message))
			return "RecurringTaskDialog.ErrorDuration";
		return "RecurringTaskDialog.ErrorDuration";
	}

	private static final class PatternOption {
		private final RecurringTaskSpec.PatternType type;
		private final String label;

		private PatternOption(RecurringTaskSpec.PatternType type, String label) {
			this.type = type;
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	static final class SpecInput {
		private final String name;
		private final Date startDate;
		private final String durationText;
		private final RecurringTaskSpec.PatternType patternType;
		private final Set<Integer> weeklyDays;
		private final boolean endByDate;
		private final Date endDate;
		private final int occurrences;

		SpecInput(
			String name,
			Date startDate,
			String durationText,
			RecurringTaskSpec.PatternType patternType,
			Set<Integer> weeklyDays,
			boolean endByDate,
			Date endDate,
			int occurrences) {
			this.name = name == null ? "" : name;
			this.startDate = startDate;
			this.durationText = durationText;
			this.patternType = patternType;
			this.weeklyDays = weeklyDays == null
				? Collections.<Integer>emptySet()
				: new LinkedHashSet<Integer>(weeklyDays);
			this.endByDate = endByDate;
			this.endDate = endDate;
			this.occurrences = occurrences;
		}
	}

	static final class ValidationResult {
		private final RecurringTaskSpec spec;
		private final String errorKey;

		private ValidationResult(RecurringTaskSpec spec, String errorKey) {
			this.spec = spec;
			this.errorKey = errorKey;
		}

		static ValidationResult success(RecurringTaskSpec spec) {
			return new ValidationResult(spec, null);
		}

		static ValidationResult error(String errorKey) {
			return new ValidationResult(null, errorKey);
		}

		boolean isValid() {
			return spec != null;
		}

		RecurringTaskSpec getSpec() {
			return spec;
		}

		String getErrorKey() {
			return errorKey;
		}
	}

	private static final class RangeSelection {
		private final RecurringTaskSpec.RangeType rangeType;
		private final long endDate;
		private final int occurrences;
		private final String errorKey;

		private RangeSelection(
			RecurringTaskSpec.RangeType rangeType,
			long endDate,
			int occurrences,
			String errorKey) {
			this.rangeType = rangeType;
			this.endDate = endDate;
			this.occurrences = occurrences;
			this.errorKey = errorKey;
		}

		static RangeSelection success(RecurringTaskSpec.RangeType rangeType, long endDate, int occurrences) {
			return new RangeSelection(rangeType, endDate, occurrences, null);
		}

		static RangeSelection error(String errorKey) {
			return new RangeSelection(null, 0L, 0, errorKey);
		}

		boolean isValid() {
			return errorKey == null;
		}

		RecurringTaskSpec.RangeType getRangeType() {
			return rangeType;
		}

		long getEndDate() {
			return endDate;
		}

		int getOccurrences() {
			return occurrences;
		}

		String getErrorKey() {
			return errorKey;
		}
	}
}
