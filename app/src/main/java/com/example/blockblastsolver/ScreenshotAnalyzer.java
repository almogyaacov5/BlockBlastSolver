package com.example.blockblastsolver;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotAnalyzer {

    // קריאת הלוח 8x8 מהתמונה
    public static int[] extractBoard(Bitmap bitmap) {
        int[] flatBoard = new int[64];
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int boardStartX = (int) (width * 0.05);
        int boardWidth  = (int) (width * 0.90);
        int boardStartY = (int) (height * 0.22);
        int cellSize    = boardWidth / 8;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int pixelX = boardStartX + (c * cellSize) + (cellSize / 2);
                int pixelY = boardStartY + (r * cellSize) + (cellSize / 2);
                if (pixelX < width && pixelY < height) {
                    int p = bitmap.getPixel(pixelX, pixelY);
                    flatBoard[r * 8 + c] = (Color.red(p) > 80 ||
                            Color.green(p) > 80 || Color.blue(p) > 80) ? 1 : 0;
                }
            }
        }
        return flatBoard;
    }

    // חילוץ 3 הצורות מהחלק התחתון
    public static List<Shape> extractShapes(Bitmap bitmap) {
        List<Shape> shapes = new ArrayList<>();
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();

        int startY      = (int) (height * 0.72);
        int endY        = (int) (height * 0.95);
        int sectionWidth = width / 3;

        for (int i = 0; i < 3; i++) {
            int startX = i * sectionWidth;
            int endX   = startX + sectionWidth;

            int minX = width, maxX = 0, minY = height, maxY = 0;
            boolean found = false;

            for (int y = startY; y < endY; y += 5) {
                for (int x = startX; x < endX; x += 5) {
                    int p = bitmap.getPixel(x, y);
                    if (Color.red(p) > 80 || Color.green(p) > 80 || Color.blue(p) > 80) {
                        found = true;
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (found) {
                int shapeW    = maxX - minX;
                int shapeH    = maxY - minY;
                int blockSize = Math.max(10, Math.max(shapeW, shapeH) / 5);
                int cols      = Math.min(5, Math.round((float) shapeW / blockSize) + 1);
                int rows      = Math.min(5, Math.round((float) shapeH / blockSize) + 1);
                int[][] grid  = new int[rows][cols];

                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        int pX = minX + c * blockSize + blockSize / 2;
                        int pY = minY + r * blockSize + blockSize / 2;
                        if (pX <= maxX && pY <= maxY) {
                            int p = bitmap.getPixel(pX, pY);
                            if (Color.red(p) > 80 || Color.green(p) > 80 || Color.blue(p) > 80)
                                grid[r][c] = 1;
                        }
                    }
                }
                shapes.add(new Shape(grid));
            }
        }

        if (shapes.isEmpty()) shapes.add(new Shape(new int[][]{{1}}));
        return shapes;
    }

    // נשמר לאחורי תאימות (לא בשימוש יותר)
    public static int[] analyzeAndSolveVisual(Bitmap bitmap) {
        int[] board = extractBoard(bitmap);
        List<Shape> shapes = extractShapes(bitmap);
        Solver.SolverResult result = Solver.solveAllThree(board, shapes);
        if (result.success && !result.steps.isEmpty()) return result.steps.get(0);
        return board;
    }
}
