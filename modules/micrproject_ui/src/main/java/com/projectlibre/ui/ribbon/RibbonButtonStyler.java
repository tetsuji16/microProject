package com.projectlibre.ui.ribbon;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.microproject.pm.graphic.IconManager;
import com.microproject.util.FlatUiSupport;

final class RibbonButtonStyler {
	static final String SIZE_PROPERTY = "ProjectLibre.ribbonButtonSize";
	static final String ICON_KEY_PROPERTY = "ProjectLibre.ribbonIconKey";
	private static final int LARGE_ICON_SIZE = 32;
	private static final int MEDIUM_ICON_SIZE = 20;
	private static final int SMALL_ICON_SIZE = 16;
	private static final int LARGE_TEXT_WIDTH = 80;
	// Includes the Office command border and Swing button margins as well as
	// breathing room around the label. Measuring text alone caused the UI delegate
	// to replace the last Japanese glyphs with an ellipsis.
	private static final int LARGE_HORIZONTAL_PADDING = 18;
	private static final int INLINE_HORIZONTAL_PADDING = 10;
	private static final int INLINE_ICON_TEXT_GAP = 4;
	private static final int LARGE_ICON_TEXT_GAP = 4;
	private static final int LARGE_LONG_LABEL_WIDTH = 92;
	private static final int SPLIT_BUTTON_EXTRA_WIDTH = 18;

	AbstractButton styleActionButton(AbstractButton button, boolean large) {
		return styleActionButton(button, large ? "large" : "small");
	}

	AbstractButton styleActionButton(AbstractButton button, String size) {
		if ("large".equals(size)) {
			RibbonButtonMetrics metrics = RibbonButtonMetrics.large(button);
			FlatUiSupport.styleRibbonLargeButton(button);
			button.setFont(FlatUiSupport.ribbonButtonFont());
			button.setText(metrics.displayText());
			applyRibbonIcon(button, LARGE_ICON_SIZE);
			button.setHorizontalTextPosition(SwingConstants.CENTER);
			button.setVerticalTextPosition(SwingConstants.BOTTOM);
			button.setIconTextGap(LARGE_ICON_TEXT_GAP);
			button.setAlignmentX(Component.CENTER_ALIGNMENT);
			button.putClientProperty(SIZE_PROPERTY, "large");
			applyButtonSize(button, metrics);
		} else if ("medium".equals(size)) {
			RibbonButtonMetrics metrics = RibbonButtonMetrics.inline(button, FlatUiSupport.ribbonInlineButtonMediumMinWidth(), FlatUiSupport.ribbonInlineButtonHeight(), MEDIUM_ICON_SIZE);
			FlatUiSupport.styleRibbonSmallButton(button);
			button.setFont(FlatUiSupport.ribbonButtonFont());
			button.setText(metrics.displayText());
			applyRibbonIcon(button, MEDIUM_ICON_SIZE);
			button.setHorizontalAlignment(SwingConstants.LEFT);
			button.setHorizontalTextPosition(SwingConstants.RIGHT);
			button.setVerticalTextPosition(SwingConstants.CENTER);
			button.setIconTextGap(INLINE_ICON_TEXT_GAP);
			button.putClientProperty(SIZE_PROPERTY, "medium");
			applyButtonSize(button, metrics);
		} else {
			RibbonButtonMetrics metrics = RibbonButtonMetrics.inline(button, FlatUiSupport.ribbonInlineButtonSmallMinWidth(), FlatUiSupport.ribbonInlineButtonHeight(), SMALL_ICON_SIZE);
			FlatUiSupport.styleRibbonSmallButton(button);
			button.setFont(FlatUiSupport.ribbonButtonFont());
			button.setText(metrics.displayText());
			applyRibbonIcon(button, SMALL_ICON_SIZE);
			button.setHorizontalAlignment(SwingConstants.LEFT);
			button.setHorizontalTextPosition(SwingConstants.RIGHT);
			button.setVerticalTextPosition(SwingConstants.CENTER);
			button.setIconTextGap(INLINE_ICON_TEXT_GAP);
			button.putClientProperty(SIZE_PROPERTY, "small");
			applyButtonSize(button, metrics);
		}
		return button;
	}

	private void applyButtonSize(AbstractButton button, RibbonButtonMetrics metrics) {
		Dimension sizeValue = metrics.preferredSize();
		button.setMinimumSize(sizeValue);
		button.setPreferredSize(sizeValue);
	}

	private void applyRibbonIcon(AbstractButton button, int iconSize) {
		if (button == null) {
			return;
		}
		Icon ribbonIcon = null;
		try {
			Object iconKeyProperty = button.getClientProperty(ICON_KEY_PROPERTY);
			if (iconKeyProperty instanceof String iconKey && !iconKey.isBlank()) {
				// Preserve the semantic colour drawn by our SVG assets.  State variants
				// are tinted separately below; tinting the normal state made every
				// command look like a disabled monochrome glyph.
				ribbonIcon = IconManager.getRibbonIcon(iconKey, iconSize, iconSize);
			}
		} catch (MissingResourceException ex) {
			ribbonIcon = null;
		}
		if (ribbonIcon == null && button.getIcon() instanceof org.pushingpixels.flamingo.api.common.icon.ResizableIcon resizableIcon) {
			resizableIcon.setDimension(new Dimension(iconSize, iconSize));
			ribbonIcon = resizableIcon;
		}
		if (ribbonIcon == null) {
			ribbonIcon = button.getIcon();
		}
		if (ribbonIcon != null) {
			button.setIcon(ribbonIcon);
			String iconKey = (String) button.getClientProperty(ICON_KEY_PROPERTY);
			Icon disabledIcon = IconManager.getRibbonIconDisabled(iconKey, iconSize, iconSize);
			// Keep the source icon for rollover and selection.  Replacing it with a
			// separately rasterized/tinted SVG can produce a fully transparent icon
			// with some FlatLaf/SVG rendering combinations, making the command appear
			// to disappear exactly when the pointer enters it.  The button border and
			// background already provide the hover/selected affordance.
			button.setRolloverIcon(ribbonIcon);
			button.setSelectedIcon(ribbonIcon);
			button.setDisabledIcon(disabledIcon == null ? ribbonIcon : disabledIcon);
		}
	}

