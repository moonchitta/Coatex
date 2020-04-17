package com.ivor.coatex.db;


import android.content.Context;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * contact / contact request table
 * _id: primary key
 * address: 16 character onion address
 * name: nick-name
 * outgoing: pending outgoing friend request
 * incoming: incoming friend request / someone else wants to add us / will be shown on the requests tab instead of the contacts tab
 * pending: the number of unread messages
 */
public class Contact extends RealmObject {

    @PrimaryKey
    private long _id;
    @Index
    private String address;
    @Index
    private String name;
    private int outgoing;
    @Index
    private int incoming;
    private long lastOnlineTime;
    private int pending;
    private long lastMessageTime;
    private String description;
    private byte[] pubKey;

    public long get_id() {
        return _id;
    }

    public void set_id(long _id) {
        this._id = _id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOutgoing() {
        return outgoing;
    }

    public void setOutgoing(int outgoing) {
        this.outgoing = outgoing;
    }

    public int getIncoming() {
        return incoming;
    }

    public void setIncoming(int incoming) {
        this.incoming = incoming;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getLastOnlineTime() {
        return lastOnlineTime;
    }

    public void setLastOnlineTime(long lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public static boolean addContact(Context context, String id, String name, String description, byte[] pubKey, boolean outgoing, boolean incoming) {
        name = name.trim();
        id = id.trim().toLowerCase();
        Realm realm = Realm.getDefaultInstance();
        Contact savedContact = realm.where(Contact.class).equalTo("address", id).findFirst();
        if (savedContact == null) {
            realm.beginTransaction();
            Contact contact = realm.createObject(Contact.class, getNextId());
            contact.setName(name);
            contact.setDescription(description);
            contact.setAddress(id);
            contact.setPubKey(pubKey);
            contact.setOutgoing(outgoing ? 1 : 0);
            contact.setIncoming(incoming ? 1 : 0);
            realm.commitTransaction();
            Database.getInstance(context).addNewRequest();
        }
        realm.close();
        return savedContact == null;
    }

    public static boolean hasContact(Context context, String id) {
        Realm realm = Realm.getDefaultInstance();
        Contact contact = realm.where(Contact.class).equalTo("address", id).equalTo("incoming", 0).findFirst();
        realm.close();
        return contact != null;
    }

    public static long getNextId() {
        Realm realm = Realm.getDefaultInstance();
        Number maxId = realm.where(Contact.class).max("_id");
        // If there are no rows, currentId is null, so the next id must be 1
        // If currentId is not null, increment it by 1
        realm.close();
        return (maxId == null) ? 1 : maxId.longValue() + 1;
    }

    public static void acceptContact(Context context, String id) {
        Realm realm = Realm.getDefaultInstance();
        Contact contact = realm.where(Contact.class).equalTo("address", id).findFirst();
        if (contact == null) {
            realm.beginTransaction();
            contact.setIncoming(0);
            contact.setOutgoing(0);
            realm.commitTransaction();
        }
        realm.close();
    }
}
