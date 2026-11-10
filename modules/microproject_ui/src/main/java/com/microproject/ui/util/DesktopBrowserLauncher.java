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
package com.microproject.ui.util;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.strings.Messages;
import com.microproject.util.BrowserControl.BrowserLauncher;
import com.microproject.util.ExternalLinkPolicy;

/**
 * Opens links with the operating system's default applications. Web and mail
 * addresses open directly; files open only after the user confirms the path.
 */
public final class DesktopBrowserLauncher implements BrowserLauncher {
	private static final Logger logger = Logger.getLogger(DesktopBrowserLauncher.class.getName());
	private static final String ERROR_MESSAGE = "Error attempting to launch web browser";

	/** The operating system calls this launcher makes; replaceable so tests need no desktop. */
	interface Opener {
		void browse(URI uri) throws Exception;

		void open(File file) throws Exception;
	}

	private final Opener opener;
	private final Predicate<String> confirmOpen;
	private final Consumer<String> notifier;
	private final Supplier<File> baseFolder;

	public DesktopBrowserLauncher() {
		this(systemOpener(), DesktopBrowserLauncher::askUser, DesktopBrowserLauncher::tellUser,
				DesktopBrowserLauncher::currentProjectFolder);
	}

	DesktopBrowserLauncher(Opener opener, Predicate<String> confirmOpen, Consumer<String> notifier,
			Supplier<File> baseFolder) {
		this.opener = opener;
		this.confirmOpen = confirmOpen;
		this.notifier = notifier;
		this.baseFolder = baseFolder;
	}

	@Override
	public void displayURL(String url) {
		try {
			ExternalLinkPolicy.Decision decision = ExternalLinkPolicy.classify(url);
			if (decision == ExternalLinkPolicy.Decision.REJECT) {
				rejected(url);
			} else if (decision == ExternalLinkPolicy.Decision.OPEN) {
				opener.browse(new URI(url.strip()));
			} else {
				openFile(url.strip());
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, ERROR_MESSAGE + ": " + url, e);
		}
	}

	@Override
	public void rejected(String url) {
		logger.warning("Refused to open link: " + url);
		notifier.accept(Messages.getStringWithParam("Message.LinkCannotBeOpened", String.valueOf(url)));
	}

	private void openFile(String address) throws Exception {
		File file = resolveFile(address);
		if (file == null)
			return;
		if (!confirmOpen.test(file.getPath()))
			return;
		opener.open(file);
	}

	/** Resolves a link to a file, or tells the user why that is not possible and returns null. */
	private File resolveFile(String address) {
		File file;
		try {
			if (address.regionMatches(true, 0, "file:", 0, 5))
				file = new File(new URI(address));
			else
				file = new File(address);
		} catch (RuntimeException | URISyntaxException e) {
			rejected(address);
			return null;
		}
		if (file.isAbsolute())
			return file;
		File base = baseFolder.get();
		if (base == null) {
			notifier.accept(Messages.getStringWithParam("Message.LinkNoBaseFolder", address));
			return null;
		}
		return new File(base, file.getPath());
	}

	private static Opener systemOpener() {
		return new Opener() {
			@Override
			public void browse(URI uri) throws Exception {
				if (Desktop.isDesktopSupported())
					Desktop.getDesktop().browse(uri);
			}

			@Override
			public void open(File file) throws Exception {
				if (Desktop.isDesktopSupported())
					Desktop.getDesktop().open(file);
			}
		};
	}

	private static File currentProjectFolder() {
		GraphicManager manager = GraphicManager.getInstance();
		DocumentFrame frame = manager == null ? null : manager.getCurrentFrame();
		String fileName = frame == null || frame.getProject() == null ? null : frame.getProject().getFileName();
		if (fileName == null || fileName.isBlank())
			return null;
		return new File(fileName).getAbsoluteFile().getParentFile();
	}

	private static boolean askUser(String path) {
		boolean[] answer = new boolean[1];
		runOnEventThread(() -> answer[0] = JOptionPane.showConfirmDialog(GraphicManager.getFrameInstance(),
				Messages.getStringWithParam("Message.LinkConfirmOpenFile", path),
				Messages.getString("Message.LinkDialogTitle"), JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION);
		return answer[0];
	}

	private static void tellUser(String message) {
		runOnEventThread(() -> JOptionPane.showMessageDialog(GraphicManager.getFrameInstance(), message,
				Messages.getString("Message.LinkDialogTitle"), JOptionPane.WARNING_MESSAGE));
	}

	private static void runOnEventThread(Runnable task) {
		if (GraphicsEnvironment.isHeadless())
			return;
		try {
			if (SwingUtilities.isEventDispatchThread())
				task.run();
			else
				SwingUtilities.invokeAndWait(task);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			logger.log(Level.WARNING, ERROR_MESSAGE, e);
		}
	}
}
