package clas.hci.calculator;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Calculation {
    @PrimaryKey(autoGenerate = true) private int cid;
    int getCid() {return cid;}
    void setCid(int cid) {this.cid = cid;}

    @ColumnInfo(name = "expression") private String expression;
    String getExpression() {return expression;}

    @ColumnInfo(name = "result") private double result;
    double getResult() {return result;}

    @ColumnInfo(name = "utc") private long utc;
    long getUtc() {return utc;}

    Calculation(String expression, double result, long utc){
        this.expression = expression;
        this.result = result;
        this.utc = utc;
    }
}
