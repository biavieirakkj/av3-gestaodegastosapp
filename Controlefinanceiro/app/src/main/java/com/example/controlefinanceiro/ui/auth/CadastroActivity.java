
package com.example.controlefinanceiro.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.controlefinanceiro.R;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;

public class CadastroActivity extends AppCompatActivity {

    private EditText edtNome;
    private EditText edtEmail;
    private EditText edtSenha;
    private EditText edtConfirmarSenha;

    private Button btnCadastrar;
    private Button btnVoltarLogin;
    private ProgressBar progresso;

    private AuthViewModel authViewModel;
    private boolean processando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        edtNome = findViewById(R.id.edtNomeCadastro);
        edtEmail = findViewById(R.id.edtEmailCadastro);
        edtSenha = findViewById(R.id.edtSenhaCadastro);
        edtConfirmarSenha =
                findViewById(R.id.edtConfirmarSenha);

        btnCadastrar = findViewById(R.id.btnCadastrar);
        btnVoltarLogin = findViewById(R.id.btnVoltarLogin);
        progresso = findViewById(R.id.progressoCadastro);

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        btnCadastrar.setOnClickListener(v -> cadastrar());

        btnVoltarLogin.setOnClickListener(v -> finish());
    }

    private void cadastrar() {

        if (processando) {
            return;
        }

        String nome = edtNome.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString();
        String confirmar = edtConfirmarSenha.getText()
                .toString();

        if (nome.isEmpty()) {
            edtNome.setError("Informe seu nome.");
            edtNome.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Informe um e-mail válido.");
            edtEmail.requestFocus();
            return;
        }

        if (senha.length() < 8) {
            edtSenha.setError(
                    "A senha deve ter pelo menos 8 caracteres."
            );
            edtSenha.requestFocus();
            return;
        }

        if (!senha.equals(confirmar)) {
            edtConfirmarSenha.setError(
                    "As senhas não coincidem."
            );
            edtConfirmarSenha.requestFocus();
            return;
        }

        setProcessando(true);

        authViewModel.cadastrar(
                nome,
                email,
                senha,
                new FinanceiroRepository.Callback<Long>() {

                    @Override
                    public void onSucesso(Long usuarioId) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setProcessando(false);

                        Toast.makeText(
                                CadastroActivity.this,
                                "Cadastro realizado! Faça seu login.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setProcessando(false);

                        Toast.makeText(
                                CadastroActivity.this,
                                erro.getMessage() != null
                                        ? erro.getMessage()
                                        : "Não foi possível cadastrar.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void setProcessando(boolean valor) {

        processando = valor;

        btnCadastrar.setEnabled(!valor);
        btnVoltarLogin.setEnabled(!valor);

        progresso.setVisibility(
                valor ? View.VISIBLE : View.GONE
        );
    }
}
