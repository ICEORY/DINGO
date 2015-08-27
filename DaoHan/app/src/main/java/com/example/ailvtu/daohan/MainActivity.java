package com.example.ailvtu.daohan;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    static final int ROBOT_BOUND = 1;
    static final int ROBOT_FORWARD = 1;
    static final int ROBOT_LEFT = 2;
    static final int ROBOT_RIGHT = 3;
    static final int ROBOT_BACKWARD = 4;
    static final int ROBOT_STOP =5 ;

    private Button btn_forward, btn_left, btn_right, btn_backward, btn_stop;
    private Button btn_input;
    private RadioButton radio_auto,radio_control;
    private EditText edit_input;

    private TextView txt_data;
    private Messenger mSendMsg;
    private boolean mBound;
    private int mAngleSet;
    private int mAngleNow;

    private SensorEventListener mSensorListener;
    private SensorManager mSensorManager;
    private Sensor mGSensor,mMSensor;

    @Override
    protected void onResume(){

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mGSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(mSensorListener,mGSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener,mMSensor,SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize all button on the view
        btn_forward = (Button)findViewById(R.id.btn_forward);
        btn_stop = (Button)findViewById(R.id.btn_stop);
        btn_left = (Button)findViewById(R.id.btn_left);
        btn_right = (Button)findViewById(R.id.btn_right);
        btn_backward = (Button)findViewById(R.id.btn_backward);
        btn_input = (Button)findViewById(R.id.btn_input);;

        radio_auto = (RadioButton)findViewById(R.id.radio_auto);
        radio_control = (RadioButton)findViewById(R.id.radio_control);
        edit_input = (EditText)findViewById(R.id.edit_input);

        //initialize all txt on the view
        txt_data = (TextView)findViewById(R.id.txt_data);
        txt_data.setText("robot stop");


        //intialize compass sensor


        //padbot auto mode
        //get the target angle
        ///////////////////////////////////////////////////////////////////////////

        mSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] accelerometerValues = new float[3];
                float[] magneticFieldValues = new float[3];
                float[] values = new float[3];
                float[] MR = new float[3];
                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    accelerometerValues=event.values;
                }
                if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                    magneticFieldValues = event.values;
                }
                //调用getRotaionMatrix获得变换矩阵R[]
                SensorManager.getRotationMatrix(MR, null, accelerometerValues, magneticFieldValues);
                SensorManager.getOrientation(MR, values);
                //经过SensorManager.getOrientation(R, values);得到的values值为弧度
                //转换为角度
                //values[0]  ：azimuth 方向角，但用（磁场+加速度）得到的数据范围是（-180～180）,
                //0表示正北，90表示正东，180/-180表示正南，-90表示正西
                mAngleNow = (int)Math.toDegrees(values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        btn_input.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(radio_control.isChecked())return;
                String str = edit_input.getText().toString();
                if(str!=null && str.length()>0)
                {
                    mAngleSet = Integer.parseInt(str);
                    txt_data.setText("target angle is:"+mAngleSet);
                }
            }
        });


        ///////////////////////////////////////////////////////////////////////////
        //padbot :control mode
        //robot forward
        btn_forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // mRunState = true;
                if(radio_auto.isChecked())return;
                txt_data.setText("going forward");
                Robot_Command(ROBOT_FORWARD);

            }
        });

        //robot left
        btn_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(radio_auto.isChecked())return;
                txt_data.setText("going left");
                Robot_Command(ROBOT_LEFT);
            }
        });

        //robot right
        btn_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(radio_auto.isChecked())return;
                txt_data.setText("going right");
                Robot_Command(ROBOT_RIGHT);
            }
        });

        //robot backward
        btn_backward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(radio_auto.isChecked())return;
                txt_data.setText("going backward");
                Robot_Command(ROBOT_BACKWARD);
            }
        });

        //robot stop
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(radio_auto.isChecked())return;
                Robot_Command(ROBOT_STOP);
                txt_data.setText("robot stop");
            }
        });
    }
    ///////////////////////////////////////////////////////////////////////////


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mSendMsg = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mSendMsg = null;
            mBound = false;
        }
    };

    private void Robot_Command(int cmd) {

        if(!mBound)return;

        //create and send a message to the service,using "BOUND + cmd"
        Message msg = Message.obtain(null, ROBOT_BOUND, cmd, 0);
        try{
            mSendMsg.send(msg);
        }
        catch (RemoteException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service
        Intent intent = new Intent();
        intent.setAction("edu.scut.dingo");
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mAngleSet = 0;
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
