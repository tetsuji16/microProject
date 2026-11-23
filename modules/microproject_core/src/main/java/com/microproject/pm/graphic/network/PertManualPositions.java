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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.network;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Transient document state holding Network Diagram (PERT) manual box positions.
 * This is stored in the Project's transient document state and serialized via MPO sidecar.
 * Not serialized via Java serialization (POD format) - manual positions are only persisted in MPO.
 */
public class PertManualPositions {
	private final Map<Long, Point2D> positions = new HashMap<>();

	public Map<Long, Point2D> getPositions() {
		return positions;
	}

	public void setPositions(Map<Long, Point2D> positions) {
		this.positions.clear();
		if (positions != null) {
			this.positions.putAll(positions);
		}
	}

	public void putPosition(long taskUniqueId, Point2D position) {
		if (position != null) {
			positions.put(taskUniqueId, new Point2D.Double(position.getX(), position.getY()));
		} else {
			positions.remove(taskUniqueId);
		}
	}

	public Point2D getPosition(long taskUniqueId) {
		Point2D pos = positions.get(taskUniqueId);
		return pos != null ? new Point2D.Double(pos.getX(), pos.getY()) : null;
	}

	public void removePosition(long taskUniqueId) {
		positions.remove(taskUniqueId);
	}

	public void clear() {
		positions.clear();
	}

	public boolean isEmpty() {
		return positions.isEmpty();
	}
}