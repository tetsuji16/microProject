package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.datatype.Rate;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourceType;
import com.microproject.undo.DataFactoryUndoController;

/** Resource sort composed with the resource filter and grouper projection. */
class ResourceSortProjectionTest {
	@Test
	void multiKeySortOrdersFilteredRowsInsideGroupBucketsWithoutChangingModelOrder() throws Exception {
		// Document-derived projection rule (not measured MSP behavior): Project describes sorting,
		// filtering and grouping as view operations. Filtering limits visible resources, grouping keeps
		// its type buckets, and the Key1-Key3 comparator orders rows inside each bucket. Resource
		// model order never changes. Sources:
		// https://learn.microsoft.com/en-us/office/vba/api/project.application.sort
		// https://support.microsoft.com/en-us/project/filter-tasks-or-resources
		// https://support.microsoft.com/en-us/project/group-tasks-or-resources
		Dictionary.getInstance();
		ResourcePool pool = ResourcePool.createRourcePool("resource-sort-projection", new DataFactoryUndoController());
		ResourceImpl zulu = resource(pool, "Zulu", ResourceType.WORK, 10);
		ResourceImpl alpha = resource(pool, "Alpha", ResourceType.WORK, 10);
		ResourceImpl bravo = resource(pool, "Bravo", ResourceType.WORK, 5);
		ResourceImpl cement = resource(pool, "Cement", ResourceType.MATERIAL, 1);
		List<Resource> originalOrder = List.copyOf(pool.getResourceList());
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createResourceNodeModelCache(pool, pool.getResourceOutline());
		ResourceView.ResourceFieldSorter sorter = new ResourceView.ResourceFieldSorter(List.of(
			new ResourceView.ResourceFieldSorter.Key(
				Comparator.comparingDouble(resource -> ((Resource) resource).getStandardRate().getValue()), false),
			new ResourceView.ResourceFieldSorter.Key(
				Comparator.comparing(resource -> ((Resource) resource).getName()), true)));
		ViewNodeModelCache view = (ViewNodeModelCache) NodeModelCacheFactory.getInstance().createFilteredCache(
			reference, "filtered-grouped-resource-sort-test", transformer -> {
				ViewTransformer viewTransformer = (ViewTransformer) transformer;
				viewTransformer.setUserFilterId("Filter.ResourcesWork");
				viewTransformer.setUserGrouperId("Grouper.ResourceType");
				viewTransformer.setUserSorter(sorter);
			});
		reference.update();
		try {
			List<GraphicNode> projected = view.getVisibleNodes().getElements().stream()
				.filter(GraphicNode.class::isInstance).map(GraphicNode.class::cast).toList();
			List<String> names = new ArrayList<>();
			for (GraphicNode node : projected)
				if (!node.isGroup() && node.getNode().getImpl() instanceof Resource resource) names.add(resource.getName());
			assertEquals(List.of("Alpha", "Zulu", "Bravo"), names,
				"rate descending, then name ascending for the 10/10 tie; filtered material is hidden");
			assertFalse(names.contains("Cement"), "the work-resource filter must hide material resources");
			assertEquals(originalOrder, pool.getResourceList(), "view sort/filter/group must not mutate resource order");
		} finally {
			view.close();
			reference.close();
		}
	}

	private static ResourceImpl resource(ResourcePool pool, String name, int type, double rate) {
		ResourceImpl resource = (ResourceImpl) pool.createScriptedResource();
		resource.setName(name);
		resource.setResourceType(type);
		resource.setStandardRate(new Rate(rate));
		return resource;
	}
}
