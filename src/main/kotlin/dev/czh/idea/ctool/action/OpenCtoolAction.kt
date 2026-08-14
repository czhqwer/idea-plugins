package dev.czh.idea.ctool.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.ToolWindowManager

class OpenDevDockAction : DumbAwareAction() {
    init {
        templatePresentation.icon = IconLoader.getIcon("/icons/devdock.svg", OpenDevDockAction::class.java)
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let { project ->
            ToolWindowManager.getInstance(project).getToolWindow("DevDock")?.show()
        }
    }
}
