package com.ivor.coatex.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ivor.coatex.R;
import com.ivor.coatex.adapters.RequestsAdapter;
import com.ivor.coatex.db.Contact;

import io.realm.Realm;
import io.realm.RealmResults;

public class RequestsFragment extends Fragment {

    private RecyclerView mRVRequests;
    private RequestsAdapter mRequestsAdapter;

    private View mainView;

    RealmResults<Contact> mContacts;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_requests, container, false);

        mRVRequests = mainView.findViewById(R.id.rcvwRequests);

        mContacts = Realm.getDefaultInstance().where(Contact.class).notEqualTo("incoming", 0).findAll();
        mRVRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        mRequestsAdapter = new RequestsAdapter(mContacts, getContext());
        mRVRequests.setAdapter(mRequestsAdapter);
        mRVRequests.setHasFixedSize(true);
        mRVRequests.addItemDecoration(new DividerItemDecoration(mainView.getContext(), DividerItemDecoration.VERTICAL));
        updateNoDataView();

        return mainView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mContacts.removeAllChangeListeners();
    }

    private void updateNoDataView() {
        mainView.findViewById(R.id.txtNoRequests).setVisibility(mRequestsAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);
    }
}
