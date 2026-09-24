/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.asdfjkl.jfxchess.lib;

//import org.asdfjkl.jfxchess.gui.PgnDatabaseEntry;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class TestCases {

    public void fenTest() {

        System.out.println("TEST: fen reading & parsing");
        boolean allPassed = true;

        String[] fens = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2"
        };

        for (int i = 0; i < fens.length; i++) {
            Board b = new Board(fens[i]);
            String fenOut = b.fen();
            if (fens[i].equals(fenOut)) {
                System.out.println("testing fen " + (i + 1) + " ... pass");
            } else {
                System.out.println("testing fen " + (i + 1) + " ... FAIL");
                System.out.println("  in : " + fens[i]);
                System.out.println("  out: " + fenOut);
                System.out.println(b);
                allPassed = false;
            }
        }

        if (!allPassed) {
            throw new RuntimeException("fenTest failed");
        }
    }

    public static String getPgnPath(String filename) {
        File f = new File(filename);
        if (f.exists()) return f.getAbsolutePath();

        String[] candidatePaths = {
            "src/main/resources/pgn/" + filename,
            "src/main/resources/" + filename,
            "src/test/resources/pgn/" + filename,
            "src/test/resources/" + filename,
            "resources/pgn/" + filename,
            "pgn/" + filename
        };
        for (String p : candidatePaths) {
            f = new File(p);
            if (f.exists()) return f.getAbsolutePath();
        }

        URL url = TestCases.class.getClassLoader().getResource("pgn/" + filename);
        if (url == null) {
            url = TestCases.class.getClassLoader().getResource(filename);
        }
        if (url != null) {
            if ("file".equals(url.getProtocol())) {
                try {
                    return new File(url.toURI()).getAbsolutePath();
                } catch (Exception e) {
                    return new File(url.getPath()).getAbsolutePath();
                }
            } else {
                try {
                    File tempFile = File.createTempFile("chess_" + filename, ".pgn");
                    tempFile.deleteOnExit();
                    try (InputStream in = url.openStream();
                         FileOutputStream out = new FileOutputStream(tempFile)) {
                        in.transferTo(out);
                    }
                    return tempFile.getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return filename;
    }

    private int countMoves(Board b, int depth) {
        ArrayList<Move> mvs = b.legalMoves();
        if (depth == 0) {
            return mvs.size();
        } else if (depth >= 3) {
            return mvs.parallelStream().mapToInt(mi -> {
                Board copy = b.makeCopy();
                copy.apply(mi);
                ArrayList<Move> mvs2 = copy.legalMoves();
                return mvs2.parallelStream().mapToInt(mi2 -> {
                    Board copy2 = copy.makeCopy();
                    copy2.apply(mi2);
                    return countMovesSerial(copy2, depth - 2);
                }).sum();
            }).sum();
        } else {
            return mvs.parallelStream().mapToInt(mi -> {
                Board copy = b.makeCopy();
                copy.apply(mi);
                return countMovesSerial(copy, depth - 1);
            }).sum();
        }
    }

    private int countMovesSerial(Board b, int depth) {
        ArrayList<Move> mvs = b.legalMoves();
        if (depth == 0) {
            return mvs.size();
        } else {
            int count = 0;
            for (Move mi : mvs) {
                b.apply(mi);
                int cnt_i = countMovesSerial(b.makeCopy(), depth - 1);
                count += cnt_i;
                b.undo();
            }
            return count;
        }
    }

    private boolean testPerft(String fen, int depth, int perftNumber, int expected) {
        Board b = new Board(fen);
        int computed = countMoves(b, depth);
        if (computed == expected) {
            System.out.println("testing perft " + perftNumber + " ... pass");
            return true;
        } else {
            System.out.println("testing perft " + perftNumber + " ... FAIL");
            System.out.println("  fen     : " + fen);
            System.out.println("  expected: " + expected);
            System.out.println("  computed: " + computed);
            return false;
        }
    }

    public void runPerfT() {

        System.out.println("TEST: PerfT");
        boolean allPassed = true;

        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 0, 1, 20)) allPassed = false;
        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 1, 2, 400)) allPassed = false;
        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 2, 3, 8902)) allPassed = false;
        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 3, 4, 197281)) allPassed = false;
        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 4, 5, 4865609)) allPassed = false;

        // "Kiwipete" by Peter McKenzie, great for identifying bugs
        // perft 1 - 5
        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 0, 1, 48)) allPassed = false;
        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 1, 2, 2039)) allPassed = false;
        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 2, 3, 97862)) allPassed = false;
        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 3, 4, 4085603)) allPassed = false;

        if (!testPerft("8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67", 0, 1, 50)) allPassed = false;
        if (!testPerft("8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67", 1, 2, 279)) allPassed = false;

        if (!testPerft("rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3", 4, 5, 11139762)) allPassed = false;
        if (!testPerft("rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3", 4, 5, 11139762)) allPassed = false;

        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 0, 1, 44)) allPassed = false;
        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 1, 2, 1486)) allPassed = false;
        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 2, 3, 62379)) allPassed = false;
        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 3, 4, 2103487)) allPassed = false;
        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 4, 5, 89941194)) allPassed = false;

        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 5, 6, 119060324)) allPassed = false;

        if (!testPerft("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0", 5, 6, 11030083)) allPassed = false;
        if (!testPerft("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0", 6, 7, 178633661)) allPassed = false;

        if (!testPerft("8/7p/p5pb/4k3/P1pPn3/8/P5PP/1rB2RK1 b - d3 0 28", 5, 6, 38633283)) allPassed = false;

        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 4, 5, 193690690)) allPassed = false;

        if (!allPassed) {
            throw new RuntimeException("runPerfT failed");
        }
    }

    public void runSanTest() {

        System.out.println("TEST: san computation");
        boolean allPassed = true;

        String[] expectedB0 = {
            "a4", "a3", "b4", "b3", "d4", "d3", "e4", "e3", "f3", "g4",
            "g3", "Na3", "Nf3", "Nh3", "Rc3c4", "Rd3", "Re3", "Rcf3", "Rg3", "Rh3",
            "Rb3", "Ra3", "Rff5", "Rf6", "Rxf7", "Rff3", "Rg4", "Rh4", "Re4", "Rd4",
            "Rfc4", "Rb4", "Ra4", "Rc6", "Rxc7", "Rc5c4", "Rd5", "Re5", "Rcf5", "Rg5",
            "Rh5", "Rb5", "Ra5"
        };
        Board b0 = new Board("rnbqkbnr/pppppppp/8/2R5/5R2/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b0Legals = b0.legalMoves();
        ArrayList<String> b0Errors = new ArrayList<>();
        boolean[] b0Hit = new boolean[expectedB0.length];
        if (b0Legals.size() != expectedB0.length) {
            b0Errors.add("expected " + expectedB0.length + " legal moves, but got " + b0Legals.size());
        }
        for (int i = 0; i < b0Legals.size(); i++) {
            Move mi = b0Legals.get(i);
            String san = b0.san(mi);
            if (i < expectedB0.length) {
                if (san.equals(expectedB0[i])) {
                    b0Hit[i] = true;
                } else {
                    b0Errors.add("mismatch at index " + i + ": expected '" + expectedB0[i] + "', got '" + san + "'");
                    for (int j = 0; j < expectedB0.length; j++) {
                        if (!b0Hit[j] && san.equals(expectedB0[j])) {
                            b0Hit[j] = true;
                            break;
                        }
                    }
                }
            } else {
                b0Errors.add("unexpected extra move at index " + i + ": '" + san + "'");
            }
        }
        for (int i = 0; i < expectedB0.length; i++) {
            if (!b0Hit[i]) {
                b0Errors.add("expected move not hit: '" + expectedB0[i] + "' (index " + i + ")");
            }
        }
        if (b0Errors.isEmpty()) {
            System.out.println("testing san 1 ... pass");
        } else {
            System.out.println("testing san 1 ... FAIL");
            for (String err : b0Errors) {
                System.out.println("  " + err);
            }
            allPassed = false;
        }

        String[] expectedB1 = {
            "a4", "a3", "b4", "b3", "d4", "d3", "e4", "e3", "f4", "f3",
            "g4", "g3", "Na3", "Nf3", "Nh3", "R3c4", "Rd3", "Re3", "Rf3", "Rg3",
            "Rh3", "Rb3", "Ra3", "Rc6", "Rxc7", "R5c4", "Rd5", "Re5", "Rf5", "Rg5",
            "Rh5", "Rb5", "Ra5"
        };
        Board b1 = new Board("rnbqkbnr/pppppppp/8/2R5/8/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b1Legals = b1.legalMoves();
        ArrayList<String> b1Errors = new ArrayList<>();
        boolean[] b1Hit = new boolean[expectedB1.length];
        if (b1Legals.size() != expectedB1.length) {
            b1Errors.add("expected " + expectedB1.length + " legal moves, but got " + b1Legals.size());
        }
        for (int i = 0; i < b1Legals.size(); i++) {
            Move mi = b1Legals.get(i);
            String san = b1.san(mi);
            if (i < expectedB1.length) {
                if (san.equals(expectedB1[i])) {
                    b1Hit[i] = true;
                } else {
                    b1Errors.add("mismatch at index " + i + ": expected '" + expectedB1[i] + "', got '" + san + "'");
                    for (int j = 0; j < expectedB1.length; j++) {
                        if (!b1Hit[j] && san.equals(expectedB1[j])) {
                            b1Hit[j] = true;
                            break;
                        }
                    }
                }
            } else {
                b1Errors.add("unexpected extra move at index " + i + ": '" + san + "'");
            }
        }
        for (int i = 0; i < expectedB1.length; i++) {
            if (!b1Hit[i]) {
                b1Errors.add("expected move not hit: '" + expectedB1[i] + "' (index " + i + ")");
            }
        }
        if (b1Errors.isEmpty()) {
            System.out.println("testing san 2 ... pass");
        } else {
            System.out.println("testing san 2 ... FAIL");
            for (String err : b1Errors) {
                System.out.println("  " + err);
            }
            allPassed = false;
        }

        if (!allPassed) {
            throw new RuntimeException("runSanTest failed");
        }
    }

    public void runBitSetTest() {
        System.out.println("TEST: bit setting in int values");
        // e.g. distance one, i.e. index 1 (=left, up, down, right square) has
        // value 0x1C = MSB 00011100 LSB, i.e. king, queen, rook can
        // potentially attack
        // 0              Knight
        // 1              Bishop
        // 2              Rook
        // 3              Queen
        // 4              King
        Board b_temp = new Board();
        int attackIdx1 = CONSTANTS.ATTACK_TABLE[1];
        boolean[] expected = { false, false, true, true, true };
        String[] bitNames = { "knight", "bishop", "rook", "queen", "king" };

        ArrayList<String> errors = new ArrayList<>();
        for (int k = 0; k < expected.length; k++) {
            boolean actual = b_temp.isKthBitSet(attackIdx1, k);
            if (actual == expected[k]) {
                System.out.println("testing bit " + k + " (" + bitNames[k] + ") ... pass");
            } else {
                System.out.println("testing bit " + k + " (" + bitNames[k] + ") ... FAIL");
                errors.add("bit " + k + " (" + bitNames[k] + "): expected " + expected[k] + ", got " + actual);
            }
        }

        if (!errors.isEmpty()) {
            System.out.println("FAIL");
            for (String err : errors) {
                System.out.println("  " + err);
            }
            throw new RuntimeException("runBitSetTest failed");
        }
    }

    public boolean comparePgnStrings(String pgn1, String pgn2) {
        if (pgn1 == null && pgn2 == null) {
            return true;
        }
        if (pgn1 == null || pgn2 == null) {
            System.out.println("FAIL: one of the PGN strings is null");
            System.out.println("PGN 1:\n" + pgn1);
            System.out.println("PGN 2:\n" + pgn2);
            return false;
        }

        String norm1 = pgn1.replaceAll("\\s+", "");
        String norm2 = pgn2.replaceAll("\\s+", "");

        if (norm1.equals(norm2)) {
            return true;
        } else {
            System.out.println("FAIL: PGN strings do not match (ignoring whitespace/line-breaks)");
            System.out.println("Expected:\n" + pgn1);
            System.out.println("Generated:\n" + pgn2);
            return false;
        }
    }

    public void runPgnPrintTest() {

        System.out.println("TEST: simple PGN printing");
        Game g = new Game();
        g.setHeader("Event", "Knaurs Schachbuch");
        g.setHeader("Site", "Paris");
        g.setHeader("Date", "1859.??.??");
        g.setHeader("Round", "1");
        g.setHeader("White", "Morphy");
        g.setHeader("Black", "NN");
        g.setHeader("Result", "1-0");
        g.setHeader("ECO", "C56");

        Board rootBoard = new Board(true);
        g.getRootNode().setBoard(rootBoard);

        g.applyMove(new Move("e2e4"));
        g.applyMove(new Move("e7e5"));
        g.applyMove(new Move("g1f3"));
        g.applyMove(new Move("b8c6"));
        g.applyMove(new Move("f1c4"));
        g.applyMove(new Move("g8f6"));
        g.applyMove(new Move("d2d4"));
        g.applyMove(new Move("e5d4"));
        g.applyMove(new Move("e1g1"));
        g.applyMove(new Move("f6e4"));

        PgnPrinter printer = new PgnPrinter();
        String generatedPgn = printer.printGame(g);
        String expectedPgn = "[Event \"Knaurs Schachbuch\"]\n" +
                "[Site \"Paris\"]\n" +
                "[Date \"1859.??.??\"]\n" +
                "[Round \"1\"]\n" +
                "[White \"Morphy\"]\n" +
                "[Black \"NN\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"C56\"]\n" +
                "\n" +
                "1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6 4. d4 exd4 5. O-O Nxe4 * ";

        if (comparePgnStrings(expectedPgn, generatedPgn)) {
            System.out.println("testing simple pgn print ... pass");
        } else {
            System.out.println("testing simple pgn print ... FAIL");
            throw new RuntimeException("runPgnPrintTest failed");
        }
    }

    public void pgnScanTest() {

        System.out.println("TEST: scanning PGN for game offsets");
        String pgnFile = getPgnPath("test_pgn_03.pgn");
        PgnReader reader = new PgnReader();

        ArrayList<Long> offsets = reader.scanPgn(pgnFile);

        String[] expectedLines = {
            "[Event \"Barbera Open\"]",
            "[Event \"Hastings\"]",
            "[Event \"Reykjavik Open\"]",
            "[Event \"Barbera Open\"]",
            "[Event \"London Chess Classic\"]",
            "[Event \"Wijk aan Zee\"]",
            "[Event \"Barbera Open\"]",
            "[Event \"Baden-Baden\"]",
            "[Event \"Dortmund\"]",
            "[Event \"St. Petersburg\"]"
        };

        ArrayList<String> errors = new ArrayList<>();
        if (offsets.size() < expectedLines.length) {
            errors.add("expected at least " + expectedLines.length + " offsets, but got " + offsets.size());
        }

        int checkCount = Math.min(expectedLines.length, offsets.size());
        for (int i = 0; i < checkCount; i++) {
            long offset_i = offsets.get(i);
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(pgnFile, "r");
                try {
                    raf.seek(offset_i);
                    String line = raf.readLine();
                    if (expectedLines[i].equals(line)) {
                        System.out.println("testing offset " + (i + 1) + " ... pass");
                    } else {
                        System.out.println("testing offset " + (i + 1) + " ... FAIL");
                        errors.add("mismatch at index " + i + ": expected '" + expectedLines[i] + "', got '" + line + "'");
                    }
                } catch (IOException e) {
                    System.out.println("testing offset " + (i + 1) + " ... FAIL");
                    errors.add("IOException reading offset " + i + " (" + offset_i + "): " + e.getMessage());
                }
            } catch (FileNotFoundException e) {
                System.out.println("testing offset " + (i + 1) + " ... FAIL");
                errors.add("FileNotFoundException: " + e.getMessage());
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

        if (!errors.isEmpty()) {
            System.out.println("FAIL");
            for (String err : errors) {
                System.out.println("  " + err);
            }
            throw new RuntimeException("pgnScanTest failed");
        }
    }


    public void pgnReadGameTest() {

        System.out.println("TEST: reading single PGN game");
        String pgnFile = getPgnPath("test_pgn_01.pgn");
        String expectedPgn = "[Event \"Paris Opera\"]\n" +
                "[Site \"Paris FRA\"]\n" +
                "[Date \"1858.10.21\"]\n" +
                "[Round \"1\"]\n" +
                "[White \"Morphy, Paul\"]\n" +
                "[Black \"Duke of Brunswick and Count Isouard\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"C41\"]\n\n" +
                "1. e4 e5 2. Nf3 d6 3. d4 Bg4 4. dxe5 Bxf3 5. Qxf3 dxe5 6. Bc4 Nf6 7. Qb3 Qe7 8. " +
                "Nc3 c6 9. Bg5 b5 10. Nxb5 cxb5 11. Bxb5+ Nbd7 12. O-O-O Rd8 13. Rxd7 Rxd7 14. " +
                "Rd1 Qe6 15. Bxd7+ Nxd7 16. Qb8+ Nxb8 17. Rd8# 1-0";

        OptimizedRandomAccessFile raf = null;
        PgnReader reader = new PgnReader();
        PgnPrinter printer = new PgnPrinter();
        boolean passed = false;
        try {
            raf = new OptimizedRandomAccessFile(pgnFile, "r");
            Game g = reader.readGame(raf);
            String pgn = printer.printGame(g);
            if (comparePgnStrings(expectedPgn, pgn)) {
                System.out.println("testing read single pgn game ... pass");
                passed = true;
            } else {
                System.out.println("testing read single pgn game ... FAIL");
            }
        } catch (IOException e) {
            System.out.println("testing read single pgn game ... FAIL");
            System.out.println("FAIL: IOException while reading PGN file: " + e.getMessage());
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

        if (!passed) {
            throw new RuntimeException("pgnReadGameTest failed");
        }
    }

    public void pgnReadMiddleGTest() {

        System.out.println("TEST: reading all games from test_pgn_02.pgn");
        String middleg = getPgnPath("test_pgn_02.pgn");

        OptimizedRandomAccessFile raf = null;
        PgnReader reader = new PgnReader();
        ArrayList<Long> offsets = reader.scanPgn(middleg);

        boolean allPassed = true;
        try {
            raf = new OptimizedRandomAccessFile(middleg, "r");
            for (int i = 0; i < offsets.size(); i++) {
                long offset_i = offsets.get(i);
                raf.seek(offset_i);
                Game g = reader.readGame(raf);
                if (g != null && g.getRootNode() != null) {
                    System.out.println("testing read game " + (i + 1) + " ... pass");
                } else {
                    System.out.println("testing read game " + (i + 1) + " ... FAIL");
                    allPassed = false;
                }
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException while reading " + middleg + ": " + e.getMessage());
            e.printStackTrace();
            allPassed = false;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!allPassed) {
            throw new RuntimeException("pgnReadMiddleGTest failed");
        }
    }

    public void pgnReadAllMillBaseTest() {

        System.out.println("TEST: reading all games from test_pgn_03.pgn");
        String millbase = getPgnPath("test_pgn_03.pgn");
        PgnReader reader = new PgnReader();

        ArrayList<Long> offsets = reader.scanPgn(millbase);
        if (offsets.size() == 12) {
            System.out.println("testing scan offsets count (12) ... pass");
        } else {
            System.out.println("testing scan offsets count ... FAIL (expected 12, got " + offsets.size() + ")");
            throw new RuntimeException("pgnReadAllMillBaseTest scan failed");
        }

        OptimizedRandomAccessFile raf = null;
        boolean allPassed = true;
        try {
            raf = new OptimizedRandomAccessFile(millbase, "r");
            for (int i = 0; i < offsets.size(); i++) {
                long offset_i = offsets.get(i);
                raf.seek(offset_i);
                Game g = reader.readGame(raf);
                if (g == null || g.getRootNode() == null) {
                    allPassed = false;
                    System.out.println("testing read game " + (i + 1) + " ... FAIL");
                }
            }
            if (allPassed) {
                System.out.println("testing read all " + offsets.size() + " games ... pass");
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException while reading " + millbase + ": " + e.getMessage());
            e.printStackTrace();
            allPassed = false;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!allPassed) {
            throw new RuntimeException("pgnReadAllMillBaseTest failed");
        }
    }

    // using file open/close
    public void pgnReadSingleEntryTestOpenClose() {

        System.out.println("TEST: scanning offsets from PGN, and reading each header w/ multiple fopen/close");
        String millbase = getPgnPath("test_pgn_03.pgn");
        PgnReader reader = new PgnReader();

        ArrayList<Long> offsets = reader.scanPgn(millbase);
        int matchCount = 0;
        for (int i = 0; i < offsets.size(); i++) {
            long offset_i = offsets.get(i);
            HashMap<String, String> header = reader.readSingleHeader(millbase, offset_i);
            if ("Barbera Open".equals(header.get("Event"))) {
                matchCount += 1;
            }
        }

        if (matchCount == 3) {
            System.out.println("testing matching 'Barbera Open' headers (3) ... pass");
        } else {
            System.out.println("testing matching 'Barbera Open' headers ... FAIL (expected 3, got " + matchCount + ")");
            throw new RuntimeException("pgnReadSingleEntryTestOpenClose failed");
        }
    }

    // using raf that is kept open
    public void pgnReadSingleEntryTestSeekWithinRAF() {

        System.out.println("TEST: scanning offsets from PGN, and reading each header, keeping file open");
        String millbase = getPgnPath("test_pgn_03.pgn");
        PgnReader reader = new PgnReader();

        ArrayList<Long> offsets = reader.scanPgn(millbase);
        OptimizedRandomAccessFile raf = null;
        int matchCount = 0;
        try {
            raf = new OptimizedRandomAccessFile(millbase, "r");
            for (int i = 0; i < offsets.size(); i++) {
                long offset_i = offsets.get(i);
                HashMap<String, String> header = reader.readSingleHeader(raf, offset_i);
                if ("Barbera Open".equals(header.get("Event"))) {
                    matchCount += 1;
                }
            }
            if (matchCount == 3) {
                System.out.println("testing matching 'Barbera Open' headers (3) ... pass");
            } else {
                System.out.println("testing matching 'Barbera Open' headers ... FAIL (expected 3, got " + matchCount + ")");
                throw new RuntimeException("pgnReadSingleEntryTestSeekWithinRAF failed");
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException reading " + millbase + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("pgnReadSingleEntryTestSeekWithinRAF failed");
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

    public void readGamesByStringTest() {

        System.out.println("TEST: reading PGN game from string");
        String s = "[Event \"Berlin\"]\n" +
                "[Site \"Berlin GER\"]\n" +
                "[Date \"1852.??.??\"]\n" +
                "[Round \"?\"]\n" +
                "[White \"Adolf Anderssen\"]\n" +
                "[Black \"Jean Dufresne\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"C52\"]\n" +
                "[BlackElo \"?\"]\n" +
                "[EventDate \"?\"]\n" +
                "[WhiteElo \"?\"]\n" +
                "[PlyCount \"47\"]\n" +
                "\n" +
                "1.e4 e5 2.Nf3 Nc6 3.Bc4 Bc5 4.b4 Bxb4 5.c3 Ba5 6.d4 exd4 7.O-O\n" +
                "d3 8.Qb3 Qf6 9.e5 Qg6 10.Re1 Nge7 11.Ba3 b5 12.Qxb5 Rb8 13.Qa4\n" +
                "Bb6 14.Nbd2 Bb7 15.Ne4 Qf5 16.Bxd3 Qh5 17.Nf6+ gxf6 18.exf6\n" +
                "Rg8 19.Rad1 Qxf3 20.Rxe7+ Nxe7 21.Qxd7+ Kxd7 22.Bf5+ Ke8\n" +
                "23.Bd7+ Kf8 24.Bxe7# 1-0";
        PgnReader reader = new PgnReader();
        PgnPrinter printer = new PgnPrinter();
        Game g = reader.readGame(s);
        String pgnOut = printer.printGame(g);
        if (comparePgnStrings(s, pgnOut)) {
            System.out.println("testing read pgn from string ... pass");
        } else {
            System.out.println("testing read pgn from string ... FAIL");
            throw new RuntimeException("readGamesByStringTest failed");
        }

    }

    public void runPosHashTest() {

        System.out.println("TEST: reading single PGN game, trying to find starting pos after 1d4 by pos hash");
        Board b1 = new Board("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1");
        Board b2 = new Board("rnbqkbnr/ppppppp1/8/7p/3P4/8/PPP1PPPP/RNBQKBNR w KQkq h6 0 2");
        long key1 = b1.getPositionHash();
        long key2 = b2.getPositionHash();

        final boolean expectedResult1 = true;
        final boolean expectedResult2 = false;

        String kingbase = getPgnPath("test_pgn_04.pgn");
        OptimizedRandomAccessFile raf = null;
        PgnReader reader = new PgnReader();
        boolean passed = false;
        ArrayList<String> errors = new ArrayList<>();

        try {
            raf = new OptimizedRandomAccessFile(kingbase, "r");
            Game g = reader.readGame(raf);
            boolean actualResult1 = g.containsPosition(key1, 0, 100);
            boolean actualResult2 = g.containsPosition(key2, 0, 100);

            if (actualResult1 == expectedResult1) {
                System.out.println("testing pos hash key 1 (1.d4) ... pass");
            } else {
                System.out.println("testing pos hash key 1 (1.d4) ... FAIL");
                errors.add("key1 search: expected " + expectedResult1 + ", got " + actualResult1 + " (key=" + key1 + ")");
            }

            if (actualResult2 == expectedResult2) {
                System.out.println("testing pos hash key 2 (non-matching) ... pass");
            } else {
                System.out.println("testing pos hash key 2 (non-matching) ... FAIL");
                errors.add("key2 search: expected " + expectedResult2 + ", got " + actualResult2 + " (key=" + key2 + ")");
            }

            if (errors.isEmpty()) {
                passed = true;
            }
        } catch (IOException e) {
            errors.add("IOException: " + e.getMessage());
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

        if (!passed) {
            System.out.println("FAIL");
            for (String err : errors) {
                System.out.println("  " + err);
            }
            throw new RuntimeException("runPosHashTest failed");
        }
    }


    public void runZobristTest() {

        System.out.println("TEST: zobrist hashing");

        String[][] testCases = {
            {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "463b96181691fc9c"},
            {"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", "823c9b50fd114196"},
            {"rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2", "756b94461c50fb0"},
            {"rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2", "662fafb965db29d4"},
            {"rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3", "22a48b5a8e47ff78"},
            {"rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 0 3", "652a607ca3f242c1"},
            {"rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 0 4", "fdd303c946bdd9"},
            {"rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3", "3c8123ea7b067637"},
            {"rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4", "5c3f9b829b279560"}
        };

        ArrayList<String> errors = new ArrayList<>();
        for (int i = 0; i < testCases.length; i++) {
            String fen = testCases[i][0];
            String expectedHex = testCases[i][1];
            long expectedKey = Long.parseUnsignedLong(expectedHex, 16);

            Board b = new Board(fen);
            long actualKey = b.getZobrist();

            if (actualKey == expectedKey) {
                System.out.println("testing zobrist " + (i + 1) + " ... pass");
            } else {
                System.out.println("testing zobrist " + (i + 1) + " ... FAIL");
                errors.add("case " + (i + 1) + " (" + fen + "): expected " + expectedHex + " (" + expectedKey + "), got " + Long.toHexString(actualKey) + " (" + actualKey + ")");
            }
        }

        if (!errors.isEmpty()) {
            System.out.println("FAIL");
            for (String err : errors) {
                System.out.println("  " + err);
            }
            throw new RuntimeException("runZobristTest failed");
        }
    }

    public void pgnMoveAmbiguityUTFTest() {

        System.out.println("TEST: PGN stress test (unicode, comments, disambiguations)");
        String pgnFile = getPgnPath("test_pgn_05.pgn");

        String expectedGame1 = "[Event \"チェス世界選手権 1997 ★ Deep Blue vs Kasparov\"]\n" +
                "[Site \"Köln & München, Deutschland Übersee-Halle № 42\"]\n" +
                "[Date \"1997.05.11\"]\n" +
                "[Round \"6\"]\n" +
                "[White \"Deep Blue (IBM 计算机)\"]\n" +
                "[Black \"Каспаров, Гарри Кимович\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"B17\"]\n\n" +
                "1. e4 c6 2. d4 d5 3. Nc3 dxe4 4. Nxe4 Nd7 { 【重要局】Каспаров выбирает надёжную защиту Каро-Канн. } " +
                "5. Ng5 Ngf6 { Ein scharfer Eröffnungszweig mit typischer Figurenentwicklung! } " +
                "6. Bd3 e6 7. N1f3 { ★ N1f3! Der Springer springt von g1 nach f3: Wunderschöne Springer-Disambiguierung! } " +
                "h6 8. Nxe6 { ⚡ Жертва коня! Ein spektakuläres Figurenopfer erschüttert die schwarze Königsstellung. } " +
                "Qe7 9. O-O fxe6 10. Bg6+ Kd8 11. Bf4 b5 12. a4 Bb7 13. Re1 Nd5 14. Bg3 Kc8 15. axb5 cxb5 16. Qd3 Bc6 " +
                "17. Bf5 exf5 18. Rxe7 Bxe7 19. c4 { 【終局】Белые побеждают в матче! Großartiger Sieg für den Schachcomputer. 1-0 } 1-0";

        String expectedGame2 = "[Event \"Матч за звание чемпиона мира по шахматам 1972 ⚡\"]\n" +
                "[Site \"Рейкьявик, Исландия — Laugardalshöll № 6\"]\n" +
                "[Date \"1972.07.23\"]\n" +
                "[Round \"6\"]\n" +
                "[White \"Фишер, Роберт Джеймс\"]\n" +
                "[Black \"Спасский, Борис Васильевич\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"D59\"]\n\n" +
                "1. c4 e6 2. Nf3 d5 3. d4 Nf6 4. Nc3 Be7 5. Bg5 O-O 6. e3 h6 7. Bh4 b6 8. cxd5 Nxd5 9. Bxe7 Qxe7 " +
                "10. Nxd5 exd5 11. Rc1 Be6 12. Qa4 c5 13. Qa3 Rc8 14. Bb5 a6 15. dxc5 bxc5 16. O-O Ra7 17. Be2 Nd7 " +
                "18. Nd4 Qf8 19. Nxe6 fxe6 20. e4 d4 21. f4 Qe7 22. e5 Rb8 23. Bc4 Kh8 24. Qh3 Nf8 25. b3 a5 26. f5 exf5 " +
                "27. Rxf5 Nh7 28. Rcf1 { ⚡ Doppelung auf der f-Linie: Rcf1! } Qd8 29. Qg3 Re7 30. h4 Rbb7 " +
                "{ ♜ Ладья b8 переходит на b7 для защиты: Rbb7. } 31. e6 Rbc7 32. Qe5 Qe8 33. a4 Qd8 34. R1f2 " +
                "{ ★ Turmmanöver auf der ersten Reihe: R1f2! } Qe8 35. R2f3 " +
                "{ ➜ Und weiter: R2f3! Weiße Schwerfiguren dominieren das Brett. } Qd8 36. Bd3 Qe8 37. Qe4 Nf6 " +
                "38. Rxf6 gxf6 39. Rxf6 Kg8 40. Bc4 Kh8 41. Qf4 " +
                "{ 【名局】Победа белых! Boris Spassky klatschte Beifall für diesen Meisterstreich. 1-0 } 1-0";

        String[] expectedGames = { expectedGame1, expectedGame2 };

        PgnReader reader = new PgnReader();
        PgnPrinter printer = new PgnPrinter();
        ArrayList<Long> offsets = reader.scanPgn(pgnFile);

        if (offsets.size() != expectedGames.length) {
            System.out.println("testing scan offsets count ... FAIL (expected " + expectedGames.length + ", got " + offsets.size() + ")");
            throw new RuntimeException("pgnStressTest scan failed");
        }

        OptimizedRandomAccessFile raf = null;
        boolean allPassed = true;
        try {
            raf = new OptimizedRandomAccessFile(pgnFile, "r");
            for (int i = 0; i < offsets.size(); i++) {
                raf.seek(offsets.get(i));
                Game g = reader.readGame(raf);
                String printed = printer.printGame(g);
                if (comparePgnStrings(expectedGames[i], printed)) {
                    System.out.println("testing stress game " + (i + 1) + " ... pass");
                } else {
                    System.out.println("testing stress game " + (i + 1) + " ... FAIL");
                    allPassed = false;
                }
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException while reading " + pgnFile + ": " + e.getMessage());
            e.printStackTrace();
            allPassed = false;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!allPassed) {
            throw new RuntimeException("pgnStressTest failed");
        }
    }

}
