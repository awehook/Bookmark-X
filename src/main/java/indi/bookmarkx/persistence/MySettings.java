package indi.bookmarkx.persistence;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import indi.bookmarkx.common.I18NEnum;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件持久化服务
 *
 * @author Nonoas
 * @date 2024/10/11 16:04
 */
@Service(Service.Level.APP)
@State(
        name = "BookmarkX.setting",
        storages = {@Storage("BookmarkX.setting.xml")}  // 应用级别存储
)
public final class MySettings implements PersistentStateComponent<MySettings.State> {

    private State state = new State();

    @Override
    public @NotNull State getState() {
        if (StringUtils.isBlank(state.language)) {
            state.language = I18NEnum.getDefault().name();
        }
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public static MySettings getInstance() {
        return ApplicationManager.getApplication().getService(MySettings.class);
    }

    //public I18NEnum
    public I18NEnum getLanguage() {
        return I18NEnum.valueOf(this.state.language);
    }

    public void setLanguage(I18NEnum lang) {
        this.state.language = lang.name();
    }

    public int getTipDelay() {
        return state.tipDelay;
    }

    public void setTipDelay(final int tipDelay) {
        state.tipDelay = tipDelay;
    }

    public List<String> getFlatImportHistoryPaths() {
        if (state.flatImportHistoryPaths == null) {
            state.flatImportHistoryPaths = new ArrayList<>();
        }
        return state.flatImportHistoryPaths;
    }

    public void addFlatImportHistoryPath(String path) {
        if (state.flatImportHistoryPaths == null) {
            state.flatImportHistoryPaths = new ArrayList<>();
        }
        // 如果路径已存在，先移除（保证最新的在最前面）
        state.flatImportHistoryPaths.remove(path);
        // 添加到列表开头
        state.flatImportHistoryPaths.add(0, path);
        // 最多保留10条历史记录
        if (state.flatImportHistoryPaths.size() > 10) {
            state.flatImportHistoryPaths = state.flatImportHistoryPaths.subList(0, 10);
        }
    }

    @XmlRootElement
    public static class State {
        public String language;
        public int tipDelay;
        public List<String> flatImportHistoryPaths = new ArrayList<>();
    }

}
