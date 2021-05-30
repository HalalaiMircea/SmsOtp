package com.example.smsotp.ui;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import com.example.smsotp.sql.AppDatabase;
import com.example.smsotp.sql.CommandDao;
import com.example.smsotp.sql.User;
import com.example.smsotp.sql.UserDao;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserViewModel extends AndroidViewModel {
    private static final String TAG = "UserViewModel";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final UserDao userDao;
    private final CommandDao commandDao;
    private final LiveData<User> userLiveData;
    private final LiveData<Integer> commCountLiveData;
    private final int userId;

    public UserViewModel(@NonNull Application application, int userId) {
        super(application);
        userDao = AppDatabase.getInstance(application).userDao();
        commandDao = AppDatabase.getInstance(application).commandDao();
        userLiveData = userDao.getById(userId);
        commCountLiveData = commandDao.countForUserId(userId);
        this.userId = userId;
    }

    public boolean isCreating() {
        return userId == -1;
    }

    public LiveData<User> getUser() {
        return userLiveData;
    }

    public LiveData<Integer> getCommCount() {
        return commCountLiveData;
    }

    public LiveData<Boolean> addOrUpdateUser(String userText, String passText) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                // If we entered from main fragment through addNewUserAction
                if (isCreating()) {
                    userDao.insert(new User(userText, passText));
                } else {// Else we entered from existing userEditAction
                    User currUser = Objects.requireNonNull(userLiveData.getValue());
                    currUser.username = userText;
                    currUser.password = passText;
                    userDao.update(currUser);
                }
                success.postValue(true);
            } catch (SQLiteConstraintException ex) {
                Log.e(TAG, Objects.requireNonNull(ex.getMessage()));
                success.postValue(false);
            }
        });
        return success;
    }

    public void clearCommands() {
        executor.execute(() -> commandDao.deleteAllForUserId(userId));
    }

    public void deleteUser() {
        executor.execute(() -> userDao.delete(userLiveData.getValue()));
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
