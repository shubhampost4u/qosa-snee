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
package uk.ac.manchester.cs.diasmc.querycompiler.queryplan;

import uk.ac.manchester.cs.diasmc.querycompiler.metadata.sensornet.Site;
import uk.ac.manchester.cs.diasmc.querycompiler.queryplan.operators.DeliverOperator;

/**
 * Abstract class to represent a task in the schedule.
 * @author Ixent Galpin
 *
 */

public abstract class Task implements Cloneable {

    /**
     * 	The start time of this task.
     */
    protected long startTime;

    /**
     *  The end time of this task.
     */
    protected long endTime;

    /**
     * Constructs a class with given startTime; endTime is derived using cost functions which 
     * derive time taken to execute the task. 
     * @param startTime
     */
    public Task(final long startTime) {
	this.startTime = startTime;
    }

    /**
     * Returns the end time of this task.
     * @return
     */
    public final long getEndTime() {
    	return this.endTime;
    }

    /**
     * Returns the start time of this task.
     * @return
     */
    public final long getStartTime() {
    	return this.startTime;
    }

    /**
     * Returns the duration of this task.
     * @return
     */
    public final long getDuration() {
    	return this.endTime - this.startTime;
    }
    
    /**
     * @return False unless overwritten by sleep task
     */
    public boolean isSleepTask() {
	return false;
    }

    public abstract String getSiteID();

    public abstract Site getSite();

    /**
     * Computes that time cost that this task should be.
     * (for use during agenda construction)
     * @param daf
     * @return
     */
    protected abstract long getTimeCost(DAF daf);

    public abstract Task clone(DAF daf);

    public boolean isDeliverTask() {
    	if (this instanceof FragmentTask) {
    		FragmentTask ft = (FragmentTask)this;
			Fragment f = ft.getFragment();
			if (f.containsOperatorType(DeliverOperator.class)) {
				return true;
			}
    	}
    	return false;	
    }
    
}
