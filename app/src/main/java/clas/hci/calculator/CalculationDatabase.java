package clas.hci.calculator;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Calculation.class}, version = 1,  exportSchema = false)

public abstract class CalculationDatabase extends RoomDatabase{
    public abstract CalculationDao dao();
}
