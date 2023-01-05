package lib;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackGet;

public class VulkanFrame {

    VulkanSemaphore imageAvailableSemaphore;
    VulkanSemaphore renderFinishedSemaphore;
    VulkanFence fence;

    public VulkanFrame(VulkanSemaphore imageAvailableSemaphore, VulkanSemaphore renderFinishedSemaphore, VulkanFence fence) {
        this.imageAvailableSemaphore = imageAvailableSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.fence = fence;
    }

    public VulkanSemaphore getImageAvailableSemaphore() {
        return imageAvailableSemaphore;
    }

    public LongBuffer getImageAvailableSemaphorePtr() {
        return stackGet().longs(imageAvailableSemaphore.getSemaphorePtr());
    }

    public VulkanSemaphore getRenderFinishedSemaphore() {
        return renderFinishedSemaphore;
    }

    public LongBuffer getRenderFinishedSemaphorePtr() {
        return stackGet().longs(renderFinishedSemaphore.getSemaphorePtr());
    }

    public VulkanFence getFence() {
        return fence;
    }

    public LongBuffer getFencePtr() {
        return stackGet().longs(fence.getFencePtr());
    }
}
