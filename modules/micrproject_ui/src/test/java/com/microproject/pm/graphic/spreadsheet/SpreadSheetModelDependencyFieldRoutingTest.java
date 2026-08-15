package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.field.Field;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.configuration.Configuration;

class SpreadSheetModelDependencyFieldRoutingTest {
	@Test
	void dependencyLagAndTypeAreResolvedByFieldId() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		Field lagField = Configuration.getFieldFromId("Field.lag");
		Field typeField = Configuration.getFieldFromId("Field.dependencyType");

		assertEquals(dependency.getLag(), SpreadSheetModel.getDependencyLag(typeField, new Duration(2L), dependency));
		assertEquals(dependency.getDependencyType(), SpreadSheetModel.getDependencyType(lagField, "SS", dependency));
	}

	@Test
	void dependencyTypeParsingRejectsInvalidText() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		Field typeField = Configuration.getFieldFromId("Field.dependencyType");

		assertThrows(IllegalArgumentException.class, () -> SpreadSheetModel.getDependencyType(typeField, "not-a-type", dependency));
	}
}
