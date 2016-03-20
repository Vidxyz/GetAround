package com.example.vidhyasagar;

import android.app.ActionBar;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.vidhyasagar.getaround.R;

public class Description extends Activity {

    TextView textView, descriptionTextView;
    ImageView pageImage;
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_description);

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            Log.i("ERROR", "NULL");
        } else {
            actionBar.setHomeButtonEnabled(true);
//            actionBar.setIcon(R.drawable.backbutton);
        }

        i = getIntent();

        pageImage = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);
        descriptionTextView = (TextView) findViewById(R.id.descriptionTextView);
        descriptionTextView.setMovementMethod(new ScrollingMovementMethod());

        textView.setText(i.getStringExtra("identifier"));
        descriptionTextView.setText(i.getStringExtra("description"));

        int resId = getResources().getIdentifier(i.getStringExtra("image"), "drawable", getPackageName());
        pageImage.setImageResource(resId);

    }

    public void learnMore(View view) {
        String url = i.getStringExtra("url");
        Intent internet = new Intent(Intent.ACTION_VIEW);
        internet.setData(Uri.parse(url));
        startActivity(internet);
    }

}
