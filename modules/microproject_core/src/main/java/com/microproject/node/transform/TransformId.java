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
package com.microproject.node.transform;

/**
 * Identifier of a configured view transform, read from a stored view configuration.
 *
 * <p>{@code ViewConfiguration} creates this bean for every {@code transform/filter},
 * {@code transform/sorter}, {@code transform/grouper} and {@code transform/transformer} element and
 * binds the {@code id} and {@code hidden} attributes to it by property name. The property names are
 * therefore configuration contract: renaming {@link #setId(String)} or {@link #setHidden(boolean)}
 * silently drops the stored view settings instead of failing a test.
 */
public class TransformId {
	private String id;
	private boolean hidden = false;

	/** Whether the referenced transform is hidden from the transform menus. */
	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	/** The configured transform ID, for example {@code Filter.DateRange}. */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
