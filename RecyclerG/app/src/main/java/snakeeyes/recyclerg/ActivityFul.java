package snakeeyes.recyclerg;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import com.squareup.picasso.Picasso;

import java.io.IOException;

/**
 * Created by Barinov Maxim on 17.04.2018.
 */
//Экран с полным изображением
public class ActivityFul extends Activity {

    ImageView mImageView;
    String path;

    //стандартная процедура инициализации активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ful_img);

        //получаем переданную нам ссылку на картинку
        Intent intent = getIntent();
        path = intent.getStringExtra("img");
        //обьявляем в mImageView нужный нам imageview и втавляем туда картинку
        mImageView = (ImageView)findViewById(R.id.image);
        Picasso.with(ActivityFul.this).load(path)
                .into(mImageView);


    }
    //добавляем тулбар из заготовленного xml
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.abar, menu);
        return super.onCreateOptionsMenu(menu);
    }
    //назначаем обьекты в тулбаре для выхода из активности
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);

    }

}
