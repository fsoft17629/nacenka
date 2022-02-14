package ru.fsoft.sa.nacenka;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;


import ru.evotor.framework.system.SystemStateApi;

public final class Globals
{
    private static String session_file_name;
    private static String exception_file_name;
    private static int max_diff;
    private static SimpleDateFormat simpleDateFormat;

    public static double nacenka=0.0;
    public static boolean used_nacenka=false;
    public static boolean service_started=false;

    private Globals()
    {
        session_file_name="last_session";
        exception_file_name="exception";
        max_diff=12;
        simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    }
    /****************************************************************************************/
    private boolean check_session(Context context)
    {
        JSONObject json;
        Long saved_session;
        Date saved_time;
        boolean time_out=false;
        Date now = Calendar.getInstance().getTime();
        String str;

        Long  this_session = SystemStateApi.getLastSessionNumber(context);
        if(this_session==null) return false;

        if(!SystemStateApi.isSessionOpened(context))
                return false;

        String read1=readFile(context, exception_file_name);
        //dropFile(context, exception_file_name);

        String read=readFile(context, session_file_name);
        if(read==null || read.isEmpty())//изначально нет файла
        {
            json = makeJson(context,this_session);
            writeFile(context, session_file_name, json.toString(),false);
            return false;
        }
        //что-то уже сохранено
        try //читаем файл
        {   json = new JSONObject(read);
            saved_session=json.getLong("session_id");//старая сессия
            saved_time= simpleDateFormat.parse(json.getString("session_time"));//последнее время
            //ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            //toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            //Toast.makeText(context, "Session dt from file:" + simpleDateFormat.format(saved_time)+".№:"+ Long.toString(saved_session)+" this:"+Long.toString(this_session), Toast.LENGTH_SHORT).show();

            int diff;
            if(saved_session == this_session)
            {
                diff=diffHours(saved_time,now);
                if(diff>=max_diff)//превысили 12 часов с начала сессии
                    return true;//предупреждение
            }

            if(saved_session < this_session)
            {
                json=makeJson(context,this_session);//обновляем файл новой сессией
                writeFile(context, session_file_name, json.toString(),false);
            }
        }
        catch(JSONException | ParseException e)
        {
            str=e.getMessage();
            Show_exception(context,str);
            //Toast.makeText(context, "Exc:" + str, Toast.LENGTH_SHORT).show();
            dropFile(context, session_file_name);
            return false;
        }
        catch(Exception e)
        {
            str=e.getMessage();
            Show_exception(context,str);
            //Toast.makeText(context, "Exc:" + str, Toast.LENGTH_SHORT).show();
            return false;
        }
        return false;
    }
    /****************************************************************************************/
    // Показ предупреждения, статический метод
    public static void Show_warn(Context context,String error)
    {
        show_any(context,error,context.getResources().getString(R.string.warn),R.drawable.ic_notifications_black_24dp);
    }
    /****************************************************************************************/
    // Показ ошибки, статический метод
    public static void Show_exception(Context context,String error)
    {
        Date now = Calendar.getInstance().getTime();
        Globals g=new Globals();
        g.writeFile(context, exception_file_name, error +". Time: "+simpleDateFormat.format(now)+"\r\n",true);

        show_any(context,error,context.getResources().getString(R.string.exception_title),R.drawable.ic_action_error);//
    }
    /****************************************************************************************/
    // Показ диалога, статический метод
    private static void show_any(Context context,String text,String caption,int icon)
    {

        Activity activity;
        try{ activity = (Activity) context; }
        catch(Exception  e) {  activity=null; }

        if(activity==null) {//из службы показываем форму
            Intent intent = new Intent(context, ExcActivity.class);//вызов окна
            intent.putExtra("text", text);
            intent.putExtra("caption", caption);
            intent.putExtra("icon", icon);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        else {//из формы - диалог
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(caption)
                    .setMessage(text)
                    .setIcon(icon)
                    .setCancelable(false)
                    .setNegativeButton(context.getResources().getString(R.string.understand),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
     /****************************************************************************************/
    // Проверяем прошло ли Н часов с начала сессии. Статический метод
    public static boolean Check_session(Context context)
    {
        Globals g=new Globals();
        if(g.check_session(context)) {
            Show_warn(context, context.getResources().getString(R.string.over_time));
            /*Intent intent = new Intent(context, WarnActivity1.class);//вызов окна
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);*/
            return true;
        }
        return false;
    }
    /****************************************************************************************/
    // Сколько часов
    private int diffHours(Date one,Date two){

        long diffInMillies = two.getTime() - one.getTime();
        long diff=TimeUnit.MILLISECONDS.toHours(diffInMillies);
        return (int)diff;
    }
    /******************************************************************************************/
    private static JSONObject makeJson(Context mcoContext,long session)
    {
        JSONObject json = new JSONObject();
        Date now = Calendar.getInstance().getTime();
        String timestamp = simpleDateFormat.format(now);

        try {
            json.put("session_id", session);
            json.put("session_time", timestamp);
        }catch(Exception e)        {

            Show_exception(mcoContext,e.getMessage());
            return null;
        }
        return json;
    }
    /******************************************************************************************/
    // Сохр информацию об открытии сессии
    private  void writeFile(Context mcoContext, String sFileName, String sBody, boolean exception){
        File file=createDir(mcoContext);
        if(file==null) return;

        try{
            File gpxfile = new File(file, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            if(exception)
                writer.append(sBody);
            else
                writer.write(sBody);
            writer.flush();
            writer.close();

        }catch (Exception e)
        {
            Toast.makeText(mcoContext, e.getStackTrace()+"\r\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    /******************************************************************************************/
    // Все в папку
    private File createDir(Context mcoContext) {
        try {

            File file = new File(mcoContext.getFilesDir(), "session");
            if (!file.exists())
                file.mkdir();
            return file;
        }
        catch(Exception e)
            {
                Show_exception(mcoContext,e.getMessage());
            }
        return null;
    }
    /******************************************************************************************/
    // Удаляем файл
    private void  dropFile(Context mcoContext, String sFileName)
    {
        try {
            File dir = new File(mcoContext.getFilesDir(), "session");
            File file = new File(dir, sFileName);
            if (file.exists())
                file.delete();
        }
        catch(Exception e)
        {
            Show_exception(mcoContext,e.getMessage());
        }
    }

    /******************************************************************************************/
    // Читаем файл
    private String readFile(Context mcoContext, String sFileName)
    {
        File file = createDir(mcoContext);
        if(file==null) return "";

        StringBuilder sb = new StringBuilder();

        try {
            File gpxfile = new File(file, sFileName);
            FileInputStream fis = new FileInputStream(gpxfile);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;


            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e)
        {
            Show_exception(mcoContext,e.getMessage());
        }
        return sb.toString();
    }
}

