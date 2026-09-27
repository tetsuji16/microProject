/*******************************************************************************
 * MIT License
 *
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Owns the desktop-wide set of window coordinators and the active fallback. */
final class GraphicManagerRegistry {
	private static final GraphicManagerRegistry INSTANCE = new GraphicManagerRegistry();

	private final List<GraphicManager> managers = new ArrayList<>();
	private GraphicManager activeManager;

	static GraphicManagerRegistry getInstance() {
		return INSTANCE;
	}

	synchronized void register(GraphicManager manager) {
		GraphicManager value = Objects.requireNonNull(manager);
		if (!managers.contains(value))
			managers.add(value);
		activate(value);
	}

	synchronized void activate(GraphicManager manager) {
		if (managers.remove(manager)) {
			managers.add(manager);
			activeManager = manager;
		}
	}

	synchronized void remove(GraphicManager manager) {
		managers.remove(manager);
		if (activeManager == manager)
			activeManager = managers.isEmpty() ? null : managers.getLast();
	}

	synchronized GraphicManager getActiveManager() {
		return activeManager;
	}

	synchronized List<GraphicManager> snapshot() {
		return List.copyOf(managers);
	}

	synchronized boolean isEmpty() {
		return managers.isEmpty();
	}
}
