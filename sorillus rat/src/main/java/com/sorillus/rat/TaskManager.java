package com.sorillus.rat;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class TaskManager {
    private final JFrame frame;
    private final JTable processTable;
    private final DefaultTableModel tableModel;
    private final Server.ClientHandler clientHandler;
    private boolean waitingForResponse = false;

    public TaskManager(Server.ClientHandler clientHandler) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        this.clientHandler = clientHandler;
        clientHandler.setTaskManager(this);

        frame = new JFrame("Task Manager - " + clientHandler.getClientId() + " (" + clientHandler.getIpAddress() + ")");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshProcessList());
        topPanel.add(refreshButton);
        frame.add(topPanel, BorderLayout.NORTH);

        String[] columnNames = {"Process Name", "PID"};
        tableModel = new DefaultTableModel(columnNames, 0);
        processTable = new JTable(tableModel);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processTable.setRowHeight(25);
        processTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem killItem = new JMenuItem("Kill Process");
        killItem.addActionListener(e -> {
            int selectedRow = processTable.getSelectedRow();
            if (selectedRow != -1) {
                String pid = (String) tableModel.getValueAt(selectedRow, 1);
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to terminate process with PID " + pid + "?",
                        "Confirm Kill",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    clientHandler.sendCommand("KILL_PROCESS:" + pid);
                    waitingForResponse = true;
                }
            }
        });
        popupMenu.add(killItem);
        processTable.setComponentPopupMenu(popupMenu);

        frame.add(new JScrollPane(processTable), BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        refreshProcessList();
    }

    private void refreshProcessList() {
        clientHandler.sendCommand("LIST_PROCESSES");
        waitingForResponse = true;
    }

    public boolean isWaitingForResponse() {
        return waitingForResponse;
    }

    public void handleProcessList(String response) {
        waitingForResponse = false;
        if (response.startsWith("PROCESS_ERROR:")) {
            JOptionPane.showMessageDialog(frame, response.substring(13), "Error", JOptionPane.ERROR_MESSAGE);
        } else if (response.startsWith("PROCESS_LIST:")) {
            tableModel.setRowCount(0);
            String processList = response.substring(12);
            if (!processList.isEmpty()) {
                String[] processes = processList.split(";");
                for (String process : processes) {
                    if (!process.isEmpty()) {
                        String[] parts = process.split("\\|");
                        if (parts.length >= 2) {
                            tableModel.addRow(new Object[]{parts[0], parts[1]});
                        }
                    }
                }
            }
        }
    }

    public void handleKillResponse(String response) {
        waitingForResponse = false;
        if (response.startsWith("KILL_SUCCESS:")) {
            JOptionPane.showMessageDialog(frame, response.substring(13), "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshProcessList();
        } else if (response.startsWith("KILL_ERROR:")) {
            JOptionPane.showMessageDialog(frame, response.substring(11), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}