package com.example.android.dogsapp.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DogDao {

    @Insert
    List<Long> insertAll(DogBreed... dogs);

    @Query("SELECT * FROM dogBreed")
    List<DogBreed> getAllDogs();

    @Query("SELECT * FROM dogBreed WHERE uuid =:dogId")
    DogBreed getDog(int dogId);

    @Query("DELETE FROM dogBreed")
    void deleteAllDog();
}
