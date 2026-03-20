package com.example.blockblastsolver;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
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

        gridSolutionBoard = findViewById(R.id.gridSolutionBoard);
        tvStepTitle       = findViewById(R.id.tvStepTitle);
        btnPrev           = findViewById(R.id.btnPrev);
        btnNext           = findViewById(R.id.btnNext);
        btnBack           = findViewById(R.id.btnBack);

        // קבלת כל השלבים מה-Intent
        int stepCount = getIntent().getIntExtra("STEP_COUNT", 0);
        for (int i = 0; i < stepCount; i++) {
            int[] step = getIntent().getIntArrayExtra("STEP_" + i);
            if (step != null) steps.add(step);
        }

        if (steps.isEmpty()) {
            tvStepTitle.setText("לא נמצא פתרון");
            return;
        }

        showStep(0);

        btnNext.setOnClickListener(v -> {
            if (currentStep < steps.size() - 1) {
                currentStep++;
                showStep(currentStep);
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentStep > 0) {
                currentStep--;
                showStep(currentStep);
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void showStep(int stepIndex) {
        currentStep = stepIndex;

        String[] stepNames = {"הנח את הבלוק הראשון", "הנח את הבלוק השני", "הנח את הבלוק השלישי"};
        String title = (stepIndex < stepNames.length) ? stepNames[stepIndex] : "שלב " + (stepIndex + 1);
        tvStepTitle.setText("שלב " + (stepIndex + 1) + ": " + title);

        btnPrev.setEnabled(stepIndex > 0);
        btnNext.setEnabled(stepIndex < steps.size() - 1);

        SolutionBoardAdapter adapter = new SolutionBoardAdapter(this, steps.get(stepIndex));
        gridSolutionBoard.setAdapter(adapter);
    }
}
