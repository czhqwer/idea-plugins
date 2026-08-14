package dev.czh.idea.ctool.model

import com.intellij.openapi.project.Project
import java.awt.image.BufferedImage
import java.nio.file.Path

data class ToolRequest(
    val project: Project?,
    val operation: String,
    val input: String,
    val secondaryInput: String = "",
    val selectedFile: Path? = null,
)

data class ToolResult(
    val text: String = "",
    val image: BufferedImage? = null,
    val isError: Boolean = false,
)

typealias ToolHandler = (ToolRequest) -> ToolResult

data class ToolDefinition(
    val id: String,
    val name: String,
    val category: String,
    val operations: List<String>,
    val keywords: List<String> = emptyList(),
    val supportsFile: Boolean = false,
    val network: Boolean = false,
    val handler: ToolHandler,
)
