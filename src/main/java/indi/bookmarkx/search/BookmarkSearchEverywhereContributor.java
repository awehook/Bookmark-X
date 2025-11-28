package indi.bookmarkx.search;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.Processor;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.persistence.MyPersistent;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Search Everywhere 书签搜索贡献者
 * 允许用户通过按两次 Shift 来搜索书签
 *
 * @author Nonoas
 */
public class BookmarkSearchEverywhereContributor implements SearchEverywhereContributor<BookmarkSearchItem> {

    private static final Logger LOG = Logger.getInstance(BookmarkSearchEverywhereContributor.class);
    
    private final Project project;

    public BookmarkSearchEverywhereContributor(@NotNull AnActionEvent event) {
        this.project = event.getProject();
    }

    @NotNull
    @Override
    public String getSearchProviderId() {
        return "BookmarkSearchEverywhereContributor";
    }

    @NotNull
    @Override
    public String getGroupName() {
        return "Bookmarks";
    }

    @Override
    public int getSortWeight() {
        return 300; // 权重，决定在搜索结果中的位置
    }

    @Override
    public boolean showInFindResults() {
        return true;
    }

    @Override
    public void fetchElements(@NotNull String pattern,
                              @NotNull ProgressIndicator progressIndicator,
                              @NotNull Processor<? super BookmarkSearchItem> consumer) {
        if (project == null || project.isDisposed()) {
            return;
        }

        // 获取所有书签
        List<BookmarkSearchItem> allBookmarks = collectAllBookmarks();

        // 过滤匹配的书签
        String lowerPattern = pattern.toLowerCase();
        for (BookmarkSearchItem item : allBookmarks) {
            if (progressIndicator.isCanceled()) {
                break;
            }

            // 匹配书签名称、描述、文件路径
            if (matches(item, lowerPattern)) {
                consumer.process(item);
            }
        }
    }

    /**
     * 收集所有书签
     */
    private List<BookmarkSearchItem> collectAllBookmarks() {
        List<BookmarkSearchItem> result = new ArrayList<>();
        
        try {
            MyPersistent persistent = MyPersistent.getInstance(project);
            BookmarkPO state = persistent.getState();
            
            // 递归收集所有书签
            collectBookmarksRecursively(state, result, "");
        } catch (Exception e) {
            LOG.error("Failed to collect bookmarks", e);
        }
        
        return result;
    }

    /**
     * 递归收集书签和分组
     */
    private void collectBookmarksRecursively(BookmarkPO node, List<BookmarkSearchItem> result, String parentPath) {
        if (node == null) {
            return;
        }

        String currentPath = parentPath.isEmpty() ? node.getName() : parentPath + " > " + node.getName();

        // 如果是书签，添加到结果中
        if (node.isBookmark()) {
            result.add(new BookmarkSearchItem(
                    node.getUuid(),
                    node.getName(),
                    node.getDesc(),
                    node.getVirtualFilePath(),
                    node.getLine(),
                    parentPath,
                    false // 不是分组
            ));
        } else {
            // 如果是分组，也添加到结果中（但跳过根节点）
            if (!parentPath.isEmpty()) { // 不添加根节点
                result.add(new BookmarkSearchItem(
                        node.getUuid(),
                        node.getName(),
                        node.getDesc(),
                        null, // 分组没有文件路径
                        0,    // 分组没有行号
                        parentPath,
                        true  // 是分组
                ));
            }
        }

        // 递归处理子节点
        if (node.getChildren() != null) {
            for (BookmarkPO child : node.getChildren()) {
                collectBookmarksRecursively(child, result, currentPath);
            }
        }
    }

    /**
     * 判断书签是否匹配搜索模式
     */
    private boolean matches(BookmarkSearchItem item, String lowerPattern) {
        if (lowerPattern.isEmpty()) {
            return true;
        }

        // 匹配名称
        if (item.getName() != null && item.getName().toLowerCase().contains(lowerPattern)) {
            return true;
        }

        // 匹配描述
        if (item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerPattern)) {
            return true;
        }

        // 匹配文件路径
        if (item.getFilePath() != null && item.getFilePath().toLowerCase().contains(lowerPattern)) {
            return true;
        }

        // 匹配父路径（分组路径）
        if (item.getGroupPath() != null && item.getGroupPath().toLowerCase().contains(lowerPattern)) {
            return true;
        }

        return false;
    }

    /**
     * 计算元素优先级（用于排序）
     */
    @Override
    public int getElementPriority(@NotNull BookmarkSearchItem item, @NotNull String pattern) {
        String lowerPattern = pattern.toLowerCase();
        
        // 名称完全匹配优先级最高
        if (item.getName() != null && item.getName().equalsIgnoreCase(pattern)) {
            return 100;
        }
        
        // 名称开头匹配
        if (item.getName() != null && item.getName().toLowerCase().startsWith(lowerPattern)) {
            return 80;
        }
        
        // 描述匹配
        if (item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerPattern)) {
            return 60;
        }
        
        // 其他匹配
        return 40;
    }

    @NotNull
    @Override
    public String getAdvertisement() {
        return "Search for bookmarks and groups by name, description, or file path";
    }

    @NotNull
    @Override
    public ListCellRenderer<? super BookmarkSearchItem> getElementsRenderer() {
        return new BookmarkSearchItemRenderer();
    }

    @Nullable
    @Override
    public Object getDataForItem(@NotNull BookmarkSearchItem element, @NotNull String dataId) {
        return null;
    }

    @Override
    public boolean processSelectedItem(@NotNull BookmarkSearchItem selected, int modifiers, @NotNull String searchText) {
        if (project == null || project.isDisposed()) {
            return false;
        }

        try {
            // 通过 UUID 在树中定位书签或分组
            BookmarksManager manager = BookmarksManager.getInstance(project);
            BookmarkTree tree = manager.getToolWindowRootPanel().tree();
            
            // 直接通过UUID从nodeCache获取节点（分组和书签都会被缓存）
            BookmarkNodeModel tempModel = new BookmarkNodeModel();
            tempModel.setUuid(selected.getUuid());
            BookmarkTreeNode node = tree.getNodeByModel(tempModel);
            
            if (node != null && node.getUserObject() != null) {
                if (selected.isGroup()) {
                    // 如果是分组，直接在树中选中并展开
                    javax.swing.tree.TreePath treePath = new javax.swing.tree.TreePath(node.getPath());
                    tree.setSelectionPath(treePath);
                    tree.scrollPathToVisible(treePath);
                    tree.expandPath(treePath);
                } else {
                    // 如果是书签，使用原有的定位逻辑
                    BookmarkNodeModel bookmarkModel = (BookmarkNodeModel) node.getUserObject();
                    manager.getToolWindowRootPanel().locateBookmark(bookmarkModel);
                }
                
                // 激活工具窗口，确保用户能看到定位结果
                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                ToolWindow toolWindow = toolWindowManager.getToolWindow("Bookmark-X");
                if (toolWindow != null && !toolWindow.isVisible()) {
                    toolWindow.show();
                }
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            LOG.error("Failed to navigate to bookmark or group", e);
            return false;
        }
    }



    /**
     * Factory 类，用于创建 Contributor 实例
     */
    public static class Factory implements SearchEverywhereContributorFactory<BookmarkSearchItem> {
        @NotNull
        @Override
        public SearchEverywhereContributor<BookmarkSearchItem> createContributor(@NotNull AnActionEvent initEvent) {
            return new BookmarkSearchEverywhereContributor(initEvent);
        }
    }
}
