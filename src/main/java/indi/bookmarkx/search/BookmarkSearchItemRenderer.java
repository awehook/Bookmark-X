package indi.bookmarkx.search;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import indi.bookmarkx.common.MyIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 书签搜索项渲染器
 * 用于在 Search Everywhere 中渲染书签项
 *
 * @author Nonoas
 */
public class BookmarkSearchItemRenderer extends ColoredListCellRenderer<BookmarkSearchItem> {

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends BookmarkSearchItem> list,
                                          BookmarkSearchItem value,
                                          int index,
                                          boolean selected,
                                          boolean hasFocus) {
        if (value == null) {
            return;
        }

        // 根据是否是分组设置不同的图标
        if (value.isGroup()) {
            setIcon(AllIcons.Nodes.Folder);
        } else {
            setIcon(MyIcons.BOOKMARK);
        }

        // 显示名称
        append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

        // 显示描述（如果有）
        if (value.getDescription() != null && !value.getDescription().isEmpty()) {
            append(" - " + value.getDescription(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        // 如果是书签，显示文件路径和行号
        if (!value.isGroup() && value.getFilePath() != null) {
            String fileName = extractFileName(value.getFilePath());
            String locationText = " (" + fileName + ":" + value.getLine() + ")";
            append(locationText, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
        }

        // 显示分组路径（如果有）
        if (value.getGroupPath() != null && !value.getGroupPath().isEmpty()) {
            append(" [" + value.getGroupPath() + "]", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
        }
    }

    /**
     * 从完整路径中提取文件名
     */
    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        // 处理 $PROJECT_DIR$ 占位符
        filePath = filePath.replace("$PROJECT_DIR$/", "").replace("$PROJECT_DIR$\\", "");
        
        // 提取文件名
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
            return filePath.substring(lastSlash + 1);
        }
        
        return filePath;
    }
}
