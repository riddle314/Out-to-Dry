package com.dimitriskatsikas.dryingtime;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class Outdoor extends AppCompatActivity {

    double [] T= new double[41];
    double [] h=new double[41];
    double [] u= new double[41];
    double [] P= new double[41];
    long t;
    double m;
    String coord;
    int time;
    boolean outdoorChecked= MainActivity.outdoorButtonChecked;
    boolean datacheck=MainActivity.datacheck;
    Handler handler;


    public Outdoor() {
        handler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outdoor);
        TextView timer=(TextView) findViewById(R.id.circleButton);
        if (datacheck){
            T=MainActivity.T;
            h=MainActivity.h;
            u=MainActivity.u;
            P=MainActivity.P;
            t=MainActivity.t;
            runCalculations();
            }
        else{
                time=0;
                timer.setText(Html.fromHtml("<small>"+getString(R.string.weather_not_found) + "</small>"));
            }
        coord=MainActivity.coord;
        updateWeatherData(coord);


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

    private void updateWeatherData(final String coord){
        new Thread(){
            public void run(){
                final JSONObject json = RemoteFetch.getJSON(Outdoor.this, coord);
                if(json == null){
                    handler.post(new Runnable(){
                        public void run(){
                            datacheck=false;
                        }
                    });
                } else {
                    datacheck=true;
                    handler.post(new Runnable(){
                        public void run(){
                            renderWeather(json);
                        }
                    });
                }
            }
        }.start();
    }

    private void renderWeather(JSONObject json){
        try {
            //Get the instance of JSONArray that contains JSONObjects
            JSONArray jsonArray = json.getJSONArray("list");

            //Iterate the jsonArray and print the info of JSONObjects
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                JSONObject main = jsonObject.getJSONObject("main");
                JSONObject wind = jsonObject.getJSONObject("wind");
                if(i>40){
                    break;
                }
                h[i] = main.getDouble("humidity");
                P[i] = main.getDouble("pressure");
                T[i] = main.getDouble("temp");
                u[i] = wind.getDouble("speed");
                h[i] = h[i] / 100; // gives percentage
                P[i] = 0.7501 * P[i]; // gives pressure from hPa to mmHg
                u[i] = 3.6 * u[i]; // gives speed at Km/h
                if(i==0){
                    t=jsonObject.getLong("dt");
                }
            }


        }catch(Exception e){
            Log.e("WeatherData", "One or more fields not found in the JSON data");
            datacheck = false;
        }
    }

    public void Calculate(View v){

        TextView timer=(TextView) findViewById(R.id.circleButton);
        if (datacheck){
            runCalculations();
        }
        else{
            time=0;
            timer.setText(Html.fromHtml("<small>" + getString(R.string.weather_not_found) + "</small>"));
        }
        updateWeatherData(coord);
    }


    public double TimeCalculator(double T1,double h1,double u1,double P1){
        double part1, part2, part3,finaltime;
        double ea=Math.exp(21.07-(5336/T1));
        part1=1129.515-(0.564372*P1);
        part2=0.44+(0.0733*u1);
        part3=(1-h1)*ea;
        m=0.22;
        double E=part1*part2*part3;
        finaltime= m*1440000/E;
        return finaltime;
    }

    public void AlarmClick(View v){
        if(time!=0){
        Calendar c = Calendar.getInstance();
        int minutes = c.get(Calendar.MINUTE);
        int hours = c.get(Calendar.HOUR_OF_DAY);
        int days = c.get(Calendar.DAY_OF_WEEK);
        minutes=minutes+time+1;
        hours=hours+minutes/60;
        minutes=minutes%60;
        if (Build.VERSION.SDK_INT >= 19) {
        days=days +hours/24;
        days=days%7;
        hours=hours%24;
        ArrayList weekday = new ArrayList();
        switch (days) {
            case 1:
                weekday.add("SUNDAY");
                break;
            case 2:
                weekday.add("MONDAY");
                break;
            case 3:
                weekday.add("TUESDAY");
                break;
            case 4:
                weekday.add("WEDNESDAY");
                break;
            case 5:
                weekday.add("THURSDAY");
                break;
            case 6:
                weekday.add("FRIDAY");
                break;
            case 7:
                weekday.add("SATURDAY");
                break;
        }
         Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
         i.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
         i.putExtra(AlarmClock.EXTRA_HOUR, hours);
         i.putExtra(AlarmClock.EXTRA_DAYS, weekday);
         i.putExtra(AlarmClock.EXTRA_MESSAGE, getString(R.string.alarm_message));
         startActivity(i);}
        else{
        Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
            i.putExtra(AlarmClock.EXTRA_HOUR, hours);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, getString(R.string.alarm_message));
           startActivity(i);}
        }
    }

    public int checkTime(){
        int k=0;
        Calendar c = Calendar.getInstance();
//        int utcOffset = c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET);
//        long deviceUtcMilliseconds = c.getTimeInMillis() + utcOffset;
        long deviceUtcMilliseconds = c.getTimeInMillis();
        long deviceHour=deviceUtcMilliseconds/3600000;
        long deviceMinutes=deviceUtcMilliseconds/60000;
        deviceMinutes=deviceMinutes+180;
        long serverHour=t/3600;
        long serverMinutes=t/60;
        if(deviceHour==serverHour+2){
            k=1;
        }
        else if(deviceHour==serverHour+1){
            long minuteDifference=deviceMinutes-serverMinutes;
            if(minuteDifference>=90){
                k=1;
            }
        }

        return k;
    }

    public void runCalculations(){
        TextView timer=(TextView) findViewById(R.id.circleButton);
        double finaltime=0;
        double timeparts;
        int hours, minutes;
        String result;
        if (outdoorChecked){
            for(int i=0; i<=40; i++){
                if(h[i]>=1){
                    h[i]=0.99;
                }};
            timeparts=TimeCalculator(T[0],h[0],u[0],P[0]);
            double an=1;
            int i=checkTime();
            while(timeparts>180){
                if(i>28){
                    break;
                }
                an=((timeparts-180)/timeparts)*an;
                timeparts=an*TimeCalculator(T[i],h[i],u[i],P[i]);
                finaltime=finaltime+180;
                i=i+1;}
            finaltime=finaltime+timeparts;
        }
        else{
            double Tinside=MainActivity.Temperature;
            double hinside=0.99;
            timeparts=TimeCalculator(Tinside,hinside,0,P[0]);
            double an=1;
            int i=checkTime();
            while(timeparts>180){
                if(i>28){
                    break;
                }
                hinside=hinside-0.01;
                if(Tinside-T[i]>=4){
                    if(Tinside-T[i]>=8){
                        Tinside=Tinside-2;
                    }
                    else{
                        Tinside=Tinside-1;
                    }
                }
                else if(T[i]-Tinside>=4){
                    if(T[i]-Tinside>=8){
                        Tinside=Tinside+2;
                    }
                    else{
                        Tinside=Tinside+1;}
                }
                an=((timeparts-180)/timeparts)*an;
                timeparts=an*TimeCalculator(Tinside,hinside,0,P[i]);
                finaltime=finaltime+180;
                i=i+1;}
            finaltime=finaltime+timeparts;
        }
        time = (int) Math.round(finaltime+10.5);
        if (time>=60){
            hours=time/60;
            minutes=time % 60;
            if(hours>1){
                result=hours+" "+getString(R.string.hours);
            }
            else{
            result=hours+" "+getString(R.string.hour);}
            if(minutes!=0){
                if (minutes>1){
                result=result+"<br>"+minutes+" "+getString(R.string.minutes);}
                else{
                    result=result+"<br>"+minutes+" "+getString(R.string.minute);
                }
            }
        }
        else{
            minutes=time;
            if(minutes<=0){
                minutes=0;
                result= minutes +" "+getString(R.string.minutes);}
            else{
                if (minutes>1){
                    result=minutes+" "+getString(R.string.minutes);}
                else{
                    result=minutes+" "+getString(R.string.minute);
                }
            }

        }
        timer.setText(Html.fromHtml("<big>" + result +"</big> <br> <small>" + getString(R.string.refresh) + "</small>"));
    }


}
