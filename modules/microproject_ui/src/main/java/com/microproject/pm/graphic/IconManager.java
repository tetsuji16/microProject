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
package com.microproject.pm.graphic;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.imgscalr.Scalr;

import com.microproject.util.ClassLoaderUtils;
import com.microproject.util.ColorLuminance;
import com.microproject.util.FlatUiSupport;

/**
 * 
 */
public class IconManager {
	protected static ClassLoader classLoader=ClassLoaderUtils.getLocalClassLoader();
	protected static final HashMap<String, ImageIcon> icons = new HashMap<>(128);
	private static final Map<RibbonIconKey, ImageIcon> ribbonIcons = new ConcurrentHashMap<>(64);
	private record RibbonIconKey(String name, int width, int height) { }

	/**
	 * Monochrome ribbon glyphs whose SVG ink is a fixed light-surface colour, grouped by that
	 * ink. On a dark ribbon those inks have too little contrast, so they are remapped to the
	 * ribbon icon ink; the light theme keeps the intended colour of every asset.
	 */
	private record RibbonIconInk(int sourceInk, Set<String> iconNames) { }
	private static final List<RibbonIconInk> RIBBON_ICON_INKS = List.of(
		// Fluent UI System Icons draw with the light-surface ink #212121.
		new RibbonIconInk(0x212121, Set.of("fluent-info", "fluent-info-16", "fluent-info-20", "fluent-info-32",
			"fluent-grid-dots", "fluent-grid-dots-16", "fluent-grid-dots-20", "fluent-grid-dots-32")),
		// The format glyphs (bold, italic, underline) use the brand navy #1f3b73.
		new RibbonIconInk(0x1F3B73, Set.of("format-bold", "format-bold-16", "format-bold-20", "format-bold-32",
			"format-italic", "format-italic-16", "format-italic-20", "format-italic-32",
			"format-underline", "format-underline-16", "format-underline-20", "format-underline-32")),
		// The alignment glyphs (left, center, right) use the brand green #2f6b53.
		new RibbonIconInk(0x2F6B53, Set.of("align-left-ribbon", "align-left-ribbon-16", "align-left-ribbon-20",
			"align-left-ribbon-32", "align-center-ribbon", "align-center-ribbon-16", "align-center-ribbon-20",
			"align-center-ribbon-32", "align-right-ribbon", "align-right-ribbon-16", "align-right-ribbon-20",
			"align-right-ribbon-32")));

	/** Drops cached icons so theme-dependent rendering is rebuilt after a theme switch. */
	public static void clearCaches() {
		synchronized (icons) {
			icons.clear();
		}
		ribbonIcons.clear();
	}

	public static int cachedIconCount() {
		synchronized (icons) {
			return icons.size() + ribbonIcons.size();
		}
	}
	private final static String[] iconPackages = new String[] {
			"com/microproject/pm/graphic/images/"
			,"com/microproject/pm/graphic/images/big/"
			,"com/microproject/pm/graphic/images/ribbon/"
			,"toolbarButtonGraphics/general/"
			,"toolbarButtonGraphics/text/"			
			,"toolbarButtonGraphics/table/"
			,"toolbarButtonGraphics/navigation/"				
			,"toolbarButtonGraphics/development/"
			,"toolbarButtonGraphics/media/"	
			
	};
	
	private static URL getIconResource(String iconName) {
		URL result = getIconResourceWithExtension(iconName, ".svg");
		if (result != null)
			return result;
		result = getIconResourceWithExtension(iconName, null);
		return result;
	}

	private static URL getIconResourceWithExtension(String iconName, String extension) {
		String candidate = iconName;
		if (extension != null) {
			int dot = iconName.lastIndexOf('.');
			if (dot >= 0)
				candidate = iconName.substring(0, dot) + extension;
			else
				candidate = iconName + extension;
		}
		URL result = null;
		for (int i = 0; i < iconPackages.length; i++) {
			result = classLoader.getResource(iconPackages[i] + candidate);
			if (result != null)
				break;
		}
		return result;
	}

