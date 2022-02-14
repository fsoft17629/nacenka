package ru.fsoft.sa.nacenka;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ExcActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exc);
        Intent intent = getIntent();
        String text=intent.getStringExtra("text");
        String caption=intent.getStringExtra("caption");
        int icon=intent.getIntExtra("icon",R.drawable.ic_notifications_black_24dp);

        AlertDialog.Builder builder = new AlertDialog.Builder(ExcActivity.this);

        builder.setTitle(caption)
                .setMessage(text)
                .setIcon(icon)
                .setCancelable(false)
                .setNegativeButton("Понятно",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                finish();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();


    }
}
