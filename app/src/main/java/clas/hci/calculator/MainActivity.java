package clas.hci.calculator;

import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class MainActivity extends AppCompatActivity {
//    MAIN VIEW BINDING----------------------------------------------
    @BindView(R.id.bMode) AppCompatButton modeButton;
    @BindView(R.id.expressionScroll) ScrollView expressionScroll;
    @BindView(R.id.expression) TextView expressionDisplay;
    @BindView(R.id.preExpression) TextView preExpressionDisplay;
    @BindView(R.id.inputScroll) HorizontalScrollView inputScroll;
    @BindView(R.id.input) TextView inputDisplay;
    @BindView(R.id.bEqual) AppCompatButton equalButton;
    @BindView(R.id.bDivide) AppCompatButton divideButton;
    @BindView(R.id.bMulti) AppCompatButton multiplyButton;
    @BindView(R.id.bSubtract) AppCompatButton subtractButton;
    @BindView(R.id.bAdd) AppCompatButton addButton;
    @BindView(R.id.bTheme) ImageButton themeButton;



//    CUSTOM ENUMS----------------------------------------------
    private enum THEME {BLACK, WHITE, CUSTOM}
    private enum OPERATOR {NONE, DIVIDE, MULTIPLY, SUBTRACT, ADD}
    private enum MODE {RPN, INFIX}



//    MAIN VARIABLES----------------------------------------------
    MODE mode = MODE.INFIX;
    Stack<BigDecimal> rpnNumberStack = new Stack<>();

    BigDecimal oldA = null;
    BigDecimal result = null;
    BigDecimal numberA = null;
    BigDecimal numberB = null;

    String currentInput = "0";
    OPERATOR currentOperator = OPERATOR.NONE;



//    UPDATE FUNCTIONS-----------------------------------------
    private String getOperationCharacter(){
        if (currentOperator == OPERATOR.DIVIDE) {return getResources().getString(R.string.divide);}
        else if (currentOperator == OPERATOR.MULTIPLY) {return getResources().getString(R.string.multiply);}
        else if (currentOperator == OPERATOR.SUBTRACT) {return getResources().getString(R.string.subtract);}
        else if (currentOperator == OPERATOR.ADD) {return getResources().getString(R.string.add);}
        return "";
    }

    private String formatBigDecimal(BigDecimal num){
        String s = num.toString();
        s = !s.contains(".") ? s : s.replaceAll("0*$", "").replaceAll("\\.$", "");
        return s;
    }

    private void updateDisplay(){
        if (mode == MODE.RPN) {
            modeButton.setText(R.string.rpn);
            equalButton.setText(R.string.enter);
            equalButton.setTextSize(25);
        }
        else {
            modeButton.setText(R.string.infix);
            equalButton.setText(getResources().getString(R.string.equals));
            equalButton.setTextSize(50);
        }

        String expression = "";
        if (oldA != null) {expression = expression.concat(formatBigDecimal(oldA));}
        expression = expression.concat(" " + getOperationCharacter());
        if (numberB != null) {expression = expression.concat(" " + formatBigDecimal(numberB));}
        if (result != null) {expression = expression.concat(" = ");}
        if (mode == MODE.INFIX) {preExpressionDisplay.setText((expression));}
//        if (result != null) {expression = expression.concat(formatBigDecimal(result));}
//        result = null;

//        if (mode == MODE.INFIX){
//            if (expression.contains("=")) {
//                expressionDisplay.append("\n" + expression);
//            }
//        }
        if (mode == MODE.RPN){
            preExpressionDisplay.setText("");
            for (int i = 0; i < rpnNumberStack.size(); i++) {
                if (i > 0) {preExpressionDisplay.append(",  ");}
                preExpressionDisplay.append(formatBigDecimal(rpnNumberStack.get(i)));
            }
        }

        inputDisplay.setText(currentInput);

        inputScroll.post(new Runnable() {
            @Override public void run() { inputScroll.fullScroll(View.FOCUS_RIGHT); }
        });

        expressionScroll.post(new Runnable() {
            @Override public void run() { expressionScroll.fullScroll(View.FOCUS_DOWN); }
        });
    }

    private void clearAllVariables(){
        currentOperator = OPERATOR.NONE;
        currentInput = "0";
        result = null;
        oldA = null;
        numberA = null;
        numberB = null;
        rpnNumberStack.clear();

        int color;
        TypedArray themeArray = getTheme().obtainStyledAttributes(new int[] {android.R.attr.textColor});
        try {color = themeArray.getColor(0, 0);}
        finally {themeArray.recycle();}

        divideButton.setTextColor(color);
        multiplyButton.setTextColor(color);
        subtractButton.setTextColor(color);
        addButton.setTextColor(color);
    }

    private BigDecimal calculateResult(BigDecimal a, BigDecimal b, OPERATOR operator){
        BigDecimal res = null;

        switch(operator){
            case DIVIDE:
                res = a.divide(b, 9, BigDecimal.ROUND_HALF_UP); break;
            case MULTIPLY:
                res = a.multiply(b); break;
            case SUBTRACT:
                res = a.subtract(b); break;
            case ADD:
                res = a.add(b); break;
        }

        (findViewById(R.id.inputScroll)).setBackground(getResources().getDrawable(R.drawable.input_ripple_green));
        (findViewById(R.id.inputScroll)).getBackground().setState(new int[] { android.R.attr.state_pressed, android.R.attr.state_enabled });
        (findViewById(R.id.inputScroll)).getBackground().setState(new int[] {  });

        return res;
    }



//    BUTTON FUNCTIONS----------------------------------------------

//    change mode button
    @OnClick(R.id.bMode) void changeModeButtonPress() {
        if (mode == MODE.RPN) {mode = MODE.INFIX;}
        else {mode = MODE.RPN;}

        clearAllVariables();
        updateDisplay();

        (findViewById(R.id.inputScroll)).setBackground(getResources().getDrawable(R.drawable.input_ripple_red));
        (findViewById(R.id.inputScroll)).getBackground().setState(new int[] { android.R.attr.state_pressed, android.R.attr.state_enabled });
        (findViewById(R.id.inputScroll)).getBackground().setState(new int[] {  });
    }

//    clear button
    @OnClick(R.id.bClear) void clearButtonPress() {
        clearAllVariables();
        updateDisplay();

        (findViewById(R.id.inputScroll)).setBackground(getResources().getDrawable(R.drawable.input_ripple));
        (findViewById(R.id.inputScroll)).getBackground().setState(new int[] { android.R.attr.state_pressed, android.R.attr.state_enabled });
        (findViewById(R.id.inputScroll)).getBackground().setState(new int[] {  });
    }

//    equal button
    @OnClick(R.id.bEqual) void equalButtonPress() {
        if (mode == MODE.INFIX && numberA != null && numberB != null && !currentInput.isEmpty() && currentOperator != OPERATOR.NONE) {
            oldA = numberA;
            result = calculateResult(numberA, numberB, currentOperator);
            numberA = result;
            currentInput = formatBigDecimal(result);
        }
        else if (mode == MODE.RPN){
            rpnNumberStack.push(BigDecimal.valueOf(Double.valueOf(currentInput)));
        }

        updateDisplay();
    }

//    operator buttons
    @OnClick(R.id.bDivide) void divideOperatorButtonPress(View view) {pressOperator(view, OPERATOR.DIVIDE);}
    @OnClick(R.id.bMulti) void multiOperatorButtonPress(View view) {pressOperator(view, OPERATOR.MULTIPLY);}
    @OnClick(R.id.bSubtract) void subtractOperatorButtonPress(View view) {pressOperator(view, OPERATOR.SUBTRACT);}
    @OnClick(R.id.bAdd) void addOperatorButtonPress(View view) {pressOperator(view, OPERATOR.ADD);}
    private void pressOperator(View view, OPERATOR operator){
        if (mode == MODE.INFIX){
            if (currentOperator == OPERATOR.NONE) {
                numberA = BigDecimal.valueOf(Double.valueOf(currentInput));
                oldA = numberA;
                numberB = null;
            }
            else {
                oldA = numberA;
                numberB = null;
                result = null;
            }
            currentOperator = operator;
        }

        if (mode == MODE.RPN && rpnNumberStack.size() >= 2){
            BigDecimal b = rpnNumberStack.pop();
            BigDecimal a = rpnNumberStack.pop();
            result = calculateResult(a, b, operator);
            rpnNumberStack.push(result);
            currentInput = formatBigDecimal(result);
        }

        if (mode == MODE.INFIX){
            int color;
            TypedArray themeArray = getTheme().obtainStyledAttributes(new int[] {android.R.attr.textColor});
            try {color = themeArray.getColor(0, 0);}
            finally {themeArray.recycle();}

            divideButton.setTextColor(color);
            multiplyButton.setTextColor(color);
            subtractButton.setTextColor(color);
            addButton.setTextColor(color);

            ((AppCompatButton) view).setTextColor(getResources().getColor(R.color.colorAccent));
        }

        updateDisplay();
    }

//    number buttons
    @OnClick(R.id.b0) void press0() {pressNumber(0);}
    @OnClick(R.id.b1) void press1() {pressNumber(1);}
    @OnClick(R.id.b2) void press2() {pressNumber(2);}
    @OnClick(R.id.b3) void press3() {pressNumber(3);}
    @OnClick(R.id.b4) void press4() {pressNumber(4);}
    @OnClick(R.id.b5) void press5() {pressNumber(5);}
    @OnClick(R.id.b6) void press6() {pressNumber(6);}
    @OnClick(R.id.b7) void press7() {pressNumber(7);}
    @OnClick(R.id.b8) void press8() {pressNumber(8);}
    @OnClick(R.id.b9) void press9() {pressNumber(9);}
    private void pressNumber(int number){
        if (result != null && numberB != null && numberA != null && currentOperator != OPERATOR.NONE){
            clearButtonPress();
        }

        if (Objects.equals(currentInput, "0")) {currentInput = "";}
        if (mode == MODE.INFIX && numberA != null && currentOperator != OPERATOR.NONE && numberB == null){currentInput = "";}
        if (mode == MODE.RPN && !rpnNumberStack.empty() && Objects.equals(rpnNumberStack.peek(), BigDecimal.valueOf(Double.valueOf(currentInput)))){currentInput = "";}

        currentInput += number;

        if (mode == MODE.INFIX && currentOperator == OPERATOR.NONE && numberB == null && result != null){numberA = BigDecimal.valueOf(Double.valueOf(currentInput)); oldA = numberA;}
        if (mode == MODE.INFIX && numberA != null && currentOperator != OPERATOR.NONE){numberB = BigDecimal.valueOf(Double.valueOf(currentInput));}

        updateDisplay();
    }

//    decimal button
    @OnClick(R.id.bDecimal) void decimalButtonPress() {
        if (result != null && numberB != null && numberA != null && currentOperator != OPERATOR.NONE){
            clearButtonPress();
        }

        if (mode == MODE.INFIX && numberA != null && currentOperator != OPERATOR.NONE && numberB == null){currentInput = "0";}
        if (mode == MODE.RPN && !rpnNumberStack.empty() && Objects.equals(rpnNumberStack.peek(), BigDecimal.valueOf(Double.valueOf(currentInput)))){currentInput = "0";}

        if (!currentInput.isEmpty() && !currentInput.contains(".")) {
            currentInput += ".";
        }

        if (mode == MODE.INFIX && currentOperator == OPERATOR.NONE && numberB == null && result != null){numberA = BigDecimal.valueOf(Double.valueOf(currentInput)); oldA = numberA;}
        if (mode == MODE.INFIX && numberA != null && currentOperator != OPERATOR.NONE){numberB = BigDecimal.valueOf(Double.valueOf(currentInput));}

        updateDisplay();
    }




//    MAIN OVERRIDES FUNCTION------------------------------------------------
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyTheme();
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        updateDisplay();
        checkSetCustomTheme();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }




//    THEME SYSTEM-----------------------------------------------------------
    private void setLightSystemBars(boolean light){
        if (light){
            View someView = findViewById(R.id.layout);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                someView.setSystemUiVisibility(someView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            if (Build.VERSION.SDK_INT >= 27) {
                someView.setSystemUiVisibility(someView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
        }
        else{
            View someView = findViewById(R.id.layout);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                someView.setSystemUiVisibility(someView.getSystemUiVisibility());
            }
            if (Build.VERSION.SDK_INT >= 27) {
                someView.setSystemUiVisibility(someView.getSystemUiVisibility());
            }
        }
    }

    private static int mixColors(int color1, int color2, float ratio) {
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private void checkSetCustomTheme(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int themeID = preferences.getInt("THEME", THEME.BLACK.ordinal());
        int resultColor = getResources().getColor(R.color.black);
        if (themeID == THEME.BLACK.ordinal()){
            setLightSystemBars(false);
            resultColor = mixColors(getResources().getColor(R.color.black), getResources().getColor(R.color.transLighter), 0.62F);
        }
        else if (themeID == THEME.WHITE.ordinal()){
            setLightSystemBars(true);
            resultColor = mixColors(getResources().getColor(R.color.white), getResources().getColor(R.color.transDarker), 0.91F);
        }
        else if (themeID == THEME.CUSTOM.ordinal()){
            int color = preferences.getInt("CUSTOM", R.color.black);
            findViewById(R.id.layout).setBackgroundColor(color);
            double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
            if(darkness<0.5){
                setLightSystemBars(true);
                resultColor = mixColors(color, getResources().getColor(R.color.transDarker), 0.91F);
            }
            else{
                setLightSystemBars(false);
                resultColor = mixColors(color, getResources().getColor(R.color.transLighter), 0.65F);
            }
        }
        setTaskDescription(resultColor);
    }

    private void setTaskDescription(int color){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String title = getString(R.string.app_name);
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);
            setTaskDescription(new ActivityManager.TaskDescription(title, icon, color));
        }
    }

    @OnClick(R.id.bTheme) void toggleTheme(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int themeID = preferences.getInt("THEME", THEME.BLACK.ordinal());

        if (themeID == THEME.BLACK.ordinal()){
            preferences.edit().putInt("THEME", THEME.WHITE.ordinal()).apply();
        }
        else if (themeID == THEME.WHITE.ordinal()){
            if (preferences.getInt("CUSTOM", THEME.BLACK.ordinal()) != R.color.black){
                preferences.edit().putInt("THEME", THEME.CUSTOM.ordinal()).apply();
            }
            else {preferences.edit().putInt("THEME", THEME.BLACK.ordinal()).apply();}
        }
        else if (themeID == THEME.CUSTOM.ordinal()){
            preferences.edit().putInt("THEME", THEME.BLACK.ordinal()).apply();
        }
        recreate();
    }

    @OnLongClick(R.id.bTheme) boolean openThemePicker(){
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int color = preferences.getInt("CUSTOM", Color.argb(255,127,127,127));

        final ColorPicker picker = new ColorPicker(MainActivity.this, Color.red(color), Color.green(color), Color.blue(color));
        picker.setCallback(new ColorPickerCallback() {
            @Override public void onColorChosen(int color) {
                preferences.edit().putInt("THEME", THEME.CUSTOM.ordinal()).apply();
                preferences.edit().putInt("CUSTOM", color).apply();
                picker.hide();
                recreate();
            }
        });
        picker.show();
        return true;
    }

    private void applyTheme(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int themeID = preferences.getInt("THEME", THEME.BLACK.ordinal());

        if (themeID == THEME.BLACK.ordinal()){super.setTheme(R.style.BlackAppTheme);}
        else if (themeID == THEME.WHITE.ordinal()){super.setTheme(R.style.WhiteAppTheme);}
        else if ( themeID == THEME.CUSTOM.ordinal()){
            int color = preferences.getInt("CUSTOM", R.color.black);
            double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;

            if(darkness<0.5){super.setTheme(R.style.WhiteAppTheme);}
            else{super.setTheme(R.style.BlackAppTheme);}
        }
    }


}
