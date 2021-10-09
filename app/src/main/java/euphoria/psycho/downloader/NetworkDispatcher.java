package euphoria.psycho.downloader;

import android.os.Process;

import java.util.concurrent.BlockingQueue;


public class NetworkDispatcher extends Thread {
    private final BlockingQueue<Request> mQueue;
    private volatile boolean mQuit = false;

    public NetworkDispatcher(BlockingQueue<Request> queue) {
        mQueue = queue;
    }//

    public void quit() {
        mQuit = true;
        interrupt();
    }

    private void processRequest() throws InterruptedException {
        // Take a request from the queue.
        Request request = mQueue.take();
        processRequest(request);
    }

    private void processRequest(Request request) {
        request.sendEvent(RequestQueue.RequestEvent.REQUEST_NETWORK_DISPATCH_STARTED);
        try {
            request.start();
        } catch (Exception e) {
        } finally {
            request.sendEvent(RequestQueue.RequestEvent.REQUEST_NETWORK_DISPATCH_FINISHED);
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            try {
                processRequest();
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }


}
