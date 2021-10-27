package euphoria.psycho.tasks;

public class RequestQueueStatus {
    private int mTotalTasks;
    private int mRunningTasks;

    public RequestQueueStatus(int totalTasks, int runningTasks) {
        mTotalTasks = totalTasks;
        mRunningTasks = runningTasks;
    }

    public int getRunningTasks() {
        return mRunningTasks;
    }

    public void setRunningTasks(int runningTasks) {
        mRunningTasks = runningTasks;
    }

    public int getTotalTasks() {
        return mTotalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        mTotalTasks = totalTasks;
    }
}
