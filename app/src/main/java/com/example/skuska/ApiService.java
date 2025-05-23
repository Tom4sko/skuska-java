package com.example.skuska;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

public class ApiService {
    private static final String BASE_URL = "http://10.0.2.2/task_manager/"; // Use 10.0.2.2 for localhost in Android emulator
    private static final String UPLOAD_ENDPOINT = "upload_tasks.php";
    private static final String DOWNLOAD_ENDPOINT = "download_tasks.php";

    public interface ApiCallback {
        void onSuccess(List<Task> tasks);
        void onError(String error);
    }

    public void uploadTasks(List<Task> tasks, ApiCallback callback) {
        new AsyncTask<List<Task>, Void, String>() {
            @Override
            protected String doInBackground(List<Task>... params) {
                try {
                    URL url = new URL(BASE_URL + UPLOAD_ENDPOINT);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONArray jsonArray = new JSONArray();
                    for (Task task : tasks) {
                        JSONObject jsonTask = new JSONObject();
                        jsonTask.put("id", task.getId());
                        jsonTask.put("title", task.getTitle());
                        jsonTask.put("description", task.getDescription());
                        jsonTask.put("completed", task.isCompleted());
                        jsonArray.put(jsonTask);
                    }

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonArray.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                        br.close();
                        return response.toString();
                    }
                    return "Error: " + responseCode;
                } catch (IOException | JSONException e) {
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("Error")) {
                    callback.onError(result);
                } else {
                    callback.onSuccess(tasks);
                }
            }
        }.execute(tasks);
    }

    public void downloadTasks(ApiCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(BASE_URL + DOWNLOAD_ENDPOINT);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                        br.close();
                        return response.toString();
                    }
                    return "Error: " + responseCode;
                } catch (IOException e) {
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                try {
                    if (result.startsWith("Error")) {
                        callback.onError(result);
                    } else {
                        JSONArray jsonArray = new JSONArray(result);
                        List<Task> tasks = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonTask = jsonArray.getJSONObject(i);
                            Task task = new Task(
                                jsonTask.getInt("id"),
                                jsonTask.getString("title"),
                                jsonTask.getString("description"),
                                jsonTask.getBoolean("completed")
                            );
                            tasks.add(task);
                        }
                        callback.onSuccess(tasks);
                    }
                } catch (JSONException e) {
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }
        }.execute();
    }
} 