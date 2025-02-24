package org.qbicc.plugin.dot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.driver.GraphGenConfig;
import org.qbicc.driver.GraphGenFilter;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlock;
import org.qbicc.object.Function;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ProgramObject;
import org.qbicc.object.Section;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.BasicElement;
import org.qbicc.type.definition.element.ElementVisitor;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MemberElement;

/**
 *
 */
public class DotGenerator implements ElementVisitor<CompilationContext, Void>, Consumer<CompilationContext> {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.dot");

    private final Phase phase;
    private final GraphGenFilter filter;

    public DotGenerator(Phase p, GraphGenConfig graphGenConfig) {
        this.phase = p;
        if (graphGenConfig != null) {
            filter = graphGenConfig.getFilter();
        } else {
            filter = null;
        }
    }

    static final class Producer {
        private final Iterator<ProgramModule> pmIter;
        private Iterator<Section> sectionIter;
        private Iterator<ProgramObject> fnIter;

        Producer(CompilationContext ctxt) {
            pmIter = ctxt.getAllProgramModules().iterator();
        }

        Function next() {
            synchronized (this) {
                ProgramObject item;
                do {
                    while (fnIter == null || ! fnIter.hasNext()) {
                        while (sectionIter == null || ! sectionIter.hasNext()) {
                            if (! pmIter.hasNext()) {
                                return null;
                            }
                            sectionIter = pmIter.next().sections().iterator();
                        }
                        fnIter = sectionIter.next().contents().iterator();
                    }
                    item = fnIter.next();
                } while (! (item instanceof Function));
                return (Function) item;
            }
        }
    }

    public void accept(final CompilationContext compilationContext) {
        Producer producer = new Producer(compilationContext);
        compilationContext.runParallelTask(ctxt -> {
            Function fn;
            for (;;) {
                fn = producer.next();
                if (fn == null) {
                    return;
                }
                MemberElement element = fn.getOriginalElement();
                MethodBody body = fn.getBody();
                if (body != null && filter != null && filter.accept(element, phase)) {
                    process(element, body);
                }
            }
        });
    }

    public Void visitUnknown(final CompilationContext param, final BasicElement basicElement) {
        if (basicElement instanceof ExecutableElement) {
            ExecutableElement element = (ExecutableElement) basicElement;
            if (element.hasMethodBody()) {
                MethodBody methodBody = element.getMethodBody();
                if (filter != null && filter.accept(element, phase)) {
                    process(element, methodBody);
                }
            }
        }
        return null;
    }

    private void process(final MemberElement element, MethodBody methodBody) {
        if (element.hasAllModifiersOf(ClassFile.ACC_ABSTRACT)) return;
        DefinedTypeDefinition def = element.getEnclosingType();
        CompilationContext ctxt = def.getContext().getCompilationContext();
        BasicBlock entryBlock = methodBody.getEntryBlock();
        Path dir = ctxt.getOutputDirectory(element);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            failedToWrite(ctxt, dir, e);
            return;
        }
        Path path = dir.resolve(phase.toString() + ".dot");
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("digraph {");
            bw.newLine();
            bw.write("graph [ rankdir = BT ];");
            bw.newLine();
            bw.write("edge [ splines = true ];");
            bw.newLine();
            bw.newLine();
            DotNodeVisitor visitor = new DotNodeVisitor(entryBlock);
            visitor.process(bw);
            bw.write("}");
        } catch (IOException e) {
            failedToWrite(ctxt, path, e);
        } catch (UncheckedIOException e) {
            IOException cause = e.getCause();
            failedToWrite(ctxt, path, cause);
        } catch (TooBigException e) {
            log.debugf("Element \"%s\" is too big to graph", element);
        }
    }

    private static Diagnostic failedToWrite(final CompilationContext ctxt, final Path path, final IOException cause) {
        return ctxt.warning("Failed to write \"%s\": %s", path, cause);
    }
}
