package com.sorillus.rat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Builder {
    public static void generate(String fileName) throws IOException {
        String projectRoot = getProjectRoot();
        String targetClassesPath = projectRoot + "/target/classes";
        String outputJarPath = projectRoot + "/" + fileName;

        // Создаём манифест
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "com.sorillus.rat.Client");

        // Создаём JAR-файл
        try (FileOutputStream fos = new FileOutputStream(outputJarPath);
             JarOutputStream jarOutputStream = new JarOutputStream(fos, manifest)) {

            // Добавляем классы из target/classes
            addFilesToJar(new File(targetClassesPath), targetClassesPath, jarOutputStream);

            // Добавляем зависимости из target/dependency (если они есть)
            String dependencyPath = projectRoot + "/target/dependency";
            File dependencyDir = new File(dependencyPath);
            if (dependencyDir.exists()) {
                addFilesToJar(dependencyDir, dependencyPath, jarOutputStream);
            }
        }

        // Копируем зависимости (если нужно)
        copyDependencies(projectRoot);
    }

    private static void addFilesToJar(File source, String basePath, JarOutputStream jarOutputStream) throws IOException {
        if (source.isDirectory()) {
            for (File nestedFile : source.listFiles()) {
                addFilesToJar(nestedFile, basePath, jarOutputStream);
            }
        } else {
            String entryName = source.getPath().substring(basePath.length() + 1).replace("\\", "/");
            if (!entryName.endsWith(".class") && !entryName.startsWith("META-INF")) {
                return; // Пропускаем не-классовые файлы, кроме зависимостей
            }
            jarOutputStream.putNextEntry(new JarEntry(entryName));
            try (FileInputStream fis = new FileInputStream(source)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    jarOutputStream.write(buffer, 0, bytesRead);
                }
            }
            jarOutputStream.closeEntry();
        }
    }

    private static void copyDependencies(String projectRoot) throws IOException {
        String dependencyPath = projectRoot + "/target/dependency";
        File dependencyDir = new File(dependencyPath);
        if (!dependencyDir.exists()) {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "mvn.cmd", "dependency:copy-dependencies");
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

            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getProjectRoot() {
        String currentDir = System.getProperty("user.dir");
        if (currentDir.endsWith("target")) {
            return new File(currentDir).getParent();
        }
        return currentDir;
    }
}