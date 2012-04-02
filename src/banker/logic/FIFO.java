package banker.logic;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import banker.types.Activity;
import banker.types.ExecutionResult;
import banker.types.State;
import banker.types.Task;

/**
 * 
 * @author Sailee Latkar
 *
 */
public class FIFO{
	static int clock, countofPendingActivities;
	static LinkedList<Activity> activityList;
	static LinkedList<Task> taskList;	
	static HashMap<Integer,Integer> resourceList, delta;
	static PrintStream out;
	static int lastVisit = -1;
	
	/**
	 * This is the entry point for performing the FIFO resource allocation on the given input
	 * @param currentState the state of the system, as indicated in the input
	 */
	public static void perform(State currentState)
	{
		int countOfTasksHeld = 0;

		init(currentState);

		while(countofPendingActivities > 0)
		{
			System.out.println("\nDuring " + (clock-1) + "-" + clock);			

			countOfTasksHeld = performActivities();			

			//check if deadlock occurred, if yes, choose a victim process and delete it.
			checkForDeadLocks(countOfTasksHeld);

			//replenish activityList			
			replenishActivityList();

			clock++;

			currentState.setResourceList(delta);	
			resourceList = (HashMap<Integer, Integer>) delta.clone();

			countofPendingActivities = activityList.size();
		}

		displayFinalState("FIFO");
	}

	/**
	 * Basic initializations
	 * @param currentState the current state of the system as indicated by the input
	 */
	static void init(State currentState)
	{
		clock = 1;
		activityList = currentState.getActivityList();
		taskList = currentState.getTaskList();
		resourceList = currentState.getResourceList();	
		countofPendingActivities = activityList.size();
		delta = (HashMap<Integer, Integer>) resourceList.clone();

		//following is used to suppress the messages created to debug the code
		out = System.out;
		System.setOut(new PrintStream(new OutputStream(){
			public void write(int b) {}
		}));
	}

	/**
	 * Once the resource manager performs one iteration of resource allocation, this method replenishes
	 * the activity FIFO queue, such that only one activity is present per task. 
	 */
	static void replenishActivityList() {
		for(Task t: taskList)
		{
			boolean present = false;

			if(t.getPendingActivityCount() != 0)
			{
				for(Activity a:activityList)
				{
					//activity pertaining to task t is present, hence do not add t's next activity to the queue
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

	/**
	 * Displays the output 
	 * @param algo Name of the resource allocation algorithm 
	 */
	static void displayFinalState(String algo) {
		System.setOut(out);
		System.out.println("\n\t\t"+ algo+"\r\n");

		double totalEnd = 0D, totalWait = 0D;

		for(Task t:taskList)
		{
			totalEnd += t.getEndTime();		//compute totalEndTime
			totalWait+= t.getWaitTime();	//compute totalWaitTime
			System.out.println(t);			//display task details
		}

		//display summary
		DecimalFormat df = new DecimalFormat("#");
		System.out.println("TOTAL \t" + df.format(totalEnd) + "\t" + df.format(totalWait) + "\t" + df.format(totalWait*100/totalEnd) + "%");
	}

	/**
	 * This method checks if at least one activity was successfully processed by the resource manager. 
	 * If not, it aborts tasks one by one, until at least one activity gets all resources it needs.
	 *   
	 * @param countOfTasksHeld number of tasks that were not successfully processed by the resource manager
	 */
	static void checkForDeadLocks(int countOfTasksHeld) {	

		if(countOfTasksHeld == countofPendingActivities)
		{								
			abortConflictingTask();
			unHoldTasks();		
		}		
		else
		{
			lastVisit = -1;
		}
	}

	/**
	 * This method picks a victim task, and aborts it, so that its resources are available for the other tasks in the next cycle.
	 */
	private static void abortConflictingTask() {
		for(Task victim : taskList)
		{						
			//if the task has not been terminated yet
			if(victim.getPendingActivityCount() > 0)
			{
				System.out.println("According to the spec task "+victim.getID()+" is aborted now and its resources are available in next cycle");
				activityList.remove(victim.getNextActivity());
				victim.abort(clock, resourceList, delta);
				ExecutionResult.aborted.execute(victim);
				break;
			}
		}		
	}

	/**
	 * In case more than one victim needs to be terminated in order to resolve the deadlock, the other tasks are held for multiple cycles 
	 * (=no. of victims to be aborted to resolve the deadlock). Since this should all happen in one clock cycle, undo the effect of waiting for
	 * multiple cycles.
	 */
	private static void unHoldTasks() {
		if(lastVisit !=-1 && lastVisit == clock-1)
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
		else
			lastVisit = clock;
	}

	/**
	 * This method attempts to process each activity from the activity queue, and returns a count indicating the number of activities that were not successfully processed.	 
	 * @return count indicating the number of activities that were not successfully processed.
	 */
	private static int performActivities() {
		int countOfTasksHeld = 0;
		Iterator<Activity> itr = activityList.iterator();

		while(itr.hasNext())
		{
			Activity currentActivity = itr.next();
			ExecutionResult result = currentActivity.perform(clock, resourceList, delta,"fifo");			
			result.execute(currentActivity.getTask());

			if(result == ExecutionResult.success || result == ExecutionResult.aborted)
			{
				itr.remove();									
			}
			else if(result == ExecutionResult.hold)
			{
				countOfTasksHeld ++;
			}			
		}
		return countOfTasksHeld;
	}
}

