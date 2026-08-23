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
package com.microproject.session;

import java.util.Locale;

public class FileHelper {
	public static final String POD_FILE_EXTENSION = "pod";
	public static final String MPO_FILE_EXTENSION = "mpo";
	public static final String DEFAULT_FILE_EXTENSION = MPO_FILE_EXTENSION;
	public static final int PROJECTLIBRE_FILE_TYPE=1;
	public static final int MPO_FILE_TYPE=2;
	public static final int MSP_FILE_TYPE=101;

	private static boolean hasExtension(String fileName, String extension) {
		if (fileName == null || extension == null) {
			return false;
		}
		String normalized = fileName.toLowerCase(Locale.ROOT);
		String suffix = "." + extension.toLowerCase(Locale.ROOT);
		return normalized.endsWith(suffix);
	}

	public static boolean isProjectLibreFile(String fileName) {
		return hasExtension(fileName, POD_FILE_EXTENSION);
	}

	/**
	 * Returns whether the name identifies the open mpo container format.
	 */
	public static boolean isMpoFile(String fileName) {
		return hasExtension(fileName, MPO_FILE_EXTENSION);
	}

	public static boolean isNativeFile(String fileName) {
		return isProjectLibreFile(fileName) || isMpoFile(fileName);
	}

	public static boolean isMicrosoftProjectFile(String fileName) {
		return hasExtension(fileName, "xml")
			|| hasExtension(fileName, "xlsx")
			|| hasExtension(fileName, "mpp")
			|| hasExtension(fileName, "mpx")
			|| hasExtension(fileName, "planner");
	}

    public static boolean isFileNameAllowed(String fileName,boolean save) {
    	if (save) {
			return hasExtension(fileName, "xml") || hasExtension(fileName, "xlsx") || isNativeFile(fileName);
		}
		return isMicrosoftProjectFile(fileName) || isNativeFile(fileName);
	}

    public static String getFileExtension(String fileName) {
		if (fileName == null) return null;
        int i=fileName.lastIndexOf('.');
		if (i>0&&i<fileName.length()-1) return fileName.substring(i+1).toLowerCase(Locale.ROOT);
        return null;
    }
    public static String changeFileExtension(String fileName,int fileType) {
    	return changeFileExtension(fileName, getFileExtension(fileType));
    }
    public static String changeFileExtension(String fileName,String extension) {
    	if( fileName==null) return null;
        int i=fileName.lastIndexOf('.');
        if (i<=0) return fileName+"."+extension;
        else return fileName.substring(0,i)+"."+extension;
    }

    public static String getFileExtension(int fileType){
    	switch (fileType) {
		//case FileHelper.SERVER_FILE_TYPE: return null;
		case FileHelper.PROJECTLIBRE_FILE_TYPE: return POD_FILE_EXTENSION;
		case FileHelper.MPO_FILE_TYPE: return MPO_FILE_EXTENSION;
		case FileHelper.MSP_FILE_TYPE: return "xml";
		default:
			return DEFAULT_FILE_EXTENSION;
		}
    }

    public static int getFileType(String fileName){
    	if (fileName==null) return 0;
		if (isProjectLibreFile(fileName))
			return PROJECTLIBRE_FILE_TYPE;
		if (isMpoFile(fileName))
			return MPO_FILE_TYPE;
    	if (isMicrosoftProjectFile(fileName))
    			return MSP_FILE_TYPE;
    	return 0;
    }
    
}
