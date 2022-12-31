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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * render a PGN representation of the game
 */
public class PgnPrinter {

    StringBuilder pgn;
    StringBuilder currentLine;
    int variationDepth;
    boolean forceMoveNumber;

    /**
     * create a new PGN printer and init all values
     */
    public PgnPrinter() {
        this.pgn = new StringBuilder();
        this.currentLine = new StringBuilder();
        this.variationDepth = 0;
        this.forceMoveNumber = true;
    }

    private void reset() {
        this.pgn = new StringBuilder();
        this.currentLine = new StringBuilder();
        this.variationDepth = 0;
        this.forceMoveNumber = true;
    }

    private void flushCurrentLine() {
        if(this.currentLine.length() != 0) {
            this.pgn.append(this.currentLine.toString().trim());
            this.pgn.append("\n");
            this.currentLine.setLength(0);
        }
    }

    private void writeToken(String token) {
        if(80 - this.currentLine.length() < token.length()) {
            this.flushCurrentLine();
        }
        this.currentLine.append(token);
    }

    private void writeLine(String line) {
        this.flushCurrentLine();
        this.pgn.append(line.trim()).append("\n");
    }

    private void printHeaders(Game g) {
        String tag = "[Event \"" + g.getHeader("Event") + "\"]";
        pgn.append(tag).append("\n");
        tag = "[Site \"" + g.getHeader("Site") + "\"]";
        pgn.append(tag).append("\n");
        tag = "[Date \"" + g.getHeader("Date") + "\"]";
        pgn.append(tag).append("\n");
        tag = "[Round \"" + g.getHeader("Round") + "\"]";
        pgn.append(tag).append("\n");
        tag = "[White \"" + g.getHeader("White") + "\"]";
        pgn.append(tag).append("\n");
        tag = "[Black \"" + g.getHeader("Black") + "\"]";
        pgn.append(tag).append("\n");
        tag = "[Result \"" + g.getHeader("Result") + "\"]";
        pgn.append(tag).append("\n");
        ArrayList<String> allTags = g.getTags();
        for(String tag_i : allTags) {
            if(!tag_i.equals("Event") && !tag_i.equals("Site") && !tag_i.equals("Date") && !tag_i.equals("Round")
                    && !tag_i.equals("White") && !tag_i.equals("Black") && !tag_i.equals("Result" ))
            {
                String value_i = g.getHeader(tag_i);
                String tag_val = "[" + tag_i + " \"" + value_i + "\"]";
                pgn.append(tag_val).append("\n");
            }
        }
        // add fen string tag if root is not initial position
        Board rootBoard = g.getRootNode().getBoard();
        if(!rootBoard.isInitialPosition()) {
            String tag_fen = "[FEN \"" + rootBoard.fen() + "\"]";
            pgn.append(tag_fen).append("\n");
        }
    }

    private void printMove(GameNode node) {
        Board b = node.getParent().getBoard();
        if(b.turn == CONSTANTS.WHITE) {
            String tkn = Integer.toString(b.fullmoveNumber);
            tkn += ". ";
            this.writeToken(tkn);
        }
        else if(this.forceMoveNumber) {
            String tkn = Integer.toString(b.fullmoveNumber);
            tkn += "... ";
            this.writeToken(tkn);
        }
        this.writeToken(node.getSan() + " ");
        this.forceMoveNumber = false;
    }

    private void printNag(int nag) {
        String tkn = "$" + nag + " ";
        this.writeToken(tkn);
    }

    private void printResult(int result) {
        String res = "";
        if(result == CONSTANTS.RES_WHITE_WINS) {
            res += "1-0";
        } else if(result == CONSTANTS.RES_BLACK_WINS) {
            res += "0-1";
        } else if(result == CONSTANTS.RES_DRAW) {
            res += "1/2-1/2";
        } else {
            res += "*";
        }
        this.writeToken(res + " ");
    }

    private void beginVariation() {
        this.variationDepth++;
        String tkn = "( ";
        this.writeToken(tkn);
        this.forceMoveNumber = true;
    }

    private void endVariation() {
        this.variationDepth--;
        String tkn = ") ";
        this.writeToken(tkn);
        this.forceMoveNumber = true;
    }

    private void printComment(String comment) {
        String write = "{ " + comment.replace("}","").trim() + " } ";
        this.writeToken(write);
        //this->forceMoveNumber = false;
    }

    private void printGameContent(GameNode g) {

        Board b = g.getBoard();

        // first write mainline move, if there are variations
        int cntVar = g.getVariations().size();
        if(cntVar > 0) {
            GameNode mainVariation = g.getVariation(0);
            this.printMove(mainVariation);
            // write nags
            for(Integer ni : mainVariation.getNags()) {
                this.printNag(ni);
            }
            // write comments
            if(!mainVariation.getComment().isEmpty()) {
                this.printComment(mainVariation.getComment());
            }
        }

        // now handle all variations (sidelines)
        for(int i=1;i<cntVar;i++) {
            // first create variation start marker, and print the move
            GameNode var_i = g.getVariation(i);
            this.beginVariation();
            this.printMove(var_i);
            // next print nags
            for(Integer ni : var_i.getNags()) {
                this.printNag(ni);
            }
            // finally print comments
            if(!var_i.getComment().isEmpty()) {
                this.printComment(var_i.getComment());
            }

            // recursive call for all children
            this.printGameContent(var_i);

            // print variation end
            this.endVariation();
        }

        // finally do the mainline
        if(cntVar > 0) {
            GameNode mainVariation = g.getVariation(0);
            this.printGameContent(mainVariation);
        }
    }

    /**
     * render a PGN representation of the supplied game
     * @param g the game for which a PGN representation is desired
     * @return PGN formatted string
     */
    public String printGame(Game g) {

        this.reset();

        this.printHeaders(g);

        this.writeLine("");
        GameNode root = g.getRootNode();

        // special case if the root node has
        // a comment before the actual game starts
        if(!root.getComment().isEmpty()) {
            this.printComment(root.getComment());
        }

        this.printGameContent(root);
        this.printResult(g.getResult());
        this.pgn.append(this.currentLine.toString());

        return this.pgn.toString();
    }

    /**
     * render a PGN representation of the supplied game
     * and directly write that to a file
     * @param g the game for which a PGN representation is desired
     * @param filename filename of the desired PGN
     */
    public void writeGame(Game g, String filename) {

        BufferedWriter out = null;
        String pgn = this.printGame(g);
        try {
            out = Files.newBufferedWriter(Path.of(filename));
            out.write(pgn);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
             if(out != null) {
                 try {
                     out.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
        }
    }

}

