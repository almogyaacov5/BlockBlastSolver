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

        // 1. קריאת הלוח (כמו מקודם)
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

        // 2. קריאת 3 הצורות שלמטה (אזור תחתון של המסך)
        List<Shape> shapes = extractShapesFromBottom(bitmap);

        // אם לא זוהו צורות טובות בגלל איכות התמונה, נכניס צורת גיבוי בסיסית כדי שלא יקרוס
        if (shapes.isEmpty()) {
            shapes.add(new Shape(new int[][]{{1}}));
        }

        // 3. שליחה לאלגוריתם למציאת המהלך הטוב ביותר
        return Solver.getBestMove(flatBoard, shapes);
    }

    private static List<Shape> extractShapesFromBottom(Bitmap bitmap) {
        List<Shape> shapes = new ArrayList<>();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // הצורות נמצאות בערך בין 75% ל-90% מגובה המסך
        int startY = (int) (height * 0.72);
        int endY = (int) (height * 0.95);

        // נחלק את הרוחב ל-3 אזורים (ל-3 הצורות)
        int sectionWidth = width / 3;

        for (int i = 0; i < 3; i++) {
            int startX = i * sectionWidth;
            int endX = startX + sectionWidth;

            // חיפוש הגבולות (Bounding Box) של הצבע באזור הזה
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

            // אם מצאנו צורה באזור הזה
            if (foundColor) {
                // המרת הפיקסלים למטריצה (מניחים שגודל קובייה קטנה הוא בערך 30-40 פיקסלים)
                int shapeW = maxX - minX;
                int shapeH = maxY - minY;

                // בבלוק בלאסט, צורה היא מקסימום 5x5. נמצא את כמות העמודות/שורות היחסית
                int blockSize = Math.max(shapeW, shapeH) / 5;
                if (blockSize < 10) blockSize = 10; // הגנה

                int cols = Math.round((float) shapeW / blockSize) + 1;
                int rows = Math.round((float) shapeH / blockSize) + 1;

                // הגנה מגלישה
                if(rows > 5) rows = 5;
                if(cols > 5) cols = 5;

                int[][] grid = new int[rows][cols];

                // דוגמים שוב לפי הרשת הפנימית של הצורה
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
