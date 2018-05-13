package ms.passengeranalytics;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "MainActivity";

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private TextView mStatusTextView;
    private TextView mStatusTextViewExp;

    private EditText mEmailField;
    private EditText mPasswordField;
    private EditText mDayField;
    private LinearLayout mLayoutLoggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Views
        mStatusTextView = findViewById(R.id.status);
        mStatusTextViewExp = findViewById(R.id.expStatus);
        mEmailField = findViewById(R.id.field_email);
        mPasswordField = findViewById(R.id.field_password);
        mDayField = findViewById(R.id.field_days);
        mLayoutLoggedIn = findViewById(R.id.layout_logged_in);


        // Buttons
        findViewById(R.id.buttonLogin).setOnClickListener(this);
        findViewById(R.id.buttonRegister).setOnClickListener(this);
        findViewById(R.id.buttonLogout).setOnClickListener(this);
        findViewById(R.id.buttonPay).setOnClickListener(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);//was commented out before might break
    }
    // [END on_start_check_user]
    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }

        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            writeNewUser(user,0L);
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
        // [END create_user_with_email]
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                    }
                });
        // [END sign_in_with_email]
    }

    private void signOut() {
        mAuth.signOut();
        updateUI(null);
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Required.");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Required.");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            mStatusTextView.setText(getString(R.string.firebaseui_status_fmt,
                    user.getEmail(), user.getUid()));

            mDatabase.child("users").child(mAuth.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            User user = dataSnapshot.getValue(User.class);
                            Calendar date = Calendar.getInstance();
                            date.setTimeInMillis(user.expirationTime * 1000L);
                            int month = date.get(Calendar.MONTH) + 1;
                            String finalDate = date.get(Calendar.DAY_OF_MONTH)+ "." +month+ "." +date.get(Calendar.YEAR);
                            mStatusTextViewExp.setText(getString(R.string.firebaseui_status_exp,finalDate));
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

            findViewById(R.id.buttonLogin).setVisibility(View.GONE);
            findViewById(R.id.buttonRegister).setVisibility(View.GONE);
            findViewById(R.id.field_email).setVisibility(View.GONE);
            findViewById(R.id.field_password).setVisibility(View.GONE);
            findViewById(R.id.layout_logged_in).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText("Signed Out");

            findViewById(R.id.field_email).setVisibility(View.VISIBLE);
            findViewById(R.id.field_password).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonLogin).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonRegister).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_logged_in).setVisibility(View.GONE);
        }
    }

    private String usernameFromEmail(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        } else {
            return email;
        }
    }

    // [START basic_write]
    private void writeNewUser(FirebaseUser firebaseUser, Long expirationTime) {
        String username = usernameFromEmail(firebaseUser.getEmail());
        User user = new User(username, expirationTime);

        mDatabase.child("users").child(firebaseUser.getUid()).setValue(user);
    }
    // [END basic_write]

    private void changeExpDate(Integer days){
        Long unixTime = (System.currentTimeMillis()/1000L) + (days * 24 * 60 * 60);//current time * number of days in seconds
        mDatabase.child("users").child(mAuth.getUid()).child("expirationTime").setValue(unixTime);
    }

    private void updateDatabaseEntry(Integer days){
        changeExpDate(days);
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.buttonRegister) {
            createAccount(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.buttonLogin) {
            signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.buttonLogout) {
            signOut();
        } else if (i == R.id.buttonPay) {
            updateDatabaseEntry(Integer.parseInt(mDayField.getText().toString()));
        }
    }
}