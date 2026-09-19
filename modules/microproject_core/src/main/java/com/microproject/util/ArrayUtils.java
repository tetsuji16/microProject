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

import java.util.Arrays;
import java.util.function.Consumer;


/**
 * Utility methods for working with arrays.
 * 
 */
public abstract class ArrayUtils {
    
    /**
     * Clones a two dimensional array of floats.
     * 
     * @param array  the array.
     * 
     * @return A clone of the array.
     */
    public static double[][] clone(final double[][] array) {
    
        if (array == null) {
            return null;
        }
        final double[][] result = new double[array.length][];
        System.arraycopy(array, 0, result, 0, array.length);

        for (int i = 0; i < array.length; i++) {
            final double[] child = array[i];
            final double[] copychild = new double[child.length];
            System.arraycopy(child, 0, copychild, 0, child.length);
            result[i] = copychild;
        }

        return result;
    
    }
    public static double[][][] clone(final double[][][] array) { //works only for arrays of n*2 matrix
        
            if (array == null) {
                return null;
            }
            final double[][][] result = new double[array.length][2][];
            System.arraycopy(array, 0, result, 0, array.length);

            for (int i = 0; i < array.length; i++) {
                final double[] child0 = array[i][0];
                final double[] child1 = array[i][1];
                final double[] copychild0 = new double[child0.length];
                final double[] copychild1 = new double[child1.length];
                System.arraycopy(child0, 0, copychild0, 0, child0.length);
                System.arraycopy(child1, 0, copychild1, 0, child1.length);
                result[i] = new double[2][];
                result[i][0] = copychild0;
                result[i][1] = copychild1;
            }

            return result;
        
     }
    
    /**
     * Tests two double arrays for equality.
     * 
     * @param array1  the first array.
     * @param array2  the second arrray.
     * 
     * @return A boolean.
     */
    public static boolean equal(final double[][] array1, final double[][] array2) {
        if (array1 == null) {
            return (array2 == null);
        }

        if (array2 == null) {
            return false;
        }

        if (array1.length != array2.length) {
            return false;
        }

        for (int i = 0; i < array1.length; i++) {
            if (!Arrays.equals(array1[i], array2[i])) {
                return false;
            }
        }
        return true;
    }
    public static void forAllDo(Object[] array, Consumer<Object> c) {
    	for (int i = 0; i < array.length; i++) {
    		c.accept(array[i]);
    	}
    }
}
