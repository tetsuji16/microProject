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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

/**
 * Issue #899: Yes/No fields show Microsoft Project's text (Yes/No, and Manually /
 * Auto Scheduled for Task Mode) and accept that text, the localized Yes/No and
 * true/false; the stored value stays a Boolean.
 */
class BooleanTextTest {
	private static final String FLAG = "Field.critical";
	private static final String TASK_MODE = BooleanText.TASK_MODE_FIELD_ID;

	@Test
	void englishLabelsAndChoicesFollowMicrosoftProject() {
		UnaryOperator<String> en = messages(Locale.ROOT);
		assertEquals(List.of("Yes", "No"), BooleanText.choices(FLAG, en));
		assertEquals(List.of("Manually Scheduled", "Auto Scheduled"), BooleanText.choices(TASK_MODE, en));
		assertEquals("Yes", BooleanText.label(FLAG, true, en));
		assertEquals("Auto Scheduled", BooleanText.label(TASK_MODE, false, en));
		assertEquals("", BooleanText.label(FLAG, null, en), "a row without a value shows nothing");
		assertEquals("Task Mode", en.apply("Field.manuallyScheduled"));
	}

	@Test
	void japaneseLabelsAndChoicesFollowMicrosoftProject() {
		UnaryOperator<String> ja = messages(Locale.JAPANESE);
		assertEquals(List.of("はい", "いいえ"), BooleanText.choices(FLAG, ja));
		assertEquals(List.of("手動スケジュール", "自動スケジュール"),
			BooleanText.choices(TASK_MODE, ja));
		assertEquals("タスク モード", ja.apply("Field.manuallyScheduled"));
	}

	@Test
	void parseAcceptsDisplayedTextLocalizedYesNoAndBooleanWords() {
		UnaryOperator<String> ja = messages(Locale.JAPANESE);
		assertEquals(Boolean.TRUE, BooleanText.parse(FLAG, "はい", ja));
		assertEquals(Boolean.FALSE, BooleanText.parse(FLAG, " いいえ ", ja));
		assertEquals(Boolean.TRUE, BooleanText.parse(TASK_MODE, "手動スケジュール", ja));
		assertEquals(Boolean.FALSE, BooleanText.parse(TASK_MODE, "自動スケジュール", ja));
		assertEquals(Boolean.TRUE, BooleanText.parse(TASK_MODE, "はい", ja), "Task Mode is stored as Yes/No");
		for (String yes : new String[] {"true", "TRUE", "Yes", "y", "1"})
			assertEquals(Boolean.TRUE, BooleanText.parse(FLAG, yes, ja), yes);
		for (String no : new String[] {"false", "No", "N", "0"})
			assertEquals(Boolean.FALSE, BooleanText.parse(FLAG, no, ja), no);
		assertNull(BooleanText.parse(FLAG, "maybe", ja));
		assertNull(BooleanText.parse(FLAG, "自動スケジュール", ja),
			"Task Mode names are not values of other Yes/No fields");
		assertNull(BooleanText.parse(FLAG, null, ja));
	}

	@Test
	void everyChoiceParsesBackToItsValue() {
		for (Locale locale : new Locale[] {Locale.ROOT, Locale.JAPANESE}) {
			UnaryOperator<String> messages = messages(locale);
			for (String field : new String[] {FLAG, TASK_MODE}) {
				for (boolean value : new boolean[] {true, false})
					assertEquals(value, BooleanText.parse(field, BooleanText.label(field, value, messages), messages),
						locale + " " + field + " " + value);
			}
		}
	}

	private static UnaryOperator<String> messages(Locale locale) {
		ResourceBundle bundle = ResourceBundle.getBundle("com.microproject.strings.client", locale,
			ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
		assertEquals(locale, bundle.getLocale(), "the bundle for " + locale + " must exist");
		return bundle::getString;
	}
}
