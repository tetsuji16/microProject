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
		assertEquals(childText((Element)tasks.item(0), "UID"), childText(predecessor, "PredecessorUID"));
	}

	private static void assertTask(Element task,String name,long id,long uid,String outlineNumber) {
		assertTrue(Integer.parseInt(childText(task, "ID")) > 0);
		assertTaskIdentity(task, name, uid, outlineNumber);
	}

	private static void assertTaskIdentity(Element task,String name,long uid,String outlineNumber) {
		assertEquals(name, childText(task, "Name"));
		long exportedUid = Long.parseLong(childText(task, "UID"));
		// MSPDI UID is a positive integer. Native POD projects may use negative
		// generated IDs, which are deterministically remapped by the exporter.
		if (uid > 0L) assertEquals(String.valueOf(uid), String.valueOf(exportedUid));
		else assertTrue(exportedUid > 0L);
		assertEquals(outlineNumber, childText(task, "OutlineNumber"));
	}

	private static String childText(Element parent,String name) {
		return parent.getElementsByTagName(name).item(0).getTextContent();
	}
}
