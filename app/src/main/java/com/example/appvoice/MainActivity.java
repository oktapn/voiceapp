package com.example.appvoice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.voxeet.sdk.VoxeetSdk;
import com.voxeet.sdk.json.UserInfo;
import com.voxeet.sdk.models.Conference;
import com.voxeet.sdk.models.v1.CreateConferenceResult;
import com.voxeet.sdk.services.conference.information.ConferenceInformation;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import eu.codlab.simplepromise.solve.ErrorPromise;
import eu.codlab.simplepromise.solve.PromiseExec;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.user_name)
    EditText TVUsername;

    @Bind(R.id.conference_name)
    EditText conference_name;

    protected List<View> views = new ArrayList<>();

    protected List<View> buttonsNotLoggedIn = new ArrayList<>();

    protected List<View> buttonsInConference = new ArrayList<>();

    protected List<View> buttonsNotInConference = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //we now initialize the sdk
        VoxeetSdk.initialize("ODIyZWc1YjZhOHZs", "ODIyZWc1YjZhOHZs");

        add(views, R.id.login);
        add(views, R.id.logout);

        add(buttonsNotLoggedIn, R.id.login);
        add(buttonsNotLoggedIn, R.id.user_name);

        add(buttonsInConference, R.id.logout);

        add(buttonsNotInConference, R.id.logout);

        //we add the join button to let it enable only when not in a conference
        add(views, R.id.join);

        add(buttonsNotInConference, R.id.join);

        //we add the leave button to be available while in conference
        add(views, R.id.leave);

        add(buttonsInConference, R.id.leave);
    }

    protected void onResume() {
        super.onResume();

        //here will be put the permission check

        //we update the various views to enable or disable the ones we want to
        updateViews();

        VoxeetSdk.instance().register(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 0x20);
        }
    }

    private void updateViews() {
        //disable every views
        setEnabled(views, false);

        //if the user is not connected, we will only enabled the not logged
        if (!VoxeetSdk.session().isSocketOpen()) {
            setEnabled(buttonsNotLoggedIn, true);
            return;
        }

        ConferenceInformation current = VoxeetSdk.conference().getCurrentConference();
        //we can now add the logic to manage our basic state
        if (null != current && VoxeetSdk.conference().isLive()) {
            setEnabled(buttonsInConference, true);
        }

        //we can now add the logic to manage our basic state
        if (null != current && VoxeetSdk.conference().isLive()) {
            setEnabled(buttonsInConference, true);
        } else {
            setEnabled(buttonsNotInConference, true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //register the current activity in the SDK
        VoxeetSdk.instance().unregister(this);
    }

    private ErrorPromise error() {
        return error -> {
            Toast.makeText(MainActivity.this, "ERROR...", Toast.LENGTH_SHORT).show();
            error.printStackTrace();
            updateViews();
        };
    }

    private void setEnabled(List<View> views, boolean enabled) {
        for (View view : views) view.setEnabled(enabled);
    }

    private MainActivity add(List<View> list, int id) {
        list.add(findViewById(id));
        return this;
    }

    @OnClick(R.id.login)
    public void onLogin() {
        VoxeetSdk.session().open(new UserInfo(TVUsername.getText().toString(), "", ""))
                .then((result, solver) -> {
                    Toast.makeText(MainActivity.this, "started...", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error(error());
    }

    @OnClick(R.id.logout)
    public void onLogout() {
        VoxeetSdk.session().close()
                .then((result, solver) -> {
                    Toast.makeText(MainActivity.this, "logout done", Toast.LENGTH_SHORT).show();
                    updateViews();
                }).error(error());
    }

    @OnClick(R.id.join)
    public void onJoin() {
        VoxeetSdk.conference().create(conference_name.getText().toString())
                .then((PromiseExec<CreateConferenceResult, Conference>) (result, solver) ->
                        solver.resolve(VoxeetSdk.conference().join(result.conferenceId)))
                .then((result, solver) -> {
                    Toast.makeText(MainActivity.this, "started...", Toast.LENGTH_SHORT).show();
                    updateViews();
                })
                .error(error());
    }

    @OnClick(R.id.leave)
    public void onLeave() {
        VoxeetSdk.conference().leave()
                .then((result, solver) -> updateViews()).error(error());
    }
}
