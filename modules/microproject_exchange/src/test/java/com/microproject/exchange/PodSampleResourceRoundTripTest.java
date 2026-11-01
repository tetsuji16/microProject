/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microproject.association.Association;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.configuration.Configuration;
import com.microproject.datatype.Duration;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.task.Task;
import com.microproject.temporary.TemporaryRoot;
import com.microproject.pm.task.NormalTask;
import com.microproject.server.data.MPXConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Project;

class PodSampleResourceRoundTripTest {
	private static final Path SAMPLE = Path.of("../../samples/Commercial construction project plan2.pod").toAbsolutePath().normalize();
	private static final String BLANK = "<blank>";

	@Test
	void commercialConstructionResourcesSurviveMpoRoundTrip() throws Exception {
		LocalFileImporter pod = new LocalFileImporter();
		pod.setFileName(Path.of("../../samples/Commercial construction project plan2.pod").toAbsolutePath().normalize().toString());
		pod.importFile();
		Project original = pod.getProject();
		assertNotNull(original);
		List<String> names = original.getResourcePool().getResourceList().stream().map(Resource::getName).sorted().toList();
		assertTrue(names.contains("G.C. General Management"));
		assertTrue(original.getResourcePool().getResourceList().stream().mapToInt(r -> r.getAssignments().size()).sum() > 0);
		Map<Long, Long> resourceIds = MPXConverter.exportedResourceIdentities(original);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, bytes));
		JsonNode taskIds = null;
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if (entry.getName().equals("changes/task-identities.json")) taskIds = new ObjectMapper().readTree(zip.readAllBytes());
			}
		}
		assertNotNull(taskIds);
		Project reopened = new MpoFileImporter().loadProject(new ByteArrayInputStream(bytes.toByteArray()));
		assertEquals(names, reopened.getResourcePool().getResourceList().stream().map(Resource::getName).sorted().toList());
		for (Resource resource : reopened.getResourcePool().getResourceList()) {
			Resource source = original.getResourcePool().getResourceList().stream()
				.filter(r -> r.getName().equals(resource.getName())).findFirst().orElseThrow();
			assertEquals(resourceIds.get(source.getUniqueId()).longValue(), resource.getUniqueId());
			assertEquals(source.getAssignments().size(), resource.getAssignments().size(), resource.getName());
			for (Object value : source.getAssignments()) {
				Assignment before = (Assignment) value;
				long taskId = taskIds.path(Long.toString(before.getTask().getUniqueId())).asLong(-1);
				assertTrue(resource.getAssignments().stream().map(Assignment.class::cast).anyMatch(after ->
					after.getTask().getUniqueId() == taskId && after.getResource() == resource
					&& ((NormalTask) after.getTask()).getAssignments().contains(after)), resource.getName());
			}
			assertNotNull(reopened.getResourcePool().getResourceOutline().search(resource), resource.getName());
		}
	}

	/**
	 * Issue #967: the reporter's whole route, compared as one document.  Each of
	 * the three reports on this route lost something a narrower test did not
	 * look at, so a further report extends this comparison instead of adding one.
	 */
	@Test
	void reporterRouteFromPodToReopenedMpoKeepsTheDocumentAndLeavesOnlyProjectFiles(@TempDir Path directory) throws Exception {
		String previousRoot = System.getProperty(TemporaryRoot.ROOT_PROPERTY);
		System.setProperty(TemporaryRoot.ROOT_PROPERTY, directory.resolve("application-data/temp").toString());
		try {
			Path folder = Files.createDirectory(directory.resolve("project"));
			Path pod = Files.copy(SAMPLE, folder.resolve(SAMPLE.getFileName()));
			Path target = folder.resolve("Commercial construction project plan2.mpo");
			Project original = loadPod(pod);
			CollaborationSession podSession = startedSession(original, pod);
			original.setGroupDirty(false); // the state ProjectFactory leaves after opening a file

			// Saved as opened, before any edit recalculates the schedule.
			Path untouched = folder.resolve("Saved without edits.mpo");
			export(original, untouched);
			assertEquals(describe(original), describe(open(untouched)));
			Files.delete(untouched);

			long blankRowsBefore = blankRows(describe(original));
			NodeModel model = original.getTaskModel();
			Node summary = firstChild(model, (Node) model.getHierarchy().getRoot());
			Node nested = firstChild(model, summary);
			model.newNode(nested, 1, NodeModel.NORMAL);
			assertTrue(original.needsSaving(), "an inserted blank row must ask to be saved");
			model.newNode(summary, 1, NodeModel.NORMAL);
			addNamedRow(model, nested, "Row added after POD open");
			List<String> document = describe(original);
			assertEquals(blankRowsBefore + 2, blankRows(document));
			assertTrue(document.stream().anyMatch(row -> row.contains("Row added after POD open")));

			export(original, target);
			podSession.stop();
			closeAfterOpening(target);
			assertEquals(document, describe(open(target)));
			assertEquals(List.of(target.getFileName().toString(), pod.getFileName().toString()), filesIn(folder));

			// The second save merges with the archive written by the first one.
			addNamedRow(model, summary, "Row added before the second save");
			document = describe(original);
			resave(original, target);
			assertEquals(document, describe(open(target)));

			// A reopened file saved again must not shift, drop, or multiply anything.
			Project reopened = open(target);
			CollaborationSession mpoSession = startedSession(reopened, target);
			resave(reopened, target);
			mpoSession.stop();
			assertEquals(document, describe(open(target)));
			assertEquals(List.of(target.getFileName().toString(), pod.getFileName().toString()), filesIn(folder));
		} finally {
			if (previousRoot == null) System.clearProperty(TemporaryRoot.ROOT_PROPERTY);
			else System.setProperty(TemporaryRoot.ROOT_PROPERTY, previousRoot);
		}
	}

	/** Issue #967: a remnant MPO from an earlier attempt must not block Save As. */
	@Test
	void savingPodOverRemnantMpoFromEarlierAttemptKeepsResources(@TempDir Path directory) throws Exception {
		Path target = directory.resolve("Commercial construction project plan2.mpo");
		export(loadSample(), target);
		Project original = loadSample();
		List<String> names = resourceNames(original);
		int assignments = assignmentCount(original);
		NodeModel model = original.getTaskModel();
		addNamedRow(model, (Node) model.getHierarchy().getRoot(), "Row added after POD open");
		// A plain save must not replace a file that holds another document.
		assertThrows(java.io.IOException.class, () -> export(original, target));
		MpoFileImporter saveAs = new MpoFileImporter();
		saveAs.setFileName(target.toString());
		saveAs.setProject(original);
		saveAs.setReplaceDifferentDocument(true);
		saveAs.exportFile();
		assertReopenedResources(target, names, assignments);
	}

	private static Project loadSample() throws Exception {
		return loadPod(SAMPLE);
	}

	private static Project loadPod(Path pod) throws Exception {
		LocalFileImporter importer = new LocalFileImporter();
		importer.setFileName(pod.toString());
		importer.importFile();
		return importer.getProject();
	}

	private static long blankRows(List<String> document) {
		return document.stream().filter(row -> row.endsWith(BLANK)).count();
	}

	private static Node firstChild(NodeModel model, Node parent) {
		return (Node) model.getChildren(parent).iterator().next();
	}

	/** The spreadsheet route: a blank row at the position, converted by entering its name. */
	private void addNamedRow(NodeModel model, Node parent, String name) throws Exception {
		Node row = model.newNode(parent, 1, NodeModel.NORMAL);
		model.replaceImplAndSetFieldValue(row, null, Configuration.getFieldFromId("Field.name"), this, name, null, NodeModel.NORMAL);
	}

	private static CollaborationSession startedSession(Project project, Path file) {
		CollaborationSession session = new CollaborationSession(project, file.toString(), "reporter");
		session.start();
		return session;
	}

	private static void closeAfterOpening(Path file) throws Exception {
		startedSession(open(file), file).stop();
	}

	private static List<String> filesIn(Path folder) throws Exception {
		try (var files = Files.list(folder)) {
			return files.map(file -> file.getFileName().toString()).sorted().toList();
		}
	}

	private static void resave(Project project, Path target) throws Exception {
		MpoFileImporter resave = new MpoFileImporter();
		resave.setFileName(target.toString());
		resave.setProject(project);
		resave.exportFile();
		assertTrue(resave.confirmSavedState(), "a re-save must not leave the project looking edited");
	}

	/** Everything a reader of the plan can see: row order and level, schedule, links, and assignments. */
	private static List<String> describe(Project project) {
		List<String> document = new ArrayList<>();
		Object root = project.getTaskOutline().getHierarchy().getRoot();
		for (Iterator<?> rows = project.getTaskOutline().iterator(); rows.hasNext();) {
			Node node = (Node) rows.next();
			if (node == root) continue;
			int level = project.getTaskOutline().getHierarchy().getLevel(node);
			if (node.isVoid()) {
				document.add(level + ":" + BLANK);
			} else if (node.getImpl() instanceof NormalTask task) {
				List<String> predecessors = new ArrayList<>();
				for (Association association : task.getPredecessorList()) {
					Dependency link = (Dependency) association;
					predecessors.add(((Task) link.getPredecessor()).getName() + "/" + link.getDependencyKind() + "/" + Duration.millis(link.getLag()));
				}
				predecessors.sort(null);
				document.add(level + ":" + task.getName() + " " + task.getStart() + ".." + task.getEnd()
					+ " duration=" + Duration.millis(task.getDuration()) + " work=" + task.getWork(null)
					+ " percent=" + Math.round(task.getPercentComplete() * 100D) + " " + predecessors);
			}
		}
		// Blank rows after the last task are the view's filler, which is not saved.
		while (!document.isEmpty() && document.getLast().endsWith(BLANK)) document.removeLast();
		for (Resource resource : project.getResourcePool().getResourceList()) {
			document.add(resource.getName() + " " + resource.getAssignments().stream()
				.map(assignment -> ((Assignment) assignment).getTask().getName()).sorted().toList());
		}
		document.subList(document.size() - project.getResourcePool().getResourceList().size(), document.size()).sort(null);
		return document;
	}

	private static void export(Project project, Path target) throws Exception {
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(target.toString());
		writer.setProject(project);
		writer.exportFile();
	}

	private static Project open(Path target) throws Exception {
		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(target.toString());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();
		return reader.getProject();
	}

	private static void assertReopenedResources(Path target, List<String> names, int assignments) throws Exception {
		Project reopened = open(target);
		assertEquals(names, resourceNames(reopened));
		assertEquals(assignments, assignmentCount(reopened));
		for (Resource resource : reopened.getResourcePool().getResourceList())
			assertNotNull(reopened.getResourcePool().getResourceOutline().search(resource), resource.getName());
	}

	private static List<String> resourceNames(Project project) {
		return project.getResourcePool().getResourceList().stream().map(Resource::getName).sorted().toList();
	}

	private static int assignmentCount(Project project) {
		return project.getResourcePool().getResourceList().stream().mapToInt(r -> r.getAssignments().size()).sum();
	}
}
