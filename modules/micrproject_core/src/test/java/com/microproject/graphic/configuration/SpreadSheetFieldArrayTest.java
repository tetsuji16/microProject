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
package com.microproject.graphic.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import com.microproject.field.Field;

class SpreadSheetFieldArrayTest {
    @Test
    void cloneOwnsIndependentMutableConfiguration() {
        SpreadSheetFieldArray original = new SpreadSheetFieldArray();
        original.setName(new String("Entry"));
        original.setMapFieldTo("Field.target");
        original.addField("missing-field");
        original.setWidths(new ArrayList<>(java.util.List.of(120)));

        SpreadSheetFieldArray copy = original.clone();
        copy.getWidths().set(0, 240);
        copy.setName("Other");

        assertNotSame(original.getWidths(), copy.getWidths());
        assertEquals(120, original.getWidth(0));
        assertNotEquals(original, copy);
    }

    @Test
    void equalityUsesNameValueAndHasMatchingHashCode() {
        SpreadSheetFieldArray first = new SpreadSheetFieldArray();
        SpreadSheetFieldArray second = new SpreadSheetFieldArray();
        first.setName(new String("Shared"));
        second.setName(new String("Shared"));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void typedListAcceptsFields() {
        SpreadSheetFieldArray fields = new SpreadSheetFieldArray();
        Field field = new Field();
        fields.add(field);
        assertEquals(field, fields.getFirst());
    }

    @Test
    void columnLayoutChangesKeepSavedWidthsAlignedWithFields() {
        SpreadSheetFieldArray fields = new SpreadSheetFieldArray();
        Field id = new Field();
        Field name = new Field();
        Field duration = new Field();
        Field start = new Field();
        fields.add(id);
        fields.add(name);
        fields.add(duration);
        fields.setWidths(new ArrayList<>(java.util.List.of(-1, 160, 80)));
        fields.setManualWidths(new ArrayList<>(java.util.List.of(false, true, false)));

        fields.insertField(2, start);
        assertIterableEquals(java.util.List.of(-1, 160, -1, 80), fields.getWidths());
        assertIterableEquals(java.util.List.of(false, true, false, false), fields.getManualWidths());

        fields.removeField(1);
        assertIterableEquals(java.util.List.of(-1, -1, 80), fields.getWidths());
        assertIterableEquals(java.util.List.of(false, false, false), fields.getManualWidths());

        fields.move(2, 1);
        assertIterableEquals(java.util.List.of(-1, 80, -1), fields.getWidths());
        assertIterableEquals(java.util.List.of(false, false, false), fields.getManualWidths());
    }
}
