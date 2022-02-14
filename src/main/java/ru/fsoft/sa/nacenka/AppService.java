package ru.fsoft.sa.nacenka;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.evotor.devices.commons.printer.printable.IPrintable;
import ru.evotor.devices.commons.printer.printable.PrintableText;
import ru.evotor.framework.core.IntegrationService;
import ru.evotor.framework.core.action.event.receipt.changes.position.IPositionChange;
import ru.evotor.framework.core.action.event.receipt.changes.position.SetExtra;
import ru.evotor.framework.core.action.event.receipt.changes.receipt.print_extra.SetPrintExtra;
import ru.evotor.framework.core.action.event.receipt.discount.ReceiptDiscountEvent;
import ru.evotor.framework.core.action.event.receipt.discount.ReceiptDiscountEventProcessor;
import ru.evotor.framework.core.action.event.receipt.discount.ReceiptDiscountEventResult;
import ru.evotor.framework.core.action.event.receipt.print_extra.PrintExtraRequiredEvent;
import ru.evotor.framework.core.action.event.receipt.print_extra.PrintExtraRequiredEventProcessor;
import ru.evotor.framework.core.action.event.receipt.print_extra.PrintExtraRequiredEventResult;
import ru.evotor.framework.core.action.processor.ActionProcessor;
import ru.evotor.framework.receipt.print_extras.PrintExtraPlacePrintGroupSummary;
import ru.evotor.framework.receipt.print_extras.PrintExtraPlacePrintGroupTop;

import android.util.Log;
import android.widget.Toast;


public class AppService extends IntegrationService {
    @Nullable
    @Override
    protected Map<String, ActionProcessor> createProcessors() {
        Map<String, ActionProcessor> map = new HashMap<>();
        map.put(ReceiptDiscountEvent.NAME_SELL_RECEIPT, new ReceiptDiscountEventProcessor() {
            @Override
            public void call(@NonNull String action, @NonNull ReceiptDiscountEvent event, @NonNull Callback callback)
            {
                String str;
                try
                {
                    Intent intent = new Intent(getApplicationContext(), NacenkaActivity.class);//вызов окна
                    callback.startActivity(intent);
                }
                catch (RemoteException e /*| JSONException e*/)
                {
                        e.printStackTrace();
                        str=e.getMessage();
                        Log.e("EVOTOR EXCEPTION", str);
                }
            }
        });

           map.put(PrintExtraRequiredEvent.NAME_SELL_RECEIPT,//NAME_SELL_RECEIPT – имя события, которое указывает тип чека, где будут напечатаны данные.
                new PrintExtraRequiredEventProcessor()
                {
                    @Override
                    public void call(String s, PrintExtraRequiredEvent printExtraRequiredEvent, Callback callback)//печать чека
                    {
                        DecimalFormat df = new DecimalFormat("###.#");
                        List<SetPrintExtra> setPrintExtras = new ArrayList<SetPrintExtra>();
                        if(Globals.used_nacenka==true && Globals.nacenka>0.0)//есть сохраненная наценка
                        {
                            setPrintExtras.add(new SetPrintExtra
                            (
                                 new PrintExtraPlacePrintGroupSummary(null),//внизу после товаров
                                 new IPrintable[]                    //Массив данных, которые требуется распечатать.
                                                    {   new PrintableText("В том числе СЕРВИС: " + df.format(Globals.nacenka)+"%")
                                                    }));
                            }
                        Globals.used_nacenka=false;
                        try {
                                callback.onResult(new PrintExtraRequiredEventResult(setPrintExtras).toBundle());//в чек
                            } catch (RemoteException exc) {  exc.printStackTrace();    }
                        }
                });

        return map;
    }
}