package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.SharedResourcePoolService;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.undo.DataFactoryUndoController;

class ProjectFactorySubprojectReplacementTest {
	@Test
	void restoresSharedPoolResolutionWhenLinkedReplacementFailsAfterResolution() {
		ProjectFactory factory = ProjectFactory.getInstance();
		Portfolio originalPortfolio = factory.portfolio;
		ResourcePoolFactory poolFactory = ResourcePoolFactory.getInstance();
		List<ResourcePool> originalPools = new ArrayList<>(poolFactory.getResourcePools());
		List<Project> projects = new ArrayList<>();
		factory.portfolio = new Portfolio(factory);
		try {
			Project master = newProject("master"); projects.add(master);
			Project previous = newProject("previous"); projects.add(previous);
			NormalTask oldTask = previous.createScriptedTask();
			Project replacement = newProject("replacement"); projects.add(replacement);
			NormalTask replacementTask = replacement.createScriptedTask(); replacementTask.setUniqueId(oldTask.getUniqueId());
			Project pool = Project.createProject(ResourcePool.createRourcePool("pool", new DataFactoryUndoController()), new DataFactoryUndoController());
			projects.add(pool); pool.setFileName("C:/plans/shared-pool.mpo");
			Resource poolResource = pool.getResourcePool().newResourceInstance(); poolResource.setName("Alex"); poolResource.setUniqueId(501L);
			Resource replacementResource = replacement.getResourcePool().newResourceInstance(); replacementResource.setName("Alex"); replacementResource.setUniqueId(501L);
			Assignment replacementAssignment = AssignmentService.getInstance().newAssignment(replacementTask, replacementResource, 1.0D, 0L, this, false);
			replacement.setSharedResourcePoolFile(pool.getFileName()); replacement.setSharedResourcePoolProjectId(pool.getUniqueId());
			Project linked = newProject("linked"); projects.add(linked);
			NormalTask external = new NormalTask(linked); external.setExternal(true);
			external.setProjectId(previous.getUniqueId()); external.setUniqueId(oldTask.getUniqueId());
			NormalTask successor = linked.createScriptedTask(); linked.connectTask(external);
			Dependency externalDependency = Dependency.getInstance(external, successor);
			external.getSuccessorList().add(externalDependency); successor.getPredecessorList().add(externalDependency);
			DependencyService.getInstance().updateSentinels(externalDependency);
			DefaultSubProj reference = new DefaultSubProj(master, previous.getUniqueId());
			Node referenceNode = NodeFactory.getInstance().createNode(reference); master.addToDefaultOutline(null, referenceNode);
			for (Project project : List.of(master, previous, pool, linked)) factory.getPortfolio().addProject(project, false, false);
			linked.addExternalTask(external);
			master.getSubprojectHandler().addSubproject(previous, referenceNode, true, false); master.setInitialized(true);
			long[] revisions = projects.stream().mapToLong(p -> p.getMutationState().revision()).toArray();
			long poolRevision = pool.getResourcePool().getCoordinationRevision();
			AtomicInteger notifications = new AtomicInteger();
			for (Project project : projects) project.addObjectListener(e -> notifications.incrementAndGet());
			factory.getPortfolio().addObjectListener(e -> notifications.incrementAndGet());
			RuntimeException expected = new RuntimeException("after shared pool resolution");
			factory.getPortfolio().setAfterSharedResourcePoolResolution(() -> { throw expected; });
			RuntimeException actual;
			try { actual = assertThrows(RuntimeException.class, () -> factory.replaceOpenSubproject(master, reference, replacement)); }
			finally { factory.getPortfolio().setAfterSharedResourcePoolResolution(null); }
			assertSame(expected, actual);
			assertSame(previous, reference.getSubproject()); assertEquals(SubProj.LoadStatus.OPEN, reference.getLoadStatus());
			assertSame(previous, factory.getPortfolio().findByUniqueId(previous.getUniqueId()));
			assertSame(referenceNode, previous.getTaskOutline().search(oldTask).getParent());
			assertSame(replacement.getTaskOutline().getRoot(), replacement.getTaskOutline().search(replacementTask).getParent());
			assertSame(replacementResource, replacement.getResourcePool().getResourceList().get(0));
			assertSame(replacementResource, replacementAssignment.getResource());
			assertSame(external, externalDependency.getPredecessor()); assertTrue(external.isExternal());
			assertFalse(replacement.getResourcePool().getProjects().contains(replacement));
			assertEquals(List.of(pool), pool.getResourcePool().getProjects());
			assertSame(replacement.getResourcePool(), ((ResourceImpl) replacementResource).getGlobalResource().getResourcePool());
			assertEquals(pool.getFileName(), replacement.getSharedResourcePoolFile());
			assertTrue(replacement.isSharedResourcePoolUnresolved());
			assertEquals(poolRevision, pool.getResourcePool().getCoordinationRevision());
			assertEquals(0, notifications.get());
			for (int i = 0; i < projects.size(); i++) assertEquals(revisions[i], projects.get(i).getMutationState().revision());
			for (Project project : projects) assertFalse(((DataFactoryUndoController) project.getUndoController()).canUndo());
		} finally {
			factory.getPortfolio().setAfterSharedResourcePoolResolution(null);
			factory.portfolio = originalPortfolio;
			for (Project project : projects) project.getResourcePool().removeProject(project);
			poolFactory.getResourcePools().removeIf(pool -> !originalPools.contains(pool));
		}
	}
	@Test
	void replacesLinkedChildProjectionAndPortfolioMembershipOnSuccess() {
		ProjectFactory factory = ProjectFactory.getInstance();
		Portfolio originalPortfolio = factory.portfolio;
		ResourcePoolFactory resourcePoolFactory = ResourcePoolFactory.getInstance();
		List<ResourcePool> originalPools = new ArrayList<>(resourcePoolFactory.getResourcePools());
		List<Project> fixtureProjects = new ArrayList<>();
		factory.portfolio = new Portfolio(factory);
		try {
			Project master = newProject("master");
			fixtureProjects.add(master);
			Project previous = newProject("previous");
			fixtureProjects.add(previous);
			NormalTask oldTask = previous.createScriptedTask();
			oldTask.setName("old child task");
			Project replacement = newProject("replacement");
			fixtureProjects.add(replacement);
			NormalTask replacementTask = replacement.createScriptedTask();
			replacementTask.setName("replacement child task");
			replacementTask.setUniqueId(oldTask.getUniqueId());
			Project linked = newProject("linked");
			fixtureProjects.add(linked);
			NormalTask placeholder = new NormalTask(linked);
			placeholder.setExternal(true);
			placeholder.setProjectId(previous.getUniqueId());
			placeholder.setUniqueId(oldTask.getUniqueId());
			NormalTask successor = linked.createScriptedTask();
			linked.connectTask(placeholder);
			Dependency externalLink = Dependency.getInstance(placeholder, successor);
			placeholder.getSuccessorList().add(externalLink);
			successor.getPredecessorList().add(externalLink);
			DependencyService.getInstance().updateSentinels(externalLink);
			DefaultSubProj reference = new DefaultSubProj(master, previous.getUniqueId());
			reference.setLoadStatus(SubProj.LoadStatus.INVALID);
			Node referenceNode = NodeFactory.getInstance().createNode(reference);
			master.addToDefaultOutline(null, referenceNode);
			factory.getPortfolio().addProject(master, false, false);
			factory.getPortfolio().addProject(previous, false, false);
			factory.getPortfolio().addProject(linked, false, false);
			linked.addExternalTask(placeholder);
			master.getSubprojectHandler().addSubproject(previous, referenceNode, true, false);
			reference.setLoadStatus(SubProj.LoadStatus.INVALID);
			master.setInitialized(true);

			long masterRevision = master.getMutationState().revision();
			long previousRevision = previous.getMutationState().revision();
			long replacementRevision = replacement.getMutationState().revision();
			AtomicInteger masterNotifications = new AtomicInteger();
			AtomicInteger portfolioNotifications = new AtomicInteger();
			master.addObjectListener(event -> masterNotifications.incrementAndGet());
			factory.getPortfolio().addObjectListener(event -> portfolioNotifications.incrementAndGet());

			assertTrue(factory.replaceOpenSubproject(master, reference, replacement));

			assertSame(replacement, reference.getSubproject());
			assertEquals(SubProj.LoadStatus.OPEN, reference.getLoadStatus());
			assertEquals(1, referenceNode.getChildCount());
			assertSame(replacementTask, ((Node) referenceNode.getChildAt(0)).getImpl());
			assertSame(replacementTask, master.getTaskOutline().search(replacementTask).getImpl());
			assertSame(replacement, replacementTask.getOwningProject());
			assertSame(master, replacementTask.getProject());
			assertTrue(replacementTask.isInSubproject());
			assertTrue(master.getTasks().contains(replacementTask));
			assertFalse(master.getTasks().contains(oldTask));
			assertSame(previous.getTaskOutline().getRoot(), previous.getTaskOutline().search(oldTask).getParent());
			assertSame(replacement, factory.getPortfolio().findByUniqueId(previous.getUniqueId()));
			assertFalse(previous.getResourcePool().getProjects().contains(previous));
			assertTrue(replacement.getResourcePool().getProjects().contains(replacement));
			assertSame(replacementTask, externalLink.getPredecessor());
			assertFalse(placeholder.isExternal());
			assertEquals(0, linked.retainedExternalTaskResolutionCount(), "committed replacement must discard inverse snapshots");
			assertTrue(masterNotifications.get() > 0);
			assertTrue(portfolioNotifications.get() > 0);
			assertTrue(master.getMutationState().revision() > masterRevision);
			assertTrue(previous.getMutationState().revision() > previousRevision);
			assertTrue(replacement.getMutationState().revision() > replacementRevision);
		} finally {
			factory.portfolio = originalPortfolio;
			for (Project project : fixtureProjects)
				project.getResourcePool().removeProject(project);
			resourcePoolFactory.getResourcePools().removeIf(pool -> !originalPools.contains(pool));
		}
	}

