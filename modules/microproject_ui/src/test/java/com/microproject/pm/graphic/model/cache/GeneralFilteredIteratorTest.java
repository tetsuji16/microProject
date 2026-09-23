package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;

class GeneralFilteredIteratorTest {
	@Test
	void nodeBasedIterationUnwrapsGraphicNodeToItsNode() {
		Node node = NodeFactory.getInstance().createNode(new Object());
		GraphicNode graphicNode = new GraphicNode(node, 1);
		GeneralFilteredIterator iterator = GeneralFilteredIterator.instance(List.of(graphicNode).iterator());
		iterator.setPredicate(null);
		iterator.setNodeBased(true);

		assertSame(node, iterator.next());
	}
}
