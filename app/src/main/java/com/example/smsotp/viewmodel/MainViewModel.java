package com.example.smsotp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.dao.UserDao;
import com.example.smsotp.entity.User;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final UserDao userDao;
    private LiveData<List<User>> usersLiveData;

    public MainViewModel(@NonNull Application application) {
        super(application);
        userDao = AppDatabase.getInstance(application).userDao();
    }

    public LiveData<List<User>> getUsers() {
        if (usersLiveData == null)
            usersLiveData = userDao.getAll();
        return usersLiveData;
    }
}