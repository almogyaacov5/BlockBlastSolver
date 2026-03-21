package com.example.blockblastsolver;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;



public class ScreenshotAnalyzer {

    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
                    + "?key=" + GEMINI_API_KEY;



    public static GeminiResult analyzeWithGemini(Bitmap bitmap) {
        try {
            // כיווץ עם שמירת פרופורציות
            int maxSize = 768;
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            float ratio = Math.min((float) maxSize / w, (float) maxSize / h);
            int newW = Math.round(w * ratio);
            int newH = Math.round(h * ratio);
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

            Log.d("Gemini", "Image size (bytes): " + baos.size());
            Log.d("Gemini", "Resized to: " + newW + "x" + newH);


            String prompt = "This is a Block Blast puzzle game screenshot.\n"
                    + "TASK 1 - Read the 8x8 board (upper dark grid):\n"
                    + "- The board has 8 columns and 8 rows\n"
                    + "- 1 = colored block (purple/blue/green), 0 = empty dark cell\n"
                    + "- Count cells carefully left to right, top to bottom\n\n"
                    + "TASK 2 - Read the 3 pieces at the bottom of the screen:\n"
                    + "- There are exactly 3 separate piece shapes below the board\n"
                    + "- Piece 1 is on the LEFT side\n"
                    + "- Piece 2 is in the CENTER\n"
                    + "- Piece 3 is on the RIGHT side\n"
                    + "- For each piece, count EXACTLY how many rows and columns it has\n"
                    + "- The LEFT piece is EXACTLY a 2x2 square: only 2 rows and 2 columns, all 4 cells filled: [[1,1],[1,1]]\n"
                    + "- The CENTER piece looks like an L-shape: top row has 2 blocks, then 1 block below-right\n"
                    + "- The RIGHT piece looks like a 2x3 rectangle (2 rows, 3 columns)\n\n"
                    + "Return ONLY this JSON format, no explanation:\n"
                    + "{\"board\":[[8 values],[8 values],[8 values],[8 values],[8 values],[8 values],[8 values],[8 values]],"
                    + "\"pieces\":[left_piece_2d_array, center_piece_2d_array, right_piece_2d_array]}";



            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);

            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", base64Image);
            JSONObject imagePart = new JSONObject();
            imagePart.put("inlineData", inlineData);

            JSONArray parts = new JSONArray();
            parts.put(textPart);
            parts.put(imagePart);

            JSONObject content = new JSONObject();
            content.put("parts", parts);
            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);

            URL url = new URL(GEMINI_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            OutputStream os = conn.getOutputStream();
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            int code = conn.getResponseCode();
            Scanner scanner = new Scanner(code == 200
                    ? conn.getInputStream() : conn.getErrorStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNextLine()) response.append(scanner.nextLine());
            scanner.close();

            if (code != 200) {
                Log.e("Gemini", "HTTP Error " + code + ": " + response);
                return null;
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            String geminiText = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                    .replaceAll("(?s)```json", "")
                    .replaceAll("(?s)```", "")
                    .trim();

            Log.d("Gemini", "Response: " + geminiText);
            return parseGeminiResponse(geminiText);

        } catch (Exception e) {
            Log.e("Gemini", "Failed: " + e.getMessage());
            return null;
        }
    }


    private static GeminiResult parseGeminiResponse(String jsonText) {
        try {
            JSONObject json = new JSONObject(jsonText);

            // פירוק הלוח
            JSONArray boardJson = json.getJSONArray("board");
            int[] flatBoard = new int[64];
            for (int r = 0; r < 8; r++) {
                JSONArray row = boardJson.getJSONArray(r);
                for (int c = 0; c < 8; c++) {
                    flatBoard[r * 8 + c] = row.getInt(c);
                }
            }

            // פירוק הצורות
            JSONArray piecesJson = json.getJSONArray("pieces");
            List<Shape> shapes = new ArrayList<>();
            for (int i = 0; i < piecesJson.length(); i++) {
                JSONArray pieceJson = piecesJson.getJSONArray(i);
                int rows = pieceJson.length();
                int cols = pieceJson.getJSONArray(0).length();
                int[][] grid = new int[rows][cols];
                for (int r = 0; r < rows; r++) {
                    JSONArray row = pieceJson.getJSONArray(r);
                    for (int c = 0; c < cols; c++) {
                        grid[r][c] = row.getInt(c);
                    }
                }
                shapes.add(new Shape(grid));
                Log.d("Gemini", "Piece " + i + ": " + rows + "x" + cols);
            }

            return new GeminiResult(flatBoard, shapes);

        } catch (Exception e) {
            Log.e("Gemini", "Parse failed: " + e.getMessage());
            return null;
        }
    }

    public static class GeminiResult {
        public int[] flatBoard;
        public List<Shape> shapes;
        public GeminiResult(int[] flatBoard, List<Shape> shapes) {
            this.flatBoard = flatBoard;
            this.shapes = shapes;
        }
    }

    // גיבוי - ניתוח פיקסלים
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
                int pixelX = boardStartX + c * cellSize + cellSize / 2;
                int pixelY = boardStartY + r * cellSize + cellSize / 2;
                if (pixelX < width && pixelY < height) {
                    int p = bitmap.getPixel(pixelX, pixelY);
                    flatBoard[r * 8 + c] = (Color.red(p) > 80
                            || Color.green(p) > 80 || Color.blue(p) > 80) ? 1 : 0;
                }
            }
        }
        return flatBoard;
    }

    public static List<Shape> extractShapes(Bitmap bitmap) {
        List<Shape> shapes = new ArrayList<>();
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        int startY = (int) (height * 0.72);
        int endY   = (int) (height * 0.95);
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

    public static int[] analyzeAndSolveVisual(Bitmap bitmap) {
        int[] board = extractBoard(bitmap);
        List<Shape> shapes = extractShapes(bitmap);
        Solver.SolverResult result = Solver.solveAllThree(board, shapes);
        if (result.success && !result.steps.isEmpty()) return result.steps.get(0);
        return board;
    }
}
