package com.dimitriskatsikas.dryingtime;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView link1 = (TextView) findViewById(R.id.textView8);
        String stringLink1 = getString(R.string.createdby)+" <a href='http://www.linkedin.com/in/dimitriskatsikas'>Dimitris Katsikas</a>";
        link1.setText(Html.fromHtml(stringLink1));
        link1.setMovementMethod(LinkMovementMethod.getInstance());
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id==android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
