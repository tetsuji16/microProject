package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.microproject.server.data.CustomFieldsMapper;

import org.mpxj.ResourceField;
import org.mpxj.TaskField;

class CustomFieldsMapperTest {
	@Test
	void resolvesRepresentativeTaskAndResourceCustomFieldConstants() {
		CustomFieldsMapper mapper = CustomFieldsMapper.getInstance();

		assertSame(TaskField.TEXT1, mapper.taskMaps.textMap[0]);
		assertSame(ResourceField.TEXT1, mapper.resourceMaps.textMap[0]);
	}
}
