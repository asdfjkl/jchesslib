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

/**
 * Stockfish 14.1, PerfT 6:    508 ms (incl. printout!)
 * jchesslib,      PerfT 6:  17998 ms
 * Python-Chess,   PerfT 6: 249846 ms
 */

package io.github.asdfjkl.jchesslib;

import java.awt.Point;
import java.util.ArrayList;
import java.lang.Math;

/**
 * This class represents a current position including all
 * associated parameters (i.e. whose side it is to move,
 * current en passant file, castling rights etc.)
 * It also implements methods for Move generation.
 */
public class Board {

    /**
     * Stores the current turn. Either White (false) or Black (true)
     * @see io.github.asdfjkl.jchesslib.CONSTANTS
     */
    public int turn;

    /**
     * Stores the number of halvmoves since the last capture
     * or pawn move
     */
    public int halfmoveClock;

    /**
     * Stores the absolute move number.
     */
    public int fullmoveNumber;

    private int[] board;
    private int[] oldBoard;
    // dim [2][7][10]
    // first dim.: WHITE / BLACK
    // second dim: CONSTANTS.PAWN, ..KING, QUEEN...
    // third dim.: idx of board or EMPTY
    private int[][][] pieceList;

    private long zobristHash;
    private long positionHash;

    private boolean undoAvailable;
    private boolean zobristInitialized;
    private boolean posHashInitialized;
    private boolean lastMoveWasNull;

    private boolean castleWkingOk;
    private boolean castleWqueenOk;
    private boolean castleBkingOk;
    private boolean castleBqueenOk;

    private boolean prevCastleWkingOk;
    private boolean prevCastleWqueenOk;
    private boolean prevCastleBkingOk;
    private boolean prevCastleBqueenOk;

    private int enPassentTarget;
    private int prevEnPassentTarget;

    private int prevHalfmoveClock;

    /**
     * for a given char of a file 'A', ... 'H'
     * returns the corresponding integer * value 0 ... 7
     * @param alpha char of 'A' ... 'H'
     * @return number between 0...7
     */
    public static int alphaToPos(char alpha) {
        if (alpha == 'A') {
            return 0;
        } else if (alpha == 'B') {
            return 1;
        } else if (alpha == 'C') {
            return 2;
        } else if (alpha == 'D') {
            return 3;
        } else if (alpha == 'E') {
            return 4;
        } else if (alpha == 'F') {
            return 5;
        } else if (alpha == 'G') {
            return 6;
        } else if (alpha == 'H') {
            return 7;
        }
        throw new IllegalArgumentException("alpha to pos called with: " + alpha);
    }

    /**
     * The board uses an internal 12 x 10 mailbox representation
     * with absolute index coordinates 0 ... 119. This converts
     * an index coordinate from 0 ... 119 to a point (x,y) where
     * x in 0 ... 7 represents the file and y in 0 ... 7 the rank
     * of the internal coordinate
     * @param internalCoordinate  the internal coordinate of a square
     * @return Point(x,y) of the board square
     */
    public static Point internalToXY(int internalCoordinate) {
        if(internalCoordinate < 21 || internalCoordinate > 98) {
            throw new IllegalArgumentException("internalToXY; arg out of range: "+internalCoordinate);
        }
        int col = internalCoordinate % 10 - 1;
        int row = (internalCoordinate / 10) - 2;
        return new Point(col, row);
    }

    /**
     * Converts an x, y board index to the internal mailbox board index
     * @param x 0 ... 7, denoting the file
     * @param y 0 ... 7, denoting the rank
     * @return internal index of the square referenced by (x,y)
     */
    public static int xyToInternal(int x, int y) {
        if(x < 0 || x > 7 || y < 0 || y > 7) {
            throw new IllegalArgumentException("xyToInternal; arg out of range: "+ x + " " + y);
        } else {
            return (x+1) + ((y*10)+20);
        }
    }

    /**
     * creates a new empty Board
     */
    public Board() {
        this(false);
    }

    /**
     * creates a new Board. If startingPosition is true,
     * the board will be set up with pieces in initial position,
     * otherwise the board will be empty
     * @param startingPosition set to true for the initial position
     */
    public Board(boolean startingPosition) {

        if(startingPosition) {
            this.board = new int[120];
            this.oldBoard = new int[120];
            this.pieceList = new int[2][7][10];

            this.turn = CONSTANTS.WHITE;
            for (int i = 0; i < 120; i++) {
                this.board[i] = CONSTANTS.INIT_POS[i];
                this.oldBoard[i] = CONSTANTS.FRINGE;;
            }
            this.initPieceList();
            this.castleWkingOk = true;
            this.castleWqueenOk = true;
            this.castleBkingOk = true;
            this.castleBqueenOk = true;
            this.prevCastleWkingOk = false;
            this.prevCastleWqueenOk = false;
            this.prevCastleBkingOk = false;
            this.prevCastleBqueenOk = false;
            this.enPassentTarget = 0;
            this.halfmoveClock = 0;
            this.fullmoveNumber = 1;
            this.undoAvailable = false;
            this.lastMoveWasNull = false;
            this.prevHalfmoveClock = 0;
            this.zobristInitialized = false;
            this.posHashInitialized = false;
        } else {  // initialize empty board
            this.board = new int[120];
            this.oldBoard = new int[120];
            this.pieceList = new int[2][7][10];

            this.turn = CONSTANTS.WHITE;
            for(int i=0;i<120;i++) {
                this.board[i] = CONSTANTS.EMPTY_POS[i];
                this.oldBoard[i] = CONSTANTS.FRINGE;
            }
            this.initPieceList();
            this.castleWkingOk = false;
            this.castleWqueenOk = false;
            this.castleBkingOk = false;
            this.castleBqueenOk = false;
            this.prevCastleWkingOk = false;
            this.prevCastleWqueenOk = false;
            this.prevCastleBkingOk = false;
            this.prevCastleBqueenOk = false;
            this.enPassentTarget = 0;
            this.halfmoveClock = 0;
            this.fullmoveNumber = 1;
            this.undoAvailable = false;
            this.lastMoveWasNull = false;
            this.prevHalfmoveClock = 0;
            this.zobristInitialized = false;
            this.posHashInitialized = false;
        }
    }

    /**
     * creates a new Board. The board position is set up according
     * to the supplied FEN string
     * @param fen string in Forsyth–Edwards Notation
     */
    public Board(String fen) {

        this.board = new int[120];
        this.oldBoard = new int[120];
        this.pieceList = new int[2][7][10];

        for(int i=0;i<120;i++) {
            this.board[i] = CONSTANTS.EMPTY_POS[i];
            this.oldBoard[i] = CONSTANTS.FRINGE;
        }

        // check that we have six parts in fen, each separated by space
        // if last two parts are missing (fullmove no. + halfmove clock)
        // try to still parse the game
        String[] fenParts = fen.split(" ");
        if(fenParts.length < 4) {
            throw new IllegalArgumentException("fen: parts missing in "+fen);
        }
        // check that the first part consists of 8 rows, each sep. by /
        String[] rows = fenParts[0].split("/");
        if(rows.length != 8) {
            throw new IllegalArgumentException("fen: not 8 rows in 0th part in "+fen);
        }
        // check that in each row, there are no two consecutive digits
        for(int i=0;i<rows.length;i++) {
            String row = rows[i];
            int field_sum = 0;
            boolean previous_was_digit = false;
            for(int j=0;j<row.length();j++) {
                char rj = row.charAt(j);
                char rjl = Character.toLowerCase(rj);
                if(rj == '1' || rj == '2' || rj == '3' || rj == '4' ||
                        rj == '5' || rj == '6' || rj == '7' || rj == '8')
                {
                    if(previous_was_digit) {
                        throw new IllegalArgumentException("fen: two consecutive digits in rows in " + fen);
                    } else {
                        field_sum += Character.getNumericValue(rj);
                        previous_was_digit = true;
                    }
                } else if(rjl == 'p' || rjl == 'n' || rjl == 'b' || rjl == 'r' ||
                        rjl == 'q' || rjl == 'k')
                {
                    field_sum += 1;
                    previous_was_digit = false;
                } else {
                    throw new IllegalArgumentException("fen: two consecutive chars in rows in " + fen);
                }
            }
            // validate that there are 8 alphanums in each row
            if(field_sum != 8) {
                throw new IllegalArgumentException("fen: field sum is not 8 in "+fen);
            }
        }
        // check that turn part is valid
        if(!(fenParts[1].equals("w") || fenParts[1].equals("b"))) {
            throw new IllegalArgumentException("fen: turn part is invalid in "+fen);
        }
        // check that castles part in correctly encoded using regex
        boolean castlesMatch = fenParts[2].matches("^-|[KQABCDEFGH]{0,2}[kqabcdefgh]{0,2}$");
        if(!castlesMatch) {
            throw new IllegalArgumentException("fen: castles encoding is invalid in "+fen);
        }
        // check correct encoding of en passant squares
        if(!fenParts[3].equals("-")) {
            if(fenParts[1].equals("w")) {
                // should be something like "e6" etc. if white is to move
                // check that int value part is sixth rank
                if(fenParts[3].length() != 2 || fenParts[3].charAt(1) != '6') {
                    throw new IllegalArgumentException("fen: invalid e.p. encoding (white to move) in " + fen);
                }
            } else {
                if(fenParts[3].length() != 2 || fenParts[3].charAt(1) != '3') {
                    throw new IllegalArgumentException("fen: invalid e.p. encoding (black to move) in "+fen);
                }
            }
        }
        // half-move counter validity (if half-move is present)
        if((fenParts.length >= 5) && Integer.parseInt(fenParts[4]) < 0) {
            throw new IllegalArgumentException("fen: negative half move clock or not a number in "+fen);
        }
        // full move number validity (if full move number is present)
        if((fenParts.length >= 6) && Integer.parseInt(fenParts[5]) < 0) {
            throw new IllegalArgumentException("fen: fullmove number not positive");
        }
        // set pieces
        for(int i=0;i<rows.length;i++) {
            int square_index = 91 - (i*10);
            String row = rows[i];
            for(int j=0;j<row.length();j++) {
                char rj = row.charAt(j);
                char rjl = Character.toLowerCase(rj);
                if(rj == '1' || rj == '2' || rj == '3' || rj == '4' || rj == '5' ||
                        rj == '6' || rj == '7' || rj == '8')
                {
                    square_index += Character.getNumericValue(rj);
                } else if(rjl == 'p' || rjl == 'n' || rjl == 'b' ||
                        rjl == 'r' || rjl == 'q' || rjl == 'k')
                {
                    int piece = this.pieceFromSymbol(rj);
                    this.board[square_index] = piece;
                    square_index += 1;
                }
            }
        }
        // set turn
        if(fenParts[1].equals("w")) {
            this.turn = CONSTANTS.WHITE;
        }
        if(fenParts[1].equals("b")) {
            this.turn = CONSTANTS.BLACK;
        }
        this.castleWkingOk = false;
        this.castleWqueenOk = false;
        this.castleBkingOk = false;
        this.castleBqueenOk = false;
        for(int i=0;i<fenParts[2].length();i++) {
            char ci = fenParts[2].charAt(i);
            if(ci == 'K') {
                this.castleWkingOk = true;
            }
            if(ci == 'Q') {
                this.castleWqueenOk = true;
            }
            if(ci == 'k') {
                this.castleBkingOk = true;
            }
            if(ci == 'q') {
                this.castleBqueenOk = true;
            }
        }
        // set en passant square
        if(fenParts[3].equals("-")) {
            this.enPassentTarget = 0;
        } else {
            int row = 10 + Character.getNumericValue(fenParts[3].charAt(1)) * 10;
            int col = 0;
            char c = Character.toLowerCase(fenParts[3].charAt(0));
            if(c == 'a') {
                col = 1;
            }
            if(c == 'b') {
                col = 2;
            }
            if(c == 'c') {
                col = 3;
            }
            if(c == 'd') {
                col = 4;
            }
            if(c == 'e') {
                col = 5;
            }
            if(c == 'f') {
                col = 6;
            }
            if(c == 'g') {
                col = 7;
            }
            if(c == 'h') {
                col = 8;
            }
            this.enPassentTarget = row + col;
        }
        if(fenParts.length >= 5) {
            this.halfmoveClock = Integer.parseInt(fenParts[4]);
        } else {
            this.halfmoveClock = 0;
        }
        if(fenParts.length >= 6) {
            int fullMoveNumber = Integer.parseInt(fenParts[5]);
            if(fullMoveNumber > 0) {
                this.fullmoveNumber = fullMoveNumber;
            } else {
                this.fullmoveNumber = 1;
            }
        } else {
            this.fullmoveNumber = 1;
        }
        this.undoAvailable = false;
        this.lastMoveWasNull = false;
        if(!this.isConsistent()) {
            throw new IllegalArgumentException("fen: board position from supplied fen is inconsistent in "+fen);
        }
        this.initPieceList();

        this.zobristInitialized = false;
        this.posHashInitialized = false;

    }

