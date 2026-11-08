
package com.example.controlefinanceiro.ui.movimentacao;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.controlefinanceiro.data.entity.Categoria;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.controlefinanceiro.R;
import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.ui.auth.LoginActivity;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;
import com.example.controlefinanceiro.viewmodel.FinanceiroViewModel;

import java.math.BigDecimal;

public class MovimentacoesActivity extends AppCompatActivity {

    private EditText edtDescricao;
    private EditText edtValor;
    private RadioGroup grupoTipo;

    private Button btnSalvar;
    private Button btnVoltar;
    private ProgressBar progresso;

    private FinanceiroViewModel financeiroViewModel;
    private AuthViewModel authViewModel;

    private Spinner spinnerCategoria;

    private final List<Categoria> categorias =
            new ArrayList<>();

    private boolean processando = false;
    private boolean sessaoValida = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_movimentacoes);

        spinnerCategoria = findViewById(R.id.spinnerCategoria);

        edtDescricao =
                findViewById(R.id.edtDescricaoMovimentacao);

        edtValor =
                findViewById(R.id.edtValorMovimentacao);

        grupoTipo =
                findViewById(R.id.grupoTipoMovimentacao);

        btnSalvar =
                findViewById(R.id.btnSalvarMovimentacao);

        btnVoltar =
                findViewById(R.id.btnVoltarMovimentacoes);

        progresso =
                findViewById(R.id.progressoMovimentacao);

        financeiroViewModel = new ViewModelProvider(this)
                .get(FinanceiroViewModel.class);

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        btnSalvar.setEnabled(false);

        btnVoltar.setOnClickListener(v -> finish());

        btnSalvar.setOnClickListener(
                v -> salvarMovimentacao()
        );

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

                        authViewModel.buscarUsuario(
                                sessao.getUsuarioId(),
                                new FinanceiroRepository.Callback<
                                        com.example.controlefinanceiro
                                                .data.entity.Usuario>() {

                                    @Override
                                    public void onSucesso(
                                            com.example.controlefinanceiro
                                                    .data.entity.Usuario usuario
                                    ) {

                                        if (isFinishing() || isDestroyed()) {
                                            return;
                                        }

                                        if (usuario == null) {
                                            abrirLogin();
                                            return;
                                        }

                                        sessaoValida = true;
                                        btnSalvar.setEnabled(true);
                                        carregarCategorias(usuario.getId());
                                    }

                                    @Override
                                    public void onErro(Exception erro) {
                                        if (!isFinishing() && !isDestroyed()) {
                                            finish();
                                        }
                                    }
                                }
                        );
                    }

                    @Override
                    public void onErro(Exception erro) {
                        if (!isFinishing() && !isDestroyed()) {
                            finish();
                        }
                    }
                }
        );
    }

    private void salvarMovimentacao() {

        if (processando || !sessaoValida) {
            return;
        }

        String descricao =
                edtDescricao.getText().toString().trim();

        String valorTexto =
                edtValor.getText().toString().trim();

        if (descricao.isEmpty()) {
            edtDescricao.setError("Informe uma descrição.");
            edtDescricao.requestFocus();
            return;
        }

        String tipo =
                grupoTipo.getCheckedRadioButtonId()
                        == R.id.radioReceita
                        ? "RECEITA"
                        : "DESPESA";

        long valorCentavos;

        try {

            if (!valorTexto.matches(
                    "\\d{1,9}([,.]\\d{1,2})?"
            )) {
                throw new IllegalArgumentException();
            }

            valorCentavos = new BigDecimal(
                    valorTexto.replace(',', '.')
            ).movePointRight(2).longValueExact();

            if (valorCentavos <= 0) {
                throw new IllegalArgumentException();
            }

        } catch (Exception erro) {

            edtValor.setError(
                    "Informe um valor válido, como 85,90."
            );

            edtValor.requestFocus();
            return;
        }

        setProcessando(true);

        Integer categoriaId = null;

        int posicao = spinnerCategoria.getSelectedItemPosition();

        if (posicao > 0 && posicao <= categorias.size()) {
            categoriaId = categorias.get(posicao - 1).getId();
        }

        financeiroViewModel.registrarMovimentacao(
                descricao,
                tipo,
                valorCentavos,
                categoriaId,
                new FinanceiroRepository.Callback<Long>() {

                    @Override
                    public void onSucesso(Long id) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Toast.makeText(
                                MovimentacoesActivity.this,
                                "Movimentação salva!",
                                Toast.LENGTH_SHORT
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
                                MovimentacoesActivity.this,
                                erro.getMessage() != null
                                        ? erro.getMessage()
                                        : "Erro ao salvar.",
                                Toast.LENGTH_LONG
                        ).show();

                        if (erro instanceof IllegalStateException) {
                            abrirLogin();
                        }
                    }
                }
        );
    }

    private void setProcessando(boolean valor) {

        processando = valor;

        btnSalvar.setEnabled(!valor && sessaoValida);
        btnVoltar.setEnabled(!valor);

        progresso.setVisibility(
                valor ? View.VISIBLE : View.GONE
        );
    }

    private void abrirLogin() {

        sessaoValida = false;

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


    private void carregarCategorias(int usuarioId) {

        financeiroViewModel.listarCategorias(usuarioId)
                .observe(this, lista -> {

                    categorias.clear();

                    List<String> nomes = new ArrayList<>();

                    nomes.add("Sem categoria");

                    if (lista != null) {

                        categorias.addAll(lista);

                        for (Categoria categoria : categorias) {
                            nomes.add(categoria.getNome());
                        }
                    }

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                    this,
                                    android.R.layout.simple_spinner_item,
                                    nomes
                            );

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerCategoria.setAdapter(adapter);
                });
    }

}
