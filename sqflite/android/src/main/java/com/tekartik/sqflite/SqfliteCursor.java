package com.tekartik.sqflite;

import net.sqlcipher.Cursor;

/**
 * Sqflite cursor
 */
public class SqfliteCursor {
    final int cursorId;
    final int pageSize;
    final Cursor cursor;

    public SqfliteCursor(int cursorId, int pageSize, Cursor cursor) {
        this.cursorId = cursorId;
        this.pageSize = pageSize;
        this.cursor = cursor;
    }
}
