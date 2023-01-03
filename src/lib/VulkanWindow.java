package lib;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jni.JNINativeInterface;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class VulkanWindow {

    private long window;
    private int width, height = 0;

    public VulkanWindow(int width, int height, String title) {

        this.width = width;
        this.height = height;

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        window = glfwCreateWindow(width, height, title, NULL, NULL);

        final long ptr = JNINativeInterface.NewGlobalRef(this);

        glfwSetWindowUserPointer(window, ptr);
        //TODO: Add some callbacks or callback support.
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getWindowPtr() {
        return window;
    }
}
