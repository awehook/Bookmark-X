package indi.bookmarkx.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.global.FileMarksCache;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.util.LogCollector;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * 书签强制刷新动作
 * 用于修复同一行出现多个书签图标的问题
 */
public class BookmarkRefreshAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(BookmarkRefreshAction.class);

    public BookmarkRefreshAction() {
        super("Refresh Bookmarks", "Force refresh all bookmark gutter icons", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                String startMsg = "Starting force refresh of bookmark gutter icons...";
                LOG.info(startMsg);
                LogCollector.getInstance().info("BookmarkRefreshAction", project, startMsg);

                // 1. 清除所有打开文件中的书签gutter图标
                clearAllBookmarkGutterIcons(project);

                // 2. 重新渲染所有书签图标
                rerenderAllBookmarkIcons(project);

                String successMsg = "Successfully refreshed all bookmark gutter icons";
                LOG.info(successMsg);
                LogCollector.getInstance().info("BookmarkRefreshAction", project, successMsg);

            } catch (Exception ex) {
                String errorMsg = "Failed to refresh bookmark gutter icons";
                LOG.error(errorMsg, ex);
                LogCollector.getInstance().error("BookmarkRefreshAction", project, errorMsg, ex);
            }
        });
    }

    /**
     * 清除所有打开文件中的书签gutter图标
     */
    private void clearAllBookmarkGutterIcons(Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile[] openFiles = fileEditorManager.getOpenFiles();

        int clearedCount = 0;
        for (VirtualFile file : openFiles) {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                continue;
            }

            MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
            
            // 查找并移除所有书签相关的 RangeHighlighter
            RangeHighlighter[] allHighlighters = markupModel.getAllHighlighters();
            for (RangeHighlighter highlighter : allHighlighters) {
                // 检查是否是书签的 highlighter（通过 GutterIconRenderer 判断）
                if (highlighter.getGutterIconRenderer() instanceof indi.bookmarkx.ui.MyGutterIconRenderer) {
                    highlighter.dispose();
                    clearedCount++;
                }
            }
        }

        String msg = "Cleared " + clearedCount + " bookmark gutter icons from " + openFiles.length + " open files";
        LOG.info(msg);
        LogCollector.getInstance().info("BookmarkRefreshAction", project, msg);
    }

    /**
     * 重新渲染所有书签图标
     */
    private void rerenderAllBookmarkIcons(Project project) {
        BookmarksManager manager = BookmarksManager.getInstance(project);
        FileMarksCache fileMarksCache = manager.getFileMarksCache();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile[] openFiles = fileEditorManager.getOpenFiles();

        int totalRendered = 0;
        for (VirtualFile file : openFiles) {
            Set<BookmarkNodeModel> bookmarks = fileMarksCache.getBookmarks(file.getPath());
            if (CollectionUtils.isEmpty(bookmarks)) {
                continue;
            }

            for (BookmarkNodeModel bookmark : bookmarks) {
                // 先释放旧的引用
                bookmark.release();
                // 重新创建 line marker
                bookmark.createLineMarker();
                totalRendered++;
            }
        }

        String msg = "Re-rendered " + totalRendered + " bookmark icons";
        LOG.info(msg);
        LogCollector.getInstance().info("BookmarkRefreshAction", project, msg);
    }
}
