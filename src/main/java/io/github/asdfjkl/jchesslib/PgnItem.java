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

public class PgnItem {

    private long offset = 0;
    private long index = 0;
    private String event = "";
    private String site = "";
    private String date = "";
    private String round = "";
    private String white = "";
    private String black = "";
    private String result = "";
    private String eco = "";

    private boolean foundAtLeast1Tag = false;

    public long getOffset() { return offset; }
    public void setOffset(long offset) { this.offset = offset; }

    public long getIndex() { return index; }
    public void setIndex(long index) { this.index = index; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getRound() { return round; }
    public void setRound(String round ) { this.round = round; }

    public String getWhite() { return white; }
    public void setWhite(String white) { this.white = white; }

    public String getBlack() { return black; }
    public void setBlack(String black) { this.black = black; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getEco() { return eco; }
    public void setEco(String eco) { this.eco = eco; }

    public void markValid() {
        foundAtLeast1Tag = true;
    }

    public boolean isValid() {
        return foundAtLeast1Tag;
    }


}