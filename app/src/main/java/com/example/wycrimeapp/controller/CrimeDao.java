package com.example.wycrimeapp.controller;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.wycrimeapp.model.Crime;

import java.util.List;

@Dao
public interface CrimeDao {

    @Query("SELECT * FROM crime")
    List<Crime> getAllCrimes();

    @Query("SELECT * FROM crime WHERE crimeId = :id")
    Crime getCrimeById(String id);

    @Insert
    void insertCrime(Crime crime);

    @Update
    void updateCrime(Crime crime);

    @Delete
    void deleteCrime(Crime crime);

    @Query("SELECT * FROM crime WHERE " +
            "crimeId LIKE '%' || :query || '%' OR " +
            "location LIKE '%' || :query || '%' OR " +
            "crimeType LIKE '%' || :query || '%' OR " +
            "lsoaName LIKE '%' || :query || '%'")
    List<Crime> searchCrimes(String query);

    // ADD THIS NEW METHOD HERE
    @Query("DELETE FROM crime")
    void deleteAllCrimes();
}