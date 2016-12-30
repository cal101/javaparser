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

package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.PrimitiveType;
import org.junit.Test;

import java.io.IOException;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

/**
 * Transforming AnnotationDeclaration and verifying the LexicalPreservation works as expected.
 */
public class AnnotationDeclarationTransformationsTest extends AbstractLexicalPreservingTest {

    @Test
    public void unchangedExamples() throws IOException {
        assertUnchanged("AnnotationDeclaration_Example1");
        assertUnchanged("AnnotationDeclaration_Example3");
    }

    // name

    @Test
    public void changingName() throws IOException {
        considerExample("AnnotationDeclaration_Example1_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setName("NewName");
        assertTransformed("AnnotationDeclaration_Example1", cu);
    }

    // modifiers

    @Test
    public void addingModifiers() throws IOException {
        considerExample("AnnotationDeclaration_Example1_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setModifiers(EnumSet.of(Modifier.PUBLIC));
        assertTransformed("AnnotationDeclaration_Example2", cu);
    }

    @Test
    public void removingModifiers() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setModifiers(EnumSet.noneOf(Modifier.class));
        assertTransformed("AnnotationDeclaration_Example3", cu);
    }

    @Test
    public void replacingModifiers() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setModifiers(EnumSet.of(Modifier.PROTECTED));
        assertTransformed("AnnotationDeclaration_Example4", cu);
    }

    // members

    @Test
    public void addingMember() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().addMember(new AnnotationMemberDeclaration(EnumSet.noneOf(Modifier.class), PrimitiveType.intType(), "foo", null));
        assertTransformed("AnnotationDeclaration_Example5", cu);
    }

    @Test
    public void removingMember() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().getFieldByName("currentRevision").get().remove();
        assertTransformed("AnnotationDeclaration_Example6", cu);
    }

    @Test
    public void replacingMember() throws IOException {
        considerExample("AnnotationDeclaration_Example3_original");
        cu.getAnnotationDeclarationByName("ClassPreamble").get().setMember(2, new AnnotationMemberDeclaration(EnumSet.noneOf(Modifier.class), PrimitiveType.intType(), "foo", null));
        assertTransformed("AnnotationDeclaration_Example7", cu);
    }

    // javadoc

}
