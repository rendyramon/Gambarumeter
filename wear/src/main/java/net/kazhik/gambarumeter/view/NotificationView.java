package net.kazhik.gambarumeter.view;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.format.DateUtils;

import net.kazhik.gambarumeter.Gambarumeter;
import net.kazhik.gambarumeter.R;

/**
 * Created by kazhik on 14/10/25.
 */
public class NotificationView {
    private Context context;
    private NotificationCompat.Builder notificationBuilder;
    private int heartRate = 0;
    private int stepCount = 0;

    private static final int NOTIFICATION_ID = 3000;

    public void initialize(Context context) {

        this.context = context;

        Intent intent = new Intent(context, Gambarumeter.class);
        PendingIntent pendingIntent
                = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action openMain
                = new NotificationCompat.Action(R.drawable.empty, null, pendingIntent);

        Bitmap bmp = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.background);

        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender()
                .setHintHideIcon(true)
                .setContentAction(0)
                .setBackground(bmp)
                .addAction(openMain);

        this.notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .extend(extender)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true);

    }
    public void clear() {
        this.heartRate = 0;
        this.stepCount = 0;
    }
    public void updateHeartRate(int heartRate) {

        this.heartRate = heartRate;
    }
    public void updateStepCount(int stepCount) {
        this.stepCount = stepCount;
    }
    private String makeText() {
        return  + this.heartRate
                + this.context.getString(R.string.bpm)
                + "/"
                + this.stepCount
                + this.context.getString(R.string.steps);
    }
    public void show(long elapsed) {
        this.notificationBuilder.setContentTitle(DateUtils.formatElapsedTime(elapsed / 1000))
                .setContentText(this.makeText());

        NotificationManagerCompat.from(this.context)
                .notify(NOTIFICATION_ID, this.notificationBuilder.build());

    }
    public void dismiss() {
        NotificationManagerCompat.from(this.context).cancel(NOTIFICATION_ID);
    }

}
