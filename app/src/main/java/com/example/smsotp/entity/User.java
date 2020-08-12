package com.example.smsotp.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = @Index(value = {"username"}, unique = true))
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String username;

    @NonNull
    public String password;

    public User(@NonNull String username, @NonNull String password) {
        this.username = username;
        this.password = password;
    }

    @NonNull
    @Override
    public String toString() {
        return id + " " + username + " " + password;
    }
}