	private static URL getSizeSpecificRibbonResource(String iconName, int size) {
		if (iconName == null || size <= 0)
			return null;
		int dot = iconName.lastIndexOf('.');
		String baseName = dot >= 0 ? iconName.substring(0, dot) : iconName;
		return getIconResourceWithExtension(baseName + "-" + size + ".svg", null);
	}

	private static ImageIcon createImageIcon(URL url) {
		if (url == null)
			return null;
		if (url.getPath() != null && url.getPath().toLowerCase(Locale.ROOT).endsWith(".svg")) {
			try (InputStream in = url.openStream()) {
				FlatSVGIcon svgIcon = new FlatSVGIcon(in);
				if (!svgIcon.hasFound())
					return null;
				int width = svgIcon.getWidth();
				int height = svgIcon.getHeight();
				if (width <= 0)
					width = svgIcon.getIconWidth();
				if (height <= 0)
					height = svgIcon.getIconHeight();
				if (width <= 0)
					width = 16;
				if (height <= 0)
					height = 16;
				if (url.getPath() != null && url.getPath().contains("/images/")) {
					width = 16;
					height = 16;
					svgIcon = svgIcon.derive(width, height);
				}
				BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = image.createGraphics();
				try {
					svgIcon.paintIcon(null, g2, 0, 0);
				} finally {
					g2.dispose();
				}
				return new ImageIcon(image);
			} catch (Throwable ex) {
				// fall back to the raster resource if SVG rendering is unavailable
			}
		}
		return new ImageIcon(url);
	}

	private static ImageIcon createScalableIcon(URL url, int width, int height) {
		if (url == null || url.getPath() == null || !url.getPath().toLowerCase(Locale.ROOT).endsWith(".svg"))
			return null;
		try {
			FlatSVGIcon icon = new FlatSVGIcon(url);
			if (!icon.hasFound())
				return null;
			return icon.derive(width, height);
		} catch (Exception ex) {
			return null;
		}
	}

	private static BufferedImage createRibbonImage(URL url, int width, int height) {
		if (url == null)
			return null;
		if (url.getPath() != null && url.getPath().toLowerCase(Locale.ROOT).endsWith(".svg")) {
			BufferedImage svgImage = createSvgImage(url, width, height);
			if (svgImage != null)
				return svgImage;
		}
		return createRasterImage(url, width, height);
	}

