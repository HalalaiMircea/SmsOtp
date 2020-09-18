package com.example.smsotp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.smsotp.dao.CommandDao;
import com.example.smsotp.dao.UserDao;
import com.example.smsotp.entity.Command;
import com.example.smsotp.entity.User;

import java.util.Date;

@Database(entities = {User.class, Command.class}, version = 1)
@TypeConverters({AppDatabase.Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "SMSOTP_DB";
    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance != null) {
            return instance;
        }
        synchronized (AppDatabase.class) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "smsotp")
                    .fallbackToDestructiveMigration()
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            new Thread(() -> {
                                instance.userDao().insert(new User("admin", "test"));
                                Log.e(TAG, "Inserted test User record... Remove this before shipping app!");
                            }).start();
                        }

                        @Override
                        public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                            Log.e(TAG, "Destructive migration done.");
                        }
                    })
                    .build();
            Log.i(TAG, "DB location=" + instance.getOpenHelper().getWritableDatabase().getPath());
            return instance;
        }
    }

    public abstract UserDao userDao();

    public abstract CommandDao commandDao();

    public static class Converters {
        @TypeConverter
        public static Date fromTimestamp(long value) {
            return new Date(value);
        }

        @TypeConverter
        public static long dateToTimestamp(Date date) {
            return date.getTime();
        }
    }
}
