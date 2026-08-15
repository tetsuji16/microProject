package com.projectlibre.ui.ribbon;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.projectlibre.ui.ribbon.SwingRibbonModel.CustomBandProvider;
import com.microproject.menu.ExtToolBarFactory;
import com.projectlibre.ui.ribbon.CustomRibbonBandGenerator;
import com.microproject.util.FlatUiSupport;

public final class SwingRibbonFactory {
	private final ExtToolBarFactory buttonFactory;
	private final ResourceBundle[] bundles;
	private final RibbonIconRegistry iconRegistry;

	public SwingRibbonFactory(ExtToolBarFactory buttonFactory, ResourceBundle... bundles) {
		this.buttonFactory = Objects.requireNonNull(buttonFactory);
		this.bundles = Objects.requireNonNull(bundles);
		this.iconRegistry = new RibbonIconRegistry(bundles);
	}

	public SwingRibbonModel createModel(String ribbonId) {
		return createModel(ribbonId, null);
	}

	public SwingRibbonModel createModel(String ribbonId, CustomRibbonBandGenerator customBandsGenerator) {
		Objects.requireNonNull(ribbonId);
		List<SwingRibbonModel.RibbonTab> tabs = new ArrayList<>();
		for (String tabId : resolveList(ribbonId)) {
			tabs.add(createTab(tabId, customBandsGenerator));
		}
		List<String> taskBarButtons = resolveList(ribbonId + ".TaskBar");
		SwingRibbonModel model = new SwingRibbonModel(ribbonId, tabs, taskBarButtons);
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
		ModernRibbonPanel panel = new ModernRibbonPanel(model, buttonFactory, bundles, helpAction);
		panel.build();
		JPanel host = new JPanel(new BorderLayout());
		host.setOpaque(true);
		host.setBackground(FlatUiSupport.ribbonChromeBackground());
		host.add(panel, BorderLayout.CENTER);
		return host;
	}

	public String getActionStringFromId(String id) {
		return buttonFactory.getActionStringFromId(id);
	}

	public List<?> getButtonsFromId(String id) {
		return buttonFactory.getButtonsFromId(id);
	}

	private SwingRibbonModel.RibbonTab createTab(String tabId, CustomRibbonBandGenerator customBandsGenerator) {
		List<SwingRibbonModel.RibbonBand> bands = new ArrayList<>();
		for (String bandId : resolveList(tabId)) {
			bands.add(createBand(bandId, customBandsGenerator));
		}
		return new SwingRibbonModel.RibbonTab(tabId, resolveString(tabId + ".title"), bands);
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

		List<SwingRibbonModel.RibbonButton> buttons = new ArrayList<>();
		for (String token : resolveList(bandId)) {
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
			buttons.add(new SwingRibbonModel.RibbonButton(
				buttonId,
				priority,
				size,
				iconRegistry.resolve(buttonId),
				"TOGGLE".equalsIgnoreCase(type),
				presentation,
				collapsePriority));
		}
		return new SwingRibbonModel.RibbonBand(bandId, resolveString(bandId + ".title"), buttons);
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
		String[] tokens = value.trim().split("\\s+");
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
		throw new MissingResourceException("Missing ribbon resource", SwingRibbonFactory.class.getName(), key);
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
