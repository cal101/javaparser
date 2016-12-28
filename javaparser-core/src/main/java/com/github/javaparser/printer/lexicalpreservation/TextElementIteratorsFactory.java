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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TextElementIteratorsFactory {

    static class CascadingIterator<E> implements Iterator<E> {
        interface Provider<E> {
            Iterator<E> provide();
        }

        private Provider<E> nextProvider;
        private Iterator<E> current;

        public CascadingIterator(Iterator<E> current, Provider<E> nextProvider) {
            this.nextProvider = nextProvider;
            this.current = current;
        }

        private Iterator<E> next;

        @Override
        public boolean hasNext() {
            if (current.hasNext()) {
                return true;
            }
            if (next == null) {
                next = nextProvider.provide();
            }
            return next.hasNext();
        }

        @Override
        public E next() {
            if (current.hasNext()) {
                return current.next();
            }
            if (next == null) {
                next = nextProvider.provide();
            }
            return next.next();
        }
    }

    static class EmptyIterator<E> implements Iterator<E> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public E next() {
            throw new IllegalArgumentException();
        }
    }

    private static class SingleElementIterator<E> implements Iterator<E> {
        private E element;
        private boolean returned;

        SingleElementIterator(E element) {
            this.element = element;
        }

        @Override
        public boolean hasNext() {
            return !returned;
        }

        @Override
        public E next() {
            returned = true;
            return element;
        }
    }

    static class ComposedIterator<E> implements Iterator<E> {
        private List<Iterator<E>> elements;
        private int currIndex;

        ComposedIterator(List<Iterator<E>> elements) {
            this.elements = elements;
            currIndex = 0;
        }

        @Override
        public boolean hasNext() {
            if (currIndex >= elements.size()) {
                return false;
            }
            if (elements.get(currIndex).hasNext()){
                return true;
            }
            currIndex++;
            return hasNext();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new IllegalArgumentException();
            }
            return elements.get(currIndex).next();
        }
    }

    public static Iterator<TokenTextElement> reverseIterator(TextElement textElement) {
        if (textElement instanceof TokenTextElement) {
            return new SingleElementIterator<>((TokenTextElement)textElement);
        } else if (textElement instanceof ChildTextElement) {
            ChildTextElement childTextElement = (ChildTextElement)textElement;
            NodeText textForChild = childTextElement.getNodeTextForWrappedNode();
            return reverseIterator(textForChild);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static Iterator<TokenTextElement> reverseIterator(NodeText nodeText) {
        return reverseIterator(nodeText, nodeText.numberOfElements() - 1);
    }

    public static Iterator<TokenTextElement> reverseIterator(NodeText nodeText, int fromIndex) {
        List<Iterator<TokenTextElement>> elements = new LinkedList<>();
        for (int i=fromIndex;i>=0;i--) {
            elements.add(reverseIterator(nodeText.getTextElement(i)));
        }
        return new ComposedIterator<>(elements);
    }

}
