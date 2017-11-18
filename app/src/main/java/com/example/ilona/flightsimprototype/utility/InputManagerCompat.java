package com.example.ilona.flightsimprototype.utility;


import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.view.InputDevice;

import java.util.HashMap;
import java.util.Map;

public class InputManagerCompat {
    private final InputManager mInputManager;
    private final Map<InputManagerCompat.InputDeviceListener, V16InputDeviceListener> mListeners;

    public InputManagerCompat(Context context) {
        mInputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        mListeners = new HashMap<InputManagerCompat.InputDeviceListener, V16InputDeviceListener>();
    }

    public InputDevice getInputDevice(int id) {
        return mInputManager.getInputDevice(id);
    }

    public int[] getInputDeviceIds() {
        return mInputManager.getInputDeviceIds();
    }

    static class V16InputDeviceListener implements InputManager.InputDeviceListener {
        final InputManagerCompat.InputDeviceListener mIDL;

        public V16InputDeviceListener(InputDeviceListener idl) {
            mIDL = idl;
        }

        @Override
        public void onInputDeviceAdded(int deviceId) {
            mIDL.onInputDeviceAdded(deviceId);
        }

        @Override
        public void onInputDeviceChanged(int deviceId) {
            mIDL.onInputDeviceChanged(deviceId);
        }

        @Override
        public void onInputDeviceRemoved(int deviceId) {
            mIDL.onInputDeviceRemoved(deviceId);
        }

    }

    public void registerInputDeviceListener(InputDeviceListener listener, Handler handler) {
        V16InputDeviceListener v16Listener = new V16InputDeviceListener(listener);
        mInputManager.registerInputDeviceListener(v16Listener, handler);
        mListeners.put(listener, v16Listener);
    }

    public void unregisterInputDeviceListener(InputDeviceListener listener) {
        V16InputDeviceListener curListener = mListeners.remove(listener);
        if (null != curListener)
        {
            mInputManager.unregisterInputDeviceListener(curListener);
        }

    }

    public interface InputDeviceListener {
        /**
         * Called whenever the input manager detects that a device has been
         * added. This will only be called in the V9 version when a motion event
         * is detected.
         *
         * @param deviceId The id of the input device that was added.
         */
        void onInputDeviceAdded(int deviceId);

        /**
         * Called whenever the properties of an input device have changed since
         * they were last queried. This will not be called for the V9 version of
         * the API.
         *
         * @param deviceId The id of the input device that changed.
         */
        void onInputDeviceChanged(int deviceId);

        /**
         * Called whenever the input manager detects that a device has been
         * removed. For the V9 version, this can take some time depending on the
         * poll rate.
         *
         * @param deviceId The id of the input device that was removed.
         */
        void onInputDeviceRemoved(int deviceId);
    }
}
