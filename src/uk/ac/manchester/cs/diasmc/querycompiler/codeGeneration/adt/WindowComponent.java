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
import java.util.Iterator;
import java.util.LinkedHashMap;

import uk.ac.manchester.cs.diasmc.querycompiler.Settings;
import uk.ac.manchester.cs.diasmc.querycompiler.codeGeneration.NesCGeneration;
import uk.ac.manchester.cs.diasmc.querycompiler.metadata.schema.AttributeType;
import uk.ac.manchester.cs.diasmc.querycompiler.metadata.sensornet.Site;
import uk.ac.manchester.cs.diasmc.querycompiler.qos.QoSSpec;
import uk.ac.manchester.cs.diasmc.querycompiler.queryplan.Fragment;
import uk.ac.manchester.cs.diasmc.querycompiler.queryplan.QueryPlan;
import uk.ac.manchester.cs.diasmc.querycompiler.queryplan.expressions.DataAttribute;
import uk.ac.manchester.cs.diasmc.querycompiler.queryplan.expressions.Expression;
import uk.ac.manchester.cs.diasmc.querycompiler.queryplan.operators.WindowOperator;

public class WindowComponent extends NesCComponent implements
	TinyOS1Component, TinyOS2Component {

    WindowOperator op;

    QueryPlan plan;

    QoSSpec qos;

    Fragment frag;

    public WindowComponent(final WindowOperator op,
    		final QueryPlan plan, final QoSSpec qos,
    		final NesCConfiguration fragConfig,
    		int tosVersion, boolean tossimFlag) {
    	super(fragConfig, tosVersion, tossimFlag);
    	this.op = op;
    	this.frag = op.getContainingFragment();
    	this.plan = plan;
    	this.qos = qos;
    	this.id = CodeGenUtils.generateOperatorInstanceName(op, this.frag,
    			this.site, tosVersion);
	}

	@Override
	public String toString() {
		return this.getID();
	}

	@Override
	public void writeNesCFile(final String outputDir)
		throws IOException, CodeGenerationException {

		final HashMap<String, String> replacements = new HashMap<String, String>();
		replacements.put("__MODULE_NAME__", this.getID());
		replacements.put("__OPERATOR_DESCRIPTION__", this.op.getText(false)
				.replace("\"", ""));
		replacements.put("__HEADER__", this.configuration
				.generateModuleHeader(this.getID()));
		replacements.put("__OUTPUT_TUPLE_TYPE__", CodeGenUtils
				.generateOutputTupleType(this.op));
		String outQueueCard = new Long(
				op.getOutputQueueCardinality(
						(Site) this.plan.getRoutingTree().getNode(
						this.site.getID()), this.plan.getDAF())).toString();
		replacements.put("__OUT_QUEUE_CARD__",outQueueCard); 		
		replacements.put("__CHILD_TUPLE_PTR_TYPE__", CodeGenUtils
				.generateOutputTuplePtrType(this.op.getInput(0)));
		//Window from and to are expressed as positive number is nesc.
		//This allows for unsigned variables to be used.
		replacements.put("__WINDOW_FROM__", new Integer(-this.op.getFrom())
			.toString()); 
		replacements.put("__WINDOW_TO__", new Integer(-this.op.getTo())
			.toString()); 
		replacements.put("__EVALUATION_INTERVAL__", new Integer((int) this.plan
				.getAgenda().getAcquisitionInterval_ms()).toString());
		
		if (op.isTimeScope()) {
			int timeslide = this.op.getTimeSlide();
			if (timeslide == 0) {
				timeslide = 1;
			}
			replacements.put("__SLIDE__", new Integer((int) timeslide)
				.toString()); 
			if (op.getRowSlide() > 0) {
				throw new CodeGenerationException(
					"Row slide in window with time scope not yet implemented");
			}
		} else {
			replacements.put("__SLIDE__", 
					new Integer((int) this.op.getRowSlide()).toString()); 
			if (op.getTimeSlide() > 0) {
				throw new CodeGenerationException(
					"Time slide in window with row scope not yet implemented");
			}
		}
		if (Settings.NESC_MAX_DEBUG_STATEMENTS_IN_TIMEWINDOW) {
			replacements.put("__MAX_DEBUG__ ", "");
		} else {
			replacements.put("__MAX_DEBUG__ ", "//");
		}
	
		final StringBuffer tupleConstructionBuff 
			= CodeGenUtils.generateTupleConstruction(op, false);
		replacements.put("__CONSTRUCT_TUPLE__", tupleConstructionBuff
				.toString());
		final String outputFileName = generateNesCOutputFileName(outputDir, this.getID());
		if (op.isTimeScope()) {
			writeNesCFile(NesCGeneration.NESC_MODULES_DIR + "/timeWindow.nc",
				outputFileName, replacements);
		} else {
			writeNesCFile(NesCGeneration.NESC_MODULES_DIR + "/rowWindow.nc",
				outputFileName, replacements);
		}
	}
}
