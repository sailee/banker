package banker.types;

public enum ExecutionResult {
	aborted {
		@Override
		public void execute(Task task) {
			task.activities.clear();						
		}
	}, 
	success {
		@Override
		public void execute(Task task) {
			if(task.activities.size() > 0)
				task.activities.remove();						
		}
	}, 
	hold {
		@Override
		public void execute(Task task) {
			task.holdTask();						
		}
	},
	invalid {
		@Override
		public void execute(Task task) {						
		}
	};
	
	public abstract void execute(Task task);
}
