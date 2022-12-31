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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/*
 * PolyglotExt is a simple bookformat
 * of consecutive entries of 18 bytes each using
 * a similar encoding as the original Polyglot.
 * Other information about the position are stored
 * however, mostly statistics that may be useful
 * for training and opening preparation
 *
 * entries are stored in ascending order w.r.t.
 * the zobrist hash; i.e. same as polyglot,
 * but weights, learn etc. replaced by different data fields
 * moves are encoded the same as in polyglot

 * A Polyglot Ext Entry (18 byte total):
 * <pre>
 | Data Type   |    Entry    |  no. of bytes |
 |-------------|-------------|---------------|
 | uint64      | zobrist     |          8    |
 | uint16      | move        |          2    |
 | uint32      | count       |          4    |
 | uint8       | white win percentage | 1    |
 | uint8       | black win percentage | 1    |
 | uint16      | average elo          | 2    |
 *</pre>
 */
public class PolyglotExt {

    byte[] book;
    /**
     * flag to check if a book has already been loaded
     */
    public boolean readFile = false;
    final char[] promotionPieces = { ' ', 'n', 'b', 'r', 'q'};

    /**
     * load an extended Polyglot book into memory
     * @param file filename of the book
     */
    public void loadBook(File file) {

        OptimizedRandomAccessFile raf = null;
        try {
            long fileLength = file.length();
            raf = new OptimizedRandomAccessFile(file, "r");
            book = new byte[(int) fileLength];
            raf.readFully(book, 0, (int) fileLength);
            readFile = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * given an offset into the file, read an entry of the book
     * @param offset offset into the file
     * @return entry of the book
     */
    public PolyglotExtEntry getEntryFromOffset(int offset) {

        if (book == null || offset > book.length - 18) {
            if(book == null) {
                System.err.println("book == null");
            } else {
                System.err.println("book length mismatch");
            }
            throw new IllegalArgumentException("polyglot ext-book is not loaded or offset out of range");
        }

        byte[] bKey = Arrays.copyOfRange(book, offset, offset + 8);
        byte[] bMove = Arrays.copyOfRange(book, offset + 8, offset + 10);
        byte[] bPosCount = Arrays.copyOfRange(book, offset + 10, offset + 14);
        byte[] bWhiteWin = Arrays.copyOfRange(book, offset + 14, offset + 15);
        //byte[] bDraw = Arrays.copyOfRange(book, offset + 15, offset + 16);
        byte[] bBlackWin = Arrays.copyOfRange(book, offset + 15, offset + 16);
        byte[] bAvgElo = Arrays.copyOfRange(book, offset + 16, offset + 18);

        long key = ByteBuffer.wrap(bKey).getLong();
        int move = ByteBuffer.wrap(bMove).getShort();
        int posCount = ByteBuffer.wrap(bPosCount).getInt();
        int whiteWinPerc = ByteBuffer.wrap(bWhiteWin).get();
        int blackWinPerc = ByteBuffer.wrap(bBlackWin).get();
        int drawPerc = 100 - whiteWinPerc - blackWinPerc;
        int avgElo = ByteBuffer.wrap(bAvgElo).getShort();

        int from = 0;
        int fromRow = 0;
        int fromFile = 0;
        int to = 0;
        int toRow = 0;
        int toFile = 0;
        int promotion = 0;

        from      = (move>>6)&077; // octal!, i.e. = 0x3F, mask 6 bits
        fromRow   = (from>>3)&0x7;
        fromFile  = from&0x7;
        to        = move&077;
        toRow     = (to>>3)&0x7;
        toFile    = to&0x7;
        promotion =(move>>12)&0x7;

        char cFromFile = (char) (fromFile+'a');
        char cFromRow  = (char) (fromRow+'1');
        char cToFile   = (char) (toFile+'a');
        char cToRow    = (char) (toRow+'1');

        StringBuilder sbUci = new StringBuilder().append(cFromFile).append(cFromRow).append(cToFile).append(cToRow);

        if(promotion > 0) {
            sbUci.append(promotionPieces[promotion]);
        }

        String uci = sbUci.toString();
        if(uci.equals("e1h1")) {
            uci = "e1g1";
        }
        if(uci.equals("e1a1")) {
            uci = "e1c1";
        }
        if(uci.equals("e8h8")) {
            uci = "e8g8";
        }
        if(uci.equals("e8a8")) {
            uci = "e8c8";
        }

        PolyglotExtEntry e = new PolyglotExtEntry();
        e.key = key;
        e.move = move;
        e.count = posCount;
        e.whiteWinPerc = whiteWinPerc;
        e.blackWinPerc = blackWinPerc;
        e.drawPerc = drawPerc;
        e.avgElo = avgElo;
        e.uci = uci;
        return e;
    }

    /**
     * find all entries for the position of the supplied board
     * @param board board with the position in question
     * @return ArrayList of PolyglotExt entries
     */
    public ArrayList<PolyglotExtEntry> findEntries(Board board) {

        long zobrist = board.getZobrist();
        return findEntries(zobrist);
    }

    /**
     * find all entries for the position of the supplied zobrist hash
     * @param zobrist position encoded as zobrist hash
     * @return ArrayList of PolyglotExt entries
     */
    public ArrayList<PolyglotExtEntry> findEntries(long zobrist) {

        ArrayList<PolyglotExtEntry> bookEntries = new ArrayList<>();

        if(readFile) {

            int low = 0;
            int high = Integer.divideUnsigned(book.length, 18);

            // find entry fast
            while(Integer.compareUnsigned(low, high) < 0) {
                int middle = Integer.divideUnsigned(low + high, 2);
                PolyglotExtEntry e = getEntryFromOffset(middle*18);
                long middleKey = e.key;
                if(Long.compareUnsigned(middleKey, zobrist) < 0) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            int offset = low;
            int size = Integer.divideUnsigned(book.length, 18);

            // now we have the lowest key pos
            // where a possible entry is. collect all
            while(Integer.compareUnsigned(offset, size) < 0) {
                PolyglotExtEntry e = getEntryFromOffset(offset*18);
                if(Long.compareUnsigned(e.key,zobrist) != 0L) {
                    break;
                }
                bookEntries.add(e);
                offset += 1;
            }
        }
        // sorting by pos count (i.e. no. of times played)
        bookEntries.sort((e1, e2) -> (int) (- e1.count + e2.count));
        return bookEntries;
    }

    /**
     * checks if the position of the supplied board is in the book
     * @param board board with the position
     * @return true if at least one entry is found, false otherwise
     */
    public boolean inBook(Board board) {

        long zobrist = board.getZobrist();
        return inBook(zobrist);

    }

    /**
     * checks if the position of the supplied zobrist hash is in the book
     * @param zobrist hash with the position
     * @return true if at least one entry is found, false otherwise
     */
    public boolean inBook(long zobrist) {

        if(!readFile) {
            return false;
        }

        int low = 0;
        int high = Integer.divideUnsigned(book.length, 18);

        // find entry fast
        while(Integer.compareUnsigned(low, high) < 0) {
            int middle = Integer.divideUnsigned(low + high, 2);
            PolyglotExtEntry e = getEntryFromOffset(middle*18);
            long middleKey = e.key;
            if(Long.compareUnsigned(middleKey, zobrist) < 0L) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        int offset = low;
        int size = Integer.divideUnsigned(book.length, 18);

        // now we have the lowest key pos
        // where a possible entry is.
        if(Integer.compareUnsigned(offset, size) < 0) {
            PolyglotExtEntry e = getEntryFromOffset(offset*18);
            if(Long.compareUnsigned(e.key, zobrist) == 0) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    /**
     * for given zobrist, get all stored moves from the
     * book and select a random move from the possible choices.
     * Return null if there is no move. The random
     * choice has a bias w.r.t. the number of times a move
     * has been played (popular moves are preferred)
     * @param zobrist the position in question
     * @return a random move, encoded as a uci string
     */
    public String getRandomMove(long zobrist) {

        ArrayList<PolyglotExtEntry> entries = findEntries(zobrist);
        if(entries.size() == 0) {
            return null;
        } else {
            int overallCount = 0;
            for(PolyglotExtEntry entry : entries) {
                overallCount += entry.count;
            }
            int idx = (int) (Math.random() * overallCount);
            int tempCount = 0;
            for(PolyglotExtEntry entry : entries) {
                tempCount += entry.count;
                if(idx <= tempCount) {
                    return entry.uci;
                }
            }
            return null;
        }
    }

}

