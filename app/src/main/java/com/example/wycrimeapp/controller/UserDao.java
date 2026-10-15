package com.example.wycrimeapp.controller;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.wycrimeapp.model.User;

@Dao
public interface UserDao {

    @Insert
    void registerUser(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User loginUser(String email);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User checkLogin(String email, String password);

    /**
     * Used to transparently upgrade legacy plaintext passwords to a hashed format after
     * successful login.
     */
    @Query("UPDATE users SET password = :newHashedPassword WHERE email = :email")
    void updatePassword(String email, String newHashedPassword);

    // ADD THIS LINE BELOW
    @Query("SELECT * FROM users")
    java.util.List<User> getAllUsers();
}