package banker.types;

import java.util.HashMap;
import java.util.LinkedList;

public class State {
	
	HashMap<Integer,Integer> resourceList; 
	LinkedList<Task> taskList;
	LinkedList<Activity> activityList;
	
	public State(int resourceCount)
	{		
		taskList =new LinkedList<Task>();
		activityList = new LinkedList<Activity>();
		resourceList = new HashMap<Integer, Integer>(resourceCount);
	}

	@SuppressWarnings("unchecked")
	public State(State currentState) {
		this(currentState.resourceList.size());
		
		for(Task t : currentState.taskList)
		{
			taskList.add(new Task(t));
			Activity activity = t.activities.getFirst();
			activityList.add(activity);
		}
		resourceList = (HashMap<Integer, Integer>) currentState.resourceList.clone();
	}

	public HashMap<Integer, Integer> getResourceList() {
		return resourceList;
	}	

	public LinkedList<Task> getTaskList() {
		return taskList;
	}
	
	public LinkedList<Activity> getActivityList() {
		return activityList;
	}

	public void setResourceList(HashMap<Integer, Integer> delta) {
		resourceList = delta;		
	}	
}
