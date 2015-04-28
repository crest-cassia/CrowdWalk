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
    private String event = null;
    private HashMap<String, ViewChangeListener> viewChangeListeners = new HashMap<String, ViewChangeListener>();
    private ArrayList<String> viewChangeEventQueue = new ArrayList<String>();

    public ViewChangeManager() {
        wonFrame = new WakeupOnElapsedFrames(0);
        wonTime = new WakeupOnElapsedTime(5);   // この程度ならば無駄な負荷が掛かることはない
    }

    @Override
    public void initialize() {
        wakeupOn(wonTime);
    }

    @Override
    public void processStimulus(java.util.Enumeration criteria) {
        synchronized (this) {
            if (viewChangeEventQueue.isEmpty()) {
                if (event != null) {
                    event = null;
                    jobLeaved();
                }
            } else {
                if (event == null) {
                    jobEntered();
                }
                event = viewChangeEventQueue.remove(0);
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
        System.err.println("@addViewChangeListener: " + event);
        viewChangeListeners.put(event, listener);
    }

    /**
     * 描画更新イベントの発生を通知する.
     */
    public synchronized boolean notifyViewChange(String event) {
        if (viewChangeEventQueue.contains(event)) {
            // 同じイベントを二重登録しない
            return false;
        } else {
            viewChangeEventQueue.add(event);
            return true;
        }
    }

    /**
     * 描画処理に入った時に呼ばれる.
     */
    public void jobEntered() {}

    /**
     * 描画処理が全て終了した時に呼ばれる.
     */
    public void jobLeaved() {}

    private synchronized ViewChangeListener getViewChangeListener(String event) {
        return viewChangeListeners.get(event);
    }

    private synchronized boolean isEventQueueEmpty() {
        return viewChangeEventQueue.isEmpty();
    }
}
