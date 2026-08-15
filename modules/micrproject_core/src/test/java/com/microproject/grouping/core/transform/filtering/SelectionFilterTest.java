package com.microproject.grouping.core.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.task.NormalTask;

class SelectionFilterTest {
    @Test
    void emptySelectionIsAStableFilterState() {
        SelectionFilter filter = new SelectionFilter("true");
        Node selected = NodeFactory.getInstance().createNode(new NormalTask());

        assertDoesNotThrow(() -> filter.setSelectedNodesImpl(null, true));
        assertFalse(filter.evaluate(selected));

        filter.setSelectedNodesImpl(List.of(selected.getImpl()), true);
        assertTrue(filter.evaluate(selected));
    }

    @Test
    void nodeWithoutImplementationDoesNotLeakTransientSelectionState() {
        SelectionFilter filter = new SelectionFilter("true");
        Node empty = NodeFactory.getInstance().createNode((Object) null);

        assertFalse(filter.evaluate(empty));
    }
}
