import abstr.Vertex;
import lib.*;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import render.VulkanRenderer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static lib.ShaderUtils.*;

public class Application {

    private VulkanWindow window;
    private VulkanRenderPass renderPass;
    private VulkanRenderer renderer;
    private VulkanGraphicsPipeline graphicsPipeline;
    private VulkanFrameBuffer frameBuffers;
    private VulkanCmdPool commandPool;
    private List<VulkanCommandBuffer> commandBuffers;

    private final int maxFramesInFlight = 2;
    private List<VulkanFrame> inFlightFrames = new ArrayList<>(maxFramesInFlight);
    private Map<Integer, VulkanFrame> imagesInFlight;
    private VulkanBuffers.Buffer vertexBuffer;


    public static VkDebugUtilsMessengerCallbackEXT dbgCb = VkDebugUtilsMessengerCallbackEXT.create(
            (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                String severity;
                if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
                    severity = "VERBOSE";
                } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
                    severity = "INFO";
                } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                    severity = "WARNING";
                } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                    severity = "ERROR";
                } else {
                    severity = "UNKNOWN";
                }

                String type;
                if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
                    type = "GENERAL";
                } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
                    type = "VALIDATION";
                } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
                    type = "PERFORMANCE";
                } else {
                    type = "UNKNOWN";
                }

                VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                System.err.format(
                        "%s %s: [%s]\n\t%s\n",
                        type, severity, data.pMessageIdNameString(), data.pMessageString()
                );

                /*
                 * false indicates that layer should not bail-out of an
                 * API call that had validation failures. This may mean that the
                 * app dies inside the driver due to invalid parameter(s).
                 * That's what would happen without validation layers, so we'll
                 * keep that behavior here.
                 */
                return VK_FALSE;
            }
    );
    private long UINT64_MAX = 0xFFFFFFFFL;
    private int currentFrame = 0;

    public void run(int width, int height) {
        VulkanUtils.initVk();
        window = new VulkanWindow(width, height, "Test");
        renderer = new VulkanRenderer(window, "title", dbgCb);
        createRenderPass();
        createVertexBuffer();
        createPipeline();
        createFrameBuffers();
        createCommandPool();
        createCommandBuffer();
        createSyncObjects();
        loop();
    }

    private void createVertexBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            Vertex[] vertices = new Vertex[]{
                    new Vertex(new Vector2f(0.f, -0.5f), new Vector4f(1.f, 0.f, 0.f, 1.f)),
                    new Vertex(new Vector2f(0.5f, 0.5f), new Vector4f(0.f, 1.f, 0.f, 1.f)),
                    new Vertex(new Vector2f(-0.5f, 0.5f), new Vector4f(0.f, 0.f, 1.f, 1.f)),
            };

            ByteBuffer buffer = stack.calloc(Vertex.SIZE_OF * vertices.length);

            for (Vertex vertex : vertices) {
                buffer.putFloat(vertex.getPos().x);
                buffer.putFloat(vertex.getPos().y);

                buffer.putFloat(vertex.getColor().x);
                buffer.putFloat(vertex.getColor().y);
                buffer.putFloat(vertex.getColor().z);
                buffer.putFloat(vertex.getColor().w);
            }

            // Used only temporarily for setting the data in the RAM (visible by the CPU), which then will be copied to the GPU
            VulkanBuffers.Buffer stagingBuffer = renderer.getDevices().createBuffer(VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_SHARING_MODE_EXCLUSIVE,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, buffer);

            vertexBuffer = renderer.getDevices().createBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, VK_SHARING_MODE_EXCLUSIVE,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, buffer.capacity());

            renderer.getDevices().copyBuffer(stagingBuffer, vertexBuffer,buffer.capacity());

        }
    }


    private void memcpy(ByteBuffer buffer, Vertex[] vertices) {
        for (Vertex vertex : vertices) {
            buffer.putFloat(vertex.getPos().x);
            buffer.putFloat(vertex.getPos().y);

            buffer.putFloat(vertex.getColor().x);
            buffer.putFloat(vertex.getColor().y);
            buffer.putFloat(vertex.getColor().z);
            buffer.putFloat(vertex.getColor().z);
        }
    }


    public void createFrameBuffers() {
        frameBuffers = new VulkanFrameBuffer(renderer.getDevices().getLogicalDevice(), renderPass, renderer.getSwapChain());
    }

    public void createCommandPool() {
        commandPool = new VulkanCmdPool(renderer.getDevices().getLogicalDevice(),
                renderer.getDevices().getPhysicalDevice().getQueueFamilyIndices().getGraphicsFamily().get());
    }

    public void createCommandBuffer() {

        int commandBufferCount = renderer.getSwapChain().getSwapChainBuffers().length;

        commandBuffers = new ArrayList<>(commandBufferCount);


        try (MemoryStack stack = MemoryStack.stackPush()) {

            for (int i = 0; i < commandBufferCount; i++) {
                commandBuffers.add(new VulkanCommandBuffer(renderer.getDevices().getLogicalDevice(), commandPool, false));
            }

            VkCommandBufferBeginInfo cmdBufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack);
            cmdBufferBeginInfo.sType$Default();

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassBeginInfo.sType$Default();
            renderPassBeginInfo.renderPass(renderPass.getRenderPassPtr());

            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(renderer.getSwapChain().getExtent());

            renderPassBeginInfo.renderArea(renderArea);

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color().float32(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));
            renderPassBeginInfo.pClearValues(clearValues);

            for (int i = 0; i < commandBuffers.size(); i++) {

                VulkanCommandBuffer buffer = commandBuffers.get(i);

                renderPassBeginInfo.framebuffer(frameBuffers.getFrameBuffers()[i]);

                buffer.beginCommandBuffer(cmdBufferBeginInfo);

                buffer.beginRenderPass(renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
                {
                    buffer.bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

                    buffer.bindVertexBuffers(0, vertexBuffer.pBuffer, 0);

                    buffer.draw(3, 1, 0, 0);
                }
                buffer.endRenderPass();

                buffer.endCommandBuffer();
            }

        }
    }

    public void createSyncObjects() {

        imagesInFlight = new HashMap<>(renderer.getSwapChain().getSwapChainImagesSize());

        for (int i = 0; i < maxFramesInFlight; i++) {
            VulkanSemaphore imageAvailableSemaphore = new VulkanSemaphore(renderer.getDevices().getLogicalDevice());
            VulkanSemaphore renderFinishedSemaphore = new VulkanSemaphore(renderer.getDevices().getLogicalDevice());

            VulkanFence fence = new VulkanFence(renderer.getDevices().getLogicalDevice(), VK_FENCE_CREATE_SIGNALED_BIT);

            inFlightFrames.add(new VulkanFrame(imageAvailableSemaphore, renderFinishedSemaphore, fence));
        }

    }

    public void loop() {

        while (!glfwWindowShouldClose(window.getWindowPtr())) {

            try (MemoryStack stack = MemoryStack.stackPush()) {

                // Draw Section

                VulkanFrame thisFrame = inFlightFrames.get(currentFrame);

                vkWaitForFences(renderer.getDevices().getVkDevice(), thisFrame.getFencePtr(), true, UINT64_MAX);

                IntBuffer pImageIndex = stack.mallocInt(1);

                vkAcquireNextImageKHR(renderer.getDevices().getVkDevice(),
                        renderer.getSwapChain().getSwapchainPtr(),
                        UINT64_MAX,
                        thisFrame.getImageAvailableSemaphorePtr().get(0),
                        VK_NULL_HANDLE,
                        pImageIndex);
                final int imageIndex = pImageIndex.get(0);

                if (imagesInFlight.containsKey(imageIndex)) {
                    vkWaitForFences(renderer.getDevices().getVkDevice(), imagesInFlight.get(imageIndex).getFencePtr(), true, UINT64_MAX);
                }

                imagesInFlight.put(imageIndex, thisFrame);

                VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
                submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

                submitInfo.waitSemaphoreCount(1);
                submitInfo.pWaitSemaphores(thisFrame.getImageAvailableSemaphorePtr());
                submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

                submitInfo.pSignalSemaphores(thisFrame.getRenderFinishedSemaphorePtr());

                submitInfo.pCommandBuffers(stack.pointers(commandBuffers.get(imageIndex).getVkCommandBuffer()));

                vkResetFences(renderer.getDevices().getVkDevice(), thisFrame.getFencePtr());

                if (vkQueueSubmit(renderer.getDevices().getLogicalDevice().getGraphicsQueue(), submitInfo, thisFrame.getFencePtr().get(0)) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to submit draw command buffer");
                }

                // Presentation Section

                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
                presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

                presentInfo.pWaitSemaphores(thisFrame.getRenderFinishedSemaphorePtr());

                presentInfo.swapchainCount(1);
                presentInfo.pSwapchains(stack.longs(renderer.getSwapChain().getSwapchainPtr()));

                presentInfo.pImageIndices(pImageIndex);

                vkQueuePresentKHR(renderer.getDevices().getLogicalDevice().getPresentQueue(), presentInfo);

                currentFrame = (currentFrame + 1) % maxFramesInFlight;

                glfwPollEvents();
            }
        }

        // Wait for the device to complete all operations before releasing resources
        vkDeviceWaitIdle(renderer.getDevices().getVkDevice());

        vertexBuffer.destroy(renderer.getDevices().getLogicalDevice());
    }

    public void createPipeline() {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            SPIRVShaderCode fragShader = compileShaderFileIntoSPIRV("shaders/fragment_shader.frag", ShaderUtils.ShaderType.FRAGMENT_SHADER);
            SPIRVShaderCode vertShader = compileShaderFileIntoSPIRV("shaders/vertex_shader.vert", ShaderUtils.ShaderType.VERTEX_SHADER);

            ShaderModule vertShaderModule = createShaderModule(renderer.getDevices().getLogicalDevice(), vertShader);
            ShaderModule fragShaderModule = createShaderModule(renderer.getDevices().getLogicalDevice(), fragShader);

            ShaderModule[] modules = new ShaderModule[]{vertShaderModule, fragShaderModule};

            // Binding the vertex Buffer ...
            VulkanBuffers.Attribute[] attributes = new VulkanBuffers.Attribute[]{
                    new VulkanBuffers.Attribute(0, VK_FORMAT_R32G32_SFLOAT, 0, 8),
                    new VulkanBuffers.Attribute(1, VK_FORMAT_R32G32B32A32_SFLOAT, 8, 16),
            };

            VulkanBuffers.VertexBuffer vertexBuffer = new VulkanBuffers.VertexBuffer(0, attributes);

            graphicsPipeline = new VulkanGraphicsPipeline(modules);

            graphicsPipeline.setupVertexStage(vertexBuffer.createAttributeDescriptions(),
                            vertexBuffer.createBindingDescription())                                // ===> VERTEX STAGE <===
                    .setupInputAssembly(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)                        // ===> ASSEMBLY STAGE <===
                    .setupDefaultViewport(renderer.getSwapChain().getExtent())                      // ===> VIEWPORT & SCISSOR <===
                    .setupDefaultRasterization()                                                    // ===> RASTERIZATION STAGE <===
                    .setupDefaultMultiSampling(VK_SAMPLE_COUNT_1_BIT, false)    // ===> MULTISAMPLING <===
                    .setupColorBlending(false)                                                      // ===> COLOR BLENDING <===
                    .initializePipeline(renderer.getDevices().getLogicalDevice(), renderPass);      // ===> PIPELINE CREATION <===

            vertShader.free();
            fragShader.free();
        }
    }

    public void createRenderPass() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.malloc(1, stack);
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.flags(0);

            int format = renderer.getDevices().getPhysicalDevice().getSwapChainSupportDetails().chooseSwapSurfaceFormat().format();

            colorAttachment.format(format);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer references = VkAttachmentReference.malloc(1, stack);
            references.attachment(0);
            references.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            // For Some reason you have to use calloc here. Maybe it has to do with not initializing values to null
            // Better stick to it
            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(references);
            subpass.pDepthStencilAttachment(null);

            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.malloc(1, stack);
            dependencies.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependencies.dstSubpass(0);
            dependencies.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependencies.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependencies.srcAccessMask(0);
            dependencies.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_COLOR_ATTACHMENT_READ_BIT);

            renderPass = new VulkanRenderPass(renderer.getDevices().getLogicalDevice(), colorAttachment, subpass, dependencies);
        }
    }
}
