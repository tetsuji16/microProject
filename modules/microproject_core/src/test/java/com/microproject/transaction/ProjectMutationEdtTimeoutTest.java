/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.transaction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.microproject.util.UiDispatch;
import com.microproject.util.UiDispatcher;

class ProjectMutationEdtTimeoutTest {
	@Test
	void uiTimeoutLeavesCoordinatorUsableAndDoesNotRunOperation() throws Exception {
		Thread uiThread = Thread.currentThread();
		UiDispatch.setDispatcher(new UiDispatcher() {
			public void invokeLater(Runnable task) { task.run(); }
			public boolean isDispatchThread() { return Thread.currentThread() == uiThread; }
		});
		var executor = Executors.newSingleThreadExecutor();
		CountDownLatch acquired = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicReference<Boolean> invoked = new AtomicReference<>(false);
		try {
			var holder = executor.submit(() -> ProjectMutationState.coordinated(() -> {
				acquired.countDown();
				try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			}));
			assertTrue(acquired.await(3, TimeUnit.SECONDS));
			assertThrows(ProjectMutationBusyException.class, () ->
				ProjectMutationState.coordinated(() -> invoked.set(true)));
			assertFalse(invoked.get());
			assertFalse(ProjectMutationState.isCoordinatorHeldByCurrentThread());
			release.countDown();
			holder.get(3, TimeUnit.SECONDS);
			assertTrue(ProjectMutationState.isCoordinatorAvailable(1000));
		} finally {
			release.countDown();
			executor.shutdownNow();
			UiDispatch.setDispatcher(null);
		}
	}
}
