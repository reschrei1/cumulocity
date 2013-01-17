/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * Copyright 2012 Cumulocity GmbH 
 */

package com.cumulocity.kontron;

import java.util.Date;

public class AccelerometerXYZReading
{
	private double x = 0 ;
	private double y = 0 ;
	private double z = 0 ;
	private Date date = null ;
	
	public AccelerometerXYZReading (Date date, double x, double y, double z)
	{
		this.x = x ;
		this.y = y ;
		this.z = z ;
		this.date = date ;
	}

	public double getX () { return x ; } 
	public double getY () { return y ; } 
	public double getZ () { return z ; }
	public Date getDate () { return date ; }

	public String toString ()
	{
		return date.toString() + " " + x + " " + y + " " + z ;
	}

	
}
