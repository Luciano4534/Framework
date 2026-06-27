package util;

import annotation.Controller;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utility class to scan a package and discover controller classes.
 */
public class ControllerScanner {

    /**
     * Finds all classes annotated with {@link Controller} in the specified package and its subpackages.
     *
     * @param packageName the base package name (e.g., "controller")
     * @param classLoader the ClassLoader to use for loading classes
     * @return a list of annotated controller classes
     */
    public static List<Class<?>> findControllers(String packageName, ClassLoader classLoader) {
        List<Class<?>> controllers = new ArrayList<>();
        String path = packageName.replace('.', '/');
        
        try {
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    try {
                        // URI is safer than getFile() as it decodes spaces and special characters correctly
                        URI uri = resource.toURI();
                        File directory = new File(uri);
                        if (directory.exists() && directory.isDirectory()) {
                            scanDirectory(directory, packageName, controllers, classLoader);
                        }
                    } catch (URISyntaxException e) {
                        System.err.println("Invalid URI syntax for resource URL: " + resource + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error while resolving package resources: " + e.getMessage());
        }
        
        return controllers;
    }

    /**
     * Overloaded method that uses the current thread's context ClassLoader or the system ClassLoader.
     *
     * @param packageName the base package name
     * @return a list of annotated controller classes
     */
    public static List<Class<?>> findControllers(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ControllerScanner.class.getClassLoader();
        }
        return findControllers(packageName, classLoader);
    }

    /**
     * Recursively scans a directory for Java class files and adds annotated controller classes.
     *
     * @param directory the directory to scan
     * @param packageName the package prefix corresponding to this directory
     * @param controllers the list accumulating the controllers
     * @param classLoader the ClassLoader used to load class instances
     */
    private static void scanDirectory(File directory, String packageName, List<Class<?>> controllers, ClassLoader classLoader) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively scan subdirectories, appending directory name to the package prefix
                String subPackageName = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                scanDirectory(file, subPackageName, controllers, classLoader);
            } else if (file.getName().endsWith(".class")) {
                // Remove the ".class" extension to get the simple class name
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllers.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    
                }
            }
        }
    }
}
