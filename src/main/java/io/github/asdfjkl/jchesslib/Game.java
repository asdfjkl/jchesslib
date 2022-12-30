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
 * Class to store Games. This includes both the actual game tree consisting
 * of GameNode's, as well as meta-information such as the PGN header information
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
     * create a new game. The game will have a root node with
     * an empty board and no pgn headers.
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
     * Given a position hash (similar to a zobrist hash, but without turn or castling rights
     * encoded), this functions search the game tree recursively and will return true if
     * a position appears in the tree
     * @param positionHash the position to look for
     * @param minHalfmove search only game nodes that are at least a number of halfmoves from root
     * @param maxHalfmove search only game nodes that are at most a number of halfmoves from root
     * @return true if a position was found, false otherwise
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

    /** Each node has a unique id associated that is created upon
     * creating the node. Given an id, find the node in the game tree that
     * belongs to that id
     * @param id the id of a node
     * @return the game node if found, throws {@code IllegalArgumentException}
     *         if a node with that id does not exist
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

    /** Get a list of all id's of all nodes that are in the game tree
     * @return contains all the id's
     */
    public ArrayList<Integer> getAllIds() {
        return getAllIds(getRootNode());
    }

    /**
     * checks if the flag {@code treeWasChanged} is set.
     * @return true if the flag is set, false otherwise
     */
    public boolean isTreeChanged() {
        return this.treeWasChanged;
    }

    /**
     * sets the flag {@code treeWasChanged}. This flag can be used
     * to track changes of the game tree and use the information to re-render
     * the game tree in a graphical user interface
     * @param status indicating if tree was changed
     */
    public void setTreeWasChanged(boolean status) {
        this.treeWasChanged = status;
    }

    /**
     * checks if the flag {@code headerWasChanged} is set.
     * @return true if the flag is set, false otherwise
     */
    public boolean isHeaderChanged() { return headerWasChanged; }

    /**
     * sets the flag {@code headerWasChanged}. This flag can be used
     * to track changes of the meta-information of the game and use
     * that info to re-render the meta-information in a graphical
     * user interface
     * @param state indicating if header was changed
     */
    public void setHeaderWasChanged(boolean state) { headerWasChanged = true; }

    /**
     * sets meta-information of the game
     * @param tag e.g. 'Site'
     * @param value e.g. 'London'
     */
    public void setHeader(String tag, String value) {
        this.pgnHeaders.put(tag, value);
    }

    /**
     * returns meta-information of the game. Will return
     * empty string if the information is not available
     * @param tag e.g. 'Site'
     * @return the value of the tag, e.g. 'London'
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
     * reset all meta-information
     */
    public void resetHeaders() {
        this.pgnHeaders = new HashMap<String, String>();
    }

    /**
     * return all meta-information (tags)
     * @return e.g. ['Site', 'Event', ... ]
     */
    public ArrayList<String> getTags() {
        ArrayList<String> tags = new ArrayList<String>();
        tags.addAll(this.pgnHeaders.keySet());
        return tags;
    }

    /**
     * return all meta-information (tag + header)
     * @return hashmap of key,value e.g. key="Site", value="London"
     */
    public HashMap<String,String> getPgnHeaders() {
        return pgnHeaders;
    }

    /**
     * set all pgn headers.
     * @param pgnHeaders must contain keys of tags, and
     *                   values of tag-values
     */
    public void setPgnHeaders(HashMap<String,String> pgnHeaders) {
        this.pgnHeaders = pgnHeaders;
    }

    /**
     * get the root node of the game
     * @return
     */
    public GameNode getRootNode() {
        return this.root;
    }

    /**
     * get the leaf node of the game (seen from root, traversing
     * the main variation)
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
     * return the currently selected node
     * @return
     */
    public GameNode getCurrentNode() {
        return this.current;
    }

    /**
     * return the result of the game
     * @return one of {code CONSTANTS.RES_*}
     */
    public int getResult() {
        return this.result;
    }

    /**
     * set the result of the game
     * @param r one of {code CONSTANTS.RES_*}
     */
    public void setResult(int r) {
        this.result = r;
    }

    /**
     * apply a move. a new variation is generated, and the currently selected
     * node is set to newly generated move. If the move already exists, then
     * the currently selected node is set to the corresponding variation (i.e.
     * child node). This does not check move legality, and will raise an error.
     * Check the legality of the move before calling this method
     * @param m the move to apply
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
     * sets the currently selected node
     * @param newCurrent the node to select
     */
    public void setCurrent(GameNode newCurrent) {
        this.current = newCurrent;
    }

    /**
     * (re)sets the root node of the game
     * @param newRoot the new root node
     */
    public void setRoot(GameNode newRoot) {
        this.root = newRoot;
    }

    /**
     * move the currently selected move down to the child node
     * of the main variation. This has no effect if we are already
     * at a leaf node
     */
    public void goToMainLineChild() {
        if(this.current.getVariations().size() > 0) {
            this.current = this.current.getVariation(0);
        }
    }

    /**
     * move the currently selected move down to the child node
     * of variation with the supplied index (starting from 0 for
     * the main variation). No effect, if such a variation does not
     * exist
     * @param idxChild
     */
    public void goToChild(int idxChild) {
        if(this.current.getVariations().size() > idxChild) {
            this.current = this.current.getVariation(idxChild);
        }
    }

    /**
     * move the currently selected node to the parent.
     * no effect if we are at the root node
     */
    public void goToParent() {
        if(this.current.getParent() != null) {
            this.current = this.current.getParent();
        }
    }

    /**
     * move the currently selected node to the
     * root node
     */
    public void goToRoot() {
        this.current = this.root;
    }

    /**
     * move the currently selected node to the leaf
     * node of the main variation (seen from the root)
     */
    public void goToEnd() {
        GameNode temp = this.root;
        while(temp.getVariations().size() > 0) {
            temp = temp.getVariation(0);
        }
        this.current = temp;
    }

    /**
     * move the variation of the supplied
     * game node one up (i.e. variation 2 becomes
     * variation 1). No effect if the variation
     * is already the main (0-th) variation
     * @param node the start of the variation that should be moved up
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
     * move the variation of the supplied
     * game node one down (i.e. variation 1 becomes
     * variation 2). No effect if the variation
     * is already the last variation
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
     * remove the variation that with
     * the supplied game node. This will traverse
     * the tree up until the start of the variation
     * that contains the supplied node
     * @param node the game node where the variation starts.
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
     * remove all variations below the one that is supplied
     * @param node
     */
    public void delBelow(GameNode node) {
        node.getVariations().clear();
        this.current = node;
    }


    /**
     * remove all comments in the supplied game node
     * and all nodes in all variations below
     * @param node
     */
    public void removeCommentRec(GameNode node) {
        node.setComment("");
        for(GameNode var_i : node.getVariations()) {
            this.removeCommentRec(var_i);
        }
    }

    /**
     * remove all comments in the game
     */
    public void removeAllComments() {
        this.removeCommentRec(this.getRootNode());
    }

    /**
     * move the currently selected node down to a leaf
     * node. This will start from the currently selected
     * node and always chose the main line
     */
    public void goToLeaf() {
        while(!current.isLeaf()) {
            this.goToChild(0);
        }
    }

    /**
     * remove all annotation information (i.e. NAG)
     * in the supplied node and all nodes of all variations
     * below
     * @param node
     */
    public void removeAllAnnotationsRec(GameNode node) {
        node.removeNagsInRange(0,120);
        for(GameNode var_i : node.getVariations()) {
            this.removeAllAnnotationsRec(var_i);
        }
    }

    /**
     * remove all annotations (i.e. NAGs) in the game
     */
    public void removeAllAnnotations() { this.removeAllAnnotationsRec(this.getRootNode()); }

    /**
     * reset the game tree, in particular create a new
     * root node and set the supplied board as the position
     * of that root
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
     * remove all variations of the game, only keep the main line
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
     * clear the seven mandatory PGN headers
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
     * count the number of halfmoves from the root to
     * the leaf of the main variation
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
     * checks if a three-fold repetition occurend
     * inbetween the root and the currently selected node
     * @return true if a three-fold repetition occurred, false otherwise
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
     * checks if there is sufficient material to mate
     * in the currently selected node
     * @return true if there is insufficient material, false otherwise
     */
    public boolean isInsufficientMaterial() {
        if (current != null && current.getBoard() != null)
            return current.getBoard().isInsufficientMaterial();
        return false;
    }

}
