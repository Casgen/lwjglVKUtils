package lib;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static lib.ShaderUtils.*;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanGraphicsPipeline implements VulkanResource {

    //TODO: Later abstract it for different kinds of pipelines
    private long pipelinePtr;
    private long pipelineLayoutPtr;

    private ShaderModule[] modules;
    private VkPipelineShaderStageCreateInfo.Buffer shaderStages;
    private VkPipelineVertexInputStateCreateInfo vertexInputCreateInfo;
    private VkPipelineInputAssemblyStateCreateInfo inputAssemblyCreateInfo;
    private VkPipelineMultisampleStateCreateInfo multisampleCreateInfo;
    private VkPipelineViewportStateCreateInfo viewportStateCreateInfo;
    private VkPipelineColorBlendStateCreateInfo colorBlendStateCreateInfo;
    private VkPipelineRasterizationStateCreateInfo rasterizerCreateInfo;


    /**
     * Creates a new VulkanGraphicsPipeline object.
     * @param modules - Array of compiled shader modules;
     */
    public VulkanGraphicsPipeline(ShaderModule[] modules) {

        if (modules.length < 2)
            throw new IllegalStateException("There are not enough mandatory shader modules to create the pipeline!" +
                    "Ensure that you have added at least the Vertex and Fragment shader into the array.");

        this.modules = modules;

        shaderStages = VkPipelineShaderStageCreateInfo.calloc(modules.length);

        for (int i = 0; i < modules.length; i++) {
            shaderStages.position(i);

            shaderStages.sType$Default();
            shaderStages.stage(chooseStage(modules[i].type));
            shaderStages.module(modules[i].modulePtr);
            shaderStages.pName(memUTF8("main"));
        }

        //TODO: Could be set also with flip() function, not sure if it leaves with the same behavior though.
        shaderStages.position(0);
    }

    /**
     * Sets up the vertex stage without any
     * @return this
     */
    public VulkanGraphicsPipeline setupVertexStage() {
        vertexInputCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc();
        vertexInputCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);

        return this;
    }

    public VulkanGraphicsPipeline setupVertexStage(VkVertexInputAttributeDescription.Buffer attributeDesc,
                                                   VkVertexInputBindingDescription.Buffer bindingDesc) {

        vertexInputCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc();
        vertexInputCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
        vertexInputCreateInfo.pVertexAttributeDescriptions(attributeDesc);
        vertexInputCreateInfo.pVertexBindingDescriptions(bindingDesc);

        return this;
    }

    /**
     * Sets up the input assembly stage of the pipeline. Restart of primitives is disabled.
     *
     * @param topology What topology for the vertices will the pipeline work with (for ex. VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
     * @return this
     */
    public VulkanGraphicsPipeline setupInputAssembly(int topology) {
        inputAssemblyCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc();
        inputAssemblyCreateInfo.sType$Default();
        inputAssemblyCreateInfo.topology(topology);
        inputAssemblyCreateInfo.primitiveRestartEnable(false);

        return this;
    }

    /**
     * Sets up the input assembly stage of the pipeline
     *
     * @param topology What topology for the vertices will the pipeline work with (for ex. VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
     * @param enableRestartPrimitive Enables or disables the ability to restart the primitive chain to avoid degenerate triangles
     * @return this
     */

    public VulkanGraphicsPipeline setupInputAssembly(int topology, boolean enableRestartPrimitive) {
        inputAssemblyCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc();
        inputAssemblyCreateInfo.sType$Default();
        inputAssemblyCreateInfo.topology(topology);
        inputAssemblyCreateInfo.primitiveRestartEnable(enableRestartPrimitive);

        return this;
    }

    /**
     * Sets up a default Viewport used for the window (respectively to its width and height)
     *
     * @param extent a VkExtent2D (Ideally the Extent should match the resolution of the window if desired)
     */
    public VulkanGraphicsPipeline setupDefaultViewport(VkExtent2D extent) {
        VkViewport.Buffer viewport = VkViewport.calloc(1);
        viewport.x(0.0f);
        viewport.y(0.0f);
        viewport.width(extent.width());
        viewport.height(extent.height());
        viewport.minDepth(0.0f);
        viewport.maxDepth(1.0f);

        VkRect2D.Buffer scissor = VkRect2D.calloc(1);
        scissor.offset(VkOffset2D.calloc().set(0, 0));
        scissor.extent(extent);

        viewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc();
        viewportStateCreateInfo.sType$Default();
        viewportStateCreateInfo.pViewports(viewport);
        viewportStateCreateInfo.pScissors(scissor);

        return this;
    }

    /**
     * Sets up the Multi Sampling state of the graphics pipeline.
     *
     * @param sampleCount Enum of the VK_SAMPLE_COUNT. ATTENTION: The sample count should match the render passes sample count!
     */

    public VulkanGraphicsPipeline setupDefaultMultiSampling(int sampleCount, boolean isSampleShadingEnabled) {
        multisampleCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc();

        multisampleCreateInfo.sType$Default();
        multisampleCreateInfo.sampleShadingEnable(isSampleShadingEnabled);
        multisampleCreateInfo.rasterizationSamples(sampleCount);

        return this;
    }

    /**
     * Sets up the color blending stage with default settings.
     *
     * @param isEnabled - Whether the stage will be enabled or not
     * @return this
     */
    public VulkanGraphicsPipeline setupColorBlending(boolean isEnabled) {
        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1);
        colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
        colorBlendAttachment.blendEnable(isEnabled);

        colorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc();
        colorBlendStateCreateInfo.sType$Default();
        colorBlendStateCreateInfo.logicOpEnable(false);
        colorBlendStateCreateInfo.logicOp(VK_LOGIC_OP_COPY);
        colorBlendStateCreateInfo.pAttachments(colorBlendAttachment);

        FloatBuffer floats = FloatBuffer.allocate(4);
        floats.put(new float[]{0.0f, 0.0f, 0.0f, 0.0f});

        colorBlendStateCreateInfo.blendConstants(floats);

        return this;
    }

    /**
     * Sets up the rasterization stage of the pipeline with default settings.
     *
     * @return this
     */
    public VulkanGraphicsPipeline setupDefaultRasterization() {
        rasterizerCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc();
        rasterizerCreateInfo.sType$Default();
        rasterizerCreateInfo.depthClampEnable(false);
        rasterizerCreateInfo.rasterizerDiscardEnable(false);
        rasterizerCreateInfo.polygonMode(VK_POLYGON_MODE_FILL);
        rasterizerCreateInfo.lineWidth(1.0f);
        rasterizerCreateInfo.cullMode(VK_CULL_MODE_BACK_BIT);
        rasterizerCreateInfo.frontFace(VK_FRONT_FACE_CLOCKWISE);
        rasterizerCreateInfo.depthBiasEnable(false);

        return this;
    }

    /**
     * Initializes and creates the Pipeline with previous setups.
     *
     * @param device     - Vulkan Logical device object
     * @param renderPass - Vullan Render Pass object
     */
    public void initializePipeline(VulkanLogicalDevice device, VulkanRenderPass renderPass) {

        if (device == null)
            throw new NullPointerException("VulkanRenderPass is null!");

        if (renderPass == null)
            throw new NullPointerException("VulkanLogicalDevice is null!");

        //Creation of the Pipeline
        try (MemoryStack stack = MemoryStack.stackPush()) {

            VkPipelineLayoutCreateInfo layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc();
            layoutCreateInfo.sType$Default();

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if (vkCreatePipelineLayout(device.getVkDevice(), layoutCreateInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayoutPtr = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputCreateInfo);
            pipelineInfo.pInputAssemblyState(inputAssemblyCreateInfo);
            pipelineInfo.pViewportState(viewportStateCreateInfo);
            pipelineInfo.pRasterizationState(rasterizerCreateInfo);
            pipelineInfo.pMultisampleState(multisampleCreateInfo);
            pipelineInfo.pColorBlendState(colorBlendStateCreateInfo);
            pipelineInfo.layout(pPipelineLayout.get(0));
            pipelineInfo.renderPass(renderPass.getRenderPassPtr());
            pipelineInfo.subpass(0);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if (vkCreateGraphicsPipelines(device.getVkDevice(), VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            pipelinePtr = pGraphicsPipeline.get(0);

            //Release Resources
            for (ShaderModule module : modules) {
                vkDestroyShaderModule(device.getVkDevice(), module.modulePtr, null);
            }
        }
    }

    private int chooseStage(ShaderType shaderType) {
        return switch (shaderType) {
            case VERTEX_SHADER -> VK_SHADER_STAGE_VERTEX_BIT;
            case FRAGMENT_SHADER -> VK_SHADER_STAGE_FRAGMENT_BIT;
            case GEOMETRY_SHADER -> VK_SHADER_STAGE_GEOMETRY_BIT;
            case TESS_EVAL_SHADER -> VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT;
            case TESS_CONTROL_SHADER -> VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT;
            default ->
                    throw new IllegalStateException(String.format("Graphics pipeline creation failed! Could not resolve the ShaderType! Value is %d", shaderType.type));
        };
    }

    public long getPipelinePtr() {
        return pipelinePtr;
    }

    @Override
    public void destroy(VulkanLogicalDevice device) {

        vkDestroyPipeline(device.getVkDevice(), pipelinePtr, null);
        vkDestroyPipelineLayout(device.getVkDevice(), pipelineLayoutPtr, null);

        shaderStages.free();
        vertexInputCreateInfo.free();
        inputAssemblyCreateInfo.free();
        multisampleCreateInfo.free();
        viewportStateCreateInfo.free();
        colorBlendStateCreateInfo.free();
        rasterizerCreateInfo.free();
    }
}
