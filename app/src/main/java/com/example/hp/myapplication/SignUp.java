package com.example.hp.myapplication;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.hp.myapplication.Model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

public class SignUp extends AppCompatActivity {
        MaterialEditText edtem,edtPass,edtCPass;
        Button btnSignUp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        edtem = (MaterialEditText)findViewById(R.id.edtemail);
        edtCPass = (MaterialEditText)findViewById(R.id.edtCPassword);
        edtPass= (MaterialEditText)findViewById(R.id.edtPassword);
        btnSignUp = (Button)findViewById(R.id.btnSignUp);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user = database.getReference("https://food-hut-777-default-rtdb.firebaseio.com");

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final ProgressDialog progressDialog = new ProgressDialog(SignUp.this);
                progressDialog.setMessage("Please wait..!");
                progressDialog.show();

                table_user.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.child(edtem.getText().toString()).exists()){
                            progressDialog.dismiss();
                            Toast.makeText(SignUp.this,"User already exists!",Toast.LENGTH_SHORT).show();
                        }
                        else {
//                            if(edtem.getText().toString().isEmpty() || edtPass.getText().toString().isEmpty() || edtCPass.getText().toString().isEmpty())
//                            {
//                                Toast.makeText(SignUp.this,"Please fill all the fields",Toast.LENGTH_SHORT).show();
//                            }
//                            else if(!edtPass.getText().toString().equals(edtCPass.getText().toString()))
//                            {
//                                Toast.makeText(SignUp.this,"Passwords are not matching",Toast.LENGTH_SHORT).show();
//
//                            }
//                            else
//                            {
//                                progressDialog.dismiss();
//                                User user = new User(edtem.getText().toString(),edtPass.getText().toString());
//                                table_user.child(edtem.getText().toString()).setValue(user);
//                                Toast.makeText(SignUp.this,"SignUp successfully! " ,Toast.LENGTH_SHORT).show();
//                                finish();
//                            }

                            progressDialog.dismiss();
                            User user = new User(edtem.getText().toString(),edtPass.getText().toString());
                            table_user.child(edtem.getText().toString()).setValue(user);
                            Toast.makeText(SignUp.this,"SignUp successfully! " ,Toast.LENGTH_SHORT).show();
                           finish();

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
