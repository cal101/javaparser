/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2015 The JavaParser Team.
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
 
package com.github.javaparser.ast.body;

import com.github.javaparser.ast.NamedNode;
import com.github.javaparser.ast.NodeWithModifiers;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NameExpr;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Set;

/**
 * @author Julio Vilmar Gesser
 */
public abstract class TypeDeclaration extends BodyDeclaration implements NamedNode, NodeWithModifiers {

	private NameExpr name;

	private Set<Modifier> modifiers;

	private List<BodyDeclaration> members;

	public TypeDeclaration() {
	}

	public TypeDeclaration(int modifiers, String name) {
		setName(name);
		setModifiers(modifiers);
	}

	public TypeDeclaration(List<AnnotationExpr> annotations,
			int modifiers, String name,
			List<BodyDeclaration> members) {
		super(annotations);
		setName(name);
		setModifiers(modifiers);
		setMembers(members);
	}

	public TypeDeclaration(int beginLine, int beginColumn, int endLine,
			int endColumn, List<AnnotationExpr> annotations,
			int modifiers, String name,
			List<BodyDeclaration> members) {
		super(beginLine, beginColumn, endLine, endColumn, annotations);
		setName(name);
		setModifiers(modifiers);
		setMembers(members);
	}

	public final List<BodyDeclaration> getMembers() {
		return members;
	}

	/**
	 * Return the modifiers of this member declaration.
	 *
	 * @see ModifierSet
	 * @return modifiers
	 * @deprecated please use getModifiersSet instead
	 */
	@Override
	@Deprecated
	public int getModifiers() {
		return ModifierSet.toInt(modifiers);
	}

	/**
	 * Return the modifiers of this member declaration.
	 *
	 * @see ModifierSet
	 * @return modifiers
	 */
	@Override
	public Set<Modifier> getModifiersSet() {
		return modifiers;
	}

	public final String getName() {
		return name.getName();
	}

	public void setMembers(List<BodyDeclaration> members) {
		this.members = members;
		setAsParentNodeOf(this.members);
	}

	public final void setModifiers(Set<Modifier> modifiers) {
		this.modifiers = modifiers;
	}

	public final void setModifiers(int modifiers) {
		this.modifiers = ModifierSet.toSet(modifiers);
	}

	public final void setName(String name) {
		this.name = new NameExpr(name);
	}

    public final void setNameExpr(NameExpr nameExpr) {
      this.name = nameExpr;
    }

    public final NameExpr getNameExpr() {
      return name;
    }
}
