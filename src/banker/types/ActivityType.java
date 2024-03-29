package banker.types;

import java.util.HashMap;

public enum ActivityType {
	initiate {
		public ExecutionResult executeFIFO(Activity a, HashMap<Integer, Integer> resourceList, HashMap <Integer, Integer> delta) 
		{			
			System.out.println("Task " +  a.getTask().getID() + " completed its initiate request.");
			a.getTask().addClaim(a.resourceType, a.resourceCount);				 
			return ExecutionResult.success;
		}

		@Override
		public ExecutionResult executeBankers(Activity a, HashMap<Integer, Integer> resourceList, HashMap<Integer, Integer> delta) 
		{			
			//validate initial claim
			Task t = a.getTask();

			if(a.resourceCount > resourceList.get(a.resourceType))
			{
				System.out.println("Task " +  t.taskID + " could not complete its initiate request. Claim is greater than units available.");
				t.abort(0, resourceList, delta);
				return ExecutionResult.aborted;
			}
			else
			{
				System.out.println("Task " +  t.taskID + " completed its initiate request.");
				t.initialClaims.put(a.resourceType, a.resourceCount);
			}
			return ExecutionResult.success;
		}
	},

	request {
		@Override
		public ExecutionResult executeFIFO(Activity a, HashMap<Integer, Integer> resourceList, HashMap <Integer, Integer> delta) {	
			Task t = a.getTask();

			System.out.println("Task " + t.getID() + " requested: " + a.resourceCount + " of type: " + a.resourceType);

			//if the task requests for more resources than currently available, hold the process
			if(a.resourceCount > resourceList.get(a.resourceType) || a.resourceCount > delta.get(a.resourceType))
			{
				System.out.println("Could not allocate resources. Need to wait.");
				System.out.println("\t"+delta.get(a.resourceType) + " items remaining of resource " + a.resourceType);
				return ExecutionResult.hold;
			}
			//allocate the required number of resources to the task
			else
			{
				System.out.println("Task " + t.taskID + " completes its request (i.e., the request is granted).");
				//allocate resources
				if(t.resourcesHeld.containsKey(a.resourceType))
				{
					t.resourcesHeld.put(a.resourceType, t.resourcesHeld.get(a.resourceType) + a.resourceCount);
				}
				else
				{					
					t.resourcesHeld.put(a.resourceType, a.resourceCount);
				}

				//update the delta
				delta.put(a.resourceType, delta.get(a.resourceType) - a.resourceCount);
				System.out.println("\t"+delta.get(a.resourceType) + " items remaining of resource " + a.resourceType);
			}	
			return ExecutionResult.success;
		}

		@Override
		public ExecutionResult executeBankers(Activity a, HashMap<Integer, Integer> resourceList, HashMap<Integer, Integer> delta) 
		{	
			Task t = a.getTask();

			System.out.println("Task " + t.getID() + " requested: " + a.resourceCount + " of type: " + a.resourceType);
			
			ExecutionResult result = validateRequest(a, resourceList, delta);

			if(result == ExecutionResult.success)
			{
				System.out.println("Task " + t.taskID + " completes its request (i.e., the request is granted).");
				//allocate resources
				if(t.resourcesHeld.containsKey(a.resourceType))
				{
					t.resourcesHeld.put(a.resourceType, t.resourcesHeld.get(a.resourceType) + a.resourceCount);
				}
				else
				{					
					t.resourcesHeld.put(a.resourceType, a.resourceCount);
				}

				//update the delta
				delta.put(a.resourceType, delta.get(a.resourceType) - a.resourceCount);
				System.out.println("\t"+delta.get(a.resourceType) + " items remaining of resource " + a.resourceType);
				return ExecutionResult.success;
			}
			
			return result;			
		}

		private ExecutionResult validateRequest(Activity a, HashMap<Integer, Integer> resourceList, HashMap<Integer, Integer> delta) 
		{
			int count;
			Task t = a.getTask();

			if(t.resourcesHeld.containsKey(a.resourceType))
				count = t.resourcesHeld.get(a.resourceType);
			else
				count = 0;

			//if the task attempts to hold resources that exceed initial claim, terminate process
			if(a.resourceCount + count > t.initialClaims.get(a.resourceType))
			{
				System.out.println("Units required exceed initial claim. Terminating process.");				
				t.abort(0, resourceList, delta);
				return ExecutionResult.aborted;
			}
			//if the request is within claim, check if request can be satisfied
			else if(a.resourceCount > resourceList.get(a.resourceType) || a.resourceCount > delta.get(a.resourceType))
				return ExecutionResult.hold;
			
			return ExecutionResult.success;
		}		
	},
	release {
		@Override
		public ExecutionResult executeFIFO(Activity a, HashMap<Integer, Integer> resourceList, HashMap <Integer, Integer> delta) {
			//			System.out.println("Inside release for task: " + t.taskID);
			Task t = a.getTask();

			return releaseResource(t.taskID, t.resourcesHeld, delta, a.resourceType, a.resourceCount);			
		}

		@Override
		public ExecutionResult executeBankers(Activity a, HashMap<Integer, Integer> resourceList, HashMap<Integer, Integer> delta) {
			executeFIFO(a,resourceList,delta);
			return ExecutionResult.success;
		}		
	},

