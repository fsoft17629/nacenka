package ru.fsoft.sa.nacenka;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ru.evotor.devices.commons.printer.printable.IPrintable;
import ru.evotor.devices.commons.printer.printable.PrintableBarcode;
import ru.evotor.devices.commons.printer.printable.PrintableText;
import ru.evotor.framework.core.IntegrationActivity;
import ru.evotor.framework.core.action.event.receipt.changes.position.IPositionChange;
import ru.evotor.framework.core.action.event.receipt.changes.position.PositionEdit;
import ru.evotor.framework.core.action.event.receipt.changes.position.SetExtra;
import ru.evotor.framework.core.action.event.receipt.changes.receipt.print_extra.SetPrintExtra;
import ru.evotor.framework.core.action.event.receipt.discount.ReceiptDiscountEventResult;
import ru.evotor.framework.receipt.Position;
import ru.evotor.framework.receipt.Receipt;
import ru.evotor.framework.receipt.ReceiptApi;
import ru.evotor.framework.system.SystemStateApi;
import ru.evotor.framework.receipt.print_extras.PrintExtraPlacePrintGroupSummary;

import static java.lang.Math.round;
import static ru.evotor.framework.system.SystemStateApi.getLastSessionNumber;

public class NacenkaActivity extends IntegrationActivity {
    EditText percent;
    TextView tv;
    EditText edNacenka;
    double sum1 =0.0; //начальная сумма
    List<Position> positions;// = curReceipt.getPositions();   //private Timer timer = new Timer();
    private final long DELAY =3000; // in ms
    Handler m_handler;
    Runnable m_handlerTask ;

    //****************************************************
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
    //****************************************************
    public double calcSum (double from, double nacenka)
    {
        double sum=from*(1+(nacenka/100));
        return round( sum,2);
    }
    //****************************************************
    public void drawSum (double nacenka)
    {
        String txt1;
        if(nacenka>0.0) {
            double rounded = calcSum(sum1, nacenka);
            txt1 = Double.toString(sum1) + " + " + Double.toString(nacenka) + "% = " + Double.toString(rounded);
        }
        else
            txt1 = Double.toString(sum1);


        try {
            tv.setText(txt1.toString());
            }
        catch (Exception e) {
            }

    }
    //****************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //if(Globals.service_started==false)            startService(new Intent(getApplicationContext(), CheckService.class));
        Intent intent = getIntent();
        double nacenka=Globals.nacenka;

        //****************************************************
            findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    //getWindow().getDecorView().
                    //View view = view.getCurrentFocus();
                    if (view != null)
                    {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }

                    /*InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);*/
                    Globals.used_nacenka=false;
                    finish();
                }
            });
        //****************************************************
        edNacenka=findViewById(R.id.etPercent);


        DecimalFormat df = new DecimalFormat("###.#");
        Receipt curReceipt = ReceiptApi.getReceipt(getApplicationContext(), Receipt.Type.SELL);
        positions = curReceipt.getPositions();

        for (int i = 0; i < positions.size(); i++)
        {
            Position pos = positions.get(i);
            BigDecimal price = pos.getPrice();
            price=price.multiply(pos.getQuantity());
            sum1+=price.doubleValue();
         }

        tv = (TextView)findViewById(R.id.tvCalc);


        if(nacenka>0)
        {
            edNacenka.setText(df.format(nacenka));
            edNacenka.setSelection(edNacenka.getText().length());
        }
        drawSum(nacenka);

        new Handler().postDelayed(new Runnable()//иногда нет клавы - эмул клик
        {
                public void run()
                {
                    EditText yourEditText= (EditText) findViewById(R.id.etPercent);
                    yourEditText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 50, 0, 0));
                    yourEditText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 50, 0, 0));
                }
        }, 200);

            // InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
           // imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 1);


        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 1);


        m_handler = new Handler();
        m_handlerTask = new Runnable()
        {
            @Override
            public void run() {
                double value;
                try {
                    value= Double.parseDouble(edNacenka.getText().toString());
                    if(value>0.0)    drawSum(value);
                }
                catch(Exception e) {}
                m_handler.postDelayed(m_handlerTask, DELAY);
            }
        };
        m_handlerTask.run();

        //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.showSoftInput(edNacenka, InputMethodManager.SHOW_IMPLICIT);
        //****************************************************


        //****************************************************
        findViewById(R.id.buttonSvc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startService(new Intent(getApplicationContext(), CheckService.class));
            }
            });
        //****************************************************
       findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                BigDecimal sum2 =new BigDecimal(0.0); //конечная сумма
                List<IPositionChange> changes = new ArrayList<IPositionChange>();
                Receipt curReceipt = ReceiptApi.getReceipt(getApplicationContext(), Receipt.Type.SELL);

                List<Position> positions = curReceipt.getPositions();
                Position pos;

                double nacenka;
                try{ nacenka=Double.parseDouble(edNacenka.getText().toString());}
                catch (Exception e)
                {
                    nacenka=0.0;
                }

                if(nacenka>0.0) {
                    JSONObject object = new JSONObject();
                    try {
                        object.put("someSuperKey", "AWESOME EDIT");
                        object.put("someSuperKey1", "0");
                    } catch (JSONException e) {
                        Globals.Show_exception(NacenkaActivity.this, e.getMessage());
                        //e.printStackTrace();
                    }
                    SetExtra extra = new SetExtra(object);
                    List<SetPrintExtra> setPrintExtras = new ArrayList<>();

                    for (int i = 0; i < positions.size(); i++) {
                        pos = positions.get(i);
                        BigDecimal priceWithAntiDiscount = new BigDecimal(calcSum(pos.getPrice().doubleValue(), nacenka), MathContext.DECIMAL64);
                        ; // сумма с наценкой
                        sum2.add(priceWithAntiDiscount);
                        Position newPos = Position.Builder.copyFrom(pos).setPrice(priceWithAntiDiscount).setPriceWithDiscountPosition(priceWithAntiDiscount).build(); // создание новой позиции на основе имеющейся.
                        PositionEdit posEdit = new PositionEdit(newPos);

                        BigDecimal diff = new BigDecimal(0);//sum2); //разница
                        changes.add(posEdit); // добавления измененной(ых) позиций в список изменений чека
                    }
                    setIntegrationResult(new ReceiptDiscountEventResult(
                            new BigDecimal("0"),
                            extra,
                            changes
                    )); // применение изменений.
                    Globals.used_nacenka=true;
                    Globals.nacenka=nacenka;
                }
                else {
                    Globals.used_nacenka = false;
                    Globals.nacenka=0.0;
                }

                if (view != null)
                {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                finish();
            }
        });
        //****************************************************
    }
}


