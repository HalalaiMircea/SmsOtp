package com.example.smsotp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.model.User;
import com.example.smsotp.model.UserDao;

import java.util.List;
import java.util.stream.Collectors;

public class MainViewModel extends AndroidViewModel {

    private final LiveData<List<UserItem>> userItemsData;

    public MainViewModel(@NonNull Application application) {
        super(application);
        UserDao userDao = AppDatabase.getInstance(application).userDao();
        LiveData<List<User>> usersLiveData = userDao.getAll();
        userItemsData = Transformations.map(usersLiveData, MainViewModel::apply);
    }

    private static List<UserItem> apply(List<User> userList) {
        return userList.stream()
                .map(user -> new UserItem(user.id, user.username))
                .collect(Collectors.toList());
    }

    public LiveData<List<UserItem>> getUserItemsData() {
        return userItemsData;
    }

    public static class UserItem {
        private final int id;
        private final String username;

        public UserItem(int id, String username) {
            this.id = id;
            this.username = username;
        }

        public int getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }
    }
}