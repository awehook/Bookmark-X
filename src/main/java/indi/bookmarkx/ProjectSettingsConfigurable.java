package indi.bookmarkx;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.persistence.MyPersistent;
import indi.bookmarkx.persistence.ProjectSettings;
import indi.bookmarkx.ui.ProjectSettingsPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import static indi.bookmarkx.common.Constants.PLUGIN_NAME;

/**
 * 项目级别的配置类
 *
 * @author Nonoas
 * @date 2025/11/21
 */
public class ProjectSettingsConfigurable implements Configurable {

    private final Project project;
    private ProjectSettingsPanel settingsComponent;

    public ProjectSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return PLUGIN_NAME;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new ProjectSettingsPanel(project);
        return settingsComponent;
    }

    @Override
    public boolean isModified() {
        ProjectSettings settings = ProjectSettings.getInstance(project);
        return isCustomStoragePathChanged(settings);
    }
    
    private boolean isCustomStoragePathChanged(ProjectSettings settings) {
        String currentPath = settings.getCustomStoragePath();
        String newPath = settingsComponent.getCustomStoragePath();
        
        if (currentPath == null && newPath == null) {
            return false;
        }
        if (currentPath == null || newPath == null) {
            return true;
        }
        return !currentPath.equals(newPath);
    }

    @Override
    public void apply() {
        ProjectSettings settings = ProjectSettings.getInstance(project);
        boolean storagePathChanged = isCustomStoragePathChanged(settings);
        
        settings.setCustomStoragePath(settingsComponent.getCustomStoragePath());

        if (storagePathChanged) {
            // 重新启动文件监听器
            MyPersistent persistent = MyPersistent.getInstance(project);
            persistent.getFileWatcher().stopWatching();
            persistent.getFileWatcher().startWatching();
            
            showRestartDialog();
        }
    }

    @Override
    public void reset() {
        ProjectSettings settings = ProjectSettings.getInstance(project);
        settingsComponent.setCustomStoragePath(settings.getCustomStoragePath());
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }

    private void showRestartDialog() {
        Messages.showInfoMessage(
                project,
                I18N.get("setting.restartMessage"),
                I18N.get("setting.restartTile")
        );
    }
}
