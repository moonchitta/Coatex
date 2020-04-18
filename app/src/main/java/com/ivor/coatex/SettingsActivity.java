package com.ivor.coatex;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.ivor.coatex.crypto.AdvancedCrypto;
import com.ivor.coatex.db.Contact;
import com.ivor.coatex.db.Database;
import com.ivor.coatex.db.Message;
import com.ivor.coatex.tor.Tor;
import com.ivor.coatex.utils.PasswordValidator;
import com.ivor.coatex.utils.Settings;
import com.ivor.coatex.utils.Util;
import com.ivor.coatex.utils.ZipManager;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import io.realm.Realm;
import io.realm.RealmResults;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getPrefs(this);
        setContentView(R.layout.settings_activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        ImageView imageView = findViewById(R.id.imvwDp);
        imageView.setOnClickListener(view -> CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setOutputUri(Uri.fromFile(new File(getFilesDir(), "dp.jpg")))
                .setFixAspectRatio(true)
                .start(this));
        TextView txtName = findViewById(R.id.txtName);
        txtName.setOnClickListener(view -> changeName());
        TextView txtAddress = findViewById(R.id.txtAddress);

        Database db = Database.getInstance(this);
        Glide.with(this).load(Uri.fromFile(new File(db.get("dp"))))
                .placeholder(R.drawable.ic_launcher_background)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);
        txtName.setText(db.getName().trim().isEmpty() ? "Anonymous" : db.getName());
        txtAddress.setText(Tor.getInstance(this).getID());

        getFragmentManager().beginTransaction().add(R.id.content, new SettingsFragment()).commit();

        ScrollView scrollView = findViewById(R.id.scrollView);

        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_UP));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Glide.with(this)
                        .load(resultUri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(((ImageView) findViewById(R.id.imvwDp)));
                Database.getInstance(this).put("dp", resultUri.getPath());
                Log.d(TAG, "onActivityResult: " + resultUri.getPath());
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            }
        }
    }

    private void changeName() {
        final FrameLayout view = new FrameLayout(this);
        final EditText editText = new EditText(this);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        view.addView(editText);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        final Database db = Database.getInstance(this);

        view.setPadding(padding, padding, padding, padding);
        editText.setText(db.getName());
        new AlertDialog.Builder(this)
                .setTitle(R.string.change_alias)
                .setView(view)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    db.setName(editText.getText().toString().trim());
                    ((TextView) findViewById(R.id.txtName)).setText(db.getName().trim().isEmpty() ? "Anonymous" : db.getName());
                    Snackbar.make(findViewById(R.id.txtName), R.string.alias_changed, Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.prefs_menu, menu);
        return true;
    }

    void doReset() {
        Settings.getPrefs(SettingsActivity.this).edit().clear().apply();
        Intent intent = getIntent();
        finish();
        startActivity(intent);
        overridePendingTransition(0, 0);
        Snackbar.make(findViewById(R.id.content), "All settings reset", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        if (id == R.id.action_reset) {
            doReset();
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {

        public static final int PR_WRITE_EXTERNAL_STORAGE = 10;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
            findPreference("use_dark_mode").setOnPreferenceChangeListener((preference, o) -> {
                boolean useDarkTheme = (boolean) o;
                if (useDarkTheme) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                return true;
            });

            findPreference("export_id").setOnPreferenceClickListener(preference -> {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PR_WRITE_EXTERNAL_STORAGE);
                    return true;
                }
                showPasswordDialog();
                return true;
            });

            findPreference("about").setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return false;
            });

        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               String[] permissions, int[] grantResults) {
            switch (requestCode) {
                case PR_WRITE_EXTERNAL_STORAGE: {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        showPasswordDialog();
                    } else {
                        Toast.makeText(getActivity(), "External storage access denied", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            }
        }

        private void showPasswordDialog() {
            final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_two_password, null);
            final TextInputEditText txtPassword = view.findViewById(R.id.txtPassword);
            final TextInputEditText txtConfPassword = view.findViewById(R.id.txtConfPassword);

            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.password_for_export_backup)
                    .setView(view)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {

                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    }).create();

            alertDialog.setOnShowListener(dialogInterface -> {

                Button button = alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(view1 -> {

                    String password = txtPassword.getText().toString().trim();
                    String confPassword = txtConfPassword.getText().toString().trim();
                    if (password.length() < 8) {
                        txtPassword.setError("Password is less than 8 characters");
                        return;
                    }
                    if (!password.equals(confPassword)) {
                        txtConfPassword.setError("Password does not match");
                        return;
                    }

                    PasswordValidator pv = PasswordValidator.getInstance();
                    if (!pv.validate(password)) {
                        txtPassword.setError("Password must have 1 numeric, 1 upper letter and 1 special character");
                        return;
                    }

                    new CreateZip(getActivity(), password).execute();

                    alertDialog.dismiss();
                });
            });

            alertDialog.show();
        }
    }

    private static class CreateZip extends AsyncTask<Void, Void, String> {

        private ProgressDialog progressDialog;
        private Context mContext;
        private String mPassword;

        public CreateZip(Context context, String password) {
            mContext = context;
            mPassword = password;
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Creating export zip");
            progressDialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {

            // check if external folder does not exist
            if (!Util.EXTERNAL_FOLDER.exists()) {
                Util.EXTERNAL_FOLDER.mkdir();
            }

            File dest = new File(mContext.getFilesDir(), "coatex_backup.zip");
            ZipManager zipManager = new ZipManager(mContext);
            zipManager.makeZip(dest.getAbsolutePath());
            String privateKey = "tor/torserv/private_key";
            String hostname = "tor/torserv/hostname";
            String settings = "settings.prop";
            String dpFile = "dp.jpg";
            String messageFile = "messages.json";
            String contactFile = "contacts.json";

            Properties properties = new Properties();
            properties.setProperty("name", Database.getInstance(mContext).get("name"));
            properties.setProperty("use_dark_theme", Settings.getPrefs(mContext).getBoolean("use_dark_mode", true) + "");
            try {
                properties.store(new FileOutputStream(new File(mContext.getCacheDir(), settings)), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            zipManager.addZipFile(new File(mContext.getFilesDir(), privateKey).getAbsolutePath(), privateKey);
            zipManager.addZipFile(new File(mContext.getFilesDir(), hostname).getAbsolutePath(), hostname);
            zipManager.addZipFile(new File(mContext.getCacheDir(), settings).getAbsolutePath(), settings);
            zipManager.addZipFile(new File(mContext.getFilesDir(), dpFile).getAbsolutePath(), dpFile);
            try {
//                zipManager.addZipFile(createMessageFile().getAbsolutePath(), messageFile);
                zipManager.addZipFile(createContactFile().getAbsolutePath(), contactFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            zipManager.closeZip();

            File destination = new File(Util.EXTERNAL_FOLDER, "coatex_backup.zip");

            try {
                AdvancedCrypto advancedCrypto = new AdvancedCrypto(mPassword);
                advancedCrypto.encryptFile(dest.getAbsolutePath(), destination.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return destination.getAbsolutePath();
        }

        private File createMessageFile() throws IOException {
            File messageFile = new File(mContext.getCacheDir(), "messages.json");
            Gson gson = new Gson();
            Realm realm = Realm.getDefaultInstance();
            RealmResults<Message> messages = realm.where(Message.class).findAll();
            JsonWriter jsonWriter = new JsonWriter(new FileWriter(messageFile));
            jsonWriter.beginArray();
            for (int i = 0; i < messages.size(); i++) {
                jsonWriter.jsonValue(gson.toJson(realm.copyFromRealm(messages.get(i))));
                jsonWriter.flush();
            }
            jsonWriter.endArray();
            jsonWriter.close();
            realm.close();
            return messageFile;
        }

        private File createContactFile() throws IOException {
            File contactFile = new File(mContext.getCacheDir(), "contacts.json");
            Gson gson = new Gson();
            Realm realm = Realm.getDefaultInstance();
            RealmResults<Contact> contacts = realm.where(Contact.class).findAll();
            JsonWriter jsonWriter = new JsonWriter(new FileWriter(contactFile));
            jsonWriter.beginArray();
            for (int i = 0; i < contacts.size(); i++) {
                jsonWriter.jsonValue(gson.toJson(realm.copyFromRealm(contacts.get(i))));
                jsonWriter.flush();
            }
            jsonWriter.endArray();
            jsonWriter.close();
            realm.close();
            return contactFile;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressDialog.dismiss();

            Toast.makeText(mContext, "Zip file created: " + s, Toast.LENGTH_SHORT).show();
        }
    }
}