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

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.configuration.FieldDictionary;
import com.microproject.field.DynamicSelect;
import com.microproject.field.Field;
import com.microproject.field.Select;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Diagnostic: check whether digester-built DynamicSelect (baseCalendar & taskCalendar)
 * actually has its listMethod resolved. If both are null, the digester choice
 * handling is broken and the JComboBoxes render empty.
 */
class DigesterChoiceDiagnostic {

	@Test
	void checkDigesterChoiceResolution() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "skip on headless CI");

		for (String id : new String[] { "Field.baseCalendar", "Field.taskCalendar" }) {
			Field f = FieldDictionary.getInstance().getFieldFromId(id);
			Select select = f.getSelect();
			String kind = select == null ? "null" : select.getClass().getSimpleName();
			String listMethod = "n/a";
			if (select instanceof DynamicSelect) {
				java.lang.reflect.Field lm = DynamicSelect.class.getDeclaredField("listMethod");
				lm.setAccessible(true);
				Object m = lm.get(select);
				listMethod = (m == null ? "NULL" : m.toString());
			}
			System.out.println("DIGESTER " + id + " select=" + kind + " listMethod=" + listMethod);
		}
		assertNotNull(FieldDictionary.getInstance().getFieldFromId("Field.taskCalendar"));
	}
}
