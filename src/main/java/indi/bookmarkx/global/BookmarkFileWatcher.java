package indi.bookmarkx.global;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.persistence.ProjectSettings;
import indi.bookmarkx.util.LogCollector;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private String watchedFilePath;
    private long lastModifiedTime = 0;
    private final AtomicBoolean isInternalChange = new AtomicBoolean(false); // 标记是否为内部修改
    
    private WatchService watchService;
    private ExecutorService executorService;
    private final AtomicBoolean isWatching = new AtomicBoolean(false);

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
            LogCollector.getInstance().info("BookmarkFileWatcher", project, msg);
            return;
        }

        // 如果已经在监听同一个文件，不需要重新启动
        if (customPath.equals(watchedFilePath) && isWatching.get()) {
            String alreadyWatchingMsg = "[BookmarkFileWatcher] Already watching file: " + customPath;
            LOG.info(alreadyWatchingMsg);
            LogCollector.getInstance().info("BookmarkFileWatcher", project, alreadyWatchingMsg);
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

        try {
            // 创建 WatchService
            watchService = FileSystems.getDefault().newWatchService();
            
            // 获取文件所在目录
            Path dirPath = Paths.get(file.getParent());
            
            // 注册目录监听（监听修改事件）
            dirPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            
            // 创建线程池并启动监听线程
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "BookmarkFileWatcher-" + project.getName());
                thread.setDaemon(true);
                return thread;
            });
            
            isWatching.set(true);
            
            // 启动监听任务
            executorService.submit(this::watchFileChanges);
            
            String startedMsg = "[BookmarkFileWatcher] Started watching bookmark file: " + customPath;
            LOG.info(startedMsg);
            LogCollector.getInstance().info("BookmarkFileWatcher", project, startedMsg);
            
        } catch (IOException e) {
            String errorMsg = "Failed to start file watcher for: " + customPath;
            LOG.error(errorMsg, e);
            LogCollector.getInstance().error("BookmarkFileWatcher", project, errorMsg, e);
        }
    }

    /**
     * 停止文件监听
     */
    public void stopWatching() {
        isWatching.set(false);
        
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executorService = null;
        }
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                LOG.warn("Error closing watch service", e);
            }
            watchService = null;
        }
        
        if (watchedFilePath != null) {
            String msg = "Stopped watching bookmark file: " + watchedFilePath;
            LOG.info(msg);
            LogCollector.getInstance().info("BookmarkFileWatcher", project, msg);
        }
        
        watchedFilePath = null;
    }

    /**
     * 监听文件变化的主循环
     */
    private void watchFileChanges() {
        String fileName = new File(watchedFilePath).getName();
        
        while (isWatching.get()) {
            try {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    continue;
                }
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path changedFile = pathEvent.context();
                    
                    // 检查是否是我们监听的文件
                    if (changedFile.toString().equals(fileName)) {
                        handleFileChange();
                    }
                }
                
                // 重置key，继续监听
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOG.error("Error in file watch loop", e);
            }
        }
    }

    /**
     * 处理文件变化事件
     */
    private void handleFileChange() {
        if (watchedFilePath == null) {
            return;
        }

        // 如果是内部修改，忽略
        if (isInternalChange.getAndSet(false)) {
            String ignoreMsg = "Ignoring internal change to bookmark file";
            LOG.info(ignoreMsg);
            LogCollector.getInstance().info("BookmarkFileWatcher", project, ignoreMsg);
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
        LogCollector.getInstance().info("BookmarkFileWatcher", project, changeMsg);
        
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
            LogCollector.getInstance().info("BookmarkFileWatcher", project, msg);
            
            // 获取BookmarksManager并重新加载
            BookmarksManager manager = BookmarksManager.getInstance(project);
            String successMsg = "manager.reload()";
            LOG.info(successMsg);
            LogCollector.getInstance().info("BookmarkFileWatcher", project, successMsg);
            manager.reload();
        } catch (Exception e) {
            String errorMsg = "Failed to reload bookmarks after file change";
            LOG.error(errorMsg, e);
            LogCollector.getInstance().error("BookmarkFileWatcher", project, errorMsg, e);
        }
    }

    /**
     * 标记即将进行内部修改（保存书签时调用）
     * 这样可以避免触发重新加载
     */
    public void markInternalChange() {
        this.isInternalChange.set(true);
        
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
        return isWatching.get() && watchedFilePath != null;
    }
}
