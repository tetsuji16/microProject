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
package com.microproject.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;

import com.microproject.strings.Messages;

/**
 * 
 */
public class Money extends BigDecimal {
	private static final long serialVersionUID = -8182666966278921881L;
	
	public static NumberFormat getMoneyFormatInstance() {
		return createCurrencyFormat(false);
	}

	public static NumberFormat getMoneyCompactFormatInstance() {
		return createCurrencyFormat(true);
	}

	private static NumberFormat createCurrencyFormat(boolean compact) {
		NumberFormat format = NumberFormat.getCurrencyInstance();
		format.setGroupingUsed(false);
		if (compact)
			format.setMaximumFractionDigits(0);
		return format;
	}
	
	public static NumberFormat getFormat(boolean compact) {
		return compact ? getMoneyCompactFormatInstance() : getMoneyFormatInstance();
	}
	
	public static Money getInstance(double arg0) {
		return new Money(arg0);
	}

	/**
	 * @param arg0
	 */
	private Money(double arg0) {
		super(Double.toString(arg0));
	}

	/**
	 * @param arg0
	 */
	public Money(String arg0) throws NumberFormatException{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public Money(BigInteger arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public Money(BigInteger arg0, int arg1) {
		super(arg0, arg1);
	}
	
	public double getPrimitiveTypeValue()
	{
		return doubleValue();
	}
	
	public static String formatCurrency(double value,boolean compact){
		if (compact){
			if (value<100) return normalCurrencyFormat(value,Math.floor(value)==value);
			else if (value<10000){ 
				value=Math.floor(value);
				return normalCurrencyFormat(value,true);
			}else if (value<100000){
				value=value/1000;
				return normalCurrencyFormat(value,Math.floor(value)==value)+Messages.getString("Text.thousandsAbbreviation"); //$NON-NLS-1$
			}else if (value<1000000){
				value=value/1000;
				return normalCurrencyFormat(value,true)+Messages.getString("Text.thousandsAbbreviation"); //$NON-NLS-1$
			}else if (value<100000000){
				value=value/1000000;
				return normalCurrencyFormat(value,Math.floor(value)==value)+Messages.getString("Text.millionsAbbreviation"); //$NON-NLS-1$
			}else{
				value=value/1000000;
				return normalCurrencyFormat(value,true)+Messages.getString("Text.millionsAbbreviation"); //$NON-NLS-1$
			}
		}else return normalCurrencyFormat(value, false);
	}
	
	public static String normalCurrencyFormat(double value,boolean compact){
		return compact?Money.getMoneyCompactFormatInstance().format(value):Money.getMoneyFormatInstance().format(value);
	}
}
