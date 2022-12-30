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

    /**
     * create a new node. the move leading to that
     * node, the current board and the parent
     * will all be set to {@code null}, i.e.
     * must set a board separately before using it
     */
    public GameNode() {
        this.nodeId = initId();
        this.variations = new ArrayList<GameNode>();
        this.nags = new ArrayList<Integer>();
        //this.board = new Board(true);
        //this.board.resetToStartingPosition();
        this.comment = "";
        this.sanCache = "";
    }

    /**
     * adds the supplied {@code Arrow} if
     * the arrow is not already existing. If the
     * supplied arrow already exists for this node,
     * it will be removed instead
     * @param arrow
     */
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

    /**
     * get all the arrows that were set
     * for this node
     * @return
     */
    public ArrayList<Arrow> getArrows() {
        if(arrows == null) {
            arrows = new ArrayList<Arrow>();
        }
        return arrows;
    }

    /**
     * add the supplied {@code coloredField} if the
     * coloredField does not exist for this node.
     * It it was added before, it will be removed instead.
     * @param coloredField
     */
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

    /**
     * get all coloredFields for this node
      * @return
     */
    public ArrayList<ColoredField> getColoredFields() {
        if(coloredFields == null) {
            coloredFields = new ArrayList<ColoredField>();
        }
        return coloredFields;
    }

    /**
     * each node has a unique id that is created during
     * object construction.
     * @return the id of the node
     */
    public int getId() {
        return this.nodeId;
    }

    /**
     * get the board of this game node.
     * @return the board. {@code null} if not set.
     */
    public Board getBoard() {
        return this.board;
    }

    /**
     * set the board of this node to the supplied one
     * @param b
     */
    public void setBoard(Board b) {
        this.board = b;
    }

    /**
     * get a short algebraic notation of the supplied Move.
     * @param m Move for which the SAN is desired
     * @return String with SAN representation
     */
    public String getSan(Move m) {
        //return this.board.san(m) + "(" + m.getUci() +")";
        return this.board.san(m);
    }

    /**
     * get a short algebraic notation of the Move that leads
     * to this node. Will raise an error if the Move has
     * not been set before
     * @return String with SAN representation
     */
    public String getSan() {
        if(this.sanCache.isEmpty() && this.parent != null) {
            this.sanCache = this.parent.getSan(this.move);
        }
        return this.sanCache;
    }

    /**
     * get the parent of this node
     * @return parent node
     */
    public GameNode getParent() {
        return this.parent;
    }

    /**
     * get the Move that leads to this node.
      * @return The Move. Null if it was not set.
     */
    public Move getMove() {
        return this.move;
    }

    /**
     * set the Move that leads to this node.
     * @param m The Move.
     */
    public void setMove(Move m) {
        this.move = m;
    }

    /**
     * sets the supplied node as the parent of this node.
     * @param node
     */
    public void setParent(GameNode node) {
        this.parent = node;
    }

    /**
     * adds a comment
     * @param s
     */
    public void setComment(String s) {
        this.comment = s;
    }

    /**
     * gets the comment of this node. Null if it was
     * not set before
     * @return
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * get the node of variation i (counting from 0,
     * where 0 is the main variation). Throws
     * an {@code IllegalArgumentException} if that
     * variation does not exist
     * @param i index of the desired variation
     * @return the start node of the variation
     */
    public GameNode getVariation(int i) {
        if(this.variations.size() > i) {
            return this.variations.get(i);
        } else {
            throw new IllegalArgumentException("there are only "+this.variations.size() + " variations, but index "+i + "requested");
        }
    }

    /**
     * delete variation i (counting from 0, where 0
     * is the main variation). Throws an {@code IllegalArgumentException}
     * if that variation does not exist.
     * @param i index of the desired variation
     */
    public void deleteVariation(int i) {
        if(this.variations.size() > i) {
            this.variations.remove(i);
        } else {
            throw new IllegalArgumentException("there are only "+this.variations.size() + " variations, " +
                    "but index "+i + "requested for deletion");
        }
    }

    /**
     * get all variations of the current node
     * @return
     */
    public ArrayList<GameNode> getVariations() {
        return this.variations;
    }

    /**
     * add a variation. It will become the last
     * variation of the current node
     * @param node the start node of the variation
     */
    public void addVariation(GameNode node) {
        this.variations.add(node);
    }

    /**
     * checks if the node has variations (i.e.
     * more than one child)
     * @return true if there are more than 1 children
     */
    public boolean hasVariations() {
        return this.variations.size() > 1;
    }

    /**
     * checks if the node has children
     * @return true if this is not a leaf node
     */
    public boolean hasChild() { return  this.variations.size() > 0; }

    /**
     * checks if the node is a leaf node
     * @return
     */
    public boolean isLeaf() {
        return this.variations.size() == 0;
    }

    /**
     * add a numeric annotation glyph (cf. PGN standard)
     * @param n the NAG encoded as an integer
     */
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

    /**
     * get all NAGs (cf. PGN standard)
     * @return
     */
    public ArrayList<Integer> getNags() {
        return this.nags;
    }

    /**
     * the the depth of this variation from root
     * @return
     */
    public int getDepth() {
        if(this.parent == null) {
            return 0;
        } else {
            return this.parent.getDepth() + 1;
        }
    }

    /**
     * remove NAGs (numeric annotation glyphs) within
     * a specified range >= start and <= stop
     * @param start starting value of the range
     * @param stop stop value of the range
     */
    public void removeNagsInRange(int start, int stop) {
        ArrayList<Integer> filteredNags = new ArrayList<>();
        for(Integer i : this.nags) {
            if(!(start <= i && i <= stop)) {
                filteredNags.add(i);
            }
        }
        this.nags = filteredNags;
    }

    /**
     * sort NAGs according to their integer number
     */
    public void sortNags() {
        Collections.sort(this.nags);
    }

}
