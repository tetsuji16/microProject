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
package com.microproject.util;

import java.awt.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public class BrowserControl {
	private static final Logger logger = Logger.getLogger(BrowserControl.class.getName());

	private static final String errMsg = "Error attempting to launch web browser";
	public static void displayURL(String url) {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop desktop = Desktop.getDesktop();
				desktop.browse(new URI(url));
			} catch (Exception e) {
				logger.log(Level.WARNING, errMsg + ": " + url, e);
			}
		}
	}


//	public static void displayURL(String url) {
//		System.out.println("Showing help: " + url);
//		String osName = System.getProperty("os.name");
//		try {
//			if (osName.startsWith("Mac OS")) {
//				Class fileMgr = Class.forName("com.apple.eio.FileManager");
//				Method openURL = fileMgr.getDeclaredMethod("openURL",
//						new Class[] { String.class });
//				openURL.invoke(null, new Object[] { url });
//			} else if (osName.startsWith("Windows"))
//				Runtime.getRuntime().exec(
//						"rundll32 url.dll,FileProtocolHandler " + url);
//			else { // assume Unix or Linux
//				String[] browsers = { "firefox", "opera", "konqueror",
//						"epiphany", "mozilla", "netscape" };
//				String browser = null;
//				for (int count = 0; count < browsers.length && browser == null; count++)
//					if (Runtime.getRuntime().exec(
//							new String[] { "which", browsers[count] })
//							.waitFor() == 0)
//						browser = browsers[count];
//				if (browser == null)
//					throw new Exception("Could not find web browser");
//				else
//					Runtime.getRuntime().exec(
//							new String[] { browser, "-new-tab", url });
//			}
//			// Debian: /etc/alternatives/x-www-browser
//			// RedHat: /usr/bin/mozilla or /usr/bin/firefox
//
//			// /usr/share/applications/defaults.list (text/html key)
//			// /usr/share/applications/<text/html key> (Exec)
//			// info in /usr/share/mime/text/html.xml
//			// extension in /usr/share/mime/globs
//			// mimeinfo.cache
//
//			// /usr/share/mime/packages/projectlibre.xml update-mime-database
//			// /usr/share/mime
//		} catch (Exception e) {
//			JOptionPane.showMessageDialog(null, errMsg + ":\n"
//					+ e.getLocalizedMessage());
//		}
//	}

}
