package lib;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

public class SwapChainSupportDetails {

    private VkSurfaceCapabilitiesKHR capabilities;
    private VkSurfaceFormatKHR.Buffer formats;
    private IntBuffer presentModes;

    public SwapChainSupportDetails(VkPhysicalDevice device, VulkanSurface surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);

            VulkanUtils.check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface.getSurfacePtr(), capabilities));

            IntBuffer formatCount = stack.callocInt(1);
            VulkanUtils.check(vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface.getSurfacePtr(), formatCount, null));

            if (formatCount.get(0) != 0) {
                formats = VkSurfaceFormatKHR.malloc(formatCount.get(0));
                VulkanUtils.check(vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface.getSurfacePtr(), formatCount, formats));
            }

            IntBuffer presentModeCount = stack.callocInt(1);
            VulkanUtils.check(vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface.getSurfacePtr(), presentModeCount, null));

            if (presentModeCount.get(0) != 0) {
                presentModes = MemoryUtil.memAllocInt(presentModeCount.get(0));
                VulkanUtils.check(vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface.getSurfacePtr(), presentModeCount, presentModes));
            }
        }
    }

    public VkSurfaceCapabilitiesKHR getCapabilities() {
        return capabilities;
    }

    public VkSurfaceFormatKHR.Buffer getFormats() {
        return formats;
    }

    public IntBuffer getPresentModes() {
        return presentModes;
    }

    public VkSurfaceFormatKHR chooseSwapSurfaceFormat() {

        for (int i = 0; i < formats.capacity(); i++) {

            formats.position(i);
            if (formats.get(i).format() == VK_FORMAT_B8G8R8A8_SRGB
                    && formats.get(i).colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return formats.get(i);
            }
        }

        formats.position(0);
        return formats.get(0);
    }

    public int chooseSwapPresentMode() {
        for (int i = 0; i < presentModes.capacity(); i++) {
            presentModes.position(i);

            if (presentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR)
                return presentModes.get(i);
        }

        return VK_PRESENT_MODE_FIFO_KHR;
    }

    public VkExtent2D chooseSwapExtent(VulkanWindow window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            if (capabilities.currentExtent().width() !=
                    Math.max(capabilities.currentExtent().width(), capabilities.maxImageExtent().width())) {
                return capabilities.currentExtent();
            }

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);

            glfwGetFramebufferSize(window.getWindowPtr(), width, height);

            int clampedWidth = Math.max(capabilities.minImageExtent().width(), Math.min(capabilities.maxImageExtent().width(), width.get(0)));
            int clampedHeight = Math.max(capabilities.minImageExtent().height(), Math.min(capabilities.maxImageExtent().height(), height.get(0)));

            VkExtent2D actualExtent = VkExtent2D.malloc();

            actualExtent.width(clampedWidth);
            actualExtent.height(clampedHeight);

            return actualExtent;
        }
    }
}
