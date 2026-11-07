
package com.example.controlefinanceiro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.ui.auth.LoginActivity;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;
import com.example.controlefinanceiro.ui.perfil.PerfilActivity;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Não mostramos dados privados antes de conferir a sessão.
        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        verificarSessao();
    }

    private void verificarSessao() {

        authViewModel.consultarSessao(
                new FinanceiroRepository.Callback<Sessao>() {

                    @Override
                    public void onSucesso(Sessao sessao) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (sessao == null) {
                            abrirLogin();
                            return;
                        }

                        carregarUsuario(sessao.getUsuarioId());
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Toast.makeText(
                                MainActivity.this,
                                "Erro ao verificar a sessão.",
                                Toast.LENGTH_LONG
                        ).show();

                        // Não liberar a tela restrita se a verificação falhar.
                        finish();
                    }
                }
        );
    }

    private void carregarUsuario(int usuarioId) {

        authViewModel.buscarUsuario(
                usuarioId,
                new FinanceiroRepository.Callback<Usuario>() {

                    @Override
                    public void onSucesso(Usuario usuario) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (usuario == null) {
                            abrirLogin();
                            return;
                        }

                        mostrarTelaPrincipal(usuario);
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Toast.makeText(
                                MainActivity.this,
                                "Erro ao carregar o usuário.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }
                }
        );
    }

    private void mostrarTelaPrincipal(Usuario usuario) {

        setContentView(R.layout.activity_main);

        TextView txtBoasVindas =
                findViewById(R.id.txtBoasVindas);

        Button btnSair =
                findViewById(R.id.btnSair);

        Button btnMeuPerfil =
                findViewById(R.id.btnMeuPerfil);

        btnMeuPerfil.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    PerfilActivity.class
            );

            startActivity(intent);
        });

        txtBoasVindas.setText(
                "Olá, " + usuario.getNome() + "!"
        );

        btnSair.setOnClickListener(v -> {

            btnSair.setEnabled(false);

            authViewModel.logout(
                    new FinanceiroRepository.Callback<Void>() {

                        @Override
                        public void onSucesso(Void resultado) {

                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            abrirLogin();
                        }

                        @Override
                        public void onErro(Exception erro) {

                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            btnSair.setEnabled(true);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Não foi possível sair.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
        });
    }

    private void abrirLogin() {

        Intent intent = new Intent(
                MainActivity.this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (authViewModel == null) {
            return;
        }

        TextView txtBoasVindas =
                findViewById(R.id.txtBoasVindas);

        if (txtBoasVindas == null) {
            return;
        }

        authViewModel.consultarSessao(
                new FinanceiroRepository.Callback<Sessao>() {

                    @Override
                    public void onSucesso(Sessao sessao) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (sessao == null) {
                            abrirLogin();
                            return;
                        }

                        authViewModel.buscarUsuario(
                                sessao.getUsuarioId(),
                                new FinanceiroRepository.Callback<Usuario>() {

                                    @Override
                                    public void onSucesso(Usuario usuario) {

                                        if (isFinishing() || isDestroyed()) {
                                            return;
                                        }

                                        if (usuario == null) {
                                            abrirLogin();
                                            return;
                                        }

                                        TextView texto =
                                                findViewById(
                                                        R.id.txtBoasVindas
                                                );

                                        if (texto != null) {
                                            texto.setText(
                                                    "Olá, " +
                                                            usuario.getNome() +
                                                            "!"
                                            );
                                        }
                                    }

                                    @Override
                                    public void onErro(Exception erro) {

                                        if (!isFinishing() && !isDestroyed()) {
                                            abrirLogin();
                                        }
                                    }
                                }
                        );
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (!isFinishing() && !isDestroyed()) {
                            abrirLogin();
                        }
                    }
                }
        );
    }

}
