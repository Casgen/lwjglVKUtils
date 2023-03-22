package lib;

import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;

public class VulkanSurface {

    private long surfacePtr = 0L;

    public VulkanSurface(VulkanInstance instance, VulkanWindow window) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pointer = stack.mallocLong(1);

            VulkanUtils.check(glfwCreateWindowSurface(instance.getVkInstance(),window.getWindowPtr(), null, pointer));
            surfacePtr = pointer.get(0);
        }
    }

    public long getSurfacePtr() {
        return surfacePtr;
    }
}
