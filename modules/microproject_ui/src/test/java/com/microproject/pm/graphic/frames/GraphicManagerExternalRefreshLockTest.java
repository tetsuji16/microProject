/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class GraphicManagerExternalRefreshLockTest {
	@Test
	void lockVerificationFailureKeepsExternalChangePendingAndDoesNotApplyIt(@TempDir Path tempDirectory) throws Exception {
		Project source = projectWithTask("before external refresh");
		NormalTask sourceTask = (NormalTask) source.getTaskList().iterator().next();
		byte[] baseline = save(source);
		Project external = load(baseline);
		Task externalTask = external.findByUniqueId(sourceTask.getUniqueId());
		assertNotNull(externalTask, "MPO copy must retain the stable task identity");
		externalTask.setName("external update that must remain pending");

		Path externalFile = tempDirectory.resolve("external.mpo");
		Files.write(externalFile, save(external));
		Project target = load(baseline);
		target.setFileName(externalFile.toString());
		Task targetTask = target.findByUniqueId(sourceTask.getUniqueId());
		assertNotNull(targetTask);
		VerificationFailureSession session = new VerificationFailureSession(target, externalFile.toString());
		target.setCollaborationSession(session);

		TestDocumentFrame testFrame = allocateWithoutConstructor(TestDocumentFrame.class);
		testFrame.setProjectForTest(target);
		GraphicManager manager = new GraphicManager(new JPanel()) {
			@Override public DocumentFrame getCurrentFrame() { return testFrame; }
		};
		try {
			Method refresh = GraphicManager.class.getDeclaredMethod("refreshProjectFromExternalFile", Project.class);
			refresh.setAccessible(true);
			refresh.invoke(manager, target);

			assertEquals("before external refresh", targetTask.getName(),
				"a failed lock verification must return before applying values from the changed MPO");
			assertTrue(session.failureCallbackInvoked,
				"a failed lock verification must mark the external refresh as failed for retry");
			Field pending = CollaborationSession.class.getDeclaredField("externalChangePending");
			pending.setAccessible(true);
			assertTrue(pending.getBoolean(session), "failed lock verification must leave the external change pending");
		} finally {
			manager.cleanUp();
			session.stop();
		}
	}

	private static Project projectWithTask(String taskName) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("external-refresh-test", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName(taskName);
		project.recalculate();
		return project;
	}

	private static byte[] save(Project project) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, bytes), "MPO fixture save must succeed");
		return bytes.toByteArray();
	}

	private static Project load(byte[] bytes) throws Exception {
		return new MpoFileImporter().loadProject(new ByteArrayInputStream(bytes));
	}

	private static <T> T allocateWithoutConstructor(Class<T> type) throws Exception {
		Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
		return type.cast(unsafe.allocateInstance(type));
	}

	private static final class VerificationFailureSession extends CollaborationSession {
		private boolean failureCallbackInvoked;

		VerificationFailureSession(Project project, String fileName) {
			super(project, fileName, "refresh-test-user");
		}

		@Override
		public ExternalProjectRefreshLease beginExternalProjectRefresh() {
			throw new IllegalStateException("injected lease verification failure");
		}

		@Override
		public void afterExternalProjectRefreshFailure() {
			super.afterExternalProjectRefreshFailure();
			failureCallbackInvoked = true;
		}
	}

	private static final class TestDocumentFrame extends DocumentFrame {
		private TestDocumentFrame() {
			super(null, null, "unused-test-frame");
			throw new UnsupportedOperationException("allocated without initialization in this test");
		}

		private void setProjectForTest(Project project) {
			this.project = project;
		}
	}
}
