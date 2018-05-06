package snakeeyes.recyclerg;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;


import com.yandex.disk.rest.RestClient;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {

    //обьявляем масив с ссылками
    static private ArrayList<String> image_ids = new ArrayList<>();

    protected Handler handler;
    MyAdapter adapter;
    //стандартная процедура инициализации активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Обьявляем переменную pref для хранения кэша
        SharedPreferences pref = MainActivity.this.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        //изменяем тулбар
        ActionBar Ab = getActionBar();
        Ab.setDisplayShowHomeEnabled(true);
        Ab.setLogo(R.drawable.bf_ico);
        Ab.setDisplayUseLogoEnabled(true);

        //показываем загрузочный экран предварительно проверив наличие кэша
        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
        intent.putExtra("bool", !pref.contains("SOME_KEY_1"));
        startActivity(intent);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
        //Обьявляем наш RecyclerView, и layoutmanager для него
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.imagegallery);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),2);
        recyclerView.setLayoutManager(layoutManager);

        //если нет интерент соединения выводит диалоговое окно с предупреждением
        if(no_conected()){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            builder.setTitle("Internet connection lost").setMessage("Some images may not be available")

                    .setIcon(R.drawable.w_er).setCancelable(false)
                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        //подготавливаем масив с ссылками на картинки
        prepareData();

        //создаем адаптер recyclerview и передаем туда первые 20 фото с помощью масива createList
        final ArrayList<CreateList> createLists = image_get();
        adapter = new MyAdapter(MainActivity.this, createLists, recyclerView);
        recyclerView.setAdapter(adapter);
        //Слушатель подгрузки для recyclerview
        adapter.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {

                    createLists.add(null);
                    adapter.notifyItemInserted(createLists.size() - 1);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            createLists.remove(createLists.size() - 1);
                            adapter.notifyItemRemoved(createLists.size());

                            //добавляем по 10 фото на загрузку
                            // или до тех пор пока список с ссылками не достигнет конца
                            int index = createLists.size();
                            int end = index + 10;
                            int fin = image_ids.size();
                            for (int i = index + 1; i < end && i < fin; i++){
                                CreateList more_img  = new CreateList();
                                more_img.setImage_ID(image_ids.get(i));
                                createLists.add(more_img);
                            }
                            adapter.notifyDataSetChanged();
                            //передаем в адаптер что все что надо уже загрузили
                            adapter.setLoaded();
                            //Когда уже все загружено выводим что это все
                            if (end >= image_ids.size()) {
                                Toast.makeText(MainActivity.this, "It's all!!!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, 2000);

            }
        });
            }
        }, 2000);//пускаем на 2 секунды позже чтобы не перебить загрузочный экран

    }


    //возвращает true если нет соединения с интернетом иначе false
    private boolean no_conected(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return false;
        } else {
            return true;
        }
    }
    //шапка и clientid для получения ссылок на картинки из api.unsplash.com
    String cap = "https://api.unsplash.com/collections/1911873/photos/?per_page=30&page=";
    String my_id = "&client_id=60aab906aa3eabf838a77d2f7e76be71e3406440578a4ed9d16d4bca5d1098f8";
    //регулярное выражение для поиска ссылок на regular картинки
    private String reg = "\"regular\":\"[^\"]*\"";

    //переменная для страницы
    int pg = 1;

    //строки для регулярных выражений
    String a = "";
    String b = "";
    //строки для формирования кэша
    String save_arr = "";
    String save_off_arr = "";

    private void prepareData(){

        //если нет соединения достаем фото из кэша иначе ищем в api
        if (no_conected()){
           offlane();
           return;
        }else {
            SharedPreferences pref = MainActivity.this.getSharedPreferences(TAG, Context.MODE_PRIVATE);
            //организовываем запрос к get api
            while (true) {
                Callable r = new Callable() {

                    @Override
                    public String call() throws Exception {
                        try {
                            DefaultHttpClient hc = new DefaultHttpClient();
                            ResponseHandler response = new BasicResponseHandler();
                            HttpGet http = new HttpGet(cap + pg + my_id);
                            return (String) hc.execute(http, response);

                        } catch (IOException e) {
                            Log.e(TAG, "error in getting response get request okhttp");
                            return null;
                        }
                    }
                };

                //достаем строку с помощью Executor из Calleble r
                ExecutorService executor = Executors.newFixedThreadPool(1);
                Future<String> future = executor.submit(r);

                String result = null;
                try {
                    result = future.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                //если result == null значит мы обратились к несуществующей странице
                //а значит что мы вытащили все что можно
                if ( result == null && image_ids == null ){
                    offlane();
                    return;
                }
                if ( result == null ) break;

                //с помощью регулярных выражений получаем ссылку
                Pattern p = Pattern.compile(reg);
                Matcher m = p.matcher(result);

                while (m.find()) {
                    a = result.substring(m.start() + 11, m.end() - 1);
                    //преобразовываем ссылки в корректные с помощью регулярных выражений
                    Pattern p_1 = Pattern.compile("\\\\u0026");
                    Matcher m_1 = p_1.matcher(a);
                    a = m_1.replaceAll("&");
                    //если мы увидели что это фото уже стоит на первой позиции у нас в кеше продолжаем подгружать уже из кэша
                    if (pref.contains("FIRST_ID")){
                        if( a.equals(pref.getString("FIRST_ID", null))){
                            offlane();
                            return;
                        }
                    }
                    // создаем строку для кэша из ссылок что получаем онлайн
                    if (!image_ids.contains(a)) {
                        image_ids.add(a);
                        save_arr = save_arr + a + " ";
                    }
                }

                //переходим на следующую страницу запросов к api
                pg++;
            }
            //Сохраняем в кэш строку составленную из ссылок которые мы получили онлайн
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("SOME_KEY_1", save_arr);
            //Сохраняем в кэшь ссылку на 1 картинку если таковая имеется
            if(image_ids != null) {
                editor.putString("FIRST_ID", image_ids.get(0));
            }
            editor.commit();
        }
    }

    private void offlane(){
        SharedPreferences pref = MainActivity.this.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        //если в кэше есть строка с ссылками добавляем их в массив с ссылками с помощью регулярных выражений
        if (pref.contains("SOME_KEY_1")) {
            save_off_arr = pref.getString("SOME_KEY_1", null);
            Pattern p_1 = Pattern.compile("h[^\\ ]*[\\ ]");
            Matcher m_1 = p_1.matcher(save_off_arr);
            while (m_1.find()) {
                b = save_off_arr.substring(m_1.start(), m_1.end() - 1);
                if(!image_ids.contains(b)){
                    image_ids.add(b);
                }
            }
            //Сохраняем в кэш строку составленную из ссылок которые мы получили онлайн + которые уже были оффлайн
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("SOME_KEY_1", save_arr + save_off_arr);
            //Сохраняем в кэшь ссылку на 1 картинку а она всегда будет в этом случае
            editor.putString("FIRST_ID", image_ids.get(0));
            editor.commit();
        }else {
            //иначе
            //значит в кэше нет ссылок
            // также нет и интернет соединения
            // просим пользователя присоеденится к интеренту
            // и перезапустить приложение с помощью диалогового окна
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            builder.setTitle("No cached data").setMessage( "Please connect to the Internet and restart the application\nor try again later")

                    .setIcon(R.drawable.w_er).setCancelable(false)
                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }


    private ArrayList<CreateList> image_get(){
        //подготавливаем первые 20 фото для показа
        Iterator<String> it = image_ids.iterator();
        int tts = 20;
        ArrayList<CreateList> theimage = new ArrayList<>();
        while (it.hasNext() && tts > 0 ){
            CreateList createList = new CreateList();
            createList.setImage_ID(it.next());
            theimage.add(createList);
            tts--;
        }

        return theimage;
    }

}