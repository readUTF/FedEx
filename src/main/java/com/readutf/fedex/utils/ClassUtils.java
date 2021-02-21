package com.readutf.fedex.utils;

import com.readutf.fedex.FedEx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassUtils {

    public static List<Class<?>> getClasses(String jarLocation, String pathToPackage) throws Exception {
        List<Class<?>> classes = new ArrayList<>();

        URL jarUrl = new URL("file://" + jarLocation);
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, FedEx.class.getClassLoader());
        JarFile jar = new JarFile(jarLocation);

        for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
            JarEntry entry = entries.nextElement();
            String file = entry.getName();
            if (file.endsWith(".class") && file.startsWith(pathToPackage.replaceAll("\\.", "/"))) {
                String classname =
                        file.replace('/', '.').substring(0, file.length() - 6).split("\\$")[0];
                try {
                    Class<?> c = loader.loadClass(classname);
                    classes.add(c);
                } catch (Throwable e) {
                    Logger.getLogger(ClassUtils.class.getName()).log(Level.WARNING, "Failed to instantiate " + classname + " from " + file + ".");
                    e.printStackTrace();
                }
            }
        }
        jar.close();
        return classes;
    }


}
