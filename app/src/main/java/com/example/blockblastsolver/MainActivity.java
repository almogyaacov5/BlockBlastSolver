package com.example.blockblastsolver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView ivScreenshot;
    private TextView tvSolution;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(), imageUri);
                        ivScreenshot.setImageBitmap(bitmap);
                        tvSolution.setText("מנתח את הלוח...");

                        // שלב 1: קריאת הלוח והצורות מהתמונה
                        int[] flatBoard = ScreenshotAnalyzer.extractBoard(bitmap);
                        List<Shape> shapes = ScreenshotAnalyzer.extractShapes(bitmap);

                        // שלב 2: חישוב הפתרון האופטימלי
                        Solver.SolverResult solverResult = Solver.solveAllThree(flatBoard, shapes);

                        if (!solverResult.success || solverResult.steps.isEmpty()) {
                            tvSolution.setText("לא נמצא פתרון");
                            Toast.makeText(this, "לא נמצא פתרון", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // שלב 3: שליחת כל שלב למסך הפתרון
                        Intent intent = new Intent(MainActivity.this, SolutionActivity.class);
                        List<int[]> steps = solverResult.steps;
                        intent.putExtra("STEP_COUNT", steps.size());
                        for (int i = 0; i < steps.size(); i++) {
                            intent.putExtra("STEP_" + i, steps.get(i));
                        }
                        startActivity(intent);
                        tvSolution.setText("הפתרון מוכן!");

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnUpload = findViewById(R.id.btnUpload);
        ivScreenshot = findViewById(R.id.ivScreenshot);
        tvSolution = findViewById(R.id.tvSolution);

        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }
}
