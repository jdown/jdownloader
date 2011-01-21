package jd.updater;

import org.appwork.controlling.State;

public class UpdaterState extends State {

    private final int percent;

    public UpdaterState(final int percent, final String string) {
        super(string);
        this.percent = percent;
    }

    public int getPercent() {
        return percent;
    }

}