	@Test
	void ordinaryExternalTaskResolutionDoesNotRetainRollbackSnapshots() {
		Project owner = newProject("ordinary-external-owner");
		Project target = newProject("ordinary-external-target");
		NormalTask placeholder = new NormalTask(owner);
		placeholder.setExternal(true);
		NormalTask realTask = target.createScriptedTask();
		placeholder.setProjectId(target.getUniqueId());
		placeholder.setUniqueId(realTask.getUniqueId());
		NormalTask successor = owner.createScriptedTask();
		owner.connectTask(placeholder);
		Dependency dependency = Dependency.getInstance(placeholder, successor);
		placeholder.getSuccessorList().add(dependency);
		successor.getPredecessorList().add(dependency);
		DependencyService.getInstance().updateSentinels(dependency);
		owner.initialize(false, false);
		target.initialize(false, false);
		owner.addExternalTask(placeholder);

		owner.handleExternalTasks(target, true, false);

		assertSame(realTask, dependency.getPredecessor());
		assertFalse(placeholder.isExternal());
		assertFalse(owner.getTaskList().contains(placeholder));
		assertEquals(0, owner.retainedExternalTaskResolutionCount());
	}

	@Test
	void restoresProjectionWhenFailureOccursBeforePortfolioReplacement() {
		ProjectFactory factory = ProjectFactory.getInstance();
		Portfolio originalPortfolio = factory.portfolio;
		ResourcePoolFactory resourcePoolFactory = ResourcePoolFactory.getInstance();
		List<ResourcePool> originalPools = new ArrayList<>(resourcePoolFactory.getResourcePools());
		List<Project> fixtureProjects = new ArrayList<>();
		factory.portfolio = new Portfolio(factory);
		try {
			assertRestorationAfterProjectionFailure(factory, fixtureProjects, false);
		} finally {
			factory.setAfterSubprojectProjection(null);
			factory.getPortfolio().setAfterProjectMembershipReplacement(null);
			factory.portfolio = originalPortfolio;
			for (Project project : fixtureProjects)
				project.getResourcePool().removeProject(project);
			resourcePoolFactory.getResourcePools().removeIf(pool -> !originalPools.contains(pool));
		}
	}

