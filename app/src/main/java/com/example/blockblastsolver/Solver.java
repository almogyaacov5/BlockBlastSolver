package com.example.blockblastsolver;

import java.util.ArrayList;
import java.util.List;

public class Solver {

    public static class SolverResult {
        public List<int[]> steps; // כל שלב הוא לוח שלם של 64 תאים
        public boolean success;

        public SolverResult() {
            steps = new ArrayList<>();
            success = false;
        }
    }

    // ערכים אפשריים בלוח:
    // 0 = ריק
    // 1 = בלוק קיים
    // 2 = בלוק חדש שהונח (ירוק)
    // 3 = תא שנמחק בגלל שורה/עמודה מלאה (כחול/לבן)

    public static SolverResult solveAllThree(int[] flatBoard, List<Shape> shapes) {
        SolverResult result = new SolverResult();

        int[][] board = flatTo2D(flatBoard);

        // מריצים DFS על 3 הבלוקים ברצף
        List<int[][]> bestSequence = new ArrayList<>();
        int[] bestScore = {Integer.MAX_VALUE};

        dfs(board, shapes, 0, new ArrayList<>(), bestSequence, bestScore);

        if (!bestSequence.isEmpty()) {
            result.success = true;

            // בנה את הלוחות לכל שלב
            int[][] currentBoard = flatTo2D(flatBoard);

            for (int step = 0; step < bestSequence.size(); step++) {
                int[][] stepBoard = bestSequence.get(step);

                // בנה לוח תצוגה: מה היה + מה נוסף + מה נמחק
                int[] displayBoard = new int[64];
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        int prev = currentBoard[r][c];
                        int next = stepBoard[r][c];

                        if (prev == 1 && next == 1) {
                            displayBoard[r * 8 + c] = 1; // בלוק קיים
                        } else if (prev == 0 && next == 1) {
                            displayBoard[r * 8 + c] = 2; // בלוק חדש (ירוק)
                        } else if (prev == 1 && next == 0) {
                            displayBoard[r * 8 + c] = 3; // נמחק (שורה/עמודה מלאה)
                        } else {
                            displayBoard[r * 8 + c] = 0; // ריק
                        }
                    }
                }
                result.steps.add(displayBoard);
                currentBoard = stepBoard;
            }
        }

        return result;
    }

    private static void dfs(
            int[][] board,
            List<Shape> shapes,
            int shapeIndex,
            List<int[][]> currentSequence,
            List<int[][]> bestSequence,
            int[] bestScore) {

        if (shapeIndex == shapes.size()) {
            int score = countBlocks(board);
            if (score < bestScore[0]) {
                bestScore[0] = score;
                bestSequence.clear();
                for (int[][] b : currentSequence) {
                    bestSequence.add(copyBoard(b));
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
                    dfs(newBoard, shapes, shapeIndex + 1, currentSequence, bestSequence, bestScore);
                    currentSequence.remove(currentSequence.size() - 1);
                }
            }
        }

        // אם לא ניתן להניח את הצורה, ממשיכים לבאה
        if (!placed) {
            dfs(board, shapes, shapeIndex + 1, currentSequence, bestSequence, bestScore);
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

        // בדיקה וניקוי שורות ועמודות מלאות
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
