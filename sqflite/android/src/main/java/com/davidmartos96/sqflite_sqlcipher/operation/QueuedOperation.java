package com.davidmartos96.sqflite_sqlcipher.operation;

public class QueuedOperation {
    final Operation operation;
    final Runnable runnable;

    public QueuedOperation(Operation operation, Runnable runnable) {
        this.operation = operation;
        this.runnable = runnable;
    }

    public void run() {
        runnable.run();
    }
}
