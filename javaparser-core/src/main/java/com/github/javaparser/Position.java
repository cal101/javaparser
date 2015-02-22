package com.github.javaparser;

import com.github.javaparser.ast.Node;

public class Position {
    private int line;
    private int column;

    public static final Position ABSOLUTE_START = new Position(Node.ABSOLUTE_BEGIN_LINE,-1);
    public static final Position ABSOLUTE_END = new Position(Node.ABSOLUTE_END_LINE,-1);

    public static Position beginOf(Node node){
        return new Position(node.getBeginLine(),node.getBeginColumn());
    }

    public static Position endOf(Node node){
        return new Position(node.getEndLine(),node.getEndColumn());
    }

    public Position(int line, int column){
        this.line = line;
        this.column = column;
    }

    public int getLine(){
        return this.line;
    }

    public int getColumn(){
        return this.column;
    }
}
