package lib;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.vulkan.KHRSurface.*;

public class SwapChainSupportDetails {

    private VkSurfaceCapabilitiesKHR capabilities;
    private VkSurfaceFormatKHR.Buffer formats;


    private IntBuffer presentModes;

    public SwapChainSupportDetails(VkPhysicalDevice device, VulkanSurface surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);

            VulkanUtils.check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface.getSurfacePtr(), capabilities));

            IntBuffer formatCount = stack.callocInt(1);
            VulkanUtils.check(vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface.getSurfacePtr(),formatCount,null));

            if (formatCount.get(0) != 0) {
                formats = VkSurfaceFormatKHR.malloc(formatCount.get(0), stack);
                VulkanUtils.check(vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface.getSurfacePtr(),formatCount,formats));
            }

            IntBuffer presentModeCount = stack.callocInt(1);
            VulkanUtils.check(vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface.getSurfacePtr(),presentModeCount,null));

            if (presentModeCount.get(0) != 0) {
                presentModes = stack.mallocInt(presentModeCount.get(0));
                VulkanUtils.check(vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface.getSurfacePtr(),presentModeCount,presentModes));
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
}
