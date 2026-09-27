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
package com.microproject.grouping.core.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TransformListTest {
	@Test
	void filtersFactoriesByAuthorizedIdsAndKeepsFactoryOrder() {
		TransformList transforms = new TransformList();
		CommonTransformFactory other = factory("custom");
		CommonTransformFactory noFilter = factory(ViewTransformer.FILTER_NONE_ID);
		transforms.addFactory(other);
		transforms.addFactory(noFilter);

		assertEquals(List.of(noFilter), transforms.getFactories(null, "user_filters"));
	}

	@Test
	void transformerTokenListsPreserveOrderAndDelimiters() {
		ViewTransformer transformer = new ViewTransformer();
		transformer.setFilters("first; second,third\tfourth");
		transformer.setSorters("sortA sortB");
		transformer.setGroupers("groupA;groupB");

		assertEquals(List.of("first", "second", "third", "fourth"), transformer.getFilterList());
		assertEquals(List.of("sortA", "sortB"), transformer.getSorterList());
		assertEquals(List.of("groupA", "groupB"), transformer.getGrouperList());

		transformer.setFilters("");
		assertEquals(List.of(), transformer.getFilterList());
	}

	private static CommonTransformFactory factory(String id) {
		CommonTransformFactory factory = new CommonTransformFactory() {
			@Override
			public CommonTransform getTransform() {
				return null;
			}
		};
		factory.setId(id);
		return factory;
	}
}
