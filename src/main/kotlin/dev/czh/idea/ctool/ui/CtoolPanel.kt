package dev.czh.idea.ctool.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent as EditorDocumentEvent
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.czh.idea.ctool.model.ToolDefinition
import dev.czh.idea.ctool.model.ToolRequest
import dev.czh.idea.ctool.model.ToolResult
import dev.czh.idea.ctool.settings.devDockSettings
import dev.czh.idea.ctool.tools.ToolCatalog
import dev.czh.idea.ctool.tools.ToolImplementations
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.awt.datatransfer.StringSelection
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.JComboBox
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import com.intellij.openapi.Disposable

private class FlatIconButton(text: String) : JButton(text) {
    var primary = false

    init {
        isFocusable = false
        isBorderPainted = false
        isContentAreaFilled = false
        isOpaque = false
        isRolloverEnabled = true
        isFocusPainted = false
        border = JBUI.Borders.empty(4, 6)
        margin = Insets(0, 0, 0, 0)
        preferredSize = Dimension(30, 28)
        font = font.deriveFont(Font.PLAIN, 13f)
    }

    override fun paintComponent(graphics: Graphics) {
        val graphics2D = graphics.create() as Graphics2D
        val pressed = model.isPressed || model.isArmed
        val hovered = model.isRollover
        if (primary || pressed || hovered) {
            graphics2D.color = when {
                primary && pressed -> JBColor(Color(0x1D4ED8), Color(0x3D74C5))
                primary -> JBColor(Color(0x2563EB), Color(0x5794FF))
                pressed -> JBColor(Color(0xD7E3F7), Color(0x38577F))
                else -> JBColor(Color(0xEEF4FF), Color(0x2B3A50))
            }
            graphics2D.fillRoundRect(0, 0, width - 1, height - 1, 8, 8)
        }
        graphics2D.dispose()
        super.paintComponent(graphics)
    }
}

class DevDockPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val borderColor = JBColor(Color(0xD9DEE7), Color(0x454B55))
    private val accentColor = JBColor(Color(0x2563EB), Color(0x5794FF))
    private val categoryBar = JPanel(BorderLayout(3, 0))
    private val categoryButtons = JPanel()
    private val categoryOverflowButton = FlatIconButton("☰")
    private var categories: List<String> = emptyList()
    private var updatingCategoryOverflow = false
    private val toolBox = JComboBox<String>()
    private val operationButtons = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
    private val jsonFilterField = JBTextField()
    private var selectedOperation = ""
    private lateinit var inputEditor: EditorEx
    private lateinit var diffEditor: EditorEx
    private lateinit var outputEditor: EditorEx
    private lateinit var inputColumn: JPanel
    private lateinit var inputCard: JComponent
    private lateinit var diffCard: JComponent
    private lateinit var workspaceContainer: JPanel
    private lateinit var outputCard: JComponent
    private lateinit var jsonWorkspace: JPanel
    private lateinit var toolHeader: JPanel
    private lateinit var toolHeaderLeft: JPanel
    private lateinit var toolActionBar: JPanel
    private var toolHeaderWrapped = false
    private lateinit var genericCopyButton: JButton
    private lateinit var genericClearButton: JButton
    private lateinit var jsonActionsPanel: JPanel
    private val imageLabel = JBLabel()
    private val networkLabel = JBLabel()
    private val statusLabel = JBLabel("准备就绪")
    private val favoriteButton = FlatIconButton("☆")
    private var selectedFile: Path? = null
    private var activeCategory = "常用"
    private var currentTool: ToolDefinition = ToolCatalog.find(devDockSettings.lastToolId)
    private var currentResult = ToolResult()
    private var allVisibleTools: List<ToolDefinition> = ToolCatalog.all
    private var updatingToolBox = false
    private var updatingJsonEditor = false

    init {
        background = UIUtil.getPanelBackground()
        border = JBUI.Borders.empty(6)
        minimumSize = Dimension(440, 360)
        buildHeader()
        buildBody()
        installShortcuts()
        refreshToolList()
        selectTool(currentTool)
    }

    private fun buildHeader() {
        categoryBar.isOpaque = false
        categoryButtons.layout = javax.swing.BoxLayout(categoryButtons, javax.swing.BoxLayout.X_AXIS)
        categoryButtons.isOpaque = false
        categoryBar.add(categoryButtons, BorderLayout.CENTER)
        categoryOverflowButton.apply {
            isFocusable = false
            preferredSize = Dimension(30, 26)
            minimumSize = preferredSize
            margin = JBUI.insets(2)
            toolTipText = "更多菜单"
            isVisible = false
            addActionListener { showCategoryOverflowMenu() }
        }
        categoryBar.add(categoryOverflowButton, BorderLayout.EAST)
        buildCategoryButtons()
        categoryBar.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent?) {
                updateCategoryOverflow()
            }
        })

        val header = JPanel(BorderLayout(0, 3)).apply { isOpaque = false }
        header.add(categoryBar, BorderLayout.NORTH)
        header.add(JSeparator(), BorderLayout.SOUTH)
        add(header, BorderLayout.NORTH)
    }

    private fun buildCategoryButtons() {
        categories = listOf("常用") + ToolCatalog.all.map { it.category }.distinct()
        categoryButtons.removeAll()
        categories.forEach { category ->
            val button = JButton(category).apply {
                isFocusable = false
                isContentAreaFilled = true
                border = JBUI.Borders.empty(3, 8)
                addActionListener { setCategory(category) }
            }
            categoryButtons.add(button)
        }
        updateCategoryButtons()
        SwingUtilities.invokeLater { updateCategoryOverflow() }
    }

    private fun updateCategoryButtons() {
        categoryButtons.components.forEach { component ->
            val button = component as? JButton ?: return@forEach
            val selected = button.text == activeCategory
            button.background = if (selected) JBColor(Color(0xE7F0FF), Color(0x244A7B)) else UIUtil.getPanelBackground()
            button.foreground = if (selected) accentColor else UIUtil.getLabelForeground()
            button.font = button.font.deriveFont(if (selected) Font.BOLD else Font.PLAIN)
        }
    }

    private fun updateCategoryOverflow() {
        if (updatingCategoryOverflow || categoryBar.width <= 0) return
        val buttons = categoryButtons.components.filterIsInstance<JButton>()
        if (buttons.isEmpty()) return
        updatingCategoryOverflow = true
        try {
            val availableWidth = categoryBar.width - categoryBar.insets.left - categoryBar.insets.right
            val totalWidth = buttons.sumOf { it.preferredSize.width }
            val needsOverflow = totalWidth > availableWidth
            val widthForButtons = if (needsOverflow) {
                availableWidth - categoryOverflowButton.preferredSize.width - 3
            } else {
                availableWidth
            }
            var usedWidth = 0
            var hiddenCount = 0
            buttons.forEach { button ->
                val buttonWidth = button.preferredSize.width
                val fits = hiddenCount == 0 && usedWidth + buttonWidth <= widthForButtons
                button.isVisible = fits
                if (fits) {
                    usedWidth += buttonWidth
                } else {
                    hiddenCount++
                }
            }
            categoryOverflowButton.isVisible = hiddenCount > 0
            categoryButtons.revalidate()
            categoryButtons.repaint()
            categoryBar.revalidate()
            categoryBar.repaint()
        } finally {
            updatingCategoryOverflow = false
        }
    }

    private fun showCategoryOverflowMenu() {
        val hiddenCategories = categoryButtons.components
            .filterIsInstance<JButton>()
            .filterNot(JButton::isVisible)
            .map { it.text }
        if (hiddenCategories.isEmpty()) return
        val menu = javax.swing.JPopupMenu()
        hiddenCategories.forEach { category ->
            menu.add(javax.swing.JMenuItem(category).apply {
                addActionListener { setCategory(category) }
            })
        }
        menu.show(categoryOverflowButton, 0, categoryOverflowButton.height)
    }

    private fun setCategory(category: String) {
        activeCategory = category
        updateCategoryButtons()
        refreshToolList()
        allVisibleTools.firstOrNull()?.let(::selectTool)
    }

    private fun buildBody() {
        val main = JPanel(BorderLayout(0, 6)).apply { border = JBUI.Borders.empty(6, 0, 0, 0) }
        main.add(buildToolHeader(), BorderLayout.NORTH)
        main.add(buildWorkspace(), BorderLayout.CENTER)
        add(main, BorderLayout.CENTER)

        val footer = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(3)
        }
        statusLabel.foreground = UIUtil.getContextHelpForeground()
        footer.add(statusLabel, BorderLayout.EAST)
        add(footer, BorderLayout.SOUTH)
    }

    private fun buildToolHeader(): JComponent {
        val header = JPanel(BorderLayout(8, 0)).apply { isOpaque = false }
        toolHeader = header
        val left = JPanel(BorderLayout(6, 0)).apply { isOpaque = false }
        toolHeaderLeft = left
        toolBox.preferredSize = Dimension(150, 28)
        toolBox.isFocusable = false
        toolBox.isOpaque = false
        toolBox.border = JBUI.Borders.empty(0, 0, 0, 4)
        toolBox.font = toolBox.font.deriveFont(Font.PLAIN, 13f)
        toolBox.toolTipText = "切换工具"
        toolBox.addActionListener {
            if (!updatingToolBox) allVisibleTools.getOrNull(toolBox.selectedIndex)?.let(::selectTool)
        }
        left.add(toolBox, BorderLayout.WEST)
        operationButtons.isOpaque = false
        operationButtons.border = JBUI.Borders.empty(0)
        left.add(operationButtons, BorderLayout.CENTER)
        header.add(left, BorderLayout.CENTER)

        val actionBar = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply { isOpaque = false }
        toolActionBar = actionBar
        genericCopyButton = iconButton("⧉", "复制结果") { copyResult() }
        genericClearButton = iconButton("×", "清空输入和结果") { clearWorkspace() }
        actionBar.add(genericCopyButton)
        actionBar.add(genericClearButton)

        jsonActionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
            isOpaque = false
            add(iconButton("{}", "格式化并复制") { runJsonAction("格式化") })
            add(iconButton("−", "压缩 JSON 并复制") { runJsonAction("压缩") })
            add(iconButton("\"", "JSON 转义并复制") { runJsonAction("转义") })
            add(iconButton("<>", "JSON 转 XML 并复制") { runJsonAction("JSON 转 XML") })
            add(iconButton("TS", "JSON 转 TypeScript 并复制") { runJsonAction("JSON 转 TypeScript") })
            add(iconButton("⧉", "复制 JSON") { copyResult() })
            isVisible = false
        }
        actionBar.add(jsonActionsPanel)

        networkLabel.foreground = JBColor(Color(0xA16207), Color(0xE9B949))
        actionBar.add(networkLabel)
        favoriteButton.preferredSize = Dimension(32, 28)
        favoriteButton.margin = JBUI.insets(3)
        favoriteButton.addActionListener { toggleFavorite() }
        favoriteButton.isFocusable = false
        actionBar.add(favoriteButton)
        header.add(actionBar, BorderLayout.EAST)
        header.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent?) {
                updateToolHeaderLayout()
            }
        })
        SwingUtilities.invokeLater { updateToolHeaderLayout() }
        return header
    }

    private fun updateToolHeaderLayout() {
        if (!::toolHeader.isInitialized || toolHeader.width <= 0) return
        val availableWidth = toolHeader.width - toolHeader.insets.left - toolHeader.insets.right
        val requiredWidth = toolHeaderLeft.preferredSize.width + toolActionBar.preferredSize.width + 8
        val shouldWrap = availableWidth < requiredWidth
        if (shouldWrap == toolHeaderWrapped) return
        toolHeaderWrapped = shouldWrap
        toolHeader.remove(toolHeaderLeft)
        toolHeader.remove(toolActionBar)
        if (shouldWrap) {
            toolHeader.layout = BorderLayout(0, 3)
            toolHeader.add(toolHeaderLeft, BorderLayout.NORTH)
            toolHeader.add(toolActionBar, BorderLayout.SOUTH)
        } else {
            toolHeader.layout = BorderLayout(8, 0)
            toolHeader.add(toolHeaderLeft, BorderLayout.CENTER)
            toolHeader.add(toolActionBar, BorderLayout.EAST)
        }
        toolHeader.revalidate()
        toolHeader.repaint()
    }

    private fun buildWorkspace(): JComponent {
        inputColumn = JPanel(BorderLayout(0, 8)).apply { isOpaque = false }
        inputCard = buildInputCard()
        inputColumn.add(inputCard, BorderLayout.CENTER)
        diffCard = buildDiffCard()
        inputColumn.add(diffCard, BorderLayout.SOUTH)
        outputCard = buildOutputCard()
        jsonWorkspace = buildJsonWorkspace()
        workspaceContainer = JPanel(BorderLayout()).apply { isOpaque = false }
        refreshWorkspaceLayout()
        return workspaceContainer
    }

    private fun refreshWorkspaceLayout() {
        if (!::workspaceContainer.isInitialized) return
        workspaceContainer.removeAll()
        inputColumn.parent?.remove(inputColumn)
        outputCard.parent?.remove(outputCard)
        if (currentTool.id == "json") {
            inputEditor.component.parent?.remove(inputEditor.component)
            jsonWorkspace.add(inputEditor.component, BorderLayout.CENTER)
            workspaceContainer.add(jsonWorkspace, BorderLayout.CENTER)
            workspaceContainer.revalidate()
            workspaceContainer.repaint()
            return
        }
        jsonWorkspace.remove(inputEditor.component)
        inputCard.add(inputEditor.component, BorderLayout.CENTER)
        val workspace = JBSplitter(isVerticalWorkspace(currentTool), 0.5f).apply {
            firstComponent = inputColumn
            secondComponent = outputCard
        }
        workspaceContainer.add(workspace, BorderLayout.CENTER)
        workspaceContainer.revalidate()
        workspaceContainer.repaint()
    }

    private fun isVerticalWorkspace(tool: ToolDefinition): Boolean =
        tool.id !in setOf("json", "code", "serialize", "diffs")

    private fun buildJsonWorkspace(): JPanel {
        val workspace = JPanel(BorderLayout(0, 6)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(0, 2)
        }
        jsonFilterField.emptyText.text = "JavaScript 过滤，例如 .filter(x => x.active).map(x => x.name)"
        jsonFilterField.toolTipText = "使用 JavaScript 风格表达式过滤 JSON"
        jsonFilterField.addActionListener { runJsonAction("过滤") }
        val filterBar = JPanel(BorderLayout(6, 0)).apply { isOpaque = false }
        filterBar.add(jsonFilterField, BorderLayout.CENTER)
        filterBar.add(iconButton("ƒ", "执行过滤并复制") { runJsonAction("过滤") }, BorderLayout.EAST)
        workspace.add(filterBar, BorderLayout.SOUTH)
        return workspace
    }

    private fun buildInputCard(): JComponent {
        val card = cardPanel()
        val header = cardHeader("输入", "支持直接粘贴代码或文本")
        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply { isOpaque = false }
        actions.add(iconButton("↥", "选择文件") { chooseFile() })
        header.add(actions, BorderLayout.EAST)
        card.add(header, BorderLayout.NORTH)
        inputEditor = createCodeEditor(false)
        card.add(inputEditor.component, BorderLayout.CENTER)
        return card
    }

    private fun buildDiffCard(): JComponent {
        val card = cardPanel()
        card.add(cardHeader("对比文本", "Diff 工具的第二段文本"), BorderLayout.NORTH)
        diffEditor = createCodeEditor(false)
        card.add(diffEditor.component, BorderLayout.CENTER)
        card.preferredSize = Dimension(0, 180)
        card.minimumSize = Dimension(0, 140)
        return card
    }

    private fun buildOutputCard(): JComponent {
        val card = cardPanel()
        card.add(cardHeader("结果", "可复制到剪贴板"), BorderLayout.NORTH)
        outputEditor = createCodeEditor(true)
        card.add(outputEditor.component, BorderLayout.CENTER)
        imageLabel.horizontalAlignment = JBLabel.CENTER
        imageLabel.border = JBUI.Borders.empty(6)
        card.add(imageLabel, BorderLayout.SOUTH)
        return card
    }

    private fun cardPanel(): JPanel = JPanel(BorderLayout(0, 8)).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            JBUI.Borders.empty(8),
        )
    }

    private fun createCodeEditor(readOnly: Boolean): EditorEx {
        val document = EditorFactory.getInstance().createDocument("")
        val editor = EditorFactory.getInstance().createEditor(document, project) as EditorEx
        editor.setViewer(readOnly)
        editor.settings.isLineNumbersShown = true
        editor.settings.isFoldingOutlineShown = true
        editor.settings.isRightMarginShown = false
        editor.settings.isIndentGuidesShown = true
        editor.settings.isCaretRowShown = !readOnly
        editor.component.border = BorderFactory.createLineBorder(borderColor)
        document.addDocumentListener(object : DocumentAdapter() {
            override fun documentChanged(event: EditorDocumentEvent) {
                SwingUtilities.invokeLater {
                    updateJsonFolding(editor)
                    if (!updatingJsonEditor && currentTool.id == "json" &&
                        ::inputEditor.isInitialized && editor === inputEditor && event.newLength > 1
                    ) {
                        autoFormatJsonInput()
                    }
                }
            }
        }, this)
        return editor
    }

    private fun editorText(editor: EditorEx): String = editor.document.text

    private fun setEditorText(editor: EditorEx, text: String) {
        ApplicationManager.getApplication().runWriteAction {
            editor.document.setText(text)
        }
        editor.caretModel.moveToOffset(0)
    }

    private fun setJsonEditorText(text: String) {
        updatingJsonEditor = true
        try {
            setEditorText(inputEditor, text)
        } finally {
            updatingJsonEditor = false
        }
    }

    private fun autoFormatJsonInput() {
        if (currentTool.id != "json" || updatingJsonEditor) return
        val input = editorText(inputEditor)
        val result = ToolImplementations.normalizeJsonInput(input)
        if (!result.isError && result.text.isNotBlank() && result.text != input) {
            setJsonEditorText(result.text)
            setStatus("已自动转换并格式化")
        }
    }

    private fun updateEditorHighlighters() {
        val extension = when (currentTool.id) {
            "json" -> "json"
            "code" -> when {
                selectedOperation.contains("JSON") -> "json"
                selectedOperation.contains("JavaScript") -> "js"
                selectedOperation.contains("TypeScript") -> "ts"
                selectedOperation.contains("HTML") -> "html"
                selectedOperation.contains("CSS") || selectedOperation.contains("SCSS") || selectedOperation.contains("Less") -> "css"
                selectedOperation.contains("SQL") -> "sql"
                selectedOperation.contains("XML") -> "xml"
                selectedOperation.contains("YAML") -> "yaml"
                selectedOperation.contains("Markdown") -> "md"
                else -> "txt"
            }
            "sqlFillParameter" -> "sql"
            "html" -> "html"
            "serialize" -> "json"
            else -> "txt"
        }
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension(extension)
        val factory = EditorHighlighterFactory.getInstance()
        inputEditor.highlighter = factory.createEditorHighlighter(project, fileType)
        outputEditor.highlighter = factory.createEditorHighlighter(project, fileType)
        if (::diffEditor.isInitialized) {
            diffEditor.highlighter = factory.createEditorHighlighter(project, fileType)
        }
        updateJsonFolding(inputEditor)
        updateJsonFolding(outputEditor)
        if (::diffEditor.isInitialized) updateJsonFolding(diffEditor)
    }

    private fun updateJsonFolding(editor: EditorEx) {
        val foldingModel = editor.foldingModel
        val ranges = if (currentTool.id == "json") jsonFoldRanges(editor.document.text) else emptyList()
        foldingModel.runBatchFoldingOperation {
            foldingModel.allFoldRegions.toList().forEach(foldingModel::removeFoldRegion)
            ranges.forEach { (start, end) ->
                foldingModel.addFoldRegion(start, end, "…")?.setExpanded(true)
            }
        }
    }

    private fun jsonFoldRanges(text: String): List<Pair<Int, Int>> {
        val stack = ArrayDeque<Pair<Char, Int>>()
        val ranges = mutableListOf<Pair<Int, Int>>()
        var inString = false
        var escaped = false

        text.forEachIndexed { index, char ->
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                return@forEachIndexed
            }
            when (char) {
                '"' -> inString = true
                '{', '[' -> stack.addLast(char to index)
                '}', ']' -> {
                    val expected = if (char == '}') '{' else '['
                    val opening = stack.removeLastOrNull()
                    if (opening?.first == expected && text.substring(opening.second, index + 1).contains('\n')) {
                        ranges += opening.second to index + 1
                    }
                }
            }
        }
        return ranges.sortedBy { it.first }
    }

    private fun cardHeader(title: String, hint: String): JPanel {
        val header = JPanel(BorderLayout(8, 0)).apply { isOpaque = false }
        val titlePanel = JPanel(BorderLayout(0, 2)).apply { isOpaque = false }
        titlePanel.add(JBLabel(title).apply { font = font.deriveFont(Font.BOLD, 13f) }, BorderLayout.NORTH)
        titlePanel.add(JBLabel(hint).apply {
            foreground = UIUtil.getContextHelpForeground()
            font = font.deriveFont(11f)
        }, BorderLayout.SOUTH)
        header.add(titlePanel, BorderLayout.WEST)
        return header
    }

    private fun iconButton(icon: String, tooltip: String, action: () -> Unit): JButton = FlatIconButton(icon).apply {
        toolTipText = tooltip
        addActionListener { action() }
    }

    private fun installShortcuts() {
        val action = object : javax.swing.AbstractAction() {
            override fun actionPerformed(event: ActionEvent?) = execute()
        }
        val inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        val actionMap = getActionMap()
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "devdock.execute")
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK), "devdock.execute")
        actionMap.put("devdock.execute", action)
    }

    private fun refreshToolList() {
        allVisibleTools = ToolCatalog.all.filter { tool ->
            when (activeCategory) {
                "常用" -> tool.id in devDockSettings.favoriteToolIds
                else -> tool.category == activeCategory
            }
        }
        updatingToolBox = true
        try {
            toolBox.removeAllItems()
            allVisibleTools.forEach { tool -> toolBox.addItem(tool.name) }
            val selectedIndex = allVisibleTools.indexOfFirst { it.id == currentTool.id }
            toolBox.selectedIndex = selectedIndex.takeIf { it >= 0 } ?: -1
        } finally {
            updatingToolBox = false
        }
        toolBox.revalidate()
        toolBox.repaint()
    }

    private fun updateToolSelector() {
        val selectedIndex = allVisibleTools.indexOfFirst { it.id == currentTool.id }
        if (selectedIndex >= 0 && toolBox.selectedIndex != selectedIndex) {
            updatingToolBox = true
            toolBox.selectedIndex = selectedIndex
            updatingToolBox = false
        }
    }

    private fun selectTool(tool: ToolDefinition) {
        currentTool = tool
        devDockSettings.lastToolId = tool.id
        networkLabel.text = if (tool.network) "需要网络" else ""
        setOperations(tool.operations)
        setEditorText(inputEditor, "")
        setEditorText(diffEditor, "")
        setEditorText(outputEditor, "")
        diffCard.isVisible = tool.id == "diffs"
        jsonFilterField.text = ""
        refreshWorkspaceLayout()
        imageLabel.icon = null
        currentResult = ToolResult()
        selectedFile = null
        updateFavoriteButton()
        updateToolSelector()
        updateToolHeader()
        updateEditorHighlighters()
        setStatus(if (tool.network) "此工具需要网络" else "准备就绪")
        revalidate()
        repaint()
    }

    private fun updateFavoriteButton() {
        val favorite = currentTool.id in devDockSettings.favoriteToolIds
        favoriteButton.text = if (favorite) "★" else "☆"
        favoriteButton.toolTipText = if (favorite) "取消收藏" else "收藏工具"
    }

    private fun chooseFile() {
        if (!currentTool.supportsFile) {
            setStatus("当前工具不需要文件", true)
            return
        }
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
        FileChooser.chooseFile(descriptor, project, null)?.let { virtualFile ->
            selectedFile = virtualFile.toNioPath()
            if (editorText(inputEditor).isBlank()) setEditorText(inputEditor, virtualFile.name)
            setStatus("已选择文件：${virtualFile.name}")
        }
    }

    private fun setOperations(operations: List<String>) {
        operationButtons.removeAll()
        operations.forEach { operation ->
            operationButtons.add(operationButton(operation) {
                executeOperation(operation)
            })
        }
        selectedOperation = operations.firstOrNull().orEmpty()
        operationButtons.revalidate()
        operationButtons.repaint()
        updateEditorHighlighters()
    }

    private fun operationButton(operation: String, action: () -> Unit): JButton = FlatIconButton(operation).apply {
        val textWidth = getFontMetrics(font).stringWidth(operation)
        preferredSize = Dimension((textWidth + 18).coerceAtLeast(44), 28)
        toolTipText = operation
        addActionListener { action() }
    }

    private fun executeOperation(operation: String) {
        selectedOperation = operation
        updateEditorHighlighters()
        execute()
    }

    private fun updateToolHeader() {
        val isJson = currentTool.id == "json"
        operationButtons.isVisible = !isJson
        genericCopyButton.isVisible = !isJson
        genericClearButton.isVisible = !isJson
        jsonActionsPanel.isVisible = isJson
        updateToolHeaderLayout()
        toolHeader.revalidate()
        toolHeader.repaint()
    }

    private fun execute() {
        if (currentTool.id == "json") {
            runJsonAction("格式化")
            return
        }
        val operation = selectedOperation
        val tool = currentTool
        val secondaryInput = if (tool.id == "diffs") editorText(diffEditor) else ""
        val request = ToolRequest(project, operation, editorText(inputEditor), secondaryInput, selectedFile)
        setStatus("正在运行 ${tool.name}…")
        setOperationButtonsEnabled(false)
        imageLabel.icon = null
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = tool.handler(request)
            SwingUtilities.invokeLater {
                setOperationButtonsEnabled(true)
                showResult(result)
            }
        }
    }

    private fun setOperationButtonsEnabled(enabled: Boolean) {
        operationButtons.components.forEach { it.isEnabled = enabled }
    }

    private fun runJsonAction(operation: String) {
        val result = ToolImplementations.jsonEditorAction(
            operation,
            editorText(inputEditor),
            jsonFilterField.text,
        )
        currentResult = result
        if (result.isError) {
            setStatus("JSON 操作失败：${result.text}", true)
            return
        }
        copyText(result.text)
        setStatus("已执行并复制")
    }

    private fun showResult(result: ToolResult) {
        currentResult = result
        setEditorText(outputEditor, result.text)
        outputEditor.caretModel.moveToOffset(0)
        imageLabel.icon = result.image?.let { javax.swing.ImageIcon(it) }
        setStatus(if (result.isError) "运行失败：请检查输入或参数" else "运行完成", result.isError)
    }

    private fun copyResult() {
        val text = if (currentTool.id == "json") editorText(inputEditor) else currentResult.text
        if (text.isBlank()) {
            setStatus("没有可复制的文本结果", true)
            return
        }
        copyText(text)
        setStatus("结果已复制")
    }

    private fun copyText(text: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    private fun clearWorkspace() {
        setEditorText(inputEditor, "")
        setEditorText(diffEditor, "")
        setEditorText(outputEditor, "")
        jsonFilterField.text = ""
        imageLabel.icon = null
        currentResult = ToolResult()
        setStatus("工作区已清空")
    }

    private fun toggleFavorite() {
        val favorites = devDockSettings.favoriteToolIds.toMutableList()
        if (currentTool.id in favorites) favorites.remove(currentTool.id) else favorites.add(currentTool.id)
        devDockSettings.favoriteToolIds = favorites
        updateFavoriteButton()
        if (activeCategory == "常用") refreshToolList()
        setStatus(if (currentTool.id in favorites) "已加入常用工具" else "已移出常用工具")
    }

    private fun setStatus(text: String, error: Boolean = false) {
        val prefix = when {
            error -> "⚠ "
            text.startsWith("正在") || text == "准备就绪" || text.startsWith("此工具") -> ""
            else -> "✓ "
        }
        statusLabel.text = prefix + text
        statusLabel.foreground = if (error) JBColor(Color(0xB42318), Color(0xFF8A80)) else UIUtil.getContextHelpForeground()
    }

    override fun dispose() {
        if (::inputEditor.isInitialized) EditorFactory.getInstance().releaseEditor(inputEditor)
        if (::diffEditor.isInitialized) EditorFactory.getInstance().releaseEditor(diffEditor)
        if (::outputEditor.isInitialized) EditorFactory.getInstance().releaseEditor(outputEditor)
    }
}
