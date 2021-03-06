package net.kazhik.gambarumeter.main;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

import net.kazhik.gambarumeter.R;
import net.kazhik.gambarumeter.main.monitor.BatteryLevelReceiver;
import net.kazhik.gambarumeter.main.monitor.Gyroscope;
import net.kazhik.gambarumeter.main.monitor.SensorValueListener;
import net.kazhik.gambarumeter.main.monitor.StepCountMonitor;
import net.kazhik.gambarumeter.main.monitor.Stopwatch;
import net.kazhik.gambarumeter.main.view.SplitTimeView;
import net.kazhik.gambarumeter.main.view.StepCountView;
import net.kazhik.gambarumeter.pager.PagerFragment;
import net.kazhik.gambarumeterlib.storage.WorkoutTable;

import java.util.List;

/**
 * Created by kazhik on 14/11/11.
 */
public abstract class MainFragment extends PagerFragment
        implements Stopwatch.OnTickListener,
        SensorValueListener,
        ServiceConnection,
        UserInputManager.UserInputListener {
    private SensorManager sensorManager;

    protected Stopwatch stopwatch;
    protected StepCountMonitor stepCountMonitor;
    private BatteryLevelReceiver batteryLevelReceiver;
    private Gyroscope gyroscope;
    private boolean isBound = false;

    private SplitTimeView splitTimeView = new SplitTimeView();
    private StepCountView stepCountView = new StepCountView();

    private UserInputManager userInputManager;
    private Vibrator vibrator;
    private MobileConnector mobileConnector = new MobileConnector();

    private static final String TAG = "MainFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: savedInstanceState = " + savedInstanceState);

        this.initializeSensor();

        Activity activity = this.getActivity();

        this.mobileConnector.initialize(activity);

        this.vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main, container, false);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.initializeUI();

        this.voiceAction(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putLong("start_time", this.stopwatch.getStartTime());

        super.onSaveInstanceState(outState);
//        Log.d(TAG, "onSaveInstanceState: " + outState.getLong("start_time"));
    }

    private void voiceAction(Bundle savedInstanceState) {
        String actionStatus =
                this.getActivity().getIntent().getStringExtra("actionStatus");
        if (actionStatus == null) {
            return;
        }

        if (actionStatus.equals("ActiveActionStatus")) {
            this.startWorkout();
        } else if (actionStatus.equals("CompletedActionStatus")) {
            if (savedInstanceState == null) {
                Log.d(TAG, "savedInstanceState is null");
                return;
            }
            if (savedInstanceState.getLong("start_time") == 0) {
                Log.d(TAG, "Not started:");
                return;
            }
            Log.d(TAG, "workout stop");
            this.stopWorkout();
        }

    }
    public boolean isBound() {
        return this.isBound;
    }
    public void setBound() {
        this.isBound = true;
    }

    @Override
    public void onDestroy() {
        long stopTime = this.stopWorkout();
        this.mobileConnector.terminate();
        if (this.gyroscope != null) {
            this.gyroscope.terminate();
        }
        if (this.stepCountMonitor != null) {
            this.stepCountMonitor.terminate();
        }
        Activity activity = this.getActivity();
        if (this.isBound) {
            activity.unbindService(this);

        }
        activity.unregisterReceiver(this.batteryLevelReceiver);

        super.onDestroy();
    }

    protected void initializeSensor() {
        Activity activity = this.getActivity();

        this.batteryLevelReceiver = new BatteryLevelReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_LOW");
        intentFilter.addAction("android.intent.action.BATTERY_OKAY");
        activity.registerReceiver(this.batteryLevelReceiver, intentFilter);

        this.sensorManager =
                (SensorManager)activity.getSystemService(Activity.SENSOR_SERVICE);

        List<Sensor> sensorList = this.sensorManager.getSensorList(Sensor.TYPE_ALL);
        Intent intent;
        boolean bound;
        for (Sensor sensor: sensorList) {
            intent = null;
            Log.i(TAG, "Sensor:" + sensor.getName() + "; " + sensor.getType());
            switch (sensor.getType()) {
                case Sensor.TYPE_STEP_COUNTER:
                    intent = new Intent(activity, StepCountMonitor.class);
                    this.stepCountMonitor = new StepCountMonitor();
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    intent = new Intent(activity, Gyroscope.class);
                    this.gyroscope = new Gyroscope(); // temporary

                    break;
                default:
                    break;
            }
            if (intent != null) {
                bound = this.getActivity().bindService(intent,
                        this, Context.BIND_AUTO_CREATE);
                if (bound) {
                    this.isBound = true;
                }
            }
        }

        this.stopwatch = new Stopwatch(1000L, this);

    }
    protected void initializeUI() {
        Activity activity = this.getActivity();

        this.splitTimeView.initialize((TextView) activity.findViewById(R.id.split_time));
        this.stepCountView.initialize((TextView) activity.findViewById(R.id.stepcount_value));

        this.userInputManager = new UserInputManager(this)
                .initTouch(activity,
                        (FrameLayout)activity.findViewById(R.id.main_layout))
                .initButtons(
                        (ImageButton)activity.findViewById(R.id.start),
                        (ImageButton)activity.findViewById(R.id.stop)
                );

    }

    protected void startWorkout() {

        if (this.stepCountMonitor != null) {
            this.stepCountView.setStepCount(0)
                    .refresh();
            this.stepCountMonitor.start();
        }
        this.splitTimeView.setTime(0)
                .refresh();

        this.stopwatch.start();

    }
    protected long stopWorkout() {
        long stopTime = this.stopwatch.stop();
        if (this.stepCountMonitor != null) {
            this.stepCountMonitor.stop(stopTime);
        }

        return stopTime;
    }
    protected abstract void saveResult();

    public void saveResult(SQLiteDatabase db, long startTime) {
        if (this.stepCountMonitor != null) {
            this.stepCountMonitor.saveResult(db, startTime);
        }
    }

    protected DataMap putData(DataMap dataMap) {
        this.stepCountMonitor.putDataMap(dataMap);

        return dataMap;

    }

    // UserInputManager.UserInputListener
    @Override
    public void onUserStart() {
        this.startWorkout();
    }

    // UserInputManager.UserInputListener
    @Override
    public void onUserStop() {
        this.stopWorkout();
        this.saveResult();
        long startTime = this.stopwatch.getStartTime();
        this.mobileConnector.sync(startTime);
        this.stopwatch.reset();
    }
    // UserInputManager.UserInputListener
    @Override
    public void onUserDismiss() {
        DismissOverlayView dismissOverlay =
                (DismissOverlayView) getActivity().findViewById(R.id.dismiss_overlay);

        dismissOverlay.show();
    }
    // SensorValueListener
    @Override
    public void onRotation(long timestamp) {
        if (!this.stopwatch.isRunning()) {
            return;
        }
        this.userInputManager.toggleVisibility(false);
        this.vibrator.vibrate(1000);

        this.stopWorkout();
        this.saveResult();
        long startTime = this.stopwatch.getStartTime();
        this.mobileConnector.sync(startTime);
        this.stopwatch.reset();
    }
    // SensorValueListener
    @Override
    public void onStepCountChanged(long timestamp, int stepCount) {
        if (!this.stopwatch.isRunning()) {
            return;
        }
        this.stepCountView.setStepCount(stepCount);
        this.getActivity().runOnUiThread(this.stepCountView);

        this.updateStepCount(stepCount);
    }
    protected abstract void updateStepCount(int stepCount);

    // SensorValueListener
    @Override
    public void onBatteryLow() {
    }

    // SensorValueListener
    @Override
    public void onBatteryOkay() {
    }

    // Stopwatch.OnTickListener
    @Override
    public void onTick(long elapsed) {
        this.splitTimeView.setTime(elapsed);
        this.getActivity().runOnUiThread(this.splitTimeView);

        this.showNotification(elapsed);
    }
    protected abstract void showNotification(long elapsed);

    // ServiceConnection
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: " + componentName.toString());

        if (iBinder instanceof Gyroscope.GyroBinder) {
            this.gyroscope =
                    ((Gyroscope.GyroBinder) iBinder).getService();
            this.gyroscope.initialize(this.sensorManager, this);
        } else if (iBinder instanceof StepCountMonitor.StepCountBinder) {
            this.stepCountMonitor =
                    ((StepCountMonitor.StepCountBinder) iBinder).getService();
            this.stepCountMonitor.init(this.getActivity(), this.sensorManager, this);
        }

    }
    // ServiceConnection
    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: " + componentName.toString());

    }

    private void initialize() {
        WorkoutTable workoutTable = new WorkoutTable(this.getActivity());
        workoutTable.openWritable();
        workoutTable.initializeSynced();
        workoutTable.close();

    }

    @Override
    public void onStart() {
        super.onStart();
        this.mobileConnector.connect();
    }

    @Override
    public void onStop() {
        this.mobileConnector.disconnect();
        super.onStop();
    }

}