	private String toLargeButtonText(String text) {
		String plainText = toPlainButtonText(text);
		List<String> lines = splitLargeButtonLines(plainText);
		if (lines.size() <= 1) {
			return plainText;
		}
		return "<html><center>" + String.join("<br>", lines) + "</center></html>";
	}

	private List<String> splitLargeButtonLines(String text) {
		if (text == null || text.isBlank()) {
			return List.of("");
		}
		String normalized = text.trim();
		String[] words = normalized.split("\\s+");
		FontMetrics metrics = new JLabel().getFontMetrics(FlatUiSupport.ribbonButtonFont());
		int maxLineWidth = LARGE_TEXT_WIDTH;
		if (words.length <= 1) {
			return wrapToken(normalized, metrics, maxLineWidth);
		}
		String firstLine = "";
		String secondLine = "";
		for (String word : words) {
			String candidate = firstLine.isEmpty() ? word : firstLine + " " + word;
			if (metrics.stringWidth(candidate) <= maxLineWidth || firstLine.isEmpty()) {
				firstLine = candidate;
			} else {
				secondLine = secondLine.isEmpty() ? word : secondLine + " " + word;
			}
		}
		if (secondLine.isEmpty()) {
			return List.of(firstLine);
		}
		return List.of(firstLine, secondLine);
	}

	private List<String> wrapToken(String text, FontMetrics metrics, int maxLineWidth) {
		if (metrics.stringWidth(text) <= maxLineWidth || text.length() <= 1) {
			return List.of(text);
		}
		List<String> lines = new ArrayList<>(2);
		StringBuilder current = new StringBuilder();
		for (int index = 0; index < text.length(); index++) {
			char ch = text.charAt(index);
			String candidate = current.toString() + ch;
			if (current.length() > 0 && metrics.stringWidth(candidate) > maxLineWidth && lines.isEmpty()) {
				lines.add(current.toString());
				current.setLength(0);
			}
			current.append(ch);
		}
		if (current.length() > 0) {
			lines.add(current.toString());
		}
		return lines.size() > 2 ? List.of(lines.get(0), String.join("", lines.subList(1, lines.size()))) : lines;
	}

	private String toPlainButtonText(String text) {
		if (text == null) {
			return "";
		}
		return text.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim();
	}

	private static final class RibbonButtonMetrics {
		private final String displayText;
		private final Dimension preferredSize;

		private RibbonButtonMetrics(String displayText, Dimension preferredSize) {
			this.displayText = displayText;
			this.preferredSize = preferredSize;
		}

		static RibbonButtonMetrics large(AbstractButton button) {
			RibbonButtonStyler styler = new RibbonButtonStyler();
			String plainText = button.getText() == null ? "" : button.getText();
			String displayText = styler.toLargeButtonText(plainText);
			JLabel probe = new JLabel(displayText);
			probe.setFont(FlatUiSupport.ribbonButtonFont());
			Dimension textSize = probe.getPreferredSize();
			int longLabelMinWidth = plainText.length() >= 7 ? LARGE_LONG_LABEL_WIDTH : FlatUiSupport.ribbonLargeButtonMinWidth();
			int preferredWidth = Math.max(
				longLabelMinWidth,
				Math.max(LARGE_ICON_SIZE + LARGE_HORIZONTAL_PADDING, textSize.width + LARGE_HORIZONTAL_PADDING))
				+ splitButtonExtraWidth(button);
			int preferredHeight = Math.max(
				FlatUiSupport.ribbonLargeButtonHeight(),
				LARGE_ICON_SIZE
					+ LARGE_ICON_TEXT_GAP
					+ textSize.height
					+ FlatUiSupport.ribbonButtonVerticalInset());
			return new RibbonButtonMetrics(displayText, new Dimension(preferredWidth, preferredHeight));
		}

		static RibbonButtonMetrics inline(AbstractButton button, int minWidth, int minHeight, int iconSize) {
			String text = button.getText() == null ? "" : button.getText();
			JLabel probe = new JLabel(text);
			probe.setFont(FlatUiSupport.ribbonButtonFont());
			FontMetrics metrics = probe.getFontMetrics(probe.getFont());
			int preferredWidth = Math.max(
				minWidth,
				iconSize + INLINE_ICON_TEXT_GAP + metrics.stringWidth(text) + INLINE_HORIZONTAL_PADDING)
				+ splitButtonExtraWidth(button);
			int preferredHeight = Math.max(
				minHeight,
				Math.max(iconSize, metrics.getHeight()) + FlatUiSupport.ribbonButtonVerticalInset());
			return new RibbonButtonMetrics(text, new Dimension(preferredWidth, preferredHeight));
		}

		String displayText() {
			return displayText;
		}

		Dimension preferredSize() {
			return preferredSize;
		}

		private static int splitButtonExtraWidth(AbstractButton button) {
			return Boolean.TRUE.equals(button.getClientProperty("ProjectLibre.ribbonSplit"))
				? SPLIT_BUTTON_EXTRA_WIDTH
				: 0;
		}
	}
}
