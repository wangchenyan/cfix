package me.wcy.cfix.simple;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showText();

        ViewPager pager = new ViewPager(this);
        Pager pager1 = new Pager(pager, 0);
    }

    public void showText() {
        TextView tv = (TextView) findViewById(R.id.tv);
        tv.setText(Hello.newHello().hello());
    }
}