	@Test
	void restoresPortfolioAndProjectionWhenFailureOccursAfterPortfolioMembershipChanges() {
		ProjectFactory factory = ProjectFactory.getInstance();
		Portfolio originalPortfolio = factory.portfolio;
		ResourcePoolFactory resourcePoolFactory = ResourcePoolFactory.getInstance();
		List<ResourcePool> originalPools = new ArrayList<>(resourcePoolFactory.getResourcePools());
		List<Project> fixtureProjects = new ArrayList<>();
		factory.portfolio = new Portfolio(factory);
		try {
			assertRestorationAfterProjectionFailure(factory, fixtureProjects, true);
		} finally {
			factory.setAfterSubprojectProjection(null);
			factory.getPortfolio().setAfterProjectMembershipReplacement(null);
			factory.portfolio = originalPortfolio;
			for (Project project : fixtureProjects)
				project.getResourcePool().removeProject(project);
			resourcePoolFactory.getResourcePools().removeIf(pool -> !originalPools.contains(pool));
		}
	}

	@Test
	void restoresExternalLinksWhenReplacementFailsAfterRebindingBeforeSharedPoolResolution() {
		ProjectFactory factory = ProjectFactory.getInstance();
		Portfolio originalPortfolio = factory.portfolio;
		ResourcePoolFactory resourcePoolFactory = ResourcePoolFactory.getInstance();
		List<ResourcePool> originalPools = new ArrayList<>(resourcePoolFactory.getResourcePools());
		List<Project> fixtureProjects = new ArrayList<>();
		factory.portfolio = new Portfolio(factory);
		try {
			Project master = newProject("master"); fixtureProjects.add(master);
			Project previous = newProject("previous"); fixtureProjects.add(previous);
			NormalTask oldTask = previous.createScriptedTask();
			Project replacement = newProject("replacement"); fixtureProjects.add(replacement);
			NormalTask replacementTask = replacement.createScriptedTask();
			replacementTask.setUniqueId(oldTask.getUniqueId());
			Project linked = newProject("linked"); fixtureProjects.add(linked);
			NormalTask placeholder = new NormalTask(linked);
			placeholder.setExternal(true);
			placeholder.setProjectId(previous.getUniqueId());
			placeholder.setUniqueId(oldTask.getUniqueId());
			NormalTask successor = linked.createScriptedTask();
			linked.connectTask(placeholder);
			Dependency dependency = Dependency.getInstance(placeholder, successor);
			placeholder.getSuccessorList().add(dependency);
			successor.getPredecessorList().add(dependency);
			DependencyService.getInstance().updateSentinels(dependency);
			DefaultSubProj reference = new DefaultSubProj(master, previous.getUniqueId());
			Node referenceNode = NodeFactory.getInstance().createNode(reference);
			master.addToDefaultOutline(null, referenceNode);
			for (Project project : List.of(master, previous, linked)) factory.getPortfolio().addProject(project, false, false);
			linked.addExternalTask(placeholder);
			master.getSubprojectHandler().addSubproject(previous, referenceNode, true, false);
			master.setInitialized(true);

			List<Dependency> placeholderSuccessors = new ArrayList<>();
			for (Object item : placeholder.getSuccessorList()) placeholderSuccessors.add((Dependency) item);
			List<Dependency> successorPredecessors = new ArrayList<>();
			for (Object item : successor.getPredecessorList()) successorPredecessors.add((Dependency) item);
			long[] revisions = fixtureProjects.stream().mapToLong(project -> project.getMutationState().revision()).toArray();
			AtomicInteger notifications = new AtomicInteger();
			for (Project project : fixtureProjects) project.addObjectListener(event -> notifications.incrementAndGet());
			factory.getPortfolio().addObjectListener(event -> notifications.incrementAndGet());
			DataFactoryUndoController undo = (DataFactoryUndoController) master.getUndoController();
			RuntimeException expected = new RuntimeException("injected after external task rebinding");
			factory.getPortfolio().setAfterExternalTaskRebinding(() -> { throw expected; });
			RuntimeException actual;
			try {
				actual = assertThrows(RuntimeException.class, () -> factory.replaceOpenSubproject(master, reference, replacement));
			} finally {
				factory.getPortfolio().setAfterExternalTaskRebinding(null);
			}

			assertSame(expected, actual);
			assertEquals(0, linked.retainedExternalTaskResolutionCount(), "rolled-back snapshots must be discarded");
			assertSame(placeholder, dependency.getPredecessor());
			assertSame(successor, dependency.getSuccessor());
			assertEquals(placeholderSuccessors, list(placeholder.getSuccessorList()));
			assertEquals(successorPredecessors, list(successor.getPredecessorList()));
			assertTrue(placeholder.isExternal());
			assertTrue(linked.getTaskList().contains(placeholder));
			assertSame(previous, reference.getSubproject());
			assertSame(previous, factory.getPortfolio().findByUniqueId(previous.getUniqueId()));
			assertSame(referenceNode, master.getTaskOutline().search(oldTask).getParent());
			assertSame(replacement.getTaskOutline().getRoot(), replacement.getTaskOutline().search(replacementTask).getParent());
			assertTrue(previous.getResourcePool().getProjects().contains(previous));
			assertFalse(replacement.getResourcePool().getProjects().contains(replacement));
			assertFalse(undo.canUndo());
			for (Project project : fixtureProjects) assertFalse(((DataFactoryUndoController) project.getUndoController()).canUndo());
			assertEquals(0, notifications.get());
			for (int i = 0; i < fixtureProjects.size(); i++) assertEquals(revisions[i], fixtureProjects.get(i).getMutationState().revision());
		} finally {
			factory.getPortfolio().setAfterExternalTaskRebinding(null);
			factory.portfolio = originalPortfolio;
			for (Project project : fixtureProjects) project.getResourcePool().removeProject(project);
			resourcePoolFactory.getResourcePools().removeIf(pool -> !originalPools.contains(pool));
		}
	}

