package com.example.smsotp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.dao.CommandDao;
import com.example.smsotp.dao.UserDao;
import com.example.smsotp.entity.User;

import java.util.Objects;

public class UserViewModel extends AndroidViewModel {

    private final UserDao userDao;
    private final CommandDao commandDao;
    private LiveData<User> userLiveData;
    private LiveData<Integer> commCountLiveData;

    public UserViewModel(@NonNull Application application) {
        super(application);
        userDao = AppDatabase.getInstance(application).userDao();
        commandDao = AppDatabase.getInstance(application).commandDao();
    }

    /**
     * Must be called by user after getting this view model from a provider
     *
     * @param userId id of the user held in this view model
     */
    public void init(int userId) {
        if (userLiveData == null) {
            userLiveData = userDao.getById(userId);
            commCountLiveData = commandDao.countForUserId(userId);
        }
    }

    public int getUserId() {
        return Objects.requireNonNull(userLiveData.getValue()).id;
    }

    public LiveData<User> getUser() {
        return userLiveData;
    }

    public LiveData<Integer> getCommCount() {
        return commCountLiveData;
    }

    public void updateUser(String username, String password) {
        User currUser = userLiveData.getValue();
        assert currUser != null;
        currUser.username = username;
        currUser.password = password;
        userDao.update(currUser);
    }

    public void clearCommands() {
        new Thread(() -> commandDao.deleteAllForUserId(getUserId())).start();
    }

    public void deleteUser() {
        new Thread(() -> userDao.delete(userLiveData.getValue())).start();
    }
}
