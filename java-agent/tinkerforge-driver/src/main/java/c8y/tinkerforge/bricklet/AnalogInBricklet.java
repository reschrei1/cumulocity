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

package c8y.tinkerforge.bricklet;

import java.math.BigDecimal;

import c8y.PowerSensor;
import c8y.VoltageMeasurement;

import com.tinkerforge.BrickletAnalogIn;
import com.tinkerforge.Device;

public class AnalogInBricklet extends BaseSensorBricklet {

	private VoltageMeasurement voltage = new VoltageMeasurement();

	public AnalogInBricklet(String id, Device device) {
		super(id, device, "AnalogInVoltage", new PowerSensor());
	}

	@Override
	public void initialize() throws Exception {
		// Nothing to be done here.

	}

	@Override
	public void run() {
		try {
			BrickletAnalogIn aib = (BrickletAnalogIn) getDevice();
			voltage.setVoltageValue(new BigDecimal((double) aib.getVoltage() / 1000));
			super.sendMeasurement(voltage);
		} catch (Exception x) {
			logger.warn("Cannot read measurments from bricklet", x);
		}
	}

}
