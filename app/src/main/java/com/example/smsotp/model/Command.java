package com.example.smsotp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.Objects;

import static androidx.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "userId",
        onDelete = CASCADE), indices = @Index(value = {"userId"}))
public class Command {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;

    public String jsonParams;

    @NonNull
    public Date executedDate;

    public Command(int userId, String jsonParams, @NonNull Date executedDate) {
        this.userId = userId;
        this.jsonParams = jsonParams;
        this.executedDate = Objects.requireNonNull(executedDate);
    }

    @NonNull
    @Override
    public String toString() {
        return "Command{" +
                "id=" + id +
                ", userId=" + userId +
                ", jsonParams='" + jsonParams + '\'' +
                ", executedDate=" + executedDate +
                '}';
    }
}
