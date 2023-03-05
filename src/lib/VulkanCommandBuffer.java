package lib;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VulkanCommandBuffer {

    private VkCommandBuffer vkCommandBuffer;

    public VulkanCommandBuffer(VulkanLogicalDevice device, VulkanCmdPool commandPool, boolean isSecondary) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool.pCommandPool);
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

    // TODO Alter this with the ability to use the VulkanBuffers.Buffer
    public void bindVertexBuffers(int firstBinding, long buffer, long offsets) {
        if (buffer == 0L)
            throw new NullPointerException("Can not bind buffer! Vertex Buffer is null!");
        vkCmdBindVertexBuffers(vkCommandBuffer, firstBinding, stackGet().longs(buffer), stackGet().longs(offsets));
    }

    /**
     * @param indexBuffer A Vlkan
     * @param indexType how the index numbers are represented. Commonly {@link  VK10#VK_INDEX_TYPE_UINT16 VK_INDEX_TYPE_UINT16} or {@link  VK10#VK_INDEX_TYPE_UINT32 VK_INDEX_TYPE_UINT32}
     */
    public void bindIndexBuffer(VulkanBuffers.Buffer indexBuffer, int indexType) {
        vkCmdBindIndexBuffer(vkCommandBuffer, indexBuffer.pBuffer,0, indexType);
    }

    public void bindIndexBuffer(VulkanBuffers.Buffer indexBuffer, int indexType, int offset) {
        vkCmdBindIndexBuffer(vkCommandBuffer, indexBuffer.pBuffer, offset, indexType);
    }

    /**
     * Draws the Object with the bound index Buffer.
     * @param indexCount How many indices to draw.
     */
    public void drawIndexed(int indexCount) {
        vkCmdDrawIndexed(vkCommandBuffer, indexCount, 1, 0, 0,0);
    }

    /**
     * Draws the Object with the bound index Buffer.
     * @param indexCount How many indices to draw.
     * @param firstIndex offset in the index buffer (From which index should be the index buffer read first).
     */
    public void drawIndexed(int indexCount, int firstIndex) {
        vkCmdDrawIndexed(vkCommandBuffer, indexCount, 1, firstIndex, 0,0);
    }

    /**
     * Draws the Object with the bound index Buffer.
     * @param indexCount How many indices to draw.
     * @param firstIndex offset in the index buffer (From which index should be the index buffer read first).
     * @param instanceCount how many instances should be drawn.
     */
    public void drawIndexed(int indexCount, int firstIndex, int instanceCount) {
        vkCmdDrawIndexed(vkCommandBuffer, indexCount, instanceCount, firstIndex, 0,0);
    }

}
