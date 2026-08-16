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
/*
 * file:       SearchableInputStream.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2017
 * date:       24/04/2017
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package com.microproject.exchange.xlsx;

import java.io.IOException;
import java.io.InputStream;

/**
 * Search through the input stream until the pattern is found, the acts as a normal input stream from that point.
 */
public class SearchableInputStream extends InputStream
{
   /**
    * Constructor.
    *
    * @param stream original input stream
    * @param pattern pattern to locate
    */
   public SearchableInputStream(InputStream stream, String pattern)
   {
      m_stream = stream;
      m_pattern = pattern.getBytes();
   }

   /**
    * {@inheritDoc}
    */
   @Override public int read() throws IOException
   {
      int c;

      if (m_searching)
      {
         int index = 0;
         c = -1;
         while (m_searching)
         {
            c = m_stream.read();
            if (c == -1)
            {
               throw new IOException("Pattern not found");
            }

            if (c == m_pattern[index])
            {
               ++index;
               if (index == m_pattern.length)
               {
                  m_searching = false;
                  c = m_stream.read();
               }
            }
            else
            {
               index = 0;
            }
         }
      }
      else
      {
         c = m_stream.read();
      }

      return c;
   }

   private final InputStream m_stream;
   private final byte[] m_pattern;
   private boolean m_searching = true;
}
