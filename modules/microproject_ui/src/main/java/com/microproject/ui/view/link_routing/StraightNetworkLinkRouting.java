/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.view.link_routing;

import java.awt.geom.GeneralPath;

import com.microproject.pm.dependency.DependencyType;

/** Network link routing that joins the two boxes with one straight line (Straight Link Styles). */
public class StraightNetworkLinkRouting extends NetworkLinkRouting {
	public void routePath(GeneralPath path, double x0, double y0, double x1, double y1, double xy2, DependencyType.Kind type) {
		routeStraight(path, x0, y0, x1, y1);
	}
}
