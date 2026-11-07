
package com.example.controlefinanceiro.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.controlefinanceiro.MainActivity;
import com.example.controlefinanceiro.R;
import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtSenha;
    private Button btnEntrar;
    private Button btnIrCadastro;
    private ProgressBar progresso;

    private AuthViewModel authViewModel;
    private boolean processando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmailLogin);
        edtSenha = findViewById(R.id.edtSenhaLogin);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnIrCadastro = findViewById(R.id.btnIrCadastro);
        progresso = findViewById(R.id.progressoLogin);

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        btnEntrar.setOnClickListener(v -> entrar());

        btnIrCadastro.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    CadastroActivity.class
            );
            startActivity(intent);
        });
    }

    private void entrar() {

        if (processando) {
            return;
        }

        String email = edtEmail.getText()
                .toString().trim();

        String senha = edtSenha.getText()
                .toString();

        if (email.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            edtEmail.setError("Informe um e-mail válido.");
            edtEmail.requestFocus();
            return;
        }

        if (senha.isEmpty()) {
            edtSenha.setError("Informe sua senha.");
            edtSenha.requestFocus();
            return;
        }

        setProcessando(true);

        authViewModel.login(
                email,
                senha,
                new FinanceiroRepository.Callback<Usuario>() {

                    @Override
                    public void onSucesso(Usuario usuario) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setProcessando(false);

                        Intent intent = new Intent(
                                LoginActivity.this,
                                MainActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setProcessando(false);

                        Toast.makeText(
                                LoginActivity.this,
                                erro.getMessage() != null
                                        ? erro.getMessage()
                                        : "Não foi possível entrar.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void setProcessando(boolean valor) {

        processando = valor;

        btnEntrar.setEnabled(!valor);
        btnIrCadastro.setEnabled(!valor);

        progresso.setVisibility(
                valor ? View.VISIBLE : View.GONE
        );
    }
}
