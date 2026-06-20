package com.projectlibre1.pm.graphic.frames;

import java.awt.GraphicsEnvironment;
import java.util.function.BooleanSupplier;

import com.projectlibre1.dialog.RecurringTaskDialog;
import com.projectlibre1.pm.task.RecurringTaskSpec;

final class RecurringTaskCoordinator {
	interface SpecProvider {
		RecurringTaskSpec show(DocumentFrame frame);
	}

	interface InsertionHandler {
		void insert(DocumentFrame frame, RecurringTaskSpec spec);
	}

	private final BooleanSupplier headlessChecker;
	private final SpecProvider specProvider;
	private final InsertionHandler insertionHandler;

	RecurringTaskCoordinator() {
		this(
			GraphicsEnvironment::isHeadless,
			new SpecProvider() {
				@Override
				public RecurringTaskSpec show(DocumentFrame frame) {
					RecurringTaskDialog dialog = RecurringTaskDialog.getInstance(frame.getGraphicManager().getFrame());
					return dialog.doModal() ? dialog.getSpec() : null;
				}
			},
			new InsertionHandler() {
				@Override
				public void insert(DocumentFrame frame, RecurringTaskSpec spec) {
					new RecurringTaskInsertionService().insertRecurringTasks(frame, spec);
				}
			});
	}

	RecurringTaskCoordinator(
		BooleanSupplier headlessChecker,
		SpecProvider specProvider,
		InsertionHandler insertionHandler) {
		this.headlessChecker = headlessChecker;
		this.specProvider = specProvider;
		this.insertionHandler = insertionHandler;
	}

	boolean openDialogAndInsert(DocumentFrame frame) {
		if (headlessChecker.getAsBoolean())
			return false;
		RecurringTaskSpec spec = specProvider.show(frame);
		if (spec == null)
			return false;
		insertionHandler.insert(frame, spec);
		return true;
	}
}
