package dev.czh.idea.ctool.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.ToolWindowManager

class OpenDevDockAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let { project ->
            ToolWindowManager.getInstance(project).getToolWindow("DevDock")?.show()
        }
    }
}
