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
package com.projectlibre1.server.data;

import java.util.function.Predicate;

import com.projectlibre1.field.Field;

/**
 * Options controlling how a project model is serialized. Implemented as a
 * {@link Predicate} over {@link Field} so callers can filter which fields are
 * written. Uses only the standard library (no external collections dependency),
 * replacing the former Apache Commons Predicate base with the JDK Predicate API.
 */
public class SerializeOptions implements Predicate<Field> {
    private boolean publish = false;

    public SerializeOptions() {
    }

    public SerializeOptions(boolean publish) {
        this.publish = publish;
    }

    public boolean isPublish() {
        return publish;
    }

    public void setPublish(boolean publish) {
        this.publish = publish;
    }

    public Predicate<Field> getFieldFilter() {
        return this;
    }

    @Override
    public boolean test(Field field) {
        return field != null;
    }
}
