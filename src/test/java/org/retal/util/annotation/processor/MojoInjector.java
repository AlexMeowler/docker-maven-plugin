package org.retal.util.annotation.processor;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.retal.docker_plugin.annotation.processor.ParameterProcessor;
import org.retal.docker_plugin.exception.FieldNotDeclaredException;
import org.retal.util.annotation.InjectMojo;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MojoInjector implements TestInstancePostProcessor {

    //TODO check for required params
    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        for(Field testField : testInstance.getClass().getDeclaredFields()) {
            InjectMojo injectMojo = testField.getAnnotation(InjectMojo.class);
            if(injectMojo != null) {
                List<Node> elements = parseXml(String.format(ParameterProcessor.FILE_NAME_PATTERN, testField.getType().getSimpleName()));
                elements.addAll(parseXml(injectMojo.value()));
                Object mojo = testField.getType().getConstructor().newInstance();

                testField.setAccessible(true);
                testField.set(testInstance, mojo);
                List<Field> parameters = Arrays.asList(mojo.getClass().getDeclaredFields());

                for(Node paramToInject : elements) {
                    String name = paramToInject.getNodeName();
                    String value = paramToInject.getTextContent();
                    Field target = parameters.stream()
                            .filter(f -> f.getName().equals(name))
                            .findFirst()
                            .orElseThrow(() -> new FieldNotDeclaredException(mojo.getClass(), name));

                    injectValue(mojo, target, value);
                }
            }
        }
    }

    private List<Node> parseXml(String path) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        Document document = builderFactory.newDocumentBuilder()
                        .parse(getClass().getClassLoader().getResourceAsStream(path));
        document.normalizeDocument();

        NodeList content = document.getDocumentElement().getChildNodes();
        List<Node> elements = new ArrayList<>();

        for(int i = 0; i< content.getLength(); i++) {
            Node node = content.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add(node);
            }
        }

        return elements;
    }

    private void injectValue(Object targetOwner, Field target, Object value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //TODO POJO injection
        target.setAccessible(true);
        Object injection;
        ClassEnum classEnum = ClassEnum.valueOf(target.getType().getSimpleName().toUpperCase());
        switch (classEnum) {
            case BYTE:
            case LONG:
            case FLOAT:
            case SHORT:
            case DOUBLE:
            case BOOLEAN:
            case INTEGER:
            case CHARACTER:
                injection = target.getType().getMethod("valueOf", String.class).invoke(null, value);
                break;
            case STRING:
                injection = value;
                break;
            default:
                throw new UnsupportedOperationException("Parsing POJOs and arrays not implemented");
        }
        target.set(targetOwner, injection);
    }

    private enum ClassEnum {
        BYTE,
        SHORT,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        BOOLEAN,
        CHARACTER,
        STRING
    }
}
