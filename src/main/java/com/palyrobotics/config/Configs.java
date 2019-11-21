package com.palyrobotics.config;

import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.VisibilityChecker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatically maps Java types to JSON by field names.
 *
 * @author Quintin Dwight
 */
public class Configs {

    private static final String CONFIG_FOLDER_NAME = "config";
    private static final Path CONFIG_FOLDER = Paths.get(System.getProperty("user.home"), CONFIG_FOLDER_NAME);
    private static final ConcurrentHashMap<String, Class<? extends AbstractConfig>> sNameToClass = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<? extends AbstractConfig>, AbstractConfig> sConfigMap = new ConcurrentHashMap<>();
    private static ObjectMapper sMapper = new ObjectMapper();
    private static final Thread sModifiedListener = new Thread(Configs::runWatchService);

    static {
        // Allows us to serialize private fields
        sMapper.setVisibilityChecker(new VisibilityChecker.Std(Visibility.ANY, Visibility.ANY, Visibility.ANY, Visibility.ANY, Visibility.ANY));
    }

    static {
        sModifiedListener.start();
    }

    private Configs() {
        throw new UnsupportedOperationException("Static only class!");
    }

    /**
     * Retrieve the singleton for this given config class.
     *
     * @param configClass Class of the config.
     * @param <T>         Type of the config class. This is usually inferred from the class argument.
     * @return Singleton or null if not found / registered.
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractConfig> T get(Class<T> configClass) {
        T config = (T) sConfigMap.get(configClass);
        if (config == null) {
            config = read(configClass);
            sConfigMap.put(configClass, config);
            sNameToClass.put(configClass.getSimpleName(), configClass);
        }
        return config;
    }

    /**
     * This should be started in a new thread to watch changes for the folder containing the JSON configuration files.
     * It detects when files are modified and written in the filesystem and modifies the config instances.
     */
    private static void runWatchService() {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            CONFIG_FOLDER.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            while (true) {
                try {
                    if (!waitForChangesAndReload(watcher)) break;
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static boolean waitForChangesAndReload(WatchService watcher) throws InterruptedException {
        WatchKey key = watcher.take(); // Blocks until an event
        /* Since there are two times when the listener is notified when a file is saved,
         * once for the actual content and another time for the timestamp updated,
         * sleeping will capture both into the same poll event list.
         * From there, we can filter out what we have already seen to avoid updating more than once. */
        Thread.sleep(100L);
        // There can be multiple updates for one file, so keep track and only reload changes once per file
        List<String> alreadySeen = new ArrayList<>();
        for (WatchEvent<?> pollEvent : key.pollEvents()) {
            if (pollEvent.kind() == StandardWatchEventKinds.OVERFLOW) continue;
            @SuppressWarnings("unchecked")
            WatchEvent<Path> event = (WatchEvent<Path>) pollEvent;
            Path context = event.context();
            String configName = context.getFileName().toString().replace(".json", "");
            Class<? extends AbstractConfig> configClass = sNameToClass.get(configName);
            if (configClass != null) {
                if (alreadySeen.contains(configName)) continue;
                System.out.printf("Config named %s hot reloaded%n", configName);
                try {
                    AbstractConfig config = get(configClass);
                    sMapper.updatingReader(config).readValue(getFileForConfig(configClass).toFile());
                    config.onPostUpdate();
                } catch (IOException readException) {
                    handleParseError(readException, configClass).printStackTrace();
                    System.err.printf("Error updating config for %s. Aborting reload.%n", configName);
                }
                alreadySeen.add(configName);
            } else {
                System.err.printf("Unknown file %s%n", context);
            }
        }
        return key.reset();
    }

    /**
     * Read the given config from the filesystem. There must be a file and it must be valid mappable JSON,
     * desired behavior is to crash if else. In attempt to help the user when there is an invalid JSON file,
     * a default empty class of the same type is printed to console to show desired format (helpful for debugging).
     *
     * @param configClass Class of the config.
     * @param <T>         Type of the config. Must extend {@link AbstractConfig}.
     * @return Instance of given type.
     * @throws RuntimeException when the file cannot be found or it could not be parsed. This is considered a critical error.
     */
    private static <T extends AbstractConfig> T read(Class<T> configClass) {
        Path configFile = getFileForConfig(configClass);
        String configClassName = configClass.getSimpleName();
        if (!Files.exists(configFile)) {
            String errorMessage = String.format(
                    "A config file was not found for %s. Critical error, aborting.%n%n%s%n",
                    configClassName, getDefaultJson(configClass)
            );
            throw new RuntimeException(errorMessage);
        }
        try {
            T value = sMapper.readValue(configFile.toFile(), configClass);
            value.onPostUpdate();
            return value;
        } catch (IOException readException) {
            RuntimeException exception = handleParseError(readException, configClass);
            exception.printStackTrace();
            throw exception;
        }
    }

    private static String getDefaultJson(Class<? extends AbstractConfig> configClass) {
        try {
            return String.format("See here for a default JSON file:%n%s%n",
                    sMapper.defaultPrettyPrintingWriter().writeValueAsString(configClass.getConstructor().newInstance()));
        } catch (IOException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException exception) {
            System.err.println("Could not show default JSON representation. Something is wrong with the config class definition.");
            exception.printStackTrace();
            return "Invalid";
        }
    }

    private static RuntimeException handleParseError(IOException readException, Class<? extends AbstractConfig> configClass) {
        String errorMessage = String.format(
                "An error occurred trying to read config for class %s%n%nSee here for default JSON: %s%n",
                configClass.getSimpleName(), getDefaultJson(configClass)
        );
        return new RuntimeException(errorMessage, readException);
    }

    private static Path resolveConfigPath(String name) {
        return CONFIG_FOLDER.resolve(String.format("%s.json", name));
    }

    private static Path getFileForConfig(Class<? extends AbstractConfig> configClass) {
        return resolveConfigPath(configClass.getSimpleName());
    }
}
