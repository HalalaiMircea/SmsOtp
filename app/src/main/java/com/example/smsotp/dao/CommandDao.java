package com.example.smsotp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.smsotp.entity.Command;

import java.util.List;

@Dao
public interface CommandDao {
    @Insert
    long insert(Command command);

    @Delete
    void delete(Command command);

    @Query("DELETE FROM command")
    void deleteAll();

    @Query("SELECT * FROM command")
    List<Command> getAll();

    @Query("SELECT COUNT(*) FROM command WHERE userId = :userId")
    LiveData<Integer> countForUserId(int userId);

    @Query("DELETE FROM command WHERE userId = :userId")
    void deleteAllForUserId(int userId);
}
