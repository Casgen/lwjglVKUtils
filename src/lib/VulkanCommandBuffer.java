package lib;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VulkanCommandBuffer {

    private VkCommandBuffer vkCommandBuffer;

    public VulkanCommandBuffer(VulkanLogicalDevice device, VulkanCMDPool commandPool, boolean isSecondary) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool.getCommandPoolPtr());
            allocInfo.level(isSecondary ? VK_COMMAND_BUFFER_LEVEL_SECONDARY : VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffers = stack.mallocPointer(1);

            if (vkAllocateCommandBuffers(device.getVkDevice(), allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            vkCommandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), device.getVkDevice());
        }
    }

    public VkCommandBuffer getVkCommandBuffer() {
        return vkCommandBuffer;
    }

    public void beginCommandBuffer(VkCommandBufferBeginInfo beginInfo) {
        VulkanUtils.check(vkBeginCommandBuffer(vkCommandBuffer,beginInfo));
    }

    public void endCommandBuffer() {
        VulkanUtils.check(vkEndCommandBuffer(vkCommandBuffer));
    }

    // ------------- COMMANDS ---------------

    public void beginRenderPass(VkRenderPassBeginInfo renderPassBeginInfo, int contents) {
        vkCmdBeginRenderPass(vkCommandBuffer,renderPassBeginInfo,contents);
    }

    public void endRenderPass() {
        vkCmdEndRenderPass(vkCommandBuffer);
    }

    public void bindPipeline(int pipelineBindPoint, VulkanGraphicsPipeline pipeline) {
        vkCmdBindPipeline(vkCommandBuffer,pipelineBindPoint,pipeline.getPipelinePtr());
    }

    public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        vkCmdDraw(vkCommandBuffer, vertexCount,instanceCount,firstVertex,firstInstance);
    }

    public void bindVertexBuffers(int firstBinding, long buffer, long offsets) {
        if (buffer == 0L)
            throw new NullPointerException("Can not bind buffer! Vertex Buffer is null!");
        vkCmdBindVertexBuffers(vkCommandBuffer, firstBinding, stackGet().longs(buffer), stackGet().longs(offsets));
    }
}
