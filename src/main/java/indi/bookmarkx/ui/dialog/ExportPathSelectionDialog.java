package indi.bookmarkx.ui.dialog;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.persistence.MySettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * 导出路径选择对话框
 *
 * @author Nonoas
 * @date 2025/11/21
 */
public class ExportPathSelectionDialog extends DialogWrapper {

    private final Project project;
    private final List<String> historyPaths;
    private final String defaultPath;
    private JBList<String> pathList;
    private DefaultListModel<String> listModel;
    private String selectedPath;
    private boolean useDefaultPath = false;

    public ExportPathSelectionDialog(Project project, List<String> historyPaths, String defaultPath) {
        super(project);
        this.project = project;
        this.historyPaths = historyPaths;
        this.defaultPath = defaultPath;
        setTitle(I18N.get("bookmark.flatExport.selectPath.title"));
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(500, 350));

        // 提示标签
        JLabel label = new JLabel(I18N.get("bookmark.flatExport.selectPath.label"));
        panel.add(label, BorderLayout.NORTH);

        // 历史路径列表
        if (historyPaths != null && !historyPaths.isEmpty()) {
            listModel = new DefaultListModel<>();
            for (String path : historyPaths) {
                // 统一显示为 / 分隔符
                String normalizedPath = path.replace("\\", "/");
                
                // 去重添加完整路径到列表
                if (!normalizedPath.isEmpty() && !listModel.contains(normalizedPath)) {
                    listModel.addElement(normalizedPath);
                }
            }
            
            // 只有当列表不为空时才创建列表组件
            if (listModel.size() > 0) {
                pathList = new JBList<>(listModel);
                pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                pathList.setSelectedIndex(0);

                JBScrollPane scrollPane = new JBScrollPane(pathList);
                panel.add(scrollPane, BorderLayout.CENTER);
            } else {
                JLabel emptyLabel = new JLabel(I18N.get("bookmark.flatExport.selectPath.empty"));
                emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                panel.add(emptyLabel, BorderLayout.CENTER);
            }
        } else {
            JLabel emptyLabel = new JLabel(I18N.get("bookmark.flatExport.selectPath.empty"));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(emptyLabel, BorderLayout.CENTER);
        }

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // 删除按钮
        JButton removeButton = new JButton(I18N.get("bookmark.flatExport.selectPath.remove"));
        removeButton.addActionListener(e -> removeSelectedPath());
        removeButton.setEnabled(pathList != null && listModel != null && !listModel.isEmpty());
        buttonPanel.add(removeButton);
        
        // 默认位置按钮
        JButton defaultButton = new JButton(I18N.get("bookmark.flatExport.selectPath.useDefault"));
        defaultButton.addActionListener(e -> {
            useDefaultPath = true;
            selectedPath = defaultPath;
            close(OK_EXIT_CODE);
        });
        buttonPanel.add(defaultButton);
        
        // 浏览按钮
        JButton browseButton = new JButton(I18N.get("bookmark.flatExport.selectPath.browse"));
        browseButton.addActionListener(e -> browseForDirectory());
        buttonPanel.add(browseButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void browseForDirectory() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true,   // 允许选择文件
                true,   // 允许选择目录
                false,
                false,
                false,
                false);
        descriptor.setTitle(I18N.get("bookmark.flatExport.selectPath.browse"));
        descriptor.withFileFilter(file -> file.isDirectory() || file.getName().endsWith(".json"));

        VirtualFile virtualFile = FileChooser.chooseFile(descriptor, project, null);
        if (virtualFile != null) {
            // 统一将路径分隔符转换为 /
            String path = virtualFile.getPath().replace("\\", "/");
            
            // 如果选择的是目录，添加默认文件名
            if (virtualFile.isDirectory()) {
                if (!path.endsWith("/")) {
                    path += "/";
                }
                path += "Bookmark_X_Flat.json";
            }
            
            selectedPath = path;
            useDefaultPath = false;
            close(OK_EXIT_CODE);
        }
    }

    @Override
    protected void doOKAction() {
        if (pathList != null && pathList.getSelectedValue() != null) {
            selectedPath = pathList.getSelectedValue();
            useDefaultPath = false;
        }
        super.doOKAction();
    }

    public String getSelectedPath() {
        return selectedPath;
    }

    /**
     * 删除选中的历史路径
     */
    private void removeSelectedPath() {
        if (pathList == null || listModel == null) {
            return;
        }
        
        String selectedValue = pathList.getSelectedValue();
        if (selectedValue != null) {
            // 从设置中删除
            MySettings.getInstance().removeFlatImportHistoryPath(selectedValue);
            
            // 从列表模型中删除
            listModel.removeElement(selectedValue);
            
            // 如果列表不为空，选择第一项
            if (!listModel.isEmpty()) {
                pathList.setSelectedIndex(0);
            }
        }
    }

    public boolean isUseDefaultPath() {
        return useDefaultPath;
    }
}
