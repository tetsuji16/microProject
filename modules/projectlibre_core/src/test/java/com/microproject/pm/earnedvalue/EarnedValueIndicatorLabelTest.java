package com.microproject.pm.earnedvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.ImageLink;
import com.microproject.field.FieldContext;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class EarnedValueIndicatorLabelTest {
	@Test
	void taskScheduleStatusUsesSpiLabel() {
		Project project = createProject();
		NormalTask task = new NormalTask(project) {
			public double getCpi(FieldContext fieldContext) {
				return 1.2D;
			}
			public double getSpi(FieldContext fieldContext) {
				return 0.8D;
			}
		};

		assertEquals("SPI=0.8", task.getScheduleStatusIndicator().getLabel());
		assertEquals("CPI=1.2", task.getBudgetStatusIndicator().getLabel());
	}

	@Test
	void resourceScheduleStatusUsesSpiLabel() {
		EnterpriseResource resource = new EnterpriseResource((ResourcePool) null) {
			public double getCpi(FieldContext fieldContext) {
				return 1.2D;
			}
			public double getSpi(FieldContext fieldContext) {
				return 0.8D;
			}
		};

		ImageLink schedule = resource.getScheduleStatusIndicator();
		ImageLink budget = resource.getBudgetStatusIndicator();

		assertEquals("SPI=0.8", schedule.getLabel());
		assertEquals("CPI=1.2", budget.getLabel());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}
}
