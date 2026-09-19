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
package com.microproject.print;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.pm.task.Project;
import com.microproject.util.SafeObjectInput;
import com.microproject.workspace.SavableToWorkspace;

public class PrintSettingsManager {
	private static final Logger logger = Logger.getLogger(PrintSettingsManager.class.getName());
	protected static PrintSettings tmpLocalSettings;
	public static PrintSettings getSettings(Project project) {
		if (project==null){//local
			if (tmpLocalSettings==null){
				byte[] buf=Preferences.userNodeForPackage(PrintSettings.class).getByteArray("printSettings",null);
				if (buf!=null){
					try (var in = SafeObjectInput.create(new ByteArrayInputStream(buf))) {
						tmpLocalSettings=(PrintSettings)in.readObject();
					} catch (Exception e) {
						logger.log(Level.WARNING, "Failed to load persisted print settings", e);
					}
				}
				if (tmpLocalSettings==null){
					tmpLocalSettings=new PrintSettings();
					tmpLocalSettings.setEmpty(true);
				}
			}
			return tmpLocalSettings;
		}else{
			PrintSettings printSettings=project.getPrintSettings(SavableToWorkspace.VIEW);
			if (printSettings==null){
				printSettings=new PrintSettings();
				printSettings.setEmpty(true);
				project.setTmpSettings(printSettings);
			}
			return printSettings;
		}
	}
	public static void saveSettings(PrintSettings printSettings,Project project,boolean persist) {
		if (project==null){//local
			if (printSettings!=null){
					printSettings.spreadsheetWorkspace=null; //don't save field array and sizes
					tmpLocalSettings=printSettings;
					if (persist){
					try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
							ObjectOutputStream out = new ObjectOutputStream(buf)) {
						out.writeObject(printSettings);
						out.flush();
						Preferences.userNodeForPackage(PrintSettings.class).putByteArray("printSettings",buf.toByteArray());
					} catch (Exception e) {
						logger.log(Level.WARNING, "Failed to persist print settings", e);
					}
			}
			}

		}else{
			project.setPrintSettings(printSettings);
		}
	}
}
