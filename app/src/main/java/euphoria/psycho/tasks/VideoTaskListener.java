package euphoria.psycho.tasks;

public interface VideoTaskListener {
    void synchronizeTask(VideoTask videoTask);

    void taskProgress(VideoTask videoTask);

}

