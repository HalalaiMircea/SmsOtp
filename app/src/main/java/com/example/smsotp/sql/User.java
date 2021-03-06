package com.example.smsotp.sql;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(indices = @Index(value = {"username"}, unique = true))
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public @NonNull String username;
    public @NonNull String password;

    public User(@NonNull String username, @NonNull String password) {
        this.username = Objects.requireNonNull(username);
        this.password = Objects.requireNonNull(password);
    }

    @NonNull
    @Override
    public String toString() {
        return id + " " + username;
    }
}
