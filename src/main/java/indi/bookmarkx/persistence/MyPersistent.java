package indi.bookmarkx.persistence;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.util.BookmarkXmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

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

    public MyPersistent(Project project) {
        this.project = project;
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
        
        // 如果配置了自定义路径，从自定义路径加载
        loadFromCustomPathIfConfigured();
        
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
        String customPath = MySettings.getInstance().getCustomStoragePath();
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
            LOG.info("Loaded bookmarks from custom path: " + customPath);
        }
    }
    
    /**
     * 如果配置了自定义路径，保存书签数据到自定义路径
     */
    private void saveToCustomPathIfConfigured() {
        String customPath = MySettings.getInstance().getCustomStoragePath();
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
        
        boolean success = BookmarkXmlUtil.saveToFile(state, customFile, project);
        if (success) {
            LOG.info("Saved bookmarks to custom path: " + customPath);
        }
    }
}
