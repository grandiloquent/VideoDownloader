package euphoria.psycho;

public interface VideoTaskListener {
    void synchronizeTask(VideoTask videoTask);

    void taskProgress(VideoTask videoTask);

    void taskStart(VideoTask videoTask);
}

