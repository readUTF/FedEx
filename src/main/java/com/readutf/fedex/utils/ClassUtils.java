package com.readutf.fedex.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@UtilityClass
public final class ClassUtils {
    /**
     * Tries to create a new instance of the class based off of default constructor
     *
     * @param clazz The class
     * @param <T>   Generic
     * @return The object or null
     */
    @Nullable
    @SuppressWarnings("unchecked") @SneakyThrows
    public <T> T tryGetInstance(@NotNull Class<T> clazz, List<Object> contexts) {
        Constructor<?>[] constructors = clazz.getConstructors();
        Optional<Constructor<?>> emptyConstructor = Arrays.stream(constructors).filter(constructor -> constructor.getParameterTypes().length == 0).findFirst();
        if(emptyConstructor.isPresent()) {
            try {
                return (T) emptyConstructor.get().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Optional<Constructor<?>> matchingConstructor = Arrays.stream(constructors)
                    .filter(constructor -> Arrays.stream(constructor.getParameterTypes()).allMatch(aClass -> contexts.stream().anyMatch(o -> o.getClass() == aClass)))
                    .findFirst();
            if(matchingConstructor.isPresent()) {
                Constructor<?> constructor = matchingConstructor.get();
                Class<?>[] neededTypes = constructor.getParameterTypes();
                List<Object> matchingObjects = Arrays.stream(neededTypes).map(aClass -> contexts.stream().filter(o -> o.getClass() == aClass).findFirst().orElse(null)).collect(Collectors.toList());
                return (T) constructor.newInstance(matchingObjects.toArray());
            }
        }
        return null;
    }
    public <T> T tryGetInstance(@NotNull Class<T> clazz) {
        return tryGetInstance(clazz, Collections.emptyList());
    }

    /**
     * Gets all the classes in the provided package.
     *
     * @return The classes in the package packageName.
     */
    //TODO: Make this not require a Plugin object.
    @NotNull
    public static Collection<Class<?>> getClassesInPackage(@NotNull Class<?> clazz1) {
        String packageName = clazz1.getPackage().getName();
        Collection<Class<?>> classes = new ArrayList<>();

        CodeSource codeSource = clazz1.getProtectionDomain().getCodeSource();
        URL resource = codeSource.getLocation();
        String relPath = packageName.replace('.', '/');
        String resPath = resource.getPath().replace("%20", " ");
        String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
        JarFile jarFile;

        try {
            jarFile = new JarFile(jarPath);
        } catch (IOException e) {
            throw (new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e));
        }

        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            String className = null;

            if (entryName.endsWith(".class") && entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length())) {
                className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
            }

            if (className != null) {
                Class<?> clazz = null;

                try {
                    clazz = clazz1.getClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if (clazz != null) {
                    classes.add(clazz);
                }
            }
        }

        try {
            jarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }
}