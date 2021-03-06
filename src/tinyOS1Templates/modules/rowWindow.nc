// __OPERATOR_DESCRIPTION__


__HEADER__

	#define OUT_QUEUE_CARD __OUT_QUEUE_CARD__
	#define SLIDE __SLIDE__
//CB: I is the FROM part of the window here
	#define EXCEPTROWS __EXCEPTROWS__

	__OUTPUT_TUPLE_TYPE__ outQueue[OUT_QUEUE_CARD];
	int8_t outHead=-1;
	int8_t outTail=0;
	int16_t currentEvalEpoch=0;
	uint8_t slideAdjust=0;
	uint8_t count=0;
	bool windowNotFull = TRUE;
	int8_t windowHead=0;
	int8_t windowTail=0;

	__CHILD_TUPLE_PTR_TYPE__ inQueue;
	int8_t inHead;
	int8_t inTail;
	uint16_t inQueueSize;


	command result_t Parent.requestData(int16_t evalEpoch)
  	{
		dbg(DBG_USR2,"__MODULE_NAME__ __OPERATOR_DESCRIPTION__ requestData() entered, now calling child at evalEpoch=%d\n",evalEpoch);
		currentEvalEpoch=evalEpoch;
		call Child.requestData(evalEpoch);
	   	return SUCCESS;
  	}

	inline void setevalEpochs()
	{
		int8_t temp=0;
		if (outHead!=-1)
		{
			temp=outHead;
			outQueue[temp].evalEpoch= currentEvalEpoch;
			temp=(temp+1) % OUT_QUEUE_CARD;
			while(temp != outTail)
			{
				outQueue[temp].evalEpoch=currentEvalEpoch;
				temp=(temp+1) % OUT_QUEUE_CARD;
			}
		}
	}

	void task signalDoneTask()
	{
		#ifdef PLATFORM_PC
			int8_t outputTupleCount;
			if (windowHead==-1)
			{
				outputTupleCount = 0;
			}
			else if (windowHead < windowTail)
			{
				outputTupleCount = windowTail - windowHead;
			}
			else
			{
				outputTupleCount = OUT_QUEUE_CARD - windowHead + windowTail;
			}
			dbg(DBG_USR1,"ROW_WINDOW: output cardinality %d tuple(s) for evalEpoch %d\n",outputTupleCount,currentEvalEpoch);
		#endif

		signal Parent.requestDataDone(outQueue, windowHead, windowTail, OUT_QUEUE_CARD);
	}

	void task refreshWindowTask() {
		__MAX_DEBUG__ dbg(DBG_USR3,"__MODULE_NAME__  refreshWindowTask count = %d\n",count);
		if (windowNotFull)
		{
			if (count >= OUT_QUEUE_CARD)
			{
				windowNotFull = FALSE;
				post refreshWindowTask();
			}
			else
			{
				slideAdjust=count % SLIDE;
				windowTail=(outTail-slideAdjust+ EXCEPTROWS);

				windowHead=(outTail+ SLIDE -1-slideAdjust);
				//window head needs to wrap ignoring some of begining
				if (windowHead > OUT_QUEUE_CARD)
					windowHead = windowHead % OUT_QUEUE_CARD;
				//Window head trying to get tuples at end that are not there
				if (windowHead>=windowTail)
					windowHead=0;
				if (windowTail <=0)
					windowHead=-1;
				__MAX_DEBUG__ dbg(DBG_USR3,"__MODULE_NAME__  count=%d, outTail=%d, windowTail=%d, windowHead=%d, slideAdjust=%d\n",count,outTail, windowTail, windowHead, slideAdjust);
				setevalEpochs();
				post signalDoneTask();
			}
		}
		else
		{
			count=count % SLIDE;
			windowTail=(outTail-count+ EXCEPTROWS + OUT_QUEUE_CARD) % OUT_QUEUE_CARD;
			windowHead=(outTail+ SLIDE -1-count) % OUT_QUEUE_CARD;
			__MAX_DEBUG__ dbg(DBG_USR3,"__MODULE_NAME__  windowFull, outTail=%d, windowTail=%d, windowHead=%d, count = %d\n", outTail, windowTail, windowHead, count);
			setevalEpochs();
			post signalDoneTask();
		}
	}

	void task outQueueAppendTask()
	{
		__MAX_DEBUG__ dbg(DBG_USR3,"__MODULE_NAME__  starting append inHead=%d, outhead=%d\n",inHead,outHead);
		if (inHead>-1)
		{
			do
				{
				// security Code
				atomic
				{
					__MAX_DEBUG__ dbg(DBG_USR3,"__MODULE_NAME__  appending inHead=%d, outhead=%d\n",inHead,outHead);
					if (outTail==outHead) {
						outHead=(outHead+1) % OUT_QUEUE_CARD;
					}
					if (outHead == -1)
					{
						outHead=0;
						outTail=0;
					}

__CONSTRUCT_TUPLE__

					outTail=(outTail+1)% OUT_QUEUE_CARD;
					count=count+1;
					inHead=((inHead+1)%inQueueSize);
				}
			} while(inHead != inTail);
		}
		__MAX_DEBUG__ dbg(DBG_USR3,"__MODULE_NAME__  append done inHead=%d, outhead=%d\n",inHead,outHead);
		post refreshWindowTask();
	}

	event result_t Child.requestDataDone(__CHILD_TUPLE_PTR_TYPE__ _inQueue, int8_t _inHead, int8_t _inTail, uint8_t _inQueueSize)
	{
		dbg(DBG_USR2,"__MODULE_NAME__ requestDataDone() signalled from child\n");

		atomic
		{
			inQueue = _inQueue;
			inHead = _inHead;
			inTail = _inTail;
			inQueueSize = _inQueueSize;
		}

		post outQueueAppendTask();

		return SUCCESS;
	}

}
