/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.offline_graphics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class NetworkSVGRendererLifecycleTest {
	@Test
	void cleanupRemovesRendererFromRegisteredCacheAndIsIdempotent() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("network-renderer-lifecycle", undo), undo);
		project.initialize(false, false);
		project.createScriptedTask();
		project.recalculate();
		ReferenceNodeModelCache referenceCache = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(referenceCache, "Network", null);
		NetworkSVGRenderer renderer = new NetworkSVGRenderer();

		renderer.init(project, cache, NetworkSVGRenderer.PERT, -1);
		assertTrue(java.util.Arrays.asList(cache.getNodeModelListeners()).contains(renderer));
		renderer.init(project, cache, NetworkSVGRenderer.PERT, -1);
		assertTrue(cache.getSize() > 0, "reinitialization with the same cache must keep it open");
		assertTrue(java.util.Arrays.stream(cache.getNodeModelListeners()).filter(renderer::equals).count() == 1,
				"reinitialization must not duplicate the listener");
		NetworkSVGRenderer printCopy = (NetworkSVGRenderer) renderer.createSafePrintCopy();
		BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			assertDoesNotThrow(() -> printCopy.paint(graphics));
		} finally {
			graphics.dispose();
		}

		renderer.cleanUp();
		renderer.cleanUp();

		assertFalse(java.util.Arrays.asList(cache.getNodeModelListeners()).contains(renderer));
		assertTrue(cache.getSize() == 0, "cleanup must close and clear the filtered cache");
		NodeModelCache freshView = NodeModelCacheFactory.getInstance().createFilteredCache(referenceCache, "Network", null);
		freshView.update();
		assertTrue(freshView.getSize() > 0, "cleanup must leave the shared reference cache available");
		freshView.close();
		referenceCache.close();
	}
}
