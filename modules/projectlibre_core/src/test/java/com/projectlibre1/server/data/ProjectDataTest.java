package com.projectlibre1.server.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ProjectDataTest {
	@Test
	void budgetStatusIndicatorUsesCpiLabel() {
		ProjectData data = new ProjectData();
		Map values = new HashMap();
		values.put("Field.cpi", Double.valueOf(1.2D));
		values.put("Field.spi", Double.valueOf(0.8D));
		data.setFieldValues(values);

		assertEquals("CPI=1.2", data.getBudgetStatusIndicator().getLabel());
		assertEquals("SPI=0.8", data.getScheduleStatusIndicator().getLabel());
	}
}
