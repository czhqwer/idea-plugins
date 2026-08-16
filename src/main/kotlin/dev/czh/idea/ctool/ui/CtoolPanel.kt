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
import com.intellij.openapi.ui.Messages
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
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
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
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.intellij.openapi.Disposable

private class WrapLayout(
    alignment: Int = FlowLayout.LEFT,
    hgap: Int = 3,
    vgap: Int = 0,
) : FlowLayout(alignment, hgap, vgap) {
    override fun preferredLayoutSize(target: Container): Dimension = layoutSize(target, true)

    override fun minimumLayoutSize(target: Container): Dimension {
        val minimum = layoutSize(target, false)
        minimum.width = (minimum.width - (hgap + 1)).coerceAtLeast(0)
        return minimum
    }

    private fun layoutSize(target: Container, preferred: Boolean): Dimension {
        synchronized(target.treeLock) {
            val insets = target.insets
            val horizontalInsetsAndGap = insets.left + insets.right + hgap * 2
            val targetWidth = target.width
            val maxWidth = if (targetWidth <= 0) Int.MAX_VALUE else targetWidth - horizontalInsetsAndGap
            val dimension = Dimension(0, 0)
            var rowWidth = 0
            var rowHeight = 0

            target.components.forEach { component ->
                if (!component.isVisible) return@forEach
                val size = if (preferred) component.preferredSize else component.minimumSize
                if (rowWidth + size.width > maxWidth) {
                    addRow(dimension, rowWidth, rowHeight)
                    rowWidth = 0
                    rowHeight = 0
                }
                if (rowWidth != 0) rowWidth += hgap
                rowWidth += size.width
                rowHeight = maxOf(rowHeight, size.height)
            }
            addRow(dimension, rowWidth, rowHeight)
            dimension.width += horizontalInsetsAndGap
            dimension.height += insets.top + insets.bottom + vgap * 2
            return dimension
        }
    }

    private fun addRow(dimension: Dimension, rowWidth: Int, rowHeight: Int) {
        dimension.width = maxOf(dimension.width, rowWidth)
        if (dimension.height > 0) dimension.height += vgap
        dimension.height += rowHeight
    }
}

class DevDockPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val borderColor = JBColor(Color(0xD9DEE7), Color(0x454B55))
    private val accentColor = JBColor(Color(0x2563EB), Color(0x5794FF))
    private val searchField = JBTextField()
    private val categoryBar = JPanel(WrapLayout(FlowLayout.LEFT, 3, 0))
    private val toolStrip = JPanel(WrapLayout(FlowLayout.LEFT, 3, 0))
    private val toolCountLabel = JBLabel()
    private val operationBox = JComboBox<String>()
    private val parameterButton = JButton("参数…")
    private val parameterHint = JBLabel()
    private var selectedOperation = ""
    private lateinit var inputEditor: EditorEx
    private lateinit var diffEditor: EditorEx
    private lateinit var outputEditor: EditorEx
    private lateinit var inputColumn: JPanel
    private lateinit var diffCard: JComponent
    private lateinit var workspaceContainer: JPanel
    private lateinit var outputCard: JComponent
    private val imageLabel = JBLabel()
    private val titleLabel = JBLabel()
    private val networkLabel = JBLabel()
    private val statusLabel = JBLabel("准备就绪")
    private val favoriteButton = JButton()
    private val executeButton = JButton("运行  Ctrl+Enter")
    private var selectedFile: Path? = null
    private var parameterText = ""
    private var activeCategory = "常用"
    private var currentTool: ToolDefinition = ToolCatalog.find(devDockSettings.lastToolId)
    private var currentResult = ToolResult()
    private var allVisibleTools: List<ToolDefinition> = ToolCatalog.all

    init {
        background = UIUtil.getPanelBackground()
        border = JBUI.Borders.empty(6)
        buildHeader()
        buildBody()
        installSearch()
        installShortcuts()
        refreshToolList()
        selectTool(currentTool)
    }

    private fun buildHeader() {
        val top = JPanel(BorderLayout(8, 0)).apply { isOpaque = false }
        categoryBar.isOpaque = false
        buildCategoryButtons()
        installWrapReflow(categoryBar)
        top.add(categoryBar, BorderLayout.CENTER)
        searchField.emptyText.text = "搜索工具、功能或关键词"
        searchField.preferredSize = Dimension(230, 28)
        top.add(searchField, BorderLayout.EAST)

        toolStrip.isOpaque = false
        installWrapReflow(toolStrip)
        val toolPicker = JPanel(BorderLayout(6, 0)).apply { isOpaque = false }
        toolPicker.add(JBLabel("工具"), BorderLayout.WEST)
        toolPicker.add(toolStrip, BorderLayout.CENTER)
        toolPicker.add(toolCountLabel, BorderLayout.EAST)

        val navigation = JPanel(BorderLayout(0, 3)).apply { isOpaque = false }
        navigation.add(top, BorderLayout.NORTH)
        navigation.add(toolPicker, BorderLayout.SOUTH)

        val header = JPanel(BorderLayout(0, 3)).apply { isOpaque = false }
        header.add(navigation, BorderLayout.NORTH)
        header.add(JSeparator(), BorderLayout.SOUTH)
        add(header, BorderLayout.NORTH)
    }

    private fun installWrapReflow(panel: JPanel) {
        panel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent?) {
                panel.parent?.revalidate()
                panel.parent?.parent?.revalidate()
            }
        })
    }

    private fun buildCategoryButtons() {
        categoryBar.removeAll()
        val categories = listOf("常用") + ToolCatalog.all.map { it.category }.distinct()
        categories.forEach { category ->
            val button = JButton(category).apply {
                isFocusable = false
                isContentAreaFilled = true
                border = JBUI.Borders.empty(4, 10)
                addActionListener { setCategory(category) }
            }
            categoryBar.add(button)
        }
        updateCategoryButtons()
    }

    private fun updateCategoryButtons() {
        categoryBar.components.forEach { component ->
            val button = component as? JButton ?: return@forEach
            val selected = button.text == activeCategory
            button.background = if (selected) JBColor(Color(0xE7F0FF), Color(0x244A7B)) else UIUtil.getPanelBackground()
            button.foreground = if (selected) accentColor else UIUtil.getLabelForeground()
            button.font = button.font.deriveFont(if (selected) Font.BOLD else Font.PLAIN)
        }
    }

    private fun setCategory(category: String) {
        activeCategory = category
        updateCategoryButtons()
        refreshToolList()
    }

    private fun buildBody() {
        val main = JPanel(BorderLayout(0, 6)).apply { border = JBUI.Borders.empty(6, 0, 0, 0) }
        main.add(buildToolHeader(), BorderLayout.NORTH)
        main.add(buildWorkspace(), BorderLayout.CENTER)
        add(main, BorderLayout.CENTER)
    }

    private fun buildToolHeader(): JComponent {
        val header = JPanel(WrapLayout(FlowLayout.LEFT, 6, 0)).apply { isOpaque = false }
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 18f)
        header.add(titleLabel)
        header.add(JBLabel("模式"))
        operationBox.preferredSize = Dimension(170, 28)
        operationBox.isFocusable = false
        operationBox.addActionListener {
            operationBox.selectedItem?.toString()?.takeIf(String::isNotBlank)?.let {
                selectedOperation = it
                updateEditorHighlighters()
            }
        }
        header.add(operationBox)
        parameterButton.isFocusable = false
        parameterButton.addActionListener { editParameters() }
        parameterHint.foreground = UIUtil.getContextHelpForeground()
        parameterHint.font = parameterHint.font.deriveFont(11f)
        header.add(parameterHint)
        header.add(parameterButton)

        executeButton.isFocusable = false
        executeButton.background = accentColor
        executeButton.foreground = Color.WHITE
        executeButton.isOpaque = true
        executeButton.border = JBUI.Borders.empty(6, 14)
        executeButton.addActionListener { execute() }
        header.add(executeButton)
        header.add(quietButton("复制结果") { copyResult() })
        header.add(quietButton("清空") { clearWorkspace() })

        networkLabel.foreground = JBColor(Color(0xA16207), Color(0xE9B949))
        header.add(networkLabel)
        statusLabel.foreground = UIUtil.getContextHelpForeground()
        header.add(statusLabel)
        favoriteButton.text = "☆ 收藏"
        favoriteButton.addActionListener { toggleFavorite() }
        favoriteButton.isFocusable = false
        header.add(favoriteButton)
        installWrapReflow(header)
        return header
    }

    private fun buildWorkspace(): JComponent {
        inputColumn = JPanel(BorderLayout(0, 8)).apply { isOpaque = false }
        inputColumn.add(buildInputCard(), BorderLayout.CENTER)
        diffCard = buildDiffCard()
        inputColumn.add(diffCard, BorderLayout.SOUTH)
        outputCard = buildOutputCard()
        workspaceContainer = JPanel(BorderLayout()).apply { isOpaque = false }
        refreshWorkspaceLayout()
        return workspaceContainer
    }

    private fun refreshWorkspaceLayout() {
        if (!::workspaceContainer.isInitialized) return
        workspaceContainer.removeAll()
        inputColumn.parent?.remove(inputColumn)
        outputCard.parent?.remove(outputCard)
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

    private fun buildInputCard(): JComponent {
        val card = cardPanel()
        val header = cardHeader("输入", "支持直接粘贴代码或文本")
        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply { isOpaque = false }
        actions.add(quietButton("选择文件") { chooseFile() })
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
                SwingUtilities.invokeLater { updateJsonFolding(editor) }
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

    private fun quietButton(text: String, action: () -> Unit): JButton = JButton(text).apply {
        isFocusable = false
        margin = JBUI.insets(3, 8)
        addActionListener { action() }
    }

    private fun installSearch() {
        val listener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = refreshToolList()
            override fun removeUpdate(e: DocumentEvent) = refreshToolList()
            override fun changedUpdate(e: DocumentEvent) = refreshToolList()
        }
        searchField.document.addDocumentListener(listener)
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
        val query = searchField.text.trim().lowercase()
        allVisibleTools = ToolCatalog.all.filter { tool ->
            val categoryMatch = when (activeCategory) {
                "常用" -> tool.id in devDockSettings.favoriteToolIds
                else -> tool.category == activeCategory
            }
            val haystack = listOf(tool.id, tool.name, tool.category, *tool.keywords.toTypedArray(), *tool.operations.toTypedArray())
                .joinToString(" ").lowercase()
            categoryMatch && (query.isBlank() || haystack.contains(query))
        }
        toolStrip.removeAll()
        allVisibleTools.forEach { tool ->
            val button = JButton(tool.name).apply {
                isFocusable = false
                isContentAreaFilled = true
                border = JBUI.Borders.empty(4, 9)
                toolTipText = "${tool.category} · ${tool.operations.joinToString(" / ")}"
                putClientProperty("devdock.toolId", tool.id)
                addActionListener { selectTool(tool) }
            }
            toolStrip.add(button)
        }
        toolCountLabel.text = "${allVisibleTools.size} / ${ToolCatalog.all.size}"
        updateToolStripSelection()
        toolStrip.revalidate()
        toolStrip.repaint()
    }

    private fun updateToolStripSelection() {
        toolStrip.components.forEach { component ->
            val button = component as? JButton ?: return@forEach
            val selected = button.getClientProperty("devdock.toolId") == currentTool.id
            button.background = if (selected) JBColor(Color(0xE7F0FF), Color(0x244A7B)) else UIUtil.getPanelBackground()
            button.foreground = if (selected) accentColor else UIUtil.getLabelForeground()
            button.font = button.font.deriveFont(if (selected) Font.BOLD else Font.PLAIN)
        }
    }

    private fun selectTool(tool: ToolDefinition) {
        currentTool = tool
        devDockSettings.lastToolId = tool.id
        titleLabel.text = tool.name
        networkLabel.text = if (tool.network) "需要网络" else ""
        setOperations(tool.operations)
        setEditorText(inputEditor, "")
        setEditorText(diffEditor, "")
        setEditorText(outputEditor, "")
        diffCard.isVisible = tool.id == "diffs"
        refreshWorkspaceLayout()
        parameterText = ""
        parameterHint.text = ""
        imageLabel.icon = null
        currentResult = ToolResult()
        selectedFile = null
        updateFavoriteButton()
        updateToolStripSelection()
        updateEditorHighlighters()
        setStatus(if (tool.network) "此工具需要网络" else "准备就绪")
        revalidate()
        repaint()
    }

    private fun updateFavoriteButton() {
        favoriteButton.text = if (currentTool.id in devDockSettings.favoriteToolIds) "★ 已收藏" else "☆ 收藏"
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
        operationBox.removeAllItems()
        operations.forEach(operationBox::addItem)
        selectedOperation = operations.firstOrNull().orEmpty()
        operationBox.selectedIndex = if (operations.isEmpty()) -1 else 0
        updateEditorHighlighters()
    }

    private fun editParameters() {
        val result = Messages.showMultilineInputDialog(
            project,
            "正则、密钥、第二段文本、换算单位等参数",
            "DevDock 参数",
            parameterText,
            null,
            null,
        )
        if (result != null) {
            parameterText = result
            parameterHint.text = if (result.isBlank()) "" else "参数已设置"
        }
    }

    private fun execute() {
        val operation = selectedOperation
        val tool = currentTool
        val secondaryInput = if (tool.id == "diffs") editorText(diffEditor) else parameterText
        val request = ToolRequest(project, operation, editorText(inputEditor), secondaryInput, selectedFile)
        setStatus("正在运行 ${tool.name}…")
        executeButton.isEnabled = false
        imageLabel.icon = null
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = tool.handler(request)
            SwingUtilities.invokeLater {
                executeButton.isEnabled = true
                showResult(result)
            }
        }
    }

    private fun showResult(result: ToolResult) {
        currentResult = result
        setEditorText(outputEditor, result.text)
        outputEditor.caretModel.moveToOffset(0)
        imageLabel.icon = result.image?.let { javax.swing.ImageIcon(it) }
        setStatus(if (result.isError) "运行失败：请检查输入或参数" else "运行完成", result.isError)
    }

    private fun copyResult() {
        if (currentResult.text.isBlank()) {
            setStatus("没有可复制的文本结果", true)
            return
        }
        CopyPasteManager.getInstance().setContents(StringSelection(currentResult.text))
        setStatus("结果已复制")
    }

    private fun clearWorkspace() {
        setEditorText(inputEditor, "")
        setEditorText(diffEditor, "")
        setEditorText(outputEditor, "")
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
        statusLabel.text = text
        statusLabel.foreground = if (error) JBColor(Color(0xB42318), Color(0xFF8A80)) else UIUtil.getContextHelpForeground()
    }

    override fun dispose() {
        if (::inputEditor.isInitialized) EditorFactory.getInstance().releaseEditor(inputEditor)
        if (::diffEditor.isInitialized) EditorFactory.getInstance().releaseEditor(diffEditor)
        if (::outputEditor.isInitialized) EditorFactory.getInstance().releaseEditor(outputEditor)
    }
}
