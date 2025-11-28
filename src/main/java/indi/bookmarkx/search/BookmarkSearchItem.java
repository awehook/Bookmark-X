package indi.bookmarkx.search;

/**
 * 书签搜索项
 * 用于在 Search Everywhere 中展示的书签信息
 *
 * @author Nonoas
 */
public class BookmarkSearchItem {
    
    private final String uuid;
    private final String name;
    private final String description;
    private final String filePath;
    private final int line;
    private final String groupPath; // 分组路径，如 "group1 > group2"

    public BookmarkSearchItem(String uuid, String name, String description, String filePath, int line, String groupPath) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.filePath = filePath;
        this.line = line;
        this.groupPath = groupPath;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLine() {
        return line;
    }

    public String getGroupPath() {
        return groupPath;
    }

    @Override
    public String toString() {
        return name;
    }
}
