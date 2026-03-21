package com.example.blockblastsolver;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class SolutionActivity extends AppCompatActivity {

    private GridView gridSolutionBoard;
    private TextView tvStepTitle;
    private Button btnPrev, btnNext, btnBack;

    private List<int[]> steps = new ArrayList<>();
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solution);

        // חיבור ה-Views
        gridSolutionBoard = findViewById(R.id.gridSolutionBoard);
        tvStepTitle       = findViewById(R.id.tvStepTitle);
        btnPrev           = findViewById(R.id.btnPrev);
        btnNext           = findViewById(R.id.btnNext);
        btnBack           = findViewById(R.id.btnBack);

        // קבלת השלבים מה-Intent
        int stepCount = getIntent().getIntExtra("STEP_COUNT", 0);
        for (int i = 0; i < stepCount; i++) {
            int[] step = getIntent().getIntArrayExtra("STEP_" + i);
            if (step != null) steps.add(step);
        }

        if (steps.isEmpty()) {
            tvStepTitle.setText("לא נמצא פתרון");
            btnPrev.setEnabled(false);
            btnNext.setEnabled(false);
            return;
        }

        showStep(0);

        btnNext.setOnClickListener(v -> {
            if (currentStep < steps.size() - 1) {
                currentStep++;
                showStep(currentStep);
            } else {
                Toast.makeText(this, "זה השלב האחרון!", Toast.LENGTH_SHORT).show();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentStep > 0) {
                currentStep--;
                showStep(currentStep);
            } else {
                Toast.makeText(this, "זה השלב הראשון!", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void showStep(int stepIndex) {
        currentStep = stepIndex;

        // כותרת השלב
        String[] stepNames = {
                "הנח את הבלוק הראשון",
                "הנח את הבלוק השני",
                "הנח את הבלוק השלישי"
        };
        String title = (stepIndex < stepNames.length)
                ? stepNames[stepIndex]
                : "שלב " + (stepIndex + 1);

        tvStepTitle.setText("שלב " + (stepIndex + 1) + " מתוך " + steps.size() + ": " + title);

        // עדכון כפתורים
        btnPrev.setEnabled(stepIndex > 0);
        btnNext.setEnabled(stepIndex < steps.size() - 1);

        // הצגת הלוח
        SolutionBoardAdapter adapter = new SolutionBoardAdapter(this, steps.get(stepIndex));
        gridSolutionBoard.setAdapter(adapter);
    }
}
