package banker.logic;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import banker.types.State;
import banker.types.Task;

public class Parser {

	public State parseInput(String filePath) throws Exception
	{
		FileReader fread;
		Scanner scan;
		@SuppressWarnings("unused")
		int taskCount = 0, resourceCount = 0;		 

		try 
		{
			fread = new FileReader(filePath);
			scan = new Scanner(fread);

			taskCount = scan.nextInt();
			resourceCount = scan.nextInt();
			
			State currentState = new State(resourceCount);

			for(int i=1;i<=resourceCount;i++)
			{				
				currentState.getResourceList().put(i,scan.nextInt());
			}		

			while(scan.hasNext())
			{
				Task t;
				String str = scan.next();
				Integer taskID = scan.nextInt();
				
				if(currentState.getTaskList().size() >= taskID)
				{
					t = currentState.getTaskList().get(taskID-1);
				}
				else
				{
					t = new Task(taskID);
					currentState.getTaskList().add(taskID-1, t);
				}
				
				if(str.equalsIgnoreCase("terminate"))
					t.addTerminate();
				else if(str.equalsIgnoreCase("compute"))
					t.addCompute(scan.nextInt());
				else
					t.addActivity(str, scan.nextInt(), scan.nextInt());
			}
			
			buildActivityList(currentState);
			return currentState;
		}
		catch(FileNotFoundException fnf)
		{
			return null;
		}
		catch(Exception ex)
		{
			throw ex;
		}
	}
	
	void buildActivityList(State currentState)
	{
		for(Task task: currentState.getTaskList())
		{
			currentState.getActivityList().add(task.getNextActivity());
		}
	}
}
