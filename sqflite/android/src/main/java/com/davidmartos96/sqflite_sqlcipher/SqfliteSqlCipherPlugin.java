package com.davidmartos96.sqflite_sqlcipher;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import com.tekartik.sqflite.operation.MethodCallOperation;

import com.tekartik.sqflite.SqflitePlugin;
import com.tekartik.sqflite.Database;
import com.tekartik.sqflite.LogLevel;

import static com.tekartik.sqflite.Constant.PARAM_READ_ONLY;
import static com.tekartik.sqflite.Constant.PARAM_PATH;
import static com.tekartik.sqflite.Constant.PARAM_SINGLE_INSTANCE;
import static com.tekartik.sqflite.Constant.TAG;
import static com.tekartik.sqflite.Constant.SQLITE_ERROR;
import static com.tekartik.sqflite.Constant.ERROR_OPEN_FAILED;

import static com.davidmartos96.sqflite_sqlcipher.Constant.PARAM_PASSWORD;
import static com.davidmartos96.sqflite_sqlcipher.Constant.PLUGIN_KEY;

/**
 * SqfliteSqlCipherPlugin Android implementation
 */
public class SqfliteSqlCipherPlugin extends SqflitePlugin {
    @Override
    protected void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        this.context = applicationContext;
        SQLiteDatabase.loadLibs(applicationContext);
        methodChannel = new MethodChannel(messenger, PLUGIN_KEY);
        methodChannel.setMethodCallHandler(this);
    }

    //
    // Sqflite.open
    //
    @Override
    protected void onOpenDatabaseCall(final MethodCall call, final Result result) {
        final String path = call.argument(PARAM_PATH);
        final Boolean readOnly = call.argument(PARAM_READ_ONLY);
        final String password = call.argument(PARAM_PASSWORD);
        final boolean inMemory = isInMemoryPath(path);

        final boolean singleInstance = !Boolean.FALSE.equals(call.argument(PARAM_SINGLE_INSTANCE)) && !inMemory;

        // For single instance we create or reuse a thread right away
        // DO NOT TRY TO LOAD existing instance, the database has been closed


        if (singleInstance) {
            // Look for in memory instance
            synchronized (databaseMapLocker) {
                if (LogLevel.hasVerboseLevel(logLevel)) {
                    Log.d(TAG, "Look for " + path + " in " + SqflitePlugin._singleInstancesByPath.keySet());
                }
                Integer databaseId = SqflitePlugin._singleInstancesByPath.get(path);
                if (databaseId != null) {
                    Database database = databaseMap.get(databaseId);
                    if (database != null) {
                        if (!database.sqliteDatabase.isOpen()) {
                            if (LogLevel.hasVerboseLevel(logLevel)) {
                                Log.d(TAG, database.getThreadLogPrefix() + "single instance database of " + path + " not opened");
                            }
                        } else {
                            if (LogLevel.hasVerboseLevel(logLevel)) {
                                Log.d(TAG, database.getThreadLogPrefix() + "re-opened single instance " + (database.inTransaction ? "(in transaction) " : "") + databaseId + " " + path);
                            }
                            result.success(SqflitePlugin.makeOpenResult(databaseId, true, database.inTransaction));
                            return;
                        }
                    }
                }
            }
        }

        // Generate new id
        int newDatabaseId;
        synchronized (databaseMapLocker) {
            newDatabaseId = ++databaseId;
        }
        final int databaseId = newDatabaseId;

        final Database database = new SqfliteSqlCipherDatabase(context, path, password, databaseId, singleInstance, logLevel);

        synchronized (databaseMapLocker) {
            // Create handler if necessary
            if (handler == null) {
                handlerThread = new HandlerThread("Sqflite", SqflitePlugin.THREAD_PRIORITY);
                handlerThread.start();
                handler = new Handler(handlerThread.getLooper());
                if (LogLevel.hasSqlLevel(database.logLevel)) {
                    Log.d(TAG, database.getThreadLogPrefix() + "starting thread" + handlerThread + " priority " + SqflitePlugin.THREAD_PRIORITY);
                }
            }
            database.handler = handler;
            if (LogLevel.hasSqlLevel(database.logLevel)) {
                Log.d(TAG, database.getThreadLogPrefix() + "opened " + databaseId + " " + path);
            }


            // Open in background thread
            handler.post(
                    () -> {

                        synchronized (openCloseLocker) {

                            if (!inMemory) {
                                File file = new File(path);
                                File directory = new File(file.getParent());
                                if (!directory.exists()) {
                                    if (!directory.mkdirs()) {
                                        if (!directory.exists()) {
                                            result.error(SQLITE_ERROR, ERROR_OPEN_FAILED + " " + path, null);
                                            return;
                                        }
                                    }
                                }
                            }

                            // force opening
                            try {
                                if (Boolean.TRUE.equals(readOnly)) {
                                    database.openReadOnly();
                                } else {
                                    database.open();
                                }
                            } catch (Exception e) {
                                MethodCallOperation operation = new MethodCallOperation(call, result);
                                database.handleException(e, operation);
                                return;
                            }

                            synchronized (databaseMapLocker) {
                                if (singleInstance) {
                                    SqflitePlugin._singleInstancesByPath.put(path, databaseId);
                                }
                                SqflitePlugin.databaseMap.put(databaseId, database);
                            }
                            if (LogLevel.hasSqlLevel(database.logLevel)) {
                                Log.d(TAG, database.getThreadLogPrefix() + "opened " + databaseId + " " + path);
                            }
                        }

                        result.success(SqflitePlugin.makeOpenResult(databaseId, false, false));
                    });
        }

    }
}