    public int negColor(int color) {
        if(color == CONSTANTS.WHITE) {
            return CONSTANTS.BLACK;
        } else {
            return CONSTANTS.WHITE;
        }
    }

    /**
     * resets the Board to the initial position
     */
    public void resetToStartingPosition() {

        this.board = new int[120];
        this.oldBoard = new int[120];
        this.pieceList = new int[2][7][10];

        this.turn = CONSTANTS.WHITE;
        for(int i=0;i<120;i++) {
            this.board[i] = CONSTANTS.INIT_POS[i];
            this.oldBoard[i] = CONSTANTS.FRINGE;
        }
        this.initPieceList();
        this.castleWkingOk = true;
        this.castleWqueenOk = true;
        this.castleBkingOk = true;
        this.castleBqueenOk = true;
        this.prevCastleWkingOk = false;
        this.prevCastleWqueenOk = false;
        this.prevCastleBkingOk = false;
        this.prevCastleBqueenOk = false;
        this.enPassentTarget = 0;
        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;
        this.undoAvailable = false;
        this.lastMoveWasNull = false;
        this.prevHalfmoveClock = 0;
        this.zobristInitialized = false;
        this.posHashInitialized = false;
    }

    /**
     * clears the Board (i.e. removes all pieces)
     * the turn is set to "white to move"
     */
    public void clear() {
        this.board = new int[120];
        this.oldBoard = new int[120];
        this.pieceList = new int[2][7][10];
        this.turn = CONSTANTS.WHITE;
        for(int i=0;i<120;i++) {
            this.board[i] = CONSTANTS.EMPTY_POS[i];
            this.oldBoard[i] = CONSTANTS.FRINGE;
        }
        this.initPieceList();
        this.castleWkingOk = false;
        this.castleWqueenOk = false;
        this.castleBkingOk = false;
        this.castleBqueenOk = false;
        this.prevCastleWkingOk = false;
        this.prevCastleWqueenOk = false;
        this.prevCastleBkingOk = false;
        this.prevCastleBqueenOk = false;
        this.enPassentTarget = 0;
        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;
        this.undoAvailable = false;
        this.lastMoveWasNull = false;
        this.prevHalfmoveClock = 0;
        this.zobristInitialized = false;
        this.posHashInitialized = false;
    }

    /**
     * creates a deep copy of the current Board
     * any modifications of the copy will have no
     * effect on the original one
     * @return copy of the current Board
     */
    public Board makeCopy() {

        Board b = new Board();

        for(int i=0;i<120;i++) {
            b.board[i] = this.board[i];
            b.oldBoard[i] = this.oldBoard[i];
        }
        for(int i=0;i<2;i++) {
            for(int j=0;j<7;j++) {
                for(int k=0;k<10;k++) {
                    b.pieceList[i][j][k] = this.pieceList[i][j][k];
                }
            }
        }

        b.turn = this.turn;
        b.halfmoveClock = this.halfmoveClock;
        b.fullmoveNumber = this.fullmoveNumber;

        b.zobristHash = this.zobristHash;
        b.positionHash = this.positionHash;

        b.undoAvailable = this.undoAvailable;
        b.zobristInitialized = this.zobristInitialized;
        b.posHashInitialized = this.posHashInitialized;
        b.lastMoveWasNull = this.lastMoveWasNull;

        b.castleWkingOk = this.castleWkingOk;
        b.castleWqueenOk = this.castleWqueenOk;
        b.castleBkingOk = this.castleBkingOk;
        b.castleBqueenOk = this.castleBqueenOk;

        b.prevCastleWkingOk = this.prevCastleWkingOk;
        b.prevCastleWqueenOk = this.prevCastleWqueenOk;
        b.prevCastleBkingOk = this.prevCastleBkingOk;
        b.prevCastleBqueenOk = this.prevCastleBqueenOk;

        b.enPassentTarget = this.enPassentTarget;
        b.prevEnPassentTarget = this.prevEnPassentTarget;

        b.prevHalfmoveClock = this.prevHalfmoveClock;

        return b;

    }

    // Copies b into this current Board instance, without making a new one.
    // (equivalent to assignment operator = , in C++)

    /**
     * copies the supplied Board into this current instance without
     * creating a new Board. Any future modification of the supplied
     * Board or the current one will have no side-effect on the other one
     * @param b the source Board
     */
    public void copy(Board b) {
        for(int i=0;i<120;i++) {
            this.board[i] = b.board[i];
            this.oldBoard[i] = b.oldBoard[i];
        }
        for(int i=0;i<2;i++) {
            for(int j=0;j<7;j++) {
                for(int k=0;k<10;k++) {
                    this.pieceList[i][j][k] = b.pieceList[i][j][k];
                }
            }
        }

        this.turn = b.turn;
        this.halfmoveClock = b.halfmoveClock;
        this.fullmoveNumber = b.fullmoveNumber;

        this.zobristHash = b.zobristHash;
        this.positionHash = b.positionHash;

        this.undoAvailable = b.undoAvailable;
        this.zobristInitialized = b.zobristInitialized;
        this.posHashInitialized = b.posHashInitialized;
        this.lastMoveWasNull = b.lastMoveWasNull;

        this.castleWkingOk = b.castleWkingOk;
        this.castleWqueenOk = b.castleWqueenOk;
        this.castleBkingOk = b.castleBkingOk;
        this.castleBqueenOk = b.castleBqueenOk;

        this.prevCastleWkingOk = b.prevCastleWkingOk;
        this.prevCastleWqueenOk = b.prevCastleWqueenOk;
        this.prevCastleBkingOk = b.prevCastleBkingOk;
        this.prevCastleBqueenOk = b.prevCastleBqueenOk;

        this.enPassentTarget = b.enPassentTarget;
        this.prevEnPassentTarget = b.prevEnPassentTarget;

        this.prevHalfmoveClock = b.prevHalfmoveClock;
    }

    /**
     * creates a string encoding of the current Board in Forsyth–Edwards Notation
     * @return the FEN string
     */
    public String fen() {
        String fenString = "";
        // first build board
        for(int i=90;i>=20;i-=10) {
            int square_counter = 0;
            for(int j=1;j<9;j++) {
                if(this.board[i+j] != CONSTANTS.EMPTY) {
                    int piece = this.board[i+j];
                    fenString += this.pieceToSymbol(piece);
                    square_counter = 0;
                } else {
                    square_counter += 1;
                    if(j==8) {
                        char c = (char) (48 + square_counter);
                        fenString += c;
                    } else {
                        if(this.board[i+j+1] != CONSTANTS.EMPTY) {
                            char c = (char) (48 + square_counter);
                            fenString += c;
                        }
                    }
                }
            }
            if(i!=20) {
                fenString += '/';
            }
        }
        // write turn
        if(this.turn == CONSTANTS.WHITE) {
            fenString += " w";
        } else {
            fenString += " b";
        }
        // write castling rights
        if(this.canCastleWhiteKing() || this.canCastleWhiteQueen() || this.canCastleBlackKing() || this.canCastleBlackQueen()) {
            fenString += " ";
            if(this.canCastleWhiteKing()) {
                fenString += "K";
            }
            if(this.canCastleWhiteQueen()) {
                fenString += "Q";
            }
            if(this.canCastleBlackKing()) {
                fenString += "k";
            }
            if(this.canCastleBlackQueen()) {
                fenString += "q";
            }

        } else {
            fenString += " -";
        }
        // write ep target if exists
        if(this.enPassentTarget != 0) {
            fenString += " " + this.internalIdxToString(this.enPassentTarget);
        } else {
            fenString += " -";
        }
        // add halfmove clock and fullmove counter
        fenString += " " + this.halfmoveClock;
        fenString += " " + this.fullmoveNumber;

        return fenString;
    }

    private void removeFromPieceList(int color, int piece_type, int idx) {

        int j = -1;
        for(int i=0;i<10;i++) {
            if(this.pieceList[color][piece_type][i] == idx) {
                j = i;
                break;
            }
        }
        if(j>=0) {
            // move all other one step further
            for(int i=j+1;i<10;i++) {
                this.pieceList[color][piece_type][i-1] = this.pieceList[color][piece_type][i];
            }
            // empty last one in list
            this.pieceList[color][piece_type][9] = CONSTANTS.EMPTY;
        }
    }

    private void addToPieceList(int color, int piece_type, int idx) {

        for(int i=0;i<10;i++) {
            if(this.pieceList[color][piece_type][i] == CONSTANTS.EMPTY) {
                this.pieceList[color][piece_type][i] = idx;
                break;
            }
        }
    }


    // doesn't check legality

