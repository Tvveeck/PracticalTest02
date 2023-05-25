package ro.pub.cs.systems.eim.practicaltest02;

import androidx.annotation.NonNull;

public class DataModel {

    private final String value;

    public String getValue() {
        return value;
    }

    public String getTime() {
        return time;
    }

    private final String time;


    public DataModel(String value, String time) {
       this.value = value;
       this.time = time;
    }


    @NonNull
    @Override
    public String toString() {
        return "Info{" + value + ' ' + time +'}';
    }
}
