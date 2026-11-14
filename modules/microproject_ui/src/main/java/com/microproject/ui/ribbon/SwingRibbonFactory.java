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
package com.microproject.ui.ribbon;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.microproject.ribbon.CustomRibbonBandGenerator;
import com.microproject.ribbon.RibbonActivation;
import com.microproject.ribbon.RibbonCommandSource;
import com.microproject.ribbon.RibbonControlKind;
import com.microproject.ribbon.RibbonIconRegistry;
import com.microproject.ribbon.SwingRibbonModel;
import com.microproject.ribbon.SwingRibbonModel.CustomBandProvider;

public final class SwingRibbonFactory {
	private final RibbonCommandSource commandSource;
	private final ResourceBundle[] bundles;
	private volatile RibbonDropDownSource dropDownSource;
	private final RibbonIconRegistry iconRegistry;

	public SwingRibbonFactory(RibbonCommandSource commandSource, ResourceBundle... bundles) {
		this.commandSource = Objects.requireNonNull(commandSource);
		this.bundles = Objects.requireNonNull(bundles);
		this.iconRegistry = new RibbonIconRegistry(bundles);
	}

	public SwingRibbonModel createModel(String ribbonId) {
		return createModel(ribbonId, null);
	}

	public SwingRibbonModel createModel(String ribbonId, CustomRibbonBandGenerator customBandsGenerator) {
		Objects.requireNonNull(ribbonId);
		List<String> tabIds = resolveList(ribbonId);
		List<SwingRibbonModel.RibbonTab> tabs = new ArrayList<>(tabIds.size());
		for (String tabId : tabIds) {
			tabs.add(createTab(tabId, customBandsGenerator));
		}
		List<String> taskBarButtons = resolveList(ribbonId + ".TaskBar");
		List<SwingRibbonModel.RibbonButton> taskBarControls = new ArrayList<>(taskBarButtons.size());
		for (String buttonId : taskBarButtons) {
			taskBarControls.add(applyActivation(applyControlContract(
				new SwingRibbonModel.RibbonButton(buttonId, SwingRibbonModel.ButtonPriority.LOW))));
		}
		SwingRibbonModel model = new SwingRibbonModel(ribbonId, tabs, taskBarButtons).withTaskBarControls(taskBarControls);
		RibbonCommandCatalog.validate(model, bundles);
		return model;
	}

	public JPanel createPanel(String ribbonId, Runnable helpAction) {
		return createPanel(createModel(ribbonId), helpAction);
	}

	public JPanel createPanel(String ribbonId, CustomRibbonBandGenerator customBandsGenerator, Runnable helpAction) {
		return createPanel(createModel(ribbonId, customBandsGenerator), helpAction);
	}

