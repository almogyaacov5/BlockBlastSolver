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

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private ImageView ivScreenshot;
    private TextView tvSolution;

    // מנגנון חדיש באנדרואיד לקבלת תוצאה (תמונה) מפעילות אחרת
    // בתוך MainActivity.java, נעדכן את ה-Launcher:

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        ivScreenshot.setImageBitmap(bitmap);
                        tvSolution.setText("מנתח את הלוח...");

                        // מנתח ומקבל מערך של הלוח עם הפתרון
                        int[] solvedBoard = ScreenshotAnalyzer.analyzeAndSolveVisual(bitmap);

                        // פותח את מסך הפתרון ומעביר אליו את המערך
                        Intent intent = new Intent(MainActivity.this, SolutionActivity.class);
                        intent.putExtra("SOLVED_BOARD", solvedBoard);
                        startActivity(intent); // מעבר למסך החדש!

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
            // פתיחת חלונית לבחירת תמונה (צילום מסך)
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }
}
