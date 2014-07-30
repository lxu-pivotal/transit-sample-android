package io.pivotal.android.ttc;

import java.util.ArrayList;

public class Notification {

    public static final class List extends ArrayList<Notification> {
        private static final long serialVersionUID = 1L;
    }

    public String route;

    public String stop;

    public String time;

    public boolean enabled;


    @Override
    public String toString() {
        return String.format("Route: %s, Stop: %s, Time: %s, Enabled %b", route, stop, time, enabled);
    }
}
