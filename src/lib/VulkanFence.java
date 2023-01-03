package lib;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanFence implements VulkanResource {

    private long pFence = NULL;

    public VulkanFence(VulkanLogicalDevice device) {
        this(device, 0);
    }

    public VulkanFence(VulkanLogicalDevice device, int flags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFenceCreateInfo createInfo = VkFenceCreateInfo.calloc(stack);
            createInfo.sType$Default();
            createInfo.flags(flags);

            long[] ptr = new long[1];

            VulkanUtils.check(vkCreateFence(device.getVkDevice(),createInfo,null, ptr));
            pFence = ptr[0];
        }
    }

    @Override
    public void destroy(VulkanLogicalDevice device) {
        vkDestroyFence(device.getVkDevice(),pFence,null);
    }
}
