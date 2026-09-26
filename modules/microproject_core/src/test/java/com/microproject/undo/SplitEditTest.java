/*
 * MIT License
 *
 * Copyright (c) 2026 microProject
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package com.microproject.undo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.microproject.pm.scheduling.Schedule;
import com.microproject.server.data.DataObject;

class SplitEditTest {
	@Test
	void presentationNameIncludesDataObjectDetailsWhenScheduleSupportsThem() {
		Schedule schedule = (Schedule) Proxy.newProxyInstance(Schedule.class.getClassLoader(),
			new Class<?>[] { Schedule.class, DataObject.class }, (proxy, method, arguments) -> switch (method.getName()) {
				case "getName" -> "Sample";
				case "getUniqueId" -> 42L;
				default -> null;
			});

		String simpleClassName = schedule.getClass().getName();
		simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf('.') + 1);
		SplitEdit edit = new SplitEdit(schedule, null, 0L, 0L, this);

		assertEquals("Split: " + simpleClassName + " Sample(42)", edit.getPresentationName());
	}

	@Test
	void presentationNameFallsBackWhenScheduleIsNotADataObject() {
		Schedule schedule = (Schedule) Proxy.newProxyInstance(Schedule.class.getClassLoader(),
			new Class<?>[] { Schedule.class }, (proxy, method, arguments) -> null);

		assertEquals("Split", new SplitEdit(schedule, null, 0L, 0L, this).getPresentationName());
	}
}
