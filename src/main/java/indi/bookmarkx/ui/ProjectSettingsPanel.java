package indi.bookmarkx.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.persistence.ProjectSettings;
import org.apache.commons.lang3.StringUtils;

import javax.swing.Box;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * 项目级别的插件设置面板
 *
 * @author Nonoas
 * @date 2025/11/21
 */
public class ProjectSettingsPanel extends JBPanel<ProjectSettingsPanel> {

    private final TextFieldWithBrowseButton customStoragePathField = new TextFieldWithBrowseButton();
    private final Project project;

    public ProjectSettingsPanel(Project project) {
        this.project = project;
        ProjectSettings settings = ProjectSettings.getInstance(project);
        
        // 设置面板布局
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; // 组件水平填充
        gbc.anchor = GridBagConstraints.NORTHWEST; // 组件靠左对齐
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // 添加自定义存储路径配置
        JPanel storagePathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        storagePathPanel.add(new JBLabel(I18N.get("setting.customStoragePath")));
        
        // 配置文件选择器
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("xml");
        descriptor.setTitle(I18N.get("setting.customStoragePath.choose"));
        descriptor.setDescription(I18N.get("setting.customStoragePath.desc"));
        customStoragePathField.addBrowseFolderListener(
                I18N.get("setting.customStoragePath.choose"),
                I18N.get("setting.customStoragePath.desc"),
                project,
                descriptor
        );
        
        // 设置当前值
        String currentPath = settings.getCustomStoragePath();
        if (StringUtils.isNotBlank(currentPath)) {
            customStoragePathField.setText(currentPath);
        }
        
        storagePathPanel.add(customStoragePathField);
        add(storagePathPanel, gbc);

        gbc.gridy++;
        gbc.weighty = 1;
        add(Box.createVerticalStrut(10), gbc);
    }

    public String getCustomStoragePath() {
        String path = customStoragePathField.getText();
        return StringUtils.isBlank(path) ? null : path.trim();
    }

    public void setCustomStoragePath(String path) {
        customStoragePathField.setText(path == null ? "" : path);
    }
}
