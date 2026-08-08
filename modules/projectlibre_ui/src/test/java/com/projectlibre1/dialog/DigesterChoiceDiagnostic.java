package com.projectlibre1.dialog;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.projectlibre1.configuration.FieldDictionary;
import com.projectlibre1.field.DynamicSelect;
import com.projectlibre1.field.Field;
import com.projectlibre1.field.Select;

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
