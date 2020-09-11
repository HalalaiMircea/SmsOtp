package com.example.smsotp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smsotp.entity.User;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM user WHERE id = :id")
    LiveData<User> getById(int id);

    @Query("SELECT id FROM user WHERE username = :username")
    int getIdByUsername(String username);

    @Query("SELECT password FROM user WHERE username = :username")
    String getPasswordByUsername(String username);

    @Query("SELECT * FROM user")
    LiveData<List<User>> getAll();
}
