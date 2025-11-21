package indi.bookmarkx.persistence;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 项目级别的插件配置服务
 * 用于存储项目特定的配置，如自定义书签存储路径
 *
 * @author Nonoas
 * @date 2025/11/21
 */
@Service(Service.Level.PROJECT)
@State(
        name = "BookmarkX.ProjectSettings",
        storages = {@Storage("BookmarkX.ProjectSettings.xml")}  // 项目级别存储，保存在 .idea/ 目录下
)
public final class ProjectSettings implements PersistentStateComponent<ProjectSettings.State> {

    private State state = new State();

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public static ProjectSettings getInstance(Project project) {
        return project.getService(ProjectSettings.class);
    }

    /**
     * 获取自定义书签存储路径
     * 
     * @return 自定义存储路径，如果为空则使用默认路径
     */
    public String getCustomStoragePath() {
        return state.customStoragePath;
    }

    /**
     * 设置自定义书签存储路径
     * 
     * @param customStoragePath 自定义存储路径（完整文件路径，包含文件名）
     */
    public void setCustomStoragePath(String customStoragePath) {
        this.state.customStoragePath = customStoragePath;
    }

    @XmlRootElement
    public static class State {
        /**
         * 自定义书签存储路径（完整文件路径，包含文件名）
         * 如果为空，则使用默认路径 .idea/SuperBookmarkState.xml
         */
        public String customStoragePath;
    }
}
