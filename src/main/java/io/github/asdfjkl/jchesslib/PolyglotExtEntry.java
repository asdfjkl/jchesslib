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
 *

 */
public class PolyglotExtEntry {
    long key;
    int move;
    long count;
    int whiteWinPerc;
    int blackWinPerc;
    int drawPerc;
    int avgElo;
    String uci;

    public long getPosCount() { return count; }
    public void setPosCount(long count) { this.count = count; }

    public String getMove() { return uci; }
    public void setMove(String uci) { this.uci = uci; }

    public Integer getWins() { return whiteWinPerc; }
    public void setWins(Integer whiteWinPerc) { this.whiteWinPerc = whiteWinPerc; }

    public Integer getDraws() { return drawPerc; }
    public void setDraws(Integer drawPerc) { this.drawPerc = drawPerc; }

    public Integer getLosses() { return blackWinPerc; }
    public void setLosses(Integer blackWinPerc) { this.blackWinPerc = blackWinPerc; }

    public int getAvgELO() { return avgElo; }
    public void setAvgELO(int avgElo) { this.avgElo = avgElo; }

    @Override
    public String toString() {
        return "" + key + "   " + count + "  " + move + " " + uci + " " + whiteWinPerc + " " + drawPerc + " " + blackWinPerc + " " + avgElo;
    }


}