/*
 * Jchesslib - A Java Chess Library
 * The MIT License
 *
 * Copyright 2022 Dominik Klein
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.asdfjkl.jchesslib;

/**
 * Represents an Arrow (i.e. to be drawn on a visual representation of a Board).
 * The Arrow stores source and target squares as tuples (x,y) with x=file from 0=A ... 7=H
 * and y=rank from 0 ... 7.
 */
public class Arrow {

    public int xFrom = 0;
    public int yFrom = 0;
    public int xTo = 0;
    public int yTo = 0;

    public boolean equals(Object o) {

        if (o instanceof Arrow) {
            Arrow other = (Arrow) o;
            if (other.xFrom == xFrom && other.yFrom == yFrom
                    && other.xTo == xTo && other.yTo == yTo) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
