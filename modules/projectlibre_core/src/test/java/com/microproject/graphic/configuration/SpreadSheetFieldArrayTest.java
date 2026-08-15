package com.microproject.graphic.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
