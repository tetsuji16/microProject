/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.task;

import java.util.EventObject;

/**
 *
 */
public class ProjectEvent extends EventObject {
	public enum Kind {
		NAME_CHANGED(1),
		GROUP_DIRTY_CHANGED(2);

		private final int code;

		Kind(int code) {
			this.code = code;
		}

		public int code() {
			return code;
		}

		public static Kind fromCode(int code) {
			for (Kind kind : values()) {
				if (kind.code == code) {
					return kind;
				}
			}
			throw new IllegalArgumentException("Unknown project event kind code: " + code);
		}
	}

	@Deprecated public static final int NAME_CHANGED = Kind.NAME_CHANGED.code();
	@Deprecated public static final int GROUP_DIRTY_CHANGED = Kind.GROUP_DIRTY_CHANGED.code();

	protected int type;
	protected Project project;
	protected Object old;
	
	
	
	/**
	 * @param source
	 * @param type
	 * @param change
	 */
	public ProjectEvent(Object source, int type, Project project,Object old) {
		super(source);
		this.type = type;
		this.project = project;
		this.old=old;
	}

	public ProjectEvent(Object source, Kind kind, Project project, Object old) {
		this(source, java.util.Objects.requireNonNull(kind, "kind").code(), project, old);
	}



	public Object getOld() {
		return old;
	}



	public void setOldName(Object old) {
		this.old = old;
	}



	public Project getProject() {
		return project;
	}



	public void setProject(Project project) {
		this.project = project;
	}



	public int getType() {
		return type;
	}

	public Kind getKind() {
		return Kind.fromCode(type);
	}

	public void setKind(Kind kind) {
		this.type = java.util.Objects.requireNonNull(kind, "kind").code();
	}



	public void setType(int type) {
		this.type = type;
	}
    
    
    
}
