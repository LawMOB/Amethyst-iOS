/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 *
 * Angel Aura Amethyst override.
 *
 * The real LWJGL GLFWErrorCallback builds a genuine native trampoline via
 * libffi (ffi_prep_closure_loc) whenever a Java-side GLFWErrorCallbackI
 * implementation needs to be callable FROM native code:
 *   - the no-arg Callback() constructor (used by createPrint()/createThrow())
 *   - CallbackI.address(), called from create(GLFWErrorCallbackI)
 *
 * libffi isn't linked into this build, so both paths crash with
 * UnsatisfiedLinkError / NoClassDefFoundError(Upcalls).
 *
 * Nothing in this custom GLFW bridge ever actually invokes an error callback
 * through a real native function pointer - there is no genuine native GLFW
 * event loop here, just our own Java-side GLFW.java shim - so this override
 * never needs a real closure. It always uses Callback's pointer-wrapping
 * constructor with a dummy address, which does no libffi work at all.
 *
 * Public API surface is kept identical to upstream so any code (including
 * Minecraft's own compiled classes, and our own GLFW.java) that calls into
 * GLFWErrorCallback keeps working unmodified.
 */
package org.lwjgl.glfw;

import org.lwjgl.system.*;

import javax.annotation.*;

import java.io.PrintStream;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Callback function: {@link #invoke GLFWerrorfun} */
public abstract class GLFWErrorCallback extends Callback implements GLFWErrorCallbackI {

    // Dummy, non-NULL "address" for callbacks that were never actually handed
    // a real native function pointer. Never dereferenced by this shim.
    private static final long DUMMY_ADDRESS = 1L;

    /**
     * Creates a {@code GLFWErrorCallback} instance from the specified function pointer.
     *
     * @return the new {@code GLFWErrorCallback}
     */
    public static GLFWErrorCallback create(long functionPointer) {
        GLFWErrorCallbackI instance;
        try {
            instance = Callback.get(functionPointer);
        } catch (Throwable t) {
            // Callback.get() also touches Upcalls/libffi internally. This path
            // isn't expected to be hit in this shim (nothing hands out real
            // native error-callback pointers here), but guard it anyway.
            t.printStackTrace();
            instance = null;
        }
        final GLFWErrorCallbackI delegate = instance;
        return delegate instanceof GLFWErrorCallback
            ? (GLFWErrorCallback) delegate
            : new GLFWErrorCallback(functionPointer) {
                @Override
                public void invoke(int error, long description) {
                    if (delegate != null) {
                        delegate.invoke(error, description);
                    }
                }
            };
    }

    /** Like {@link #create(long) create}, but returns {@code null} if {@code functionPointer} is {@code NULL}. */
    @Nullable
    public static GLFWErrorCallback createSafe(long functionPointer) {
        return functionPointer == NULL ? null : create(functionPointer);
    }

    /** Creates a {@code GLFWErrorCallback} instance that delegates to the specified {@code GLFWErrorCallbackI} instance. */
    public static GLFWErrorCallback create(GLFWErrorCallbackI instance) {
        if (instance instanceof GLFWErrorCallback) {
            return (GLFWErrorCallback) instance;
        }
        // Upstream calls instance.address() here, which is what builds the
        // libffi closure. Skip it entirely - use a dummy address instead.
        return new GLFWErrorCallback(DUMMY_ADDRESS) {
            @Override
            public void invoke(int error, long description) {
                instance.invoke(error, description);
            }
        };
    }

    /** Safe no-arg constructor: wraps a dummy pointer instead of building a real libffi closure. */
    protected GLFWErrorCallback() {
        super(DUMMY_ADDRESS);
    }

    GLFWErrorCallback(long functionPointer) {
        super(functionPointer);
    }

    /**
     * No real native closure was ever allocated (dummy address, never a genuine
     * libffi upcall trampoline), so there's nothing to free. The inherited
     * Callback.free() unconditionally calls into Upcalls/libffi regardless of
     * how the object was constructed, which would crash the same way creation
     * did. Overriding to a no-op is correct, not a workaround: it accurately
     * reflects that this object holds no native resource.
     */
    @Override
    public void free() {
    }

    /**
     * Converts the specified {@code GLFWErrorCallback} argument to a String.
     *
     * <p>This method may only be used inside a GLFWErrorCallback invocation.</p>
     *
     * @param description pointer to the UTF-8 encoded description string
     *
     * @return the description as a String
     */
    public static String getDescription(long description) {
        return memUTF8(description);
    }

    /**
     * Returns a {@code GLFWErrorCallback} instance that prints the error to the {@link APIUtil#DEBUG_STREAM}.
     *
     * @return the GLFWerrorCallback
     */
    public static GLFWErrorCallback createPrint() {
        return createPrint(APIUtil.DEBUG_STREAM);
    }

    /**
     * Returns a {@code GLFWErrorCallback} instance that prints the error in the specified {@link PrintStream}.
     *
     * @param stream the PrintStream to use
     *
     * @return the GLFWerrorCallback
     */
    public static GLFWErrorCallback createPrint(PrintStream stream) {
        return new GLFWErrorCallback() {
            private Map<Integer, String> ERROR_CODES = APIUtil.apiClassTokens((field, value) -> 0x10000 < value && value < 0x20000, null, GLFW.class);

            @Override
            public void invoke(int error, long description) {
                String msg = getDescription(description);

                StringBuilder sb = new StringBuilder(512);
                sb
                    .append("[LWJGL] ")
                    .append(ERROR_CODES.get(error))
                    .append(" error\n")
                    .append("\tDescription : ")
                    .append(msg)
                    .append("\n")
                    .append("\tStacktrace :\n");
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                for (int i = 4; i < stack.length; i++) {
                    sb.append("\t\t");
                    sb.append(stack[i]);
                    sb.append("\n");
                }
                stream.print(sb);
            }
        };
    }

    /**
     * Returns a {@code GLFWErrorCallback} instance that throws an {@link IllegalStateException} when an error occurs.
     *
     * @return the GLFWerrorCallback
     */
    public static GLFWErrorCallback createThrow() {
        return new GLFWErrorCallback() {
            @Override
            public void invoke(int error, long description) {
                throw new IllegalStateException(String.format("GLFW error [0x%X]: %s", error, getDescription(description)));
            }
        };
    }

    /** See {@link GLFW#glfwSetErrorCallback SetErrorCallback}. */
    public GLFWErrorCallback set() {
        glfwSetErrorCallback(this);
        return this;
    }
}
