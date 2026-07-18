package com.projectlibre1.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContextStoreTest {
    @Test
    void absentContextTypeUsesEmptyResults() {
        ContextStore store = new ContextStore();
        assertTrue(store.getContexts(99, null).isEmpty());
        assertNull(store.createDefaultContext(99));
    }

    @Test
    void returnedContextsAreDefensiveClones() {
        ContextStore store = new ContextStore();
        ConverterContext context = new ConverterContext();
        context.setType(ConverterContext.ALL);
        context.setName("All tasks");
        store.addContext(context);

        ConverterContext returned = store.getContexts(ConverterContext.ALL, null).getFirst();

        assertNotSame(context, returned);
        assertEquals("All tasks", returned.getName());
    }
}
