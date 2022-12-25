package lib;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class VulkanWindow {

    private long window;

    public VulkanWindow(int width, int height, String title) {

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        window = glfwCreateWindow(width, height, title, NULL, NULL);

        //TODO: Add some callbacks or callback support.
    }

    public long getWindowPtr() {
        return window;
    }
}
