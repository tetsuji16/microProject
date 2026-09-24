/*
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
 */
package com.microproject.bootstrap;

import javax.swing.JPanel;

import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.workspace.FrameHolder;
import com.microproject.pm.graphic.frames.workspace.FrameManager;

public final class BootstrapApplet extends JPanel {
	private static GraphicManager graphicManager;

	public static void setGraphicManager(GraphicManager manager) {
		graphicManager = manager;
	}

	public static FrameHolder getObject() {
		return new FrameHolder() {
			@Override
			public FrameManager getFrameManager() {
				return null;
			}

			@Override
			public GraphicManager getGraphicManager() {
				return graphicManager;
			}

			@Override
			public void setGraphicManager(GraphicManager manager) {
				graphicManager = manager;
			}
		};
	}
}
