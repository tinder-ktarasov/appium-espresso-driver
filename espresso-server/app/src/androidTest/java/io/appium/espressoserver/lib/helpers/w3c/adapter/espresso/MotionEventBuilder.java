package io.appium.espressoserver.lib.helpers.w3c.adapter.espresso;

import android.os.SystemClock;
import android.support.test.espresso.InjectEventSecurityException;
import android.support.test.espresso.UiController;
import android.view.MotionEvent;

import java.util.List;

import io.appium.espressoserver.lib.handlers.exceptions.AppiumException;
import io.appium.espressoserver.lib.helpers.AndroidLogger;
import io.appium.espressoserver.lib.helpers.w3c.models.InputSource.PointerType;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static io.appium.espressoserver.lib.helpers.w3c.adapter.espresso.Helpers.getToolType;

public class MotionEventBuilder {

    private MotionEventParams motionEventParams;

    public MotionEventBuilder() {
        motionEventParams = new MotionEventParams();
    }

    public MotionEventBuilder withX(List<Long> x) {
        motionEventParams.x = x;
        return this;
    }

    public MotionEventBuilder withY(List<Long> y) {
        motionEventParams.y = y;
        return this;
    }

    public MotionEventBuilder withDownTime(long downTime) {
        motionEventParams.downTime = downTime;
        return this;
    }

    public MotionEventBuilder withEventTime(long eventTime) {
        motionEventParams.eventTime = eventTime;
        return this;
    }

    public MotionEventBuilder withAction(int action) {
        motionEventParams.action = action;
        return this;
    }

    public MotionEventBuilder withMetaState(int metaState) {
        motionEventParams.metaState = metaState;
        return this;
    }

    public MotionEventBuilder withButtonState(int buttonState) {
        motionEventParams.buttonState = buttonState;
        return this;
    }

    public MotionEventBuilder withXPrecision(float xPrecision) {
        motionEventParams.xPrecision = xPrecision;
        return this;
    }

    public MotionEventBuilder withYPrecision(float yPrecision) {
        motionEventParams.yPrecision = yPrecision;
        return this;
    }

    public MotionEventBuilder withDeviceId(int deviceId) {
        motionEventParams.deviceId = deviceId;
        return this;
    }

    public MotionEventBuilder withSource(int source) {
        motionEventParams.source = source;
        return this;
    }

    public MotionEventBuilder withEdgeFlags(int edgeFlags) {
        motionEventParams.edgeFlags = edgeFlags;
        return this;
    }

    public MotionEventBuilder withPointerType(PointerType pointerType) {
        motionEventParams.pointerType = pointerType;
        return this;
    }

    public MotionEventRunner build() {
        return new MotionEventRunner(motionEventParams);
    }

    static class MotionEventParams {
        private long downTime;
        private int action;
        private List<Long> x;
        private List<Long> y;
        private int metaState;
        private float xPrecision;
        private float yPrecision;
        private int deviceId;
        private int edgeFlags;
        private int buttonState;
        private int source;
        private PointerType pointerType;
        private long eventTime;
    }

    static class MotionEventRunner {
        private final MotionEventParams motionEventParams;

        public MotionEventRunner(final MotionEventParams motionEventParams) {
            this.motionEventParams = motionEventParams;
        }

        public MotionEvent run (UiController uiController) throws AppiumException {
            int pointerCount = motionEventParams.x == null ? 0 : motionEventParams.x.size();

            AndroidLogger.logger.info("Calling pointers", pointerCount);

            // Don't do anything if no pointers were provided
            if (pointerCount == 0 && motionEventParams.action != ACTION_CANCEL) {
                return null;
            }

            MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[pointerCount];
            MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[pointerCount];

            for (int pointerIndex = 0; pointerIndex < pointerCount; pointerIndex++) {
                // Set pointer coordinates
                pointerCoords[pointerIndex] = new MotionEvent.PointerCoords();
                pointerCoords[pointerIndex].clear();
                pointerCoords[pointerIndex].pressure = 1;
                pointerCoords[pointerIndex].size = 1;
                pointerCoords[pointerIndex].x = motionEventParams.x.get(pointerIndex);
                pointerCoords[pointerIndex].y = motionEventParams.y.get(pointerIndex);

                // Set pointer properties
                pointerProperties[pointerIndex] = new MotionEvent.PointerProperties();
                pointerProperties[pointerIndex].toolType = getToolType(motionEventParams.pointerType);
                pointerProperties[pointerIndex].id = pointerIndex;

            }

            // ACTION_POINTER_DOWN and ACTION_POINTER_UP need a bit mask
            int action = motionEventParams.action;
            if (pointerCount > 1 && (action == ACTION_POINTER_DOWN || action == ACTION_POINTER_UP)) {
                action += (pointerProperties[1].id << ACTION_POINTER_INDEX_SHIFT);
            }

            // ACTION_DOWN and ACTION_UP and ACTION_CANCEL has a pointer count of 1
            if (action == ACTION_DOWN || action == ACTION_UP || action == ACTION_CANCEL) {
                if (motionEventParams.x != null && motionEventParams.y != null) {
                    pointerCount = 1;
                } else {
                    pointerCount = 0;
                }
            }

            MotionEvent evt = MotionEvent.obtain(
                    motionEventParams.downTime,
                    motionEventParams.eventTime > 0 ? motionEventParams.eventTime : SystemClock.uptimeMillis(),
                    action,
                    pointerCount,
                    pointerProperties,
                    pointerCoords,
                    motionEventParams.metaState,
                    motionEventParams.buttonState,
                    motionEventParams.xPrecision,
                    motionEventParams.yPrecision,
                    motionEventParams.deviceId,
                    motionEventParams.edgeFlags,
                    motionEventParams.source,
                    0 // TODO: How to get Motion Event flags?
            );

            try {
                boolean success = uiController.injectMotionEvent(evt);
                if (!success) {
                    throw new AppiumException("Could not complete pointer operation");
                }
            } catch (InjectEventSecurityException e) {
                throw new AppiumException(String.format(
                        "Could not complete pointer operation. An internal server error occurred: %s",
                        e.getCause()
                ));
            }

            return evt;
        }
    }
}