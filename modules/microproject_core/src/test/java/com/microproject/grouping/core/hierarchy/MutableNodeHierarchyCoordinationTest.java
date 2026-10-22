/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.transaction.ProjectMutationState;

class MutableNodeHierarchyCoordinationTest {
	@Test
	void silentStructuralWriterCannotInterleaveWithCacheStyleRead() throws Exception {
		MutableNodeHierarchy hierarchy = new MutableNodeHierarchy();
		Node root = (Node) hierarchy.getRoot();
		hierarchy.setNbEndVoidNodes(0);
		Node existing = NodeFactory.getInstance().createNode("existing");
		hierarchy.add(root, List.of(existing), -1, NodeModel.SILENT);
		Node inserted = NodeFactory.getInstance().createNode("inserted");
		CountDownLatch readerLocked = new CountDownLatch(1);
		CountDownLatch startWriter = new CountDownLatch(1);
		CountDownLatch writerStarted = new CountDownLatch(1);
		CountDownLatch releaseReader = new CountDownLatch(1);
		CountDownLatch writerFinished = new CountDownLatch(1);
		ExecutorService workers = Executors.newFixedThreadPool(2);
		try {
			var read = workers.submit(() -> ProjectMutationState.coordinated(() -> {
				readerLocked.countDown();
				await(releaseReader);
				// Mirrors a cache refresh enumerating the live child vector under the barrier.
				return new ArrayList<>(hierarchy.getChildren(root));
			}));
			var write = workers.submit(() -> {
				await(startWriter);
				writerStarted.countDown();
				hierarchy.add(root, List.of(inserted), -1, NodeModel.SILENT);
				writerFinished.countDown();
			});
			assertTrue(readerLocked.await(2, TimeUnit.SECONDS));
			startWriter.countDown();
			assertTrue(writerStarted.await(2, TimeUnit.SECONDS));
			assertFalse(writerFinished.await(100, TimeUnit.MILLISECONDS), "writer must wait for the cache read barrier");
			releaseReader.countDown();
			assertEquals(List.of(existing), read.get(2, TimeUnit.SECONDS));
			write.get(2, TimeUnit.SECONDS);
			assertEquals(List.of(existing, inserted), hierarchy.getChildren(root));
		} finally {
			releaseReader.countDown();
			workers.shutdownNow();
			assertTrue(workers.awaitTermination(2, TimeUnit.SECONDS));
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(2, TimeUnit.SECONDS)) throw new AssertionError("coordination latch timed out");
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new AssertionError("coordination test interrupted", interrupted);
		}
	}
}
