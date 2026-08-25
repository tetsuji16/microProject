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
package com.microproject.pm.graphic.model.cache;

import java.util.Optional;

import com.microproject.pm.task.ProjectTaskKey;

/** Primitive Workspace representation for durable projection identities. */
public final class ProjectionRowKeyCodec {
	private static final String VERSION = "v1";

	private ProjectionRowKeyCodec() {
	}

	public static Optional<String> encodeDurable(ProjectionRowKey key) {
		if (key == null || !key.isDurable())
			return Optional.empty();
		return Optional.of(String.join(":", VERSION, key.kind().name(),
				Long.toString(key.taskKey().owningProjectId()), Long.toString(key.taskKey().taskUniqueId()),
				Long.toString(key.entityId())));
	}

	public static Optional<ProjectionRowKey> decodeDurable(String encoded) {
		if (encoded == null || encoded.isBlank())
			return Optional.empty();
		String[] parts = encoded.split(":", -1);
		if (parts.length != 5 || !VERSION.equals(parts[0]))
			return Optional.empty();
		try {
			ProjectionRowKey.Kind kind = ProjectionRowKey.Kind.valueOf(parts[1]);
			long projectId = Long.parseLong(parts[2]);
			long taskId = Long.parseLong(parts[3]);
			long entityId = Long.parseLong(parts[4]);
			return Optional.of(new ProjectionRowKey(kind, new ProjectTaskKey(projectId, taskId), entityId, 0L));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}
}
