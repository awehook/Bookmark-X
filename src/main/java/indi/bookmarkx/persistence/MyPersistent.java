package indi.bookmarkx.persistence;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.global.BookmarkFileWatcher;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.util.BookmarkXmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * @author Nonoas
 * @date 2023/6/5
 */
@State(
        name = "SuperBookmarkState",
        storages = {@Storage("SuperBookmarkState.xml")}
)
public class MyPersistent implements PersistentStateComponent<BookmarkPO> {

    private static final Logger LOG = Logger.getInstance(MyPersistent.class);

    private BookmarkPO state;

    private final Project project;
    
    private final BookmarkFileWatcher fileWatcher;
    
    // 标志位：是否已从自定义路径加载过
    private boolean hasLoadedFromCustomPath = false;

    public MyPersistent(Project project) {
        this.project = project;
        this.fileWatcher = new BookmarkFileWatcher(project);
        
        // 启动文件监听
        fileWatcher.startWatching();
    }

    public static MyPersistent getInstance(Project project) {
        return project.getService(MyPersistent.class);
    }

    public void setState(BookmarkPO state) {
        this.state = state;
        // 如果配置了自定义路径，保存到自定义路径
        saveToCustomPathIfConfigured();
    }

    @Override
    public @NotNull BookmarkPO getState() {
        LOG.info("获取：" + state);
        
        if (state == null) {
            state = new BookmarkPO();
            state.setBookmark(false);
        }
        state.setName(project.getName());
        return state;
    }

    @Override
    public void loadState(@NotNull BookmarkPO state) {
        LOG.info("加载：" + state);
        this.state = state;
    }
    
    /**
     * 如果配置了自定义路径，从自定义路径加载书签数据
     */
    private void loadFromCustomPathIfConfigured() {
        String customPath = ProjectSettings.getInstance(project).getCustomStoragePath();
        LOG.info(String.format("loadFromCustomPathIfConfigured - Project: %s, Custom Path: %s",
                project.getBasePath(), customPath));

        if (StringUtils.isBlank(customPath)) {
            return;
        }
        
        File customFile = new File(customPath);
        if (!customFile.exists()) {
            LOG.info("Custom storage file does not exist: " + customPath);
            return;
        }
        
        BookmarkPO loadedState = BookmarkXmlUtil.loadFromFile(customFile, project);
        if (loadedState != null) {
            this.state = loadedState;
            LOG.info(project.getBasePath() + "Loaded bookmarks from custom path: " + customPath);
            // printStateTree(state);
        }
    }
    
    /**
     * 如果配置了自定义路径，保存书签数据到自定义路径
     */
    private void saveToCustomPathIfConfigured() {
        String customPath = ProjectSettings.getInstance(project).getCustomStoragePath();
        if (StringUtils.isBlank(customPath) || state == null) {
            return;
        }
        
        File customFile = new File(customPath);
        // 确保父目录存在
        File parentDir = customFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                LOG.error("Failed to create parent directory: " + parentDir.getAbsolutePath());
                return;
            }
        }
        
        // 标记为内部修改，避免触发重新加载
        fileWatcher.markInternalChange();
        
        boolean success = BookmarkXmlUtil.saveToFile(state, customFile, project);
        if (success) {
            LOG.info("Saved bookmarks to custom path: " + customPath);
        }
    }
    
    /**
     * 强制重新加载（用于文件监听器检测到外部变更时调用）
     */
    public void forceReload() {
        hasLoadedFromCustomPath = false;
        loadFromCustomPathIfConfigured();
        hasLoadedFromCustomPath = true;
        LOG.info("Forced reload completed for project: " + project.getBasePath());
    }
    
    /**
     * 获取文件监听器
     */
    public BookmarkFileWatcher getFileWatcher() {
        return fileWatcher;
    }
    
    /**
     * 以树状形式打印书签结构（用于调试）
     */
    public void printStateTree(BookmarkPO state) {
        if (state == null) {
            LOG.info("State is null");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== Bookmark Tree ==========\n");
        printBookmarkNode(state, "", true, sb);
        sb.append("===================================\n");
        LOG.info(sb.toString());
    }
    
    /**
     * 递归打印书签节点
     * @param node 当前节点
     * @param prefix 前缀（用于缩进）
     * @param isLast 是否是最后一个子节点
     * @param sb 字符串构建器
     */
    private void printBookmarkNode(BookmarkPO node, String prefix, boolean isLast, StringBuilder sb) {
        if (node == null) {
            return;
        }
        
        // 打印当前节点
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        
        // 如果是分组，添加[g]标记
        if (!node.isBookmark()) {
            sb.append("[g] ");
        }
        
        sb.append(node.getName());
        sb.append("\n");
        
        // 递归打印子节点
        List<BookmarkPO> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                boolean isLastChild = (i == children.size() - 1);
                String childPrefix = prefix + (isLast ? "    " : "│   ");
                printBookmarkNode(children.get(i), childPrefix, isLastChild, sb);
            }
        }
    }
}
