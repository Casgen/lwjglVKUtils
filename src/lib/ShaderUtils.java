package lib;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.ClassLoader.getSystemClassLoader;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

public class ShaderUtils {

    public static SPIRVShaderCode compileShaderFileIntoSPIRV(String shaderFile, ShaderType shaderType) {
        return compileShaderAbsoluteFile(getSystemClassLoader().getResource(shaderFile).toExternalForm(), shaderType);
    }

    public static SPIRVShaderCode compileShaderAbsoluteFile(String shaderFile, ShaderType shaderType) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            return compileShader(shaderFile, source, shaderType);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SPIRVShaderCode compileShader(String filename, String source, ShaderType shaderType) {

        long compiler = shaderc_compiler_initialize();

        if (compiler == NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }

        long result = shaderc_compile_into_spv(compiler, source, shaderType.type, filename, "main", NULL);

        if (result == NULL) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        }

        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            throw new RuntimeException("Failed to compile shader " + filename + "into SPIR-V:\n " + shaderc_result_get_error_message(result));
        }

        shaderc_compiler_release(compiler);

        return new SPIRVShaderCode(result, shaderc_result_get_bytes(result), shaderType);
    }

    public enum ShaderType {
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader),
        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        TESS_CONTROL_SHADER(shaderc_glsl_tess_control_shader),
        TESS_EVAL_SHADER(shaderc_tess_evaluation_shader),
        MESH_SHADER(shaderc_glsl_mesh_shader),
        TASK_SHADER(shaderc_glsl_task_shader);

        public final int type;

        ShaderType(int type) {
            this.type = type;
        }
    }

    public static final class ShaderModule {

        public ShaderType type;
        public long modulePtr;

        public ShaderModule(long modulePtr, ShaderType type) {
            this.modulePtr = modulePtr;
            this.type = type;
        }
    }

    public static final class SPIRVShaderCode implements NativeResource {

        private final long handle;
        private ByteBuffer bytecode;

        private ShaderType type;

        public SPIRVShaderCode(long handle, ByteBuffer bytecode, ShaderType type) {
            this.handle = handle;
            this.bytecode = bytecode;
            this.type = type;
        }

        public ByteBuffer bytecode() {
            return bytecode;
        }

        @Override
        public void free() {
            shaderc_result_release(handle);
            bytecode = null; // Help the GC
        }
    }

    public static ShaderModule createShaderModule(VulkanLogicalDevice device, SPIRVShaderCode spirvCode) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode.bytecode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            VulkanUtils.check(vkCreateShaderModule(device.getVkDevice(), createInfo, null, pShaderModule));

            return new ShaderModule(pShaderModule.get(0), spirvCode.type);
        }
    }
}
