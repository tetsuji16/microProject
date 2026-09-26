/*******************************************************************************
 * MIT License
 *
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ProjectChoiceKindTest {
	@Test
	@SuppressWarnings("deprecation")
	void projectTypeAndStatusKindsRoundTripStableCodes() {
		Project project = createProject();

		project.setProjectTypeKind(ProjectType.Kind.IT);
		project.setProjectStatusKind(ProjectStatus.Kind.ON_HOLD);

		assertEquals(ProjectType.Kind.IT, project.getProjectTypeKind());
		assertEquals(ProjectStatus.Kind.ON_HOLD, project.getProjectStatusKind());
		assertEquals(ProjectType.Kind.IT.code(), project.getProjectType());
		assertEquals(ProjectStatus.Kind.ON_HOLD.code(), project.getProjectStatus());
	}

	@Test
	@SuppressWarnings("deprecation")
	void unknownLegacyCodesRemainAvailableThroughIntegerAccessors() {
		Project project = createProject();
		project.setProjectType(99);
		project.setProjectStatus(98);

		assertEquals(99, project.getProjectType());
		assertEquals(98, project.getProjectStatus());
		assertThrows(IllegalArgumentException.class, project::getProjectTypeKind);
		assertThrows(IllegalArgumentException.class, project::getProjectStatusKind);
	}

	@Test
	void accessControlPolicyKindAdaptsThePersistedCodeAndKeepsUnknownValues() {
		Project project = createProject();
		project.setAccessControlPolicy(AccessControlPolicy.Kind.RESTRICTED);

		assertEquals(AccessControlPolicy.Kind.RESTRICTED, project.getAccessControlPolicyKind());
		assertEquals(AccessControlPolicy.Kind.RESTRICTED.code(), project.getAccessControlPolicy());

		project.setAccessControlPolicy(99);
		assertEquals(99, project.getAccessControlPolicy());
		assertNull(project.getAccessControlPolicyKind());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}
}
