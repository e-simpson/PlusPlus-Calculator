package clas.hci.calculator;

import android.app.ActivityManager;
import android.arch.persistence.room.Room;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class MainActivity extends AppCompatActivity {
//    MAIN VIEW BINDING----------------------------------------------
    @BindView(R.id.bMode) AppCompatButton modeButton;
    @BindView(R.id.historyScroll) ScrollView historyScroll;
    @BindView(R.id.historyLayout) LinearLayoutCompat historyLayout;
    @BindView(R.id.expression) TextView expressionDisplay;
    @BindView(R.id.inputScroll) HorizontalScrollView inputScroll;
    @BindView(R.id.input) TextView inputDisplay;
    @BindView(R.id.bEqual) AppCompatButton equalButton;
    @BindView(R.id.bDivide) AppCompatButton divideButton;
    @BindView(R.id.bMulti) AppCompatButton multiplyButton;
    @BindView(R.id.bSubtract) AppCompatButton subtractButton;
    @BindView(R.id.bAdd) AppCompatButton addButton;
    @BindView(R.id.bClear) AppCompatButton clearButton;
    @BindView(R.id.bTheme) ImageButton themeButton;



    //    CUSTOM ENUMS----------------------------------------------
    private enum THEME {BLACK, WHITE, CUSTOM}
    private enum OPERATOR {NONE, DIVIDE, MULTIPLY, SUBTRACT, ADD}
    private enum RIPPLE {RED, ACCENT, GREEN}
    private enum MODE {RPN, INFIX}



//    MAIN VARIABLES----------------------------------------------
    private MODE mode = MODE.INFIX;
    private Stack<BigDecimal> rpnNumberStack = new Stack<>();

    private BigDecimal oldA = null;
    private BigDecimal result = null;
    private BigDecimal numberA = null;
    private BigDecimal numberB = null;
    private OPERATOR currentOperator = OPERATOR.NONE;
    private String currentInput = "0";

    private CalculationDatabase db;


//    DB HELPER FUNCTIONS
    private static class calculationsGetter extends AsyncTask<Void, Void, List> {
        private WeakReference<MainActivity> activityReference;
        calculationsGetter(MainActivity context) {activityReference = new WeakReference<>(context);}
        @Override protected List doInBackground(Void... params) {return activityReference.get().db.dao().getAll();}
        @Override protected void onPostExecute(List result) {activityReference.get().updateHistory(result);}
    }

    private static class calculationPoster extends AsyncTask<Void, Void, String> {
        private WeakReference<MainActivity> activityReference;
        String expression = "";
        double result = 0;

        long epoch = 0;

        calculationPoster(MainActivity context, final String expression, final double result, final long epoch) {
            this.expression = expression;
            this.result = result;
            this.epoch = epoch;
            activityReference = new WeakReference<>(context);
        }

        @Override protected String doInBackground(Void... params) {
            Calculation newCalculation = new Calculation(expression, result, epoch);
            activityReference.get().db.dao().insertCalculation(newCalculation);
            return "task finished";
        }
        @Override protected void onPostExecute(String result) {
            new calculationsGetter(activityReference.get()).execute();
//            activityReference.get().historyDisplay.append("\n" + expression + " = " + result);

        }
    }

    private void updateHistory(List calculations){
        for (int i = 0; i < calculations.size() ; i++) {
            Calculation c = (Calculation)calculations.get(i);
//            historyDisplay.append("\n" + c.getExpression());
            addCalculationToScroll(c);
        }
        historyScroll.post(new Runnable() {@Override public void run() { historyScroll.fullScroll(View.FOCUS_DOWN); }});
    }

    private void addCalculationToScroll(Calculation calculation){
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

//        TextView time = new TextView(this);
//        time.setTextColor(getResources().getColor(R.color.custom));
//        time.setTextSize(12);
//        Date date = new Date(calculation.getUtc());
//        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("EEE, MMM d" );
//        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
//        time.setText(formatter.format(date));
//        time.setGravity(Gravity.CENTER);
//        time.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
//        time.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//        chat.addView(time);

        final LinearLayout calculationBubble = new LinearLayout(this);
        calculationBubble.setBackground((getResources().getDrawable(R.drawable.calculation_bubble)));
        calculationBubble.setPadding(40,10,40,10);
        calculationBubble.setOrientation(LinearLayout.VERTICAL);
        calculationBubble.setGravity(Gravity.END);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {calculationBubble.setElevation(10);}
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(40, 20, 0, 20);
        calculationBubble.setLayoutParams(params);
        layout.addView(calculationBubble);


        TextView words = new TextView(this);
        words.setTextColor(getResources().getColor(R.color.colorAccentLighter));
        words.setTextSize(20);
        words.setText(calculation.getExpression());
        words.setGravity(Gravity.CENTER_VERTICAL);
        words.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        words.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        calculationBubble.addView(words);



//        calculationBubble.setClickable(true);
//        calculationBubble.setOnClickListener(new View.OnClickListener() {
//            @Override public void onClick(View view) {}
//        });

//        if (!startAnimationComplete){
//        TranslateAnimation translate = new TranslateAnimation(0, 0, 200, 0);
//        translate.setFillAfter(true);
//        translate.setDuration(800);
//        chat.startAnimation(translate);

        new Handler().post(new Runnable() {@Override public void run() {historyScroll.fullScroll(View.FOCUS_DOWN);}});
//        }

        historyLayout.addView(layout);
    }



    //    HELPER FUNCTIONS-----------------------------------------
    private String getOperationCharacter(){
        if (currentOperator == OPERATOR.DIVIDE) {return getResources().getString(R.string.divide);}
        else if (currentOperator == OPERATOR.MULTIPLY) {return getResources().getString(R.string.multiply);}
        else if (currentOperator == OPERATOR.SUBTRACT) {return getResources().getString(R.string.subtract);}
        else if (currentOperator == OPERATOR.ADD) {return getResources().getString(R.string.add);}
        return "";
    }

    private String formatBigDecimal(BigDecimal num){
//        String s = num.toString();
//        s = !s.contains(".") ? s : s.replaceAll("0*$", "").replaceAll("\\.$", "");
        return num.stripTrailingZeros().toString();
    }

    private boolean checkIfClearNeeded(){
        return result != null && numberB != null && numberA != null && currentOperator != OPERATOR.NONE;
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

        assert res != null;

        String expression = formatBigDecimal(a) + " " + getOperationCharacter()  + " "  + formatBigDecimal(b) + " = " + formatBigDecimal(res);
        new calculationPoster(this, expression, res.doubleValue(), System.currentTimeMillis()).execute();
        rippleInput(RIPPLE.GREEN);
        return res;
    }



    //    UI FUNCTIONS-----------------------------------------
    private void rippleInput(RIPPLE rippleType){
        switch(rippleType){
            case RED:
                inputScroll.setBackground(getResources().getDrawable(R.drawable.input_ripple_red));
                inputScroll.getBackground().setState(new int[] { android.R.attr.state_pressed, android.R.attr.state_enabled });
                inputScroll.getBackground().setState(new int[] {  });
                break;
            case GREEN:
                inputScroll.setBackground(getResources().getDrawable(R.drawable.input_ripple_green));
                inputScroll.getBackground().setState(new int[] { android.R.attr.state_pressed, android.R.attr.state_enabled });
                inputScroll.getBackground().setState(new int[] {  });
                break;
            case ACCENT:
                inputScroll.setBackground(getResources().getDrawable(R.drawable.input_ripple));
                inputScroll.getBackground().setState(new int[] { android.R.attr.state_pressed, android.R.attr.state_enabled });
                inputScroll.getBackground().setState(new int[] {  });
                break;
        }

    }

    private void setInputColour(RIPPLE rippleType){
        switch(rippleType){
            case RED:
                inputDisplay.setTextColor(getResources().getColor(R.color.red));
                expressionDisplay.setTextColor(getResources().getColor(R.color.red));
                break;
            case ACCENT:
                inputDisplay.setTextColor(getResources().getColor(R.color.colorAccent));
                expressionDisplay.setTextColor(getResources().getColor(R.color.colorAccentLight));
                break;
        }

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
        if (mode == MODE.INFIX) {expressionDisplay.setText((expression));}

        if (mode == MODE.RPN){
            expressionDisplay.setText("");
            for (int i = 0; i < rpnNumberStack.size(); i++) {
                if (i > 0) {
                    expressionDisplay.append(",  ");}
                expressionDisplay.append(formatBigDecimal(rpnNumberStack.get(i)));
            }
        }

        if (checkIfClearNeeded()) {clearButton.setText(R.string.clear);} else {clearButton.setText(R.string.delete);}
        inputDisplay.setText(currentInput);
        setInputColour(RIPPLE.ACCENT);
        inputScroll.post(new Runnable() {
            @Override public void run() { inputScroll.fullScroll(View.FOCUS_RIGHT); }
        });
    }



//    BUTTON FUNCTIONS----------------------------------------------

//    change mode button
    /*
    @OnClick(R.id.bMode) void changeModeButtonPress() {
        if (mode == MODE.RPN) {mode = MODE.INFIX;}
        else {mode = MODE.RPN;}

        clearAllVariables();
        updateDisplay();

        rippleInput(RIPPLE.red);
    }
    */

//    clear button
    @OnClick(R.id.bClear) void clearButtonPress() {
        if (checkIfClearNeeded()){
            clearButtonLongPress();
        }
        else{
            currentInput = currentInput.substring(0, currentInput.length()-1);
            if (Objects.equals(currentInput, "")){currentInput = "0";}
            updateDisplay();
        }
    }
    @OnLongClick(R.id.bClear) boolean clearButtonLongPress() {
        clearAllVariables();
        updateDisplay();
        rippleInput(RIPPLE.ACCENT);
        return true;
    }

//    equal button
    @OnClick(R.id.bEqual) void equalButtonPress() {
        if (mode == MODE.INFIX && numberA != null && numberB != null && !currentInput.isEmpty() && currentOperator != OPERATOR.NONE) {
            if (numberB.compareTo(BigDecimal.ZERO) == 0 && currentOperator == OPERATOR.DIVIDE){
                clearButton.setText(R.string.clear);
                expressionDisplay.setText(R.string.division_by_zero);
                inputDisplay.setText(R.string.lenny);
                result = BigDecimal.ZERO;
                rippleInput(RIPPLE.RED);
                setInputColour(RIPPLE.RED);
                return;
            }
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
        if (checkIfClearNeeded()){
            clearButtonLongPress();
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
        if (checkIfClearNeeded()){
            clearButtonLongPress();
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

        db = Room.databaseBuilder(getApplicationContext(), CalculationDatabase.class, "calculations.db").fallbackToDestructiveMigration().build();
        new calculationsGetter(this).execute();

        updateDisplay();
        checkSetCustomTheme();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }



//    THEME SYSTEM-----------------------------------------------------------
    private void setLightSystemBars(boolean status, boolean navigation){
        View view = findViewById(R.id.layout);
        if (status && !navigation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        else if (!status && navigation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        else if (status && navigation){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        else {
            view.setSystemUiVisibility(view.getSystemUiVisibility());
        }
    }

    private static int mixColors(int color1, int color2, float ratio) {
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private void setTaskDescription(int color){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String title = getString(R.string.app_name);
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);
            setTaskDescription(new ActivityManager.TaskDescription(title, icon, color));
        }
    }

    private void checkSetCustomTheme(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int themeID = preferences.getInt("THEME", THEME.BLACK.ordinal());
        int inputColour = getResources().getColor(R.color.black);
        int expressionColour = getResources().getColor(R.color.black);
        if (themeID == THEME.BLACK.ordinal()){
            setLightSystemBars(false, false);
            inputColour = mixColors(getResources().getColor(R.color.black), getResources().getColor(R.color.white), 0.8F);
            expressionColour = mixColors(getResources().getColor(R.color.black), getResources().getColor(R.color.white), 0.9F);
        }
        else if (themeID == THEME.WHITE.ordinal()){
            setLightSystemBars(true, true);
            inputColour = mixColors(getResources().getColor(R.color.white), getResources().getColor(R.color.black), 0.97F);
            expressionColour = mixColors(getResources().getColor(R.color.white), getResources().getColor(R.color.black), 0.98F);
        }
        else if (themeID == THEME.CUSTOM.ordinal()){
            int color = preferences.getInt("CUSTOM", R.color.custom);
            findViewById(R.id.layout).setBackgroundColor(color);
            double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
            if(darkness<0.5){
                setLightSystemBars(false, true);
                inputColour = mixColors(color, getResources().getColor(R.color.black), 0.2F);
                expressionColour = mixColors(color, getResources().getColor(R.color.black), 0.9F);
            }
            else{
                setLightSystemBars(true, false);
                inputColour = mixColors(color, getResources().getColor(R.color.white), 0.2F);
                expressionColour = mixColors(color, getResources().getColor(R.color.white), 0.95F);
            }
            Drawable myIcon = getResources().getDrawable( R.drawable.palette );
            ColorFilter filter = new LightingColorFilter( expressionColour, expressionColour);
            myIcon.setColorFilter(filter);
            themeButton.setImageDrawable(myIcon);
        }
        setTaskDescription(inputColour);
        findViewById(R.id.inputLayout).setBackgroundColor(inputColour);
        findViewById(R.id.expressionLayout).setBackgroundColor(expressionColour);
    }

    @OnClick(R.id.bTheme) void toggleTheme(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int themeID = preferences.getInt("THEME", THEME.BLACK.ordinal());

        if (themeID == THEME.BLACK.ordinal()){
            preferences.edit().putInt("THEME", THEME.WHITE.ordinal()).apply();
        }
        else if (themeID == THEME.WHITE.ordinal()){
//            if (preferences.getInt("CUSTOM", THEME.BLACK.ordinal()) != R.color.black){
                preferences.edit().putInt("THEME", THEME.CUSTOM.ordinal()).apply();
//            }
//            else {preferences.edit().putInt("THEME", THEME.BLACK.ordinal()).apply();}
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
