package com.microproject.grouping.core.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Tests for the per-column auto-filter model (issue #205).
 */
class ColumnValueFilterTest {

	@Test
	void keepsImplsWhoseFieldValueIsAccepted() throws Exception {
		Field name = field("Field.name");
		ColumnValueFilter filter = new ColumnValueFilter(name);
		filter.setAcceptedValues(List.of("Alpha", "Beta"), false);

		assertTrue(filter.matchesImpl(task("Alpha")));
		assertTrue(filter.matchesImpl(task("Beta")));
		assertFalse(filter.matchesImpl(task("Gamma")));
	}

	@Test
	void matchingIsCaseInsensitiveByDefault() throws Exception {
		Field name = field("Field.name");
		ColumnValueFilter filter = new ColumnValueFilter(name);
		filter.setAcceptedValues(List.of("alpha"), false);

		assertTrue(filter.matchesImpl(task("ALPHA")));
	}

	@Test
	void matchingIsCaseSensitiveWhenRequested() throws Exception {
		Field name = field("Field.name");
		ColumnValueFilter filter = new ColumnValueFilter(name);
		filter.setAcceptedValues(List.of("Alpha"), true);

		assertTrue(filter.matchesImpl(task("Alpha")));
		assertFalse(filter.matchesImpl(task("alpha")));
	}

	@Test
	void emptyAcceptedSetMeansInactiveAndShowsEverything() throws Exception {
		Field name = field("Field.name");
		ColumnValueFilter filter = new ColumnValueFilter(name);

		assertFalse(filter.isActive());
		assertTrue(filter.evaluate(null));
		assertTrue(filter.evaluate("not-a-node"));
		assertTrue(filter.matchesImpl(task("Anything")));
	}

	@Test
	void changingValuesFiresRedefinitionCallback() {
		Field name = field("Field.name");
		ColumnValueFilter filter = new ColumnValueFilter(name);
		AtomicInteger calls = new AtomicInteger();
		Consumer<Object> callback = ignored -> calls.incrementAndGet();
		filter.setRedefinitionCallBack(callback);

		filter.setAcceptedValues(List.of("Alpha"), false);
		assertEquals(1, calls.get());
		filter.setAcceptedValues(List.of("Beta"), false, false);
		assertEquals(1, calls.get(), "needCallback=false must not fire");
	}

	@Test
	void sessionCopyDoesNotShareMutableAcceptedValues() throws Exception {
		Field name = field("Field.name");
		ColumnValueFilter original = new ColumnValueFilter(name);
		original.setAcceptedValues(List.of("Alpha"), false, false);
		ViewTransformer configured = new ViewTransformer();
		configured.setHiddenFilter(original);

		ColumnValueFilter first = (ColumnValueFilter)configured.copyForSession().getHiddenFilter();
		ColumnValueFilter second = (ColumnValueFilter)configured.copyForSession().getHiddenFilter();
		assertNotSame(first, second);
		first.setAcceptedValues(List.of("Beta"), false, false);

		assertTrue(second.matchesImpl(task("Alpha")));
		assertFalse(second.matchesImpl(task("Beta")));
	}

	@Test
	void sessionCopyRejectsUnknownMutableTransformInsteadOfSharingIt() {
		ViewTransformer configured = new ViewTransformer();
		configured.setHiddenFilter(new NodeFilter() {
			@Override public boolean evaluate(Object value) { return true; }
		});

		assertThrows(IllegalStateException.class, configured::copyForSession);
	}

	private static Field field(String id) {
		return Configuration.getFieldFromId(id);
	}

	private static NormalTask task(String name) {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("column-value-filter-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		return task;
	}
}
