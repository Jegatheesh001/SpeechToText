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
    private Robot robot;

    public SpeechToTextApp() {
        super("Java Speech-to-Text");
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

        // --- Action Listener for the Button ---
        toggleButton.addActionListener(e -> toggleListening());

        // --- Window Setup ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null); // Center the window
        setVisible(true);
        
        // Ensure the helper process is terminated when the app closes
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
            speechHelperProcess = pb.start();
            isListening = true;

            // Update UI on the Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater(() -> {
                toggleButton.setText("Stop Listening");
                statusLabel.setText("Status: Listening...");
                recognizedTextArea.setText("Waiting for speech...\n");
            });

            // Create a new thread to read the helper's output
            // This prevents the UI from freezing
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
                    // This often happens when the process is destroyed, which is normal.
                    // Only show an error if we are still supposed to be listening.
                    if (isListening) {
                       ioException.printStackTrace();
                    }
                } finally {
                    // Ensure the state is updated if the process terminates unexpectedly
                    stopListening();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to start helper process.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopListening() {
        if (speechHelperProcess != null) {
            isListening = false;
            speechHelperProcess.destroy();
            speechHelperProcess = null;
        }
        // Update UI on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            toggleButton.setText("Start Listening");
            statusLabel.setText("Status: Stopped");
        });
    }

    /**
     * Uses the AWT Robot to type the given string at the current cursor position.
     * @param text The text to be typed.
     */
    private void typeTextAtCursor(String text) {
        // Give a tiny delay for focus to shift if needed
        robot.delay(50); 

        for (char c : text.toCharArray()) {
            // The Robot class doesn't handle upper/lower case directly.
            // KeyEvent.getExtendedKeyCodeForChar is the modern way to get the correct key code.
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            
            // Check if the character has a direct key code
            if (keyCode != KeyEvent.VK_UNDEFINED) {
                // For special characters that require SHIFT, the key code handles it.
                // However, for uppercase letters, we must explicitly press SHIFT.
                boolean needsShift = Character.isUpperCase(c);
                
                if (needsShift) {
                    robot.keyPress(KeyEvent.VK_SHIFT);
                }
                
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
                
                if (needsShift) {
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                }
            }
             // Add small delay between keystrokes for reliability
            robot.delay(10);
        }
    }

    public static void main(String[] args) {
        // Ensure the UI is created on the Event Dispatch Thread
        SwingUtilities.invokeLater(SpeechToTextApp::new);
    }
}