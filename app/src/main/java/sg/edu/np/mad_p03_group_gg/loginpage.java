package sg.edu.np.mad_p03_group_gg;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import sg.edu.np.mad_p03_group_gg.view.ui.MainActivity;


public class loginpage extends AppCompatActivity {
    FirebaseAuth auth;
    //Set storage code for getting external storage permission
    private int EXTERNAL_STORAGE_PERMISSION_CODE = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginpage);
        //Get current authenticated instance
        auth = FirebaseAuth.getInstance(); // singleton
        //Check for permissions and ask permissions
        if (ContextCompat.checkSelfPermission(loginpage.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(loginpage.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    23);
        }
        //find views and buttons
        Button button = (Button) findViewById(R.id.changedtls);
        TextView signup = findViewById(R.id.Sign_up);
        TextView fgtpassword = findViewById(R.id.forgetpsswrdbtn);
        //On click listener to bring user to forget password activity when clicked
        fgtpassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent fgtpassword = new Intent(loginpage.this, forget_password_activitiy.class);

                startActivity(fgtpassword);
                //Pause current activity, enable user to return to this activity when needed
                loginpage.this.onPause();
            }
        });
        // On click for sigin button
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sign in
                Log_in(view);

            }
        });
        //On click, bring user to signup activity
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signup = new Intent(loginpage.this,
                        signupactivity.class);
                //Clear activity stack on top (prevent duplication of activities)
                signup.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(signup);//Starts sign up activity
                //Pause current activity, enable user to retun to loginactivity if needed


            }
        });

        //Authenticate users

    }
    boolean isEmail(EditText text) {  // checks if email input field is correct also checks if input field is empty using patterns libary
        CharSequence email = text.getText().toString();
        return (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }



    public void Log_in(View v) {
        //Find view
        TextView error = findViewById(R.id.siginerror);
        EditText Email = (EditText) findViewById(R.id.ccredentialemail);
        EditText password = (EditText) findViewById(R.id.password_toggle);
        String email = Email.getText().toString().trim();
        String Password = password.getText().toString();
        //Set error back to invisible
        error.setVisibility(View.INVISIBLE);
        //If email is missing, set error
        if (!isEmail(Email)) {
            Email.setError("Invalid email");
            return;

        }
        // If password is missing, set error message
        if (TextUtils.isEmpty(Password)) {
            password.setError("Missing Password");
            return;

        }

        //Get auth user from firebase
        auth.signInWithEmailAndPassword(email, Password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    //When user's sign in is sucessful, it will automatically go to the next page
                    Intent mainActivity = new Intent(loginpage.this, MainActivity.class);
                    Toast.makeText(loginpage.this, "Signed-In!", Toast.LENGTH_SHORT).show();

                    startActivity(mainActivity); //Starts up main activity
                    //Finish current activity
                    loginpage.this.finish();
                }
            }

        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // On failure, show this toast message
                                        Toast.makeText(loginpage.this, "Check your internet connection or your email and password", Toast.LENGTH_SHORT).show();
                                    }

                                }
        );
    }
    public void request(View view) {
        // Requesting Permission to access External Storage
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                EXTERNAL_STORAGE_PERMISSION_CODE);
    }
}