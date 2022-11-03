package com.davidmartos96.sqflite_sqlcipher.operation;


import com.davidmartos96.sqflite_sqlcipher.SqlCommand;

import java.util.List;

import static com.davidmartos96.sqflite_sqlcipher.Constant.PARAM_CONTINUE_OR_ERROR;
import static com.davidmartos96.sqflite_sqlcipher.Constant.PARAM_IN_TRANSACTION;
import static com.davidmartos96.sqflite_sqlcipher.Constant.PARAM_NO_RESULT;
import static com.davidmartos96.sqflite_sqlcipher.Constant.PARAM_SQL;
import static com.davidmartos96.sqflite_sqlcipher.Constant.PARAM_SQL_ARGUMENTS;
import static com.davidmartos96.sqflite_sqlcipher.Constant.PARAM_TRANSACTION_ID;

/**
 * Created by alex on 09/01/18.
 */

public abstract class BaseReadOperation implements Operation {
    private String getSql() {
        return getArgument(PARAM_SQL);
    }

    private List<Object> getSqlArguments() {
        return getArgument(PARAM_SQL_ARGUMENTS);
    }

    @Nullable
    public Integer getTransactionId() {
        return getArgument(PARAM_TRANSACTION_ID);
    }

    public boolean hasNullTransactionId() {
        return hasArgument(PARAM_TRANSACTION_ID) && getTransactionId() == null;
    }

    public SqlCommand getSqlCommand() {
        return new SqlCommand(getSql(), getSqlArguments());
    }

    public Boolean getInTransactionChange() {
        return getBoolean(PARAM_IN_TRANSACTION_CHANGE);
    }

    @Override
    public boolean getNoResult() {
        return Boolean.TRUE.equals(getArgument(PARAM_NO_RESULT));
    }

    @Override
    public boolean getContinueOnError() {
        return Boolean.TRUE.equals(getArgument(PARAM_CONTINUE_OR_ERROR));
    }

    private Boolean getBoolean(String key) {
        Object value = getArgument(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    // We actually have an inner object that does the implementation
    protected abstract OperationResult getOperationResult();

    @NonNull
    @Override
    public String toString() {
        return "" + getMethod() + " " + getSql() + " " + getSqlArguments();
    }
}