    /**
     * applies the supplied Move on the current board. The Board state
     * changes to the Board after the move is executed. This function
     * does not check if the supplied Move is legal in the current position.
     * @param m Move to apply
     */
    public void apply(Move m) {

        if(m.isNullMove) {
            this.turn = negColor(turn);
            this.prevEnPassentTarget = this.enPassentTarget;
            this.enPassentTarget = 0;
            this.lastMoveWasNull = true;
            this.undoAvailable = true;
            if(this.turn == CONSTANTS.WHITE) {
                this.fullmoveNumber++;
            }
        } else {
            this.lastMoveWasNull = false;
            this.turn = negColor(turn);
            this.prevEnPassentTarget = this.enPassentTarget;
            this.prevCastleWkingOk = this.castleWkingOk;
            this.prevCastleWqueenOk = this.castleWqueenOk;
            this.prevCastleBkingOk = this.castleBkingOk;
            this.prevCastleBqueenOk = this.castleBqueenOk;
            this.enPassentTarget = 0;
            if(this.turn == CONSTANTS.WHITE) {
                this.fullmoveNumber++;
            }
            for(int i=0;i<120;i++) {
                this.oldBoard[i] = this.board[i];
            }
            int oldPieceType = this.getPieceTypeAt(m.from);
            int color = this.getPieceColorAt(m.from);
            // if target field is not empty, remove from piece list
            // this must be of opposite color as the currently moving piece
            if(this.board[m.to] != CONSTANTS.EMPTY) {
                int currentTargetPiece = this.getPieceTypeAt(m.to);
                this.removeFromPieceList(negColor(color), currentTargetPiece, m.to);
            }
            // also remove the currently moving piece from the list
            this.removeFromPieceList(color, oldPieceType, m.from);
            // increase halfmove clock only if no capture or pawn advance
            // happened
            this.prevHalfmoveClock = this.halfmoveClock;
            if(oldPieceType == CONSTANTS.PAWN || this.board[m.to] != CONSTANTS.EMPTY) {
                this.halfmoveClock = 0;
            } else {
                this.halfmoveClock++;
            }
            // if we move a pawn two steps up, set the en_passant field
            if(oldPieceType == CONSTANTS.PAWN) {
                // white pawn moved two steps up
                if((m.to - m.from) == CONSTANTS.DIR_N2) {
                    this.enPassentTarget = m.from + CONSTANTS.DIR_N;
                }
                // black pawn moved two steps up (down)
                if((m.to - m.from == CONSTANTS.DIR_S2)) {
                    this.enPassentTarget = m.from + CONSTANTS.DIR_S;
                }
            }
            // if the move is an en-passant capture,
            // remove the (non-target) corresponding pawn
            // move is an en passant move, if
            // a) color is white, piece type is pawn, target
            // is up left or upright and empty
            // b) color is black, piece type is pawn, target
            // is down right or down left and empty
            // also set last_move_was_ep to true
            if(oldPieceType == CONSTANTS.PAWN) {
                if(this.board[m.to] == CONSTANTS.EMPTY) {
                    if(color == CONSTANTS.WHITE && ((m.to-m.from == CONSTANTS.DIR_NW) || (m.to-m.from) == CONSTANTS.DIR_NE)) {
                        // remove captured pawn
                        this.board[m.to + CONSTANTS.DIR_S] = CONSTANTS.EMPTY;
                        // also remove from piece list
                        this.removeFromPieceList(negColor(color), CONSTANTS.PAWN, m.to + CONSTANTS.DIR_S);
                    }
                    if(color == CONSTANTS.BLACK && ((m.from -m.to == CONSTANTS.DIR_NW) || (m.from - m.to) == CONSTANTS.DIR_NE)) {
                        // remove captured pawn
                        this.board[m.to + CONSTANTS.DIR_N] = CONSTANTS.EMPTY;
                        // also remove from piece list
                        this.removeFromPieceList(negColor(color), CONSTANTS.PAWN, m.to + CONSTANTS.DIR_N);
                    }
                }
            }
            // if the move is a promotion, the target
            // field becomes the promotion choice
            if(m.promotionPiece != CONSTANTS.EMPTY) {
                // true means black
                if(color == CONSTANTS.BLACK) {
                    // +128 sets 7th bit to true (means black)
                    this.board[m.to] = m.promotionPiece + 128;
                    // add to piece list
                    this.addToPieceList(CONSTANTS.BLACK, m.promotionPiece, m.to);
                }
                else {
                    this.board[m.to] = m.promotionPiece;
                    this.addToPieceList(CONSTANTS.WHITE, m.promotionPiece, m.to);
                }
            } else {
                // otherwise the target is the piece on the from field
                this.board[m.to] = this.board[m.from];
                this.addToPieceList(color, oldPieceType, m.to);
            }
            this.board[m.from] = CONSTANTS.EMPTY;
            // check if the move is castles, i.e. 0-0 or 0-0-0
            // then we also need to move the rook
            // white kingside
            if(oldPieceType == CONSTANTS.KING) {
                if(color==CONSTANTS.WHITE) {
                    if(m.from == CONSTANTS.E1 && m.to == CONSTANTS.G1) {
                        this.board[CONSTANTS.F1] = this.board[CONSTANTS.H1];
                        this.board[CONSTANTS.H1] = CONSTANTS.EMPTY;
                        this.setCastleWKing(false);
                        this.removeFromPieceList(CONSTANTS.WHITE, CONSTANTS.ROOK, CONSTANTS.H1);
                        this.addToPieceList(CONSTANTS.WHITE, CONSTANTS.ROOK, CONSTANTS.F1);
                    }
                    // white queenside
                    if(m.from == CONSTANTS.E1 && m.to == CONSTANTS.C1) {
                        this.board[CONSTANTS.D1] = this.board[CONSTANTS.A1];
                        this.board[CONSTANTS.A1] = CONSTANTS.EMPTY;
                        this.setCastleWQueen(false);
                        this.removeFromPieceList(CONSTANTS.WHITE, CONSTANTS.ROOK, CONSTANTS.A1);
                        this.addToPieceList(CONSTANTS.WHITE, CONSTANTS.ROOK, CONSTANTS.D1);
                    } }
                else if(color==CONSTANTS.BLACK) {
                    // black kingside
                    if(m.from == CONSTANTS.E8 && m.to == CONSTANTS.G8) {
                        this.board[CONSTANTS.F8] = this.board[CONSTANTS.H8];
                        this.board[CONSTANTS.H8] = CONSTANTS.EMPTY;
                        this.setCastleBKing(false);
                        this.removeFromPieceList(CONSTANTS.BLACK, CONSTANTS.ROOK, CONSTANTS.H8);
                        this.addToPieceList(CONSTANTS.BLACK, CONSTANTS.ROOK, CONSTANTS.F8);
                    }
                    // black queenside
                    if(m.from == CONSTANTS.E8 && m.to == CONSTANTS.C8) {
                        this.board[CONSTANTS.D8] = this.board[CONSTANTS.A8];
                        this.board[CONSTANTS.A8] = CONSTANTS.EMPTY;
                        this.setCastleBQueen(false);
                        this.removeFromPieceList(CONSTANTS.BLACK, CONSTANTS.ROOK, CONSTANTS.A8);
                        this.addToPieceList(CONSTANTS.BLACK, CONSTANTS.ROOK, CONSTANTS.D8);
                    }
                }
            }
            // check if someone loses castling rights
            // by moving king or by moving rook
            // or if one of the rooks is captured by the
            // opposite side
            if(color == CONSTANTS.WHITE) {
                if(oldPieceType == CONSTANTS.KING) {
                    if(m.from == CONSTANTS.E1 && m.to != CONSTANTS.G1) {
                        this.setCastleWKing(false);
                    }
                    if(m.from == CONSTANTS.E1 && m.to != CONSTANTS.C1) {
                        this.setCastleWQueen(false);
                    }
                }
                if(oldPieceType == CONSTANTS.ROOK) {
                    if(m.from == CONSTANTS.A1) {
                        this.setCastleWQueen(false);
                    }
                    if(m.from == CONSTANTS.H1) {
                        this.setCastleWKing(false);
                    }
                }
                // white moves a piece to H8 or A8
                // means either white captures rook
                // or black has moved rook prev.
                // [even though: in the latter case, should be already
                // done by check above in prev. moves]
                if(m.to == CONSTANTS.H8) {
                    this.setCastleBKing(false);
                }
                if(m.to == CONSTANTS.A8) {
                    this.setCastleBQueen(false);
                }
            }
            // same for black
            if(color == CONSTANTS.BLACK) {
                if(oldPieceType == CONSTANTS.KING) {
                    if(m.from == CONSTANTS.E8 && m.to != CONSTANTS.G8) {
                        this.setCastleBKing(false);
                    }
                    if(m.from == CONSTANTS.E8 && m.to != CONSTANTS.C8) {
                        this.setCastleBQueen(false);
                    }
                }
                if(oldPieceType == CONSTANTS.ROOK) {
                    if(m.from == CONSTANTS.A8) {
                        this.setCastleBQueen(false);
                    }
                    if(m.from == CONSTANTS.H8) {
                        this.setCastleBKing(false);
                    }
                }
                // black moves piece to A1 or H1
                if(m.to == CONSTANTS.H1) {
                    this.setCastleWKing(false);
                }
                if(m.to == CONSTANTS.A1) {
                    this.setCastleWQueen(false);
                }
            }
            // after move is applied, can revert to the previous position
            this.undoAvailable = true;
        }
    }

    /**
     * Undoes the last applied Move and resets the Board to the state
     * prior applying the last Move. Undo is only available once, i.e.
     * a sequence apply() - undo() - apply() - undo() is possible, but
     * apply() - apply() - undo() - undo() will throw an IllegalArgumentException
     * Some other functions, like move generation destroy the ability to undo a move.
     * Always check isUndoAvailable() before calling undo()!
     */
    public void undo() {
        if(!this.undoAvailable) {
            throw new IllegalArgumentException("must call board.apply(move) each time before calling undo() ");
        } else {
            if(this.lastMoveWasNull) {
                this.turn = negColor(this.turn);
                this.enPassentTarget = this.prevEnPassentTarget;
                this.prevEnPassentTarget = 0;
                this.lastMoveWasNull = false;
                this.undoAvailable = true;
            } else {
                for(int i=0;i<120;i++) {
                    this.board[i] = this.oldBoard[i];
                }
                this.undoAvailable = false;
                this.enPassentTarget = this.prevEnPassentTarget;
                this.prevEnPassentTarget = 0;
                this.castleWkingOk = this.prevCastleWkingOk;
                this.castleWqueenOk = this.prevCastleWqueenOk;
                this.castleBkingOk = this.prevCastleBkingOk;
                this.castleBqueenOk = this.prevCastleBqueenOk;
                this.turn = negColor(this.turn);
                this.halfmoveClock = this.prevHalfmoveClock;
                this.prevHalfmoveClock = 0;
                if(this.turn == CONSTANTS.BLACK) {
                    this.fullmoveNumber--;
                }
            }
        }
        this.initPieceList();
    }

    private String internalIdxToString(int idx) {
        if(idx<21 || idx>98) {
            throw new IllegalArgumentException("called idx_to_str but idx is in fringe: "+idx);
        } else {
            char row = (char) ((idx / 10) + 47);
            char col = (char) ((idx % 10) + 96);
            String s = "";
            s += col;
            s += row;
            return s;
        }
    }

    //private boolean isOffside(int internalCoordinate) {
    //    return (this.board[internalCoordinate] == 0xFF);
    //}

    //private boolean isEmpty(int internalCoordinate) {
    //    return (this.board[internalCoordinate] == 0);
    //}

    /**
     * computes all pseudo-legal moves in the current position     *
     * @return ArrayList containing pseudo-legal Moves
     */
    public ArrayList<Move> pseudoLegalMoves() {
        return this.pseudoLegalMoves(CONSTANTS.ANY_SQUARE, CONSTANTS.ANY_SQUARE, CONSTANTS.ANY_PIECE,true, this.turn);
    }