	private static List<Dependency> list(Iterable<?> dependencies) {
		List<Dependency> result = new ArrayList<>();
		for (Object dependency : dependencies) result.add((Dependency) dependency);
		return result;
	}

	private static void assertRestorationAfterProjectionFailure(ProjectFactory factory, List<Project> fixtureProjects, boolean failAfterPortfolioMembership) {
		RuntimeException expected = new RuntimeException(failAfterPortfolioMembership
				? "injected after portfolio membership" : "injected after projection");
		Project master = newProject("master");
		fixtureProjects.add(master);
		Project previous = newProject("previous");
		fixtureProjects.add(previous);
		NormalTask oldTask = previous.createScriptedTask();
		oldTask.setName("old child task");
		Project replacement = newProject("replacement");
		fixtureProjects.add(replacement);
		NormalTask replacementTask = replacement.createScriptedTask();
		DefaultSubProj reference = new DefaultSubProj(master, previous.getUniqueId());
		Node referenceNode = NodeFactory.getInstance().createNode(reference);
		master.addToDefaultOutline(null, referenceNode);
		factory.getPortfolio().addProject(master, false, false);
		factory.getPortfolio().addProject(previous, false, false);
		master.getSubprojectHandler().addSubproject(previous, referenceNode, true, false);
		Object oldTaskParent = master.getTaskOutline().search(oldTask).getParent();
		assertSame(referenceNode, oldTaskParent);

		long masterRevision = master.getMutationState().revision();
		long childRevision = previous.getMutationState().revision();
		long replacementRevision = replacement.getMutationState().revision();
		AtomicInteger notifications = new AtomicInteger();
		master.addObjectListener(event -> notifications.incrementAndGet());
		previous.addObjectListener(event -> notifications.incrementAndGet());
		replacement.addObjectListener(event -> notifications.incrementAndGet());
		factory.getPortfolio().addObjectListener(event -> notifications.incrementAndGet());
		DataFactoryUndoController undo = (DataFactoryUndoController) master.getUndoController();

		if (failAfterPortfolioMembership)
			factory.getPortfolio().setAfterProjectMembershipReplacement(() -> { throw expected; });
		else
			factory.setAfterSubprojectProjection(() -> { throw expected; });
		RuntimeException actual;
		try {
			actual = assertThrows(RuntimeException.class,
				() -> factory.replaceOpenSubproject(master, reference, replacement));
		} finally {
			factory.setAfterSubprojectProjection(null);
			factory.getPortfolio().setAfterProjectMembershipReplacement(null);
		}

		assertSame(expected, actual);
		assertSame(oldTaskParent, master.getTaskOutline().search(oldTask).getParent());
		assertEquals(1, referenceNode.getChildCount());
		assertSame(oldTask, ((Node) referenceNode.getChildAt(0)).getImpl());
		assertSame(previous, oldTask.getOwningProject());
		assertSame(master, oldTask.getProject());
		assertTrue(oldTask.isInSubproject());
		assertEquals("previous", oldTask.getSourceProject());
		assertTrue(master.getTasks().contains(oldTask));
		assertFalse(master.getTasks().contains(replacementTask));
		assertSame(replacement.getTaskOutline().getRoot(), replacement.getTaskOutline().search(replacementTask).getParent());
		assertSame(replacement, replacementTask.getOwningProject());
		assertFalse(replacementTask.isInSubproject());
		assertSame(previous, reference.getSubproject());
		assertSame(previous, factory.getPortfolio().findByUniqueId(previous.getUniqueId()));
		assertSame(previous, factory.getPortfolio().getNodeModel().search(previous, Portfolio.comparator).getImpl());
		assertNull(factory.getPortfolio().getNodeModel().search(replacement, Portfolio.comparator));
		assertTrue(previous.getResourcePool().getProjects().contains(previous));
		assertFalse(replacement.getResourcePool().getProjects().contains(replacement));
		assertTrue(ResourcePoolFactory.getInstance().getResourcePools().contains(previous.getResourcePool()));
		assertFalse(ResourcePoolFactory.getInstance().getResourcePools().contains(replacement.getResourcePool()));
		assertEquals(SubProj.LoadStatus.OPEN, reference.getLoadStatus());
		assertFalse(undo.canUndo());
		assertFalse(((DataFactoryUndoController) previous.getUndoController()).canUndo());
		assertFalse(((DataFactoryUndoController) replacement.getUndoController()).canUndo());
		assertEquals(0, notifications.get());
		assertEquals(masterRevision, master.getMutationState().revision());
		assertEquals(childRevision, previous.getMutationState().revision());
		assertEquals(replacementRevision, replacement.getMutationState().revision());
	}

