/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2016 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser.ast.observer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Optional;

import static com.github.javaparser.ast.observer.ObservableProperty.Type.MULTIPLE_NODES;
import static com.github.javaparser.ast.observer.ObservableProperty.Type.MULTIPLE_PROPERTIES;
import static com.github.javaparser.ast.observer.ObservableProperty.Type.SINGLE_PROPERTY;

/**
 * Properties considered by the AstObserver
 */
public enum ObservableProperty {
    ANNOTATIONS(MULTIPLE_NODES),
    ANONYMOUS_CLASS_BODY,
    ARGUMENTS(MULTIPLE_NODES),
    IS_ASTERISK,
    BLOCK,
    BODY,
    CATCH_CLAUSES(MULTIPLE_NODES),
    CHECK,
    CLASS_BODY,
    CLASS_DECLARATION,
    CLASS_EXPR,
    COMMENT,
    COMMENTED_NODE,
    COMPARE,
    COMPONENT_TYPE,
    CONDITION,
    CONTENT,
    DEFAULT_VALUE,
    DIMENSION,
    ELEMENTS,
    ELSE_EXPR,
    ELSE_STMT,
    ENCLOSING_PARAMETERS,
    ENTRIES,
    EXPRESSION,
    EXTENDED_TYPES(MULTIPLE_NODES),
    FIELD,
    FINALLY_BLOCK,
    IDENTIFIER,
    IMPLEMENTED_TYPES(MULTIPLE_NODES),
    IMPORTS,
    INDEX,
    INITIALIZER,
    INNER,
    IS_INTERFACE,
    ITERABLE,
    IS_THIS,
    LABEL,
    LEFT,
    LEVELS,
    MEMBERS,
    MEMBER_VALUE,
    MODIFIERS(MULTIPLE_PROPERTIES),
    MESSAGE,
    NAME,
    OPERATOR,
    PACKAGE_DECLARATION,
    PAIRS,
    PARAMETER,
    PARAMETERS,
    QUALIFIER,
    RANGE,
    RESOURCES,
    RIGHT,
    SCOPE,
    SELECTOR,
    IS_STATIC,
    STATIC_MEMBER,
    STATEMENT,
    STATEMENTS,
    SUPER,
    TARGET,
    THEN_EXPR,
    THEN_STMT,
    THROWN_TYPES(MULTIPLE_NODES),
    TRY_BLOCK,
    TYPE(SINGLE_PROPERTY),
    TYPES,
    TYPE_ARGUMENTS(MULTIPLE_NODES),
    TYPE_BOUND,
    TYPE_PARAMETERS(MULTIPLE_NODES),
    UPDATE,
    VALUE,
    VALUES,
    VARIABLE,
    VARIABLES(MULTIPLE_NODES),
    ELEMENT_TYPE,
    VAR_ARGS(MULTIPLE_NODES),
    MAXIMUM_COMMON_TYPE();

    enum Type {
        SINGLE_PROPERTY(false, false),
        SINGLE_NODE(false, true),
        MULTIPLE_PROPERTIES(true, false),
        MULTIPLE_NODES(true, true);

        private boolean multiple;
        private boolean node;

        Type(boolean multiple, boolean node) {
            this.multiple = multiple;
            this.node = node;
        }
    }

    private Type type;

    ObservableProperty(Type type) {
        this.type = type;
    }

    ObservableProperty() {
        this(Type.SINGLE_NODE);
    }

    public boolean isAboutNodes() {
        return type.node;
    }

    public boolean isAboutValues() {
        return !isAboutNodes();
    }

    public boolean isMultiple() {
        return type.multiple;
    }

    public boolean isSingle() {
        return !isMultiple();
    }

    public String camelCaseName() {
        return Utils.toCamelCase(name());
    }

    public Node singlePropertyFor(Node node) {
        String getterName = "get" + Utils.capitalize(camelCaseName());
        try {
            Object result = node.getClass().getMethod(getterName).invoke(node);
            if (result == null) {
                return null;
            }
            if (result instanceof Node) {
                return (Node)result;
            } else if (result instanceof Optional){
                Optional<Node> opt = (Optional<Node>)result;
                if (opt.isPresent()) {
                    return opt.get();
                } else {
                    return null;
                }
            } else {
                throw new RuntimeException(String.format("Property %s returned %s (%s)", this.name(), result.toString(), result.getClass().getCanonicalName()));
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Unable to get single value for " + this.name() + " from " + node, e);
        }
    }

    public Object singleValueFor(Node node) {
        String getterName = "get" + Utils.capitalize(camelCaseName());
        try {
            Object result = node.getClass().getMethod(getterName).invoke(node);
            if (result == null) {
                return null;
            }
           if (result instanceof Optional){
                Optional<Node> opt = (Optional<Node>)result;
                if (opt.isPresent()) {
                    return opt.get();
                } else {
                    return null;
                }
            } else {
                return result;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Unable to get single value for " + this.name() + " from " + node, e);
        }
    }

    public NodeList<? extends Node> listValueFor(Node node) {
        String getterName = "get" + Utils.capitalize(camelCaseName());
        try {
            Object result = node.getClass().getMethod(getterName).invoke(node);
            if (result == null) {
                return null;
            }
            if (result instanceof NodeList) {
                return (NodeList) result;
            } else {
                Optional<NodeList> opt = (Optional<NodeList>)result;
                if (opt.isPresent()) {
                    return opt.get();
                } else {
                    return null;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Unable to get list value for " + this.name() + " from " + node + " (class: " + node.getClass().getSimpleName() + ")", e);
        }
    }

    public Collection<?> listPropertyFor(Node node) {
        String getterName = "get" + Utils.capitalize(camelCaseName());
        try {
            Object result = node.getClass().getMethod(getterName).invoke(node);
            if (result == null) {
                return null;
            }
            return (Collection) result;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Unable to get list value for " + this.name() + " from " + node + " (class: " + node.getClass().getSimpleName() + ")", e);
        }
    }

    public String singleStringValueFor(Node node) {
        String getterName = "get" + Utils.capitalize(camelCaseName());
        try {
            return (String)node.getClass().getMethod(getterName).invoke(node);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Unable to get single value for " + this.name() + " from " + node, e);
        }
    }
}
