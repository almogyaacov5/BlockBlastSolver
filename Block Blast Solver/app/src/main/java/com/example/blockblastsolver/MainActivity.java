package com.example.blockblastsolver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ImageView ivScreenshot;
    private TextView tvSolution;
    private Button btnUpload;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(), imageUri);
                        ivScreenshot.setImageBitmap(bitmap);

                        if (!btnUpload.isEnabled()) return;
                        btnUpload.setEnabled(false);
                        tvSolution.setText("מנתח עם Gemini AI...");

                        executor.execute(() -> {
                            // נסה עד 3 פעמים — attempt מוגדר final בנפרד
                            ScreenshotAnalyzer.GeminiResult geminiResult = null;
                            for (int i = 1; i <= 3; i++) {
                                final int attemptNum = i; // final לשימוש בlambda
                                mainHandler.post(() ->
                                        tvSolution.setText("מנתח עם Gemini... (ניסיון " + attemptNum + "/3)"));
                                geminiResult = ScreenshotAnalyzer.analyzeWithGemini(bitmap);
                                if (geminiResult != null) break;
                                Log.d("Gemini", "Attempt " + attemptNum + " failed, retrying...");
                            }

                            final int[] flatBoard;
                            final List<Shape> shapes;
                            final String method;

                            if (geminiResult != null) {
                                flatBoard = geminiResult.flatBoard;
                                shapes    = geminiResult.shapes;
                                method    = "Gemini זיהה " + shapes.size() + " צורות!";
                            } else {
                                flatBoard = ScreenshotAnalyzer.extractBoard(bitmap);
                                shapes    = ScreenshotAnalyzer.extractShapes(bitmap);
                                method    = "גיבוי: ניתוח פיקסלים";
                            }

                            final Solver.SolverResult solverResult =
                                    Solver.solveAllThree(flatBoard, shapes);

                            mainHandler.post(() -> {
                                btnUpload.setEnabled(true);
                                tvSolution.setText(method);

                                if (!solverResult.success || solverResult.steps.isEmpty()) {
                                    tvSolution.setText("לא נמצא פתרון");
                                    Toast.makeText(this, "לא נמצא פתרון", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Intent intent = new Intent(MainActivity.this, SolutionActivity.class);
                                List<int[]> steps = solverResult.steps;
                                intent.putExtra("STEP_COUNT", steps.size());
                                for (int i = 0; i < steps.size(); i++) {
                                    intent.putExtra("STEP_" + i, steps.get(i));
                                }
                                startActivity(intent);
                            });
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        btnUpload.setEnabled(true);
                        Toast.makeText(this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnUpload    = findViewById(R.id.btnUpload);
        ivScreenshot = findViewById(R.id.ivScreenshot);
        tvSolution   = findViewById(R.id.tvSolution);
        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
