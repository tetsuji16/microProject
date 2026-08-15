package com.microproject.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

public class MsProjectTaskMoveCompatibilityTest {
	@Test
	public void xmlExportUsesNewIdsButPreservesUniqueIdsAndLinksAfterMove() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("ms-project-move", undo), undo);
		project.initialize(false, false);
		Node firstNode = project.createLocalTaskNode(null);
		Node secondNode = project.createLocalTaskNode(null);
		Node thirdNode = project.createLocalTaskNode(null);
		NormalTask first = (NormalTask)firstNode.getImpl(); first.setName("First");
		NormalTask second = (NormalTask)secondNode.getImpl(); second.setName("Second");
		NormalTask third = (NormalTask)thirdNode.getImpl(); third.setName("Third");
		DependencyService.getInstance().newDependency(second, third, DependencyType.FS, 0L, this);
		long firstUid = first.getUniqueId();
		long secondUid = second.getUniqueId();
		long thirdUid = third.getUniqueId();
		assertTrue(project.getTaskModel().moveSelectedNodes(Collections.singletonList(secondNode), -1, NodeModel.NORMAL));

		MicrosoftImporter exporter = new MicrosoftImporter();
		exporter.setFileName("task-move.xml");
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertTrue(exporter.saveProject(project, output));

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		org.w3c.dom.Document xml = factory.newDocumentBuilder().parse(new ByteArrayInputStream(output.toByteArray()));
		NodeList tasks = xml.getElementsByTagName("Task");
		assertEquals(3, tasks.getLength());
		assertTask((Element)tasks.item(0), "Second", second.getId(), secondUid, "1");
		assertTask((Element)tasks.item(1), "First", first.getId(), firstUid, "2");
		assertTaskIdentity((Element)tasks.item(2), "Third", thirdUid, "3");
		Element predecessor = (Element)((Element)tasks.item(2)).getElementsByTagName("PredecessorLink").item(0);
		assertEquals(String.valueOf(secondUid), childText(predecessor, "PredecessorUID"));
	}

	private static void assertTask(Element task,String name,long id,long uid,String outlineNumber) {
		assertEquals(String.valueOf(id), childText(task, "ID"));
		assertTaskIdentity(task, name, uid, outlineNumber);
	}

	private static void assertTaskIdentity(Element task,String name,long uid,String outlineNumber) {
		assertEquals(name, childText(task, "Name"));
		assertEquals(String.valueOf(uid), childText(task, "UID"));
		assertEquals(outlineNumber, childText(task, "OutlineNumber"));
	}

	private static String childText(Element parent,String name) {
		return parent.getElementsByTagName(name).item(0).getTextContent();
	}
}
