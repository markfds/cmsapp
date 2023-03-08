package com.example.hp.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hp.myapplication.Common.Common;
import com.example.hp.myapplication.Model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

public class SignIn extends AppCompatActivity {
    EditText edtem1,edtPass1;
    Button btnSignIn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        edtem1 = (MaterialEditText)findViewById(R.id.edtemail);
        edtPass1 = (MaterialEditText)findViewById(R.id.edtPassword);


        btnSignIn = (Button)findViewById(R.id.btnSignIn);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user = database.getReferenceFromUrl("https://food-hut-777-default-rtdb.firebaseio.com/");

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final ProgressDialog progressDialog = new ProgressDialog(SignIn.this);
                progressDialog.setMessage("Please wait!");
                progressDialog.show();
                table_user.addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.child(edtem1.getText().toString()).exists()) {

                            progressDialog.dismiss();

                            User user = dataSnapshot.child(edtem1.getText().toString()).getValue(User.class);
                            user.setPhone(edtem1.getText().toString());
                            if (user.getPassword().equals(edtPass1.getText().toString())) {
                                //Toast.makeText(SignIn.this, "Welcome to Tomato..!", Toast.LENGTH_SHORT).show();
                                Intent home = new Intent(SignIn.this,Home.class);
                                Common.currentUser = user;
                                startActivity(home);
                                finish();
                            } else {
                                Toast.makeText(SignIn.this, "Phone no. or Password is incorrect..", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else{
                            progressDialog.dismiss();
                            Toast.makeText(SignIn.this, "Please Sign Up First..!",Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
    }
}
