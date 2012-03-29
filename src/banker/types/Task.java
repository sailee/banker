package banker.types;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;

public class Task {		
	int taskID;
	Double endTime, waitTime;
	HashMap<Integer, Integer> initialClaims;
	HashMap<Integer, Integer> resourcesHeld;
	LinkedList<Activity> activities;	
	boolean isAborted;

	public Task(int ID)
	{
		isAborted = false;
		endTime = -1D;
		waitTime = 0D;
		initialClaims = new HashMap<Integer, Integer>();
		resourcesHeld = new HashMap<Integer, Integer>();
		activities = new LinkedList<Activity>();
		taskID = ID;
	}

	@SuppressWarnings("unchecked")
	public Task(Task task) {
		this(task.taskID);
		
		initialClaims = (HashMap<Integer, Integer>) task.initialClaims.clone();
		resourcesHeld = (HashMap<Integer, Integer>) task.resourcesHeld.clone();
		
		for(Activity a : task.activities)
		{
			if(a.type == ActivityType.terminate)
				activities.add(new Activity(a.type.name(), this));
			else if(a.type == ActivityType.compute)
				activities.add(new Activity(a.type.name(), a.cycleCount, this));
			else
				activities.add(new Activity(a.type.name(),a.resourceType, a.resourceCount, this));
		}
	}

	//terminate
	public void addTerminate()
	{
		this.activities.add(new Activity("terminate", this));
	}

	//compute
	public void addCompute(int cycles)
	{
		this.activities.add(new Activity("compute", cycles, this));
	}

	//others
	public void addActivity(String type, int resourceType, int resourceCount)
	{
		this.activities.add(new Activity(type, resourceType, resourceCount, this));
	}
	
	public int getPendingActivityCount()
	{
		return this.activities.size();
	}

	//Terminate
	public HashMap<Integer, Integer> Terminate(int clock)
	{
		//set end time
		endTime = (double)clock;

		//free resources
		return resourcesHeld;
	}

	//Wait
	public void holdTask()
	{
		waitTime ++;
	}	
	
	public String toString()
	{
		if(!isAborted)
		{
			DecimalFormat df = new DecimalFormat("#");
			Double result = (double) (waitTime*100/(endTime));
			return "Task " + taskID + "\t" + df.format(endTime) + "\t" + df.format(waitTime) + "\t" + df.format(result) + "%";
		}
		return "Task " + taskID + "\t" + "aborted";
	}	

	public void unHold() {
		waitTime --;
		
	}

	public int getID() {
		return taskID;
	}
	
	public void addClaim(int resourceType, int resourceCount)
	{
		initialClaims.put(resourceType, resourceCount);
	}
	
	public Activity getNextActivity()
	{
		if(activities.size() > 0)
			return activities.getFirst();
		return null;
	}
	
	public void abort(int clock, HashMap<Integer, Integer> resourceList, HashMap<Integer, Integer> delta) {
		activities.clear();
		isAborted = true;
		addTerminate();
		activities.getFirst().perform(clock, resourceList, delta,"fifo");
	}

	public Integer resourcesRequired(Integer resourceType) {
		int claim = 0, held = 0;
		if(initialClaims.containsKey(resourceType))
			claim = initialClaims.get(resourceType);
		
		if(resourcesHeld.containsKey(resourceType))
			held = resourcesHeld.get(resourceType);
		
		return claim - held;
	}
	
	public Double getEndTime()
	{
		if(isAborted)
			return 0D; 
		return endTime;
	}
	
	public Double getWaitTime()
	{
		if(isAborted)
			return 0D;		
		return waitTime;
	}
}
