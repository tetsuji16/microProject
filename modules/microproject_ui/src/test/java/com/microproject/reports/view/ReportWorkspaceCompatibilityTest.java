/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.reports.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportWorkspaceCompatibilityTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void savedReportSelectionAndZoomRetainTheExistingSerializedWorkspaceContract() throws Exception {
		ReportView.Workspace saved = new ReportView.Workspace();
		saved.setReportName("Who Does What");
		saved.setFieldArrayName("Spreadsheet.ResourceAssignment.reportAssignmentTask");
		saved.setZoomRatio(1.25f);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(saved); }
		ReportView.Workspace restored;
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			restored = (ReportView.Workspace)input.readObject();
		}
		assertEquals("Who Does What", restored.getReportName());
		assertEquals("Spreadsheet.ResourceAssignment.reportAssignmentTask", restored.getFieldArrayName());
		assertEquals(1.25f, restored.getZoomRatio());
		assertEquals(-7768176701769503845L, ObjectStreamClass.lookup(ReportView.Workspace.class).getSerialVersionUID());
	}

	@Test
	void mpoReopenReattachesThePerUserReportWorkspaceFromItsCollaborationSidecar() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project original = Project.createProject(ResourcePool.createRourcePool("report-workspace", undo), undo);
		original.initialize(false, false);
		Path mpo = temporaryDirectory.resolve("report-workspace.mpo");
		MpoFileImporter importer = new MpoFileImporter();
		try (var output = Files.newOutputStream(mpo)) {
			assertTrue(importer.saveProject(original, output));
		}

		ReportView.Workspace saved = new ReportView.Workspace();
		saved.setReportName("Who Does What");
		saved.setFieldArrayName("Spreadsheet.ResourceAssignment.reportAssignmentTask");
		saved.setZoomRatio(1.25f);
		new CollaborationSession(original, mpo.toString(), "report-user").saveWorkspace(saved);

		Project reopened;
		try (var input = Files.newInputStream(mpo)) {
			reopened = importer.loadProject(input);
		}
		var restored = (ReportView.Workspace)
			new CollaborationSession(reopened, mpo.toString(), "report-user").loadWorkspace();
		assertEquals("Who Does What", restored.getReportName());
		assertEquals("Spreadsheet.ResourceAssignment.reportAssignmentTask", restored.getFieldArrayName());
		assertEquals(1.25f, restored.getZoomRatio());
	}
}