	private static BufferedImage createSvgImage(URL url, int width, int height) {
		try (InputStream in = url.openStream()) {
			FlatSVGIcon svgIcon = new FlatSVGIcon(in);
			if (!svgIcon.hasFound())
				return null;
			FlatSVGIcon sizedIcon = svgIcon.derive(width, height);
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = image.createGraphics();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				sizedIcon.paintIcon(null, g2, 0, 0);
			} finally {
				g2.dispose();
			}
			return image;
		} catch (Throwable ex) {
			return null;
		}
	}

	private static BufferedImage createRasterImage(URL url, int width, int height) {
		try (InputStream in = url.openStream()) {
			BufferedImage source = ImageIO.read(in);
			if (source == null)
				return null;
			return scaleToCanvas(source, width, height);
		} catch (Throwable ex) {
			return null;
		}
	}

	static BufferedImage scaleToCanvas(BufferedImage source, int width, int height) {
		if (source == null || width <= 0 || height <= 0)
			return null;
		double scale = Math.min((double) width / source.getWidth(), (double) height / source.getHeight());
		int scaledWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
		int scaledHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
		BufferedImage resized = Scalr.resize(source, Scalr.Method.QUALITY, scaledWidth, scaledHeight, new BufferedImageOp[0]);
		BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = canvas.createGraphics();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int x = (width - resized.getWidth()) / 2;
			int y = (height - resized.getHeight()) / 2;
			g2.drawImage(resized, x, y, null);
		} finally {
			g2.dispose();
		}
		return canvas;
	}

	private static String getIconName(String key) {
		ResourceBundle bundle = ResourceBundle
				.getBundle("com/microproject/pm/graphic/images",Locale.getDefault(),classLoader);
		return bundle.getString(key);
	}

	public static String getConfiguredIconName(String key) {
		return getIconName(key);
	}

	public static URL resolveIconResource(String key) {
		String iconName = getIconName(key);
		return iconName == null ? null : getIconResource(iconName);
	}

	public static URL resolveIconResourceByName(String iconName) {
		return iconName == null ? null : getIconResource(iconName);
	}

	public static boolean hasSvgResource(String key) {
		String iconName = getIconName(key);
		return iconName != null && getIconResourceWithExtension(iconName, ".svg") != null;
	}

	static boolean hasSizeSpecificRibbonResource(String key, int size) {
		String iconName = getIconName(key);
		return getSizeSpecificRibbonResource(iconName, size) != null;
	}

	public static URL getURL(String key) {
		String iconName = getIconName(key);
		return iconName == null ? null : getIconResource(iconName);
	}
	
	public static ImageIcon getIcon(String key) {
		ImageIcon icon = icons.get(key);
		if (icon == null) {
			String iconName = getIconName(key);
			URL url = iconName == null ? null : getIconResource(iconName);
			if (url == null)
				return null;
			Dimension logicalSize = getSvgLogicalSize(iconName, url);
			icon = createScalableIcon(url, logicalSize.width, logicalSize.height);
			if (icon instanceof FlatSVGIcon svgIcon && isTreeExpanderKey(key))
				svgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(IconManager::treeExpanderInk));
			if (icon == null)
				icon = createImageIcon(url);
			if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
				URL fallbackUrl = getIconResourceWithExtension(iconName, null);
				if (fallbackUrl != null && !fallbackUrl.equals(url))
					icon = createImageIcon(fallbackUrl);
			}
			icons.put(key, icon);
		}
		return icon;
	}

	private static boolean isTreeExpanderKey(String key) {
		return "spreadsheet.collapsed.icon".equals(key) || "spreadsheet.expanded.icon".equals(key);
	}

	/**
	 * The outline expanders are drawn in black. The mapping is evaluated on every
	 * paint, so the cached icon follows theme changes: on a dark table the black
	 * ink is replaced by the table foreground, otherwise it is left untouched.
	 */
	private static Color treeExpanderInk(Color source) {
		return treeExpanderInk(source, FlatUiSupport.tableBackground(), FlatUiSupport.tableForeground());
	}

	static Color treeExpanderInk(Color source, Color tableBackground, Color tableForeground) {
		if (source == null || tableForeground == null || (source.getRGB() & 0xFFFFFF) != 0
				|| !ColorLuminance.isDark(tableBackground))
			return source;
		return new Color(tableForeground.getRed(), tableForeground.getGreen(), tableForeground.getBlue(),
			source.getAlpha());
	}

	private static Dimension getSvgLogicalSize(String iconName, URL iconUrl) {
		if (iconUrl.getPath() == null || !iconUrl.getPath().toLowerCase(Locale.ROOT).endsWith(".svg"))
			return new Dimension(16, 16);

		URL rasterUrl = getIconResourceWithExtension(iconName, null);
		if (rasterUrl == null || rasterUrl.equals(iconUrl)
				|| (rasterUrl.getPath() != null && rasterUrl.getPath().toLowerCase(Locale.ROOT).endsWith(".svg"))) {
			rasterUrl = null;
			for (String extension : new String[] {".png", ".gif", ".jpg", ".jpeg"}) {
				URL candidate = getIconResourceWithExtension(iconName, extension);
				if (candidate != null && !candidate.equals(iconUrl)) {
					rasterUrl = candidate;
					break;
				}
			}
		}
		if (rasterUrl != null) {
			ImageIcon rasterFallback = new ImageIcon(rasterUrl);
			int width = rasterFallback.getIconWidth();
			int height = rasterFallback.getIconHeight();
			if (width > 0 && height > 0 && width <= 16 && height <= 16)
				return new Dimension(width, height);
		}
		return new Dimension(16, 16);
	}
	public static ImageIcon getRibbonIcon(String key) {
		return getRibbonIcon(key,48,48);
	}
	public static ImageIcon getRibbonIcon(String name, int width , int height) {
		if (name == null || width <= 0 || height <= 0)
			return null;
		String iconName = getConfiguredIconName(name);
		if (iconName == null)
			return null;
		RibbonIconKey cacheKey = new RibbonIconKey(iconName, width, height);
		return ribbonIcons.computeIfAbsent(cacheKey, key -> {
			URL preferredUrl = width == height ? getSizeSpecificRibbonResource(iconName, width) : null;
			if (preferredUrl == null)
				preferredUrl = getIconResourceWithExtension(iconName, ".svg");
			URL fallbackUrl = getIconResourceWithExtension(iconName, null);
			ImageIcon vectorIcon = createScalableIcon(preferredUrl, width, height);
			if (vectorIcon != null)
				return applyRibbonInk(vectorIcon, preferredUrl);
			BufferedImage image = createRibbonImage(preferredUrl, width, height);
			if (image == null)
				image = createRibbonImage(fallbackUrl, width, height);
			return image == null ? null : applyRibbonInk(new ImageIcon(image), preferredUrl != null ? preferredUrl : fallbackUrl);
		});
	}

	/** Maps the light-only ink of a monochrome glyph to the dark ribbon icon ink. */
	private static ImageIcon applyRibbonInk(ImageIcon icon, URL resource) {
		int sourceInk = fixedLightInkOf(resource);
		if (!FlatUiSupport.isDarkTheme() || sourceInk < 0)
			return icon;
		// These glyphs are monochrome; keep their neutral ink instead of introducing a hue.
		Color ribbonInk = FlatUiSupport.ribbonIconColor();
		int gray = (int) Math.round(0.2126 * ribbonInk.getRed() + 0.7152 * ribbonInk.getGreen()
			+ 0.0722 * ribbonInk.getBlue());
		Color ink = new Color(gray, gray, gray);
		if (icon instanceof FlatSVGIcon svgIcon) {
			svgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(source -> isInk(source, sourceInk) ? ink : source));
			return svgIcon;
		}
		BufferedImage image = toBufferedImage(icon.getImage());
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if (isInk(new Color(argb), sourceInk))
					image.setRGB(x, y, (argb & 0xFF000000) | (ink.getRGB() & 0xFFFFFF));
			}
		}
		return new ImageIcon(image);
	}

	/** The fixed light-theme ink of the asset behind {@code resource}, or -1 when it keeps its colours. */
	private static int fixedLightInkOf(URL resource) {
		if (resource == null || resource.getPath() == null)
			return -1;
		String path = resource.getPath();
		String name = path.substring(path.lastIndexOf('/') + 1);
		int dot = name.lastIndexOf('.');
		String baseName = dot >= 0 ? name.substring(0, dot) : name;
		for (RibbonIconInk replacement : RIBBON_ICON_INKS) {
			if (replacement.iconNames().contains(baseName))
				return replacement.sourceInk();
		}
		return -1;
	}

	private static boolean isInk(Color color, int ink) {
		return color != null && (color.getRGB() & 0xFFFFFF) == ink;
	}

	/** Creates a themed state variant while preserving the source alpha. */
	public static ImageIcon getRibbonIconTinted(String name, int width, int height, Color color) {
		if (name == null || color == null || width <= 0 || height <= 0)
			return null;
		ImageIcon source = getRibbonIcon(name, width, height);
		if (source instanceof FlatSVGIcon svgIcon)
			return new ScalableRibbonStateIcon(svgIcon, color);
		BufferedImage image = source == null ? null : toBufferedImage(source.getImage());
		if (image == null)
			return null;
		return new ImageIcon(transformRibbonIconState(image, color));
	}

	/** Creates an Office-style disabled variant without replacing enabled SVG colours. */
	public static ImageIcon getRibbonIconDisabled(String name, int width, int height) {
		if (name == null || width <= 0 || height <= 0)
			return null;
		ImageIcon source = getRibbonIcon(name, width, height);
		if (source instanceof FlatSVGIcon svgIcon)
			return new ScalableRibbonStateIcon(svgIcon, null);
		BufferedImage image = source == null ? null : toBufferedImage(source.getImage());
		if (image == null)
			return null;
		return new ImageIcon(transformRibbonIconState(image, null));
	}

	private static BufferedImage transformRibbonIconState(BufferedImage source, Color tint) {
		BufferedImage transformed = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int argb = source.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xff;
				if (alpha == 0)
					continue;
				if (tint != null) {
					transformed.setRGB(x, y, (alpha << 24) | (tint.getRed() << 16) | (tint.getGreen() << 8) | tint.getBlue());
					continue;
				}
				int red = (argb >>> 16) & 0xff;
				int green = (argb >>> 8) & 0xff;
				int blue = argb & 0xff;
				int luminance = (red * 30 + green * 59 + blue * 11) / 100;
				int gray = Math.min(210, 128 + luminance / 3);
				int disabledAlpha = Math.round(alpha * 0.38f);
				transformed.setRGB(x, y, (disabledAlpha << 24) | (gray << 16) | (gray << 8) | gray);
			}
		}
		return transformed;
	}

	private static final class ScalableRibbonStateIcon extends ImageIcon {
		private final FlatSVGIcon source;
		private final Color tint;
		private volatile BufferedImage logicalImage;

		private ScalableRibbonStateIcon(FlatSVGIcon source, Color tint) {
			this.source = source;
			this.tint = tint;
		}

		@Override
		public int getIconWidth() {
			return source.getIconWidth();
		}

		@Override
		public int getIconHeight() {
			return source.getIconHeight();
		}

		@Override
		public Image getImage() {
			BufferedImage image = logicalImage;
			if (image != null)
				return image;
			synchronized (this) {
				image = logicalImage;
				if (image == null) {
					BufferedImage rendered = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
					Graphics2D renderer = rendered.createGraphics();
					try {
						source.paintIcon(null, renderer, 0, 0);
					} finally {
						renderer.dispose();
					}
					logicalImage = image = transformRibbonIconState(rendered, tint);
				}
			}
			return image;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			if (!(graphics instanceof Graphics2D graphics2D)) {
				graphics.drawImage(getImage(), x, y, component);
				return;
			}
			AffineTransform transform = graphics2D.getTransform();
			double scaleX = Math.hypot(transform.getScaleX(), transform.getShearY());
			double scaleY = Math.hypot(transform.getShearX(), transform.getScaleY());
			if (!Double.isFinite(scaleX) || scaleX <= 0d) scaleX = 1d;
			if (!Double.isFinite(scaleY) || scaleY <= 0d) scaleY = 1d;
			int pixelWidth = Math.max(1, (int) Math.ceil(getIconWidth() * scaleX));
			int pixelHeight = Math.max(1, (int) Math.ceil(getIconHeight() * scaleY));
			BufferedImage surface = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D renderer = surface.createGraphics();
			try {
				renderer.setRenderingHints(graphics2D.getRenderingHints());
				renderer.scale((double) pixelWidth / getIconWidth(), (double) pixelHeight / getIconHeight());
				source.paintIcon(component, renderer, 0, 0);
			} finally {
				renderer.dispose();
			}
			graphics2D.drawImage(transformRibbonIconState(surface, tint), x, y, getIconWidth(), getIconHeight(), null);
		}
	}

	private static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage)
			return (BufferedImage) image;
		if (image == null)
			return null;
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		if (width <= 0 || height <= 0)
			return null;
		BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = canvas.createGraphics();
		try {
			g2.drawImage(image, 0, 0, null);
		} finally {
			g2.dispose();
		}
		return canvas;
	}

	public static ImageIcon getHalfSizedIcon(String key) {
		ImageIcon icon = getIcon(key);
		if (icon instanceof FlatSVGIcon svgIcon)
			return svgIcon.derive(Math.max(1, icon.getIconWidth() / 2), Math.max(1, icon.getIconHeight() / 2));
		BufferedImage image = toBufferedImage(icon.getImage());
		if (image == null)
			return null;
		BufferedImage resized = Scalr.resize(image, Scalr.Method.QUALITY,
				Math.max(1, icon.getIconWidth() / 2), Math.max(1, icon.getIconHeight() / 2), new BufferedImageOp[0]);
		return new ImageIcon(resized);
	}
	
	
	public static Image getImage(String key) {
		ImageIcon icon=getIcon(key);
		if (icon==null) return null;
		else return icon.getImage();
	}
	
	
	

}