    /**
     * computes a subset of all pseudo-legal moves in the current position.
     * The subset is can be defined by providing source and target squares,
     * the desired piece type, whether castling moves should be considered,
     * and the color (i.e. whether we consider Black's or White's moves)
     * @param internalFromSquare internal coordinate denoting source square, or CONSTANTS.ANY_SQUARE
     * @param internalToSquare internal coordinate denoting target square, or CONSTANTS.ANY_SQUARE
     * @param pieceType piece type (i.e. King, Queen, Rook, Bishop, Knight, Pawn) or CONSTANTS.ANY_PIECE
     * @param genCastleMoves true if castling moves shoudl be included
     * @param color one of CONSTANTS.WHITE or CONSTANTS.BLACK
     * @return ArrayList containing pseudo-legal Moves
     */
    public ArrayList<Move> pseudoLegalMoves(int internalFromSquare, int internalToSquare, int pieceType,
                                            boolean genCastleMoves, int color) {

        ArrayList<Move> moves = new ArrayList<Move>();
        // pawn moves
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.PAWN) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[color][CONSTANTS.PAWN][i];
                if(from == CONSTANTS.EMPTY) { // we reached the end of the piece list
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                //assert(this->board[from] != 0xff);
                int piece_idx = CONSTANTS.IDX_WPAWN;
                if(color == CONSTANTS.BLACK) {
                    piece_idx = CONSTANTS.IDX_BPAWN;
                }
                // take up right, or up left
                for(int j=3;j<=4;j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[piece_idx][j];
                    if((internalToSquare == CONSTANTS.ANY_SQUARE || idx == internalToSquare) && this.board[idx] != CONSTANTS.FRINGE) {
                        if(( this.board[idx] != 0 && color==CONSTANTS.BLACK && (getPieceColorAt(idx) == CONSTANTS.WHITE)) ||
                                ( this.board[idx] != 0 && color==CONSTANTS.WHITE && (getPieceColorAt(idx) == CONSTANTS.BLACK)))
                        {
                            // if it's a promotion square, add four moves
                            if((color==CONSTANTS.WHITE && (idx / 10 == 9)) || (color==CONSTANTS.BLACK && (idx / 10 == 2)))
                            {
                                //assert(this->board[from] != 0xff);
                                moves.add(new Move(from,idx,CONSTANTS.QUEEN));
                                moves.add(new Move(from,idx,CONSTANTS.ROOK));
                                moves.add(new Move(from,idx,CONSTANTS.BISHOP));
                                moves.add(new Move(from,idx,CONSTANTS.KNIGHT));
                            } else {
                                //assert(this->board[from] != 0xff);
                                moves.add(new Move(from,idx));
                            }
                        }
                    }
                }
                // move one (j=1) or two (j=2) up (or down in the case of black)
                int idx_1up = from + CONSTANTS.DIR_TABLE[piece_idx][1];
                int idx_2up = from + CONSTANTS.DIR_TABLE[piece_idx][2];
                if((internalToSquare == CONSTANTS.ANY_SQUARE || idx_2up == internalToSquare) && this.board[idx_2up] != CONSTANTS.FRINGE) {
                    if((color == CONSTANTS.WHITE && (from/10==3)) || (color==CONSTANTS.BLACK && (from/10==8))) {
                        // means we have a white/black pawn in initial position, direct square
                        // in front is empty => allow to move two forward
                        if( this.board[idx_1up] == 0 && this.board[idx_2up] == 0) {
                            //assert(this->board[from] != 0xff);
                            moves.add(new Move(from,idx_2up));
                        }
                    }
                }
                if((internalToSquare == CONSTANTS.ANY_SQUARE || idx_1up == internalToSquare)
                        && this.board[idx_1up] == 0) {
                    // if it's a promotion square, add four moves
                    if((color==CONSTANTS.WHITE && (idx_1up / 10 == 9)) || (color==CONSTANTS.BLACK && (idx_1up / 10 == 2))) {
                        //assert(this->board[from] != 0xff);
                        moves.add(new Move(from,idx_1up,CONSTANTS.QUEEN));
                        moves.add(new Move(from,idx_1up,CONSTANTS.ROOK));
                        moves.add(new Move(from,idx_1up,CONSTANTS.BISHOP));
                        moves.add(new Move(from,idx_1up,CONSTANTS.KNIGHT));
                    } else {
                        //assert(this->board[from] != 0xff);
                        moves.add(new Move(from,idx_1up));
                    }
                }
                // finally, potential en-passant capture is handled
                // left up
                if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == this.enPassentTarget) {
                    if (color == CONSTANTS.WHITE && (this.enPassentTarget - from) == CONSTANTS.DIR_NW) {
                        //assert(this.board[from] != 0xff);
                        Move m = new Move(from, this.enPassentTarget);
                        moves.add(m);
                    }
                    // right up
                    if (color == CONSTANTS.WHITE && (this.enPassentTarget - from) == CONSTANTS.DIR_NE) {
                        //assert(this->board[from] != 0xff);
                        Move m = new Move(from, this.enPassentTarget);
                        moves.add(m);
                    }
                    // left down
                    if (color == CONSTANTS.BLACK && (this.enPassentTarget - from) == CONSTANTS.DIR_SE) {
                        //assert(this->board[from] != 0xff);
                        Move m = new Move(from, this.enPassentTarget);
                        moves.add(m);
                    }
                    if (color == CONSTANTS.BLACK && (this.enPassentTarget - from) == CONSTANTS.DIR_SW) {
                        //assert(this->board[from] != 0xff);
                        Move m = new Move(from, this.enPassentTarget);
                        moves.add(m);
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.KNIGHT) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[color][CONSTANTS.KNIGHT][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                //assert(this->board[from] != 0xff);
                int lookup_idx = CONSTANTS.IDX_KNIGHT;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0];j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    if((internalToSquare == CONSTANTS.ANY_SQUARE || idx == internalToSquare) && this.board[idx] != CONSTANTS.FRINGE) {
                        if(  this.board[idx] == 0 || (this.getPieceColorAt(idx) != color)) {
                            moves.add(new Move(from,idx));
                        }
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.KING) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[color][CONSTANTS.KING][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                //assert(this->board[from] != 0xff);
                int lookup_idx = CONSTANTS.IDX_KING;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0];j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    if((internalToSquare == CONSTANTS.ANY_SQUARE || idx == internalToSquare) && this.board[idx] != CONSTANTS.FRINGE) {
                        if( this.board[idx] == 0 || (this.getPieceColorAt(idx) != color)) {
                            moves.add(new Move(from,idx));
                        }
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.ROOK) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[color][CONSTANTS.ROOK][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                //assert(this->board[from] != 0xff);
                int lookup_idx = CONSTANTS.IDX_ROOK;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0] ;j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    boolean stop = false;
                    while(!stop) {
                        if(this.board[idx] != CONSTANTS.FRINGE) {
                            if(this.board[idx]==0) {
                                if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                    moves.add(new Move(from,idx));
                                }
                            } else {
                                stop = true;
                                if(this.getPieceColorAt(idx) != color) {
                                    if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                        moves.add(new Move(from,idx));
                                    }
                                }
                            }
                            idx = idx + CONSTANTS.DIR_TABLE[lookup_idx][j];
                        } else {
                            stop = true;
                        }
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.BISHOP) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[color][CONSTANTS.BISHOP][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                int lookup_idx = CONSTANTS.IDX_BISHOP;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0] ;j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    boolean stop = false;
                    while(!stop) {
                        if(this.board[idx]!=CONSTANTS.FRINGE) {
                            if(this.board[idx] == 0) {
                                if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                    moves.add(new Move(from,idx));
                                }
                            } else {
                                stop = true;
                                if(this.getPieceColorAt(idx) != color) {
                                    if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                        moves.add(new Move(from,idx));
                                    }
                                }
                            }
                            idx = idx + CONSTANTS.DIR_TABLE[lookup_idx][j];
                        } else {
                            stop = true;
                        }
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.QUEEN) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[color][CONSTANTS.QUEEN][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                int lookup_idx = CONSTANTS.IDX_QUEEN;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0] ;j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    boolean stop = false;
                    while(!stop) {
                        if(this.board[idx]!=CONSTANTS.FRINGE) {
                            if(this.board[idx] == 0) {
                                if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                    moves.add(new Move(from,idx));
                                }
                            } else {
                                stop = true;
                                if(this.getPieceColorAt(idx) != color) {
                                    if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                        moves.add(new Move(from,idx));
                                    }
                                }
                            }
                            idx = idx + CONSTANTS.DIR_TABLE[lookup_idx][j];
                        } else {
                            stop = true;
                        }
                    }
                }
            }
        }
        if(genCastleMoves) {
            if(color == CONSTANTS.WHITE) {
                // check for castling
                // white kingside
                if(  this.board[CONSTANTS.E1] != CONSTANTS.EMPTY && this.canCastleWhiteKing()
                        && this.board[CONSTANTS.H1] != CONSTANTS.EMPTY &&
                        this.getPieceColorAt(CONSTANTS.E1) == CONSTANTS.WHITE &&
                        this.getPieceColorAt(CONSTANTS.H1) == CONSTANTS.WHITE &&
                        this.getPieceTypeAt(CONSTANTS.E1) == CONSTANTS.KING &&
                        this.getPieceTypeAt(CONSTANTS.H1) == CONSTANTS.ROOK &&
                        this.board[CONSTANTS.F1] == CONSTANTS.EMPTY && this.board[CONSTANTS.G1] == CONSTANTS.EMPTY) {
                    moves.add(new Move(CONSTANTS.E1,CONSTANTS.G1));
                }
                // white queenside
                if( this.board[CONSTANTS.E1] != CONSTANTS.EMPTY && this.canCastleWhiteQueen()
                        && this.board[CONSTANTS.A1] != CONSTANTS.EMPTY &&
                        this.getPieceColorAt(CONSTANTS.E1) == CONSTANTS.WHITE &&
                        this.getPieceColorAt(CONSTANTS.A1) == CONSTANTS.WHITE &&
                        this.getPieceTypeAt(CONSTANTS.E1) == CONSTANTS.KING &&
                        this.getPieceTypeAt(CONSTANTS.A1) == CONSTANTS.ROOK
                        && this.board[CONSTANTS.D1]==CONSTANTS.EMPTY
                        && this.board[CONSTANTS.C1]==CONSTANTS.EMPTY && this.board[CONSTANTS.B1]==CONSTANTS.EMPTY) {
                    moves.add(new Move(CONSTANTS.E1, CONSTANTS.C1));
                }
            }
            if(color == CONSTANTS.BLACK) {
                // black kingside
                if(this.board[CONSTANTS.E8] !=CONSTANTS.EMPTY && this.canCastleBlackKing()
                        && this.board[CONSTANTS.H8]!=CONSTANTS.EMPTY &&
                        this.getPieceColorAt(CONSTANTS.E8) == CONSTANTS.BLACK &&
                        this.getPieceColorAt(CONSTANTS.H8) == CONSTANTS.BLACK &&
                        this.getPieceTypeAt(CONSTANTS.E8) == CONSTANTS.KING &&
                        this.getPieceTypeAt(CONSTANTS.H8) == CONSTANTS.ROOK &&
                        this.board[CONSTANTS.F8]==CONSTANTS.EMPTY && this.board[CONSTANTS.G8]==CONSTANTS.EMPTY) {
                    moves.add(new Move(CONSTANTS.E8, CONSTANTS.G8));
                }
                // black queenside
                if(this.board[CONSTANTS.E8]!=CONSTANTS.EMPTY && this.canCastleBlackQueen()
                        && board[CONSTANTS.A8] !=CONSTANTS.EMPTY &&
                        this.getPieceColorAt(CONSTANTS.E8) == CONSTANTS.BLACK &&
                        this.getPieceColorAt(CONSTANTS.A8) == CONSTANTS.BLACK &&
                        this.getPieceTypeAt(CONSTANTS.E8) == CONSTANTS.KING &&
                        this.getPieceTypeAt(CONSTANTS.A8) == CONSTANTS.ROOK &&
                        this.board[CONSTANTS.D8]==CONSTANTS.EMPTY && this.board[CONSTANTS.C8]==CONSTANTS.EMPTY
                        && this.board[CONSTANTS.B8]==CONSTANTS.EMPTY) {
                    moves.add(new Move(CONSTANTS.E8, CONSTANTS.C8));
                }
            }
        }
        return moves;
    }

    // doesn't account for attacks via en-passant
    private boolean isAttacked(int idx, int attacker_color) {
        // first check for potential pawn attackers
        // attacker color white, pawn must be white.
        // lower right
        if(attacker_color == CONSTANTS.WHITE && (this.board[idx+CONSTANTS.DIR_SE]!=CONSTANTS.FRINGE
                && this.board[idx+CONSTANTS.DIR_SE] != CONSTANTS.EMPTY)
            && (this.getPieceColorAt(idx+CONSTANTS.DIR_SE)==CONSTANTS.WHITE)
                && (this.getPieceTypeAt(idx+CONSTANTS.DIR_SE)==CONSTANTS.PAWN)) {
            return true;
        }
        // lower left
        if(attacker_color == CONSTANTS.WHITE && (this.board[idx+CONSTANTS.DIR_SW]!=CONSTANTS.FRINGE
                && this.board[idx+CONSTANTS.DIR_SW] != CONSTANTS.EMPTY)
            && (this.getPieceColorAt(idx+CONSTANTS.DIR_SW)==CONSTANTS.WHITE)
                && (this.getPieceTypeAt(idx+CONSTANTS.DIR_SW)==CONSTANTS.PAWN)) {
            return true;
        }
        // check black, upper right
        if(attacker_color == CONSTANTS.BLACK && (this.board[idx+CONSTANTS.DIR_NE]!=CONSTANTS.FRINGE
                && this.board[idx+CONSTANTS.DIR_NE] != CONSTANTS.EMPTY)
            && (this.getPieceColorAt(idx+CONSTANTS.DIR_NE)==CONSTANTS.BLACK)
                && (this.getPieceTypeAt(idx+CONSTANTS.DIR_NE)==CONSTANTS.PAWN)) {
            return true;
        }
        // check black, upper left
        if(attacker_color == CONSTANTS.BLACK && (this.board[idx+9]!=CONSTANTS.FRINGE
                && this.board[idx+CONSTANTS.DIR_NW] != CONSTANTS.EMPTY)
            && (this.getPieceColorAt(idx+CONSTANTS.DIR_NW)==CONSTANTS.BLACK)
                && (this.getPieceTypeAt(idx+CONSTANTS.DIR_NW)==CONSTANTS.PAWN)) {
            return true;
        }
        // check if knight attacks
        for(int i=1;i<CONSTANTS.DIR_TABLE[CONSTANTS.KNIGHT][0]+1;i++) {
            int sqOffset = idx + CONSTANTS.DIR_TABLE[CONSTANTS.KNIGHT][i];
            int sq = board[sqOffset];
            if(sq != 0xFF && sq != CONSTANTS.EMPTY) {
                int pieceType = this.getPieceTypeAt(sqOffset);
                int pieceColor = this.getPieceColorAt(sqOffset);
                if (pieceType == CONSTANTS.KNIGHT && pieceColor == attacker_color) {
                    return true;
                }
            }
        }
        // check if other king attacks
        for(int i=1;i<CONSTANTS.DIR_TABLE[CONSTANTS.KING][0]+1;i++) {
            int sqOffset = idx + CONSTANTS.DIR_TABLE[CONSTANTS.KING][i];
            int sq = board[sqOffset];
            if(sq != 0xFF && sq != CONSTANTS.EMPTY) {
                int pieceType = this.getPieceTypeAt(sqOffset);
                int pieceColor = this.getPieceColorAt(sqOffset);
                if (pieceType == CONSTANTS.KING && pieceColor == attacker_color) {
                    return true;
                }
            }
        }
        // check sliders
        int[] sliders = { CONSTANTS.QUEEN, CONSTANTS.BISHOP, CONSTANTS.ROOK };
        // for each slider
        for ( int slider : sliders ) {
            int noDirections = CONSTANTS.DIR_TABLE[slider][0];
            // for each possible direction (i.e. north, northwest, ...)
            // that the slider can take
            for(int i=1;i<noDirections+1;i++) {
                int direction = CONSTANTS.DIR_TABLE[slider][i];
                // move along the direction (ray), at most
                // seven times
                for(int j=1;j<8;j++) {
                    int offset = idx + (direction * j);
                    // if in fringe, stop
                    if(board[offset] == CONSTANTS.FRINGE) {
                        break;
                    }
                    // if empty, move further along the ray
                    if(board[offset] == CONSTANTS.EMPTY) {
                        continue;
                    }
                    int pieceTypeAtSq = this.getPieceTypeAt(offset);
                    int pieceColorAtSq = this.getPieceColorAt(offset);
                    if(pieceTypeAtSq == slider && pieceColorAtSq == attacker_color) {
                        return true;
                        //newWayResult = true;
                    } else {
                        // here the square cannot be empty (otherwise the above if-cond would trigger)
                        // therefore there must be another piece, either any piece of the current player
                        // or a non-slider of the attacker. This piece blocks this ray direction.
                        break;
                    }
                }
            }
        }
        return false;
    }

    private boolean isCastlesWking(Move m) {
        if(this.getPieceTypeAt(m.from) == CONSTANTS.KING
                && this.getPieceColorAt(m.from) == CONSTANTS.WHITE
                && m.from == CONSTANTS.E1 && m.to == CONSTANTS.G1) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isCastlesWQueen(Move m) {
        if(this.getPieceTypeAt(m.from) == CONSTANTS.KING
                && this.getPieceColorAt(m.from) == CONSTANTS.WHITE
                && m.from == CONSTANTS.E1 && m.to == CONSTANTS.C1) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isCastlesBking(Move m) {
        if(this.getPieceTypeAt(m.from) == CONSTANTS.KING
                && this.getPieceColorAt(m.from) == CONSTANTS.BLACK
                && m.from == CONSTANTS.E8 && m.to == CONSTANTS.G8) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isCastlesBqueen(Move m) {
        if(this.getPieceTypeAt(m.from) == CONSTANTS.KING
                && this.getPieceColorAt(m.from) == CONSTANTS.BLACK
                && m.from == CONSTANTS.E8 && m.to == CONSTANTS.C8) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the supplied pseudo-legal Move is a legal Move in the current position.
     * It does not check if the supplied Move is actually a pseudo-legal Move in the current
     * position
     * @param m a pseudo-legal Move in the current position
     * @return true, if the Move is legal, false otherwise
     */
    public boolean isPseudoALegal(Move m) {
        // a pseudo legal move is a legal move if
        // a) doesn't put king in check
        // b) if castles, must ensure that 1) king is not currently in check
        //                                2) castle over squares are not in check
        //                                3) doesn't castle into check
        // first find color of mover
        int color = this.getPieceColorAt(m.from);
        // find king with that color
        int i = pieceList[color][CONSTANTS.KING][0];
        // if the move is not by the king
        if (i != m.from) {
            boolean whiteEpCapture = false;
            boolean blackEpCapture = false;
            if(getPieceTypeAt(m.from) == CONSTANTS.PAWN) {
                // detect if we have an en passant move
                // for ep captures we need extra care to later store and put
                // the vanishing pawn back to its square to undo and roll back
                if (this.board[m.to] == CONSTANTS.EMPTY) {
                    if (color == CONSTANTS.WHITE && ((m.to - m.from == CONSTANTS.DIR_NW)
                            || (m.to - m.from) == CONSTANTS.DIR_NE)) {
                        whiteEpCapture = true;
                    }
                    if (color == CONSTANTS.BLACK && ((m.from - m.to == CONSTANTS.DIR_NW)
                            || (m.from - m.to) == CONSTANTS.DIR_NE)) {
                        blackEpCapture = true;
                    }
                }
            }
            int old_target = board[m.to];
            board[m.to] = board[m.from];
            board[m.from] = CONSTANTS.EMPTY;
            if(whiteEpCapture) {
                this.board[m.to + CONSTANTS.DIR_S] = CONSTANTS.EMPTY;
            }
            if(blackEpCapture) {
                this.board[m.to + CONSTANTS.DIR_N] = CONSTANTS.EMPTY;
            }
            boolean legal = !this.isAttacked(i, negColor(color));
            board[m.from] = board[m.to];
            board[m.to] = old_target;
            if(whiteEpCapture) {
                this.board[m.to + CONSTANTS.DIR_S] = CONSTANTS.BLACK_PAWN;
            }
            if(blackEpCapture) {
                this.board[m.to + CONSTANTS.DIR_N] = CONSTANTS.WHITE_PAWN;
            }
            return legal;
        } else {
            // means we move the king
            // first check castles
            if (this.isCastlesWking(m)) {
                if (!this.isAttacked(CONSTANTS.E1, CONSTANTS.BLACK)
                        && !this.isAttacked(CONSTANTS.F1, CONSTANTS.BLACK)
                        && !this.isAttacked(CONSTANTS.G1, CONSTANTS.BLACK)) {
                    this.board[CONSTANTS.H1] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.E1] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.G1] = CONSTANTS.WHITE_KING;
                    this.board[CONSTANTS.F1] = CONSTANTS.WHITE_ROOK;
                    boolean legal = !this.isAttacked(CONSTANTS.G1, CONSTANTS.BLACK);
                    this.board[CONSTANTS.H1] = CONSTANTS.WHITE_ROOK;
                    this.board[CONSTANTS.E1] = CONSTANTS.WHITE_KING;
                    this.board[CONSTANTS.G1] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.F1] = CONSTANTS.EMPTY;
                    return legal;
                } else {
                    return false;
                }
            }
            if (this.isCastlesBking(m)) {
                if (!this.isAttacked(CONSTANTS.E8, CONSTANTS.WHITE)
                        && !this.isAttacked(CONSTANTS.F8, CONSTANTS.WHITE)
                        && !this.isAttacked(CONSTANTS.G8, CONSTANTS.WHITE)) {
                    this.board[CONSTANTS.H8] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.E8] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.G8] = CONSTANTS.BLACK_KING;
                    this.board[CONSTANTS.F8] = CONSTANTS.BLACK_ROOK;
                    boolean legal = !this.isAttacked(CONSTANTS.G8, CONSTANTS.WHITE);
                    this.board[CONSTANTS.H8] = CONSTANTS.BLACK_ROOK;
                    this.board[CONSTANTS.E8] = CONSTANTS.BLACK_KING;
                    this.board[CONSTANTS.G8] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.F8] = CONSTANTS.EMPTY;
                    return legal;
                } else {
                    return false;
                }
            }
            if (this.isCastlesWQueen(m)) {
                if (!this.isAttacked(CONSTANTS.E1, CONSTANTS.BLACK)
                        && !this.isAttacked(CONSTANTS.D1, CONSTANTS.BLACK)
                        && !this.isAttacked(CONSTANTS.C1, CONSTANTS.BLACK)) {
                    this.board[CONSTANTS.E1] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.A1] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.D1] = CONSTANTS.WHITE_ROOK;
                    this.board[CONSTANTS.C1] = CONSTANTS.WHITE_KING;
                    boolean legal = !this.isAttacked(CONSTANTS.C1, CONSTANTS.BLACK);
                    this.board[CONSTANTS.E1] = CONSTANTS.WHITE_KING;
                    this.board[CONSTANTS.A1] = CONSTANTS.WHITE_ROOK;
                    this.board[CONSTANTS.D1] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.C1] = CONSTANTS.EMPTY;
                    return legal;
                } else {
                    return false;
                }
            }
            if (this.isCastlesBqueen(m)) {
                if (!this.isAttacked(CONSTANTS.E8, CONSTANTS.WHITE)
                        && !this.isAttacked(CONSTANTS.D8, CONSTANTS.WHITE)
                        && !this.isAttacked(CONSTANTS.C8, CONSTANTS.WHITE)) {
                    this.board[CONSTANTS.E8] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.A8] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.D8] = CONSTANTS.BLACK_ROOK;
                    this.board[CONSTANTS.C8] = CONSTANTS.BLACK_KING;
                    boolean legal = !this.isAttacked(CONSTANTS.C8, CONSTANTS.WHITE);
                    this.board[CONSTANTS.E8] = CONSTANTS.BLACK_KING;
                    this.board[CONSTANTS.A8] = CONSTANTS.BLACK_ROOK;
                    this.board[CONSTANTS.D8] = CONSTANTS.EMPTY;
                    this.board[CONSTANTS.C8] = CONSTANTS.EMPTY;
                    return legal;
                } else {
                    return false;
                }
            }
            // if none of the castles cases triggered, we have a standard king move
            // just check if king isn't attacked after applying the move
            int old_target = board[m.to];
            board[m.to] = board[m.from];
            board[m.from] = CONSTANTS.EMPTY;
            boolean legal = !this.isAttacked(m.to, negColor(color));
            board[m.from] = board[m.to];
            board[m.to] = old_target;
            return legal;
        }
    }

    /**
     * Checks if the supplied Move is a legal Move in the current position.
     * This is done by first generating all pseudo-legal Moves, then filtering
     * for all legal Moves, and then finally checking if the supplied Move is
     * contained in the list of legal Moves
     * @param m the Move that is about to be checked for legality
     * @return true, if Move is legal, false otherwise
     */
    public boolean isLegal(Move m) {
        ArrayList<Move> pseudoLegals = this.pseudoLegalMoves(m.from, m.to, CONSTANTS.ANY_PIECE, true, this.turn);
        for(Move mi : pseudoLegals) {
            if(mi.from == m.from && mi.to == m.to && mi.promotionPiece == m.promotionPiece) {
                if(isPseudoALegal(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the supplied Move is a legal Move in the current position
     * and if the Move leads to a promotion (i.e. is a pawn Move that reaches
     * the last rank). This will return true even if the promotionPiece
     * is _not_ set in the supplied Move.
     * @param m the Move. Only source and target square have to be set
     * @return true, if the Move is legal and leads to a promotion, false otherwise
     */
    public boolean isLegalAndPromotes(Move m) {
        ArrayList<Move> pseudoLegals = this.pseudoLegalMoves(m.from, m.to, CONSTANTS.ANY_PIECE, true, this.turn);
        for(Move mi : pseudoLegals) {
            if(mi.from == m.from && mi.to == m.to && mi.promotionPiece != 0) {
                if(isPseudoALegal(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * computes an ArrayList of all legal Moves in the current position
     * by first generating all pseudo-legal Moves and then filtering for legal ones.
     * @return ArrayList containing all legal Moves
     */
    public ArrayList<Move> legalMoves() {

        ArrayList<Move> pseudoLegals = this.pseudoLegalMoves();
        // System.out.println("pseudoLegalSize: "+pseudoLegals.size());
        ArrayList<Move> legals = new ArrayList<Move>();
        for(Move mi : pseudoLegals) {
            try {
                if (this.isPseudoALegal(mi)) {
                    legals.add(mi);
                }
            } catch(IllegalArgumentException e) {
                int cnt_i = pseudoLegals.size();
                this.initPieceList();
                int cnt_i1 = this.pseudoLegalMoves().size();
                throw new IllegalArgumentException("err: before init pcl: "+ cnt_i + ", after: "+cnt_i1);
            }
        }
        return legals;
    }

    /**
     * computes a subset of all legal Moves in the current position by first generating
     * all pseudo-legal Moves and then filtering for legal ones. The subset can be defined
     * by providing a target square and the desired piece type
     * @param internalToSquare the target square (internal coordinates) that all Moves must lead to or CONSTANTS.ANY_SQUARE
     * @param pieceType limit generated Moves to those of the specific piece type or CONSTANTS.ANY_PIECE
     * @return ArrayList with a subset of all legal Moves
     */
    public ArrayList<Move> legaMovesTo(int internalToSquare, int pieceType) {

        ArrayList<Move> pseudoLegals = this.pseudoLegalMoves(CONSTANTS.ANY_SQUARE, internalToSquare, pieceType, true, this.turn);
        ArrayList<Move> legals = new ArrayList<Move>();
        for(Move mi : pseudoLegals) {
            if(this.isPseudoALegal(mi)) {
                legals.add(mi);
            }
        }
        return legals;
    }

    /**
     * computes a subset of all legal Moves in the current position by first generating
     * all pseudo-legal Moves and then filtering for legal ones. The subset can be defined
     * by providing a source square where the Move must originate in
     * @param internalFromSquare the source square (internal coordinates) that all Moves must originate in
     *                           or CONSTANTS.ANY_SQUARE
     * @return ArrayList with a subset of all legal Moves
     */
    public ArrayList<Move> legalMovesFrom(int internalFromSquare) {
        ArrayList<Move> pseudoLegals =
                this.pseudoLegalMoves(
                        internalFromSquare,
                        CONSTANTS.ANY_SQUARE,
                        CONSTANTS.ANY_PIECE,
                        true,
                        this.turn
                );
        ArrayList<Move> legals = new ArrayList<Move>();
        for(Move mi : pseudoLegals) {
            if(this.isPseudoALegal(mi)) {
                legals.add(mi);
            }
        }
        return legals;
    }

    /**
     * This filters a supplied ArrayList of pseudo legal Moves for legal Moves
     * @param pseudos an ArrayList of pseudo legal Moves. These Moves are
     *                not verified for pseudo-legality
     * @return those Moves from the input list that are legal
     */
    public ArrayList<Move> legalsFromPseudos(ArrayList<Move> pseudos) {
        ArrayList<Move> legals = new ArrayList<Move>();
        for(Move mi : pseudos) {
            if(this.isPseudoALegal(mi)) {
                legals.add(mi);
            }
        }
        return legals;
    }

    /**
     * Checks if the position is a checkmate.
     * @return true if checkmate, false otherwise
     */
    public boolean isCheckmate() {
        // search for king of player with current turn
        // check whether king is attacked
        // check if player has no moves
        for(int i=21;i<99;i++) {
            if(this.board[i] != CONSTANTS.EMPTY && this.board[i] != CONSTANTS.FRINGE) {
                if(this.getPieceTypeAt(i) == CONSTANTS.KING && this.getPieceColorAt(i) == this.turn) {
                    if(this.isAttacked(i, negColor(this.turn))) {
                        ArrayList<Move> legals = this.legalMoves();
                        return legals.size() == 0;
                    } else{
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the current position is a stalemate.
     * @return true for a stalemate, false otherwise
     */
    public boolean isStalemate() {
        // search for king of player with current turn
        // check whether king is not attacked
        // check if player has no moves
        for(int i=21;i<99;i++) {
            if(this.board[i] != CONSTANTS.EMPTY && this.board[i] != CONSTANTS.FRINGE) {
                if(this.getPieceTypeAt(i) == CONSTANTS.KING && this.getPieceColorAt(i) == this.turn) {
                    if(!this.isAttacked(i, negColor(this.turn))) {
                        ArrayList<Move> legals = this.legalMoves();
                        return legals.size() == 0;
                    } else{
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the king of the player whose turn it is in check
     * @return true if the king is in check, false otherwise
     */
    public boolean isCheck() {
        for(int i=21;i<99;i++) {
            if(this.board[i] != CONSTANTS.EMPTY && this.board[i] != CONSTANTS.FRINGE) {
                if (this.getPieceTypeAt(i) == CONSTANTS.KING && this.getPieceColorAt(i) == this.turn) {
                    return this.isAttacked(i, negColor(this.turn));
                }
            }
        }
        return false;
    }

    /**
     * Creates a Move representation in Short Algebraic Notation
     * @param m a Move for which SAN is desired
     * @return String with SAN
     */
    public String san(Move m) {
        // first check for null move
        StringBuilder san = new StringBuilder();
        if(m.isNullMove) {
            return "--";
        }
        // first test for checkmate and check (to be appended later)
        // create temp board, since applying move and
        // testing for checkmate (which again needs
        // application of a move) makes it impossible
        // to undo (undo can only be done once, not twice in a row)
        Board b_temp = this.makeCopy();
        b_temp.apply(m);
        boolean is_check = b_temp.isCheck();
        boolean is_checkmate = b_temp.isCheckmate();

        if(this.isCastlesWking(m) || this.isCastlesBking(m)) {
            san.append("O-O");
            if(is_checkmate) {
                san.append("#");
            }
            if(is_check) {
                san.append("+");
            }
            return san.toString();
        } else if(this.isCastlesWQueen(m) || this.isCastlesBqueen(m)) {
            san.append("O-O-O");
            if(is_checkmate) {
                san.append("#");
            } else if(is_check) {
                san.append("+");
            }
            return san.toString();
        } else {
            int pieceType = this.getPieceTypeAt(m.from);
            //int piece = this.getPieceAt(m.from);
            if(pieceType == CONSTANTS.KNIGHT) {
                san.append("N");
            }
            if(pieceType == CONSTANTS.BISHOP) {
                san.append("B");
            }
            if(pieceType == CONSTANTS.ROOK) {
                san.append("R");
            }
            if(pieceType == CONSTANTS.QUEEN) {
                san.append("Q");
            }
            if(pieceType == CONSTANTS.KING) {
                san.append("K");
            }
            //QVector<Move> col_disambig;
            //QVector<Move> row_disambig;
            int thisRow = (m.from / 10) - 1;
            int thisCol = m.from % 10;

            ArrayList<Move> colDisAmbig = new ArrayList<>();
            ArrayList<Move> rowDisAmbig = new ArrayList<>();
            // find amibguous moves (except for pawns)
            if(pieceType != CONSTANTS.PAWN) {
                // if piece list contains only one piece, skip move generation
                // for testing disambiguity
                //int color = 0;
                //if(this.turn) {
                //    color = 1;
                //}
                if(this.pieceList[this.turn][pieceType][1] != CONSTANTS.EMPTY) {
                    // otherwise we are finished as there is only one piece
                    ArrayList<Move> pseudos = this.pseudoLegalMoves(CONSTANTS.ANY_SQUARE, m.to,
                            pieceType, false, this.turn);
                    if(pseudos.size() != 1) { // otherwise we are finished, as there is only one pseudo-legal move
                        ArrayList<Move> legals = this.legalsFromPseudos(pseudos);
                        if(legals.size() != 1) { // really need to resolve disambiguity
                            for(Move mi : legals) {
                                if(mi.from != m.from) { // skip the actual move to render
                                    if (mi.from % 10 != thisCol) {
                                        colDisAmbig.add(mi);
                                    } else {
                                        rowDisAmbig.add(mi);
                                    }
                                }
                            }
                        }
                    }
                }
                int cntColDisambig = colDisAmbig.size();
                int cntRowDisambig = rowDisAmbig.size();
                // if there is an ambiguity
                if(cntColDisambig != 0 || cntRowDisambig != 0) {
                    // preferred way: resolve via column
                    if(cntColDisambig>0 && cntRowDisambig==0) {
                        san.append((char) (thisCol + 96));
                        // if not try to resolve via row
                    } else if(cntRowDisambig>0 && cntColDisambig==0) {
                        san.append((char) (thisRow + 48));
                    } else {
                        // if that also fails (think three queens)
                        // resolve via full coordinate
                        san.append((char) (thisCol + 96));
                        san.append((char) (thisRow + 48));
                    }
                }
            }
            // handle a capture, i.e. if destination field
            // is not empty
            // in case of an en-passent capture, the destiation field
            // is empty. But then the destination field is the e.p. square
            if(this.board[m.to] != CONSTANTS.EMPTY || m.to == this.enPassentTarget) {
                if(pieceType == CONSTANTS.PAWN) {
                    san.append((char) (thisCol + 96));
                }
                san.append("x");
            }
            san.append(this.internalIdxToString(m.to));
            if(m.promotionPiece == CONSTANTS.KNIGHT) {
                san.append("=N");
            }
            if(m.promotionPiece == CONSTANTS.BISHOP) {
                san.append("=B");
            }
            if(m.promotionPiece == CONSTANTS.ROOK) {
                san.append("=R");
            }
            if(m.promotionPiece == CONSTANTS.QUEEN) {
                san.append("=Q");
            }
        }
        if(is_checkmate) {
            san.append("#");
        } else if(is_check) {
            san.append("+");
        }
        return san.toString();
    }

    /**
     * Tests if a Move promotes by testing the promotionPiece member
     * of the Move
     * @param m Move to be considered
     * @return true if Move promotes, false otherwise
     */
    public boolean isPromoting(Move m) {
        return m.promotionPiece > 0;
    }

    /**
     * Checks if the current Board is set up with the initial position
     * Castling rights are considered as well as other parameters
     * such as halfmove clock
     * @return true if has initial position, false otherwise
     */
    public boolean isInitialPosition() {
        if(negColor(this.turn) == CONSTANTS.WHITE) {
            return false;
        }
        for(int i=0;i<120;i++) {
            if(this.board[i] != CONSTANTS.INIT_POS[i]) {
                return false;
            }
        }
        if(!(this.canCastleWhiteKing() && this.canCastleBlackKing()
                && this.canCastleWhiteQueen() && this.canCastleBlackQueen())) {
            return false;
        }

        if(this.enPassentTarget != 0) {
            return false;
        }
        if(this.halfmoveClock != 0) {
            return false;
        }
        if(this.fullmoveNumber != 1) {
            return false;
        }
        if(this.undoAvailable) {
            return false;
        }
        return true;
    }

    /**
     * Checks if white can castle short
     * @return true if white can castle short, false otherwise
     */
    public boolean canCastleWhiteKing() {
        return this.castleWkingOk;
    }

    /**
     * Checks if black can castle short
     * @return true if black can castle short, false otherwise
     */
    public boolean canCastleBlackKing() {
        return this.castleBkingOk;
    }

    /**
     * Checks if white can castle long
     * @return true if white can castle long, false otherwise
     */
    public boolean canCastleWhiteQueen() {
        return this.castleWqueenOk;
    }

    /**
     * Checks if black can castle long
     * @return true if black can castle long, false otherwise
     */
    public boolean canCastleBlackQueen() {
        return this.castleBqueenOk;
    }

    /**
     * Checks if Undo is possible for the current Board. Note that at most
     * one undo operation is possible at a time (only undo the last applied move)
     * @return
     */
    public boolean isUndoAvailable() {
        return this.undoAvailable;
    }

    /**
     * Sets the castling rights for white castles short
     * @param canDo
     */
    public void setCastleWKing(boolean canDo) {
        this.castleWkingOk = canDo;
    }

    /**
     * Sets the castling rights for black castles short
     * @param canDo
     */
    public void setCastleBKing(boolean canDo) {
        this.castleBkingOk = canDo;
    }

    /**
     * Sets the castling rights for white castles long
     * @param canDo
     */
    public void setCastleWQueen(boolean canDo) {
        this.castleWqueenOk = canDo;
    }

    /**
     * Sets the castling rights for black castles long
     * @param canDo
     */
    public void setCastleBQueen(boolean canDo) {
        this.castleBqueenOk = canDo;
    }

    /**
     * Stores a piece at the provided square. The square location must
     * be provided as x,y (not internal coordinates)
     * @param x file (i.e. 0=A ... 7=H)
     * @param y rank (0=Rank 1, ... 7=Rank 8)
     * @param piece the piece (of the constants, i.e. CONSTANTS.WHITE_ROOK, CONSTANTS.BLACK_PAWN etc.)
     */
    public void setPieceAt(int x, int y, int piece) {
        // check wether x,y is a valid location on chess board
        // and wether piece is a valid piece
        if(x>=0 && x<8 && y>=0 && y <8 &&
                ((piece >= 0x01 && piece <= 0x07) ||  // white piece
                 (piece >= 0x81 && piece <= 0x87) || (piece == 0x00))) // black piece or empty
        {
            int idx = Board.xyToInternal(x,y);
            this.board[idx] = piece;
            // we need to recalculate the piece list, if the board
            // was manually modified
            this.initPieceList();
        } else {
            throw new IllegalArgumentException("called setPieceAt with invalid paramters, (x,y,piece): "+x+","+y+","+piece);
        }
    }

    /**
     * Returns the piece (one of CONSTANTS.WHITE_KING, CONSTANTS.BLACK_ROOK etc.)
     * at the requested square.
     * @param x file (i.e. 0=A ... 7=H)
     * @param y rank (0=Rank 1, ... 7=Rank 8)
     * @return Piece at the given location. Returns CONSTANTS.EMPTY if not piece is present.
     */
    public int getPieceAt(int x, int y) {
        if(x>=0 && x<8 && y>=0 && y <8) {
            int idx = Board.xyToInternal(x,y);
            return this.board[idx];
        } else {
            throw new IllegalArgumentException("called getPieceAt with invalid parameters, (x,y): "+x+","+y);
        }
    }

    /**
     * Tests if a piece is set at the given square, of if the square is empty
     * @param x file (i.e. 0=A ... 7=H)
     * @param y rank (0=Rank 1, ... 7=Rank 8)
     * @return true if a piece is present at the square, false otherwise
     */
    public boolean isPieceAt(int x, int y) {
        if (x >= 0 && x < 8 && y >= 0 && y < 8) {
            int idx = Board.xyToInternal(x, y);
            if ((this.board[idx] >= CONSTANTS.WHITE_PAWN && this.board[idx] <= CONSTANTS.WHITE_KING) ||
                    (this.board[idx] >= CONSTANTS.BLACK_PAWN && this.board[idx] <= CONSTANTS.BLACK_KING)) {
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException("called isPieceAt with invalid paramters, (x,y): "+x+","+y);
        }
    }

    /**
     * Returns the piece at the given square, of if the square is empty
     * @param internalCoordinate square location as internal coordinate
     *                           (i.e. absolute index of the 120 array representation of the Board)
     * @return piece of the requested square, CONSTANTS.EMPTY otherwise
     */
    public int getPieceAt(int internalCoordinate) {
        if(internalCoordinate <21 || internalCoordinate >98) {
            throw new IllegalArgumentException("out of range: " + internalCoordinate);
        }
        return this.board[internalCoordinate];
    }

    /**
     * Returns the piece type (regardless of color, i.e. one of CONSTANTS.ROOK,
     * CONSTANTS.KING etc.) of a given square, of if the square is empty
     * @param x file (i.e. 0=A ... 7=H)
     * @param y rank (0=Rank 1, ... 7=Rank 8)
     * @return piece type at the requested square, CONSTANTS.EMPTY otherwise
     */
    public int getPieceTypeAt(int x, int y) {
        if(x>=0 && x<8 && y>=0 && y <8) {
            int idx = Board.xyToInternal(x,y);
            int piece = this.board[idx];
            if(piece >= 0x80) {
                return piece - 0x80;
            } else {
                return piece;
            }
        } else {
            throw new IllegalArgumentException("called get_piece_type_at with invalid paramters, (x,y): "+x + ","+y);
        }
    }

    /**
     * Return the piece type (regardless of color, i.e. one of CONSTANTS.ROOK,
     * CONSTANTS.KING etc.) at the given square, of if the square is empty
     * @param internalCoordinate square location as internal coordinate
     *                           (i.e. absolute index of the 120 array representation of the Board)
     * @return piece type at the requested square, throws error otherwise
     */
    public int getPieceTypeAt(int internalCoordinate) {
        if(internalCoordinate <21 || internalCoordinate >98) {
            throw new IllegalArgumentException("out of range: " + internalCoordinate);
        }
        int piece = this.board[internalCoordinate];
        if(piece == CONSTANTS.EMPTY) {
            throw new IllegalArgumentException("no piece: field is empty!");
        }
        if(piece == CONSTANTS.FRINGE) {
            throw new IllegalArgumentException("no piece: field in fringe!");
        }
        if(piece > 0x80) {
            return piece - 0x80;
        } else {
            return piece;
        }
    }

    /**
     * Get the color of the piece at the given square. Does not
     * check if a piece exists at the square and will return
     * incorrect information for empty square
     * @param x file (i.e. 0=A ... 7=H)
     * @param y rank (0=Rank 1, ... 7=Rank 8)
     * @return one of CONSTANTS.WHITE or CONSTANTS.BLACK
     */
    public int getPieceColorAt(int x, int y) {
        int internalCoordinate = Board.xyToInternal(x,y);
        return this.getPieceColorAt(internalCoordinate);
    }

    /**
     * Get the color of the piece at the given square. Does not
     * check if a piece exists at the square and will return
     * incorrect information for empty square
     * @param internalCoordinate square location as internal coordinate
     *                           (i.e. absolute index of the 120 array representation of the Board)
     * @return one of CONSTANTS.WHITE or CONSTANTS.BLACK
     */
    public int getPieceColorAt(int internalCoordinate) {
        if(this.board[internalCoordinate] > 0x80) {
            return CONSTANTS.BLACK;
        } else {
            return CONSTANTS.WHITE;
        }
    }

    /**
     * Get the location of the king in internal coordinates. The
     * King's color is supplied as a parameter
     * @param player one of CONSTANTS.WHITE or CONSTANTS.BLACK
     * @return internal coordinate of the requested king
     */
    public int getKingPos(boolean player) {
        for(int i=21;i<99;i++) {
            if(player == CONSTANTS.B_WHITE) {
                if(this.board[i] == CONSTANTS.WHITE_KING) {
                    return i;
                }
            } else {
                if(this.board[i] == CONSTANTS.BLACK_KING) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("there is no king on the board for supplied player!");
    }

    /**
     * Computes various characteristics of the current position to check if the
     * position is in a consistent state. Metrics used: Number of pieces does not
     * exceed the theoretical maximum numbers, each player has one king,
     * side not to move is not in check, kings are at least on square apart,
     * castling rights fits to the position of kings and rooks
     * @return true if the board is in a consistent state, false otherwise
     */
    public boolean isConsistent() {
        int whiteKingPos = -1;
        int blackKingPos = -1;

        int cntWhiteKing = 0;
        int cntBlackKing = 0;

        int cntWhiteQueens = 0;
        int cntWhiteRooks = 0;
        int cntWhiteBishops = 0;
        int cntWhiteKnights = 0;
        int cntWhitePawns = 0;

        int cntBlackQueens = 0;
        int cntBlackRooks = 0;
        int cntBlackBishops = 0;
        int cntBlackKnights = 0;
        int cntBlackPawns = 0;

        for(int i=21;i<99;i++) {
            if(this.board[i] != CONSTANTS.EMPTY && this.board[i] != CONSTANTS.FRINGE) {
                int pieceType = this.getPieceTypeAt(i);
                int pieceColor = this.getPieceColorAt(i);
                if (pieceType == CONSTANTS.KING) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        whiteKingPos = i;
                        cntWhiteKing++;
                    } else {
                        blackKingPos = i;
                        cntBlackKing++;
                    }
                } else if (pieceType == CONSTANTS.QUEEN) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        cntWhiteQueens++;
                    } else {
                        cntBlackQueens++;
                    }
                } else if (pieceType == CONSTANTS.ROOK) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        cntWhiteRooks++;
                    } else {
                        cntBlackRooks++;
                    }
                } else if (pieceType == CONSTANTS.BISHOP) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        cntWhiteBishops++;
                    } else {
                        cntBlackBishops++;
                    }
                } else if (pieceType == CONSTANTS.KNIGHT) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        cntWhiteKnights++;
                    } else {
                        cntBlackKnights++;
                    }
                } else if (pieceType == CONSTANTS.PAWN) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        if ((i / 10) == 2 || (i / 10) == 9) {
                            // white pawn in first rank or on promotion square
                            return false;
                        } else {
                            cntWhitePawns++;
                        }
                    } else {
                        if ((i / 10) == 9 || (i / 10) == 2) {
                            // black pawn in 8th rank or on promotion square
                            return false;
                        } else {
                            cntBlackPawns++;
                        }
                    }
                }
            }
        }
        // exactly one white and black king exist on board
        if(cntWhiteKing != 1 || cntBlackKing != 1) {
            return false;
        }
        // white and black king at least one field apart
        int larger = whiteKingPos;
        int smaller = blackKingPos;
        if(blackKingPos > whiteKingPos) {
            larger = blackKingPos;
            smaller = whiteKingPos;
        }
        int diff = larger - smaller;
        if(diff == 10 || diff == 1 || diff == 11 || diff == 9) {
            return false;
        }
        // side not to move must not be in check
        int notToMove = negColor(this.turn);
        int toMove = this.turn;
        int idx_king_not_to_move = whiteKingPos;
        if(notToMove == CONSTANTS.BLACK) {
            idx_king_not_to_move = blackKingPos;
        }
        if(this.isAttacked(idx_king_not_to_move, toMove)) {
            return false;
        }
        // each side has 8 pawns or less
        if(cntWhitePawns > 8 || cntBlackPawns > 8) {
            return false;
        }
        // check whether no. of promotions and pawn count fits for white
        int whiteExtraPieces = Math.max(0, cntWhiteQueens-1) + Math.max(0, cntWhiteRooks-2)
        + Math.max(0, cntWhiteBishops - 2) + Math.max(0, cntWhiteKnights - 2);
        if(whiteExtraPieces > (8-cntWhitePawns)) {
            return false;
        }
        // ... for black
        int blackExtraPieces = Math.max(0, cntBlackQueens-1) + Math.max(0, cntBlackRooks-2)
        + Math.max(0, cntBlackBishops - 2) + Math.max(0, cntBlackKnights - 2);
        if(blackExtraPieces > (8-cntBlackPawns)) {
            return false;
        }
        // compare encoded castling rights of this board w/ actual
        // position of king and rook
        if(this.canCastleWhiteKing() && this.isWhiteKingCastleLost()) {
            return false;
        }
        if(this.canCastleWhiteQueen() && this.isWhiteQueenCastleLost()) {
            return false;
        }
        if(this.canCastleBlackKing() && this.isBlackKingCastleLost()) {
            return false;
        }
        if(this.canCastleBlackQueen() && this.isBlackQueenCastleLost()) {
            return false;
        }
        return true;
    }

    /**
     * Checks if black can still castle short
     * @return true if possible
     */
    public boolean isBlackKingCastleLost() {
        if(this.board[CONSTANTS.E8] == CONSTANTS.BLACK_KING &&
        this.board[CONSTANTS.H8] == CONSTANTS.BLACK_ROOK) {
            return false;
        }
        return true;
    }

    /**
     * Checks if black can still castle long
     * @return true if possible
     */
    public boolean isBlackQueenCastleLost() {
        if(this.board[CONSTANTS.E8] == CONSTANTS.BLACK_KING &&
                this.board[CONSTANTS.A8] == CONSTANTS.BLACK_ROOK) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks if white can still castle short
     * @return true if possible
     */
    public boolean isWhiteKingCastleLost() {
        if(this.board[CONSTANTS.E1] == CONSTANTS.WHITE_KING &&
                this.board[CONSTANTS.H1] == CONSTANTS.WHITE_ROOK) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks if white can still castle long
     * @return true if possible
     */
    public boolean isWhiteQueenCastleLost() {
        if(this.board[CONSTANTS.E1] == CONSTANTS.WHITE_KING &&
                this.board[CONSTANTS.A1] == CONSTANTS.WHITE_ROOK) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns the en passant square (internal coordinates)
     * of the current position.
     * @return ep square, 0 if there is none
     */
    public int getEpTarget() {
        return this.enPassentTarget;
    }

    /**
     * Checks 50 moves rule
     * @return true if the side to move can claim a draw according to the 50 moves rule
     */
    public boolean canClaimFiftyMoves() {
        return this.halfmoveClock >= 100;
    }

    private int zobristPieceType(int piece) {
        switch (piece)
        {
            case CONSTANTS.BLACK_PAWN:
                return 0;
            case CONSTANTS.WHITE_PAWN:
                return 1;
            case CONSTANTS.BLACK_KNIGHT:
                return 2;
            case CONSTANTS.WHITE_KNIGHT:
                return 3;
            case CONSTANTS.BLACK_BISHOP:
                return 4;
            case CONSTANTS.WHITE_BISHOP:
                return 5;
            case CONSTANTS.BLACK_ROOK:
                return 6;
            case CONSTANTS.WHITE_ROOK:
                return 7;
            case CONSTANTS.BLACK_QUEEN:
                return 8;
            case CONSTANTS.WHITE_QUEEN:
                return 9;
            case CONSTANTS.BLACK_KING:
                return 10;
            case CONSTANTS.WHITE_KING:
                return 11;
        }
        throw new IllegalArgumentException("piece type out of range in ZobristHash:kind_of_piece");
    }

    /**
     * Computes the Zobrist hash of the current position
     * @return zobrist hash value
     */
    public long getZobrist() {
        if (this.zobristInitialized) {
            return this.zobristHash;
        } else {
            long piece = 0L;
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    int internalCoordinate = Board.xyToInternal(i, j);
                    if (this.board[internalCoordinate] != CONSTANTS.EMPTY) {
                        int pieceAt_ij = this.getPieceAt(internalCoordinate);
                        int kind_of_piece = this.zobristPieceType(pieceAt_ij);
                        int offset_piece = 64 * kind_of_piece + 8 * j + i;
                        piece = piece ^ CONSTANTS.POLYGLOT_RANDOM_64[offset_piece];
                    }
                }
            }
            long enPassent = 0L;
            int epTarget = this.getEpTarget();
            if (epTarget != 0) {
                int file = (epTarget % 10) - 1;
                // check if left or right is a pawn from player to move
                if (this.turn == CONSTANTS.WHITE) {
                    int left = this.getPieceAt(epTarget - 11);
                    int right = this.getPieceAt(epTarget - 9);
                    if (left == CONSTANTS.WHITE_PAWN || right == CONSTANTS.WHITE_PAWN) {
                        enPassent = CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_EN_PASSENT + file];
                    }
                } else {
                    int left = this.getPieceAt(epTarget + 11);
                    int right = this.getPieceAt(epTarget + 9);
                    if (left == CONSTANTS.BLACK_PAWN || right == CONSTANTS.BLACK_PAWN) {
                        enPassent = CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_EN_PASSENT + file];
                    }
                }
            }
            long castle = 0L;
            if (this.canCastleWhiteKing()) {
                castle = castle ^ CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_CASTLE];
            }
            if (this.canCastleWhiteQueen()) {
                castle = castle ^ CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_CASTLE + 1];
            }
            if (this.canCastleBlackKing()) {
                castle = castle ^ CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_CASTLE + 2];
            }
            if (this.canCastleBlackKing()) {
                castle = castle ^ CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_CASTLE + 3];
            }

            long turn = 0L;
            if (this.turn == CONSTANTS.WHITE) {
                turn = CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_TURN];
            }

            this.zobristHash = piece ^ castle ^ enPassent ^ turn;
            return this.zobristHash;
        }
    }

    /**
     * Computes a hash value of the current position. This is similar to
     * a Zobrist hash, but only considers the location of the pieces and their type,
     * i.e. ignores castling possibilities, turn and en passant square. Can be used
     * to quickly search for positions
     * @return hash value of the current position
     */
    public long getPositionHash() {
        if(this.posHashInitialized) {
            return this.positionHash;
        } else {
            long piece = 0L;
            for(int i=0;i<8;i++) {
                for(int j=0;j<8;j++) {
                    int pieceAt_ij = this.getPieceAt(i,j);
                    if(pieceAt_ij != CONSTANTS.EMPTY) {
                        int kindOfPiece = this.zobristPieceType(pieceAt_ij);
                        int offset_piece = 64 * kindOfPiece + 8 * j + i;
                        piece = piece^CONSTANTS.POLYGLOT_RANDOM_64[offset_piece];
                    }
                }
            }
            this.positionHash = piece;
            return this.positionHash;
        }
    }

    /**
     * Computes the piece constant value (one of CONSTANTS.WHITE_KING, CONSTANTS.BLACK_QUEEN)
     * for a given char encoding the pice (i.e. 'k' for black king
     * @param c encoding a piece
     * @return int value that is used to represent a piece
     */
    public int pieceFromSymbol(char c) {
        if(c == 'K') {
            return 0x06;
        }
        if(c == 'Q') {
            return 0x05;
        }
        if(c == 'R') {
            return 0x04;
        }
        if(c == 'B') {
            return 0x03;
        }
        if(c == 'N') {
            return 0x02;
        }
        if(c == 'P') {
            return 0x01;
        }
        if(c == 'k') {
            return 0x86;
        }
        if(c == 'q') {
            return 0x85;
        }
        if(c == 'r') {
            return 0x84;
        }
        if(c == 'b') {
            return 0x83;
        }
        if(c == 'n') {
            return 0x82;
        }
        if(c == 'p') {
            return 0x81;
        }
        throw new IllegalArgumentException("piece to symbol: unknown input: "+c);
    }

    /**
     * Tests if the k-th bit of a provided int value is set or not
     * @param n the int value to check
     * @param k k-th bit value
     * @return true if set to 1, false if set to 0
     */
    public boolean isKthBitSet(int n, int k) {
        // 1.) shift k positions to the right ( n >> k ) into tempVal
        // 2.) do bitwise AND with 000000....1 (int 1), i.e. res = tempVal & 1
        // 3.) if rightmost bit was set (LSB), result should be 1
        return ((n >> k) & 1) == 1;
    }

    private char pieceToSymbol(int piece) {
        if(piece == CONSTANTS.WHITE_KING) {
            return 'K';
        }
        if(piece == CONSTANTS.WHITE_QUEEN) {
            return 'Q';
        }
        if(piece == CONSTANTS.WHITE_ROOK) {
            return 'R';
        }
        if(piece == CONSTANTS.WHITE_BISHOP) {
            return 'B';
        }
        if(piece == CONSTANTS.WHITE_KNIGHT) {
            return 'N';
        }
        if(piece == CONSTANTS.WHITE_PAWN) {
            return 'P';
        }
        if(piece == CONSTANTS.BLACK_KING) {
            return 'k';
        }
        if(piece == CONSTANTS.BLACK_QUEEN) {
            return 'q';
        }
        if(piece == CONSTANTS.BLACK_ROOK) {
            return 'r';
        }
        if(piece == CONSTANTS.BLACK_BISHOP) {
            return 'b';
        }
        if(piece == CONSTANTS.BLACK_KNIGHT) {
            return 'n';
        }
        if(piece == CONSTANTS.BLACK_PAWN) {
            return  'p';
        }
        throw new IllegalArgumentException("called piece to symbol with unkown value: "+piece);
    }

    @Override
    public String toString() {
        String s = "";
        for(int i=90;i>=20;i-=10) {
            for(int j=1;j<9;j++) {
                int piece = this.board[i+j];
                if(piece != CONSTANTS.EMPTY) {
                    s += this.pieceToSymbol(piece);
                } else {
                    if(this.enPassentTarget == (i+j)) {
                        s += ":";
                    } else {
                        s += ".";
                    }
                }
            }
            s += "\n";
        }
        return s;
    }

    private void initPieceList() {

        for(int i=0;i<7;i++) {
            for(int j=0;j<10;j++) {
                this.pieceList[CONSTANTS.WHITE][i][j] = CONSTANTS.EMPTY;
                this.pieceList[CONSTANTS.BLACK][i][j] = CONSTANTS.EMPTY;
            }
        }
        for(int i=21;i<99;i++) {
            int piece = this.board[i];
            if(!(piece == CONSTANTS.EMPTY) && !(piece == CONSTANTS.FRINGE)) {
                int color = CONSTANTS.WHITE;
                if(piece > 0x80) {
                    piece = piece - 0x80;
                    color = CONSTANTS.BLACK;
                }
                // piece contains now the piece type
                for(int j=0;j<10;j++) {
                    if(this.pieceList[color][piece][j] == CONSTANTS.EMPTY) {
                        this.pieceList[color][piece][j] = i;
                        break;
                    }
                }
            }
        }
    }

    /**
     * For given piece (one of CONSTANTS.WHITE_KING, CONSTANTS.BLACK_KNIGHT)
     * returns the piece type (i.e. one of CONSTANTS.KING, CONSTANTS.KNIGHT)
     * removing the color
     * @param piece the piece to consider
     * @return the piece type
     */
    public int getPieceType(int piece)
    {
      return piece & 0x0000000F;
    }

    /**
     * For given square in x,y coordinates return the color
     * of the piece that is located at the square
     * @param x file (i.e. 0=A ... 7=H)
     * @param y rank (0=Rank 1, ... 7=Rank 8)
     * @return color value (one of CONSTANTS.IWHITE or CONSTANTS.IBLACK)
     */
    public int getSquareColorAt(int x, int y)
    {
      // optional TODO: Check that x and y values are within range 0..7.
      if ((x+y)%2 == 0) {
          return CONSTANTS.BLACK;
      }
      return CONSTANTS.WHITE;
    }

    /**
     * Tests if there is insufficient material to mate and the position is thus drawn
     * @return true if a draw, false otherwise
     */
    public boolean isInsufficientMaterial() {
        int pCounter = 0;
        int lastPiece = CONSTANTS.EMPTY;
        int lastPieceSquareColor = CONSTANTS.WHITE; // just default
        
        // Scan the board:
        for(int x=0;x<8;x++) {
            for(int y=0;y<8;y++) {
                int piece = getPieceAt(x,y);
                if (piece == CONSTANTS.EMPTY)
                    continue;
                
                switch (getPieceType(piece)) {
                    case CONSTANTS.KING:
                        continue;

                    case CONSTANTS.PAWN:
                    case CONSTANTS.QUEEN:
                    case CONSTANTS.ROOK:
                        // Possible to force mate.
                        return false;

                    case CONSTANTS.KNIGHT:
                        if(++pCounter > 1) {
                            // More than a knight:
                            // Theoretically possible to mate or to force mate,
                            // depending on the color of the pieces.
                            return false;
                        }
                        // This is the first knight that we find:
                        // Save it. (not necessary)
                        lastPiece = piece;
                        break;

                    case CONSTANTS.BISHOP:
                        if(++pCounter > 2) {
                            // More than two bishops):
                            // Theoretically possible to mate or to force mate.
                            return false;
                        }
                        else if (pCounter > 1)
                        {
                            // We have found another piece (knight or bishop) 
                            // before this one:
                            if (getPieceType(lastPiece) != CONSTANTS.BISHOP)
                                // A knight and a bishop:
                                // Theoretically possible to mate
                                // or to force mate.
                                return false;
                            
                            // Two bishops:
                            if (getPieceAt(x,y) == lastPiece)
                                // Two Bishops with the same "pieceColor":
                                // Possible to force checkmate.
                                return false;
                            
                            // One bishop each:
                            if (getSquareColorAt(x,y) != lastPieceSquareColor)
                                // One bishop each on squares of different
                                // colors: Theoretically Possible to mate.
                                return false;
                        }
                        else
                        {
                            // This is the first piece (bishop) that we find: Save it.
                            lastPieceSquareColor = getSquareColorAt(x,y);
                            lastPiece = piece;
                        }
                        break;
                }
            }
        }
        return true;
    }
}
