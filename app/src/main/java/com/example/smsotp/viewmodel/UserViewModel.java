package com.example.smsotp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.dao.UserDao;
import com.example.smsotp.entity.User;

public class UserViewModel extends AndroidViewModel {
    private final UserDao userDao;
    private LiveData<User> userLiveData;

    public UserViewModel(@NonNull Application application) {
        super(application);
        userDao = AppDatabase.getInstance(application).userDao();
    }

    public LiveData<User> getUser(int userId) {
        if (userLiveData == null)
            userLiveData = userDao.getById(userId);
        return userLiveData;
    }

    public void insertUser(User user) {
        userDao.insert(user);
    }

    public void updateUser(String username, String password) {
        User currUser = userLiveData.getValue();
        assert currUser != null;
        currUser.username = username;
        currUser.password = password;
        userDao.update(currUser);
    }

    public void deleteUser() {
        Thread thread = new Thread(() -> userDao.delete(userLiveData.getValue()));
        thread.start();
    }
}
