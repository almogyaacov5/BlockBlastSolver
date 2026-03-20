package com.example.blockblastsolver;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotAnalyzer {

    public static int[] analyzeAndSolveVisual(Bitmap bitmap) {
        int[] flatBoard = new int[64];
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // 1. קריאת הלוח (כמו קודם)
        int boardStartX = (int) (width * 0.05);
        int boardWidth = (int) (width * 0.90);
        int boardStartY = (int) (height * 0.22);
        int cellSize = boardWidth / 8;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int pixelX = boardStartX + (c * cellSize) + (cellSize / 2);
                int pixelY = boardStartY + (r * cellSize) + (cellSize / 2);

                if (pixelX < width && pixelY < height) {
                    int p = bitmap.getPixel(pixelX, pixelY);
                    if (Color.red(p) > 80 || Color.green(p) > 80 || Color.blue(p) > 80) {
                        flatBoard[(r * 8) + c] = 1;
                    } else {
                        flatBoard[(r * 8) + c] = 0;
                    }
                }
            }
        }

        // 2. חילוץ 3 הצורות מהחלק התחתון של המסך
        List<Shape> shapes = extractShapesFromBottom(bitmap);

        // אם לא זוהתה אף צורה, מוסיפים צורה ברירת מחדל (1x1)
        if (shapes.isEmpty()) {
            shapes.add(new Shape(new int[][]{{1}}));
        }

        // 3. ✅ תוקן: קריאה ל-solveAllThree (המתודה הנכונה) וחזרה של הצעד הראשון
        Solver.SolverResult result = Solver.solveAllThree(flatBoard, shapes);
        if (result.success && !result.steps.isEmpty()) {
            return result.steps.get(0);
        }
        return flatBoard; // אין פתרון — מחזיר את הלוח הנוכחי
    }

    private static List<Shape> extractShapesFromBottom(Bitmap bitmap) {
        List<Shape> shapes = new ArrayList<>();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // האזור של הצורות: 72% - 95% מהגובה
        int startY = (int) (height * 0.72);
        int endY = (int) (height * 0.95);

        // חלוקה ל-3 חלקים שווים לרוחב
        int sectionWidth = width / 3;

        for (int i = 0; i < 3; i++) {
            int startX = i * sectionWidth;
            int endX = startX + sectionWidth;

            // מצא את ה-Bounding Box של הצורה בחלק זה
            int minX = width, maxX = 0, minY = height, maxY = 0;
            boolean foundColor = false;

            for (int y = startY; y < endY; y += 5) {
                for (int x = startX; x < endX; x += 5) {
                    int p = bitmap.getPixel(x, y);
                    if (Color.red(p) > 80 || Color.green(p) > 80 || Color.blue(p) > 80) {
                        foundColor = true;
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (foundColor) {
                int shapeW = maxX - minX;
                int shapeH = maxY - minY;

                // גודל בלוק אומד: מחלקים ל-5, מינימום 10 פיקסל
                int blockSize = Math.max(shapeW, shapeH) / 5;
                if (blockSize < 10) blockSize = 10;

                int cols = Math.round((float) shapeW / blockSize) + 1;
                int rows = Math.round((float) shapeH / blockSize) + 1;

                // הגבלה ל-5x5
                if (rows > 5) rows = 5;
                if (cols > 5) cols = 5;

                int[][] grid = new int[rows][cols];

                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        int pX = minX + (c * blockSize) + (blockSize / 2);
                        int pY = minY + (r * blockSize) + (blockSize / 2);
                        if (pX <= maxX && pY <= maxY) {
                            int p = bitmap.getPixel(pX, pY);
                            if (Color.red(p) > 80 || Color.green(p) > 80 || Color.blue(p) > 80) {
                                grid[r][c] = 1;
                            }
                        }
                    }
                }
                shapes.add(new Shape(grid));
            }
        }
        return shapes;
    }
}
