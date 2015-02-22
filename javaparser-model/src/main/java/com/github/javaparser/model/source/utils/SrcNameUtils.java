package com.github.javaparser.model.source.utils;

import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.github.javaparser.model.scope.EltName;
import com.github.javaparser.model.scope.EltNames;

/**
 * @author Didier Villevalois
 */
public abstract class SrcNameUtils {

	public static EltName asName(NameExpr expr) {
		if (expr instanceof QualifiedNameExpr) {
			QualifiedNameExpr qNameExpr = (QualifiedNameExpr) expr;
			return EltNames.make(asName(qNameExpr.getQualifier()), qNameExpr.getName());
		} else return EltNames.make(expr.getName());
	}
}