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

import com.ivor.coatex.MainActivity;
import com.ivor.coatex.R;
import com.ivor.coatex.adapters.ContactsAdapter;
import com.ivor.coatex.db.Contact;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class ContactsFragment extends Fragment {

    private RecyclerView mRVContacts;
    public ContactsAdapter mContactsAdapter;

    private View mainView;

    private RealmResults<Contact> mContacts;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mainView = inflater.inflate(R.layout.fragment_contacts, container, false);

        mRVContacts = mainView.findViewById(R.id.rcvwContacts);

        mContacts = Realm.getDefaultInstance().where(Contact.class)
                .equalTo("incoming", 0)
                .findAll()
                .sort("lastMessageTime", Sort.DESCENDING);

        mRVContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        mContactsAdapter = new ContactsAdapter(mContacts, getActivity(), false);
        mRVContacts.setAdapter(mContactsAdapter);
        mRVContacts.setHasFixedSize(true);
        mRVContacts.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        updateNoDataView();

        mContacts.addChangeListener((contacts1, changeSet) -> updateNoDataView());
        return mainView;
    }

    public void filter(String text) {
        if (mContactsAdapter != null) {
            mContactsAdapter.filter(text);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        mContacts.removeAllChangeListeners();
    }

    private void updateNoDataView() {
        mainView.findViewById(R.id.txtNoContacts).setVisibility(mContactsAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);

        ((MainActivity) getActivity()).setRequestsUpdate();
    }
}