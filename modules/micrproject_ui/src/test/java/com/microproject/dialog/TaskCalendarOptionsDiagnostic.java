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
import java.lang.reflect.Method;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.configuration.FieldDictionary;
import com.microproject.field.DynamicSelect;
import com.microproject.field.Field;
import com.microproject.field.Select;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.ClassUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Diagnostic: prove why Field.taskCalendar's JComboBox is empty.
 * Checks whether the DynamicSelect listMethod (resolved by reflection) is null.
 */
class TaskCalendarOptionsDiagnostic {

	@Test
	void checkTaskCalendarListMethod() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"skip on headless CI");

		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		Field f = FieldDictionary.getInstance().getFieldFromId("Field.taskCalendar");
		Select select = f.getSelect();
		String listMethodName = "com.microproject.pm.calendar.CalendarService.allBaseCalendars";
		Method resolved = ClassUtils.staticVoidMethodFromFullName(listMethodName);

		System.out.println("FIELD taskCalendar select=" + (select == null ? "null" : select.getClass().getName()));
		System.out.println("FIELD taskCalendar select.isStatic=" + (select == null ? "n/a" : select.isStatic()));
		System.out.println("LIST METHOD resolved=" + (resolved == null ? "NULL" : resolved.toGenericString()));
		System.out.println("CalendarService.allBaseCalendars() actual length=" + CalendarService.allBaseCalendars().length);
		System.out.println("Field.getOptions(task)=" + java.util.Arrays.toString(f.getOptions(task)));
		if (select instanceof DynamicSelect) {
			java.lang.reflect.Field lm = DynamicSelect.class.getDeclaredField("listMethod");
			lm.setAccessible(true);
			System.out.println("DynamicSelect.listMethod=" + lm.get(select));
		}
		assertNotNull(resolved, "listMethod should resolve for allBaseCalendars");
	}
}
