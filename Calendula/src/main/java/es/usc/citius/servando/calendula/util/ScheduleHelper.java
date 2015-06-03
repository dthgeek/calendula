package es.usc.citius.servando.calendula.util;

import android.util.Log;
import com.google.ical.values.Frequency;
import es.usc.citius.servando.calendula.fragments.ScheduleTypeFragment;
import es.usc.citius.servando.calendula.persistence.Medicine;
import es.usc.citius.servando.calendula.persistence.Schedule;
import es.usc.citius.servando.calendula.persistence.ScheduleItem;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

/**
 * Created by joseangel.pineiro on 12/12/13.
 */
public class ScheduleHelper {

    private static ScheduleHelper instance;

    private List<ScheduleItem> scheduleItems;
    private Schedule schedule;
    private Medicine selectedMed;

    private int selectedScheduleIdx = 0;
    private int timesPerDay = 1;
    private int scheduleType = -1;

    private ScheduleHelper() {
        setScheduleItems(new ArrayList<ScheduleItem>());
    }

    public static ScheduleHelper instance() {
        if (instance == null)
            instance = new ScheduleHelper();
        return instance;
    }

    public Medicine getSelectedMed() {
        return selectedMed;
    }

    public void setSelectedMed(Medicine selectedMed) {
        this.selectedMed = selectedMed;
    }

    public List<ScheduleItem> getScheduleItems() {
        return scheduleItems;
    }

    public void setScheduleItems(List<ScheduleItem> scheduleItems) {
        this.scheduleItems = scheduleItems;
    }

    public int getSelectedScheduleIdx() {
        return selectedScheduleIdx;
    }

    public void setSelectedScheduleIdx(int selectedScheduleIdx) {
        this.selectedScheduleIdx = selectedScheduleIdx;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        if (schedule != null && scheduleType == -1)
        {
            if (schedule.rule().frequency() == Frequency.HOURLY)
            {
                this.setScheduleType(ScheduleTypeFragment.TYPE_HOURLY);
            } else if (schedule.type() == Schedule.SCHEDULE_TYPE_CYCLE)
            {
                this.setScheduleType(ScheduleTypeFragment.TYPE_PERIOD);
            } else
            {
                this.setScheduleType(ScheduleTypeFragment.TYPE_ROUTINES);
            }
        }
    }

    public void setTimesPerDay(int timesPerDay) {
        this.timesPerDay = timesPerDay;
    }

    public int getTimesPerDay() {
        return timesPerDay;
    }

    public void clear() {
        instance = null;
    }

    @Override
    public String toString() {
        return "ScheduleCreationHelper{" +
                "selectedMed=" + selectedMed.name() +
                ", selectedScheduleIdx=" + selectedScheduleIdx +
                ", timesPerDay=" + timesPerDay +
                ", scheduleItems=" + scheduleItems.size() +
                '}';
    }

    public int getScheduleType()
    {
        return scheduleType;
    }

    public void setScheduleType(int scheduleType)
    {
        this.scheduleType = scheduleType;
    }

    public static boolean cycleEnabledForDate(LocalDate date, LocalDate s, int activeDays,
        int restDays)
    {
        DateTime start = s.toDateTimeAtStartOfDay();
        DateTime day = date.toDateTimeAtStartOfDay().plusDays(1);

        if (day.isBefore(start)) return false;

        int activePeriod = activeDays;
        int restPeriod = restDays;
        int cycleLength = activePeriod + restPeriod;
        int days = (int) new Interval(start, day).toDuration().getStandardDays();
        int cyclesUntilNow = days / cycleLength;

        Log.d("ScheduleHelper",
            "start: " + start.toString("dd/MM/YYYY") + ", day: " + day.toString("dd/MM/YYYY"));
        Log.d("ScheduleHelper", "Active: "
            + activePeriod
            + ", rest: "
            + restPeriod
            + ", cycle: "
            + cycleLength
            + ", days: "
            + days
            + ", cyclesUntilNow: "
            + cyclesUntilNow);

        // get start of current cycle

        DateTime cycleStart = start.plusDays(cyclesUntilNow * cycleLength);

        if (new Interval(cycleStart, cycleStart.plusDays(activePeriod)).contains(
            date.toDateTimeAtStartOfDay()))
        {
            return true;
        } else
        {
            return false;
        }

    }
}