	private static Project newProject(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.setName(name);
		return project;
	}

	@Test
	void duplicatePortfolioNodesForOneChildDoNotLeakTheMutationCoordinator() throws Exception {
		ProjectFactory factory = ProjectFactory.getInstance();
		Portfolio originalPortfolio = factory.portfolio;
		ResourcePoolFactory poolFactory = ResourcePoolFactory.getInstance();
		List<ResourcePool> originalPools = new ArrayList<>(poolFactory.getResourcePools());
		factory.portfolio = new Portfolio(factory);
		try {
			Project master = newProject("master");
			Project previous = newProject("previous");
			previous.createScriptedTask();
			Project replacement = newProject("replacement");
			replacement.createScriptedTask();
			DefaultSubProj reference = new DefaultSubProj(master, previous.getUniqueId());
			Node referenceNode = NodeFactory.getInstance().createNode(reference);
			master.addToDefaultOutline(null, referenceNode);
			factory.getPortfolio().addProject(master, false, false);
			// Unverified adds do not de-duplicate; the same child is reachable twice.
			factory.getPortfolio().addProject(previous, false, false);
			factory.getPortfolio().addProject(previous, false, false);
			master.getSubprojectHandler().addSubproject(previous, referenceNode, true, false);

			assertTrue(factory.replaceOpenSubproject(master, reference, replacement));

			assertSame(replacement, reference.getSubproject());
			assertFalse(master.getMutationState().isHeldByCurrentThread(),
				"a duplicate portfolio node must not leave a mutation scope open");
			java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
			try {
				assertTrue(worker.submit(() -> com.microproject.transaction.ProjectMutationState.coordinated(() -> Boolean.TRUE))
					.get(2, java.util.concurrent.TimeUnit.SECONDS), "another thread must acquire the coordinator after replacement");
			} finally {
				worker.shutdownNow();
			}
		} finally {
			factory.portfolio = originalPortfolio;
			poolFactory.getResourcePools().removeIf(pool -> !originalPools.contains(pool));
		}
	}
}
