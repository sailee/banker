package banker.logic;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import banker.types.Activity;
import banker.types.ActivityType;
import banker.types.ExecutionResult;
import banker.types.State;
import banker.types.Task;

public class Bankers {

	static int clock = 1;
	static LinkedList<Activity> activityList;
	static LinkedList<Task> taskList;	
	static HashMap<Integer,Integer> resourceList;
	static HashMap<Integer, Integer> delta;

	@SuppressWarnings("unchecked")
	public static void perform(State currentState)
	{
		int initialActivityCount = 0;
		activityList = currentState.getActivityList();
		taskList = currentState.getTaskList();
		resourceList = currentState.getResourceList();
		
		PrintStream out = System.out;
		PrintStream temp;

		try {
			temp = new PrintStream("file1.txt");
		} catch (FileNotFoundException e) {
			temp = System.err;
		}
		System.setOut(temp);
		
		while(true)
		{
			initialActivityCount = 0;

			//exit when no task has any activity left
			if(activityList.size() == 0)
				break;

			System.out.println("\nDuring " + (clock-1) + "-" + clock);

			delta = (HashMap<Integer, Integer>) resourceList.clone();			

			initialActivityCount = performActivities(initialActivityCount);

			//replenish activityList			
			replenishActivityList();

			clock++;

			currentState.setResourceList(delta);	
			resourceList = delta;
		}

		System.setOut(out);
		
		System.out.println("\n\t\tBANKERS\r\nTask#\tEnd\tWait\tWait %");

		double totalEnd = 0D, totalWait = 0D;

		for(Task t:taskList)
		{
			totalEnd += t.getEndTime();
			totalWait+= t.getWaitTime();
			System.out.println(t);
		}
		DecimalFormat df = new DecimalFormat("#");
		System.out.println("TOTAL \t" + df.format(totalEnd) + "\t" + df.format(totalWait) + "\t" + df.format(totalWait*100/totalEnd) + "%");

	}	

	private static void replenishActivityList() {
		for(Task t: taskList)
		{
			boolean present = false;

			if(t.getPendingActivityCount() != 0)
			{
				for(Activity a:activityList)
				{
					if(a.getTask() == t)
					{
						present = true;
						break;
					}
				}

				if(!present)
					activityList.add(t.getNextActivity());
			}
		}
	}

	private static int performActivities(int countOfTasksHeld) 
	{
		Iterator<Activity> itr = activityList.iterator();
		boolean isRequestValid = false;

		while(itr.hasNext())
		{
			Activity currentActivity = itr.next();
			if(currentActivity.getType() == ActivityType.request)
			{
				isRequestValid = validateRequest(currentActivity);
			}
			else
			{
				isRequestValid = true;
			}

			if(isRequestValid)
			{
				ExecutionResult result = currentActivity.perform(clock, resourceList, delta, "bankers");
				result.execute(currentActivity.getTask());

				if(result == ExecutionResult.success || result == ExecutionResult.aborted)
				{
					itr.remove();
					activityList.remove(currentActivity);					
				}
				else if(result == ExecutionResult.hold)
				{
					countOfTasksHeld ++;
				}
			}
			else
			{
				countOfTasksHeld ++;
				currentActivity.getTask().holdTask();
			}
		}
		return countOfTasksHeld;
	}

	@SuppressWarnings("unchecked")
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
			{
				currentTask = newTaskList.getLast();
			}
		}

		PrintStream out = System.out;
		PrintStream temp;
//		temp = System.err;

		try {
			temp = new PrintStream("file1.txt");
		} catch (FileNotFoundException e) {
			temp = System.err;
			//e.printStackTrace();
		}		

		System.setOut(temp);
		ExecutionResult result = currentTask.getNextActivity().perform(clock, newResourceList, newDelta, "bankers");
		
		//verify if granting the request leads to a safe state. result = success indicates that the request can be granted 
		if(result == ExecutionResult.success)
		{
			int count = 0;

			for(int i=0;i<newTaskList.size();i++)		
			{			
				Boolean matchFound = true;
				count = 0;
				for(Task task: newTaskList)
				{		
					matchFound = true;
					if(task.getPendingActivityCount() > 0 && task.getNextActivity().getType() != ActivityType.terminate)
					{									
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

						if(matchFound)
						{
							task.abort(-1, newResourceList, newDelta);
							count++;
						}
					}
					else
					{
						count++;
					}
				}

				if(!matchFound)
				{
					System.setOut(out);
					System.out.println("Cannot grant request for Task " + currentActivity.getTask().getID());
					return false;				
				}

				if(count == taskList.size())
				{
					System.setOut(out);
					return true;
				}				
			}
		}
		
		// The request cannot be granted, hence the task should either be held, or aborted based on result
		System.setOut(out);
		System.out.println("Execution result for Task " + currentActivity.getTask().getID() + " = " +  result.name());
		return true;
	}	

}
