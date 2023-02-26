package render;

import lib.*;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;

public class VulkanRenderer {

    private VulkanInstance instance;
    private VulkanSurface surface;
    private VulkanDevices devices;
    private VulkanSwapChain swapChain;

    public long getGraphicsPipeline() {
        return graphicsPipeline;
    }

    public void setGraphicsPipeline(long graphicsPipeline) {
        this.graphicsPipeline = graphicsPipeline;
    }

    private long graphicsPipeline;

    public VulkanInstance getInstance() {
        return instance;
    }

    public void setInstance(VulkanInstance instance) {
        this.instance = instance;
    }

    public VulkanSurface getSurface() {
        return surface;
    }

    public void setSurface(VulkanSurface surface) {
        this.surface = surface;
    }

    public VulkanDevices getDevices() {
        return devices;
    }

    public VulkanSwapChain getSwapChain() {
        return swapChain;
    }

    public void setSwapChain(VulkanSwapChain swapChain) {
        this.swapChain = swapChain;
    }

    public VulkanRenderer(VulkanWindow window, String appName, VkDebugUtilsMessengerCallbackEXT dbgFunc) {
        instance = new VulkanInstance(appName, dbgFunc);
        surface = new VulkanSurface(instance, window);
        devices = new VulkanDevices(instance, surface);
        swapChain = new VulkanSwapChain(devices,surface,window);
    }
}
