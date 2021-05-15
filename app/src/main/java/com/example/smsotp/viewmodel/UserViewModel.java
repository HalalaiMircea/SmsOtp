package com.example.smsotp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.model.CommandDao;
import com.example.smsotp.model.User;
import com.example.smsotp.model.UserDao;

import java.util.Objects;

public class UserViewModel extends AndroidViewModel {

    private final UserDao userDao;
    private final CommandDao commandDao;
    private final LiveData<User> userLiveData;
    private final LiveData<Integer> commCountLiveData;

    public UserViewModel(@NonNull Application application, int userId) {
        super(application);
        userDao = AppDatabase.getInstance(application).userDao();
        commandDao = AppDatabase.getInstance(application).commandDao();
        userLiveData = userDao.getById(userId);
        commCountLiveData = commandDao.countForUserId(userId);
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

    public static class Factory extends ViewModelProvider.AndroidViewModelFactory {
        private final Application mApplication;
        private final int mUserId;

        public Factory(@NonNull Application application, int userId) {
            super(application);
            this.mApplication = application;
            this.mUserId = userId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass == UserViewModel.class) {
                //noinspection unchecked
                return (T) new UserViewModel(mApplication, mUserId);
            }
            throw new IllegalArgumentException("UserViewModel class not found");
        }
    }
}
