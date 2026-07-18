package com.projectlibre1.scripting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class GroovyClassCompilerTest {
    @Test
    void deterministicNamesAndSourceCacheReuseCompiledClass() throws Exception {
        String definition = "value * 2";
        String name = GroovyClassCompiler.scriptClassName("Generated", definition);
        assertEquals(name, GroovyClassCompiler.scriptClassName("Generated", definition));

        String source = "public class " + name + " implements Runnable { void run() {} }";
        Runnable first = GroovyClassCompiler.compileAndInstantiate(source, Runnable.class);
        Runnable second = GroovyClassCompiler.compileAndInstantiate(source, Runnable.class);

        assertNotSame(first, second);
        assertSame(first.getClass(), second.getClass());
    }

    @Test
    void concurrentRequestsShareOneTypedCompilation() throws Exception {
        String name = GroovyClassCompiler.scriptClassName("ConcurrentGenerated", "run");
        String source = "public class " + name + " implements Runnable { void run() {} }";
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<Class<? extends Runnable>>> calls = new ArrayList<>();
            for (int index = 0; index < 32; index++)
                calls.add(() -> GroovyClassCompiler.compile(source, Runnable.class));
            Class<?> expected = executor.invokeAll(calls).getFirst().get();
            for (var result : executor.invokeAll(calls))
                assertSame(expected, result.get());
        }
    }

    @Test
    void rejectsGeneratedClassWithUnexpectedContract() {
        String name = GroovyClassCompiler.scriptClassName("WrongType", "plain");
        String source = "public class " + name + " {}";
        assertThrows(ClassCastException.class,
            () -> GroovyClassCompiler.compile(source, Runnable.class));
    }

    @Test
    void cacheCanReleaseGeneratedClassLoaders() throws Exception {
        GroovyClassCompiler.clearCache();
        String name = GroovyClassCompiler.scriptClassName("DisposableGenerated", "run");
        String source = "public class " + name + " implements Runnable { void run() {} }";
        GroovyClassCompiler.compile(source, Runnable.class);
        assertEquals(1, GroovyClassCompiler.cachedClassCount());

        GroovyClassCompiler.clearCache();

        assertEquals(0, GroovyClassCompiler.cachedClassCount());
    }
}
