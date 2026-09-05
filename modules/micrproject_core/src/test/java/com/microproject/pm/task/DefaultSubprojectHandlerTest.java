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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.field.Field;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class DefaultSubprojectHandlerTest {
	@Test
	void storesSubprojectReferencesAndCreatesStatefulPlaceholder() {
		DefaultSubprojectHandler handler = new DefaultSubprojectHandler(null);

		handler.setReferringSubprojectTasks(Collections.emptyList());
		SubProj subproject = handler.createSubProj(42L);

		assertNotNull(subproject);
		assertTrue(subproject instanceof NormalTask);
		assertTrue(subproject instanceof com.microproject.grouping.core.LazyParent);
		assertEquals(42L, subproject.getSubprojectUniqueId());
		assertEquals(0L, handler.getReferringSubprojectTaskDependencyDate());
	}

	@Test
	void insertingASubprojectRecordsTheParentPlaceholderForPersistenceWithoutDuplicates() {
		Project master = newProject("master");
		Project subproject = newProject("subproject");
		subproject.setFileName("C:/plans/subproject.pod");
		DefaultSubProj placeholder = new DefaultSubProj(master, 42L);
		Node node = NodeFactory.getInstance().createNode(placeholder);
		DefaultSubprojectHandler handler = new DefaultSubprojectHandler(master);

		handler.addSubproject(subproject, node, true, false);
		handler.addSubproject(subproject, node, false, true);

		assertTrue(subproject.isOpenedAsSubproject());
		assertEquals("subproject.pod", placeholder.getName());
		assertEquals("subproject.pod", placeholder.getName(null));
		assertEquals("C:/plans/subproject.pod", placeholder.getSubprojectFile());
		assertEquals(SubProj.LoadStatus.OPEN, placeholder.getLoadStatus());
		assertFalse(placeholder.isSubprojectReadOnly());
		assertSame(placeholder, subproject.getContainingSubprojectTask());
		assertEquals(1, subproject.getReferringSubprojectTasks().size());
		assertSame(placeholder, subproject.getReferringSubprojectTasks().iterator().next());
	}

	@Test
	void savingAnUntitledMasterRebasesTheStoredChildPathToItsNewLocation() {
		Project master = newProject("master");
		Project child = newProject("child");
		child.setFileName("C:/plans/children/child.mpo");
		DefaultSubProj placeholder = new DefaultSubProj(master, child.getUniqueId());
		Node node = NodeFactory.getInstance().createNode(placeholder);
		master.addToDefaultOutline(null, node);
		new DefaultSubprojectHandler(master).addSubproject(child, node, true, false);
		assertEquals("C:/plans/children/child.mpo", placeholder.getStoredSubprojectPath().replace('\\', '/'));

		master.setFileName("C:/plans/master/master.mpo");

		assertEquals("../children/child.mpo", placeholder.getStoredSubprojectPath().replace('\\', '/'));
		assertEquals("C:/plans/children/child.mpo", placeholder.getCanonicalSubprojectPath().replace('\\', '/'));
	}

	@Test
	void allowsAnUnopenedProjectFileToBeInserted() {
		DefaultSubprojectHandler handler = new DefaultSubprojectHandler(newProject("master"));

		assertTrue(handler.canInsertProject(987654321L));
		assertFalse(handler.canInsertProject(0L));
	}

	@Test
	void identifiesDuplicateReferencesByCanonicalPath() {
		Project master = newProject("master");
		DefaultSubProj placeholder = new DefaultSubProj(master, 42L);
		placeholder.setSubprojectFile("C:/plans/child.mpo");
		master.connectTask(placeholder);
		master.addToDefaultOutline(null, NodeFactory.getInstance().createNode(placeholder));
		DefaultSubprojectHandler handler = new DefaultSubprojectHandler(master);

		assertTrue(handler.hasSubprojectReference("C:/plans/./child.mpo"));
		assertFalse(handler.hasSubprojectReference("C:/plans/other.mpo"));
	}

	@Test
	void rejectsAChildThatReferencesTheMaster() {
		Project master = newProject("master");
		Project candidate = newProject("candidate");
		DefaultSubProj backReference = new DefaultSubProj(candidate, master.getUniqueId());
		candidate.connectTask(backReference);
		candidate.addToDefaultOutline(null, NodeFactory.getInstance().createNode(backReference));
		DefaultSubprojectHandler handler = new DefaultSubprojectHandler(master);

		assertTrue(handler.wouldCreateCircularReference(candidate));
		assertEquals("master -> candidate -> master", handler.describeCircularReference(candidate));
		assertFalse(handler.canInsertProject(master.getUniqueId()));
	}

	@Test
	void preservesReadOnlyStateOnTheSubprojectPlaceholder() {
		Project master = newProject("master");
		Project subproject = newProject("subproject");
		subproject.setReadOnly(true);
		DefaultSubProj placeholder = new DefaultSubProj(master, 42L);

		new DefaultSubprojectHandler(master).addSubproject(subproject,
				NodeFactory.getInstance().createNode(placeholder), true, false);

		assertTrue(placeholder.isSubprojectReadOnly());
		assertFalse(placeholder.isWritable());
	}

	@Test
	void preservesExplicitReadOnlyInsertionModeForAnEditableChild() {
		Project master = newProject("master");
		Project subproject = newProject("subproject");
		DefaultSubProj placeholder = new DefaultSubProj(master, 42L);
		placeholder.setSubprojectReadOnly(true);

		new DefaultSubprojectHandler(master).addSubproject(subproject,
				NodeFactory.getInstance().createNode(placeholder), true, false);

		assertTrue(placeholder.isSubprojectReadOnly());
		assertFalse(placeholder.isWritable());
	}

	@Test
	void attachesLoadedChildTasksBelowTheMasterPlaceholderWithoutChangingTheirOwner() {
		Project master = newProject("master");
		Project child = newProject("child");
		NormalTask childTask = child.createScriptedTask();
		childTask.setName("Child task");
		DefaultSubProj placeholder = new DefaultSubProj(master, 42L);
		Node placeholderNode = NodeFactory.getInstance().createNode(placeholder);
		master.addToDefaultOutline(null, placeholderNode);

		new DefaultSubprojectHandler(master).addSubproject(child, placeholderNode, true, false);

		Node childNode = master.getTaskOutline().search(childTask);
		assertNotNull(childNode);
		assertSame(placeholderNode, childNode.getParent());
		assertSame(child, childTask.getOwningProject());
		assertEquals("child", childTask.getSourceProject());
		assertTrue(childTask.getUniqueId() > 0L, "the source task UID must remain available for the projected row");
		assertSame(master, childTask.getProject());
		assertTrue(childTask.isInSubproject());
		assertTrue(master.getTasks().contains(childTask));
	}

	@Test
	void replacingLoadedChildRestoresTheOldOutlineAndProjectsTheReloadedChild() {
		Project master = newProject("master");
		Project previous = newProject("previous");
		NormalTask discardedTask = previous.createScriptedTask();
		discardedTask.setName("Discarded local task");
		Project reloaded = newProject("reloaded");
		NormalTask diskTask = reloaded.createScriptedTask();
		diskTask.setName("Reloaded disk task");
		DefaultSubProj placeholder = new DefaultSubProj(master, 42L);
		Node placeholderNode = NodeFactory.getInstance().createNode(placeholder);
		master.addToDefaultOutline(null, placeholderNode);
		DefaultSubprojectHandler handler = new DefaultSubprojectHandler(master);

		handler.addSubproject(previous, placeholderNode, true, false);
		handler.replaceSubproject(previous, reloaded, placeholderNode);

		assertSame(previous.getTaskOutline().getRoot(), previous.getTaskOutline().search(discardedTask).getParent());
		assertFalse(master.getTasks().contains(discardedTask));
		assertSame(placeholderNode, master.getTaskOutline().search(diskTask).getParent());
		assertSame(master, diskTask.getProject());
		assertTrue(diskTask.isInSubproject());
	}

	@Test
	void replacingPortfolioChildPreservesTheReferenceIdentityAndUnregistersTheOldPoolMember() {
		Project previous = newProject("previous");
		Project replacement = newProject("replacement");
		replacement.setUniqueId(previous.getUniqueId());
		Portfolio portfolio = new Portfolio(null);
		portfolio.addProject(previous, false, false);

		assertTrue(portfolio.replaceProject(previous, replacement));

		assertSame(replacement, portfolio.findByUniqueId(previous.getUniqueId()));
		assertFalse(previous.getResourcePool().getProjects().contains(previous));
		assertTrue(replacement.getResourcePool().getProjects().contains(replacement));
	}

	@Test
	void readOnlySubprojectProjectionRejectsFieldEditsButWritableProjectionDoesNot() {
		Project master = newProject("master");
		Project child = newProject("child");
		NormalTask childTask = child.createScriptedTask();
		DefaultSubProj placeholder = new DefaultSubProj(master, 42L);
		placeholder.setSubprojectReadOnly(true);
		Node placeholderNode = NodeFactory.getInstance().createNode(placeholder);
		master.addToDefaultOutline(null, placeholderNode);
		new DefaultSubprojectHandler(master).addSubproject(child, placeholderNode, true, false);

		Field taskField = new Field();
		taskField.setClass(Task.class);
		assertTrue(taskField.isReadOnly(childTask, null));

		placeholder.setSubprojectReadOnly(false);
		assertFalse(taskField.isReadOnly(childTask, null));
	}

	@Test
	void loadsALocalSubprojectThroughTheLocalSession() {
		Project master = newProject("master");
		master.setLocal(true);

		com.microproject.session.LoadOptions options = ProjectFactory.subprojectLoadOptions(master, 42L);

		assertTrue(options.isSubproject());
		assertTrue(options.isLocal());
		assertEquals(42L, options.getId());
	}

	private static Project newProject(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.setName(name);
		return project;
	}

}
