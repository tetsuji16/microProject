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
package com.microproject.interval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelFactory;
import com.microproject.pm.availability.AvailabilityTable;

/**
 * Issue #167: findActive() used to call valueObjects.get(-1) when the table
 * was empty or the date preceded the first entry, throwing
 * IndexOutOfBoundsException. Callers (e.g. ResourceAvailabilityFunctor)
 * already handle a null result.
 */
class ValueObjectForIntervalTableTest {

	@Test
	void findActiveReturnsNullInsteadOfThrowingForEmptyTable() {
		AvailabilityTable table = new AvailabilityTable();
		assertNull(table.findActive(System.currentTimeMillis()));
		assertNull(table.findCurrent());
	}

	/**
	 * Issue #997: the first row has no effective date and holds the base interval every later row
	 * depends on, so it must be reported as not removable while the later rows stay removable.
	 */
	@Test
	void canRemoveKeepsTheFirstIntervalRowOnly() throws Exception {
		AvailabilityTable table = new AvailabilityTable("test");
		ValueObjectForInterval first = table.findActive(ValueObjectForInterval.NA_TIME);
		ValueObjectForInterval second = table.newValueObject(ValueObjectForInterval.NA_TIME + 1_000L);

		assertTrue(first.isFirst());
		assertFalse(table.canRemove(first), "the base row cannot go");
		assertTrue(table.canRemove(second), "later rows can go");
		assertTrue(table.canRemove(null), "unrelated implementations are not this table's rows");
		assertThrows(InvalidValueObjectForIntervalException.class, () -> table.remove(first));
	}

	/**
	 * Issue #997: a refused removal must not take the row out of the model either. It used to vanish
	 * from the view while the table kept the interval, so the rendered rows and getList() diverged.
	 */
	@Test
	void modelRemovalRefusesTheFirstRowAndKeepsRowDataInStep() throws Exception {
		AvailabilityTable table = new AvailabilityTable("test");
		table.newValueObject(ValueObjectForInterval.NA_TIME + 1_000L);
		NodeModel model = NodeModelFactory.getInstance().createNodeModelFromCollection(table.getList(), table);
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		assertEquals(2, root.getChildCount());

		model.remove((Node) root.getChildAt(0), NodeModel.NORMAL);
		assertEquals(2, root.getChildCount(), "the base row must stay in the model");
		assertEquals(2, table.getList().size(), "the base row must stay in the table");

		model.remove((Node) root.getChildAt(1), NodeModel.NORMAL);
		assertEquals(1, root.getChildCount());
		assertEquals(1, table.getList().size());
	}

	@Test
	void typedIntervalTraversalPreservesBoundsAndCloneOwnership() throws Exception {
		AvailabilityTable table = new AvailabilityTable("test");
		ValueObjectForInterval first = table.findActive(ValueObjectForInterval.NA_TIME);
		ValueObjectForInterval second = table.newValueObject(ValueObjectForInterval.NA_TIME + 1_000L);
		first.setEnd(2_000L);
		second.setEnd(5_000L);

		assertEquals(ValueObjectForInterval.NA_TIME, table.getStart());
		assertEquals(5_000L, table.getEnd());

		AvailabilityTable clone = (AvailabilityTable) table.clone();
		clone.initAfterCloning();
		assertSame(table.getName(), clone.getName());
		assertNotSame(table.getList().get(0), clone.getList().get(0));
		for (ValueObjectForInterval valueObject : clone.getList())
			assertSame(clone, valueObject.getTable());
	}
}
