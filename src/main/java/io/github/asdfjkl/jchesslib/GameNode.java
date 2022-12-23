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

import java.util.ArrayList;
import java.util.Collections;

public class GameNode {

    static int id;
    private final int nodeId;
    private Board board = null;
    private Move move = null; // move leading to this node
    private GameNode parent = null;
    private String comment;
    private String sanCache;
    private final ArrayList<GameNode> variations;
    private ArrayList<Integer> nags;
    private ArrayList<ColoredField> coloredFields;
    private ArrayList<Arrow> arrows;

    protected static int initId() {
        return id++;
    }

    public GameNode() {
        this.nodeId = initId();
        this.variations = new ArrayList<GameNode>();
        this.nags = new ArrayList<Integer>();
        //this.board = new Board(true);
        //this.board.resetToStartingPosition();
        this.comment = "";
        this.sanCache = "";
    }

    public void addOrRemoveArrow(Arrow arrow) {
        if(arrows == null) {
            arrows = new ArrayList<Arrow>();
            arrows.add(arrow);
        } else {
            if(!arrows.contains(arrow)) {
                arrows.add(arrow);
            } else {
                int idx = arrows.indexOf(arrow);
                arrows.remove(idx);
            }
        }
    }

    public ArrayList<Arrow> getArrows() {
        if(arrows == null) {
            arrows = new ArrayList<Arrow>();
        }
        return arrows;
    }

    public void addOrRemoveColoredField(ColoredField coloredField) {
        if(coloredFields == null) {
            coloredFields = new ArrayList<ColoredField>();
            coloredFields.add(coloredField);
        } else {
            if(!coloredFields.contains(coloredField)) {
                coloredFields.add(coloredField);
            } else {
                int idx = coloredFields.indexOf(coloredField);
                coloredFields.remove(idx);
            }
        }
    }


    public ArrayList<ColoredField> getColoredFields() {
        if(coloredFields == null) {
            coloredFields = new ArrayList<ColoredField>();
        }
        return coloredFields;
    }

    public int getId() {
        return this.nodeId;
    }

    public Board getBoard() {
        return this.board;
    }

    public void setBoard(Board b) {
        this.board = b;
    }

    public String getSan(Move m) {
        //return this.board.san(m) + "(" + m.getUci() +")";
        return this.board.san(m);
    }

    public String getSan() {
        if(this.sanCache.isEmpty() && this.parent != null) {
            this.sanCache = this.parent.getSan(this.move);
        }
        return this.sanCache;
    }

    public GameNode getParent() {
        return this.parent;
    }

    public Move getMove() {
        return this.move;
    }

    public void setMove(Move m) {
        this.move = m;
    }

    public void setParent(GameNode node) {
        this.parent = node;
    }

    public void setComment(String s) {
        this.comment = s;
    }

    public String getComment() {
        return this.comment;
    }

    public GameNode getVariation(int i) {
        if(this.variations.size() > i) {
            return this.variations.get(i);
        } else {
            throw new IllegalArgumentException("there are only "+this.variations.size() + " variations, but index "+i + "requested");
        }
    }

    public void deleteVariation(int i) {
        if(this.variations.size() > i) {
            this.variations.remove(i);
        } else {
            throw new IllegalArgumentException("there are only "+this.variations.size() + " variations, " +
                    "but index "+i + "requested for deletion");
        }
    }

    public ArrayList<GameNode> getVariations() {
        return this.variations;
    }

    public void addVariation(GameNode node) {
        this.variations.add(node);
    }

    public boolean hasVariations() {
        return this.variations.size() > 1;
    }

    public boolean hasChild() { return  this.variations.size() > 0; }

    public boolean isLeaf() {
        return this.variations.size() == 0;
    }

    public void addNag(int n) {
        if(!nags.contains(n)) {
            // if move annotation, first remove
            // old move annotation
            if(n > 0 && n < 11) {
                removeNagsInRange(1,10);
            }
            // same for position annotation
            if(n > 12 && n < 20) {
                removeNagsInRange(12,20);
            }
            this.nags.add(n);
        }
    }

    public ArrayList<Integer> getNags() {
        return this.nags;
    }

    public int getDepth() {
        if(this.parent == null) {
            return 0;
        } else {
            return this.parent.getDepth() + 1;
        }
    }

    public void removeNagsInRange(int start, int stop) {
        ArrayList<Integer> filteredNags = new ArrayList<>();
        for(Integer i : this.nags) {
            if(!(start <= i && i <= stop)) {
                filteredNags.add(i);
            }
        }
        this.nags = filteredNags;
    }

    public void sortNags() {
        Collections.sort(this.nags);
    }

}
