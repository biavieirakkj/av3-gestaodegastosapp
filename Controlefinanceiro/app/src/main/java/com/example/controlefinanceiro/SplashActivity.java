package com.example.controlefinanceiro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.ui.auth.LoginActivity;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;

public class SplashActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private Button btnTentarNovamente;
    private TextView txtStatus;

    private boolean verificando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int modoSalvo = getSharedPreferences(
                "preferencias_aparencia",
                MODE_PRIVATE
        ).getInt(
                "modo_noturno",
                AppCompatDelegate.MODE_NIGHT_NO
        );

        AppCompatDelegate.setDefaultNightMode(modoSalvo);


        setContentView(R.layout.activity_splash);

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        btnTentarNovamente =
                findViewById(R.id.btnTentarNovamente);

        txtStatus = findViewById(R.id.txtStatusSplash);

        btnTentarNovamente.setOnClickListener(
                v -> verificarSessao()
        );

        verificarSessao();
    }

    private void verificarSessao() {

        if (verificando) {
            return;
        }

        verificando = true;

        btnTentarNovamente.setVisibility(View.GONE);
        txtStatus.setText("Verificando sua sessão...");

        authViewModel.consultarSessao(
                new FinanceiroRepository.Callback<Sessao>() {

                    @Override
                    public void onSucesso(Sessao sessao) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        verificando = false;

                        if (sessao != null) {
                            abrirTela(MainActivity.class);
                        } else {
                            abrirTela(LoginActivity.class);
                        }
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        verificando = false;

                        txtStatus.setText(
                                "Não foi possível verificar a sessão."
                        );

                        btnTentarNovamente.setVisibility(
                                View.VISIBLE
                        );

                        Toast.makeText(
                                SplashActivity.this,
                                "Verifique novamente.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void abrirTela(Class<?> destino) {

        Intent intent = new Intent(this, destino);

        startActivity(intent);

        finish();
    }
}
