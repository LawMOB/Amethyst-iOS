package org.lwjgl.glfw;

import java.util.*;

public class CallbackBridge {
    // Clipboard constants
    public static final int CLIPBOARD_COPY = 2000;
    public static final int CLIPBOARD_PASTE = 2001;
    
    // GLFW compatibility event types
    public static final int EVENT_TYPE_CHAR = 1000;
    public static final int EVENT_TYPE_CHAR_MODS = 1001;
    public static final int EVENT_TYPE_CURSOR_ENTER = 1002;
    public static final int EVENT_TYPE_CURSOR_POS = 1003;
    public static final int EVENT_TYPE_FRAMEBUFFER_SIZE = 1004;
    public static final int EVENT_TYPE_KEY = 1005;
    public static final int EVENT_TYPE_MOUSE_BUTTON = 1006;
    public static final int EVENT_TYPE_SCROLL = 1007;
    public static final int EVENT_TYPE_WINDOW_SIZE = 1008;

    public static final boolean INPUT_DEBUG_ENABLED;

    static {
        INPUT_DEBUG_ENABLED = Boolean.parseBoolean(System.getProperty("glfwstub.debugInput", "false"));
    }

    // Native Bridge Declarations
    public static native String nativeClipboard(int action, byte[] copy);
    public static native void nativeSetGrabbing(boolean grab, float xset, float yset);
    
    public static void nativeSetGrabbing(boolean grabbing) {
        nativeSetGrabbing(grabbing, 0.0f, 0.0f);
    }

    public static native boolean nativeIsGrabbing();
    public static native void nativeInitSDL();
    public static native void nativePollSDLEvents();

    // Event Helper Dispatchers
    public static void sendChar(char codepoint) {
        if (INPUT_DEBUG_ENABLED) {
            System.out.println("[CallbackBridge] Char event: " + (int)codepoint);
        }
    }

    public static void sendKey(int key, int scancode, int action, int mods) {
        if (INPUT_DEBUG_ENABLED) {
            System.out.println("[CallbackBridge] Key event: key=" + key + ", scancode=" + scancode + ", action=" + action + ", mods=" + mods);
        }
    }

    public static void sendCursorPos(double x, double y) {
        if (INPUT_DEBUG_ENABLED) {
            System.out.println("[CallbackBridge] CursorPos event: x=" + x + ", y=" + y);
        }
    }

    public static void sendMouseButton(int button, int action, int mods) {
        if (INPUT_DEBUG_ENABLED) {
            System.out.println("[CallbackBridge] MouseButton event: button=" + button + ", action=" + action + ", mods=" + mods);
        }
    }
}
