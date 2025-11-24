package indi.bookmarkx.ui.pannel;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.util.LogCollector;

import javax.swing.*;
import java.awt.*;

/**
 * 日志显示面板
 * 用于显示BookmarkXmlUtil和BookmarkFileWatcher的日志信息
 *
 * @author Nonoas
 * @date 2025/11/24
 */
public class LogPanel extends JPanel implements LogCollector.LogListener {

    private final JTextArea logTextArea;
    private final JScrollPane scrollPane;
    private static final int MAX_LOG_LINES = 1000; // 最大日志行数
    
    public LogPanel() {
        setLayout(new BorderLayout());
        
        // 创建日志文本区域
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logTextArea.setBackground(new JBColor(new Color(43, 43, 43), new Color(43, 43, 43)));
        logTextArea.setForeground(new JBColor(new Color(169, 183, 198), new Color(169, 183, 198)));
        logTextArea.setLineWrap(false);
        
        // 创建滚动面板
        scrollPane = new JBScrollPane(logTextArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        
        // 创建工具栏
        JPanel toolBar = createToolBar();
        
        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // 设置边框
        setBorder(JBUI.Borders.empty(2));
        
        // 注册日志监听器
        LogCollector.getInstance().addListener(this);
    }
    
    /**
     * 创建工具栏
     */
    private JPanel createToolBar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        // 清空日志按钮
        JButton clearButton = new JButton("清空日志");
        clearButton.addActionListener(e -> clearLogs());
        
        // 自动滚动复选框
        JCheckBox autoScrollCheckBox = new JCheckBox("自动滚动", true);
        autoScrollCheckBox.addActionListener(e -> {
            // 可以在这里添加自动滚动的逻辑
        });
        
        toolBar.add(clearButton);
        toolBar.add(autoScrollCheckBox);
        
        return toolBar;
    }
    
    /**
     * 接收日志消息
     */
    @Override
    public void onLogReceived(String logMessage) {
        SwingUtilities.invokeLater(() -> {
            // 添加日志
            logTextArea.append(logMessage + "\n");
            
            // 限制日志行数
            limitLogLines();
            
            // 自动滚动到底部
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }
    
    /**
     * 限制日志行数，避免内存溢出
     */
    private void limitLogLines() {
        String text = logTextArea.getText();
        String[] lines = text.split("\n");
        
        if (lines.length > MAX_LOG_LINES) {
            // 保留最新的日志
            int removeCount = lines.length - MAX_LOG_LINES;
            StringBuilder sb = new StringBuilder();
            for (int i = removeCount; i < lines.length; i++) {
                sb.append(lines[i]).append("\n");
            }
            logTextArea.setText(sb.toString());
        }
    }
    
    /**
     * 清空日志
     */
    private void clearLogs() {
        logTextArea.setText("");
    }
    
    /**
     * 销毁面板时取消注册监听器
     */
    public void dispose() {
        LogCollector.getInstance().removeListener(this);
    }
}
