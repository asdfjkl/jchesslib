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
import java.util.HashMap;

/**
 *
 */
public class Game {

    private GameNode root = null;
    private GameNode current = null;
    private int result;
    private boolean treeWasChanged;
    private boolean headerWasChanged;
    private boolean wasEcoClassified;
    private HashMap<String, String> pgnHeaders;

    /**
     *
     */
    public Game() {
        this.root = new GameNode();
        this.result = CONSTANTS.RES_UNDEF;
        this.current = this.root;
        this.treeWasChanged = false;
        this.headerWasChanged = false;
        this.wasEcoClassified = false;
        this.pgnHeaders = new HashMap<String,String>();
    }

    /**
     * @param positionHash
     * @param node
     * @param maxHalfmove
     * @return
     */
    private boolean containsPositionRec(long positionHash, GameNode node, int maxHalfmove) {
        if(maxHalfmove <= node.getBoard().halfmoveClock) {
            return false;
        }
        if(node.getBoard().getPositionHash() == positionHash) {
            return true;
        } else {
            for(GameNode var_i : node.getVariations()) {
                boolean hasPosition = containsPositionRec(positionHash, var_i, maxHalfmove);
                if(hasPosition) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param positionHash
     * @param minHalfmove
     * @param maxHalfmove
     * @return
     */
    public boolean containsPosition(long positionHash, int minHalfmove, int maxHalfmove) {
        GameNode current = this.getRootNode();
        for(int i=0;i<minHalfmove-1;i++) {
            if(current.hasChild()) {
                current = current.getVariation(0);
            } else {
                return false;
            }
        }
        return containsPositionRec(positionHash, current, maxHalfmove);
    }

    private GameNode findNodeByIdRec(int id, GameNode node) {
        if(node.getId() == id) {
            return node;
        } else {
            for(GameNode var_i : node.getVariations()) {
                GameNode result = this.findNodeByIdRec(id, var_i);
                if(result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * @param id
     * @return
     */
    public GameNode findNodeById(int id) {
        GameNode current = this.getRootNode();
        GameNode result = this.findNodeByIdRec(id, current);
        if(result == null) {
            throw new IllegalArgumentException("node with id "+id+" doesn't exist!");
        } else {
            return result;
        }
    }

    private ArrayList<Integer> getAllIds(GameNode node) {

        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(node.getId());
        for(GameNode nodeI : node.getVariations()) {
            ids.addAll(getAllIds(nodeI));
        }
        return ids;
    }

    /**
     * @return
     */
    public ArrayList<Integer> getAllIds() {
        return getAllIds(getRootNode());
    }

    /**
     * @return
     */
    public boolean isTreeChanged() {
        return this.treeWasChanged;
    }

    /**
     * @param status
     */
    public void setTreeWasChanged(boolean status) {
        this.treeWasChanged = status;
    }

    /**
     * @return
     */
    public boolean isHeaderChanged() { return headerWasChanged; }

    /**
     * @param state
     */
    public void setHeaderWasChanged(boolean state) { headerWasChanged = true; }

    /**
     * @param tag
     * @param value
     */
    public void setHeader(String tag, String value) {
        this.pgnHeaders.put(tag, value);
    }

    /**
     * @param tag
     * @return
     */
    public String getHeader(String tag) {
        String value = this.pgnHeaders.get(tag);
        if(value == null) {
            return "";
        } else {
            return value;
        }
    }

    /**
     *
     */
    public void resetHeaders() {
        this.pgnHeaders = new HashMap<String, String>();
    }

    /**
     * @return
     */
    public ArrayList<String> getTags() {
        ArrayList<String> tags = new ArrayList<String>();
        tags.addAll(this.pgnHeaders.keySet());
        return tags;
    }

    /**
     * @return
     */
    public HashMap<String,String> getPgnHeaders() {
        return pgnHeaders;
    }

    /**
     * @param pgnHeaders
     */
    public void setPgnHeaders(HashMap<String,String> pgnHeaders) {
        this.pgnHeaders = pgnHeaders;
    }

    /**
     * @return
     */
    public GameNode getRootNode() {
        return this.root;
    }

    /**
     * @return
     */
    public GameNode getEndNode() {
        GameNode temp = this.getRootNode();
        while(temp.getVariations().size() > 0) {
            temp = temp.getVariation(0);
        }
        return temp;
    }

    /**
     * @return
     */
    public GameNode getCurrentNode() {
        return this.current;
    }

    /**
     * @return
     */
    public int getResult() {
        return this.result;
    }

    /**
     * @param r
     */
    public void setResult(int r) {
        this.result = r;
    }

    /**
     * @param m
     */
    public void applyMove(Move m) {
        boolean existsChild = false;
        for(GameNode var_i : this.current.getVariations()) {
            Move mi = var_i.getMove();
            if(m.from == mi.from && m.to == mi.to && m.promotionPiece == mi.promotionPiece) {
                existsChild = true;
                this.current = var_i;
                break;
            }
        }
        if(!existsChild) {
            GameNode current = this.getCurrentNode();
            Board bCurrent = current.getBoard();
            Board bChild = bCurrent.makeCopy();
            bChild.apply(m);
            GameNode newCurrent = new GameNode();
            newCurrent.setBoard(bChild);
            newCurrent.setMove(m);
            newCurrent.setParent(current);
            current.getVariations().add(newCurrent);
            this.current = newCurrent;
            this.treeWasChanged = true;
        }
    }

    /**
     * @param newCurrent
     */
    public void setCurrent(GameNode newCurrent) {
        this.current = newCurrent;
    }

    /**
     * @param newRoot
     */
    public void setRoot(GameNode newRoot) {
        this.root = newRoot;
    }

    /**
     *
     */
    public void goToMainLineChild() {
        if(this.current.getVariations().size() > 0) {
            this.current = this.current.getVariation(0);
        }
    }

    /**
     * @param idxChild
     */
    public void goToChild(int idxChild) {
        if(this.current.getVariations().size() > idxChild) {
            this.current = this.current.getVariation(idxChild);
        }
    }

    /**
     *
     */
    public void goToParent() {
        if(this.current.getParent() != null) {
            this.current = this.current.getParent();
        }
    }

    /**
     *
     */
    public void goToRoot() {
        this.current = this.root;
    }

    /**
     *
     */
    public void goToEnd() {
        GameNode temp = this.root;
        while(temp.getVariations().size() > 0) {
            temp = temp.getVariation(0);
        }
        this.current = temp;
    }

    /**
     * @param node
     */
    public void moveUp(GameNode node) {
        if(node.getParent() != null) {
            GameNode parent = node.getParent();
            int i = parent.getVariations().indexOf(node);
            if (i > 0) {
                parent.getVariations().remove(i);
                parent.getVariations().add(i - 1, node);
            }
            this.treeWasChanged = true;
        }
    }

    /**
     * @param node
     */
    public void moveDown(GameNode node) {
        if(node.getParent() != null) {
            GameNode parent = node.getParent();
            int i = parent.getVariations().indexOf(node);
            if(i < parent.getVariations().size() -1) {
                parent.getVariations().remove(i);
                parent.getVariations().add(i+1,node);
            }
            this.treeWasChanged = true;
        }
    }

    /**
     * @param node
     */
    public void delVariant(GameNode node) {
        // go up the variation until we
        // find the root of the variation
        GameNode child = node;
        GameNode variationRoot = node;
        while (variationRoot.getParent() != null && variationRoot.getParent().getVariations().size() == 1) {
            child = variationRoot;
            variationRoot = variationRoot.getParent();
        }
        int idx = -1;
        // one more to get the actual root
        if (variationRoot.getParent() != null) {
            child = variationRoot;
            variationRoot = variationRoot.getParent();
            idx = variationRoot.getVariations().indexOf(child);
        }
        if (idx != -1) {
            variationRoot.getVariations().remove(idx);
            this.current = variationRoot;
        }
    }

    /**
     * @param node
     */
    public void delBelow(GameNode node) {
        node.getVariations().clear();
        this.current = node;
    }


    /**
     * @param node
     */
    public void removeCommentRec(GameNode node) {
        node.setComment("");
        for(GameNode var_i : node.getVariations()) {
            this.removeCommentRec(var_i);
        }
    }

    /**
     *
     */
    public void removeAllComments() {
        this.removeCommentRec(this.getRootNode());
    }

    /**
     *
     */
    public void goToLeaf() {
        while(!current.isLeaf()) {
            this.goToChild(0);
        }
    }

    /**
     * @param node
     */
    public void removeAllAnnotationsRec(GameNode node) {
        node.removeNagsInRange(0,120);
        for(GameNode var_i : node.getVariations()) {
            this.removeAllAnnotationsRec(var_i);
        }
    }

    /**
     *
     */
    public void removeAllAnnotations() { this.removeAllAnnotationsRec(this.getRootNode()); }

    /**
     * @param newRootBoard
     */
    public void resetWithNewRootBoard(Board newRootBoard) {
        GameNode oldRoot = this.getRootNode();
        this.delBelow(oldRoot);
        GameNode newRoot = new GameNode();
        newRoot.setBoard(newRootBoard);
        this.setRoot(newRoot);
        this.setCurrent(newRoot);
        this.result = CONSTANTS.RES_UNDEF;
        this.clearHeaders();
        this.treeWasChanged = true;
    }

    /**
     *
     */
    public void removeAllVariants() {
        GameNode temp = this.getRootNode();
        int size = temp.getVariations().size();
        while(size > 0) {
            GameNode main = temp.getVariations().get(0);
            temp.getVariations().clear();
            temp.getVariations().add(main);
            temp = main;
            size = temp.getVariations().size();
        }
        this.current = this.getRootNode();
    }

    /**
     *
     */
    public void clearHeaders() {
        this.pgnHeaders.clear();
        this.pgnHeaders.put("Event", "");
        this.pgnHeaders.put("Date", "");
        this.pgnHeaders.put("Round", "");
        this.pgnHeaders.put("White", "");
        this.pgnHeaders.put("Black", "");
        this.pgnHeaders.put("Result", "*");
    }

    /**
     * @return
     */
    public int countHalfmoves() {
        int halfmoves = 0;
        GameNode temp = this.root;
        while(temp.getVariations().size() > 0) {
            temp = temp.getVariation(0);
            halfmoves += 1;
        }
        return halfmoves;
    }

    /**
     * @return
     */
    public boolean isThreefoldRepetition() {

        int counter = 1;
        long zobrist = current.getBoard().getZobrist();
        GameNode temp = this.current;
        while(temp.getParent() != null) {
            temp = temp.getParent();
            long tempZobrist = temp.getBoard().getZobrist();
            if(tempZobrist == zobrist) {
                counter++;
            }
            if(counter >= 3) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     */
    public boolean isInsufficientMaterial() {
        if (current != null && current.getBoard() != null)
            return current.getBoard().isInsufficientMaterial();
        return false;
    }

}
