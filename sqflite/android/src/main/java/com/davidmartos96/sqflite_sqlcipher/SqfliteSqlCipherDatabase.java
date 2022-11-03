package com.davidmartos96.sqflite_sqlcipher;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.sqlcipher.DatabaseErrorHandler;
import com.tekartik.sqflite.Database;

import java.io.File;

import static com.tekartik.sqflite.Constant.TAG;

class SqfliteSqlCipherDatabase extends Database {
    final String password;

    SqfliteSqlCipherDatabase(Context context, String path, String password, int id, boolean singleInstance, int logLevel) {
        super(context, path, id, singleInstance, logLevel);
        this.password = (password != null) ? password : "";
    }

    @Override
    public void open() {
        openWithFlags(SQLiteDatabase.CREATE_IF_NECESSARY);
    }

    // Change default error handler to avoid erasing the existing file.
    @Override
    public void openReadOnly() {
        openWithFlags(SQLiteDatabase.OPEN_READONLY, new DatabaseErrorHandler() {
            @Override
            public void onCorruption(SQLiteDatabase dbObj) {
                // ignored
                // default implementation delete the file
                //
                // This happens asynchronously so cannot be tracked. However a simple
                // access should fail
            }
        });
    }

    private void openWithFlags(int flags) {
        openWithFlags(flags, null);
    }

    private void openWithFlags(int flags, DatabaseErrorHandler errorHandler) {
        try {
            this.sqliteDatabase = SQLiteDatabase.openDatabase(path, password, null, flags, null, errorHandler);

        }catch (Exception e) {
            Log.d(TAG, "Opening db in " + path + " with PRAGMA cipher_migrate");
            SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
                @Override
                public void preKey(net.sqlcipher.database.SQLiteDatabase database) {
                }

                @Override
                public void postKey(net.sqlcipher.database.SQLiteDatabase database) {
                    database.rawExecSQL("PRAGMA cipher_migrate;");
                }
            };

            this.sqliteDatabase = SQLiteDatabase.openDatabase(path, password, null, flags, hook, errorHandler);
        }
    }

    static void deleteDatabase(String path) {
        File file = new File(path);

        file.delete();
        new File(file.getPath() + "-journal").delete();
        new File(file.getPath() + "-shm").delete();
        new File(file.getPath() + "-wal").delete();
    }
}
