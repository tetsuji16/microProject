package com.projectlibre1.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.projectlibre1.datatype.Duration;
import com.projectlibre1.field.Field;
import com.projectlibre1.pm.dependency.Dependency;
import com.projectlibre1.pm.dependency.DependencyService;
import com.projectlibre1.pm.dependency.DependencyType;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;
import com.projectlibre1.configuration.Configuration;

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
