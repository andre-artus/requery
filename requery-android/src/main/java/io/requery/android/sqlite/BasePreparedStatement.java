/*
 * Copyright 2016 requery.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.requery.android.sqlite;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * {@link PreparedStatement} implementation using Android's local SQLite database.
 */
public abstract class BasePreparedStatement extends BaseStatement implements PreparedStatement {

    private static final char[] hex = "0123456789ABCDEF".toCharArray();

    protected final String sql;
    protected final int autoGeneratedKeys;
    protected List<Object> bindings;

    protected BasePreparedStatement(BaseConnection connection, String sql, int autoGeneratedKeys)
        throws SQLException {
        super(connection);
        if (sql == null) {
            throw new SQLException("null sql");
        }
        if (autoGeneratedKeys != RETURN_GENERATED_KEYS) {
            bindings = new ArrayList<>(4);
        }
        this.sql = sql;
        this.autoGeneratedKeys = autoGeneratedKeys;
    }

    protected abstract void bindNullOrString(int index, Object value);

    protected abstract void bindLong(int index, long value);

    protected abstract void bindDouble(int index, double value);

    protected abstract void bindBlob(int index, byte[] value);

    protected String[] bindingsToArray() {
        String[] args = new String[bindings.size()];
        for (int i = 0; i < bindings.size() ; i++) {
            Object value = bindings.get(i);
            if (value != null) {
                args[i] = value.toString();
            }
        }
        return args;
    }

    protected static String byteToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(hex[(b >> 4) & 0xF]);
            sb.append(hex[(b & 0xF)]);
        }
        return sb.toString();
    }

    @Override
    public void addBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        bindNullOrString(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        setBytes(parameterIndex, x.getBytes(0, (int) x.length()));
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        long value = x ? 1 : 0;
        bindLong(parameterIndex, value);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        bindLong(parameterIndex, (long) x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        bindBlob(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        setDate(parameterIndex, x, null);
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        if (x == null) {
            bindNullOrString(parameterIndex, null);
        } else {
            bindLong(parameterIndex, x.getTime());
        }
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        bindDouble(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        bindDouble(parameterIndex, (double) x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        bindLong(parameterIndex, (long) x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        bindLong(parameterIndex, x);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        bindNullOrString(parameterIndex, null);
    }

    @Override
    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
        bindNullOrString(paramIndex, null);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        if (x == null) {
            setNull(parameterIndex, Types.NULL);
        } else {
            if (x instanceof String) {
                setString(parameterIndex, x.toString());
            } else if (x instanceof Byte) {
                setByte(parameterIndex, (Byte) x);
            } else if (x instanceof Short) {
                setShort(parameterIndex, (Short) x);
            } else if (x instanceof Integer) {
                setInt(parameterIndex, (Integer) x);
            } else if (x instanceof Long) {
                setLong(parameterIndex, (Long) x);
            } else if (x instanceof Double) {
                setDouble(parameterIndex, (Double) x);
            } else if (x instanceof Float) {
                setFloat(parameterIndex, (Float) x);
            } else if (x instanceof Boolean) {
                setLong(parameterIndex, (Boolean) x ? 1 : 0);
            } else if (x instanceof byte[]) {
                setBytes(parameterIndex, (byte[]) x);
            } else if (x instanceof Date) {
                setDate(parameterIndex, (Date) x);
            } else if (x instanceof java.util.Date) {
                java.util.Date date = (java.util.Date) x;
                setDate(parameterIndex, new Date(date.getTime()));
            } else {
                throw new SQLException("unhandled type " + x.getClass().getCanonicalName());
            }
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType)
            throws SQLException {
        if (x == null || targetSqlType == Types.NULL) {
            setNull(parameterIndex, Types.NULL);
            return;
        }
        switch (targetSqlType) {
            case Types.INTEGER:
            case Types.SMALLINT:
                if (x instanceof Integer) {
                    bindLong(parameterIndex, ((Integer) x).longValue());
                } else if (x instanceof Long) {
                    bindLong(parameterIndex, (Long) x);
                } else if (x instanceof Short) {
                    bindLong(parameterIndex, ((Short) x).longValue());
                }
                break;
            case Types.TINYINT:
                if (x instanceof Byte) {
                    bindLong(parameterIndex, ((Byte) x).longValue());
                }
                break;
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                if (x instanceof Double) {
                    setDouble(parameterIndex, (Double) x);
                } else if (x instanceof Float) {
                    setFloat(parameterIndex, (Float) x);
                }
                break;
            case Types.BLOB:
            case Types.BINARY:
            case Types.VARBINARY:
                setBytes(parameterIndex, (byte[]) x);
                break;
            case Types.BOOLEAN:
                Boolean value = (Boolean) x;
                setBoolean(parameterIndex, value);
                break;
            case Types.VARCHAR:
            case Types.NVARCHAR:
                String string = x instanceof String ?
                        (String) x : x.toString();
                setString(parameterIndex, string);
                break;
            case Types.DATE:
                if (x instanceof Date) {
                    Date date = (Date) x;
                    setLong(parameterIndex, date.getTime());
                } else if (x instanceof java.util.Date) {
                    java.util.Date date = (java.util.Date) x;
                    setLong(parameterIndex, date.getTime());
                }
                break;
            case Types.TIMESTAMP:
                if (x instanceof Timestamp) {
                    Timestamp timestamp = (Timestamp) x;
                    setLong(parameterIndex, timestamp.getTime());
                }
                break;
            case Types.BIGINT:
                if (x instanceof BigInteger) {
                    BigInteger bigInteger = (BigInteger) x;
                    bindNullOrString(parameterIndex, bigInteger.toString());
                }
                break;
            default:
                throw new SQLException("unhandled type " + targetSqlType);
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        bindLong(parameterIndex, (long) x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        bindNullOrString(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        if (x == null) {
            bindNullOrString(parameterIndex, null);
        } else {
            bindLong(parameterIndex, x.getTime());
        }
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        if (x == null) {
            bindNullOrString(parameterIndex, null);
        } else {
            bindLong(parameterIndex, x.getTime());
        }
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        if (x == null) {
            bindNullOrString(parameterIndex, null);
        } else {
            bindLong(parameterIndex, x.getTime());
        }
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
        throws SQLException {
        if (x == null) {
            bindNullOrString(parameterIndex, null);
        } else {
            bindLong(parameterIndex, x.getTime());
        }
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        bindNullOrString(parameterIndex, x);
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setNString(int parameterIndex, String x) throws SQLException {
        bindNullOrString(parameterIndex, x);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream inputStream, long length)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream inputStream, long length)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String toString() {
        return sql;
    }
}
