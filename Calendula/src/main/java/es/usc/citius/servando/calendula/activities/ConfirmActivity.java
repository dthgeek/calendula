package es.usc.citius.servando.calendula.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

import es.usc.citius.servando.calendula.CalendulaActivity;
import es.usc.citius.servando.calendula.CalendulaApp;
import es.usc.citius.servando.calendula.R;
import es.usc.citius.servando.calendula.database.DB;
import es.usc.citius.servando.calendula.persistence.DailyScheduleItem;
import es.usc.citius.servando.calendula.persistence.Medicine;
import es.usc.citius.servando.calendula.persistence.Patient;
import es.usc.citius.servando.calendula.persistence.Presentation;
import es.usc.citius.servando.calendula.persistence.Routine;
import es.usc.citius.servando.calendula.persistence.Schedule;
import es.usc.citius.servando.calendula.persistence.ScheduleItem;
import es.usc.citius.servando.calendula.scheduling.AlarmScheduler;
import es.usc.citius.servando.calendula.scheduling.ScheduleUtils;
import es.usc.citius.servando.calendula.util.AvatarMgr;

public class ConfirmActivity extends CalendulaActivity {


    boolean isRoutine;
    Patient patient;
    Routine routine;
    Schedule schedule;
    LocalTime time;
    List<DailyScheduleItem> items = new ArrayList<>();

    DateTimeFormatter df = DateTimeFormat.forPattern("kk:mm");

    RecyclerView listView;
    ImageView avatar;
    TextView title;

    ImageView avatarTitle;
    TextView titleTitle;

    TextView hour;
    TextView minute;

    ConfirmItemAdapter itemAdapter;

    IconicsDrawable unchekedIcon;
    IconicsDrawable chekedIcon;

    FloatingActionButton fab;

    boolean stateChanged = false;
    int position = -1;



    int color;
    private String action;
    private AppBarLayout appBarLayout;
    CollapsingToolbarLayout toolbarLayout;

