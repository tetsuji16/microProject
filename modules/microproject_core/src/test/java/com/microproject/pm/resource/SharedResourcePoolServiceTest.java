/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.document.ObjectEvent;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class SharedResourcePoolServiceTest {
	@Test
	void poolPrecedenceReusesOneResourceForAssignmentsAcrossProjects() {
		Project poolProject = newProject("pool", "C:/plans/resources.pod");
		Project sharer = newProject("sharer", "C:/plans/sharer.pod");
		Resource poolResource = addResource(poolProject, "Alex");
		Resource sharerResource = addResource(sharer, "Alex");
		// Same persisted identity represents the same shared resource.  Name is
		// deliberately not the identity key.
		poolResource.setUniqueId(101L);
		sharerResource.setUniqueId(101L);
		NormalTask task = new NormalTask(sharer);
		sharer.connectTask(task);
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, sharerResource, 1.0D, 0L, this, false);
		ResourcePool sharerPool = sharer.getResourcePool();
		long sharerRevision = sharerPool.getCoordinationRevision();
		long poolRevision = poolProject.getResourcePool().getCoordinationRevision();
		ExecutorService snapshotWorker = Executors.newSingleThreadExecutor();
		AtomicInteger callbacksOutsideCoordinator = new AtomicInteger();
		java.util.List<Long> sharerRevisions = new java.util.ArrayList<>();
		java.util.List<Long> poolRevisions = new java.util.ArrayList<>();
		java.util.List<Object> sharerObjects = new java.util.ArrayList<>();
		java.util.List<Object> poolObjects = new java.util.ArrayList<>();
		ObjectEvent.Listener sharerListener = event -> {
			try {
				Future<Boolean> snapshot = snapshotWorker.submit(() -> {
					try (var ignored = sharer.getMutationState().begin()) {
						return true;
					}
				});
				if (snapshot.get(2, TimeUnit.SECONDS)) callbacksOutsideCoordinator.incrementAndGet();
			} catch (Exception failure) {
				throw new AssertionError("shared pool observer ran while the global mutation barrier was held", failure);
			}
			sharerRevisions.add(event.getPublicationRevision());
			sharerObjects.add(event.getObject());
		};
		ObjectEvent.Listener poolListener = event -> {
			try {
				Future<Boolean> snapshot = snapshotWorker.submit(() -> {
					try (var ignored = sharer.getMutationState().begin()) {
						return true;
					}
				});
				if (snapshot.get(2, TimeUnit.SECONDS)) callbacksOutsideCoordinator.incrementAndGet();
			} catch (Exception failure) {
				throw new AssertionError("shared pool observer ran while the global mutation barrier was held", failure);
			}
			poolRevisions.add(event.getPublicationRevision());
			poolObjects.add(event.getObject());
		};
		poolProject.getResourcePool().addObjectListener(poolListener);
		sharerPool.addObjectListener(sharerListener);

		try {
			SharedResourcePoolService.getInstance().share(sharer, poolProject,
				SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE);
		} finally {
			poolProject.getResourcePool().removeObjectListener(poolListener);
			sharerPool.removeObjectListener(sharerListener);
			snapshotWorker.shutdownNow();
		}

		assertSame(poolProject.getResourcePool(), sharer.getResourcePool());
		assertSame(poolResource, assignment.getResource());
		assertEquals(1, poolProject.getResourcePool().getResourceList().size());
		assertTrue(callbacksOutsideCoordinator.get() >= 2,
			"both pool observers must run only after sharing releases the coordinator");
		assertEquals(java.util.List.of(sharerRevision + 1), sharerRevisions,
			"source-pool events must carry its own single operation revision");
		assertEquals(java.util.List.of(poolRevision + 1), poolRevisions,
			"destination-pool events must carry its own single operation revision");
		assertTrue(sharerObjects.contains(sharerResource));
		assertTrue(poolObjects.contains(poolResource));
		assertEquals("C:\\plans\\resources.pod", sharer.getSharedResourcePoolFile());
		assertTrue(sharer.isResourcePoolTakesPrecedence());
	}

	@Test
	void resolvesNegativePoolIdentityAfterItsFileHasMoved() {
		Project pool = newProject("pool", "C:/moved/resources.pod");
		pool.setUniqueId(-1001L);
		Project sharer = newProject("sharer", "C:/plans/sharer.pod");
		sharer.setSharedResourcePoolFile("C:/plans/resources.pod");
		sharer.setSharedResourcePoolProjectId(-1001L);
		assertTrue(SharedResourcePoolService.getInstance().resolve(sharer, java.util.List.of(pool)));
		assertSame(pool.getResourcePool(), sharer.getResourcePool());
	}

	@Test
	void resolvesAStoredPoolReferenceWhenThePoolProjectIsOpenedLater() {
		Project poolProject = newProject("pool", "C:/plans/resources.pod");
		Project sharer = newProject("sharer", "C:/plans/sharer.pod");
		sharer.setSharedResourcePoolFile("C:/plans/resources.pod");

		assertTrue(SharedResourcePoolService.getInstance().resolve(sharer, java.util.List.of(sharer, poolProject)));
		assertSame(poolProject.getResourcePool(), sharer.getResourcePool());
	}

	@Test
	void doesNotMergeSameNamedResourcesWithDifferentPersistentIds() {
		Project poolProject = newProject("pool", "C:/plans/resources.pod");
		Project sharer = newProject("sharer", "C:/plans/sharer.pod");
		Resource poolResource = addResource(poolProject, "Alex");
		poolResource.setUniqueId(101L);
		Resource sharerResource = addResource(sharer, "Alex");
		sharerResource.setUniqueId(202L);

		SharedResourcePoolService.getInstance().share(sharer, poolProject,
				SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE);

		assertEquals(2, poolProject.getResourcePool().getResourceList().size());
		assertTrue(poolProject.getResourcePool().getResourceList().contains(poolResource));
		assertTrue(poolProject.getResourcePool().getResourceList().contains(sharerResource));
	}

	@Test
	void doesNotUseAnAmbiguousLegacyNameToMergeResources() {
		Project poolProject = newProject("pool", "C:/plans/resources.pod");
		Project sharer = newProject("sharer", "C:/plans/sharer.pod");
		Resource firstPoolResource = addResource(poolProject, "Alex");
		Resource secondPoolResource = addResource(poolProject, "Alex");
		Resource sharerResource = addResource(sharer, "Alex");
		firstPoolResource.setUniqueId(0L);
		secondPoolResource.setUniqueId(0L);
		sharerResource.setUniqueId(0L);

		SharedResourcePoolService.getInstance().share(sharer, poolProject,
				SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE);

		assertEquals(3, poolProject.getResourcePool().getResourceList().size());
		assertTrue(poolProject.getResourcePool().getResourceList().contains(sharerResource));
	}

	@Test
	void rejectsAnUnsavedPoolBecauseItsReferenceCouldNotSurviveReopen() {
		Project poolProject = newProject("pool", null);
		Project sharer = newProject("sharer", "C:/plans/sharer.pod");

		assertThrows(IllegalStateException.class, () -> SharedResourcePoolService.getInstance().share(sharer, poolProject,
			SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE));
		assertTrue(sharer.getSharedResourcePoolFile() == null);
	}

	@Test
	void matchesAStoredPoolReferenceOnlyByCanonicalFileIdentity() {
		Project poolProject = newProject("pool", "C:/plans/resources.pod");
		Project sharer = newProject("sharer", "C:/plans/sharer.pod");
		sharer.setSharedResourcePoolFile("C:/plans/resources.pod");

		assertTrue(SharedResourcePoolService.getInstance().isPoolReference(sharer, poolProject));
	}

	@Test
	void missingStoredPoolIsExplicitlyUnresolvedUntilThePoolIsOpened() {
		Project sharer = newProject("sharer", "C:/plans/sharer.pod");
		sharer.setSharedResourcePoolFile("C:/plans/missing-resources.pod");

		assertTrue(sharer.isSharedResourcePoolUnresolved());
		assertTrue(!SharedResourcePoolService.getInstance().resolve(sharer, java.util.List.of(sharer)));
		assertTrue(sharer.isSharedResourcePoolUnresolved());
	}

	private static Project newProject(String name, String fileName) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.setName(name);
		project.setFileName(fileName);
		return project;
	}

	private static Resource addResource(Project project, String name) {
		Resource resource = project.getResourcePool().newResourceInstance();
		resource.setName(name);
		return resource;
	}
}
