package render;

import lib.*;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;

public class VulkanRenderer {

    private VulkanInstance instance;
    private VulkanSurface surface;
    private VulkanPhysicalDevice physicalDevice;
    private VulkanLogicalDevice logicalDevice;

    public VulkanRenderer(VulkanWindow window, String appName, VkDebugUtilsMessengerCallbackEXT dbgFunc) {
        instance = new VulkanInstance(appName, dbgFunc);
        surface = new VulkanSurface(instance, window);
        physicalDevice = new VulkanPhysicalDevice(instance, surface);
        logicalDevice = new VulkanLogicalDevice(physicalDevice);
    }
}
