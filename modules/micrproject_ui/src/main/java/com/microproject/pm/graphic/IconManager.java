/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.microproject.pm.graphic;

import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;

import com.microproject.util.ClassLoaderUtils;

/**
 * 
 */
public class IconManager {
	protected static ClassLoader classLoader=ClassLoaderUtils.getLocalClassLoader();
	protected static HashMap icons = new HashMap();
	protected static HashMap ribbonIcons = new HashMap();
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

	private static BufferedImage scaleToCanvas(BufferedImage source, int width, int height) {
		if (source == null || width <= 0 || height <= 0)
			return null;
		double scale = Math.min((double) width / (double) source.getWidth(), (double) height / (double) source.getHeight());
		int scaledWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
		int scaledHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
		BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = canvas.createGraphics();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			int x = (width - scaledWidth) / 2;
			int y = (height - scaledHeight) / 2;
			g2.drawImage(source, x, y, scaledWidth, scaledHeight, null);
		} finally {
			g2.dispose();
		}
		return canvas;
	}

	private static final class BufferedImageResizableIcon implements ResizableIcon {
		private final BufferedImage originalImage;
		private int width;
		private int height;
		private BufferedImage renderedImage;

		private BufferedImageResizableIcon(BufferedImage image, int width, int height) {
			this.originalImage = image;
			this.width = width;
			this.height = height;
			this.renderedImage = scaleToCanvas(image, width, height);
		}

		@Override
		public synchronized void setDimension(Dimension newDimension) {
			this.width = newDimension.width;
			this.height = newDimension.height;
			this.renderedImage = scaleToCanvas(this.originalImage, this.width, this.height);
		}

		@Override
		public synchronized int getIconWidth() {
			return this.width;
		}

		@Override
		public synchronized int getIconHeight() {
			return this.height;
		}

		@Override
		public synchronized void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
			if (this.renderedImage != null) {
				g.drawImage(this.renderedImage, x, y, null);
			}
		}
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
		ImageIcon icon = (ImageIcon) icons.get(key);
		if (icon == null) {
			String iconName = getIconName(key);
			URL url = iconName == null ? null : getIconResource(iconName);
			if (url == null)
				return null;
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
	public static ResizableIcon getRibbonIcon(String key) {
		return getRibbonIcon(key,48,48);
	}
	public static ResizableIcon getRibbonIcon(String name, int width , int height) {
		String iconName = getConfiguredIconName(name);
		if (iconName == null)
			return null;
		URL preferredUrl = width == height ? getSizeSpecificRibbonResource(iconName, width) : null;
		if (preferredUrl == null)
			preferredUrl = getIconResourceWithExtension(iconName, ".svg");
		URL fallbackUrl = getIconResourceWithExtension(iconName, null);
		BufferedImage image = createRibbonImage(preferredUrl, width, height);
		if (image == null && fallbackUrl != null && !fallbackUrl.equals(preferredUrl))
			image = createRibbonImage(fallbackUrl, width, height);
		if (image == null)
			image = createRibbonImage(fallbackUrl, width, height);
		if (image == null)
			return null;
		return new BufferedImageResizableIcon(image, width, height);
	}

	/** Creates a themed state variant while preserving the source alpha. */
	public static ImageIcon getRibbonIconTinted(String name, int width, int height, Color color) {
		if (name == null || color == null || width <= 0 || height <= 0)
			return null;
		String iconName = getConfiguredIconName(name);
		if (iconName == null)
			return null;
		URL preferredUrl = width == height ? getSizeSpecificRibbonResource(iconName, width) : null;
		if (preferredUrl == null)
			preferredUrl = getIconResourceWithExtension(iconName, ".svg");
		URL fallbackUrl = getIconResourceWithExtension(iconName, null);
		BufferedImage image = createRibbonImage(preferredUrl, width, height);
		if (image == null && fallbackUrl != null && !fallbackUrl.equals(preferredUrl))
			image = createRibbonImage(fallbackUrl, width, height);
		if (image == null)
			return null;
		BufferedImage tinted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int argb = image.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xff;
				if (alpha == 0)
					continue;
				tinted.setRGB(x, y, (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue());
			}
		}
		return new ImageIcon(tinted);
	}

	/** Creates an Office-style disabled variant without replacing enabled SVG colours. */
	public static ImageIcon getRibbonIconDisabled(String name, int width, int height) {
		if (name == null || width <= 0 || height <= 0)
			return null;
		String iconName = getConfiguredIconName(name);
		if (iconName == null)
			return null;
		URL preferredUrl = width == height ? getSizeSpecificRibbonResource(iconName, width) : null;
		if (preferredUrl == null)
			preferredUrl = getIconResourceWithExtension(iconName, ".svg");
		URL fallbackUrl = getIconResourceWithExtension(iconName, null);
		BufferedImage image = createRibbonImage(preferredUrl, width, height);
		if (image == null && fallbackUrl != null && !fallbackUrl.equals(preferredUrl))
			image = createRibbonImage(fallbackUrl, width, height);
		if (image == null)
			return null;
		BufferedImage disabled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int argb = image.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xff;
				if (alpha == 0)
					continue;
				int red = (argb >>> 16) & 0xff;
				int green = (argb >>> 8) & 0xff;
				int blue = argb & 0xff;
				int luminance = (red * 30 + green * 59 + blue * 11) / 100;
				int gray = Math.min(210, 128 + luminance / 3);
				int disabledAlpha = Math.round(alpha * 0.38f);
				disabled.setRGB(x, y, (disabledAlpha << 24) | (gray << 16) | (gray << 8) | gray);
			}
		}
		return new ImageIcon(disabled);
	}

	public static ImageIcon getHalfSizedIcon(String key) {
		ImageIcon icon = getIcon(key);
		Image image = icon.getImage();
		Image half = image.getScaledInstance(icon.getIconWidth()/2, icon.getIconHeight()/2, Image.SCALE_DEFAULT);
		return new ImageIcon(half);
	}
	
	
	public static Image getImage(String key) {
//		System.out.println("getImage: "+key);
		ImageIcon icon=getIcon(key);
		if (icon==null) return null;
		else return icon.getImage();
	}
	
	
	

}

