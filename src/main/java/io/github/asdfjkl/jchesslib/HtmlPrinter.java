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

/**
 * Render a html representation of the game.
 */
public class HtmlPrinter {

    StringBuilder html;
    StringBuilder currentLine;
    int variationDepth;
    boolean forceMoveNumber;
    boolean newLine;

    /**
     * create a new html printer and init all values
     */
    public HtmlPrinter() {
        this.html = new StringBuilder();
        this.currentLine = new StringBuilder();
        this.variationDepth = 0;
        this.forceMoveNumber = true;
        this.newLine = false;
    }

    private void reset() {
        this.html = new StringBuilder();
        this.currentLine = new StringBuilder();
        this.variationDepth = 0;
        this.forceMoveNumber = true;
        this.newLine = false;
    }

    private void flushCurrentLine() {
        if(this.currentLine.length() != 0) {
            this.html.append(this.currentLine.toString().trim());
            this.html.append("\n");
            this.currentLine.setLength(0);
        }
    }

    private void writeToken(String token) {
        //if(80 - this.currentLine.length() < token.length()) {
        //    this.flushCurrentLine();
        //}
        this.currentLine.append(token);
    }

    private void writeLine(String line) {
        this.flushCurrentLine();
        this.html.append(line.trim()).append("\n");
    }

    private void printHeaders(Game g) {
        String tag = "[Event \"" + g.getHeader("Event") + "\"]";
        html.append(tag).append("\n");
        tag = "[Site \"" + g.getHeader("Site") + "\"]";
        html.append(tag).append("\n");
        tag = "[Date \"" + g.getHeader("Date") + "\"]";
        html.append(tag).append("\n");
        tag = "[Round \"" + g.getHeader("Round") + "\"]";
        html.append(tag).append("\n");
        tag = "[White \"" + g.getHeader("White") + "\"]";
        html.append(tag).append("\n");
        tag = "[Black \"" + g.getHeader("Black") + "\"]";
        html.append(tag).append("\n");
        tag = "[Result \"" + g.getHeader("Result") + "\"]";
        html.append(tag).append("\n");
        ArrayList<String> allTags = g.getTags();
        for(String tag_i : allTags) {
            if(!tag_i.equals("Event") && !tag_i.equals("Site") && !tag_i.equals("Date") && !tag_i.equals("Round")
                    && !tag_i.equals("White") && !tag_i.equals("Black") && !tag_i.equals("Result" ))
            {
                String value_i = g.getHeader(tag_i);
                String tag_val = "[" + tag_i + " \"" + value_i + "\"]";
                html.append(tag_val).append("\n");
            }
        }
        // add fen string tag if root is not initial position
        Board rootBoard = g.getRootNode().getBoard();
        if(!rootBoard.isInitialPosition()) {
            String tag_fen = "[FEN \"" + rootBoard.fen() + "\"]";
            html.append(tag_fen).append("\n");
        }
    }




    private void printMove(GameNode node) {
        int nodeId = node.getId();
        String sNodeId = Integer.toString(nodeId);
        Board b = node.getParent().getBoard();

        writeToken("<span id=\"n");
        writeToken(sNodeId);
        writeToken("\">");
        //writeToken("<a id=\"n");
        //writeToken(sNodeId);
        //writeToken("\" href=\"#");
        writeToken("<a href=\"#");
        writeToken(sNodeId);
        writeToken("\">");

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
        this.writeToken(node.getSan());
        this.writeToken("</a>");
        writeToken("</span>");

        this.forceMoveNumber = false;
        this.newLine = false;
    }

    private void printNag(int nag) {
        String tkn = "";
        switch(nag) {
            case 1:
                tkn += "!";
                break;
            case 2:
                tkn += "?";
                break;
            case 3:
                tkn += "!!";
                break;
            case 4:
                tkn += "??";
                break;
            case 5:
                tkn += "!?";
                break;
            case 6:
                tkn += "?!";
                break;
            case 10:
                tkn += "=";
                break;
            case 13:
                tkn += "∞";
                break;
            case 14:
                tkn += "⩲";
                break;
            case 15:
                tkn += "⩱";
                break;
            case 16:
                tkn += "±";
                break;
            case 17:
                tkn += "∓";
                break;
            case 18:
                tkn += "+−";
                break;
            case 19:
                tkn += "−+";
                break;
            case 22:
                tkn += "⨀";
                break;
            case 23:
                tkn += "⨀";
                break;
            case 132:
                tkn += "⇆";
                break;
            case 133:
                tkn += "⇆";
                break;
            default:
                tkn += "$ " + nag;

        }
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

        if(this.variationDepth == 1) {
            // if we just started a new line due to
            // ending a previous variation directly below
            // mainline, we do not need to add another linebreak
            String tkn = "";
            if(this.newLine) {
                tkn += "&nbsp;[ ";
            } else {
                tkn += "<br>&nbsp;[ ";
            }
            this.writeToken(tkn);
            this.forceMoveNumber = true;
        } else {
            String tkn = "( ";
            this.writeToken(tkn);
            this.forceMoveNumber = true;
        }
        this.newLine = false;
    }

    private void endVariation() {
        this.variationDepth--;
        if(this.variationDepth == 0) {
            String tkn = "]<br> ";
            this.writeToken(tkn);
            this.forceMoveNumber = true;
            this.newLine = true;
        } else {
            String tkn = ") ";
            this.writeToken(tkn);
            this.forceMoveNumber = true;
        }
    }

    private void printComment(String comment) {
        String write = "{ " + comment.replace("}","").trim() + " } ";
        this.writeToken(write);
        //this->forceMoveNumber = false;
    }

    private void printGameContent(GameNode g, boolean onMainLine) {

        Board b = g.getBoard();

        // first write mainline move, if there are variations
        int cntVar = g.getVariations().size();
        if(cntVar > 0) {
            if(onMainLine) {
                this.writeToken("<b>");
            }
            GameNode mainVariation = g.getVariation(0);
            this.printMove(mainVariation);
            // write nags
            for(Integer ni : mainVariation.getNags()) {
                this.printNag(ni);
            }
            writeToken(" ");
            if(onMainLine) {
                this.writeToken("</b>");
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
            writeToken(" ");
            // finally print comments
            if(!var_i.getComment().isEmpty()) {
                this.printComment(var_i.getComment());
            }

            // recursive call for all children
            this.printGameContent(var_i, false);

            // print variation end
            this.endVariation();
        }

        // finally do the mainline
        if(cntVar > 0) {
            GameNode mainVariation = g.getVariation(0);
            this.printGameContent(mainVariation, onMainLine);
        }
    }

    /**
     * render a html representation of the supplied game
     * @param g the game for which a html representation is desired
     * @return html formatted string
     */
    public String printGame(Game g) {

        this.reset();

        //this.printHeaders(g);

        //this.writeLine("");
        //this.writeLine("<html><body>");
        GameNode root = g.getRootNode();

        // special case if the root node has
        // a comment before the actual game starts
        if(!root.getComment().isEmpty()) {
            this.printComment(root.getComment());
        }

        this.printGameContent(root, true);
        this.printResult(g.getResult());
        //this.html.append(this.currentLine.toString());
        //this.writeLine("</body></html>");
        this.writeLine("");

        return this.html.toString();
    }


}