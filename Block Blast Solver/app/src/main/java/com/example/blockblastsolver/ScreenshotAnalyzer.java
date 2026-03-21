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
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"
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


            String prompt =
                    "You are analyzing a Block Blast mobile game screenshot.\n\n" +
                            "BOARD RULES:\n" +
                            "- The board is EXACTLY 8 columns x 8 rows (8x8 grid).\n" +
                            "- Output a 2D array of exactly 8 rows, each with exactly 8 values.\n" +
                            "- 1 = a colored block exists in that cell.\n" +
                            "- 0 = the cell is empty (black/dark background).\n" +
                            "- Carefully count every row and column — do not skip or merge cells.\n\n" +
                            "PIECES RULES:\n" +
                            "- There are EXACTLY 3 pieces shown at the bottom of the screen.\n" +
                            "- Each piece is a 2D array representing its shape.\n" +
                            "- Use only the minimum bounding box for each piece (no extra empty rows or columns).\n" +
                            "- 1 = filled cell, 0 = empty cell within the bounding box.\n" +
                            "- Piece shapes can be 1x1 up to 5x5. Common shapes: 2x2, 3x3, L-shape, T-shape, line.\n" +
                            "- A 2x2 square piece has exactly 2 rows and 2 columns — NOT 3 rows.\n" +
                            "- Count rows and columns pixel by pixel, do not guess by shape similarity.\n\n" +
                            "IMPORTANT:\n" +
                            "- Do NOT include markdown, code blocks, or explanation.\n" +
                            "- Return ONLY raw valid JSON, nothing else.\n" +
                            "- The JSON must follow this exact structure:\n\n" +
                            "{\n" +
                            "  \"board\": [\n" +
                            "    [0,0,0,0,0,0,0,0],\n" +
                            "    [0,0,0,0,0,0,0,0],\n" +
                            "    [0,0,0,0,0,0,0,0],\n" +
                            "    [0,0,0,0,0,0,0,0],\n" +
                            "    [0,0,0,0,0,0,0,0],\n" +
                            "    [0,0,0,0,0,0,0,0],\n" +
                            "    [0,0,0,0,0,0,0,0],\n" +
                            "    [0,0,0,0,0,0,0,0]\n" +
                            "  ],\n" +
                            "  \"pieces\": [\n" +
                            "    [[1,1],[1,1]],\n" +
                            "    [[1,1,1],[1,1,1],[1,1,1]],\n" +
                            "    [[1,1,1],[1,1,1],[1,1,1]]\n" +
                            "  ]\n" +
                            "}\n\n" +
                            "Now analyze the provided screenshot and return the JSON.";





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
            conn.setConnectTimeout(5000);  // 8 שניות לחיבור
            conn.setReadTimeout(10000);    // 15 שניות לתשובה

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

            JSONArray boardJson = json.getJSONArray("board");
            int[] flatBoard = new int[64];
            for (int r = 0; r < 8; r++) {
                JSONArray row = boardJson.getJSONArray(r);
                for (int c = 0; c < 8; c++) {
                    flatBoard[r * 8 + c] = row.getInt(c);
                }
            }

            JSONArray piecesJson = json.getJSONArray("pieces");
            List<Shape> shapes = new ArrayList<>();

            for (int i = 0; i < piecesJson.length(); i++) {
                JSONArray pieceJson = piecesJson.getJSONArray(i);
                int rows = pieceJson.length();
                int cols = pieceJson.getJSONArray(0).length();
                int[][] grid = new int[rows][cols];

                boolean allOnes = true;
                for (int r = 0; r < rows; r++) {
                    JSONArray row = pieceJson.getJSONArray(r);
                    for (int c = 0; c < cols; c++) {
                        grid[r][c] = row.getInt(c);
                        if (grid[r][c] != 1) allOnes = false;
                    }
                }

                // תיקון: אם הצורה כולה מלאה ב-1 אבל אינה ריבוע,
                // וההפרש בין שורות לעמודות הוא 1 — נעגל לריבוע הקטן יותר
                if (allOnes && rows != cols && Math.abs(rows - cols) == 1) {
                    int squareSize = Math.min(rows, cols);
                    Log.d("Gemini", "Piece " + i + ": fixing " + rows + "x" + cols
                            + " -> " + squareSize + "x" + squareSize);
                    grid = new int[squareSize][squareSize];
                    for (int r = 0; r < squareSize; r++)
                        for (int c = 0; c < squareSize; c++)
                            grid[r][c] = 1;
                    rows = squareSize;
                    cols = squareSize;
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

        // מיקומים יחסיים (עובד מעולה לרוב מסכי הסמארטפונים המודרניים)
        int boardStartX = (int) (width * 0.05);
        int boardWidth  = (int) (width * 0.90);
        int boardStartY = (int) (height * 0.22);

        // חשוב: שימוש ב-float כדי למנוע הצטברות של שגיאות עיגול (Rounding errors)
        float cellSize = boardWidth / 8f;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                // מציאת המרכז המדויק של התא הנוכחי
                int centerX = (int) (boardStartX + (c * cellSize) + (cellSize / 2));
                int centerY = (int) (boardStartY + (r * cellSize) + (cellSize / 2));

                boolean isFilled = false;

                // דוגמים אזור ברדיוס של 8 פיקסלים סביב המרכז (יוצר רשת דגימה קטנה)
                // זה מונע מצב שבו נפלנו בדיוק על הצללה או חריץ שחור בתוך הבלוק
                int sampleRadius = 8;

                for (int y = centerY - sampleRadius; y <= centerY + sampleRadius; y += 4) {
                    for (int x = centerX - sampleRadius; x <= centerX + sampleRadius; x += 4) {
                        // מוודאים שאנחנו לא חורגים מגבולות התמונה
                        if (x >= 0 && x < width && y >= 0 && y < height) {
                            int p = bitmap.getPixel(x, y);
                            // משבצות ריקות הן אפור-כהה. הבלוקים הרבה יותר בהירים.
                            // העלינו קצת את הסף ל-90 כדי לסנן רעשים מהרקע
                            if (Color.red(p) > 90 || Color.green(p) > 90 || Color.blue(p) > 90) {
                                isFilled = true;
                                break; // מצאנו צבע! אין טעם להמשיך לסרוק את התא הזה
                            }
                        }
                    }
                    if (isFilled) break;
                }

                flatBoard[r * 8 + c] = isFilled ? 1 : 0;
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

        // גודל בלוק משוער בתחתית המסך (בד"כ קטן יותר מבלוק בלוח עצמו)
        int expectedBlockSize = (int) (width * 0.055);

        for (int i = 0; i < 3; i++) {
            int startX = i * sectionWidth;
            int endX   = startX + sectionWidth;
            int minX = width, maxX = 0, minY = height, maxY = 0;
            boolean found = false;

            // מציאת ה-Bounding Box של הצורה
            for (int y = startY; y < endY; y += 3) {
                for (int x = startX; x < endX; x += 3) {
                    int p = bitmap.getPixel(x, y);
                    // סף רגישות נמוך יותר כדי לא להתבלבל מההצללות
                    if (Color.red(p) > 90 || Color.green(p) > 90 || Color.blue(p) > 90) {
                        found = true;
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (found) {
                int shapeW = maxX - minX;
                int shapeH = maxY - minY;

                // חישוב מספר העמודות והשורות לפי הגודל הפיזי של הצורה חלקי גודל בלוק צפוי
                int cols = Math.max(1, Math.round((float) shapeW / expectedBlockSize));
                int rows = Math.max(1, Math.round((float) shapeH / expectedBlockSize));

                // תיקון למקרי קצה של רווחים (Padding)
                int actualBlockW = shapeW / cols;
                int actualBlockH = shapeH / rows;

                int[][] grid = new int[rows][cols];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        // דיגום מהמרכז המדויק של כל בלוק פוטנציאלי
                        int pX = minX + (c * actualBlockW) + (actualBlockW / 2);
                        int pY = minY + (r * actualBlockH) + (actualBlockH / 2);

                        if (pX <= maxX && pY <= maxY) {
                            int p = bitmap.getPixel(pX, pY);
                            if (Color.red(p) > 90 || Color.green(p) > 90 || Color.blue(p) > 90) {
                                grid[r][c] = 1;
                            } else {
                                grid[r][c] = 0;
                            }
                        }
                    }
                }
                shapes.add(new Shape(grid));
            }
        }

        // אם לא נמצאו צורות, נחזיר צורה ריקה כדי למנוע קריסות
        if (shapes.isEmpty()) {
            shapes.add(new Shape(new int[][]{{1}}));
        }

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
