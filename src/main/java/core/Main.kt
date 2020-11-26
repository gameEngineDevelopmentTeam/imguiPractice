package core

import gui.ExampleUi
import imgui.ImFontConfig
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiConfigFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*


class ImGuiGlfwExample {
    private var windowPtr // pointer to the current GLFW window
            : Long = 0

    // LWJGJ3 window backend
    private val imGuiGlfw = ImGuiImplGlfw()

    // LWJGL3 renderer (SHOULD be initialized)
    private val imGuiGl3 = ImGuiImplGl3()
    private var glslVersion: String? = null // We can initialize our renderer with different versions of the GLSL

    // User UI to render
    private val exampleUi: ExampleUi = ExampleUi()

    @Throws(Exception::class)
    fun run() {
        setupGlfw()
        setupImGui()

        // Method initializes GLFW backend.
        // This method SHOULD be called after you've setup GLFW.
        // ImGui context should be created as well.
        imGuiGlfw.init(windowPtr, true)
        // Method initializes LWJGL3 renderer.
        // This method SHOULD be called after you've initialized your ImGui configuration (fonts and so on).
        // ImGui context should be created as well.
        imGuiGl3.init(glslVersion)
        loop()

        // You should clean up after yourself in reverse order.
        imGuiGl3.dispose()
        imGuiGlfw.dispose()
        ImGui.destroyContext()
        disposeWindow()
    }

    // Initialize GLFW + create an OpenGL context.
    // All code is mostly a copy-paste from the official LWJGL3 "Get Started": https://www.lwjgl.org/guide
    private fun setupGlfw() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE) // the window will stay hidden after creation
        decideGlGlslVersions()
        windowPtr = GLFW.glfwCreateWindow(1280, 768, "Dear ImGui+LWJGL Example", MemoryUtil.NULL, MemoryUtil.NULL)
        if (windowPtr == MemoryUtil.NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }
        MemoryStack.stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            GLFW.glfwGetWindowSize(windowPtr, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode: GLFWVidMode = Objects.requireNonNull(GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())) ?: return

            // Center the window
            GLFW.glfwSetWindowPos(windowPtr, (vidmode.width() - pWidth[0]) / 2, (vidmode.height() - pHeight[0]) / 2)
        }
        GLFW.glfwMakeContextCurrent(windowPtr) // Make the OpenGL context current
        GLFW.glfwSwapInterval(GLFW.GLFW_TRUE) // Enable v-sync
        GLFW.glfwShowWindow(windowPtr) // Make the window visible

        // IMPORTANT!!
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()
    }

    private fun decideGlGlslVersions() {
        val isMac = System.getProperty("os.name").toLowerCase().contains("mac")
        if (isMac) {
            glslVersion = "#version 150"
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE) // 3.2+ only
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE) // Required on Mac
        } else {
            glslVersion = "#version 130"
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0)
        }
    }

    // Initialize Dear ImGui.
    private fun setupImGui() {
        // IMPORTANT!!
        // This line is critical for Dear ImGui to work.
        ImGui.createContext()

        // ------------------------------------------------------------
        // Initialize ImGuiIO config
        val io = ImGui.getIO()
        io.iniFilename = null // We don't want to save .ini file
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard) // Enable Keyboard Controls
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable) // Enable Docking
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable) // Enable Multi-Viewport / Platform Windows
        io.configViewportsNoTaskBarIcon = true

        // ------------------------------------------------------------
        // Fonts configuration
        // Read: https://raw.githubusercontent.com/ocornut/imgui/master/docs/FONTS.txt
        val fontAtlas = io.fonts
        val fontConfig = ImFontConfig() // Natively allocated object, should be explicitly destroyed

        // Glyphs could be added per-font as well as per config used globally like here
        fontConfig.glyphRanges = fontAtlas.glyphRangesCyrillic

        // Add a default font, which is 'ProggyClean.ttf, 13px'
        fontAtlas.addFontDefault()

        // Fonts merge example
        fontConfig.mergeMode = true // When enabled, all fonts added with this config would be merged with the previously added font
        fontConfig.pixelSnapH = true
        fontAtlas.addFontFromMemoryTTF(loadFromResources("basis33.ttf"), 16f, fontConfig)
        fontConfig.mergeMode = false
        fontConfig.pixelSnapH = false

        // Fonts from file/memory example
        // We can add new fonts from the file system
        fontAtlas.addFontFromFileTTF("src/test/resources/Righteous-Regular.ttf", 14f, fontConfig)
        fontAtlas.addFontFromFileTTF("src/test/resources/Righteous-Regular.ttf", 16f, fontConfig)

        // Or directly from the memory
        fontConfig.setName("Roboto-Regular.ttf, 14px") // This name will be displayed in Style Editor
        fontAtlas.addFontFromMemoryTTF(loadFromResources("Roboto-Regular.ttf"), 14f, fontConfig)
        fontConfig.setName("Roboto-Regular.ttf, 16px") // We can apply a new config value every time we add a new font
        fontAtlas.addFontFromMemoryTTF(loadFromResources("Roboto-Regular.ttf"), 16f, fontConfig)
        fontConfig.destroy() // After all fonts were added we don't need this config more

        // When viewports are enabled we tweak WindowRounding/WindowBg so platform windows can look identical to regular ones.
        if (io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            val style = ImGui.getStyle()
            style.windowRounding = 0.0f
            style.setColor(ImGuiCol.WindowBg, ImGui.getColorU32(ImGuiCol.WindowBg, 1f))
        }
    }

    // Main application loop
    @Throws(Exception::class)
    private fun loop() {
        // Run the rendering loop until the user has attempted to close the window
        while (!GLFW.glfwWindowShouldClose(windowPtr)) {
            startFrame()

            // Any Dear ImGui code SHOULD go between ImGui.newFrame()/ImGui.render() methods
            imGuiGlfw.newFrame()
            ImGui.newFrame()
            exampleUi.render()
            ImGui.render()
            endFrame()
        }
    }

    private fun startFrame() {
        // Set the clear color and clear the window
        GL11.glClearColor(exampleUi.backgroundColor.get(0), exampleUi.backgroundColor.get(1), exampleUi.backgroundColor.get(2), 0.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
    }

    private fun endFrame() {
        // After Dear ImGui prepared a draw data, we use it in the LWJGL3 renderer.
        // At that moment ImGui will be rendered to the current OpenGL context.
        imGuiGl3.renderDrawData(ImGui.getDrawData())
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            val backupWindowPtr = GLFW.glfwGetCurrentContext()
            ImGui.updatePlatformWindows()
            ImGui.renderPlatformWindowsDefault()
            GLFW.glfwMakeContextCurrent(backupWindowPtr)
        }
        GLFW.glfwSwapBuffers(windowPtr)
        GLFW.glfwPollEvents()
    }

    private fun disposeWindow() {
        Callbacks.glfwFreeCallbacks(windowPtr)
        GLFW.glfwDestroyWindow(windowPtr)
        GLFW.glfwTerminate()
        Objects.requireNonNull(GLFW.glfwSetErrorCallback(null))!!.free()
    }

    private fun loadFromResources(fileName: String): ByteArray {
        try {
            Objects.requireNonNull(javaClass.classLoader.getResourceAsStream(fileName)).use { `is` ->
                ByteArrayOutputStream().use { buffer ->
                    val data = ByteArray(16384)
                    var nRead: Int
                    while (`is`.read(data, 0, data.size).also { nRead = it } != -1) {
                        buffer.write(data, 0, nRead)
                    }
                    return buffer.toByteArray()
                }
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }
}
fun main(args: Array<String>) {
    ImGuiGlfwExample().run()
}