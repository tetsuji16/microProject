package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldFormulaTest {
    @Test
    void scriptedFormulaCanBeSetEvaluatedAndCleared() {
        Field field = new Field();

        field.setFormula("tripleFieldFormula", "value", "value * 3");

        assertTrue(field.isFormula());
        assertEquals(21, field.evaluateFormula(7));

        field.clearFormula();
        assertFalse(field.isFormula());
    }

    @Test
    void scriptedFormulaCanBeReplacedAfterItWasEvaluated() {
        Field field = new Field();
        field.setFormula("replaceableFieldFormula", "value", "value * 2");
        assertEquals(10, field.evaluateFormula(5));

        field.setFormula("replaceableFieldFormula", "value", "value * 4");

        assertEquals(20, field.evaluateFormula(5));
    }
}
