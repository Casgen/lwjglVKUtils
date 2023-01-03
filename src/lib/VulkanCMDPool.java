package lib;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanCMDPool {

    private long pCommandPool = 0L;

    /**
     * Creates and initializes a command pool for Commands
     * @param device - A VulkanLogicalDevice object
     * @param queueFamilyIndex - An index to a queue family which will be used for submitting commands into the queue
     */
    public VulkanCMDPool(VulkanLogicalDevice device, int queueFamilyIndex) {

        try(MemoryStack stack = stackPush()) {

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndex);

            LongBuffer pCommandPool = stack.mallocLong(1);

            if (vkCreateCommandPool(device.getVkDevice(), poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }

            this.pCommandPool = pCommandPool.get(0);
        }
    }

    public long getCommandPoolPtr() {
        return pCommandPool;
    }
}
