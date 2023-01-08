package io.github.asdfjkl.jchesslib;

import static org.junit.Assert.*;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Unit tests for jchesslib.
 */
public class JchesslibTest
{

    @Test
    public void fenTest() {

        System.out.println("TEST: fenTest");
        // starting position
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        Board b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);
        assertEquals(fen, b.fen());

        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);
        assertEquals(fen, b.fen());

        fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2";
        b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);
        assertEquals(fen, b.fen());

        fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
        b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);
        assertEquals(fen, b.fen());

    }

    private int countMoves(Board b, int depth) {
        int count = 0;
        ArrayList<Move> mvs = b.legalMoves();
        if(depth == 0) {
            return mvs.size();
        } else {
            // recursive case: for each possible move, apply
            // the move, do the recursive call and undo the move
            for(Move mi : mvs ) {
                b.apply(mi);
                int cnt_i = countMoves(b.makeCopy(), depth - 1);
                count += cnt_i;
                b.undo();
            }
            return count;
        }
    }

    @Test
    public void runPerfT() {

        System.out.println("TEST: runPerfT");
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        Board b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 20");
        int c = countMoves(b, 0);
        assertEquals(c, 20);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 400");
        c = countMoves(b, 1);
        System.out.println("computed: " + c);
        assertEquals(c, 400);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 3, expected 8902");
        c = countMoves(b, 2);
        System.out.println("computed: " + c);
        assertEquals(c, 8902);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 4, expected 197281");
        c = countMoves(b, 3);
        System.out.println("computed: " + c);
        assertEquals(c, 197281);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 4865609");
        c = countMoves(b, 4);
        System.out.println("computed: " + c);
        assertEquals(c, 4865609);

        // "Kiwipete" by Peter McKenzie, great for identifying bugs
        // perft 1 - 5
        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 48");
        c = countMoves(b, 0);
        System.out.println("computed: " + c);
        assertEquals(c, 48);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 2039");
        c = countMoves(b, 1);
        System.out.println("computed: " + c);
        assertEquals(c, 2039);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 3, expected 97862");
        c = countMoves(b, 2);
        System.out.println("computed: " + c);
        assertEquals(c, 97862);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 4, expected 4085603");
        c = countMoves(b, 3);
        System.out.println("computed: " + c);
        assertEquals(c, 4085603);

        fen = "8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 50");
        c = countMoves(b, 0);
        System.out.println("computed: " + c);
        assertEquals(c, 50);

        fen = "8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 279");
        c = countMoves(b, 1);
        System.out.println("computed: " + c);
        assertEquals(c, 279);

        fen = "rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 11139762");
        c = countMoves(b, 4);
        System.out.println("computed: " + c);
        assertEquals(c, 11139762);

        fen = "rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 11139762");
        c = countMoves(b, 4);
        System.out.println("computed: " + c);
        assertEquals(c, 11139762);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 44");
        c = countMoves(b, 0);
        System.out.println("computed: " + c);
        assertEquals(c, 44);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 1486");
        c = countMoves(b, 1);
        System.out.println("computed: " + c);
        assertEquals(c, 1486);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 3, expected 62379");
        c = countMoves(b, 2);
        System.out.println("computed: " + c);
        assertEquals(c, 62379);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 4, expected 2103487");
        c = countMoves(b, 3);
        System.out.println("computed: " + c);
        assertEquals(c, 2103487);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 89941194");
        c = countMoves(b, 4);
        System.out.println("computed: " + c);
        assertEquals(c, 89941194);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 6, expected 119060324");
        Instant start = Instant.now();
        c = countMoves(b, 5);
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        System.out.println("PerfT 6: "+timeElapsed+"ms");
        System.out.println("computed: " + c);
        assertEquals(c, 119060324);

        fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 6, expected 11030083");
        c = countMoves(b, 5);
        System.out.println("computed: " + c);
        assertEquals(c, 11030083);

        fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 7, expected 178633661");
        c = countMoves(b, 6);
        System.out.println("computed: " + c);
        assertEquals(c, 178633661);

        fen = "8/7p/p5pb/4k3/P1pPn3/8/P5PP/1rB2RK1 b - d3 0 28";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 6, expected 38633283");
        c = countMoves(b, 5);
        System.out.println("computed: " + c);
        assertEquals(c, 38633283);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 193690690");
        c = countMoves(b, 4);
        System.out.println("computed: " + c);
        assertEquals(c, 193690690);

    }

    @Test
    public void runSanTest() {

        System.out.println("TEST: runSanTest");
        Board b0 = new Board("rnbqkbnr/pppppppp/8/2R5/5R2/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        System.out.println("rnbqkbnr/pppppppp/8/2R5/5R2/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b0Legals = b0.legalMoves();
        for(Move mi : b0Legals) {
            System.out.println(b0.san(mi));
        }
        assertEquals(b0Legals.size(), 43);

        Board b1 = new Board("rnbqkbnr/pppppppp/8/2R5/8/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        System.out.println("rnbqkbnr/pppppppp/8/2R5/8/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b1Legals = b1.legalMoves();
        for(Move mi : b1Legals) {
            System.out.println(b1.san(mi));
        }
        assertEquals(b1Legals.size(), 33);

    }

    @Test
    public void runPgnPrintTest() {

        System.out.println("TEST: runPgnPrintTest");
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
        String gameString = printer.printGame(g);
        String gameStringExpected = "[Event \"Knaurs Schachbuch\"]\n" +
                "[Site \"Paris\"]\n" +
                "[Date \"1859.??.??\"]\n" +
                "[Round \"1\"]\n" +
                "[White \"Morphy\"]\n" +
                "[Black \"NN\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"C56\"]\n" +
                "\n" +
                "1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6 4. d4 exd4 5. O-O Nxe4 * ";
        assertEquals(gameString, gameStringExpected);

    }

    @Test
    public void readGamesByStringTest() {

        System.out.println("TEST: readGamesByStringTest");

        String s = "[Event \"Berlin\"]\n" +
                "[Site \"Berlin GER\"]\n" +
                "[Date \"1852.??.??\"]\n" +
                "[EventDate \"?\"]\n" +
                "[Round \"?\"]\n" +
                "[Result \"1-0\"]\n" +
                "[White \"Adolf Anderssen\"]\n" +
                "[Black \"Jean Dufresne\"]\n" +
                "[ECO \"C52\"]\n" +
                "[WhiteElo \"?\"]\n" +
                "[BlackElo \"?\"]\n" +
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
        String sExpected = "[Event \"Berlin\"]\n" +
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
                "1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. b4 Bxb4 5. c3 Ba5 6. d4 exd4 7. O-O d3 8. Qb3\n" +
                "Qf6 9. e5 Qg6 10. Re1 Nge7 11. Ba3 b5 12. Qxb5 Rb8 13. Qa4 Bb6 14. Nbd2 Bb7 15.\n" +
                "Ne4 Qf5 16. Bxd3 Qh5 17. Nf6+ gxf6 18. exf6 Rg8 19. Rad1 Qxf3 20. Rxe7+ Nxe7\n" +
                "21. Qxd7+ Kxd7 22. Bf5+ Ke8 23. Bd7+ Kf8 24. Bxe7# 1-0 ";
        String sOut = (printer.printGame(g));
        assertEquals(sOut, sExpected);

    }

    @Test
    public void runZobristTest() {

        System.out.println("TEST: runZobristTest");

        Board b = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        long key = b.getZobrist();
        String s = "463b96181691fc9c";
        System.out.println("expected zobrist: 463b96181691fc9c");
        System.out.println("got zobrist.....: " + Long.toHexString(key));
        assertEquals(Long.toHexString(key), s);

        b = new Board("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        key = b.getZobrist();
        System.out.println("expected zobrist: 823c9b50fd114196");
        s = "823c9b50fd114196";
        System.out.println("got zobrist.....: " + Long.toHexString(key));
        assertEquals(Long.toHexString(key), s);

        b = new Board("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2");
        key = b.getZobrist();
        System.out.println("expected zobrist: 756b94461c50fb0");
        s = "756b94461c50fb0";
        System.out.println("got zobrist.....: " + Long.toHexString(key));
        assertEquals(Long.toHexString(key), s);

        b = new Board("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2");
        key = b.getZobrist();
        System.out.println("expected zobrist: 662fafb965db29d4");
        s = "662fafb965db29d4";
        System.out.println("got zobrist.....: " + Long.toHexString(key));
        assertEquals(Long.toHexString(key), s);

        b = new Board("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3");
        key = b.getZobrist();
        System.out.println("expected zobrist: 22a48b5a8e47ff78");
        s = "22a48b5a8e47ff78";
        System.out.println("got zobrist.....: " + Long.toHexString(key));
        assertEquals(Long.toHexString(key), s);

        b = new Board("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 0 3");
        key = b.getZobrist();
        System.out.println("expected zobrist: 652a607ca3f242c1");
        s = "652a607ca3f242c1";
        System.out.println("got zobrist.....: " + Long.toHexString(key));
        assertEquals(Long.toHexString(key), s);

        b = new Board("rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 0 4");
        key = b.getZobrist();
        System.out.println("expected zobrist: fdd303c946bdd9");
        s = "fdd303c946bdd9";
        System.out.println("got zobrist.....: " + Long.toHexString(key));
        assertEquals(Long.toHexString(key), s);

        b = new Board("rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3");
        key = b.getZobrist();
        System.out.println("expected zobrist: 3c8123ea7b067637");
        s = "3c8123ea7b067637";
        System.out.println("got zobrist.....: " + Long.toHexString(key));
        assertEquals(Long.toHexString(key), s);

        b = new Board("rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4");
        key = b.getZobrist();
        System.out.println("expected zobrist: 5c3f9b829b279560");
        s = "5c3f9b829b279560";
        System.out.println("got zobrist.....: " + Long.toHexString(key));
        assertEquals(Long.toHexString(key), s);

    }


}
