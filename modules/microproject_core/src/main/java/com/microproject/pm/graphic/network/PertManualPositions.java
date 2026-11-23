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

import java.util.HashMap;
import java.util.Map;

/**
 * Transient document state holding Network Diagram (PERT) manual box positions.
 * This is stored in the Project's transient document state and serialized via MPO sidecar.
 * Not serialized via Java serialization (POD format) - manual positions are only persisted in MPO.
 */
public class PertManualPositions {
	/** Presentation-independent logical coordinates, measured at 100% zoom. */
	public record Position(double x, double y) { }

	private final Map<Long, Position> positions = new HashMap<>();

	public Map<Long, Position> getPositions() {
		return positions;
	}

	public void setPositions(Map<Long, Position> positions) {
		this.positions.clear();
		if (positions != null) {
			this.positions.putAll(positions);
		}
	}

	public void putPosition(long taskUniqueId, Position position) {
		if (position != null) {
			positions.put(taskUniqueId, position);
		} else {
			positions.remove(taskUniqueId);
		}
	}

	public Position getPosition(long taskUniqueId) {
		return positions.get(taskUniqueId);
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
