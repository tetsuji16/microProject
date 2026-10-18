package com.microproject.reports.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DataSourceProviderTest {

	@Test
	void supportedReportCollectionsResolveToTheirStandardViews() {
		assertEquals(DataSourceProvider.REPORT_VIEW, DataSourceProvider.getViewName(DataSourceProvider.PROJECT));
		assertEquals(DataSourceProvider.TASK_REPORT_VIEW, DataSourceProvider.getViewName(DataSourceProvider.TASKS_TREE));
		assertEquals(DataSourceProvider.TASK_REPORT_VIEW, DataSourceProvider.getViewName(DataSourceProvider.ASSIGNMENTS_PROJECT_BASED));
		assertEquals(DataSourceProvider.RESOURCE_REPORT_VIEW, DataSourceProvider.getViewName(DataSourceProvider.RESOURCES_TREE));
		assertEquals(DataSourceProvider.RESOURCE_REPORT_VIEW, DataSourceProvider.getViewName(DataSourceProvider.ASSIGNMENTS_RESOURCE_BASED));
		assertNull(DataSourceProvider.getViewName(DataSourceProvider.PREDECESSORS));
	}
}
