package com.example.smsotp.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = CASCADE))
public class Command {
    @PrimaryKey(autoGenerate = true)
    public int id;

    //TODO index this foreign key reference
    public int userId;

    public String status;

    public String paramsJson;
}
