package banker.types;

import java.util.EnumSet;
import java.util.HashMap;

public class Activity {
	ActivityType type;
	int resourceType, resourceCount, cycleCount;
	Task task;
	
	//for terminate
	public Activity(String type, Task task)
	{
		 for(ActivityType activityType : EnumSet.allOf(ActivityType.class))
		 {
			 if(activityType.name().equalsIgnoreCase(type))
			 {
				 this.type = activityType;
				 break;
			 }
		 }            
		 
		 this.task = task;		 
	}
	
	//for compute
	public Activity(String type, int cycleCount, Task task)
	{
		 this(type,task);
		 this.cycleCount = cycleCount;
	}
	
	//for all others
	public Activity(String type, int resType, int count, Task task)
	{
		 this(type, task);             
		 
		 resourceType = resType;
		 resourceCount = count;
	}
	
	public boolean perform(int clock, HashMap<Integer, Integer> resourceList, HashMap<Integer, Integer> delta)
	{		
		if(type == ActivityType.terminate)
		{
			task.endTime = clock - 1;
		}
		
		//if the activity was successful, remove it from the list, in order to proceed to the next, else hold		
		if(type.executeFIFO(this, resourceList, delta))
		{				
			task.activities.remove();
			return true;
		}
		else
		{
			task.holdTask();
			return false;
		}		
	}
	
	
	public String toString()
	{
		return type.name() + ": Resource Type: " + resourceType + " Resource Count: " +  resourceCount + " Cycle Count: " + cycleCount;		
	}

	public Task getTask() {
		return task;
	}
	
}
