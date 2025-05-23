<?php
header('Content-Type: application/json');
require_once 'db_connect.php';

try {
    // Get all tasks from database
    $stmt = $conn->prepare("SELECT * FROM tasks");
    $stmt->execute();
    $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Convert boolean values to proper format
    foreach ($tasks as &$task) {
        $task['completed'] = (bool)$task['completed'];
    }

    echo json_encode($tasks);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
}
?> 