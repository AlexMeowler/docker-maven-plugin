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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                    Field target = parameters.stream()
                            .filter(f -> f.getName().equals(name))
                            .findFirst()
                            .orElseThrow(() -> new FieldNotDeclaredException(mojo.getClass(), name));

                    injectValue(mojo, target, paramToInject);
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
                filterChildren(node);
            }
        }

        return elements;
    }

    private void filterChildren(Node node) {
        List<Node> childrenToRemove = new ArrayList<>();
        NodeList children = node.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if(child.getNodeType() == Node.ELEMENT_NODE || !child.getTextContent().matches("^\\s*$")) {
                filterChildren(child);
            } else {
                childrenToRemove.add(child);
            }
        }
        childrenToRemove.forEach(node::removeChild);
    }

    private void injectValue(Object targetOwner, Field target, Node node) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        target.setAccessible(true);
        String type = target.getType().getSimpleName().toUpperCase();
        boolean isArray = type.endsWith("[]");
        ClassEnum classEnum = isArray ? ClassEnum.ARRAY : ClassEnum.fromValue(type);
        Object injection = parseValue(classEnum, target, nodeToListWithChildren(node));
        target.set(targetOwner, injection);
    }

    private Node[] nodeToListWithChildren(Node node) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(node);
        NodeList nodeList = node.getChildNodes();
        for(int i = 0; i< nodeList.getLength(); i++) {
            nodes.add(nodeList.item(i));
        }
        return nodes.toArray(new Node[0]);
    }

    private Object parseValue(ClassEnum classEnum, Field target, Node[] nodes) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Object value;
        switch (classEnum) {
            case BYTE:
            case LONG:
            case FLOAT:
            case SHORT:
            case DOUBLE:
            case BOOLEAN:
            case INTEGER:
            case CHARACTER:
                value = target.getType().getMethod("valueOf", String.class).invoke(null, nodes[0].getTextContent());
                break;
            case STRING:
                value = nodes[0].getTextContent();
                break;
            case ARRAY:
                List<Node> nodeList = Arrays.stream(nodes).collect(Collectors.toList());
                nodeList.remove(0);
                Class type = target.getType().getComponentType();
                Object array = Array.newInstance(type, nodeList.size());
                for(int i = 0; i < nodeList.size(); i++) {
                    boolean isArray = type.getSimpleName().endsWith("[]");
                    ClassEnum clazz = isArray ? ClassEnum.ARRAY : ClassEnum.fromValue(type.getSimpleName());
                    Array.set(array, i, parseValue(clazz, target, nodeToListWithChildren(nodeList.get(i))));
                }
                value = array;
                break;
            case CUSTOM_CLASS:
                nodeList = Arrays.stream(nodes).collect(Collectors.toList());
                nodeList.remove(0);
                Object pojo = target.getType().getConstructor().newInstance();
                for(int i = 0; i < nodeList.size(); i++) {
                    Node node = nodeList.get(i);
                    Field field = Arrays.stream(pojo.getClass().getDeclaredFields())
                            .filter(f -> f.getName().equals(node.getNodeName()))
                            .findFirst().orElseThrow(() -> new FieldNotDeclaredException(pojo.getClass(), node.getNodeName()));
                    injectValue(pojo, field, node);
                }
                value = pojo;
                break;
            default:
                throw new UnsupportedOperationException("Something went wrong");
        }
        return value;
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
        STRING,
        ARRAY,
        CUSTOM_CLASS;

        public static ClassEnum fromValue(String value) {
            try {
                return ClassEnum.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return CUSTOM_CLASS;
            }
        }
    }
}
