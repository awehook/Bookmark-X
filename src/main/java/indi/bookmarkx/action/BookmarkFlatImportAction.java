package indi.bookmarkx.action;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.persistence.MyPersistent;
import indi.bookmarkx.persistence.MySettings;
import indi.bookmarkx.ui.dialog.PathSelectionDialog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

/**
 * 平铺格式书签导入Action
 *
 * @author Nonoas
 * @date 2025/11/21
 */
public class BookmarkFlatImportAction extends AnAction {

    private Project project;

    public BookmarkFlatImportAction() {
        super(() -> I18N.get("bookmark.flatImport"), () -> null, AllIcons.ToolbarDecorator.Import);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        if (project == null) {
            return;
        }

        MySettings settings = MySettings.getInstance();
        List<String> historyPaths = settings.getFlatImportHistoryPaths();

        // 显示路径选择对话框
        PathSelectionDialog dialog = new PathSelectionDialog(project, historyPaths);
        if (!dialog.showAndGet()) {
            return;
        }

        String selectedPath = dialog.getSelectedPath();
        if (selectedPath == null || selectedPath.trim().isEmpty()) {
            return;
        }

        // 执行导入
        try {
            importFromFlatJson(selectedPath);
            // 保存到历史记录
            settings.addFlatImportHistoryPath(selectedPath);
            Messages.showInfoMessage(
                    project,
                    I18N.get("bookmark.flatImport.success"),
                    I18N.get("bookmark.flatImport.title")
            );
        } catch (Exception ex) {
            Messages.showErrorDialog(
                    project,
                    I18N.get("bookmark.flatImport.error") + ": " + ex.getMessage(),
                    I18N.get("bookmark.flatImport.title")
            );
        }
    }

    /**
     * 从平铺JSON导入书签
     */
    private void importFromFlatJson(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        // 读取JSON文件
        String content = new String(Files.readAllBytes(file.toPath()));
        Gson gson = new Gson();
        Type listType = new TypeToken<List<BookmarkPO>>() {}.getType();
        List<BookmarkPO> flatList = gson.fromJson(content, listType);

        if (flatList == null || flatList.isEmpty()) {
            throw new IOException("Empty or invalid JSON file");
        }

        // 将平铺列表转换为树形结构
        BookmarkPO rootNode = convertFlatListToTree(flatList);

        // 路径转换
        stateTranslate(rootNode);

        // 清除现有数据并导入新数据
        MyPersistent persistent = MyPersistent.getInstance(project);
        persistent.setState(rootNode);

        // 重新加载
        BookmarksManager.getInstance(project).reload();
    }

    /**
     * 将平铺列表转换为树形结构
     */
    private BookmarkPO convertFlatListToTree(List<BookmarkPO> flatList) {
        // 创建UUID到节点的映射
        Map<String, BookmarkPO> nodeMap = new HashMap<>();
        BookmarkPO root = null;

        // 第一遍：将所有节点放入map
        for (BookmarkPO node : flatList) {
            nodeMap.put(node.getUuid(), node);
            // 初始化children列表
            if (!node.isBookmark()) {
                node.setChildren(new ArrayList<>());
            }
        }

        // 第二遍：构建树形结构
        for (BookmarkPO node : flatList) {
            String parentUuid = node.getParentUuid();
            if (parentUuid == null || parentUuid.trim().isEmpty()) {
                // 根节点
                root = node;
            } else {
                // 找到父节点并添加为子节点
                BookmarkPO parent = nodeMap.get(parentUuid);
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        // 如果没有找到根节点，创建一个默认根节点
        if (root == null) {
            root = new BookmarkPO();
            root.setName(project.getName());
            root.setBookmark(false);
            root.setChildren(new ArrayList<>(flatList));
        }

        return root;
    }

    /**
     * 路径转换：将$PROJECT_DIR$替换为实际项目路径
     */
    private void stateTranslate(BookmarkPO po) {
        String basePath = project.getBasePath();
        if (null == basePath) {
            return;
        }
        String projectDir = FileUtil.toSystemIndependentName(basePath);
        stateTranslate(po, projectDir);
    }

    private void stateTranslate(BookmarkPO po, String dir) {
        String virtualFilePath = po.getVirtualFilePath();
        if (StringUtil.isNotEmpty(virtualFilePath)) {
            po.setVirtualFilePath(virtualFilePath.replace("$PROJECT_DIR$", dir));
        }
        if (po.getChildren() != null && !po.getChildren().isEmpty()) {
            for (BookmarkPO child : po.getChildren()) {
                stateTranslate(child, dir);
            }
        }
    }
}
