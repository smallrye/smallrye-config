/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config.source.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import lombok.extern.java.Log;

@Log
public class Repository {
    
    PreparedStatement selectOne = null;
    PreparedStatement selectAll = null;
    
    public Repository(){}
    
    public Repository(String datasource, String table, String keyColumn, String valueColumn) {
        DataSource ds = getDatasource(datasource);
        if (datasource != null) {
            String queryOne = "select " + valueColumn + " from " +table + " where " + keyColumn + " = ?";
            String queryAll = "select " + keyColumn + ", " + valueColumn + " from " + table;
            try {
                selectOne = ds.getConnection().prepareStatement(queryOne);
                selectAll = ds.getConnection().prepareStatement(queryAll);
            } catch (SQLException e) {
                log.log(Level.FINE, () -> "Configuration query could not be prepared: " + e.getMessage());
            }
        }
    }
    
    public Map<String, String> getAllConfigValues() {
        Map<String, String> result = new HashMap<>();
        if (selectAll != null) {
            try {
                ResultSet rs = selectAll.executeQuery();
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getString(2));
                }
            } catch (SQLException e) {
                log.log(Level.FINE, () -> "query for config values failed:}" + e.getMessage());
            }
        }
        return result;
    }
    
    public String getConfigValue(String key) {
        if (selectOne != null) {
            try {
                selectOne.setString(1, key);
                ResultSet rs = selectOne.executeQuery();
                if (rs.next()) {
                    return rs.getString(1);
                }
            } catch (SQLException e) {
                log.log(Level.FINE, () -> "query for config value failed: " + e.getMessage());
            }
        }
        return null;
    }
    
    private DataSource getDatasource(String jndi) {
        try {
            return (DataSource) InitialContext.doLookup(jndi);
        } catch (NamingException e) {
            log.log(Level.WARNING, () -> "Could not get datasource: " + e.getMessage());
            return null;
        }
    }
}