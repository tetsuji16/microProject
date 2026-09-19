/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class DocumentGenerationTest {
	@Test
	void oldCompletionTokenIsRejectedAfterDocumentSwitch() {
		var generation = new DocumentGeneration();
		long oldToken = generation.current();
		assertTrue(generation.isCurrent(oldToken));
		generation.advance();
		assertFalse(generation.isCurrent(oldToken));
	}

	@Test
	void staleCompletionIsNotMutatedOnEdt() throws Exception {
		var generation = new DocumentGeneration();
		long token = generation.current();
		generation.advance();
		var invoked = new AtomicBoolean();
		GraphicManager.dispatchDocumentCompletion(generation, token, () -> invoked.set(true));
		SwingUtilities.invokeAndWait(() -> { });
		assertFalse(invoked.get());
	}

	@Test
	void realFileSaveAndLoadCompletionsDiscardAnOlderDocumentGeneration() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project source = Project.createProject(ResourcePool.createRourcePool("generation-file", undo), undo);
		source.initialize(false, false);
		NormalTask task = source.createScriptedTask();
		task.setName("persisted generation fixture");
		source.recalculate();

		Path file = Files.createTempFile("microproject-generation-", ".mpo");
		try {
			ByteArrayOutputStream encoded = new ByteArrayOutputStream();
			assertTrue(new MpoFileImporter().saveProject(source, encoded), "real MPO save must succeed");
			Files.write(file, encoded.toByteArray());
			Project loaded;
			try (var input = Files.newInputStream(file)) {
				loaded = new MpoFileImporter().loadProject(input);
			}
			assertTrue(loaded.getTaskList().stream().anyMatch(candidate ->
				"persisted generation fixture".equals(candidate.getName())),
				"real MPO load must produce the persisted document");

			DocumentGeneration generations = new DocumentGeneration();
			AtomicReference<String> visibleDocument = new AtomicReference<>("current-document");
			long oldGeneration = generations.current();
			GraphicManager.dispatchDocumentCompletion(generations, oldGeneration,
				() -> visibleDocument.set("stale-save-completion"));
			generations.advance();
			long currentGeneration = generations.current();
			GraphicManager.dispatchDocumentCompletion(generations, currentGeneration,
				() -> visibleDocument.set(loaded.getName()));
			SwingUtilities.invokeAndWait(() -> { });

			assertTrue(visibleDocument.get().equals(loaded.getName()),
				"current real-file load completion must update the visible document");
			assertFalse("stale-save-completion".equals(visibleDocument.get()),
				"an older save completion must not overwrite the newer loaded document");
		} finally {
			Files.deleteIfExists(file);
		}
	}
}
