package com.trace.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PostgreSQL TEXT[] 与 Java List<String> 互转
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.ARRAY)
public class ListStringTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> list, JdbcType jdbcType) throws SQLException {
        String[] arr = list.toArray(new String[0]);
        Array sqlArr = ps.getConnection().createArrayOf("text", arr);
        ps.setArray(i, sqlArr);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array arr = rs.getArray(columnName);
        return arr != null ? new ArrayList<>(Arrays.asList((String[]) arr.getArray())) : new ArrayList<>();
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array arr = rs.getArray(columnIndex);
        return arr != null ? new ArrayList<>(Arrays.asList((String[]) arr.getArray())) : new ArrayList<>();
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array arr = cs.getArray(columnIndex);
        return arr != null ? new ArrayList<>(Arrays.asList((String[]) arr.getArray())) : new ArrayList<>();
    }
}
