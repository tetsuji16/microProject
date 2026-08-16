package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

import net.sf.mpxj.Duration;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Relation;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.Task;
import net.sf.mpxj.TimeUnit;

/**
 * Issue #155: MpxDependencyConverter used to convert an MPXJ relation lag
 * (value + unit) to a microproject Dependency lag by dropping the unit, so a
 * "2d" lag became 2 milliseconds instead of 172800000. Dependency lag is
 * stored in milliseconds (see Dependency.getLeadValue), so the unit must be
 * applied.
 */
public class MpxDependencyConverterLagTest {

	@Test
	public void lagInDaysIsConvertedToMilliseconds() {
		Project project = newProject();
		ProjectFile file = new ProjectFile();
		Task mpxPred = file.addTask();
		Task mpxSucc = file.addTask();
		MpxImportState state = buildState(project, mpxPred, mpxSucc);

		Relation relation = new Relation(mpxSucc, mpxPred, RelationType.FINISH_START,
				Duration.getInstance(2, TimeUnit.DAYS));
		Dependency dependency = new MpxDependencyConverter().from(relation, state);

		assertNotNull(dependency);
		assertEquals(2L * 24L * 60L * 60L * 1000L, dependency.getLag());
	}

	@Test
	public void lagInMinutesIsConvertedToMilliseconds() {
		Project project = newProject();
		ProjectFile file = new ProjectFile();
		Task mpxPred = file.addTask();
		Task mpxSucc = file.addTask();
		MpxImportState state = buildState(project, mpxPred, mpxSucc);

		Relation relation = new Relation(mpxSucc, mpxPred, RelationType.FINISH_START,
				Duration.getInstance(90, TimeUnit.MINUTES));
		Dependency dependency = new MpxDependencyConverter().from(relation, state);

		assertNotNull(dependency);
		assertEquals(90L * 60L * 1000L, dependency.getLag());
	}

	@Test
	public void negativeLagLeadIsPreserved() {
		Project project = newProject();
		ProjectFile file = new ProjectFile();
		Task mpxPred = file.addTask();
		Task mpxSucc = file.addTask();
		MpxImportState state = buildState(project, mpxPred, mpxSucc);

		Relation relation = new Relation(mpxSucc, mpxPred, RelationType.FINISH_START,
				Duration.getInstance(-1, TimeUnit.HOURS));
		Dependency dependency = new MpxDependencyConverter().from(relation, state);

		assertNotNull(dependency);
		assertEquals(-1L * 60L * 60L * 1000L, dependency.getLag());
	}

	@Test
	public void elapsedUnitLagDoesNotThrow() {
		Project project = newProject();
		ProjectFile file = new ProjectFile();
		Task mpxPred = file.addTask();
		Task mpxSucc = file.addTask();
		MpxImportState state = buildState(project, mpxPred, mpxSucc);

		// elapsed units (value >= 7) used to throw ArrayIndexOutOfBoundsException
		Relation relation = new Relation(mpxSucc, mpxPred, RelationType.FINISH_START,
				Duration.getInstance(1, TimeUnit.ELAPSED_DAYS));
		Dependency dependency = new MpxDependencyConverter().from(relation, state);

		assertNotNull(dependency);
		assertEquals(1440L * 60L * 1000L, dependency.getLag());
	}

	private Project newProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private MpxImportState buildState(Project project, Task mpxPred, Task mpxSucc) {
		NormalTask pred = new NormalTask(project);
		project.connectTask(pred);
		NormalTask succ = new NormalTask(project);
		project.connectTask(succ);
		MpxImportState state = new MpxImportState();
		state.mapTask(mpxPred, pred);
		state.mapTask(mpxSucc, succ);
		return state;
	}
}
