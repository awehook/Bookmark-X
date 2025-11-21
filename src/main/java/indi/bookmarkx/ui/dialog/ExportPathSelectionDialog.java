package indi.bookmarkx.ui.dialog;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import indi.bookmarkx.common.I18N;
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
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String path : historyPaths) {
                // 统一显示为 / 分隔符
                String normalizedPath = path.replace("\\", "/");
                // 只显示目录路径
                File file = new File(normalizedPath);
                String dirPath = file.getParent();
                if (dirPath != null) {
                    // 目录路径也统一为 / 分隔符
                    String normalizedDirPath = dirPath.replace("\\", "/");
                    if (!listModel.contains(normalizedDirPath)) {
                        listModel.addElement(normalizedDirPath);
                    }
                }
            }
            pathList = new JBList<>(listModel);
            pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            if (listModel.size() > 0) {
                pathList.setSelectedIndex(0);
            }

            JBScrollPane scrollPane = new JBScrollPane(pathList);
            panel.add(scrollPane, BorderLayout.CENTER);
        } else {
            JLabel emptyLabel = new JLabel(I18N.get("bookmark.flatExport.selectPath.empty"));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(emptyLabel, BorderLayout.CENTER);
        }

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
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
                false,
                true,  // 只选择目录
                false,
                false,
                false,
                false);
        descriptor.setTitle(I18N.get("bookmark.flatExport.selectPath.browse"));

        VirtualFile virtualFile = FileChooser.chooseFile(descriptor, project, null);
        if (virtualFile != null) {
            // 统一将路径分隔符转换为 /
            selectedPath = virtualFile.getPath().replace("\\", "/");
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

    public boolean isUseDefaultPath() {
        return useDefaultPath;
    }
}
