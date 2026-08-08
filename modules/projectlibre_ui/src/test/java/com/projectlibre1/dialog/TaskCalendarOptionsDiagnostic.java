package com.projectlibre1.dialog;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.projectlibre1.configuration.FieldDictionary;
import com.projectlibre1.field.DynamicSelect;
import com.projectlibre1.field.Field;
import com.projectlibre1.field.Select;
import com.projectlibre1.pm.calendar.CalendarService;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;
import com.projectlibre1.util.ClassUtils;

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
		String listMethodName = "com.projectlibre1.pm.calendar.CalendarService.allBaseCalendars";
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
