import lib.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import render.VulkanRenderer;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static lib.ShaderUtils.*;

public class Application {

    private VulkanRenderPass renderPass;
    private VulkanRenderer renderer;
    private VulkanGraphicsPipeline graphicsPipeline;
    private VulkanFrameBuffer frameBuffers;
    private VulkanCMDPool commandPool;
    private List<VulkanCommandBuffer> commandBuffers;


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

    public void run(int width, int height) {
        VulkanUtils.initVk();
        VulkanWindow window = new VulkanWindow(width, height, "Test");
        renderer = new VulkanRenderer(window, "title", dbgCb);
        createRenderPass();
        createPipeline();
        createFrameBuffers();
        createCommandPool();
        createCommandBuffer();
        createSyncObjects();
    }


    public void createFrameBuffers() {
        frameBuffers = new VulkanFrameBuffer(renderer.getLogicalDevice(),renderPass,renderer.getSwapChain());
    }

    public void createCommandPool() {
        commandPool = new VulkanCMDPool(renderer.getLogicalDevice(),renderer.getPhysicalDevice().getQueueFamilyIndices().getGraphicsFamily().get());
    }

    public void createCommandBuffer() {

        int commandBufferCount = renderer.getSwapChain().getSwapChainBuffers().length;

        commandBuffers = new ArrayList<>(commandBufferCount);


        try (MemoryStack stack = MemoryStack.stackPush()) {

            for (int i = 0; i < commandBufferCount; i++) {
                commandBuffers.add(new VulkanCommandBuffer(renderer.getLogicalDevice(), commandPool,false));
            }

            VkCommandBufferBeginInfo cmdBufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack);
            cmdBufferBeginInfo.sType$Default();

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassBeginInfo.sType$Default();
            renderPassBeginInfo.renderPass(renderPass.getRenderPassPtr());

            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0,0));
            renderArea.extent(renderer.getSwapChain().getExtent());

            renderPassBeginInfo.renderArea(renderArea);

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color().float32(stack.floats(0.0f,0.0f,0.0f,0.0f));
            renderPassBeginInfo.pClearValues(clearValues);

            for (int i = 0; i < commandBuffers.size(); i++) {

                VulkanCommandBuffer buffer = commandBuffers.get(i);

                renderPassBeginInfo.framebuffer(frameBuffers.getFrameBuffers()[i]);

                buffer.beginCommandBuffer(cmdBufferBeginInfo);

                buffer.beginRenderPass(renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
                {
                    buffer.bindPipeline(VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
                    buffer.draw(3, 1,0,0);
                }
                buffer.endRenderPass();

                buffer.endCommandBuffer();
            }

        }
    }

    public void createSyncObjects() {

    }

    public void createPipeline() {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            SPIRVShaderCode fragShader = compileShaderFileIntoSPIRV("shaders/fragment_shader.frag", ShaderUtils.ShaderType.FRAGMENT_SHADER);
            SPIRVShaderCode vertShader = compileShaderFileIntoSPIRV("shaders/vertex_shader.vert", ShaderUtils.ShaderType.VERTEX_SHADER);

            ShaderModule vertShaderModule = createShaderModule(renderer.getLogicalDevice(), vertShader);
            ShaderModule fragShaderModule = createShaderModule(renderer.getLogicalDevice(), fragShader);

            ShaderModule[] modules = new ShaderModule[]{vertShaderModule,fragShaderModule};

            graphicsPipeline = new VulkanGraphicsPipeline(modules);

            // ===> VERTEX STAGE <===
            graphicsPipeline.setupVertexStage();

            // ===> ASSEMBLY STAGE <===
            graphicsPipeline.setupInputAssembly(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);

            // ===> VIEWPORT & SCISSOR
            graphicsPipeline.setupDefaultViewport(renderer.getSwapChain().getExtent());

            // ===> RASTERIZATION STAGE <===
            graphicsPipeline.setupDefaultRasterization();

            // ===> MULTISAMPLING <===
            graphicsPipeline.setupDefaultMultiSampling(VK_SAMPLE_COUNT_1_BIT, false);

            // ===> COLOR BLENDING <===
            graphicsPipeline.setupColorBlending(false);

            // ===> PIPELINE CREATION <===
            graphicsPipeline.initializePipeline(renderer.getLogicalDevice(),renderPass);

            vertShader.free();
            fragShader.free();
        }
    }

    public void createRenderPass() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.malloc(1,stack);
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.flags(0);

            int format = renderer.getPhysicalDevice().getSwapChainSupportDetails().chooseSwapSurfaceFormat().format();

            colorAttachment.format(format);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer references = VkAttachmentReference.malloc(1,stack);
            references.attachment(0);
            references.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            // For Some reason you have to use calloc here. Maybe it has to do with not initializing values to null
            // Better stick to it
            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1,stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(references);
            subpass.pDepthStencilAttachment(null);

            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.malloc(1,stack);
            dependencies.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependencies.dstSubpass(0);
            dependencies.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependencies.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependencies.srcAccessMask(0);
            dependencies.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_COLOR_ATTACHMENT_READ_BIT);

            renderPass = new VulkanRenderPass(renderer.getLogicalDevice(),colorAttachment,subpass,dependencies);
        }
    }
}
