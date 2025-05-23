package com.example.skuska;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {
    private DatabaseHelper dbHelper;
    private ApiService apiService;
    private TaskAdapter adapter;
    private List<Task> taskList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        apiService = new ApiService();
        taskList = new ArrayList<>();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(taskList, this);
        recyclerView.setAdapter(adapter);

        Button btnAddTask = findViewById(R.id.btnAddTask);
        Button btnUpload = findViewById(R.id.btnUpload);
        Button btnDownload = findViewById(R.id.btnDownload);

        btnAddTask.setOnClickListener(v -> showAddTaskDialog());
        btnUpload.setOnClickListener(v -> uploadTasks());
        btnDownload.setOnClickListener(v -> downloadTasks());

        loadTasks();
    }

    private void loadTasks() {
        taskList.clear();
        taskList.addAll(dbHelper.getAllTasks());
        adapter.updateTasks(taskList);
    }

    private void showAddTaskDialog(Task existingTask) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_task, null);
        EditText editTextTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextDescription);
        CheckBox checkBoxCompleted = dialogView.findViewById(R.id.checkBoxCompleted);

        if (existingTask != null) {
            editTextTitle.setText(existingTask.getTitle());
            editTextDescription.setText(existingTask.getDescription());
            checkBoxCompleted.setChecked(existingTask.isCompleted());
        }

        new AlertDialog.Builder(this)
                .setTitle(existingTask == null ? "Add Task" : "Edit Task")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = editTextTitle.getText().toString().trim();
                    String description = editTextDescription.getText().toString().trim();
                    boolean isCompleted = checkBoxCompleted.isChecked();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (existingTask == null) {
                        Task newTask = new Task(0, title, description, isCompleted);
                        long id = dbHelper.addTask(newTask);
                        newTask.setId((int) id);
                        taskList.add(newTask);
                    } else {
                        existingTask.setTitle(title);
                        existingTask.setDescription(description);
                        existingTask.setCompleted(isCompleted);
                        dbHelper.updateTask(existingTask);
                    }
                    adapter.updateTasks(taskList);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddTaskDialog() {
        showAddTaskDialog(null);
    }

    @Override
    public void onTaskClick(Task task) {
        showAddTaskDialog(task);
    }

    @Override
    public void onTaskStatusChanged(Task task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        dbHelper.updateTask(task);
    }

    @Override
    public void onTaskDelete(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteTask(task.getId());
                    taskList.remove(task);
                    adapter.updateTasks(taskList);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadTasks() {
        apiService.uploadTasks(taskList, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Tasks uploaded successfully", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Upload failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void downloadTasks() {
        apiService.downloadTasks(new ApiService.ApiCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                runOnUiThread(() -> {
                    // Clear local database
                    dbHelper.deleteAllTasks();
                    
                    // Add downloaded tasks to local database
                    for (Task task : tasks) {
                        dbHelper.addTask(task);
                    }
                    
                    // Update UI
                    loadTasks();
                    Toast.makeText(MainActivity.this, "Tasks downloaded successfully", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Download failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}