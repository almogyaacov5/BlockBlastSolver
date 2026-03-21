package com.example.blockblastsolver;

import java.util.ArrayList;
import java.util.List;

public class Solver {

    public static class SolverResult {
        public List<int[]> steps; // כל שלב הוא מערך שטוח של 64 תאים
        public boolean success;

        public SolverResult() {
            steps = new ArrayList<>();
            success = false;
        }
    }

    // ערכים אפשריים בלוח:
    // 0 = ריק
    // 1 = קיים (לא שונה)
    // 2 = חדש (הונח בשלב זה)
    // 3 = נמחק (שורה/עמודה שנוקתה)

    /**
     * getBestMove — wrapper ל-solveAllThree.
     * זוהי המתודה שנקראת מ-ScreenshotAnalyzer.
     */
    public static int[] getBestMove(int[] flatBoard, List<Shape> shapes) {
        SolverResult result = solveAllThree(flatBoard, shapes);
        if (result.success && !result.steps.isEmpty()) {
            return result.steps.get(0); // מחזיר את השלב הראשון
        }
        return flatBoard; // אם אין פתרון, מחזיר את הלוח הנוכחי
    }

    public static SolverResult solveAllThree(int[] flatBoard, List<Shape> shapes) {
        SolverResult result = new SolverResult();

        int[][] board = flatTo2D(flatBoard);

        // מחפש את הסדר האופטימלי להניח את 3 הצורות
        List<int[][]> bestSequence = new ArrayList<>();
        int[] bestScore = {Integer.MAX_VALUE};

        // חדש: נשמור גם אילו צורות הונחו בפועל עבור הפתרון הטוב ביותר
        boolean[] bestPlacedFlags = {false}; // placeholder כדי שנוכל להעביר reference
        List<Boolean> bestPlacedList = new ArrayList<>();

        dfs(
                board,
                shapes,
                0,
                new ArrayList<>(),
                new ArrayList<>(),      // currentPlaced
                bestSequence,
                bestScore,
                bestPlacedList
        );

        // בדיקה בסוף ה‑DFS: כל הצורות חייבות להיות הונחו (ללא skip) כדי שנסמן success
        if (!bestSequence.isEmpty() && !bestPlacedList.isEmpty()) {
            boolean allPlaced = true;
            for (Boolean b : bestPlacedList) {
                if (!b) {
                    allPlaced = false;
                    break;
                }
            }

            if (allPlaced) {
                result.success = true;

                // בונה את רצף השלבים לתצוגה
                int[][] currentBoard = flatTo2D(flatBoard);

                for (int step = 0; step < bestSequence.size(); step++) {
                    int[][] stepBoard = bestSequence.get(step);

                    // בונה displayBoard עם ערכים 0/1/2/3
                    int[] displayBoard = new int[64];
                    for (int r = 0; r < 8; r++) {
                        for (int c = 0; c < 8; c++) {
                            int prev = currentBoard[r][c];
                            int next = stepBoard[r][c];

                            if (prev == 1 && next == 1) {
                                displayBoard[r * 8 + c] = 1; // קיים ולא שונה
                            } else if (prev == 0 && next == 1) {
                                displayBoard[r * 8 + c] = 2; // חדש (הונח)
                            } else if (prev == 1 && next == 0) {
                                displayBoard[r * 8 + c] = 3; // נמחק (שורה/עמודה נוקתה)
                            } else {
                                displayBoard[r * 8 + c] = 0; // ריק
                            }
                        }
                    }
                    result.steps.add(displayBoard);
                    currentBoard = stepBoard;
                }
            }
        }

        return result;
    }

