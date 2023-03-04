package lib;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanCmdPool implements VulkanResource {

    private long pCommandPool = 0L;

    /**
     * Creates and initializes a command pool for Commands
     * @param device - A VulkanLogicalDevice object
     * @param queueFamilyIndex - An index to a queue family which will be used for submitting commands into the queue
     */
    public VulkanCmdPool(VulkanLogicalDevice device, int queueFamilyIndex) {

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

    /**
     * Creates and initializes a command pool for Commands
     * @param device - A VulkanLogicalDevice object
     * @param queueFamilyIndex - An index to a queue family which will be used for submitting commands into the queue
     * @param flags - defines how the command pool is going to behave or how it will be used
     */
    public VulkanCmdPool(VulkanLogicalDevice device, int queueFamilyIndex, int flags) {

        try(MemoryStack stack = stackPush()) {

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndex);
            poolInfo.flags(flags);

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

    @Override
    public void destroy(VulkanLogicalDevice device) {
        vkDestroyCommandPool(device.getVkDevice(), this.pCommandPool, null);
    }
}