	compute {
		@Override
		public ExecutionResult executeFIFO(Activity a, HashMap<Integer, Integer> resourceList, HashMap <Integer, Integer> delta) 
		{		
			Task t = a.getTask();

			System.out.println("Inside compute for task: " + t.taskID);
			System.out.println("Pending hold time: " + a.cycleCount);

			a.cycleCount--;
			if(a.cycleCount > 0)
			{
				t.activities.addFirst(new Activity(a.type.name(), a.cycleCount, t));				
			}			

			return ExecutionResult.success;			
		}

		@Override
		public ExecutionResult executeBankers(Activity a, HashMap<Integer, Integer> resourceList, HashMap<Integer, Integer> delta) 
		{
			return executeFIFO(a, resourceList, delta) ;			
		}
	},
	terminate {
		@Override
		public ExecutionResult executeFIFO(Activity a, HashMap<Integer, Integer> resourceList, HashMap <Integer, Integer> delta) {
			Task t = a.getTask();
			return terminate(t,delta);			
		}

		@Override
		public ExecutionResult executeBankers(Activity a, HashMap<Integer, Integer> resourceList, HashMap<Integer, Integer> delta) 
		{
			return executeFIFO(a, resourceList, delta);			
		}
	};

	public abstract ExecutionResult executeFIFO(Activity a, HashMap<Integer, Integer> resourceList, HashMap <Integer, Integer> delta);
	public abstract ExecutionResult executeBankers(Activity a, HashMap<Integer, Integer> resourceList, HashMap <Integer, Integer> delta);

	private static ExecutionResult releaseResource(int taskID, HashMap<Integer, Integer> resourcesHeld,
			HashMap<Integer, Integer> delta, int resourceType, int resourceCount) 
	{
		if(resourceCount <= resourcesHeld.get(resourceType))
		{
			resourcesHeld.put(resourceType, resourcesHeld.get(resourceType) - resourceCount );
			delta.put(resourceType, delta.get(resourceType) + resourceCount);		

			System.out.println("Task " + taskID + " released "+ resourceCount +  " units of resource type " + resourceType);
			System.out.println("\t"+delta.get(resourceType) + " items remaining of resource " + resourceType);

			return ExecutionResult.success;
		}

		return ExecutionResult.invalid;
	}

	private static ExecutionResult terminate(Task t, HashMap<Integer, Integer> delta) {
		System.out.println("Task " + t.taskID + " terminated.");
		//release all resources			
		for(Integer resID : t.resourcesHeld.keySet())
		{
			releaseResource(t.taskID, t.resourcesHeld, delta, resID, t.resourcesHeld.get(resID));
		}

		return ExecutionResult.success;
	}
};
