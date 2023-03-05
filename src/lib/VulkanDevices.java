package lib;

import exc.MemoryAllocationException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanDevices {

    private VulkanLogicalDevice logicalDevice;
    private VulkanPhysicalDevice physicalDevice;

    public VulkanDevices(VulkanInstance instance, VulkanSurface surface) {
        physicalDevice = new VulkanPhysicalDevice(instance, surface);
        logicalDevice = new VulkanLogicalDevice(physicalDevice);
    }

    public VulkanLogicalDevice getLogicalDevice() {
        return logicalDevice;
    }

    public VulkanPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkDevice getVkDevice() {
        return logicalDevice.getVkDevice();
    }

    public VkPhysicalDevice getVkPhysicalDevice() {
        return physicalDevice.getVkPhysicalDevice();
    }


    /**
     * Creates a VkBuffer, binds the device memory to it and fills it up with the given data.
     *
     * @param usage          What purpose will the buffer be used for.
     * @param sharingMode    Sets how the buffer will be shared. That means if it's going to be exclusive to only one
     *                       family queue or be shared between multiple queues.
     * @param memoryProperty Sets the properties of the memory (How it should behave)
     * @param srcData        A buffer of pre-filled data
     * @return A pointer to the newly created buffer;
     * @implNote The size is determined by the ByteBuffer capacity.
     */
    public VulkanBuffers.Buffer createBuffer(int usage, int sharingMode, int memoryProperty, ByteBuffer srcData) {
        // DON'T FORGET TO REWIND TO SET THE POSITION TO 0!
        srcData.rewind();

        VulkanBuffers.Buffer vertexBuffer = createBuffer(usage, sharingMode, memoryProperty, srcData.capacity());
        vertexBuffer.setData(logicalDevice, srcData);

        return vertexBuffer;
    }

    /**
     * Creates a VkBuffer and binds the device memory to it.
     *
     * @param usage          What purpose will the buffer be used for.
     * @param sharingMode    Sets how the buffer will be shared. That means if it's going to be exclusive to only one
     *                       family queue or be shared between multiple queues.
     * @param size           Size of the data in bytes
     * @param memoryProperty Sets the properties of the memory (How it should behave)
     * @return A pointer to the newly created buffer;
     */
    public VulkanBuffers.Buffer createBuffer(int usage, int sharingMode, int memoryProperty, int size) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.create();
            bufferCreateInfo.sType$Default()
                    .usage(usage)
                    .sharingMode(sharingMode)
                    .size(size);

            LongBuffer pBuffer = stack.callocLong(1);

            VulkanUtils.check(vkCreateBuffer(getVkDevice(), bufferCreateInfo, null, pBuffer));

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc(stack);

            vkGetBufferMemoryRequirements(getVkDevice(), pBuffer.get(0), memoryRequirements);

            VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack);

            allocateInfo.sType$Default()
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(physicalDevice.findMemoryType(memoryRequirements.memoryTypeBits(), memoryProperty));


            LongBuffer pBufferMemory = stack.mallocLong(1);

            VulkanUtils.check(vkAllocateMemory(getVkDevice(), allocateInfo, null, pBufferMemory));

            if (pBufferMemory.get(0) == MemoryUtil.NULL)
                throw new MemoryAllocationException("Failed to allocate Memory! vkAllocateMemory returned a null pointer!");

            vkBindBufferMemory(getVkDevice(), pBuffer.get(0), pBufferMemory.get(0), 0);

            VulkanBuffers.Buffer vertexBuffer;
            vertexBuffer = new VulkanBuffers.Buffer(pBuffer.get(0), pBufferMemory.get(0), bufferCreateInfo.size());

            return vertexBuffer;
        }
    }

    /**
     * Copies the data from the source buffer and pastes it onto the destination buffer.
     *
     * @param srcBuffer
     * @param dstBuffer
     * @param size
     * @implNote THIS IS VERY IMPORTANT. Since a new command pool is being created here, it shouldn't be used in a hot
     * code! (for ex. a for loop) Here we are creating an entirely new transient command pool for a single command!
     * if you wish to copy frequently, please create a command pool dedicated only for these operations and use the other function!
     */
    public void copyBuffer(VulkanBuffers.Buffer srcBuffer, VulkanBuffers.Buffer dstBuffer, long size) {
        try (MemoryStack stack = stackPush()) {
            int graphicsQueue;
            if (physicalDevice.getQueueFamilyIndices().getGraphicsFamily().isEmpty())
                throw new NullPointerException("Graphics family queue is null!");

            graphicsQueue = physicalDevice.getQueueFamilyIndices().getGraphicsFamily().get();

            VulkanCmdPool cmdPool = new VulkanCmdPool(logicalDevice, graphicsQueue, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);

            copyBuffer(srcBuffer, dstBuffer, size, cmdPool);

            cmdPool.destroy(logicalDevice);

        }
    }

    /**
     * Copies the data from the source buffer and pastes it onto the destination buffer.
     *
     * @param srcBuffer - Source buffer which the data will be copied from.
     * @param dstBuffer - Destination buffer for pasting the copied data.
     * @param size      - How many bytes of data will be copied.
     * @param cmdPool   = Command pool which will be used for the copy operation
     */
    public void copyBuffer(VulkanBuffers.Buffer srcBuffer, VulkanBuffers.Buffer dstBuffer, long size, VulkanCmdPool cmdPool) {
        try (MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandPool(cmdPool.pCommandPool);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(logicalDevice.getVkDevice(), allocInfo, pCommandBuffer);
            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), logicalDevice.getVkDevice());

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(commandBuffer, beginInfo);
            {
                VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
                copyRegion.size(size);
                vkCmdCopyBuffer(commandBuffer, srcBuffer.pBuffer, dstBuffer.pBuffer, copyRegion);
            }
            vkEndCommandBuffer(commandBuffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(pCommandBuffer);

            if (vkQueueSubmit(logicalDevice.getGraphicsQueue(), submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
                throw new RuntimeException("Failed to submit copy command buffer");
            }

            vkQueueWaitIdle(logicalDevice.getGraphicsQueue());

            vkFreeCommandBuffers(getVkDevice(), cmdPool.pCommandPool, pCommandBuffer);
        }
    }

}