    /**
     * dfs מורחב:
     * currentPlaced – רשימה בוליאנית המקבילה ל-currentSequence, מסמנת האם הצורה
     * בשלב הזה באמת הונחה (true) או שזו צורה שדילגנו עליה (false).
     */
    private static void dfs(
            int[][] board,
            List<Shape> shapes,
            int shapeIndex,
            List<int[][]> currentSequence,
            List<Boolean> currentPlaced,
            List<int[][]> bestSequence,
            int[] bestScore,
            List<Boolean> bestPlacedList) {

        if (shapeIndex == shapes.size()) {
            // נבדוק רק רצפים שבהם כל הצורות הונחו בפועל (אין skip)
            if (currentPlaced.size() != shapes.size()) {
                return;
            }
            for (Boolean b : currentPlaced) {
                if (!b) {
                    return; // יש skip – לא נחשב כפיתרון תקין
                }
            }

            int score = countBlocks(board);
            if (score < bestScore[0]) {
                bestScore[0] = score;
                bestSequence.clear();
                bestPlacedList.clear();
                for (int i = 0; i < currentSequence.size(); i++) {
                    bestSequence.add(copyBoard(currentSequence.get(i)));
                    bestPlacedList.add(currentPlaced.get(i));
                }
            }
            return;
        }

        Shape shape = shapes.get(shapeIndex);
        boolean placed = false;

        for (int r = 0; r <= 8 - shape.rows; r++) {
            for (int c = 0; c <= 8 - shape.cols; c++) {
                if (canPlace(board, shape, r, c)) {
                    placed = true;
                    int[][] newBoard = placeAndClear(board, shape, r, c);
                    currentSequence.add(newBoard);
                    currentPlaced.add(Boolean.TRUE); // צורה זו הונחה בפועל
                    dfs(newBoard, shapes, shapeIndex + 1, currentSequence, currentPlaced,
                            bestSequence, bestScore, bestPlacedList);
                    currentSequence.remove(currentSequence.size() - 1);
                    currentPlaced.remove(currentPlaced.size() - 1);
                }
            }
        }

        // אם לא ניתן להניח את הצורה, מסמנים skip בצורה explicit
        if (!placed) {
            // מסמנים שלא הונחה צורה בשלב הזה
            currentPlaced.add(Boolean.FALSE);
            // לא משנים את הלוח
            dfs(board, shapes, shapeIndex + 1, currentSequence, currentPlaced,
                    bestSequence, bestScore, bestPlacedList);
            currentPlaced.remove(currentPlaced.size() - 1);
        }
    }

    private static boolean canPlace(int[][] board, Shape shape, int row, int col) {
        for (int r = 0; r < shape.rows; r++) {
            for (int c = 0; c < shape.cols; c++) {
                if (shape.grid[r][c] == 1 && board[row + r][col + c] == 1) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int[][] placeAndClear(int[][] board, Shape shape, int row, int col) {
        int[][] temp = copyBoard(board);

        for (int r = 0; r < shape.rows; r++) {
            for (int c = 0; c < shape.cols; c++) {
                if (shape.grid[r][c] == 1) {
                    temp[row + r][col + c] = 1;
                }
            }
        }

        // בודק שורות ועמודות מלאות לניקוי
        boolean[] clearRow = new boolean[8];
        boolean[] clearCol = new boolean[8];

        for (int i = 0; i < 8; i++) {
            boolean rowFull = true, colFull = true;
            for (int j = 0; j < 8; j++) {
                if (temp[i][j] == 0) rowFull = false;
                if (temp[j][i] == 0) colFull = false;
            }
            clearRow[i] = rowFull;
            clearCol[i] = colFull;
        }

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (clearRow[i] || clearCol[j]) {
                    temp[i][j] = 0;
                }
            }
        }

        return temp;
    }

    private static int countBlocks(int[][] board) {
        int count = 0;
        for (int[] row : board)
            for (int cell : row)
                if (cell == 1) count++;
        return count;
    }

    public static int[][] flatTo2D(int[] flat) {
        int[][] board = new int[8][8];
        for (int i = 0; i < 64; i++) {
            board[i / 8][i % 8] = flat[i];
        }
        return board;
    }

    private static int[][] copyBoard(int[][] board) {
        int[][] copy = new int[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, 8);
        }
        return copy;
    }
}
