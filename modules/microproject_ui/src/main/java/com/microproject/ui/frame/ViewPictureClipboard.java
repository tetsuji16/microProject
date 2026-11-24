/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

/** Copy Picture: renders a view as an image and puts it on the system clipboard. */
public final class ViewPictureClipboard {
	/** Refuses to build images beyond this many pixels (about 16k x 16k). */
	static final long MAX_PIXELS = 256L * 1024L * 1024L;

	private ViewPictureClipboard() {
	}

	/** Paints the component as it is currently laid out; {@code null} when it has no size or is too large. */
	public static BufferedImage render(JComponent component) {
		if (component == null)
			return null;
		int width = component.getWidth();
		int height = component.getHeight();
		if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS)
			return null;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, height);
			component.printAll(graphics);
		} finally {
			graphics.dispose();
		}
		return image;
	}

	/** Renders the component and copies the image to the system clipboard; false when nothing was copied. */
	public static boolean copy(JComponent component) {
		BufferedImage image = render(component);
		if (image == null)
			return false;
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(image), null);
		return true;
	}

	/** A clipboard content that carries one image under {@link DataFlavor#imageFlavor}. */
	public static final class ImageTransferable implements Transferable {
		private final BufferedImage image;

		public ImageTransferable(BufferedImage image) {
			this.image = image;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.imageFlavor };
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor))
				throw new UnsupportedFlavorException(flavor);
			return image;
		}
	}
}
