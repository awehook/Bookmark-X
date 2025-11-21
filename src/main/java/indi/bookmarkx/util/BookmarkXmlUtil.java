package indi.bookmarkx.util;

import com.intellij.openapi.diagnostic.Logger;
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
     * @return BookmarkPO对象，如果加载失败返回null
     */
    public static BookmarkPO loadFromFile(File file) {
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
     * @return 是否保存成功
     */
    public static boolean saveToFile(BookmarkPO bookmarkPO, File file) {
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
}
