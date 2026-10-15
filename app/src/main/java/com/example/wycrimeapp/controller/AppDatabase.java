package com.example.wycrimeapp.controller;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.wycrimeapp.util.PasswordUtils;

import com.example.wycrimeapp.model.Crime;
import com.example.wycrimeapp.model.User;

@Database(entities = {Crime.class, User.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CrimeDao crimeDao();
    public abstract UserDao userDao();

    private static AppDatabase instance;

    /**
     * Hardcoded, pre-provisioned admin account.
     *
     * SECURITY NOTE (coursework requirement):
     * - No UI path exists to register as ADMIN.
     * - The only ADMIN account is inserted automatically on first install.
     */
    public static final String DEFAULT_ADMIN_EMAIL = "admin@wycrimeapp.local";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin@12345"; // Change for production.

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "crime_database"
                    ).allowMainThreadQueries()   // temporary – easy testing
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(SupportSQLiteDatabase db) {
                            super.onCreate(db);

                            // Insert the initial admin account during DB creation.
                            // Password is stored as a strong PBKDF2 hash (never plaintext).
                            String hashed = PasswordUtils.hashPassword(DEFAULT_ADMIN_PASSWORD);
                            db.execSQL(
                                    "INSERT INTO users(email, password, isAdmin) VALUES(?, ?, 1)",
                                    new Object[]{DEFAULT_ADMIN_EMAIL, hashed}
                            );
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}