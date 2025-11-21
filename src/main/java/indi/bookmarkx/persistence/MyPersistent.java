package indi.bookmarkx.persistence;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.model.po.BookmarkPO;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Nonoas
 * @date 2023/6/5
 */
@State(
        name = "SuperBookmarkState",
        storages = {@Storage("SuperBookmarkState.xml")}
)
public class MyPersistent implements PersistentStateComponent<BookmarkPO> {

    private static final Logger LOG = Logger.getInstance(MyPersistent.class);

    private BookmarkPO state;

    private final Project project;
    
    private JAXBContext jaxbContext;

    public MyPersistent(Project project) {
        this.project = project;
        try {
            jaxbContext = JAXBContext.newInstance(BookmarkPO.class);
        } catch (JAXBException e) {
            LOG.error("Failed to create JAXB context", e);
        }
    }

    public static MyPersistent getInstance(Project project) {
        return project.getService(MyPersistent.class);
    }

    public void setState(BookmarkPO state) {
        this.state = state;
        // 如果配置了自定义路径，保存到自定义路径
        saveToCustomPathIfConfigured();
    }

    @Override
    public @NotNull BookmarkPO getState() {
        LOG.info("获取：" + state);
        
        // 如果配置了自定义路径，从自定义路径加载
        loadFromCustomPathIfConfigured();
        
        if (state == null) {
            state = new BookmarkPO();
            state.setBookmark(false);
        }
        state.setName(project.getName());
        return state;
    }

    @Override
    public void loadState(@NotNull BookmarkPO state) {
        LOG.info("加载：" + state);
        this.state = state;
    }
    
    /**
     * 如果配置了自定义路径，从自定义路径加载书签数据
     */
    private void loadFromCustomPathIfConfigured() {
        String customPath = MySettings.getInstance().getCustomStoragePath();
        if (StringUtils.isBlank(customPath)) {
            return;
        }
        
        File customFile = new File(customPath);
        if (!customFile.exists()) {
            LOG.info("Custom storage file does not exist: " + customPath);
            return;
        }
        
        try (FileReader reader = new FileReader(customFile)) {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            BookmarkPO loadedState = (BookmarkPO) unmarshaller.unmarshal(reader);
            if (loadedState != null) {
                this.state = loadedState;
                LOG.info("Loaded bookmarks from custom path: " + customPath);
            }
        } catch (JAXBException | IOException e) {
            LOG.error("Failed to load bookmarks from custom path: " + customPath, e);
        }
    }
    
    /**
     * 如果配置了自定义路径，保存书签数据到自定义路径
     */
    private void saveToCustomPathIfConfigured() {
        String customPath = MySettings.getInstance().getCustomStoragePath();
        if (StringUtils.isBlank(customPath) || state == null) {
            return;
        }
        
        File customFile = new File(customPath);
        // 确保父目录存在
        File parentDir = customFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                LOG.error("Failed to create parent directory: " + parentDir.getAbsolutePath());
                return;
            }
        }
        
        try (FileWriter writer = new FileWriter(customFile)) {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.marshal(state, writer);
            LOG.info("Saved bookmarks to custom path: " + customPath);
        } catch (JAXBException | IOException e) {
            LOG.error("Failed to save bookmarks to custom path: " + customPath, e);
        }
    }
}
