# jchesslib: a chess library for Java

## Introduction

jchesslib is a chess library for Java. It supports:
- move generation
- move validation
- managing game (trees)
- PGN import and export
- HTML export
- Polyglot and PolyglotExt opening books

## Installation

Install via [Maven](https://mvnrepository.com) or download the jar [here](http://...)

## Documentation

Javadoc for jchess lib is [here](https://asdfjkl.github.io/jchesslib/)

## Examples

### Board Creation and Moves

Scholar's mate:
````Java
Board board = new Board(true);
Move m1 = new Move("e2e4");
Move m2 = new Move("e7e5");
Move m3 = new Move("d1h5");
Move m4 = new Move("b8c6");
Move m5 = new Move("f1c4");
Move m6 = new Move("g8f6");
Move m7 = new Move("h5f7");

board.apply(m1);
board.apply(m2);
board.apply(m3);
board.apply(m4);
board.apply(m5);
board.apply(m6);
board.apply(m7);

System.out.println(board.isCheckmate());
>> true
````     

### Make and Unmake Moves

Some functions change the board state so that undo is not possible anymore. 
Always call `isUndoAvailable` before undoing a move:

````Java
Board board = new Board(true);
Move m1 = new Move("e2e4");
board.apply(m1);
if(isUndoAvailable()){
    board.undo();
}
````

### Print an ASCII Board

````Java
Board board = new Board(true);
System.out.println(board);

>>> rnbqkbnr
>>> pppppppp
>>> ........
>>> ........
>>> ........
>>> ........
>>> PPPPPPPP
>>> RNBQKBNR
````

### Parse and create FEN strings

````Java
Board board = new Board("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
Move m = new Move("e7e5");
board.apply(m);
System.out.println(board.fen());

>>> rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2
````

### Detect Checkmate, Stalemate and Draw

````Java
Board board = new Board(true);
board.isStalemate();
board.isCheckmate();
board.isInsufficientMaterial();
````

### Detects repetitions

````Java
Game game = new Game();
Board board = new Board(true);
game.getRootNode().setBoard(board);
game.isThreefoldRepetition();
````

### Reading PGNs

Scan a PGN and get file offsets of all games

````Java
String millbase = "/home/user/millionbase-2.22.pgn";
PgnReader reader = new PgnReader();
if(reader.isIsoLatin1(millbase)) {
    reader.setEncodingIsoLatin1();
}
ArrayList<Long> offsets = reader.scanPgn(millbase);
````

Then seek to an offset and read the game

````Java
try {
    raf = new OptimizedRandomAccessFile(millbase, "r");
    offset = offsets.get(10);
    raf.seek(offset);
    Game g = reader.readGame(raf);
} catch (IOException e) {
    e.printStackTrace();
}
````

Scan a PGN and get header information and file offsets of all games

````Java
String millbase = "/home/user/millionbase-2.22.pgn";
PgnReader reader = new PgnReader();
if(reader.isIsoLatin1(millbase)) {
    reader.setEncodingIsoLatin1();
}
ArrayList<PgnItem> entries = reader.scanPgnGetSTR(millbase);
PgnItem entry = entries.get(10):
System.out.println("White: "+entry.getWhite());
````

Then seek to an offset and read the game

````Java
try {
    raf = new OptimizedRandomAccessFile(millbase, "r");
    PgnItem entry = entries.get(10):
    offset = entry.getOffset();
    raf.seek(offset);
    Game g = reader.readGame(raf);
} catch (IOException e) {
    e.printStackTrace();
}
````

Read the first game in a PGN

````Java
String filename = "/home/user/game.pgn";
OptimizedRandomAccessFile raf = null;
PgnReader reader = new PgnReader();
if(reader.isIsoLatin1(filename)) {
    reader.setEncodingIsoLatin1();
}
try {
    raf = new OptimizedRandomAccessFile(kingbase, "r");
    Game g = reader.readGame(raf);
} catch (IOException e) {
    e.printStackTrace();
}
````

Reading a PGN from a string:

````Java
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
````

### Writing PGNs and Render HTML

Writing a PGN to a file or to a String:

````Java
Game g = new Game();
g.setHeader("Event", "Training");
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
System.out.println(printer.printGame(g));
printer.writeGame(g, "temp.pgn");
````

### Open Polyglot Opening Books

Reading a Polyglot book. PolyglotExt books work similar.

````Java
Polyglot pg = new Polyglot();

File file = null;
URL book = getClass().getClassLoader().getResource("books/book.bin");
if(book != null) {
    file = new File(book.getFile());
    pg1.loadBook(file);
}
try {
    PolyglotEntry e = pg.getEntryFromOffset(0x62c20);
    System.out.println(e.uci);
    System.out.println(e.weight);
    System.out.println(e.learn);

    Board b = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    long key = b.getZobrist();
    ArrayList<String> entries = pg.findMoves(key);
    for(String uci : entries) {
        System.out.println(uci);
    }

    entries = pg.findMoves(0x2d3888dac361814aL);
    for(String uci : entries) {
        System.out.println(uci);
    }

    entries = pg.findMoves(0x823c9b50fd114196L);
    for(String uci : entries) {
        System.out.println(uci);
    }

} catch(IllegalArgumentException e) {
    e.printStackTrace();
}
````

## Performance

jchesslib is not optimized for fast move generation, especially legal (not pseudo-legal) 
move generation is slow. It has reasonable performance to process large PGN files. 
Some benchmarks:

| Library                | Perft 6 (ms) |
|------------------------|:------------:|
| Stockfish 14.1         |    0.572     |
| jchesslib              |      18      |
| python-chess (cpython) |     238      |

Processing a large (1.5 GB) PGN File:

| jchesslib                                  | seconds |
|--------------------------------------------|:-------:|
| scan PGN for game offsets                  |    4    |
| read all games                             |   171   |
| read all games and search for a position   |   248   |
| read all games and write to another file   |   518   |

As a comparison:

| Fritz 8                                    | seconds |
| ------------------------------------------ |:-------:|
| read all games and search for a position   |   332   |


## License

jchesslib is licensed under MIT. See LICENSE for the full text.
