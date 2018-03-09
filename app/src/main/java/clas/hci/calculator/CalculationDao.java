package clas.hci.calculator;

import android.arch.persistence.room.Dao;
//import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface CalculationDao {
    @Query("SELECT * FROM Calculation") List<Calculation> getAll();

    @Insert void insertCalculation(Calculation... calculations);

//    @Delete void delete(Calculation calculation);

//    @Query("DELETE FROM Calculation") void deleteAll();
}
