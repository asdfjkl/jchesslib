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
 * Represents chess moves.
 */
public class Move {

    int from;
    int to;
    int promotionPiece;
    boolean isNullMove;

    /**
     * create a new move. Source and destination must
     * be in the internal format (absolute offset in the 120=12x10
     * mailbox format)
     * @param from source square
     * @param to destination square
     */
    public Move(int from, int to) {

        this.from = from;
        this.to = to;
        this.promotionPiece = 0;
        this.isNullMove = false;

    }

    /**
     * create a new Move. Source square is given as (x,y) with
     * column = x, and row = y. Same for destination square.
     * Counting starts from zero, i.e. 0=a,...,7=h, and
     * 0=row 1,...,7=row 8
     * @param fromColumn source column
     * @param fromRow source row
     * @param toColumn target column
     * @param toRow target row
     */
    public Move(int fromColumn, int fromRow, int toColumn, int toRow) {

        this.from = ((fromRow + 2) * 10) + (fromColumn + 1);
        this.to = ((toRow + 2) * 10) + (toColumn + 1);
        this.promotionPiece = 0;
        this.isNullMove = false;

    }

    /**
     * create a null move
     */
    public Move() {

        this.from = 0;
        this.to = 0;
        this.promotionPiece = 0;
        this.isNullMove = true;

    }

    /**
     * create a new move that promotes. Source and destination must
     * be in the internal format (absolute offset in the 120=12x10
     * mailbox format).
     * @param from source square
     * @param to destination square
     * @param promotionPiece piece must be one of e.g. {@code CONSTANTS.KNIGHT}, i.e.
     *                       no color encoding, just the piece type
     */
    public Move(int from, int to, int promotionPiece) {

        this.from = from;
        this.to = to;
        this.promotionPiece = promotionPiece;

    }

    /**
     * reate a new move that promotes. Source square is given as (x,y) with
     *      * column = x, and row = y. Same for destination square.
     *      * Counting starts from zero, i.e. 0=a,...,7=h, and
     *      * 0=row 1,...,7=row 8.
     * @param fromColumn source column
     * @param fromRow source row
     * @param toColumn target column
     * @param toRow target row
     * @param promotionPiece must be one of 'N', 'B', 'R', or 'Q'
     */
    public Move(int fromColumn, int fromRow, int toColumn, int toRow, char promotionPiece) {

        this.from = ((fromRow + 2) * 10) + (fromColumn + 1);
        this.to = ((toRow + 2) * 10) + (toColumn + 1);

        this.promotionPiece = -1;
        if (promotionPiece == 'N') {
            this.promotionPiece = CONSTANTS.KNIGHT;
        }
        if (promotionPiece == 'B') {
            this.promotionPiece = CONSTANTS.BISHOP;
        }
        if (promotionPiece == 'R') {
            this.promotionPiece = CONSTANTS.ROOK;
        }
        if (promotionPiece == 'Q') {
            this.promotionPiece = CONSTANTS.QUEEN;
        }
        if (this.promotionPiece < 0) {
            throw new IllegalArgumentException("Illegal Promotion Piece: " + promotionPiece);
        }
        this.isNullMove = false;

    }

    /**
     * set the promotion piece
     * @param promotionPiece piece must be one of e.g. {@code CONSTANTS.KNIGHT}, i.e.
     *                       no color encoding, just the piece type
     */
    public void setPromotionPiece(int promotionPiece) {
        this.promotionPiece = promotionPiece;
    }

    /**
     * get the source square (absolute 120=12x10 mailbox coordinate)
     * @return
     */
    public int getMoveSourceSquare() {
        return from;
    }

    /**
     * get the target square (absolute 120=12x10 mailbox coordinate)
     * @return
     */
    public int getMoveTargetSquare() {
        return to;
    }

    private int alphaToPos(char alpha) {
        if (alpha == 'A') {
            return 1;
        } else if (alpha == 'B') {
            return 2;
        } else if (alpha == 'C') {
            return 3;
        } else if (alpha == 'D') {
            return 4;
        } else if (alpha == 'E') {
            return 5;
        } else if (alpha == 'F') {
            return 6;
        } else if (alpha == 'G') {
            return 7;
        } else if (alpha == 'H') {
            return 8;
        }
        throw new IllegalArgumentException("alpha to pos called with: " + alpha);
    }


    /**
     * create a new move from a uci string
     * @param uci
     */
    public Move(String uci) {

        if (!(uci.length() == 4 || uci.length() == 5)) {
            throw new IllegalArgumentException("Illegal Uci String: " + uci);
        }
        String uciUpper = uci.toUpperCase();

        int fromColumn = this.alphaToPos(uciUpper.charAt(0));
        // -49 is for ascii(1) -> int 0, * 10 + 20 is to get internal board coordinate
        int fromRow = ((((int) uciUpper.charAt(1)) - 49) * 10) + 20;
        this.from = fromRow + fromColumn;

        int toColumn = this.alphaToPos(uciUpper.charAt(2));
        int toRow = ((((int) uciUpper.charAt(3)) - 49) * 10) + 20;
        this.to = toRow + toColumn;

        if (uciUpper.length() == 5) {
            char promPiece = uciUpper.charAt(4);
            this.promotionPiece = -1;
            if (promPiece == 'N') {
                this.promotionPiece = CONSTANTS.KNIGHT;
            }
            if (promPiece == 'B') {
                this.promotionPiece = CONSTANTS.BISHOP;
            }
            if (promPiece == 'R') {
                this.promotionPiece = CONSTANTS.ROOK;
            }
            if (promPiece == 'Q') {
                this.promotionPiece = CONSTANTS.QUEEN;
            }
            if (promotionPiece < 0) {
                throw new IllegalArgumentException("illegal uci string: " + uci);
            }
        }
        this.isNullMove = false;

    }

    /**
     * return a uci string of the current move
     * @return
     */
    public String getUci() {

        if (this.isNullMove) {
            return "0000";
        } else {
            char colFrom = (char) ((this.from % 10) + 96);
            char rowFrom = (char) ((this.from / 10) + 47);

            char colTo = (char) ((this.to % 10) + 96);
            char rowTo = (char) ((this.to / 10) + 47);

            String uci = "";
            uci += colFrom;
            uci += rowFrom;
            uci += colTo;
            uci += rowTo;
            if (this.promotionPiece == CONSTANTS.KNIGHT) {
                uci += "N";
            }
            if (this.promotionPiece == CONSTANTS.ROOK) {
                uci += "R";
            }
            if (this.promotionPiece == CONSTANTS.QUEEN) {
                uci += "Q";
            }
            if (this.promotionPiece == CONSTANTS.BISHOP) {
                uci += "B";
            }
            return uci;
        }
    }

    /**
     * return a uci string of the current move
     */
    @Override
    public String toString() {
        return this.getUci();
    }


}