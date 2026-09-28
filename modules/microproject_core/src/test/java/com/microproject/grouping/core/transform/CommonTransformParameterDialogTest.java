/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.grouping.core.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.field.InvalidFormulaException;

class CommonTransformParameterDialogTest {
	@AfterEach
	void clearProvider() {
		TransformParameterDialogServices.setProvider(null);
	}

	@Test
	void requestsDialogFromProviderOncePerTransform() {
		AtomicInteger created = new AtomicInteger();
		AtomicReference<CommonTransform> shown = new AtomicReference<>();
		TransformParameterDialogServices.setProvider(() -> {
			created.incrementAndGet();
			return shown::set;
		});
		CommonTransform transform = new TestTransform();
		TransformParameter parameter = new TransformParameter();
		parameter.setId("date");
		parameter.setValue(1L);
		transform.addParameter(parameter);

		transform.askForParameters();
		transform.askForParameters();

		assertEquals(1, created.get());
		assertSame(transform, shown.get());
	}

	@Test
	void doesNotCreateDialogWhenTransformHasNoParameters() {
		AtomicInteger created = new AtomicInteger();
		TransformParameterDialogServices.setProvider(() -> {
			created.incrementAndGet();
			return ignored -> { };
		});

		new TestTransform().askForParameters();

		assertEquals(0, created.get());
	}

	@Test
	void allowsLegacySubclassDialogConsumerAndNoProvider() {
		AtomicReference<Object> shown = new AtomicReference<>();
		TestTransform transform = new TestTransform();
		transform.parameterDialog = shown::set;
		TransformParameter parameter = new TransformParameter();
		parameter.setId("date");
		parameter.setValue(1L);
		transform.addParameter(parameter);

		transform.askForParameters();

		assertSame(transform, shown.get());
		TransformParameterDialogServices.setProvider(null);
		new TestTransformWithParameter().askForParameters();
	}

	private static final class TestTransformWithParameter extends TestTransform {
		private TestTransformWithParameter() {
			TransformParameter parameter = new TransformParameter();
			parameter.setId("date");
			parameter.setValue(1L);
			addParameter(parameter);
		}
	}

	private static class TestTransform extends CommonTransformFactory {
		@Override
		public CommonTransform getTransform() throws InvalidFormulaException {
			return this;
		}
	}
}
