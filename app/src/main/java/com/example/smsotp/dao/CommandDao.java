package com.example.smsotp.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.smsotp.entity.Command;

@Dao
public interface CommandDao {
    @Insert
    void insert(Command command);

    @Delete
    void delete(Command command);

    @Query("DELETE FROM command")
    void deleteAll();
}
