package banker.logic;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import banker.types.Activity;
import banker.types.ExecutionResult;
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
		
		System.setOut(out);

		System.out.println("\n\t\tFIFO\r\nTask#\tEnd\tWait\tWait %");
		
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
				ExecutionResult.aborted.execute(victim);
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
			ExecutionResult result = currentActivity.perform(clock, resourceList, delta,"fifo");			
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
		return countOfTasksHeld;
	}
}

