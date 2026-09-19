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
package com.microproject.strings;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.preference.ConfigurationFile;

public class DirectoryClassLoader extends ClassLoader{
    private static final Logger logger = Logger.getLogger(DirectoryClassLoader.class.getName());

    protected File directory;
    public DirectoryClassLoader(){
    	directory=ConfigurationFile.getConfDir();
    }
    public DirectoryClassLoader(File directory){
        this.directory = directory;
		if (directory!=null){
			if (!directory.isDirectory()) directory=null;
		}
    }

    public boolean isValid(){
    	return directory!=null;
    }

    protected Class findClass(String name) throws ClassNotFoundException{
    	if (directory==null) throw new ClassNotFoundException(name);
        File file = new File(directory, name + ".properties");
        if (!file.isFile()) {
            throw new ClassNotFoundException(name);
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
			byte b[] = new byte[(int) file.length()];
			in.readFully(b);
			return defineClass(name, b, 0, b.length);
		} catch (IOException e) {
			logger.log(Level.FINE, "Failed to load class {0} from {1}", new Object[]{name, file});
			throw new ClassNotFoundException(name, e);
		}
    }

	public InputStream getResourceAsStream(String name) {
    	if (directory==null) return null;
        try {
			File file = new File(directory, name + "_" + Locale.getDefault() + ".properties");
			if (!file.isFile()) {
				return null;
			}
			return new FileInputStream(file);
		} catch (IOException e) {
			logger.log(Level.FINE, "Failed to load resource {0}", name);
			return null;
		}
	}
}
