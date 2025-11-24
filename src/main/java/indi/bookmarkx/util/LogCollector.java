package indi.bookmarkx.util;

import com.intellij.openapi.application.ApplicationManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 日志收集器
 * 用于收集BookmarkXmlUtil和BookmarkFileWatcher的日志，并分发给UI显示
 *
 * @author Nonoas
 * @date 2025/11/24
 */
public class LogCollector {

    private static final LogCollector INSTANCE = new LogCollector();
    
    private final CopyOnWriteArrayList<LogListener> listeners = new CopyOnWriteArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    private LogCollector() {
    }
    
    public static LogCollector getInstance() {
        return INSTANCE;
    }
    
    /**
     * 添加日志监听器
     */
    public void addListener(LogListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除日志监听器
     */
    public void removeListener(LogListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 记录INFO级别日志
     */
    public void info(String source, String message) {
        log("INFO", source, message);
    }
    
    /**
     * 记录ERROR级别日志
     */
    public void error(String source, String message) {
        log("ERROR", source, message);
    }
    
    /**
     * 记录ERROR级别日志（带异常）
     */
    public void error(String source, String message, Throwable throwable) {
        String fullMessage = message + "\n" + getStackTrace(throwable);
        log("ERROR", source, fullMessage);
    }
    
    /**
     * 记录日志并通知所有监听器
     */
    private void log(String level, String source, String message) {
        String timestamp = dateFormat.format(new Date());
        String formattedLog = String.format("[%s] [%s] [%s] %s", timestamp, level, source, message);
        
        // 在EDT线程中通知监听器
        ApplicationManager.getApplication().invokeLater(() -> {
            for (LogListener listener : listeners) {
                listener.onLogReceived(formattedLog);
            }
        });
    }
    
    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        if (throwable.getCause() != null) {
            sb.append("Caused by: ").append(getStackTrace(throwable.getCause()));
        }
        
        return sb.toString();
    }
    
    /**
     * 日志监听器接口
     */
    public interface LogListener {
        void onLogReceived(String logMessage);
    }
}
