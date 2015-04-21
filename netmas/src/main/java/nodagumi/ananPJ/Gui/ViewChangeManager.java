package nodagumi.ananPJ.Gui;

import java.util.ArrayList;
import java.util.HashMap;

import javax.media.j3d.Behavior;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.media.j3d.WakeupOnElapsedTime;

/**
 * シミュレーション画面の描画更新イベントを処理する.
 *
 * イベントにはイベント名を表す文字列を使用する。<br />
 * イベントソースやパラメータ等は特に使用しないためこれで問題ない。
 */
public class ViewChangeManager extends Behavior {
    private WakeupOnElapsedFrames wonFrame;
    private WakeupOnElapsedTime wonTime;
    // private String event;
    private HashMap<String, ViewChangeListener> viewChangeListeners = new HashMap<String, ViewChangeListener>();
    private ArrayList<String> viewChangeEventQueue = new ArrayList<String>();

    public ViewChangeManager() {
        wonFrame = new WakeupOnElapsedFrames(0);
        wonTime = new WakeupOnElapsedTime(100);
    }

    @Override
    public void initialize() {
        wakeupOn(wonTime);
    }

    @Override
    public void processStimulus(java.util.Enumeration criteria) {
        String event;
        synchronized (this) {
            if (viewChangeEventQueue.isEmpty()) {
                event = null;
            } else {
                event = viewChangeEventQueue.remove(0);
                while (viewChangeEventQueue.remove(event)) ;    // 同じイベントは一度の処理で済ませる
            }
        }
        if (event != null) {
            ViewChangeListener listener = getViewChangeListener(event);
            listener.update();
        }
        // 未処理のイベントがある場合には直ちに処理を再開する
        wakeupOn(isEventQueueEmpty() ? wonTime : wonFrame);
    }

    /**
     * 描画更新イベント用のリスナを登録する.
     */
    public synchronized void addViewChangeListener(String event, ViewChangeListener listener) {
        viewChangeListeners.put(event, listener);
    }

    /**
     * 描画更新イベントの発生を通知する.
     */
    public synchronized void notifyViewChange(String event) {
        viewChangeEventQueue.add(event);
    }

    private synchronized ViewChangeListener getViewChangeListener(String event) {
        return viewChangeListeners.get(event);
    }

    private synchronized boolean isEventQueueEmpty() {
        return viewChangeEventQueue.isEmpty();
    }
}
