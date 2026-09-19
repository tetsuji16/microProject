/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.link_routing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.GeneralPath;

import org.junit.jupiter.api.Test;

class DefaultNetworkLinkRoutingTest {
    @Test
    void verticalRouteUsesSharedOrthogonalGeometry() {
        DefaultNetworkLinkRouting routing = new DefaultNetworkLinkRouting();
        routing.setVertical(true);
        routing.routePath(new GeneralPath(), 10, 20, 80, 100, 60, 0);

        assertEquals(10f, routing.getFirstX());
        assertEquals(20f, routing.getFirstY());
        assertEquals(80f, routing.getLastX());
        assertEquals(100f, routing.getLastY());
    }

    @Test
    void horizontalRouteUsesSharedOrthogonalGeometry() {
        DefaultNetworkLinkRouting routing = new DefaultNetworkLinkRouting();
        routing.setVertical(false);
        routing.routePath(new GeneralPath(), 10, 20, 80, 100, 45, 0);

        assertEquals(10f, routing.getFirstX());
        assertEquals(20f, routing.getFirstY());
        assertEquals(80f, routing.getLastX());
        assertEquals(100f, routing.getLastY());
    }
}
