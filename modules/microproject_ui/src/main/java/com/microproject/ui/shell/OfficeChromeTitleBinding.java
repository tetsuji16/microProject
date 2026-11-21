/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import java.util.function.Consumer;

import javax.swing.JFrame;

/** Bridges the existing document-frame title to the two Office chrome projections. */
final class OfficeChromeTitleBinding {
	private static final String APPLICATION_NAME = "microProject";

	private final JFrame frame;
	private final Consumer<String> documentTitleConsumer;
	private boolean synchronizingNativeTitle;
	private java.beans.PropertyChangeListener listener;

	private OfficeChromeTitleBinding(JFrame frame, Consumer<String> documentTitleConsumer) {
		this.frame = frame;
		this.documentTitleConsumer = documentTitleConsumer;
	}

	static OfficeChromeTitleBinding attach(JFrame frame, Consumer<String> documentTitleConsumer) {
		OfficeChromeTitleBinding binding = new OfficeChromeTitleBinding(frame, documentTitleConsumer);
		binding.listener = event -> binding.onTitleChanged((String) event.getNewValue());
		frame.addPropertyChangeListener("title", binding.listener);
		binding.project(frame.getTitle());
		return binding;
	}

	/** Stops following the frame title, so a replaced shell is neither retained nor updated. */
	void detach() {
		if (listener != null) frame.removePropertyChangeListener("title", listener);
		listener = null;
	}

	private void onTitleChanged(String title) {
		if (synchronizingNativeTitle) return;
		project(title);
	}

	private void project(String title) {
		documentTitleConsumer.accept(title);
		String nativeTitle = applicationTitle(title);
		if (nativeTitle.equals(frame.getTitle())) return;
		synchronizingNativeTitle = true;
		try {
			frame.setTitle(nativeTitle);
		} finally {
			synchronizingNativeTitle = false;
		}
	}

	static String compactDocumentTitle(String title) {
		if (title == null || title.isBlank()) return APPLICATION_NAME;
		int appSeparator = title.indexOf(" - ");
		if (appSeparator >= 0 && appSeparator + 3 < title.length()) title = title.substring(appSeparator + 3);
		int separator = Math.max(title.lastIndexOf('\\'), title.lastIndexOf('/'));
		return separator >= 0 ? title.substring(separator + 1) : title;
	}

	private static String applicationTitle(String title) {
		if (title == null || title.isBlank()) return APPLICATION_NAME;
		int separator = title.indexOf(" - ");
		if (separator <= 0)
			return title;
		// Keep native window titles document-specific for task switching, while
		// applying the same path-free compacting rule used by the Office chrome.
		return APPLICATION_NAME + " - " + compactDocumentTitle(title);
	}
}
