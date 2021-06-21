package com.example.smsotp.sql;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.example.smsotp.server.dto.SmsDto;
import com.example.smsotp.server.handlers.RestHandler;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static androidx.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "userId",
        onDelete = CASCADE),
        indices = @Index(value = {"userId"}))
@TypeConverters({Command.Converters.class})
public class Command {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;

    public String message;

    public List<SmsDto.Result> phoneResults; // Converted to Json Format

    @NonNull
    public Date executedDate;

    public Command(int userId, String message, List<SmsDto.Result> phoneResults,
                   @NonNull Date executedDate) {
        this.userId = userId;
        this.message = message;
        this.phoneResults = phoneResults;
        this.executedDate = Objects.requireNonNull(executedDate);
    }

    @NonNull
    @Override
    public String toString() {
        return "Command{" +
                "id=" + id +
                ", userId=" + userId +
                ", phoneResults='" + phoneResults + '\'' +
                ", executedDate=" + executedDate +
                '}';
    }

    public static class Converters {
        private static final Type RESULT_LIST_TYPE = new TypeToken<List<SmsDto.Result>>() {
        }.getType();

        @TypeConverter
        public static List<SmsDto.Result> toResultList(String json) {
            return RestHandler.gson.fromJson(json, RESULT_LIST_TYPE);
        }

        @TypeConverter
        public static String fromResultList(List<SmsDto.Result> results) {
            return RestHandler.gson.toJson(results);
        }
    }
}
