/****************************************************************************\ 
*                                                                            *
*  SNEE (Sensor NEtwork Engine)                                              *
*  http://code.google.com/p/snee                                             *
*  Release 1.0, 24 May 2009, under New BSD License.                          *
*                                                                            *
*  Copyright (c) 2009, University of Manchester                              *
*  All rights reserved.                                                      *
*                                                                            *
*  Redistribution and use in source and binary forms, with or without        *
*  modification, are permitted provided that the following conditions are    *
*  met: Redistributions of source code must retain the above copyright       *
*  notice, this list of conditions and the following disclaimer.             *
*  Redistributions in binary form must reproduce the above copyright notice, *
*  this list of conditions and the following disclaimer in the documentation *
*  and/or other materials provided with the distribution.                    *
*  Neither the name of the University of Manchester nor the names of its     *
*  contributors may be used to endorse or promote products derived from this *
*  software without specific prior written permission.                       *
*                                                                            *
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS   *
*  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, *
*  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR    *
*  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR          *
*  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,     *
*  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,       *
*  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR        *
*  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
*  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING      *
*  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS        *
*  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.              *
*                                                                            *
\****************************************************************************/
package uk.ac.manchester.cs.diasmc.querycompiler.metadata.units;

import java.io.FileNotFoundException;
import java.util.TreeMap;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.ac.manchester.cs.diasmc.common.Utils;
import uk.ac.manchester.cs.diasmc.querycompiler.Settings;

// This class is based on the Bill Pugh singleton design pattern, http://en.wikipedia.org/wiki/Singleton_pattern
public class Units {

    TreeMap<String, Long> timeScalingFactors;

    TreeMap<String, Long> memoryScalingFactors;

    TreeMap<String, Long> energyScalingFactors;

    private Units() {

	this.timeScalingFactors = new TreeMap<String, Long>();
	this.energyScalingFactors = new TreeMap<String, Long>();
	this.memoryScalingFactors = new TreeMap<String, Long>();

	this.parseXMLFile();
    }

    private void parseXMLFile() {
	try {
	    Utils.validateXMLFile(Settings.INPUTS_USER_UNITS_FILE,
		    "input/units.xsd");

	    // parse file
	    final String timeBaseUnit = Utils.doXPathStrQuery(
		    Settings.INPUTS_USER_UNITS_FILE,
		    "/snee:units/snee:time/snee:base-unit");
	    this.setScalingFactor(timeBaseUnit.toUpperCase(), new Long(1),
		    this.timeScalingFactors);

	    final String energyBaseUnit = Utils.doXPathStrQuery(
		    Settings.INPUTS_USER_UNITS_FILE,
		    "/snee:units/snee:energy/snee:base-unit");
	    this.setScalingFactor(energyBaseUnit.toUpperCase(), new Long(1),
		    this.energyScalingFactors);

	    final String memoryBaseUnit = Utils.doXPathStrQuery(
		    Settings.INPUTS_USER_UNITS_FILE,
		    "/snee:units/snee:memory/snee:base-unit");
	    this.setScalingFactor(memoryBaseUnit.toUpperCase(), new Long(1),
		    this.memoryScalingFactors);

	    this.setScalingFactors("/snee:units/snee:time/snee:unit",
		    this.timeScalingFactors);
	    this.setScalingFactors("/snee:units/snee:energy/snee:unit",
		    this.energyScalingFactors);
	    this.setScalingFactors("/snee:units/snee:memory/snee:unit",
		    this.memoryScalingFactors);

		} catch (final Exception e) {
			Utils.handleCriticalException(e);
		}
    }

    private void setScalingFactors(final String context,
	    final TreeMap<String, Long> unitScalingFactors)
	    throws XPathExpressionException, FileNotFoundException {
	final NodeList nodeList = Utils.doXPathQuery(
		Settings.INPUTS_USER_UNITS_FILE, context);
	for (int i = 0; i < nodeList.getLength(); i++) {
	    final Node node = nodeList.item(i);
	    final String unitName = Utils.doXPathStrQuery(node, "snee:name");
	    final Long scalingFactor = new Long(Utils.doXPathStrQuery(node,
		    "snee:scaling-factor"));
	    this.setScalingFactor(unitName, scalingFactor, unitScalingFactors);
	}
    }

    private void setScalingFactor(final String unitName,
	    final Long scalingFactor,
	    final TreeMap<String, Long> unitScalingFactors) {
	//Add two versions, one plural, one singular, because users will get it wrong!
	unitScalingFactors.put(unitName, scalingFactor);
	if (unitName.toUpperCase().endsWith("S")) {
	    unitScalingFactors.put(unitName.toUpperCase().substring(0,
		    unitName.length() - 1), scalingFactor);
	} else {
	    unitScalingFactors.put(unitName.toUpperCase() + "S", scalingFactor);
	}
    }

    private static class UnitsHolder {
	private final static Units instance = new Units();
    }

    public static Units getInstance() {
	return UnitsHolder.instance;
    }

    public long getTimeScalingFactor(final String unitName)
	    throws UnrecognizedUnitException {
	if (this.timeScalingFactors.containsKey(unitName.toUpperCase())) {
	    return this.timeScalingFactors.get(unitName.toUpperCase())
		    .longValue();
	} else {
	    throw new UnrecognizedUnitException(unitName
		    + " not a recognized time unit");
	}
    }

    public long getEnergyScalingFactor(final String unitName)
	    throws UnrecognizedUnitException {
	if (this.energyScalingFactors.containsKey(unitName.toUpperCase())) {
	    return this.energyScalingFactors.get(unitName.toUpperCase())
		    .longValue();
	} else {
	    throw new UnrecognizedUnitException(unitName
		    + " not a recognized energy unit");
	}
    }

    public long getMemoryScalingFactor(final String unitName)
	    throws UnrecognizedUnitException {
	if (this.memoryScalingFactors.containsKey(unitName.toUpperCase())) {
	    return this.memoryScalingFactors.get(unitName.toUpperCase())
		    .longValue();
	} else {
	    throw new UnrecognizedUnitException(unitName
		    + " not a recognized memory unit");
	}
    }

    public long getScalingFactor(final String unitName)
	    throws UnrecognizedUnitException {
    
    // some values don't have units, e.g., buffering factor
    if (unitName == null || unitName.equals("null")) {
    	return 1;
    }
    	
	if (this.timeScalingFactors.containsKey(unitName.toUpperCase())) {
	    return this.timeScalingFactors.get(unitName.toUpperCase())
		    .longValue();
	} else if (this.energyScalingFactors
		.containsKey(unitName.toUpperCase())) {
	    return this.energyScalingFactors.get(unitName.toUpperCase())
		    .longValue();
	} else if (this.memoryScalingFactors
		.containsKey(unitName.toUpperCase())) {
	    return this.memoryScalingFactors.get(unitName.toUpperCase())
		    .longValue();
	} else {
	    throw new UnrecognizedUnitException(unitName
		    + " not a recognized memory unit");
	}
    }
}
