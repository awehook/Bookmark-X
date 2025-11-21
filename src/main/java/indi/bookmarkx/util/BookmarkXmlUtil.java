package indi.bookmarkx.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializer;
import indi.bookmarkx.model.po.BookmarkPO;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;

/**
 * 书签XML工具类，用于处理IntelliJ格式的XML文件
 * IntelliJ格式：<project><component name="SuperBookmarkState"><option>...</option></component></project>
 * 
 * @author Nonoas
 * @date 2025/11/21
 */
public class BookmarkXmlUtil {
    
    private static final Logger LOG = Logger.getInstance(BookmarkXmlUtil.class);
    private static final String COMPONENT_NAME = "SuperBookmarkState";
    
    /**
     * 从IntelliJ格式的XML文件中加载BookmarkPO对象
     * 
     * @param file XML文件
     * @param project 项目对象，用于路径宏展开
     * @return BookmarkPO对象，如果加载失败返回null
     */
    public static BookmarkPO loadFromFile(File file, Project project) {
        if (file == null || !file.exists()) {
            LOG.warn("File does not exist: " + (file != null ? file.getAbsolutePath() : "null"));
            return null;
        }
        
        try {
            // 使用JDOM解析XML文件
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(file);
            Element rootElement = document.getRootElement();
            
            // 查找component节点
            Element componentElement = null;
            for (Object obj : rootElement.getChildren("component")) {
                Element component = (Element) obj;
                String name = component.getAttributeValue("name");
                
                if (COMPONENT_NAME.equals(name)) {
                    componentElement = component;
                    break;
                }
            }
            
            if (componentElement == null) {
                LOG.warn("No component element found with name: " + COMPONENT_NAME);
                return null;
            }
            
            // 展开路径宏（$PROJECT_DIR$ -> 实际路径）
            if (project != null) {
                try {
                    expandPathMacros(componentElement, project);
                } catch (Exception e) {
                    LOG.warn("Failed to expand path macros, continuing anyway", e);
                }
            }
            
            // 使用IntelliJ的XmlSerializer反序列化
            BookmarkPO bookmarkPO = XmlSerializer.deserialize(componentElement, BookmarkPO.class);
            
            if (bookmarkPO == null) {
                LOG.warn("Failed to deserialize BookmarkPO from file: " + file.getAbsolutePath());
                return null;
            }
            
            LOG.info("Successfully loaded bookmarks from: " + file.getAbsolutePath());
            return bookmarkPO;
            
        } catch (Exception e) {
            LOG.error("Failed to load bookmarks from file: " + file.getAbsolutePath(), e);
            return null;
        }
    }
    
    /**
     * 将BookmarkPO对象保存为IntelliJ格式的XML文件
     * 
     * @param bookmarkPO BookmarkPO对象
     * @param file 目标文件
     * @param project 项目对象，用于路径宏折叠
     * @return 是否保存成功
     */
    public static boolean saveToFile(BookmarkPO bookmarkPO, File file, Project project) {
        if (bookmarkPO == null || file == null) {
            LOG.warn("Invalid parameters: bookmarkPO or file is null");
            return false;
        }
        
        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    LOG.error("Failed to create parent directory: " + parentDir.getAbsolutePath());
                    return false;
                }
            }
            
            // 使用IntelliJ的XmlSerializer序列化BookmarkPO
            Element componentElement = XmlSerializer.serialize(bookmarkPO);
            componentElement.setAttribute("name", COMPONENT_NAME);
            
            // 折叠路径宏（实际路径 -> $PROJECT_DIR$）
            if (project != null) {
                try {
                    collapsePathMacros(componentElement, project);
                } catch (Exception e) {
                    LOG.warn("Failed to collapse path macros, continuing anyway", e);
                }
            }
            
            // 创建IntelliJ格式的XML文档结构
            Element projectElement = new Element("project");
            projectElement.setAttribute("version", "4");
            projectElement.addContent(componentElement);
            
            Document document = new Document(projectElement);
            
            // 写入文件
            XMLOutputter xmlOutputter = new XMLOutputter();
            Format format = Format.getPrettyFormat();
            format.setIndent("  ");
            format.setEncoding("UTF-8");
            xmlOutputter.setFormat(format);
            
            try (FileWriter writer = new FileWriter(file)) {
                xmlOutputter.output(document, writer);
            }
            
            LOG.info("Successfully saved bookmarks to: " + file.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to save bookmarks to file: " + file.getAbsolutePath(), e);
            return false;
        }
    }
    
    /**
     * 展开路径宏（$PROJECT_DIR$ -> 实际路径）
     * 
     * @param element XML元素
     * @param project 项目对象
     */
    private static void expandPathMacros(Element element, Project project) {
        if (element == null || project == null) {
            return;
        }
        
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return;
        }
        
        // 处理当前元素的属性
        String value = element.getAttributeValue("value");
        if (value != null && value.contains("$PROJECT_DIR$")) {
            String expandedValue = value.replace("$PROJECT_DIR$", projectPath);
            element.setAttribute("value", expandedValue);
            LOG.debug("Expanded path macro: " + value + " -> " + expandedValue);
        }
        
        // 递归处理子元素
        for (Object child : element.getChildren()) {
            if (child instanceof Element) {
                expandPathMacros((Element) child, project);
            }
        }
    }
    
    /**
     * 折叠路径宏（实际路径 -> $PROJECT_DIR$）
     * 
     * @param element XML元素
     * @param project 项目对象
     */
    private static void collapsePathMacros(Element element, Project project) {
        if (element == null || project == null) {
            return;
        }
        
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return;
        }
        
        // 规范化项目路径（统一使用正斜杠）
        String normalizedProjectPath = projectPath.replace("\\", "/");
        
        // 处理当前元素的属性
        String value = element.getAttributeValue("value");
        if (value != null && value.contains(normalizedProjectPath)) {
            String collapsedValue = value.replace(normalizedProjectPath, "$PROJECT_DIR$");
            element.setAttribute("value", collapsedValue);
            LOG.debug("Collapsed path macro: " + value + " -> " + collapsedValue);
        } else if (value != null && value.contains(projectPath)) {
            // 处理反斜杠路径
            String collapsedValue = value.replace(projectPath, "$PROJECT_DIR$");
            element.setAttribute("value", collapsedValue);
            LOG.debug("Collapsed path macro: " + value + " -> " + collapsedValue);
        }
        
        // 递归处理子元素
        for (Object child : element.getChildren()) {
            if (child instanceof Element) {
                collapsePathMacros((Element) child, project);
            }
        }
    }
}
