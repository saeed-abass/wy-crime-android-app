package com.example.wycrimeapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wycrimeapp.R;
import com.example.wycrimeapp.controller.AppDatabase;
import com.example.wycrimeapp.model.User;
import com.example.wycrimeapp.util.PasswordUtils;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user by email only
        User user = AppDatabase.getInstance(this).userDao().loginUser(email);

        boolean passwordOk = false;
        if (user != null) {
            // Prefer hashed verification; transparently support legacy plaintext passwords.
            passwordOk = PasswordUtils.verifyPassword(password, user.getPassword());

            // If the stored value is legacy plaintext, upgrade it in-place after successful auth.
            if (passwordOk && !PasswordUtils.isHashedPassword(user.getPassword())) {
                String upgraded = PasswordUtils.hashPassword(password);
                AppDatabase.getInstance(this).userDao().updatePassword(email, upgraded);
                user.setPassword(upgraded);
            }
        }

        if (user != null && passwordOk) {
            // SUCCESS – save login state
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("isLoggedIn", true)
                    .putString("userEmail", email)
                    .putBoolean("isAdmin", user.isAdmin())
                    .apply();

            // Show different Toast based on isAdmin
            String toastMessage = user.isAdmin() ? "Admin login successful!" : "User login successful!";
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Wrong email or password", Toast.LENGTH_SHORT).show();
        }
    }
}