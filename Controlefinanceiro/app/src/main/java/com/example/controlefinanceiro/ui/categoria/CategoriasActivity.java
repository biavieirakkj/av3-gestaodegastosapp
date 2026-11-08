
package com.example.controlefinanceiro.ui.categoria;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.controlefinanceiro.R;
import com.example.controlefinanceiro.data.entity.Categoria;
import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.ui.auth.LoginActivity;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;
import com.example.controlefinanceiro.viewmodel.FinanceiroViewModel;

import java.util.List;

public class CategoriasActivity extends AppCompatActivity {

    private EditText edtNome;
    private Button btnSalvar;
    private ProgressBar progresso;
    private LinearLayout lista;

    private AuthViewModel authViewModel;
    private FinanceiroViewModel financeiroViewModel;

    private boolean autenticado = false;
    private boolean salvando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_categorias);

        edtNome = findViewById(R.id.edtNomeCategoria);
        btnSalvar = findViewById(R.id.btnSalvarCategoria);
        progresso = findViewById(R.id.progressoCategoria);
        lista = findViewById(R.id.listaCategorias);

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        financeiroViewModel = new ViewModelProvider(this)
                .get(FinanceiroViewModel.class);

        findViewById(R.id.btnVoltarCategorias)
                .setOnClickListener(v -> finish());

        btnSalvar.setOnClickListener(v -> salvar());

        btnSalvar.setEnabled(false);

        verificarSessao();
    }

    private void verificarSessao() {

        authViewModel.consultarSessao(
                new FinanceiroRepository.Callback<Sessao>() {

                    @Override
                    public void onSucesso(Sessao sessao) {

                        if (isFinishing() || isDestroyed()) return;

                        if (sessao == null) {
                            abrirLogin();
                            return;
                        }

                        authViewModel.buscarUsuario(
                                sessao.getUsuarioId(),
                                new FinanceiroRepository.Callback<Usuario>() {

                                    @Override
                                    public void onSucesso(Usuario usuario) {

                                        if (isFinishing() || isDestroyed()) return;

                                        if (usuario == null) {
                                            abrirLogin();
                                            return;
                                        }

                                        autenticado = true;
                                        btnSalvar.setEnabled(true);

                                        financeiroViewModel
                                                .listarCategorias(usuario.getId())
                                                .observe(
                                                        CategoriasActivity.this,
                                                        categorias ->
                                                                mostrarCategorias(categorias)
                                                );
                                    }

                                    @Override
                                    public void onErro(Exception erro) {
                                        if (!isFinishing()) finish();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onErro(Exception erro) {
                        if (!isFinishing()) finish();
                    }
                }
        );
    }

    private void salvar() {

        if (!autenticado || salvando) return;

        String nome = edtNome.getText().toString().trim();

        if (nome.isEmpty()) {
            edtNome.setError("Digite o nome da categoria.");
            edtNome.requestFocus();
            return;
        }

        salvando = true;
        btnSalvar.setEnabled(false);
        progresso.setVisibility(View.VISIBLE);

        financeiroViewModel.criarCategoria(
                nome,
                new FinanceiroRepository.Callback<Long>() {

                    @Override
                    public void onSucesso(Long id) {

                        if (isFinishing() || isDestroyed()) return;

                        finalizarSalvamento();
                        edtNome.setText("");

                        Toast.makeText(
                                CategoriasActivity.this,
                                "Categoria adicionada!",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) return;

                        finalizarSalvamento();

                        Toast.makeText(
                                CategoriasActivity.this,
                                erro.getMessage() != null
                                        ? erro.getMessage()
                                        : "Erro ao salvar categoria.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void finalizarSalvamento() {
        salvando = false;
        progresso.setVisibility(View.GONE);
        btnSalvar.setEnabled(autenticado);
    }

    private void mostrarCategorias(List<Categoria> categorias) {

        lista.removeAllViews();

        if (!autenticado) return;

        if (categorias == null || categorias.isEmpty()) {
            adicionarTexto("Nenhuma categoria cadastrada.");
            return;
        }

        for (Categoria categoria : categorias) {
            adicionarTexto("• " + categoria.getNome());
        }
    }

    private void adicionarTexto(String texto) {

        TextView item = new TextView(this);

        item.setText(texto);
        item.setTextSize(18);
        item.setTextColor(getColor(R.color.cor_texto));
        item.setPadding(12, 18, 12, 18);

        lista.addView(item);
    }

    private void abrirLogin() {

        autenticado = false;

        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
