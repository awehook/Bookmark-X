package indi.bookmarkx.action;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowId;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.persistence.MyPersistent;
import indi.bookmarkx.persistence.MySettings;
import indi.bookmarkx.ui.dialog.ExportPathSelectionDialog;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static indi.bookmarkx.utils.PersistenceUtil.deepCopy;

/**
 * 书签平铺导出（所有节点记录父节点UUID）
 * 
 * @author Nonoas
 * @date 2025-11-21
 */
public final class BookmarkFlatExportAction extends AnAction {

    private Project project;

    public BookmarkFlatExportAction() {
        super(() -> "平铺导出", () -> "导出为平铺格式（所有节点记录父UUID）", AllIcons.Actions.Download);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        if (null == project) {
            return;
        }
        
        MyPersistent persistent = MyPersistent.getInstance(project);
        BookmarkPO state = persistent.getState();

        BookmarkPO copy = deepCopy(state, BookmarkPO.class);
        stateTranslate(copy);
        
        // 转换为平铺格式
        List<BookmarkPO> flatList = flattenBookmarks(copy);
        
        // 获取历史路径和默认路径
        MySettings settings = MySettings.getInstance();
        List<String> historyPaths = settings.getFlatImportHistoryPaths();
        String projectDir = FileUtil.toSystemIndependentName(Objects.requireNonNull(project.getBasePath()));
        String defaultPath = projectDir + File.separator + "Bookmark_X_Flat.json";
        
        // 如果历史路径为空，直接导出到默认位置
        if (historyPaths == null || historyPaths.isEmpty()) {
            saveToJsonFile(flatList, defaultPath);
            settings.addFlatImportHistoryPath(defaultPath);
            return;
        }
        
        // 显示路径选择对话框
        ExportPathSelectionDialog dialog = new ExportPathSelectionDialog(project, historyPaths, defaultPath);
        if (!dialog.showAndGet()) {
            return;
        }
        
        String selectedPath = dialog.getSelectedPath();
        if (selectedPath == null || selectedPath.trim().isEmpty()) {
            return;
        }
        
        // 如果选择的是目录，则添加文件名
        File file = new File(selectedPath);
        if (file.isDirectory()) {
            selectedPath = selectedPath + File.separator + "Bookmark_X_Flat.json";
        } else if (!selectedPath.endsWith(".json")) {
            selectedPath = selectedPath + File.separator + "Bookmark_X_Flat.json";
        }
        
        // 保存文件并添加到历史记录
        saveToJsonFile(flatList, selectedPath);
        settings.addFlatImportHistoryPath(selectedPath);
    }

    /**
     * 将树形结构转换为平铺结构
     * @param root 根节点
     * @return 平铺后的节点列表
     */
    private List<BookmarkPO> flattenBookmarks(BookmarkPO root) {
        List<BookmarkPO> flatList = new ArrayList<>();
        flattenBookmarksRecursive(root, null, null, flatList);
        return flatList;
    }

    /**
     * 递归遍历树形结构，将所有节点平铺
     * @param node 当前节点
     * @param parentUuid 父节点UUID
     * @param parentName 父节点名称
     * @param flatList 平铺列表
     */
    private void flattenBookmarksRecursive(BookmarkPO node, String parentUuid, String parentName, List<BookmarkPO> flatList) {
        // 设置父节点UUID和名称
        node.setParentUuid(parentUuid);
        node.setParentName(parentName);
        
        // 保存当前节点的UUID、名称和children
        String currentUuid = node.getUuid();
        String currentName = node.getName();
        List<BookmarkPO> children = node.getChildren() != null ? new ArrayList<>(node.getChildren()) : new ArrayList<>();
        
        // 清空children（平铺后不需要children）
        if(!node.isBookmark()) {
            node.setChildren(new ArrayList<>());
        }

        
        // 将当前节点添加到平铺列表
        flatList.add(node);
        
        // 递归处理子节点
        if (CollectionUtils.isNotEmpty(children)) {
            for (BookmarkPO child : children) {
                flattenBookmarksRecursive(child, currentUuid, currentName, flatList);
            }
        }
    }

    /**
     * 保存为JSON文件
     * @param flatList 平铺后的节点列表
     * @param outputPath 输出文件路径
     */
    private void saveToJsonFile(List<BookmarkPO> flatList, String outputPath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter fw = new FileWriter(outputPath)) {
            gson.toJson(flatList, fw);
        } catch (IOException e) {
            throw new RuntimeException("export error", e);
        }
        success(outputPath);
    }

    /**
     * 显示成功通知
     * @param outputPath 输出文件路径
     */
    private void success(String outputPath) {
        String groupId = ToolWindowId.PROJECT_VIEW;
        Notification notification = new Notification(groupId,
                I18N.get("bookmark.notification.title"),
                "平铺导出成功: " + outputPath,
                NotificationType.INFORMATION);
        Notifications.Bus.notify(notification, project);
    }

    /**
     * 将绝对路径转换为相对路径（$PROJECT_DIR$）
     * @param po 书签节点
     */
    private void stateTranslate(BookmarkPO po) {
        String basePath = project.getBasePath();
        if (null == basePath) {
            return;
        }
        String projectDir = FileUtil.toSystemIndependentName(basePath);
        stateTranslate(po, projectDir);
    }

    /**
     * 递归转换路径
     * @param po 书签节点
     * @param dir 项目目录
     */
    private void stateTranslate(BookmarkPO po, String dir) {
        String virtualFilePath = po.getVirtualFilePath();
        if (StringUtil.isNotEmpty(virtualFilePath)) {
            po.setVirtualFilePath(virtualFilePath.replace(dir, "$PROJECT_DIR$"));
        }
        if (CollectionUtils.isNotEmpty(po.getChildren())) {
            for (BookmarkPO child : po.getChildren()) {
                stateTranslate(child, dir);
            }
        }
    }
}
