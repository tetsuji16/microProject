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
 * One parameter of a configured transform, for example the date range of {@code Filter.DateRange}.
 *
 * <p>The view configuration digester creates the bean from a {@code <param id="..."/>} element and
 * binds only the {@code id} attribute, so the value stays {@code null} until a dialog or caller sets
 * it. {@link #getValue()} is a heterogeneous {@link Object}: filters compare it against task dates
 * (a {@link java.util.Date}) while other transforms store strings or numbers, and
 * {@link CommonTransform#addParameter(TransformParameter)} keeps the value only when it is not
 * {@code null}.
 */
public class TransformParameter {
	private String id;
	private Object value;

	/** The configured parameter ID, for example {@code TransformParameter.StartDate}. */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/** The parameter value as supplied by the caller; the type depends on the transform. */
	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
}
