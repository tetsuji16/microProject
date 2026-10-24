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
package com.microproject.field;

import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

import com.microproject.strings.Messages;

/**
 * Display text and parsing for Yes/No fields (issue #899).
 *
 * Microsoft Project shows a Yes/No field as the text Yes or No and the Task Mode
 * field as Manually Scheduled or Auto Scheduled; the user picks one from a list.
 * The stored value stays a Boolean. Text input (typing, paste, the list) accepts
 * the field's displayed labels, the localized Yes/No, and true/false, yes/no, y/n
 * and 1/0; anything else is not a Yes/No value.
 */
public final class BooleanText {
	/** Microsoft Project's Task Mode field: Yes means manually scheduled. */
	public static final String TASK_MODE_FIELD_ID = "Field.manuallyScheduled";

	private static final List<String> TRUE_WORDS = List.of("true", "yes", "y", "1");
	private static final List<String> FALSE_WORDS = List.of("false", "no", "n", "0");

	private BooleanText() {
	}

	public static boolean isTaskMode(String fieldId) {
		return TASK_MODE_FIELD_ID.equals(fieldId);
	}

	/** The displayed text of a value; empty for null (a row without a value). */
	public static String label(String fieldId, Boolean value) {
		return label(fieldId, value, Messages::getString);
	}

	/** The list choices in Microsoft Project order: Yes then No, or Manually then Auto Scheduled. */
	public static List<String> choices(String fieldId) {
		return choices(fieldId, Messages::getString);
	}

	/** The Boolean for a text, or null when the text is not a Yes/No value of this field. */
	public static Boolean parse(String fieldId, String text) {
		return parse(fieldId, text, Messages::getString);
	}

	static String label(String fieldId, Boolean value, UnaryOperator<String> messages) {
		if (value == null)
			return "";
		return choices(fieldId, messages).get(value ? 0 : 1);
	}

	static List<String> choices(String fieldId, UnaryOperator<String> messages) {
		if (isTaskMode(fieldId))
			return List.of(messages.apply("TaskMode.manual"), messages.apply("TaskMode.auto"));
		return List.of(messages.apply("Text.Yes"), messages.apply("Text.No"));
	}

	static Boolean parse(String fieldId, String text, UnaryOperator<String> messages) {
		if (text == null)
			return null;
		String trimmed = text.trim();
		List<String> choices = choices(fieldId, messages);
		List<String> yesNo = choices(null, messages);
		if (trimmed.equalsIgnoreCase(choices.get(0)) || trimmed.equalsIgnoreCase(yesNo.get(0)))
			return Boolean.TRUE;
		if (trimmed.equalsIgnoreCase(choices.get(1)) || trimmed.equalsIgnoreCase(yesNo.get(1)))
			return Boolean.FALSE;
		String word = trimmed.toLowerCase(Locale.ROOT);
		if (TRUE_WORDS.contains(word))
			return Boolean.TRUE;
		if (FALSE_WORDS.contains(word))
			return Boolean.FALSE;
		return null;
	}
}
