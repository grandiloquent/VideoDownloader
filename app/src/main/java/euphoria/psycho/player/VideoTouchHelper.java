package euphoria.psycho.player;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class VideoTouchHelper {
    private final Listener mListener;
    private boolean mAlwaysInTapRegion;
    private float mDownFocusX;
    private float mDownFocusY;
    private float mLastFocusX;
    private float mLastFocusY;
    private boolean mStillDown;
    private int mTouchSlopSquare;

    public VideoTouchHelper(Context context, Listener listener) {
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop();
        mListener = listener;
        mTouchSlopSquare = touchSlop * touchSlop;
    }

    public boolean onTouch(MotionEvent ev) {
        int action = ev.getAction();
        float focusX = ev.getX();
        float focusY = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                mStillDown = true;
                mAlwaysInTapRegion = true;
            }
            case MotionEvent.ACTION_MOVE: {
                final float scrollX = mLastFocusX - focusX;
                final float scrollY = mLastFocusY - focusY;
                if (mAlwaysInTapRegion) {
                    final int deltaX = (int) (focusX - mDownFocusX);
                    final int deltaY = (int) (focusY - mDownFocusY);
                    int distance = (deltaX * deltaX) + (deltaY * deltaY);
                    int slopSquare = mTouchSlopSquare;
                    if (distance > slopSquare) {
                        mListener.onScroll(scrollX, scrollY);
                        mLastFocusX = focusX;
                        mLastFocusY = focusY;
                        mAlwaysInTapRegion = false;
                    } else {
                        mIgnoreNextUpEvent = true;
                    }
                } else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                    mListener.onScroll(scrollX, scrollY);
                    mLastFocusX = focusX;
                    mLastFocusY = focusY;
                }
            }
            case MotionEvent.ACTION_UP: {
                mStillDown = false;
                if (mAlwaysInTapRegion && !mIgnoreNextUpEvent) {
                    mListener.onSingleTapConfirmed();
                }
                mIgnoreNextUpEvent = false;
            }
        }
        return true;
    }

    private boolean mIgnoreNextUpEvent;

    public interface Listener {
        boolean onScroll(float distanceX, float distanceY);

        void onSingleTapConfirmed();
    }
}