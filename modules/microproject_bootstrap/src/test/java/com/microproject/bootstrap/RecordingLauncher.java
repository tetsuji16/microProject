/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.bootstrap;

import org.update4j.LaunchContext;
import org.update4j.service.Launcher;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-process {@link Launcher} used by tests to confirm the launch path resolves
 * and executes without forking a real JVM (the production {@link DefaultLauncher}
 * would spawn the business application).
 */
public final class RecordingLauncher implements Launcher {

    public static final AtomicBoolean RAN = new AtomicBoolean(false);

    @Override
    public void run(LaunchContext context) {
        RAN.set(true);
    }
}
