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
package uk.ac.manchester.cs.diasmc.querycompiler.codeGeneration.adt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import uk.ac.manchester.cs.diasmc.querycompiler.Settings;
import uk.ac.manchester.cs.diasmc.querycompiler.codeGeneration.NesCGeneration;
import uk.ac.manchester.cs.diasmc.querycompiler.metadata.sensornet.Site;
import uk.ac.manchester.cs.diasmc.querycompiler.qos.QoSSpec;
import uk.ac.manchester.cs.diasmc.querycompiler.queryplan.ExchangePart;
import uk.ac.manchester.cs.diasmc.querycompiler.queryplan.Fragment;
import uk.ac.manchester.cs.diasmc.querycompiler.queryplan.QueryPlan;

public class RXT1Component extends NesCComponent implements TinyOS1Component {

    Site childSite;

    Site currentSite;

    QueryPlan plan;

    QoSSpec qos;

    ArrayList<ExchangePart> exchComponents = new ArrayList<ExchangePart>();

    private final String templateFileName = NesCGeneration.NESC_MODULES_DIR
	    + "/rx.nc";

    public RXT1Component(final Site childSite, final Site currentSite,
	    final NesCConfiguration config, final QueryPlan plan,
	    final QoSSpec qos, boolean tossimFlag) {
	super(config, 1, tossimFlag);
	this.childSite = childSite;
	this.currentSite = currentSite;
	this.id = this.generateName();
	this.plan = plan;
	this.qos = qos;
    }

    @Override
    public String toString() {
	return this.getID();
    }

    private String generateName() {
	return "rx_n" + this.childSite.getID() + "_n"
		+ this.currentSite.getID() + "M";
    }

    public void addExchangeComponent(final ExchangePart exchComp) {
	boolean found = false;

	Fragment exchCompType = CodeGenUtils.getFragType(exchComp.getSourceFrag());
	
	for (int i = 0; i < this.exchComponents.size(); i++) {
		ExchangePart currentExchComp = this.exchComponents.get(i);
		Fragment currentExchCompType = CodeGenUtils.getFragType(currentExchComp.getSourceFrag());
		
	    if ((exchCompType == currentExchCompType)
		    && (currentExchComp.getDestFrag() == exchComp.getDestFrag())
		    && (currentExchComp.getDestSite() == exchComp.getDestSite())) {
		found = true;
		break;
	    }

	}

	if (!found) { //we don't want more than on exchange component with same dest site/frag
	    this.exchComponents.add(exchComp);
	}
    }

    @Override
    public void writeNesCFile(final String outputDir)
	    throws IOException, CodeGenerationException {

	final HashMap<String, String> replacements = new HashMap<String, String>();

	replacements.put("__MODULE_NAME__", this.getID());
	replacements.put("__HEADER__", this.configuration
		.generateModuleHeader(this.getID()));

	final StringBuffer rxMethodsBuff = new StringBuffer();
	for (int i = 0; i < this.exchComponents.size(); i++) {
	    final ExchangePart exchComp = this.exchComponents.get(i);
	    final Fragment sourceFrag = exchComp.getSourceFrag();
	    final Fragment destFrag = exchComp.getDestFrag();
	    final Site destSite = exchComp.getDestSite(); //nb: final destination, not next hop

	    final String receiveInterface = CodeGenUtils
		    .generateUserReceiveInterfaceName(sourceFrag, destFrag,
			    destSite.getID());
	    final String messageTypePtr = CodeGenUtils
		    .generateMessagePtrType(sourceFrag);
	    final String trayPutInterface = CodeGenUtils
		    .generatePutTuplesInterfaceInstanceName(sourceFrag);

	    final HashMap<String, String> rxMethodsReplacements = new HashMap<String, String>();
	    rxMethodsReplacements
		    .put("__RECEIVE_INTERFACE__", receiveInterface);
	    rxMethodsReplacements.put("__TUPLE_MESSAGE_PTR_TYPE__",
		    messageTypePtr);
	    rxMethodsReplacements.put("__TRAY_PUT_INTERFACE__",
		    trayPutInterface);
	    rxMethodsBuff.append(generateNesCMethods(NesCGeneration.NESC_MODULES_DIR
		    + "/rx_methods.nc", rxMethodsReplacements));
	}

	replacements.put("__RX_METHODS__", rxMethodsBuff.toString());

	final String siteID = this.currentSite.getID();
	final String outputFileName = generateNesCOutputFileName(outputDir, this.getID());

	super
		.writeNesCFile(this.templateFileName, outputFileName,
			replacements);
    }

}
