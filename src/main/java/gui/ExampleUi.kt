package gui

import imgui.ImColor
import imgui.flag.*
import imgui.internal.ImGui
import imgui.internal.flag.ImGuiDockNodeFlags
import imgui.type.ImBoolean
import imgui.type.ImString
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO


internal class ExampleUi {
    // Test data for payload
    private var dropTargetText = "Drop Here"

    // Resizable input example
    private val resizableStr = ImString(5)

    // Toggles
    private val showBottomDockedWindow = ImBoolean(true)
    private val showDemoWindow = ImBoolean()

    // Attach image example
    private var dukeTexture = 0
    private var isBottomDockedWindowInit = false

    // To modify background color dynamically
    val backgroundColor = floatArrayOf(0.5f, 0f, 0f)

    @Throws(Exception::class)
    fun render() {
        val dockspaceId = ImGui.getID("MyDockSpace")
        showDockSpace(dockspaceId)
        setupBottomDockedWindow(dockspaceId)
        showBottomDockedWindow()
        val mainViewport = ImGui.getMainViewport()
        ImGui.setNextWindowSize(600f, 300f, ImGuiCond.Once)
        ImGui.setNextWindowPos(mainViewport.workPosX + 10, mainViewport.workPosY + 10, ImGuiCond.Once)
        ImGui.begin("Custom window") // Start Custom window
        showWindowImage()
        showToggles()
        ImGui.separator()
        showDragNDrop()
        showBackgroundPicker()
        ImGui.separator()
        showResizableInput()
        ImGui.separator()
        ImGui.newLine()
        showDemoLink()
        ImGui.end() // End Custom window
        if (showDemoWindow.get()) {
            ImGui.showDemoWindow(showDemoWindow)
        }
    }

    private fun showDockSpace(dockspaceId: Int) {
        val mainViewport = ImGui.getMainViewport()
        val windowFlags = (ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoResize
                or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoBringToFrontOnFocus or ImGuiWindowFlags.NoNavFocus or ImGuiWindowFlags.NoBackground)
        ImGui.setNextWindowPos(mainViewport.workPosX, mainViewport.workPosY)
        ImGui.setNextWindowSize(mainViewport.workSizeX, mainViewport.workSizeY)
        ImGui.setNextWindowViewport(mainViewport.id)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
        ImGui.begin("Dockspace Demo", windowFlags)
        ImGui.popStyleVar(3)
        ImGui.dockSpace(dockspaceId, 0f, 0f, ImGuiDockNodeFlags.PassthruCentralNode)
        ImGui.end()
    }

    private fun setupBottomDockedWindow(dockspaceId: Int) {
        if (!isBottomDockedWindowInit) {
            ImGui.dockBuilderRemoveNode(dockspaceId)
            ImGui.dockBuilderAddNode(dockspaceId, ImGuiDockNodeFlags.DockSpace)
            val dockIdBottom = ImGui.dockBuilderSplitNode(dockspaceId, ImGuiDir.Down, .25f, null, null)
            ImGui.dockBuilderDockWindow("Bottom Docked Window", dockIdBottom)
            ImGui.dockBuilderSetNodeSize(dockIdBottom, 150f, 150f)
            ImGui.dockBuilderFinish(dockspaceId)
            isBottomDockedWindowInit = true
        }
    }

    private fun showBottomDockedWindow() {
        if (showBottomDockedWindow.get()) {
            ImGui.begin("Bottom Docked Window", showBottomDockedWindow, ImGuiWindowFlags.AlwaysAutoResize)
            ImGui.text("An example of how to create docked windows.")
            ImGui.end()
        }
    }

    @Throws(IOException::class)
    private fun showWindowImage() {
        if (dukeTexture == 0) {
            dukeTexture = loadTexture(ImageIO.read(File("src/test/resources/Duke_waving.png")))
        }

        // Draw an image in the bottom-right corner of the window
        val xPoint = ImGui.getWindowPosX() + ImGui.getWindowSizeX() - 100
        val yPoint = ImGui.getWindowPosY() + ImGui.getWindowSizeY()
        ImGui.getWindowDrawList().addImage(dukeTexture, xPoint, yPoint - 180, xPoint + 100, yPoint)
    }

    private fun showToggles() {
        ImGui.checkbox("Show Demo Window", showDemoWindow)
        ImGui.checkbox("Show Bottom Docked Window", showBottomDockedWindow)
        if (ImGui.button("Reset Bottom Dock Window")) {
            isBottomDockedWindowInit = false
        }
    }

    private fun showDragNDrop() {
        ImGui.button("Drag me")
        if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayloadObject("payload_type", "Test Payload")
            ImGui.text("Drag started")
            ImGui.endDragDropSource()
        }
        ImGui.sameLine()
        ImGui.text(dropTargetText)
        if (ImGui.beginDragDropTarget()) {
            val payload = ImGui.acceptDragDropPayloadObject("payload_type")
            if (payload != null) {
                dropTargetText = payload as String
            }
            ImGui.endDragDropTarget()
        }
    }

    private fun showBackgroundPicker() {
        ImGui.alignTextToFramePadding()
        ImGui.text("Background color:")
        ImGui.sameLine()
        ImGui.colorEdit3("##click_counter_col", backgroundColor, ImGuiColorEditFlags.NoInputs or ImGuiColorEditFlags.NoDragDrop)
    }

    private fun showResizableInput() {
        ImGui.text("You can use text inputs with auto-resizable strings!")
        ImGui.inputText("Resizable input", resizableStr, ImGuiInputTextFlags.CallbackResize)
        ImGui.text("text len:")
        ImGui.sameLine()
        ImGui.textColored(COLOR_DODGERBLUE, Integer.toString(resizableStr.length))
        ImGui.sameLine()
        ImGui.text("| buffer size:")
        ImGui.sameLine()
        ImGui.textColored(COLOR_CORAL, Integer.toString(resizableStr.bufferSize))
    }

    private fun showDemoLink() {
        ImGui.text("Consider to look the original ImGui demo: ")
        ImGui.setNextItemWidth(500f)
        ImGui.textColored(COLOR_LIMEGREEN, IMGUI_DEMO_LINK)
        ImGui.sameLine()
        if (ImGui.button("Copy")) {
            ImGui.setClipboardText(IMGUI_DEMO_LINK)
        }
    }

    private fun loadTexture(image: BufferedImage): Int {
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        val buffer = BufferUtils.createByteBuffer(image.width * image.height * 4) // 4 for RGBA, 3 for RGB
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = pixels[y * image.width + x]
                buffer.put((pixel shr 16 and 0xFF).toByte())
                buffer.put((pixel shr 8 and 0xFF).toByte())
                buffer.put((pixel and 0xFF).toByte())
                buffer.put((pixel shr 24 and 0xFF).toByte())
            }
        }
        buffer.flip()
        val textureID = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.width, image.height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)
        return textureID
    }

    companion object {
        private const val IMGUI_DEMO_LINK = "https://raw.githubusercontent.com/ocornut/imgui/05bc204/imgui_demo.cpp"
        private val COLOR_DODGERBLUE = ImColor.rgbToColor("#1E90FF")
        private val COLOR_CORAL = ImColor.rgbToColor("#FF7F50")
        private val COLOR_LIMEGREEN = ImColor.rgbToColor("#32CD32")
    }
}