package indi.bookmarkx;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import indi.bookmarkx.action.BookmarkExportAction;
import indi.bookmarkx.action.BookmarkFlatExportAction;
import indi.bookmarkx.action.BookmarkFlatImportAction;
import indi.bookmarkx.action.BookmarkImportAction;
import indi.bookmarkx.action.BookmarkRefreshAction;
import indi.bookmarkx.ui.pannel.LogPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;


public class RootWindowFactory implements ToolWindowFactory, DumbAware {

    private static final Logger LOG = Logger.getInstance(RootWindowFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        initTitleAction(toolWindow);
        BookmarksManager manager = BookmarksManager.getInstance(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        
        // 添加书签管理面板
        Content bookmarkContent = contentFactory.createContent(manager.getToolWindowRootPanel(), "Bookmark", false);
        toolWindow.getContentManager().addContent(bookmarkContent);
        
        // 添加日志面板
        LogPanel logPanel = new LogPanel();
        Content logContent = contentFactory.createContent(logPanel, "Log", false);
        toolWindow.getContentManager().addContent(logContent);
    }

    private void initTitleAction(ToolWindow toolWindow) {
        BookmarkRefreshAction refreshAction = new BookmarkRefreshAction();
        BookmarkExportAction exportAction = new BookmarkExportAction();
        BookmarkFlatExportAction flatExportAction = new BookmarkFlatExportAction();
        BookmarkImportAction importAction = new BookmarkImportAction();
        BookmarkFlatImportAction flatImportAction = new BookmarkFlatImportAction();

        // 在 ToolWindow 的标题栏中添加自定义动作按钮
        toolWindow.setTitleActions(Arrays.asList(refreshAction, importAction, flatImportAction, exportAction, flatExportAction));
    }
}
