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
 * wraps information about a game within a PGN file. This includes
 * the seven mandatory PGN headers, ECO classification as well as the
 * index (n-th game in the file) and the absolut offset within the file.
 */
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

    /**
     * get the absolute file offset of the game
     * @return
     */
    public long getOffset() { return offset; }

    /**
     * set the absolute file offset of the game
     * @param offset
     */
    public void setOffset(long offset) { this.offset = offset; }

    /**
     * get the index (i.e. n-th game) of the game
     * @return
     */
    public long getIndex() { return index; }

    /**
     * set the index (i.e. n-th game) of the game
     * @param index
     */
    public void setIndex(long index) { this.index = index; }

    /**
     * get the event tag
     * @return
     */
    public String getEvent() { return event; }

    /**
     * set the event tag
     * @param event
     */
    public void setEvent(String event) { this.event = event; }

    /**
     * get the site tag
     * @return
     */
    public String getSite() { return site; }

    /**
     * set the site tag
     * @param site
     */
    public void setSite(String site) { this.site = site; }

    /**
     * get the date (String formatted to PGN convention)
     * @return
     */
    public String getDate() { return date; }

    /**
     * set the date (String formatted to PGN convention)
     * @param date
     */
    public void setDate(String date) { this.date = date; }

    /**
     * get the round
     * @return
     */
    public String getRound() { return round; }

    /**
     * set the round
     * @param round
     */
    public void setRound(String round ) { this.round = round; }

    /**
     * get the white player name
     * @return
     */
    public String getWhite() { return white; }

    /**
     * set the white player name
     * @param white
     */
    public void setWhite(String white) { this.white = white; }

    /**
     * get the black player name
     * @return
     */
    public String getBlack() { return black; }

    /**
     * set the black player name
     * @param black
     */
    public void setBlack(String black) { this.black = black; }

    /**
     * get the result (string formatted to PGN convention)
     * @return
     */
    public String getResult() { return result; }

    /**
     * set the result (string formatted to PGN convention)
     * @param result
     */
    public void setResult(String result) { this.result = result; }

    /**
     * get the ECO code
     * @return
     */
    public String getEco() { return eco; }

    /**
     * set the ECO code
     * @param eco
     */
    public void setEco(String eco) { this.eco = eco; }

    public void markValid() {
        foundAtLeast1Tag = true;
    }

    public boolean isValid() {
        return foundAtLeast1Tag;
    }


}