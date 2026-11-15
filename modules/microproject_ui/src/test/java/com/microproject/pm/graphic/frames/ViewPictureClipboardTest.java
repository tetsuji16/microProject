/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class ViewPictureClipboardTest {
	@Test
	void rendersTheComponentAsAnImageOfItsSize() {
		JPanel panel = new JPanel();
		panel.setBackground(Color.RED);
		panel.setSize(30, 20);
		BufferedImage image = ViewPictureClipboard.render(panel);
		assertEquals(30, image.getWidth());
		assertEquals(20, image.getHeight());
		assertEquals(Color.RED.getRGB(), image.getRGB(5, 5));
	}

	@Test
	void aComponentWithoutSizeHasNoPicture() {
		assertNull(ViewPictureClipboard.render(new JPanel()));
		assertNull(ViewPictureClipboard.render(null));
	}

	@Test
	void theClipboardContentCarriesOnlyAnImageFlavor() throws Exception {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		var transferable = new ViewPictureClipboard.ImageTransferable(image);
		assertTrue(transferable.isDataFlavorSupported(DataFlavor.imageFlavor));
		assertSame(image, transferable.getTransferData(DataFlavor.imageFlavor));
		assertThrows(UnsupportedFlavorException.class, () -> transferable.getTransferData(DataFlavor.stringFlavor));
	}
}
