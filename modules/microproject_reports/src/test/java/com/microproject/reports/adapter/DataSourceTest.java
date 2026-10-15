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
package com.microproject.reports.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.NoSuchElementException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationDefaults;
import com.microproject.datatype.TimeUnit;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.grouping.core.transform.filtering.PredicatedNodeFilterIterator;
import com.microproject.grouping.core.Node;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class DataSourceTest {
	@Test
	void forwardsNodeBasedModeToPredicatedIterator() {
		TrackingPredicatedIterator iterator = new TrackingPredicatedIterator();
		DataSource dataSource = new DataSource();
		dataSource.setIterator(iterator);

		dataSource.setNodeBased(true);

		assertTrue(iterator.nodeBased);
	}

	@Test
	void forwardsStandardPredicateToFilterIterator() {
		TrackingPredicatedIterator iterator = new TrackingPredicatedIterator();
		DataSource dataSource = new DataSource();
		dataSource.setIterator(iterator);
		dataSource.setPredicate(value -> "accepted".equals(value));

		assertTrue(iterator.predicate.test("accepted"));
		assertFalse(iterator.predicate.test("rejected"));
	}

	@Test
	void returnsNodeModelDataFactoryWhenNodeModelProvidesOne() {
		NodeModelDataFactory expectedFactory = (NodeModelDataFactory) Proxy.newProxyInstance(
			NodeModelDataFactory.class.getClassLoader(),
			new Class<?>[] { NodeModelDataFactory.class },
			new DefaultHandler(null));
		NodeModel nodeModel = (NodeModel) Proxy.newProxyInstance(
			NodeModel.class.getClassLoader(),
			new Class<?>[] { NodeModel.class },
			new DefaultHandler(expectedFactory));
		DataSource dataSource = new DataSource();

		dataSource.setNodeModel(nodeModel);

		assertSame(expectedFactory, dataSource.getDataFactory());
	}

	@Test
	void taskDurationReportTextUsesPerTaskFormatRatherThanOnlyProjectDefault() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("duration-report", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		DurationDefaults defaults = new DurationDefaults(360.0D, 1800.0D, 20.0D,
			TimeUnit.HOURS, TimeUnit.MINUTES);
		project.setDurationDefaults(defaults);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.setDuration(Duration.getInstance(1.0D, TimeUnit.DAYS, defaults));
		Node node = (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[] { Node.class },
			(proxy, method, arguments) -> switch (method.getName()) {
				case "getImpl" -> task;
				case "isVoid", "isVirtual", "isRoot" -> Boolean.FALSE;
				case "toString" -> "duration report task";
				case "equals" -> proxy == arguments[0];
				case "hashCode" -> System.identityHashCode(proxy);
				default -> null;
			});
		DataSource dataSource = new DataSource();
		dataSource.setNodeBased(true);
		dataSource.setIterator(List.of(node).iterator());
		dataSource.setProject(project);
		dataSource.setNodeModel((WalkersNodeModel) Proxy.newProxyInstance(
			WalkersNodeModel.class.getClassLoader(), new Class<?>[] { WalkersNodeModel.class },
			(proxy, method, arguments) -> "isSummary".equals(method.getName()) ? Boolean.FALSE : null));
		assertTrue(dataSource.next());
		FieldContext context = new FieldContext();
		context.setDurationDefaultsResolver(project::getDurationDefaults);
		Field duration = Configuration.getFieldFromId("Field.duration");

		assertEquals("1 day", duration.getText(dataSource, context));
	}

	private static final class DefaultHandler implements InvocationHandler {
		private final NodeModelDataFactory factory;

		private DefaultHandler(NodeModelDataFactory factory) {
			this.factory = factory;
		}

		public Object invoke(Object proxy, Method method, Object[] args) {
			if ("getDataFactory".equals(method.getName())) {
				return factory;
			}
			Class<?> type = method.getReturnType();
			if (type.equals(Boolean.TYPE)) {
				return Boolean.FALSE;
			}
			if (type.equals(Integer.TYPE)) {
				return Integer.valueOf(0);
			}
			if (type.equals(Long.TYPE)) {
				return Long.valueOf(0L);
			}
			if (type.equals(Double.TYPE)) {
				return Double.valueOf(0.0d);
			}
			return null;
		}
	}

	private static final class TrackingPredicatedIterator implements PredicatedNodeFilterIterator {
		private boolean nodeBased;
		private java.util.function.Predicate<Object> predicate;

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			throw new NoSuchElementException();
		}

		@Override
		public void setPredicate(java.util.function.Predicate<Object> predicate) {
			this.predicate = predicate;
		}

		@Override
		public void setNodeBased(boolean nodeBased) {
			this.nodeBased = nodeBased;
		}
	}
}
