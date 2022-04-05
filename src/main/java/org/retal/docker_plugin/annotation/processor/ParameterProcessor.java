package org.retal.docker_plugin.annotation.processor;

import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("org.apache.maven.plugins.annotations.Mojo")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ParameterProcessor extends AbstractProcessor {

    public static final String FILE_NAME_PATTERN = "%s-default-values.xml";

    //TODO refactoring??
    //TODO separate project for testing plugins???

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for(TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                Map<String, String> defaultValues = element.getEnclosedElements().stream()
                        .filter(e -> e.getAnnotation(Parameter.class) != null)
                        .filter(e -> !e.getAnnotation(Parameter.class).defaultValue().isEmpty())
                        .collect(Collectors.toMap(e -> e.getSimpleName().toString(),
                                e -> e.getAnnotation(Parameter.class).defaultValue()));
                try {
                    String fileName = String.format(FILE_NAME_PATTERN, element.getSimpleName().toString());
                    FileObject fileObject = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", fileName);
                    PrintWriter writer = new PrintWriter(fileObject.openWriter());
                    writer.println("<defaultValues>");
                    for(Map.Entry<String, String> entry : defaultValues.entrySet()) {
                        writer.println(String.format("\t<%s>%s</%s>", entry.getKey(), entry.getValue(), entry.getKey()));
                    }
                    writer.println("</defaultValues>");
                    writer.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return false;
    }
}
