package net.kazhik.gambarumeter.main.view;

import android.text.format.DateUtils;

import net.kazhik.gambarumeter.Util;

import java.text.DecimalFormat;

/**
 * Created by kazhik on 14/10/25.
 */
public class LocationNotificationView extends NotificationView {
    private float distance = -1.0f;
    private long lapTime = 0;
    private String distanceUnit;
    private String distanceUnitStr;

    public void clear() {
        super.clear();
        this.distance = -1.0f;
        this.lapTime = 0;
    }
    public LocationNotificationView setDistanceUnit(String distanceUnit) {
        this.distanceUnit = distanceUnit;
        this.distanceUnitStr =
                Util.distanceUnitDisplayStr(this.distanceUnit,
                        this.getContext().getResources());

        return this;
    }
    public void updateDistance(float distance) {
        this.distance = distance;
    }
    public void updateLap(long laptime) {
        this.lapTime = laptime;
        
    }
    public String makeShortText() {
        String str = "";
        if (this.distance > 0) {
            float distance = Util.convertMeter(this.distance, this.distanceUnit);

            str += " ";
            str += new DecimalFormat("#.##").format(distance);
            str += this.distanceUnitStr;
        }
        return str;
    }
    public String makeLongText(String str) {
        if (this.lapTime > 0) {
            if (!str.isEmpty()) {
                str += " ";
            }
            str += DateUtils.formatElapsedTime(this.lapTime / 1000);
            str += "/";
            str += this.distanceUnitStr;
        }
        return str;

    }

}
