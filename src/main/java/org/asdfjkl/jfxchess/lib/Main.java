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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        boolean runAll = false;
        boolean runLogic = false;
        boolean runPerft = false;
        boolean runPgn = false;
        String pgnFile = null;
        boolean printOutput = true;

        for (String arg : args) {
            switch (arg) {
                case "--help":
                case "-h":
                    printHelp();
                    return;
                case "--all":
                case "run-all-tests":
                    runAll = true;
                    break;
                case "--logic-tests":
                    runLogic = true;
                    break;
                case "--perft":
                    runPerft = true;
                    break;
                case "--pgn":
                    runPgn = true;
                    break;
                case "noout":
                    printOutput = false;
                    break;
                default:
                    if (arg.startsWith("-")) {
                        System.err.println("Unknown option: " + arg);
                        printHelp();
                        System.exit(1);
                    } else if (pgnFile == null) {
                        pgnFile = arg;
                    }
                    break;
            }
        }

        if (pgnFile != null) {
            processPgnFile(pgnFile, printOutput);
            return;
        }

        if (!runAll && !runLogic && !runPerft && !runPgn) {
            printHelp();
            return;
        }

        TestCases cases = new TestCases();
        Map<String, Runnable> selectedTests = new LinkedHashMap<>();

        if (runAll || runLogic) {
            selectedTests.put("runBitSetTest", cases::runBitSetTest);
            selectedTests.put("fenTest", cases::fenTest);
            selectedTests.put("runSanTest", cases::runSanTest);
            selectedTests.put("runZobristTest", cases::runZobristTest);
            selectedTests.put("runPosHashTest", cases::runPosHashTest);
        }

        if (runAll || runPgn) {
            selectedTests.put("runPgnPrintTest", cases::runPgnPrintTest);
            selectedTests.put("readGamesByStringTest", cases::readGamesByStringTest);
            selectedTests.put("pgnReadGameTest", cases::pgnReadGameTest);
            selectedTests.put("pgnReadMiddleGTest", cases::pgnReadMiddleGTest);
            selectedTests.put("pgnScanTest", cases::pgnScanTest);
            selectedTests.put("pgnReadSingleEntryTestOpenClose", cases::pgnReadSingleEntryTestOpenClose);
            selectedTests.put("pgnReadSingleEntryTestSeekWithinRAF", cases::pgnReadSingleEntryTestSeekWithinRAF);
            selectedTests.put("pgnReadAllMillBaseTest", cases::pgnReadAllMillBaseTest);
            selectedTests.put("pgnStressTest", cases::pgnMoveAmbiguityUTFTest);
        }

        if (runAll || runPerft) {
            selectedTests.put("runPerfT", cases::runPerfT);
        }

        String suiteName;
        if (runAll) {
            suiteName = "All Tests";
        } else {
            ArrayList<String> parts = new ArrayList<>();
            if (runLogic) parts.add("Logic Tests");
            if (runPgn) parts.add("PGN Tests");
            if (runPerft) parts.add("Perft Tests");
            suiteName = String.join(", ", parts);
        }

        boolean success = runTests(suiteName, selectedTests);
        if (!success) {
            System.exit(1);
        }
    }

    public static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  java -jar jfxchess.jar [OPTIONS]");
        System.out.println("  java -jar jfxchess.jar <pgn-file> [noout]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --all          Run all tests (logic, pgn, and perft)");
        System.out.println("  --logic-tests  Run core logic tests (runBitSetTest, fenTest, runSanTest, runZobristTest, runPosHashTest)");
        System.out.println("  --perft        Run move generation perft tests (runPerfT)");
        System.out.println("  --pgn          Run PGN reading, scanning, and printing tests");
        System.out.println("  --help, -h     Display this help message and exit");
    }

    public static boolean runAllTests() {
        TestCases cases = new TestCases();
        Map<String, Runnable> tests = new LinkedHashMap<>();
        tests.put("runBitSetTest", cases::runBitSetTest);
        tests.put("fenTest", cases::fenTest);
        tests.put("runSanTest", cases::runSanTest);
        tests.put("runZobristTest", cases::runZobristTest);
        tests.put("runPosHashTest", cases::runPosHashTest);
        tests.put("runPgnPrintTest", cases::runPgnPrintTest);
        tests.put("readGamesByStringTest", cases::readGamesByStringTest);
        tests.put("pgnReadGameTest", cases::pgnReadGameTest);
        tests.put("pgnReadMiddleGTest", cases::pgnReadMiddleGTest);
        tests.put("pgnScanTest", cases::pgnScanTest);
        tests.put("pgnReadSingleEntryTestOpenClose", cases::pgnReadSingleEntryTestOpenClose);
        tests.put("pgnReadSingleEntryTestSeekWithinRAF", cases::pgnReadSingleEntryTestSeekWithinRAF);
        tests.put("pgnReadAllMillBaseTest", cases::pgnReadAllMillBaseTest);
        tests.put("pgnStressTest", cases::pgnMoveAmbiguityUTFTest);
        tests.put("runPerfT", cases::runPerfT);

        return runTests("All Tests", tests);
    }

    public static boolean runTests(String suiteName, Map<String, Runnable> tests) {
        System.out.println("================================================================================");
        System.out.println("               JFXChess Library - Test Suite: " + suiteName);
        System.out.println("================================================================================");

        int total = tests.size();
        int passed = 0;
        int failed = 0;
        long totalStartTime = System.currentTimeMillis();
        Map<String, String> results = new LinkedHashMap<>();

        int current = 0;
        for (Map.Entry<String, Runnable> entry : tests.entrySet()) {
            current++;
            String testName = entry.getKey();
            Runnable testMethod = entry.getValue();

            System.out.println(String.format("\n[%d/%d] RUNNING: %s", current, total, testName));
            System.out.println("--------------------------------------------------------------------------------");
            long start = System.currentTimeMillis();
            try {
                testMethod.run();
                long elapsed = System.currentTimeMillis() - start;
                passed++;
                results.put(testName, String.format("pass (%d ms)", elapsed));
                System.out.println(String.format("--> %s: pass (%d ms)", testName, elapsed));
            } catch (Throwable t) {
                long elapsed = System.currentTimeMillis() - start;
                failed++;
                results.put(testName, String.format("FAIL: %s (%d ms)", t.getMessage(), elapsed));
                System.err.println(String.format("--> %s: FAIL (%d ms)", testName, elapsed));
                t.printStackTrace();
            }
        }

        long totalElapsed = System.currentTimeMillis() - totalStartTime;

        System.out.println("\n================================================================================");
        System.out.println("                              TEST EXECUTION REPORT                             ");
        System.out.println("================================================================================");
        for (Map.Entry<String, String> res : results.entrySet()) {
            System.out.println(String.format(" - %-38s : %s", res.getKey(), res.getValue()));
        }
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(String.format(" Total Tests : %d", total));
        System.out.println(String.format(" pass        : %d", passed));
        System.out.println(String.format(" FAIL        : %d", failed));
        System.out.println(String.format(" Total Time  : %.2f s", totalElapsed / 1000.0));
        System.out.println("================================================================================");

        return failed == 0;
    }

    private static void processPgnFile(String filename, boolean printOutput) {
        PgnReader reader = new PgnReader();
        PgnPrinter printer = new PgnPrinter();
        ArrayList<Long> offsets = reader.scanPgn(filename);

        for (int i = 0; i < offsets.size(); i++) {
            OptimizedRandomAccessFile raf = null;
            try {
                raf = new OptimizedRandomAccessFile(filename, "r");
                raf.seek(offsets.get(i));
                Game g = reader.readGame(raf);
                if (printOutput) {
                    System.out.println(printer.printGame(g));
                    System.out.println("\n");
                }
                raf.close();
            } catch (IOException e) {
                System.out.println("error reading: " + filename);
                System.out.println("game nr......: " + i);
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
    }
}
