package lib;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSemaphore implements VulkanResource {

    private long pSemaphore = NULL;

    public VulkanSemaphore(VulkanLogicalDevice device) {
        this(device, 0);
    }

    public long getSemaphorePtr() {
        return pSemaphore;
    }

    public VulkanSemaphore(VulkanLogicalDevice device, int flags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo createInfo = VkSemaphoreCreateInfo.calloc(stack);
            createInfo.sType$Default();
            createInfo.flags(flags);

            long[] ptr = new long[1];

            VulkanUtils.check(vkCreateSemaphore(device.getVkDevice(),createInfo,null, ptr));
            pSemaphore = ptr[0];
        }
    }

    @Override
    public void destroy(VulkanLogicalDevice device) {
        vkDestroySemaphore(device.getVkDevice(),pSemaphore,null);
    }
}
