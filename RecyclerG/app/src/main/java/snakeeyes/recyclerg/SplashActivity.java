package snakeeyes.recyclerg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static android.content.ContentValues.TAG;

/**
 * Created by Barinov Maxim on 06.05.2018.
 */

public class SplashActivity extends Activity {
    //время показа загрузочного экрана
    public int TIME_MILS = 5000;

    //стандартная процедура инициализации активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        getActionBar().hide();//скрываем тулбар

        //узнаем есть ли в кеше строка с ссылками(
        // если нет то либо он стерт пользователем либо запуск производится в первый раз)
        Intent intent = getIntent();
        boolean frst_st = intent.getBooleanExtra("bool", false);

        //если кеша нет
        if(frst_st) {
            //обьявляем текст для оповещения долгой загрузки
            TextView text = (TextView)findViewById(R.id.splash_txt_2);
            text.setText("first run may take a long time");
            //привлекаем внимание пользователя всплывающим сообщением
            Toast.makeText(SplashActivity.this, "This is your first run !!!", Toast.LENGTH_SHORT).show();
            //увеличиваем время показа загрузочного экрана до 15 секунд
            TIME_MILS = 15000;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                finish();
            }
        }, TIME_MILS);//через чтолько секунд экран закорется и перед нами предстанет галерея
    }
}