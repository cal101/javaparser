package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.observer.AstObserver;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.observer.PropagatingAstObserver;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class LexicalPreservingPrinter {

    private interface Inserter {
        void insert(Node parent, Node child);
    }

    private Map<Node, NodeText> textForNodes = new IdentityHashMap<>();

    public String print(Node node) {
        StringWriter writer = new StringWriter();
        try {
            print(node, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException on a StringWriter", e);
        }
        return writer.toString();
    }

    public void print(Node node, Writer writer) throws IOException {
        if (textForNodes.containsKey(node)) {
            final NodeText text = textForNodes.get(node);
            writer.append(text.expand());
        } else {
            writer.append(node.toString());
        }
    }

    public void registerText(Node node, String documentCode) {
        String text = getRangeFromDocument(node.getRange().get(), documentCode);
        NodeText nodeText = putPlaceholders(documentCode, node.getRange().get(), text, new ArrayList<>(node.getChildNodes()));
        textForNodes.put(node, nodeText);
    }

    public NodeText getTextForNode(Node node) {
        return textForNodes.get(node);
    }

    private NodeText putPlaceholders(String documentCode, Range range, String text, List<Node> children) {

        children.sort((o1, o2) -> o1.getRange().get().begin.compareTo(o2.getRange().get().begin));

        NodeText nodeText = new NodeText(this);

        int start = findIndex(documentCode, range.begin);
        int caret = start;
        for (Node child : children) {
            int childStartIndex = findIndex(documentCode, child.getBegin().get());
            int fromStart = childStartIndex - caret;
            if (fromStart > 0) {
                nodeText.addElement(new StringNodeTextElement(text.substring(caret - start, childStartIndex - start)));
                caret += fromStart;
            }
            nodeText.addElement(new ChildNodeTextElement(this, child));
            int lengthOfOriginalCode = getRangeFromDocument(child.getRange().get(), documentCode).length();
            caret += lengthOfOriginalCode;
        }
        // last string
        int endOfNode = findIndex(documentCode, range.end) + 1;
        if (caret < endOfNode) {
            nodeText.addElement(new StringNodeTextElement(text.substring(caret - start)));
        }

        return nodeText;
    }

    private String getRangeFromDocument(Range range, String documentCode) {
        return documentCode.substring(findIndex(documentCode, range.begin), findIndex(documentCode, range.end) + 1);
    }

    private int findIndex(String documentCode, Position position) {
        int indexOfLineStart = 0;
        for (int i = 1; i < position.line; i++) {
            int indexR = documentCode.indexOf('\r', indexOfLineStart);
            int indexN = documentCode.indexOf('\n', indexOfLineStart);
            int nextIndex = -1;
            if (indexN == -1 && indexR != -1) {
                nextIndex = indexR;
            } else if (indexN != -1 && indexR == -1) {
                nextIndex = indexN;
            } else {
                nextIndex = Math.min(indexR, indexN);
            }
            if (nextIndex == -1) {
                throw new IllegalArgumentException("Searching for line "+position.line);
            }
            if ((documentCode.charAt(nextIndex) == '\r' && documentCode.charAt(nextIndex + 1) == '\n') ||
                    (documentCode.charAt(nextIndex) == '\n' && documentCode.charAt(nextIndex + 1) == '\r')) {
                nextIndex++;
            }
            indexOfLineStart = nextIndex + 1;
        }
        return findIndexOfColumn(documentCode, indexOfLineStart, position.column);
    }

    private int findIndexOfColumn(String documentCode, int indexOfLineStart, int column) {
        // consider tabs
        return indexOfLineStart + column - 1;
    }

    public void updateTextBecauseOfRemovedChild(NodeList nodeList, int index, Optional<Node> parentNode, Node child) {
        if (!parentNode.isPresent()) {
            return;
        }
        Node parent = parentNode.get();
        String key = parent.getClass().getSimpleName() + ":" + findNodeListName(nodeList);

        switch (key) {
            case "MethodDeclaration:Parameters":
                if (index == 0 && nodeList.size() > 1) {
                    // we should remove all the text between the child and the comma
                    textForNodes.get(parent).removeTextBetween(child, ",", true);
                }
                if (index != 0) {
                    // we should remove all the text between the child and the comma
                    textForNodes.get(parent).removeTextBetween(",", child);
                }
            default:
                textForNodes.get(parent).removeElementsForChild(child);
        }
    }

    public void updateTextBecauseOfAddedChild(NodeList nodeList, int index, Optional<Node> parentNode, Node child) {
        if (!parentNode.isPresent()) {
            return;
        }
        Node parent = parentNode.get();
        String nodeListName = findNodeListName(nodeList);

        if (index == 0) {
            Inserter inserter = getPositionFinder(parent.getClass(), nodeListName);
            inserter.insert(parent, child);
        } else {
            Inserter inserter = insertAfterChild(nodeList.get(index - 1), ", ");
            inserter.insert(parent, child);
        }
    }

    private String findNodeListName(NodeList nodeList) {
        Node parent = nodeList.getParentNodeForChildren();
        for (Method m : parent.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().getCanonicalName().equals(NodeList.class.getCanonicalName())) {
                try {
                    NodeList result = (NodeList)m.invoke(parent);
                    if (result == nodeList) {
                        String name = m.getName();
                        if (name.startsWith("get")) {
                            name = name.substring("get".length());
                        }
                        return name;
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalArgumentException();
    }

    private Inserter getPositionFinder(Class<?> parentClass, String nodeListName) {
        String key = String.format("%s:%s", parentClass.getSimpleName(), nodeListName);
        switch (key) {
            case "ClassOrInterfaceDeclaration:Members":
                return insertAfter("{", InsertionMode.ON_ITS_OWN_LINE);
            case "FieldDeclaration:Variables":
                try {
                    return insertAfterChild(FieldDeclaration.class.getMethod("getElementType"), " ");
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            case "MethodDeclaration:Parameters":
                return insertAfter("(", InsertionMode.PLAIN);
            case "BlockStmt:Stmts":
                return insertAfter("{", InsertionMode.ON_ITS_OWN_LINE);
        }

        throw new UnsupportedOperationException(key);
    }

    private void printModifiers(NodeText nodeText, final EnumSet<Modifier> modifiers) {
        if (modifiers.size() > 0) {
            nodeText.addElement(new StringNodeTextElement(modifiers.stream().map(Modifier::name).collect(Collectors.joining(" ")) + " "));
        }
    }

    private NodeText prettyPrintingTextNode(Node node) {
        NodeText nodeText = new NodeText(this);
        if (node instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration)node;
            nodeText.addList(fieldDeclaration.getAnnotations(), "\n", true);
            printModifiers(nodeText, fieldDeclaration.getModifiers());
            nodeText.addChild(fieldDeclaration.getElementType());
            //nodeText.addList(fieldDeclaration.getAr(), "", true);
            //nodeText.addString(" ");
            nodeText.addList(fieldDeclaration.getVariables(), ", ", false);
            nodeText.addString(";\n");
            return nodeText;
        }
        throw new UnsupportedOperationException(node.getClass().getCanonicalName());
    }

    private NodeText getOrCreateNodeText(Node node) {
        if (!textForNodes.containsKey(node)) {
            textForNodes.put(node, prettyPrintingTextNode(node));
        }
        return textForNodes.get(node);
    }

    private Inserter insertAfterChild(Node childToFollow, String separatorBefore) {
        return (parent, child) -> {
            NodeText nodeText = getOrCreateNodeText(parent);
            if (childToFollow == null) {
                nodeText.addElement(0, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                return;
            }
            for (int i=0; i< nodeText.numberOfElements();i++) {
                NodeTextElement element = nodeText.getTextElement(i);
                if (element instanceof ChildNodeTextElement) {
                    ChildNodeTextElement childElement = (ChildNodeTextElement)element;
                    if (childElement.getChild() == childToFollow) {
                        nodeText.addString(i+1, separatorBefore);
                        nodeText.addElement(i+2, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                        return;
                    }
                }
            }
            throw new IllegalArgumentException();
        };
    }

    private Inserter insertAfterChild(Method method, String separatorBefore) {
        return (parent, child) -> {
            try {
                NodeText nodeText = getOrCreateNodeText(parent);
                Node childToFollow = (Node) method.invoke(parent);
                if (childToFollow == null) {
                    nodeText.addElement(0, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                    return;
                }
                for (int i=0; i< nodeText.numberOfElements();i++) {
                    NodeTextElement element = nodeText.getTextElement(i);
                    if (element instanceof ChildNodeTextElement) {
                        ChildNodeTextElement childElement = (ChildNodeTextElement)element;
                        if (childElement.getChild() == childToFollow) {
                            nodeText.addString(i+1, separatorBefore);
                            nodeText.addElement(i+2, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                            return;
                        }
                    }
                }
                throw new IllegalArgumentException();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private enum InsertionMode {
        PLAIN,
        ON_ITS_OWN_LINE
    }

    private Inserter insertAfter(final String subString, InsertionMode insertionMode) {
        return (parent, child) -> {
            NodeText nodeText = textForNodes.get(parent);
            for (int i=0; i< nodeText.numberOfElements();i++) {
                NodeTextElement element = nodeText.getTextElement(i);
                if (element instanceof StringNodeTextElement) {
                    StringNodeTextElement stringElement = (StringNodeTextElement)element;
                    int index = stringElement.getText().indexOf(subString);
                    if (index != -1) {
                        int end = index + subString.length();
                        String textBefore = stringElement.getText().substring(0, end);
                        if (insertionMode == InsertionMode.ON_ITS_OWN_LINE) {
                            // TODO calculate correct indentation
                            textBefore += "\n    ";
                        }
                        String textAfter = stringElement.getText().substring(end);
                        if (textAfter.isEmpty()) {
                            nodeText.addElement(i+1, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                        } else {
                            nodeText.replaceElement(i, new StringNodeTextElement(textBefore));
                            nodeText.addElement(i+1, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                            nodeText.addElement(i+2, new StringNodeTextElement(textAfter));
                        }
                        return;
                    }
                }
            }
            throw new IllegalArgumentException();
        };
    }

    public static LexicalPreservingPrinter setup(CompilationUnit cu, String code) {
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        AstObserver observer = createObserver(lpp);
        cu.registerForSubtree(observer);
        cu.onSubStree(node -> lpp.registerText(node, code));
        return lpp;
    }

    private static AstObserver createObserver(LexicalPreservingPrinter lpp) {
        return new PropagatingAstObserver() {
            @Override
            public void concretePropertyChange(Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {
                if (oldValue != null && oldValue.equals(newValue)) {
                    return;
                }
                if (oldValue instanceof Node && newValue instanceof Node) {
                    lpp.getTextForNode(observedNode).replaceChild((Node)oldValue, (Node)newValue);
                    return;
                }
                if (oldValue == null && newValue instanceof Node) {
                    if (property == ObservableProperty.INITIALIZER) {
                        lpp.getOrCreateNodeText(observedNode).addString(" = ");
                        lpp.getOrCreateNodeText(observedNode).addChild((Node)newValue);
                        return;
                    }
                    throw new UnsupportedOperationException("Set property " + property);
                }
                if (oldValue instanceof Node && newValue == null) {
                    if (property == ObservableProperty.INITIALIZER) {
                        lpp.getOrCreateNodeText(observedNode).removeTextBetween("=", (Node)oldValue);
                        lpp.getOrCreateNodeText(observedNode).removeElementsForChild((Node)oldValue);
                        return;
                    }
                    throw new UnsupportedOperationException("Unset property " + property);
                }
                if ((oldValue instanceof EnumSet) && ObservableProperty.MODIFIERS == property){
                    EnumSet<Modifier> oldEnumSet = (EnumSet<Modifier>)oldValue;
                    EnumSet<Modifier> newEnumSet = (EnumSet<Modifier>)newValue;
                    for (Modifier removedModifier : oldEnumSet.stream().filter(e -> !newEnumSet.contains(e)).collect(Collectors.toList())) {
                        lpp.getOrCreateNodeText(observedNode).removeString(removedModifier.name().toLowerCase());
                    }
                    for (Modifier addedModifier : newEnumSet.stream().filter(e -> !oldEnumSet.contains(e)).collect(Collectors.toList())) {
                        lpp.getOrCreateNodeText(observedNode).addAtBeginningString(addedModifier.name().toLowerCase() + " ");
                    }
                    return;
                }
                if (property == ObservableProperty.RANGE) {
                    return;
                }
                throw new UnsupportedOperationException(String.format("Property %s. OLD %s (%s) NEW %s (%s)", property, oldValue,
                        oldValue == null ? "": oldValue.getClass(), newValue, newValue == null ? "": newValue.getClass()));
            }

            @Override
            public void concreteListChange(NodeList observedNode, ListChangeType type, int index, Node nodeAddedOrRemoved) {
                if (type == type.REMOVAL) {
                    lpp.updateTextBecauseOfRemovedChild(observedNode, index, observedNode.getParentNode(), nodeAddedOrRemoved);
                } else if (type == type.ADDITION) {
                    lpp.updateTextBecauseOfAddedChild(observedNode, index, observedNode.getParentNode(), nodeAddedOrRemoved);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        };
    }
}
