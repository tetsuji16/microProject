/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.ui.ribbon;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;

public final class SwingRibbonModel {
	public enum ButtonPriority {
		TOP,
		MEDIUM,
		LOW
	}

	public enum ButtonSize {
		LARGE,
		MEDIUM,
		SMALL
	}

	/**
	 * Presentation metadata is deliberately independent from the action id.  It
	 * lets the resource-driven command map remain the functional source of truth
	 * while the ribbon can choose an Office-like affordance for a command.
	 */
	public enum ButtonPresentation {
		COMMAND,
		SPLIT,
		BACKSTAGE
	}

	public enum RibbonBandKind {
		BUTTONS,
		CUSTOM
	}

	public interface CustomBandProvider {
		JComponent createComponent();

		default int getPreferredWidthHint() {
			return -1;
		}
	}

	public static final class RibbonButton {
		private final String id;
		private final ButtonPriority priority;
		private final ButtonSize size;
		private final String iconKey;
		private final boolean toggle;
		private final ButtonPresentation presentation;
		private final int collapsePriority;

		public RibbonButton(String id, ButtonPriority priority) {
			this(id, priority, null, false);
		}

		public RibbonButton(String id, ButtonPriority priority, String iconKey, boolean toggle) {
			this(id, priority, iconKey, toggle, ButtonPresentation.COMMAND);
		}

		public RibbonButton(String id, ButtonPriority priority, String iconKey, boolean toggle, ButtonPresentation presentation) {
			this(id, priority, defaultSize(priority), iconKey, toggle, presentation, defaultCollapsePriority(priority));
		}

		public RibbonButton(String id, ButtonPriority priority, ButtonSize size, String iconKey, boolean toggle,
			ButtonPresentation presentation, int collapsePriority) {
			this.id = Objects.requireNonNull(id);
			this.priority = Objects.requireNonNull(priority);
			this.size = Objects.requireNonNull(size);
			this.iconKey = iconKey;
			this.toggle = toggle;
			this.presentation = Objects.requireNonNull(presentation);
			this.collapsePriority = collapsePriority;
		}

		public String getId() {
			return id;
		}

		public ButtonPriority getPriority() {
			return priority;
		}

		public ButtonSize getButtonSize() {
			return size;
		}

		private static ButtonSize defaultSize(ButtonPriority priority) {
			return switch (priority) {
				case TOP -> ButtonSize.LARGE;
				case MEDIUM -> ButtonSize.MEDIUM;
				case LOW -> ButtonSize.SMALL;
			};
		}

		private static int defaultCollapsePriority(ButtonPriority priority) {
			return switch (priority) {
				case TOP -> 100;
				case MEDIUM -> 50;
				case LOW -> 0;
			};
		}

		public String getIconKey() {
			return iconKey;
		}

		public boolean isToggle() {
			return toggle;
		}

		public ButtonPresentation getPresentation() {
			return presentation;
		}

		public boolean isSplit() {
			return presentation == ButtonPresentation.SPLIT;
		}

		public int getCollapsePriority() {
			return collapsePriority;
		}
	}

	public static final class RibbonBand {
		private final String id;
		private final String title;
		private final RibbonBandKind kind;
		private final List<RibbonButton> buttons;
		private final CustomBandProvider customBandProvider;
		private final int preferredWidthHint;

		public RibbonBand(String id, String title, List<RibbonButton> buttons) {
			this(id, title, RibbonBandKind.BUTTONS, List.copyOf(buttons), null, -1);
		}

		public RibbonBand(String id, String title, CustomBandProvider customBandProvider) {
			this(id, title, RibbonBandKind.CUSTOM, List.of(), Objects.requireNonNull(customBandProvider), customBandProvider.getPreferredWidthHint());
		}

		private RibbonBand(String id, String title, RibbonBandKind kind, List<RibbonButton> buttons, CustomBandProvider customBandProvider, int preferredWidthHint) {
			this.id = Objects.requireNonNull(id);
			this.title = Objects.requireNonNull(title);
			this.kind = Objects.requireNonNull(kind);
			this.buttons = buttons;
			this.customBandProvider = customBandProvider;
			this.preferredWidthHint = preferredWidthHint;
		}

		public String getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public RibbonBandKind getKind() {
			return kind;
		}

		public boolean isCustomBand() {
			return kind == RibbonBandKind.CUSTOM;
		}

		public List<RibbonButton> getButtons() {
			return buttons;
		}

		public CustomBandProvider getCustomBandProvider() {
			return customBandProvider;
		}

		public int getPreferredWidthHint() {
			return preferredWidthHint;
		}
	}

	public static final class RibbonTab {
		private final String id;
		private final String title;
		private final List<RibbonBand> bands;
		private final boolean contextual;

		public RibbonTab(String id, String title, List<RibbonBand> bands) {
			this(id, title, bands, false);
		}

		public RibbonTab(String id, String title, List<RibbonBand> bands, boolean contextual) {
			this.id = Objects.requireNonNull(id);
			this.title = Objects.requireNonNull(title);
			this.bands = List.copyOf(bands);
			this.contextual = contextual;
		}

		public String getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public List<RibbonBand> getBands() {
			return bands;
		}

		/** A view-specific tab, such as Gantt Chart Tools > Format. */
		public boolean isContextual() {
			return contextual;
		}
	}

	private final String id;
	private final List<RibbonTab> tabs;
	private final List<String> taskBarButtons;

	public SwingRibbonModel(String id, List<RibbonTab> tabs, List<String> taskBarButtons) {
		this.id = Objects.requireNonNull(id);
		this.tabs = List.copyOf(tabs);
		this.taskBarButtons = List.copyOf(taskBarButtons);
	}

	public String getId() {
		return id;
	}

	public List<RibbonTab> getTabs() {
		return Collections.unmodifiableList(tabs);
	}

	public List<String> getTaskBarButtons() {
		return Collections.unmodifiableList(taskBarButtons);
	}
}
