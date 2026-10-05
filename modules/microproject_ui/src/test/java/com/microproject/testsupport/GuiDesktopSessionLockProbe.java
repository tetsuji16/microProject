/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.nio.file.Path;
import java.time.Duration;

/** Child JVM entry point used to prove that the desktop lock is process-wide. */
public final class GuiDesktopSessionLockProbe {
	private GuiDesktopSessionLockProbe() {
	}

	public static void main(String[] arguments) throws Exception {
		Path lockFile = Path.of(arguments[0]);
		long waitMillis = Long.parseLong(arguments[1]);
		try (GuiDesktopSessionCoordinator.Lease lease = GuiDesktopSessionCoordinator.acquire(lockFile,
				Duration.ofMillis(waitMillis))) {
			System.out.println("ACQUIRED " + lease.lockFile());
		} catch (GuiDesktopSessionCoordinator.DesktopContendedException contended) {
			System.out.println("CONTENDED " + contended.getMessage());
		}
	}
}
