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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RepositoryTest {


    Repository repository;
    
    @BeforeEach
    public void init() {
        Config config = mock(Config.class);
        when(config.getOptionalValue(Mockito.anyString(), Mockito.any())).thenReturn(Optional.empty());
        repository = new Repository();
    }

    @Test
    void testGetConfigValue_exception() throws SQLException {
        repository.selectOne = mock(PreparedStatement.class);
        when(repository.selectOne.executeQuery()).thenThrow(SQLException.class);
        assertNull(repository.getConfigValue("test"));
    }

    @Test
    void testGetConfigValue_none() throws SQLException {
        repository.selectOne = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(false);
        when(repository.selectOne.executeQuery()).thenReturn(rs);
        assertNull(repository.getConfigValue("test"));
    }

    @Test
    void testGetConfigValue_no_stmt() throws SQLException {
        assertNull(repository.getConfigValue("test"));
    }

    @Test
    void testGetConfigValue() throws SQLException {
        repository.selectOne = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getString(1)).thenReturn("value");
        when(repository.selectOne.executeQuery()).thenReturn(rs);
        assertEquals("value", repository.getConfigValue("test"));
    }

    @Test
    void testGetAllConfigValues_no_stmt() throws SQLException {
        assertEquals(0, repository.getAllConfigValues().size());
    }

    @Test
    void testGetAllConfigValues_exception() throws SQLException {
        repository.selectAll = mock(PreparedStatement.class);
        when(repository.selectAll.executeQuery()).thenThrow(SQLException.class);
        assertEquals(0, repository.getAllConfigValues().size());
    }

    @Test
    void testGetAllConfigValues() throws SQLException {
        repository.selectAll = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true).thenReturn(false);
        when(rs.getString(1)).thenReturn("test");
        when(rs.getString(2)).thenReturn("value");
        when(repository.selectAll.executeQuery()).thenReturn(rs);
        assertEquals(1, repository.getAllConfigValues().size());
    }

}
