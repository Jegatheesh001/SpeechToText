import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

public class SpeechToTextApp extends JFrame {

    private final JButton toggleButton;
    private final JTextArea recognizedTextArea;
    private final JLabel statusLabel;
    private Process speechHelperProcess;
    private volatile boolean isListening = false;
    private final Object processLock = new Object();
    private Robot robot;

    public SpeechToTextApp() {
        super("Java Speech-to-Text");

        // Initialize the AWT Robot
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to initialize Robot class. Cannot type text.",
                    "Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // --- UI Setup ---
        toggleButton = new JButton("Start Listening");
        recognizedTextArea = new JTextArea(10, 40);
        recognizedTextArea.setEditable(false);
        recognizedTextArea.setLineWrap(true);
        recognizedTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(recognizedTextArea);
        statusLabel = new JLabel("Status: Stopped");

        // --- Layout ---
        setLayout(new BorderLayout(10, 10));
        JPanel topPanel = new JPanel();
        topPanel.add(toggleButton);
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        toggleButton.addActionListener(e -> toggleListening());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null); // Center the window
        setVisible(true);

        // Ensure the helper process is terminated on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopListening));
    }

    private void toggleListening() {
        if (!isListening) {
            startListening();
        } else {
            stopListening();
        }
    }

    private void startListening() {
        File helperExe = new File("SpeechToTextHelper.exe");
        if (!helperExe.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Error: SpeechToTextHelper.exe not found!\n" +
                            "Make sure it's compiled and in the same directory as this application.",
                    "File Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(helperExe.getAbsolutePath());
            pb.redirectErrorStream(true); // Merge stderr with stdout for easier handling
            speechHelperProcess = pb.start();
            isListening = true;

            SwingUtilities.invokeLater(() -> {
                toggleButton.setText("Stop Listening");
                statusLabel.setText("Status: Listening...");
                recognizedTextArea.setText("Waiting for speech...\n");
            });

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(speechHelperProcess.getInputStream()))) {
                    String line;
                    while (isListening && (line = reader.readLine()) != null) {
                        final String recognizedText = line;
                        SwingUtilities.invokeLater(() -> {
                            recognizedTextArea.append("Recognized: " + recognizedText + "\n");
                            typeTextAtCursor(recognizedText + " ");
                        });
                    }
                } catch (IOException ioException) {
                    if (isListening) {
                        ioException.printStackTrace();
                    }
                } finally {
                    // Safely update state if process ends unexpectedly
                    stopListening();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to start helper process.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopListening() {
        synchronized (processLock) {
            if (speechHelperProcess != null) {
                isListening = false;
                speechHelperProcess.destroy();
                speechHelperProcess = null;
            }
        }
        SwingUtilities.invokeLater(() -> {
            toggleButton.setText("Start Listening");
            statusLabel.setText("Status: Stopped");
        });
    }

    private void typeTextAtCursor(String text) {
        robot.delay(50);
        for (char c : text.toCharArray()) {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (keyCode != KeyEvent.VK_UNDEFINED) {
                boolean needsShift = Character.isUpperCase(c) || isSpecialShiftChar(c);
                if (needsShift) {
                    robot.keyPress(KeyEvent.VK_SHIFT);
                }
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
                if (needsShift) {
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                }
            }
            robot.delay(10);
        }
    }

    private boolean isSpecialShiftChar(char c) {
        // Common special characters requiring SHIFT (like !@#$%^&*())
        return "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpeechToTextApp::new);
    }
}