	public JPanel createPanel(SwingRibbonModel model, Runnable helpAction) {
		Objects.requireNonNull(model);
		ModernRibbonPanel panel = new ModernRibbonPanel(model, commandSource, bundles, helpAction);
		panel.setDropDownSource(() -> dropDownSource);
		panel.build();
		JPanel host = new JPanel(new BorderLayout());
		host.setOpaque(true);
		host.setBackground(new FlatLafRibbonTheme().chromeBackground());
		host.add(panel, BorderLayout.CENTER);
		host.putClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY, panel);
		return host;
	}

	/** Sets where the drop-down controls of every ribbon built by this factory read their values. */
	public void setDropDownSource(RibbonDropDownSource source) {
		this.dropDownSource = source;
	}

	/** Re-reads the label of the drop-downs of {@code id} on the event dispatch thread. */
	public void refreshDropDowns(String id) {
		Runnable refresh = () -> {
			for (javax.swing.AbstractButton button : commandSource.getButtons(id)) {
				if (button.getClientProperty(RibbonControlPopups.REFRESH_PROPERTY) instanceof Runnable action) {
					action.run();
				}
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			refresh.run();
		} else {
			javax.swing.SwingUtilities.invokeLater(refresh);
		}
	}

	/**
	 * The popup of the split button or menu registered for {@code id}, or {@code null}
	 * when the command has none. For the quick access bar, whose buttons are drawn by
	 * the window chrome.
	 */
	public javax.swing.JPopupMenu getControlPopup(String id) {
		for (JComponent control : commandSource.getCommandControls(id)) {
			if (control.getClientProperty(RibbonControlPopups.POPUP_PROPERTY) instanceof javax.swing.JPopupMenu popup) {
				return popup;
			}
		}
		return null;
	}

	/** Re-reads the dynamic entries of {@code popup}; false when it then has nothing to show. */
	public static boolean prepareControlPopup(javax.swing.JPopupMenu popup) {
		return RibbonControlPopups.prepareForShow(popup);
	}

	public String getActionStringFromId(String id) {
		return commandSource.getActionId(id);
	}

	public List<?> getButtonsFromId(String id) {
		return commandSource.getButtons(id);
	}

	public List<JComponent> getRibbonControlsFromId(String id) {
		return commandSource.getCommandControls(id);
	}

	public void registerRibbonControl(String id, JComponent component) {
		commandSource.registerCommandControl(id, component);
	}

	public void unregisterRibbonControls(List<? extends JComponent> controls) {
		commandSource.unregisterCommandControls(controls);
	}

	private SwingRibbonModel.RibbonTab createTab(String tabId, CustomRibbonBandGenerator customBandsGenerator) {
		List<String> bandIds = resolveList(tabId);
		List<SwingRibbonModel.RibbonBand> bands = new ArrayList<>(bandIds.size());
		for (String bandId : bandIds) {
			bands.add(createBand(bandId, customBandsGenerator));
		}
		return new SwingRibbonModel.RibbonTab(tabId, resolveString(tabId + ".title"), bands,
			Boolean.parseBoolean(resolveStringOrNull(tabId + ".contextual")), resolveStringOrNull(tabId + ".accessKey"));
	}

	private SwingRibbonModel.RibbonBand createBand(String bandId, CustomRibbonBandGenerator customBandsGenerator) {
		if (customBandsGenerator != null) {
			JComponent sampleComponent = customBandsGenerator.createRibbonComponent(bandId);
			if (sampleComponent != null) {
				return new SwingRibbonModel.RibbonBand(
					bandId,
					resolveString(bandId + ".title"),
					createCustomBandProvider(bandId, customBandsGenerator, sampleComponent));
			}
		}

		List<String> tokens = resolveList(bandId);
		List<SwingRibbonModel.RibbonButton> buttons = new ArrayList<>(tokens.size());
		for (String token : tokens) {
			if ("-".equals(token)) {
				continue;
			}
			SwingRibbonModel.ButtonPriority priority = SwingRibbonModel.ButtonPriority.MEDIUM;
			String buttonId = token;
			if (token.endsWith(".TOP")) {
				priority = SwingRibbonModel.ButtonPriority.TOP;
				buttonId = token.substring(0, token.length() - 4);
			} else if (token.endsWith(".LOW")) {
				priority = SwingRibbonModel.ButtonPriority.LOW;
				buttonId = token.substring(0, token.length() - 4);
			}
			String type = resolveStringOrNull(buttonId + ".type");
			String presentationValue = resolveStringOrNull(buttonId + ".presentation");
			SwingRibbonModel.ButtonPresentation presentation = "SPLIT".equalsIgnoreCase(presentationValue)
				? SwingRibbonModel.ButtonPresentation.SPLIT
				: SwingRibbonModel.ButtonPresentation.COMMAND;
			SwingRibbonModel.ButtonSize size = resolveButtonSize(buttonId, priority);
			int collapsePriority = resolveCollapsePriority(buttonId, priority);
			String text = resolveStringOrNull(buttonId + ".text");
			buttons.add(applyActivation(applyControlContract(new SwingRibbonModel.RibbonButton(
				buttonId,
				text == null ? buttonId : text,
				priority,
				size,
				iconRegistry.resolve(buttonId),
				"TOGGLE".equalsIgnoreCase(type),
				presentation,
				collapsePriority,
				resolveStringOrNull(buttonId + ".backstagePage"),
				Boolean.parseBoolean(resolveStringOrNull(buttonId + ".backstageImmediate"))))));
		}
		SwingRibbonModel.RibbonBand band = new SwingRibbonModel.RibbonBand(bandId, resolveString(bandId + ".title"), buttons);
		String dialogLauncher = resolveStringOrNull(bandId + ".dialogLauncher");
		return dialogLauncher == null || dialogLauncher.isBlank() ? band : band.withDialogLauncher(dialogLauncher.trim());
	}

	/**
	 * Reads the Office control contract of a command: {@code X.control} (a
	 * {@link RibbonControlKind}), {@code X.items} (popup command ids, {@code -} is a
	 * separator) and {@code X.keytip}. Commands that declare none of them keep the
	 * kind implied by {@code X.type} / {@code X.presentation}.
	 */
	private SwingRibbonModel.RibbonButton applyControlContract(SwingRibbonModel.RibbonButton button) {
		String buttonId = button.getId();
		String controlValue = resolveStringOrNull(buttonId + ".control");
		List<String> items = resolveList(buttonId + ".items");
		String keyTip = resolveStringOrNull(buttonId + ".keytip");
		if (controlValue == null && items.isEmpty() && keyTip == null) {
			return button;
		}
		RibbonControlKind kind = button.getControlKind();
		if (controlValue != null) {
			try {
				kind = RibbonControlKind.parse(controlValue);
			} catch (IllegalArgumentException ex) {
				throw new IllegalStateException("Invalid " + buttonId + ".control: " + ex.getMessage(), ex);
			}
		}
		if (kind == RibbonControlKind.DIALOG_LAUNCHER) {
			throw new IllegalStateException(buttonId + ".control: a dialog launcher belongs to a group; declare <band>.dialogLauncher instead");
		}
		if (!items.isEmpty() && !kind.hasPopup()) {
			throw new IllegalStateException(buttonId + ".items is declared but control kind " + kind + " has no popup");
		}
		return button.withControl(kind, items, keyTip == null || keyTip.isBlank() ? null : keyTip.trim());
	}

	/**
	 * Reads {@code X.activation} (a {@link RibbonActivation}): what a click on the
	 * control body does. Commands that declare none keep the activation implied by
	 * their kind, {@code X.type} and Backstage metadata. A declaration that
	 * contradicts that metadata fails model creation.
	 */
	private SwingRibbonModel.RibbonButton applyActivation(SwingRibbonModel.RibbonButton button) {
		String buttonId = button.getId();
		String value = resolveStringOrNull(buttonId + ".activation");
		if (value == null) {
			return button;
		}
		RibbonActivation activation;
		try {
			activation = RibbonActivation.parse(value);
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("Invalid " + buttonId + ".activation: " + ex.getMessage(), ex);
		}
		RibbonControlKind kind = button.getControlKind();
		boolean popupOnly = kind.hasPopup() && !kind.invokesOnClick();
		if ((activation == RibbonActivation.POPUP) != popupOnly) {
			throw new IllegalStateException(buttonId + ".activation " + activation + " contradicts control kind " + kind
				+ ": exactly the kinds whose click only opens their popup are POPUP");
		}
		if (activation.keepsPressedState() != button.isToggle() && !popupOnly) {
			throw new IllegalStateException(buttonId + ".activation " + activation + " contradicts " + buttonId
				+ ".type: exactly the activations that keep a pressed state need type=TOGGLE");
		}
		boolean backstagePage = button.getBackstagePage() != null && !button.isBackstageImmediate();
		if ((activation == RibbonActivation.BACKSTAGE) != backstagePage) {
			throw new IllegalStateException(buttonId + ".activation " + activation + " contradicts " + buttonId
				+ ".backstagePage: exactly the commands that open a Backstage page are BACKSTAGE");
		}
		return button.withActivation(activation);
	}

	private SwingRibbonModel.ButtonSize resolveButtonSize(String buttonId, SwingRibbonModel.ButtonPriority priority) {
		String value = resolveStringOrNull(buttonId + ".size");
		if (value != null) {
			try {
				return SwingRibbonModel.ButtonSize.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
				// Preserve compatibility with older resource bundles.
			}
		}
		return switch (priority) {
			case TOP -> SwingRibbonModel.ButtonSize.LARGE;
			case MEDIUM -> SwingRibbonModel.ButtonSize.MEDIUM;
			case LOW -> SwingRibbonModel.ButtonSize.SMALL;
		};
	}

	private int resolveCollapsePriority(String buttonId, SwingRibbonModel.ButtonPriority priority) {
		String value = resolveStringOrNull(buttonId + ".collapsePriority");
		if (value != null) {
			try {
				return Integer.parseInt(value.trim());
			} catch (NumberFormatException ignored) {
				// Preserve compatibility with older resource bundles.
			}
		}
		return switch (priority) {
			case TOP -> 100;
			case MEDIUM -> 50;
			case LOW -> 0;
		};
	}

	private CustomBandProvider createCustomBandProvider(String bandId, CustomRibbonBandGenerator customBandsGenerator, JComponent sampleComponent) {
		int preferredWidth = sampleComponent == null || sampleComponent.getPreferredSize() == null
			? -1
			: sampleComponent.getPreferredSize().width;
		return new CustomBandProvider() {
			@Override
			public JComponent createComponent() {
				return customBandsGenerator.createRibbonComponent(bandId);
			}

			@Override
			public int getPreferredWidthHint() {
				return preferredWidth;
			}
		};
	}

	private List<String> resolveList(String key) {
		String value = resolveStringOrNull(key);
		if (value == null || value.isBlank()) {
			return List.of();
		}
		String[] tokens = value.trim().split("\\s+", -1);
		List<String> result = new ArrayList<>(tokens.length);
		for (String token : tokens) {
			if (!token.isBlank()) {
				result.add(token);
			}
		}
		return result;
	}

	private String resolveString(String key) {
		String value = resolveStringOrNull(key);
		if (value != null) {
			return value;
		}
		throw new MissingResourceException("Missing ribbon resource: " + key,
				SwingRibbonFactory.class.getName(), key);
	}

	private String resolveStringOrNull(String key) {
		for (ResourceBundle bundle : bundles) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException ex) {
				// keep searching the fallback chain
			}
		}
		return null;
	}
}
