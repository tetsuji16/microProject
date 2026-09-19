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
package com.microproject.dialog.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

// taken from http://www.exampledepot.com/egs/javax.swing.text/LimitText.html with slight modif for null str
public class FixedSizeFilter extends DocumentFilter {
    int maxSize;

    // limit is the maximum number of characters allowed.
    public FixedSizeFilter(int limit) {
        maxSize = limit;
    }

    // This method is called when characters are inserted into the document
    public void insertString(DocumentFilter.FilterBypass fb, int offset, String str,
            AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, str, attr);
    }

    // This method is called when characters in the document are replace with other characters
    public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
            String str, AttributeSet attrs) throws BadLocationException {
        int newLength = fb.getDocument().getLength()-length+ (str == null ? 0 : str.length());
    	int overflow = newLength - maxSize;
    	if (overflow > 0) {
    		str = str.substring(0,str.length() - overflow);
            newLength = fb.getDocument().getLength()-length+ (str == null ? 0 : str.length());
    	}
        if (newLength <= maxSize) {
            fb.replace(offset, length, str, attrs);
        } else {
            throw new BadLocationException("New characters exceeds max size of document", offset);
        }
    }
}

