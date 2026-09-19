/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.event;

import java.util.EventObject;

/** Common base for graph, cache, and selection notifications. */
public abstract class GraphicEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	protected GraphicEvent(Object source) { super(source); }
}
