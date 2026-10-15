package com.example.wycrimeapp.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wycrimeapp.R;
import com.example.wycrimeapp.controller.AppDatabase;
import com.example.wycrimeapp.model.User;
import com.example.wycrimeapp.util.PasswordUtils;

public class RegisterActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editEmail = findViewById(R.id.editRegEmail);
        editPassword = findViewById(R.id.editRegPassword);
        btnRegister = findViewById(R.id.btnRegisterConfirm);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();
        // SECURITY: Users must never be able to self-assign the ADMIN role.
        // All registrations are forced to USER (isAdmin = false).
        boolean isAdmin = false;

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hash the password before persisting.
        // NOTE: The app also supports legacy plaintext passwords during login and upgrades them.
        String hashedPassword = PasswordUtils.hashPassword(password);

        User newUser = new User(email, hashedPassword, isAdmin);
        AppDatabase.getInstance(this).userDao().registerUser(newUser);

        Toast.makeText(this, "Registered successfully! You can now login.", Toast.LENGTH_LONG).show();
        finish(); // go back to login
    }
}