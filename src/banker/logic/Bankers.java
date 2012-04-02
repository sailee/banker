package banker.logic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import banker.types.Activity;
import banker.types.ActivityType;
import banker.types.ExecutionResult;
import banker.types.State;
import banker.types.Task;

public class Bankers extends FIFO {	
	
	/**
	 * This is the entry point for performing the FIFO resource allocation on the given input
	 * @param currentState the state of the system, as indicated in the input
	 */
	public static void perform(State currentState)
	{	

		init(currentState);

		while(countofPendingActivities > 0)
		{
			System.out.println("\nDuring " + (clock-1) + "-" + clock);			
			
			performActivities();			

			//replenish activityList			
			replenishActivityList();

			clock++;

			currentState.setResourceList(delta);	
			resourceList = (HashMap<Integer, Integer>) delta.clone();

			countofPendingActivities = activityList.size();
		}

		displayFinalState("Bankers");
	}

	
	static void performActivities() 
	{		
		Iterator<Activity> itr = activityList.iterator();
		boolean isRequestValid = false;

		while(itr.hasNext())
		{
			Activity currentActivity = itr.next();
			isRequestValid = validateActivity(currentActivity);

			if(isRequestValid)
			{
				ExecutionResult result = currentActivity.perform(clock, resourceList, delta, "bankers");
				result.execute(currentActivity.getTask());

				if(result == ExecutionResult.success || result == ExecutionResult.aborted)
				{
					itr.remove();										
				}				
			}
			else
			{				
				currentActivity.getTask().holdTask();
			}
		}		
	}

	private static boolean validateActivity(Activity currentActivity) {
		boolean isRequestValid;
		if(currentActivity.getType() == ActivityType.request)
		{
			isRequestValid = validateRequest(currentActivity);
		}
		else
		{
			isRequestValid = true;
		}
		return isRequestValid;
	}

	private static boolean validateRequest(Activity currentActivity) {	

		LinkedList<Task> newTaskList = new LinkedList<Task>();	
		HashMap<Integer, Integer> newResourceList = (HashMap<Integer, Integer>) resourceList.clone();		
		HashMap<Integer, Integer> newDelta = (HashMap<Integer, Integer>) delta.clone();
		Task currentTask = null;

		//clone task list
		for(Task t : taskList)
		{	
			newTaskList.add(new Task(t));
			
			if(currentActivity.getTask() == t)
				currentTask = newTaskList.getLast();
		}
		
		ExecutionResult result = currentTask.getNextActivity().perform(clock, newResourceList, newDelta, "bankers");

		//verify if granting the request leads to a safe state. result = success indicates that the request can be granted 
		if(result == ExecutionResult.success)
		{
			return checkForSafeState(currentActivity, newTaskList, newResourceList, newDelta);
		}
		return true;		
		
	}

	private static boolean checkForSafeState(Activity currentActivity,
			LinkedList<Task> newTaskList,
			HashMap<Integer, Integer> newResourceList,
			HashMap<Integer, Integer> newDelta) {
		int terminatedProcessCount = 0;	//keeps a track of count of terminated processes
		
		System.out.println(".......Inside validate....");

		for(int i=0;i<newTaskList.size();i++)		
		{			
			Boolean matchFound = true;
			terminatedProcessCount = 0;
			
			for(Task task: newTaskList)
			{		
				matchFound = true;
				if(task.getPendingActivityCount() > 0 && task.getNextActivity().getType() != ActivityType.terminate)
				{									
					//finding the task whose additional resource needs can be currently satisfied
					for(Integer resourceType : newDelta.keySet())
					{
						Integer quantity = newDelta.get(resourceType);

						int needs = task.resourcesRequired(resourceType);

						System.out.println("Task " + task.getID() + " requires " + needs + " of resource " + resourceType + ". Current Availability = " + quantity);
						
						if(needs > quantity)
						{
							matchFound = false;
							break;
						}
					}

					//if such a task is found, pretend that it terminated (equivalent to abort)
					if(matchFound)
					{
						task.abort(-1, newResourceList, newDelta);
						terminatedProcessCount++;
					}
				}
				else
				{
					terminatedProcessCount++;
				}
			}
			
			//if not a single task could be completed after granting this request, system will be led to unsafe state, hence reject this request. 
			if(!matchFound)
			{
				System.out.println(".......validate returned false....");
				return false;				
			}

			//if all processes could be completed after granting this request, the system will be led to safe state, hence grant this request.
			if(terminatedProcessCount == taskList.size())
			{				
				System.out.println(".......validate returned true....");
				return true;
			}		
		}
		System.out.println(".......validate returned ??....");
		return false;
	}
}
