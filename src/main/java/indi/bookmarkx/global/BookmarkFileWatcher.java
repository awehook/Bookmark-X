package indi.bookmarkx.global;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.persistence.ProjectSettings;
import indi.bookmarkx.util.LogCollector;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * 书签配置文件监听器
 * 监听自定义书签存储文件的变化，当文件被外部修改时自动重新加载书签
 *
 * @author Nonoas
 * @date 2025/11/21
 */
public class BookmarkFileWatcher {

    private static final Logger LOG = Logger.getInstance(BookmarkFileWatcher.class);

    private final Project project;
    private MessageBusConnection connection;
    private String watchedFilePath;
    private long lastModifiedTime = 0;
    private boolean isInternalChange = false; // 标记是否为内部修改

    public BookmarkFileWatcher(Project project) {
        this.project = project;
    }

    /**
     * 启动文件监听
     */
    public void startWatching() {
        String customPath = ProjectSettings.getInstance(project).getCustomStoragePath();
        
        if (StringUtils.isBlank(customPath)) {
            String msg = "[BookmarkFileWatcher] No custom storage path configured, file watcher not started";
            LOG.info(msg);
            LogCollector.getInstance().info("BookmarkFileWatcher", msg);
            return;
        }

        // 如果已经在监听同一个文件，不需要重新启动
        if (customPath.equals(watchedFilePath) && connection != null) {
            String alreadyWatchingMsg = "[BookmarkFileWatcher] Already watching file: " + customPath;
            LOG.info(alreadyWatchingMsg);
            LogCollector.getInstance().info("BookmarkFileWatcher", alreadyWatchingMsg);
            return;
        }

        // 停止旧的监听
        stopWatching();

        watchedFilePath = customPath;
        
        // 初始化最后修改时间
        File file = new File(customPath);
        if (file.exists()) {
            lastModifiedTime = file.lastModified();
        }

        // 注册文件变化监听器
        connection = project.getMessageBus().connect();
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event instanceof VFileContentChangeEvent) {
                        handleFileChange((VFileContentChangeEvent) event);
                    }
                }
            }
        });

        String startedMsg = "[BookmarkFileWatcher] Started watching bookmark file: " + customPath;
        LOG.info(startedMsg);
        LogCollector.getInstance().info("BookmarkFileWatcher", startedMsg);
    }

    /**
     * 停止文件监听
     */
    public void stopWatching() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
            String msg = "Stopped watching bookmark file: " + watchedFilePath;
            LOG.info(msg);
            LogCollector.getInstance().info("BookmarkFileWatcher", msg);
        }
        watchedFilePath = null;
    }

    /**
     * 处理文件变化事件
     */
    private void handleFileChange(VFileContentChangeEvent event) {
        if (watchedFilePath == null) {
            return;
        }

        VirtualFile changedFile = event.getFile();
        String changedPath = changedFile.getPath();

        // 检查是否是我们监听的文件
        if (!isSameFile(changedPath, watchedFilePath)) {
            return;
        }

        // 如果是内部修改，忽略
        if (isInternalChange) {
            String ignoreMsg = "Ignoring internal change to bookmark file";
            LOG.info(ignoreMsg);
            LogCollector.getInstance().info("BookmarkFileWatcher", ignoreMsg);
            isInternalChange = false;
            return;
        }

        // 检查文件修改时间，避免重复处理
        File file = new File(watchedFilePath);
        long currentModifiedTime = file.lastModified();
        if (currentModifiedTime <= lastModifiedTime) {
            return;
        }
        lastModifiedTime = currentModifiedTime;

        String changeMsg = "[BookmarkFileWatcher] Detected external change to bookmark file: " + watchedFilePath;
        LOG.info(changeMsg);
        LogCollector.getInstance().info("BookmarkFileWatcher", changeMsg);
        
        // 重新加载书签
        reloadBookmarks();
    }

    /**
     * 比较两个文件路径是否指向同一个文件
     */
    private boolean isSameFile(String path1, String path2) {
        if (path1 == null || path2 == null) {
            return false;
        }
        
        // 规范化路径（统一使用正斜杠）
        String normalizedPath1 = path1.replace("\\", "/");
        String normalizedPath2 = path2.replace("\\", "/");
        
        return normalizedPath1.equals(normalizedPath2);
    }

    /**
     * 重新加载书签
     */
    private void reloadBookmarks() {
        try {
            String msg = "Reloading bookmarks from external file change...";
            LOG.info(msg);
            LogCollector.getInstance().info("BookmarkFileWatcher", msg);
            
            // 获取BookmarksManager并重新加载
            BookmarksManager manager = BookmarksManager.getInstance(project);
            manager.reload();
            
            String successMsg = "Bookmarks reloaded successfully";
            LOG.info(successMsg);
            LogCollector.getInstance().info("BookmarkFileWatcher", successMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to reload bookmarks after file change";
            LOG.error(errorMsg, e);
            LogCollector.getInstance().error("BookmarkFileWatcher", errorMsg, e);
        }
    }

    /**
     * 标记即将进行内部修改（保存书签时调用）
     * 这样可以避免触发重新加载
     */
    public void markInternalChange() {
        this.isInternalChange = true;
        
        // 更新最后修改时间
        if (watchedFilePath != null) {
            File file = new File(watchedFilePath);
            if (file.exists()) {
                lastModifiedTime = file.lastModified();
            }
        }
    }

    /**
     * 获取当前监听的文件路径
     */
    public String getWatchedFilePath() {
        return watchedFilePath;
    }

    /**
     * 检查是否正在监听文件
     */
    public boolean isWatching() {
        return connection != null && watchedFilePath != null;
    }
}
