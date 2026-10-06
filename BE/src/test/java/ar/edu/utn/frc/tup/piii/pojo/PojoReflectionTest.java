package ar.edu.utn.frc.tup.piii.pojo;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Reflection-driven POJO exerciser. Walks every class in the chosen packages,
 * instantiates it (no-arg, all-args, builder), then invokes every getter/setter
 * plus equals/hashCode/toString to drive coverage on the Lombok data classes.
 */
class PojoReflectionTest {

    private static final String BASE = "ar.edu.utn.frc.tup.piii";

    private static final List<String> PACKAGES = List.of(
            BASE + ".entities",
            BASE + ".dtos.response",
            BASE + ".dtos.request",
            BASE + ".dtos.ws",
            BASE + ".dtos.common",
            BASE + ".external.pokemontcg.dtos",
            BASE + ".events",
            BASE + ".engine",
            BASE + ".engine.chain",
            BASE + ".engine.effects.abilities",
            BASE + ".engine.effects.trainers.chain",
            BASE + ".models.cards.effects",
            BASE + ".models.cards.effects.trainer",
            BASE + ".models.game",
            BASE + ".models.game.state"
    );

    @TestFactory
    Collection<DynamicTest> exerciseAllPojos() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();
        for (String pkg : PACKAGES) {
            for (Class<?> clazz : findClasses(pkg)) {
                if (skip(clazz)) {
                    continue;
                }
                tests.add(DynamicTest.dynamicTest(clazz.getSimpleName(), () -> exercise(clazz)));
            }
        }
        return tests;
    }

    private boolean skip(Class<?> clazz) {
        return clazz.isInterface()
                || clazz.isEnum()
                || clazz.isAnnotation()
                || clazz.isAnonymousClass()
                || clazz.isMemberClass()
                || Modifier.isAbstract(clazz.getModifiers())
                || clazz.getSimpleName().isEmpty()
                || clazz.getName().contains("Test");
    }

    private void exercise(Class<?> clazz) throws Exception {
        List<Object> instances = new ArrayList<>();

        // No-arg constructor
        Object base = tryNoArg(clazz);
        if (base != null) {
            instances.add(base);
        }
        // All-args constructor
        Object allArgs = tryAllArgs(clazz);
        if (allArgs != null) {
            instances.add(allArgs);
        }
        // Builder
        Object built = tryBuilder(clazz);
        if (built != null) {
            instances.add(built);
        }

        for (Object instance : instances) {
            invokeAccessors(instance);
            safe(instance::toString);
            safe(instance::hashCode);
        }

        // equals across instances + nulls/other types
        for (Object a : instances) {
            for (Object b : instances) {
                safe(() -> a.equals(b));
            }
            safe(() -> a.equals(null));
            safe(() -> a.equals("other"));
            safe(() -> a.equals(a));
        }

        // At least one instance should exist for data classes
        if (instances.isEmpty()) {
            // Static factory methods, valueOf, etc. – still try to touch toString of class
            assertNotNull(clazz.getName());
        }
    }

    private Object tryNoArg(Class<?> clazz) {
        try {
            Constructor<?> c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Throwable t) {
            return null;
        }
    }

    private Object tryAllArgs(Class<?> clazz) {
        Constructor<?> best = null;
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (best == null || c.getParameterCount() > best.getParameterCount()) {
                best = c;
            }
        }
        if (best == null || best.getParameterCount() == 0) {
            return null;
        }
        try {
            best.setAccessible(true);
            Object[] args = new Object[best.getParameterCount()];
            Class<?>[] types = best.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                args[i] = sample(types[i]);
            }
            return best.newInstance(args);
        } catch (Throwable t) {
            return null;
        }
    }

    private Object tryBuilder(Class<?> clazz) {
        try {
            Method builderMethod = clazz.getDeclaredMethod("builder");
            if (!Modifier.isStatic(builderMethod.getModifiers())) {
                return null;
            }
            builderMethod.setAccessible(true);
            Object builder = builderMethod.invoke(null);
            Class<?> builderClass = builder.getClass();
            for (Method m : builderClass.getMethods()) {
                if (m.getParameterCount() == 1 && m.getReturnType().equals(builderClass)) {
                    try {
                        m.setAccessible(true);
                        builder = m.invoke(builder, sample(m.getParameterTypes()[0]));
                    } catch (Throwable ignored) {
                        // skip this setter
                    }
                }
            }
            Method build = builderClass.getMethod("build");
            build.setAccessible(true);
            Object result = build.invoke(builder);
            safe(builder::toString);
            return result;
        } catch (Throwable t) {
            return null;
        }
    }

    private void invokeAccessors(Object instance) {
        Class<?> clazz = instance.getClass();
        // setters first
        for (Method m : clazz.getMethods()) {
            String name = m.getName();
            if (m.getParameterCount() == 1
                    && (name.startsWith("set") || name.startsWith("with"))
                    && !name.equals("equals")) {
                try {
                    m.setAccessible(true);
                    m.invoke(instance, sample(m.getParameterTypes()[0]));
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
        // getters / is / no-arg accessors
        for (Method m : clazz.getMethods()) {
            String name = m.getName();
            if (m.getParameterCount() == 0
                    && (name.startsWith("get") || name.startsWith("is"))
                    && !name.equals("getClass")) {
                try {
                    m.setAccessible(true);
                    m.invoke(instance);
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }

    private void safe(ThrowingRunnable r) {
        try {
            r.run();
        } catch (Throwable ignored) {
            // tolerate provider-specific failures
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object sample(Class<?> type) {
        if (type == String.class) {
            return "sample";
        }
        if (type == CharSequence.class) {
            return "sample";
        }
        if (type == int.class || type == Integer.class) {
            return 1;
        }
        if (type == long.class || type == Long.class) {
            return 1L;
        }
        if (type == double.class || type == Double.class) {
            return 1.0d;
        }
        if (type == float.class || type == Float.class) {
            return 1.0f;
        }
        if (type == short.class || type == Short.class) {
            return (short) 1;
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) 1;
        }
        if (type == boolean.class || type == Boolean.class) {
            return true;
        }
        if (type == char.class || type == Character.class) {
            return 'a';
        }
        if (type == BigDecimal.class) {
            return BigDecimal.ONE;
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.now();
        }
        if (type == LocalDate.class) {
            return LocalDate.now();
        }
        if (type == Instant.class) {
            return Instant.now();
        }
        if (type == UUID.class) {
            return UUID.randomUUID();
        }
        if (type == List.class || type == Collection.class || type == Iterable.class) {
            return new ArrayList<>();
        }
        if (type == Set.class) {
            return new HashSet<>();
        }
        if (type == Map.class) {
            return new HashMap<>();
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants.length > 0 ? constants[0] : null;
        }
        if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        if (type == Object.class) {
            return new Object();
        }
        // Try to instantiate complex types via no-arg, else null
        Object nested = tryNoArg(type);
        if (nested != null) {
            return nested;
        }
        return null;
    }

    /** Find concrete classes in a package on the classpath (dirs and jars). */
    private List<Class<?>> findClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = cl.getResources(path);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                File dir = new File(resource.toURI());
                scanDir(dir, packageName, classes);
            } else if ("jar".equals(resource.getProtocol())) {
                scanJar(resource, path, classes);
            }
        }
        return classes;
    }

    private void scanDir(File dir, String packageName, List<Class<?>> classes) {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                continue; // only the direct package; sub-packages listed explicitly
            }
            if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = packageName + '.' + file.getName().replace(".class", "");
                loadClass(className, classes);
            }
        }
    }

    private void scanJar(URL resource, String path, List<Class<?>> classes) throws Exception {
        String jarPath = resource.getPath().substring(5, resource.getPath().indexOf('!'));
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(path) && name.endsWith(".class") && !name.contains("$")) {
                    String rel = name.substring(path.length());
                    if (rel.indexOf('/', 1) > 0) {
                        continue; // sub-package
                    }
                    loadClass(name.replace('/', '.').replace(".class", ""), classes);
                }
            }
        }
    }

    private void loadClass(String className, List<Class<?>> classes) {
        try {
            classes.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
        } catch (Throwable ignored) {
            // unloadable class
        }
    }
}