    View toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);
        processIntent();
        color = AvatarMgr.colorsFor(getResources(), patient.avatar())[0];
        color = Color.parseColor("#263238");
        //color = ScreenUtils.equivalentNoAlpha(color, 0.6f);

        setupStatusBar(Color.TRANSPARENT);
        setupToolbar("", Color.TRANSPARENT, Color.WHITE);
        toolbar.setTitleTextColor(Color.WHITE);
        findViewById(R.id.imageView5).setBackgroundColor(patient.color());
        fab = (FloatingActionButton) findViewById(R.id.myFAB);
        listView = (RecyclerView) findViewById(R.id.listView);
        avatar = (ImageView) findViewById(R.id.patient_avatar);
        title = (TextView) findViewById(R.id.routine_name);

        avatarTitle = (ImageView) findViewById(R.id.patient_avatar_title);
        titleTitle = (TextView) findViewById(R.id.routine_name_title);

        hour = (TextView) findViewById(R.id.routines_list_item_hour);
        minute = (TextView) findViewById(R.id.routines_list_item_minute);
        toolbarTitle = findViewById(R.id.toolbar_title);
        avatar.setImageResource(AvatarMgr.res(patient.avatar()));
        avatarTitle.setImageResource(AvatarMgr.res(patient.avatar()));
        titleTitle.setText(patient.name());
        title.setText(isRoutine ? routine.name() : schedule.toReadableString(this));

        hour.setText(time.toString("kk:"));
        minute.setText(time.toString("mm"));

        fab.setImageDrawable(new IconicsDrawable(this)
                .icon(CommunityMaterial.Icon.cmd_check_all)
                .color(Color.WHITE)
                .sizeDp(24)
                .paddingDp(0));

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (DailyScheduleItem item : items) {
                    item.setTakenToday(true);
                    item.save();
                }
                itemAdapter.notifyDataSetChanged();
                stateChanged = true;


                String msg =ConfirmActivity.this.getString(R.string.all_meds_taken);
                Toast.makeText(ConfirmActivity.this, msg, Toast.LENGTH_SHORT).show();
                fab.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        supportFinishAfterTransition();
                    }
                }, 500);
            }
        });

        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        AppBarLayout.OnOffsetChangedListener mListener = new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if(toolbarLayout.getHeight() + verticalOffset < 2 * ViewCompat.getMinimumHeight(toolbarLayout)) {
//                    toolbarTitle.animate().alpha(1);
                } else {
//                    toolbarTitle.animate().alpha(0);
                }
            }
        };
        appBarLayout.addOnOffsetChangedListener(mListener);
        toolbarLayout.setContentScrimColor(patient.color());
        setupListView();

        if("delay".equals(action)){
            showDelayDialog();
        }


    }

    private void setupListView() {

        loadItems();
        itemAdapter = new ConfirmItemAdapter();
        LinearLayoutManager llm = new LinearLayoutManager(this);
        listView.setLayoutManager(llm);
        listView.setAdapter(itemAdapter);
        listView.setItemAnimator(new DefaultItemAnimator());
    }

    private void processIntent() {
        Intent i = getIntent();
        action = i.getStringExtra("action");
        position = i.getIntExtra("position", -1);
        Long id = i.getLongExtra("routine_id",-1);
        if( id != -1){
            isRoutine = true;
            routine = Routine.findById(id);
            time = routine.time();
            patient = routine.patient();

        }else{
            id = i.getLongExtra("schedule_id", -1);
            String timeStr = i.getStringExtra("schedule_time");
            time = LocalTime.parse(timeStr, df);
            schedule = Schedule.findById(id);
            patient = schedule.patient();
        }
    }

    private void loadItems(){
        if(isRoutine){
            List<ScheduleItem> rsi = ScheduleUtils.getRoutineScheduleItems(routine, true);
            for(ScheduleItem si : rsi){
                items.add(DailyScheduleItem.findByScheduleItem(si));
            }
        }else{
            items.add(DB.dailyScheduleItems().findByScheduleAndTime(schedule,time));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.confirm, menu);

        menu.findItem(R.id.action_delay).setIcon(new IconicsDrawable(this)
                .icon(CommunityMaterial.Icon.cmd_history)
                .color(Color.WHITE)
                .sizeDp(24));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            case R.id.action_delay:
                showDelayDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void showDelayDialog() {
        final int[] values = this.getResources().getIntArray(R.array.delays_array_values);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.notification_delay)
                .setItems(R.array.delays_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int minutes = values[which];
                        if(isRoutine) {
                            AlarmScheduler.instance().onDelayRoutine(routine, ConfirmActivity.this, minutes);
                        }
                        else{
                            AlarmScheduler.instance().onDelayHourlySchedule(schedule, time, ConfirmActivity.this, minutes);
                        }

                        String msg =ConfirmActivity.this.getString(R.string.alarm_delayed_message, minutes);
                        Toast.makeText(ConfirmActivity.this, msg, Toast.LENGTH_SHORT).show();
                        supportFinishAfterTransition();

                    }
                });
        builder.create().show();
    }


    protected void onDailyAgendaItemCheck() {

        int total = items.size();
        int checked = 0;

        for(DailyScheduleItem i : items){
            if(i.takenToday())
                checked++;
        }

        if (checked == total) {
            if (isRoutine) {
                AlarmScheduler.instance().onCancelRoutineNotifications(routine, this);
            } else {
                AlarmScheduler.instance().onCancelHourlyScheduleNotifications(schedule, time, this);
            }
        } else {
            if (isRoutine) {
                AlarmScheduler.instance().onDelayRoutine(routine, this);
            } else {
                AlarmScheduler.instance().onDelayHourlySchedule(schedule, time, this);
            }
        }
    }


    private Drawable getCheckedIcon(int color) {
        if(chekedIcon == null) {
            chekedIcon = new IconicsDrawable(this, CommunityMaterial.Icon.cmd_checkbox_marked_circle_outline) //cmd_checkbox_marked_outline
                    .sizeDp(30)
                    .paddingDp(0)
                    .color(color);
        }
        return chekedIcon;
    }

    private Drawable getUncheckedIcon(int color) {
        if (unchekedIcon == null) {
            unchekedIcon = new IconicsDrawable(this, CommunityMaterial.Icon.cmd_checkbox_blank_circle_outline) //cmd_checkbox_blank_outline
                    .sizeDp(30)
                    .paddingDp(0)
                    .color(color);
        }
        return unchekedIcon;
    }

    public String getDisplayableDose(String dose, Medicine m) {
        return dose + " " + m.presentation().units(getResources());

    }



    @Override
    protected void onDestroy() {

        if(stateChanged && position != -1){
            CalendulaApp.eventBus().post(new ConfirmStateChangeEvent(position));
        }
        super.onDestroy();
    }

    public static class ConfirmStateChangeEvent {
        public int position = -1;
        public ConfirmStateChangeEvent(int position) {
            this.position = position;
        }
    }


    private class ConfirmItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{


        public class ConfirmItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            TextView med;
            TextView dose;
            ImageButton check;
            ImageView icon;

            DailyScheduleItem dailyScheduleItem;

            public ConfirmItemViewHolder(View itemView) {
                super(itemView);
                med  = (TextView) itemView.findViewById(R.id.med_item_name);
                dose = (TextView) itemView.findViewById(R.id.med_item_dose);
                check = (ImageButton) itemView.findViewById(R.id.check_button);
                icon = (ImageView) itemView.findViewById(R.id.imageView);
                itemView.setOnClickListener(this);
                check.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                boolean taken = dailyScheduleItem.takenToday();
                dailyScheduleItem.setTakenToday(!taken);
                dailyScheduleItem.save();
                stateChanged = true;
                onDailyAgendaItemCheck();
                notifyItemChanged(getAdapterPosition());
            }
        }

        ConfirmItemViewHolder h;
        DailyScheduleItem i;
        ScheduleItem si;
        Long sid;
        Schedule s;
        Medicine m;
        Presentation p;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.confirm_activity_list_item, parent, false);
            return new ConfirmItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            h = (ConfirmItemViewHolder) holder;
            i = items.get(position);
            si = i.scheduleItem();
            sid = i.boundToSchedule() ?  i.schedule().getId() : si.schedule().getId();
            s = DB.schedules().findById(sid);
            m = s.medicine();
            p = m.presentation();

            h.med.setText(m.name());
            h.dose.setText(getDisplayableDose(i.boundToSchedule() ? s.displayDose() : si.displayDose(), m));

            h.dailyScheduleItem = i;
            updateCheckedStatus();
        }

        void updateCheckedStatus(){
            Drawable medDrawable = new IconicsDrawable(ConfirmActivity.this)
                    .icon(p.icon())
                    .color(i.takenToday() ? Color.parseColor("#81c784") : Color.parseColor("#11000000"))
                    .sizeDp(36)
                    .paddingDp(0);

            Drawable checkDrawable = i.takenToday() ?
                    getCheckedIcon(Color.parseColor("#81c784"))
                    : getUncheckedIcon(Color.parseColor("#11000000"));

            h.check.setImageDrawable(checkDrawable);
            h.icon.setImageDrawable(medDrawable);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}