package banker.logic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import banker.types.Activity;
import banker.types.State;
import banker.types.Task;

public class FIFO {
	static int clock = 1;
	static LinkedList<Activity> activityList;
	static LinkedList<Task> taskList;	
	static HashMap<Integer,Integer> resourceList; 

	public static void perform(State currentState)
	{
		int initialActivityCount = 0, countofPendingActivities, lastVisit = -1;
		activityList = currentState.getActivityList();
		taskList = currentState.getTaskList();
		resourceList = currentState.getResourceList();

		System.out.println(currentState.getResourceList());

		while(true)
		{
			initialActivityCount = 0;
			countofPendingActivities = activityList.size();

			//exit when no task has any activity left
			if(countofPendingActivities == 0)
				break;
			
			System.out.println("\nDuring " + (clock-1) + "-" + clock);

			@SuppressWarnings("unchecked")
			HashMap<Integer, Integer> delta = (HashMap<Integer, Integer>) resourceList.clone();			

			initialActivityCount = performActivities( initialActivityCount, delta);

			//deadlock
			if(initialActivityCount == countofPendingActivities)
			{
				clock = unHoldTasks(lastVisit);				
				lastVisit = abortConflictingTask(delta);
			}

			//replenish activityList			
			replenishActivityList();

			clock++;

			currentState.setResourceList(delta);	
			resourceList = delta;
		}

		System.out.println("\n\t\tFIFO\nTask#\tEnd\tWait\tWait %");

		for(Task t:taskList)
		{
			System.out.println(t);
		}
	}

	private static int abortConflictingTask(HashMap<Integer, Integer> delta) {
		int lastVisit;
		lastVisit = clock;

		for(Task victim : taskList)
		{						
			if(victim.getPendingActivityCount() > 0)
			{
				System.out.println("According to the spec task "+victim.getID()+" is aborted now and its resources are available in next cycle");
				activityList.remove(victim.getNextActivity());
				victim.abort(clock, resourceList, delta);
				
				break;
			}
		}
		return lastVisit;
	}

	private static int unHoldTasks(int lastVisit) {
		if(lastVisit != -1 && lastVisit == clock - 1)
		{
			clock--;		

			//unhold all held processes					
			for(Task t : taskList)
			{						
				if(t.getPendingActivityCount() > 0)
				{
					t.unHold();
				}
			}	
		}
		return clock;
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

	private static int performActivities(int countOfTasksHeld, HashMap<Integer, Integer> delta) {

		Iterator<Activity> itr = activityList.iterator();

		while(itr.hasNext())
		{
			Activity currentActivity = itr.next();
			boolean result = currentActivity.perform(clock, resourceList, delta);

			if(result)
			{
				itr.remove();
				activityList.remove(currentActivity);
			}
			else
			{
				countOfTasksHeld ++;
			}
		}
		return countOfTasksHeld;
	}
}
