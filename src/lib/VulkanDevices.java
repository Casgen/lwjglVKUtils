package lib;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

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
     * Creates a VkBuffer and binds the device memory to it.
     *
     * @param usage - What purpose will the buffer be used for.
     * @param sharingMode - Sets how the buffer will be shared. That means if it's going to be exclusive to only one
     *                    family queue or be shared between multiple queues.
     * @param size - Size of the data in bytes
     * @param memoryProperty - Sets the properties of the memory (How it should behave)
     * @return A pointer to the newly created buffer;
     */
    public long createBuffer(int usage, int sharingMode, int size, int memoryProperty, ByteBuffer srcData) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            // DON'T FORGET TO REWIND!
            srcData.rewind();

            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.create();
            bufferCreateInfo.sType$Default()
                    .usage(usage)
                    .sharingMode(sharingMode)
                    .size(size);

            LongBuffer pBuffer = stack.callocLong(1);

            VulkanUtils.check(vkCreateBuffer(logicalDevice.getVkDevice(), bufferCreateInfo, null, pBuffer));

            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc(stack);

            vkGetBufferMemoryRequirements(logicalDevice.getVkDevice(), pBuffer.get(0), memoryRequirements);

            VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack);

            allocateInfo.sType$Default()
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(physicalDevice.findMemoryType(memoryRequirements.memoryTypeBits(), memoryProperty));

            LongBuffer pVertexBufferMemory = stack.mallocLong(1);

            VulkanUtils.check(vkAllocateMemory(logicalDevice.getVkDevice(), allocateInfo, null, pVertexBufferMemory));

            vkBindBufferMemory(logicalDevice.getVkDevice(), pBuffer.get(0), pVertexBufferMemory.get(0), 0);

            PointerBuffer dest = stack.mallocPointer(1);

            vkMapMemory(logicalDevice.getVkDevice(), pVertexBufferMemory.get(0), 0, bufferCreateInfo.size(), 0, dest);
            {
                dest.getByteBuffer(0, (int) bufferCreateInfo.size()).put(srcData);
            }

            vkUnmapMemory(logicalDevice.getVkDevice(), pVertexBufferMemory.get(0));

            return pBuffer.get(0);
        }
    }

}
