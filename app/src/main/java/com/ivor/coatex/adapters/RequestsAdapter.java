package com.ivor.coatex.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.ivor.coatex.R;
import com.ivor.coatex.db.Contact;
import com.ivor.coatex.tor.Client;
import com.ivor.coatex.ui.DeleteContact;

import java.util.HashSet;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class RequestsAdapter extends RealmRecyclerViewAdapter<Contact, RequestViewHolder> {

    private Context mContext;

    private boolean inDeletionMode = false;
    private Set<Integer> countersToDelete = new HashSet<>();

    public RequestsAdapter(RealmResults<Contact> data, Context context) {
        super(data, true);
        mContext = context;

        setHasStableIds(true);
    }

    void enableDeletionMode(boolean enabled) {
        inDeletionMode = enabled;
        if (!enabled) {
            countersToDelete.clear();
        }
        notifyDataSetChanged();
    }

    Set<Integer> getCountersToDelete() {
        return countersToDelete;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final RequestViewHolder viewHolder = new RequestViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_request, parent, false));
        viewHolder.accept.setOnClickListener(v -> {
            Contact contact = viewHolder.contact;
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            contact.setIncoming(0);
            contact.setOutgoing(0);
            realm.commitTransaction();
            realm.close();
            Client.getInstance(mContext).startAskForNewMessages(contact.getAddress());
        });
        viewHolder.decline.setOnClickListener(v -> {
            final String address = viewHolder.contact.getAddress();
            final String name = viewHolder.contact.getName();
            final String description = viewHolder.contact.getDescription();
            final byte[] pubKey = viewHolder.contact.getPubKey();
            new DeleteContact(mContext).execute(viewHolder.contact.get_id());
            Snackbar.make(viewHolder.itemView, mContext.getString(R.string.contact_request_declined), Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, v1 -> Contact.addContact(mContext, address, name, description, pubKey, false, true))
                    .show();
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        final Contact contact = getItem(position);
        holder.contact = contact;
        holder.address.setText(contact.getAddress());
        holder.description.setText(contact.getDescription());
        String name = contact.getName();
        if (name == null || name.equals("")) name = "Anonymous";
        holder.name.setText(name);
    }

    @Override
    public long getItemId(int index) {
        //noinspection ConstantConditions
        return getItem(index).get_id();
    }

}

class RequestViewHolder extends RecyclerView.ViewHolder {
    TextView address, name, description;
    View accept, decline;
    View badge;
    TextView count;
    Contact contact;

    public RequestViewHolder(View view) {
        super(view);
        address = view.findViewById(R.id.address);
        name = view.findViewById(R.id.name);
        description = view.findViewById(R.id.description);
        accept = view.findViewById(R.id.accept);
        decline = view.findViewById(R.id.decline);
        badge = view.findViewById(R.id.badge);
        if (badge != null) count = view.findViewById(R.id.count);
    }
}
