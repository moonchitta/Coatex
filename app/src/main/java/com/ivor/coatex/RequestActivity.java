package com.ivor.coatex;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ivor.coatex.adapters.RequestsAdapter;
import com.ivor.coatex.db.Contact;

import io.realm.Realm;
import io.realm.RealmResults;

public class RequestActivity extends AppCompatActivity {

    private RecyclerView mRVRequests;
    private RequestsAdapter mRequestsAdapter;

    private RealmResults<Contact> mContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRVRequests = findViewById(R.id.rcvwRequests);

        mContacts = Realm.getDefaultInstance().where(Contact.class).notEqualTo("incoming", 0).findAll();
        mContacts.addChangeListener((contacts, changeSet) -> updateNoDataView());
        mRVRequests.setLayoutManager(new LinearLayoutManager(this));
        mRequestsAdapter = new RequestsAdapter(mContacts, this);
        mRVRequests.setAdapter(mRequestsAdapter);
        mRVRequests.setHasFixedSize(true);
        mRVRequests.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        updateNoDataView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContacts.removeAllChangeListeners();
    }

    private void updateNoDataView() {
        findViewById(R.id.txtNoRequests).setVisibility(mRequestsAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);
    }

}
