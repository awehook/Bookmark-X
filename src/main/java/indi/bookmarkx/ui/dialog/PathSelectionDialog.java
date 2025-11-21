package indi.bookmarkx.ui.dialog;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import indi.bookmarkx.common.I18N;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 历史路径选择对话框
 *
 * @author Nonoas
 * @date 2025/11/21
 */
public class PathSelectionDialog extends DialogWrapper {

    private final Project project;
    private final List<String> historyPaths;
    private JBList<String> pathList;
    private String selectedPath;

    public PathSelectionDialog(Project project, List<String> historyPaths) {
        super(project);
        this.project = project;
        this.historyPaths = historyPaths;
        setTitle(I18N.get("bookmark.flatImport.selectPath.title"));
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(500, 300));

        // 提示标签
        JLabel label = new JLabel(I18N.get("bookmark.flatImport.selectPath.label"));
        panel.add(label, BorderLayout.NORTH);

        // 历史路径列表
        if (historyPaths != null && !historyPaths.isEmpty()) {
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String path : historyPaths) {
                listModel.addElement(path);
            }
            pathList = new JBList<>(listModel);
            pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            pathList.setSelectedIndex(0);

            JBScrollPane scrollPane = new JBScrollPane(pathList);
            panel.add(scrollPane, BorderLayout.CENTER);
        } else {
            JLabel emptyLabel = new JLabel(I18N.get("bookmark.flatImport.selectPath.empty"));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(emptyLabel, BorderLayout.CENTER);
        }

        // 浏览按钮
        JButton browseButton = new JButton(I18N.get("bookmark.flatImport.selectPath.browse"));
        browseButton.addActionListener(e -> browseForFile());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(browseButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void browseForFile() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true,
                false,
                false,
                false,
                false,
                false);
        descriptor.withFileFilter(file -> file != null && file.getName().toLowerCase().endsWith(".json"));
        descriptor.setTitle(I18N.get("bookmark.flatImport.selectPath.browse"));

        VirtualFile virtualFile = FileChooser.chooseFile(descriptor, project, null);
        if (virtualFile != null) {
            selectedPath = virtualFile.getPath();
            close(OK_EXIT_CODE);
        }
    }

    @Override
    protected void doOKAction() {
        if (pathList != null && pathList.getSelectedValue() != null) {
            selectedPath = pathList.getSelectedValue();
        }
        super.doOKAction();
    }

    public String getSelectedPath() {
        return selectedPath;
    }
}
