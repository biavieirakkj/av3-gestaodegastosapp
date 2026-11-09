
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

import android.app.DatePickerDialog;

import com.example.controlefinanceiro.data.entity.Movimentacao;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

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
    private int movimentacaoIdEmEdicao = -1;

    private Movimentacao movimentacaoOriginal;

    private Integer categoriaPendente;

    private Button btnDataMovimentacao;

    private final Calendar dataEdicao =
            Calendar.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_movimentacoes);

        movimentacaoIdEmEdicao = getIntent().getIntExtra(
                "movimentacao_id",
                -1
        );

        btnDataMovimentacao = findViewById(
                R.id.btnDataMovimentacao
        );

        movimentacaoIdEmEdicao = getIntent().getIntExtra(
                "movimentacao_id",
                -1
        );

        btnDataMovimentacao = findViewById(
                R.id.btnDataMovimentacao
        );


        spinnerCategoria = findViewById(R.id.spinnerCategoria);

        edtDescricao =
                findViewById(R.id.edtDescricaoMovimentacao);

        edtValor =
                findViewById(R.id.edtValorMovimentacao);

        grupoTipo =
                findViewById(R.id.grupoTipoMovimentacao);

        btnSalvar =
                findViewById(R.id.btnSalvarMovimentacao);
        if (movimentacaoIdEmEdicao > 0) {
            btnSalvar.setText("Salvar alterações");
        }

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

        btnDataMovimentacao.setOnClickListener(
                v -> escolherDataEdicao()
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

                                        btnSalvar.setEnabled(
                                                movimentacaoIdEmEdicao <= 0
                                        );

                                        carregarCategorias(usuario.getId());

                                        if (movimentacaoIdEmEdicao > 0) {
                                            carregarMovimentacaoParaEdicao();
                                        }

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


        if (movimentacaoIdEmEdicao > 0) {

            if (movimentacaoOriginal == null) {
                setProcessando(false);

                Toast.makeText(
                        this,
                        "Movimentação ainda não carregada.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Movimentacao editada = new Movimentacao(
                    movimentacaoOriginal.getUsuarioId(),
                    categoriaId,
                    descricao,
                    tipo,
                    valorCentavos,
                    dataEdicao.getTimeInMillis()
            );

            editada.setId(
                    movimentacaoOriginal.getId()
            );

            financeiroViewModel.atualizarMovimentacao(
                    editada,
                    new FinanceiroRepository.Callback<Void>() {

                        @Override
                        public void onSucesso(Void resultado) {

                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            Toast.makeText(
                                    MovimentacoesActivity.this,
                                    "Movimentação atualizada!",
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
                                            : "Erro ao atualizar.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

            return;
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
                        if (movimentacaoIdEmEdicao > 0
                                && movimentacaoOriginal != null) {
                            selecionarCategoriaPendente();
                    }
                });
    }

    private void carregarMovimentacaoParaEdicao() {

        financeiroViewModel.buscarMovimentacao(
                movimentacaoIdEmEdicao,
                new FinanceiroRepository.Callback<Movimentacao>() {

                    @Override
                    public void onSucesso(Movimentacao movimentacao) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        movimentacaoOriginal = movimentacao;

                        edtDescricao.setText(
                                movimentacao.getDescricao()
                        );

                        String valor = BigDecimal.valueOf(
                                movimentacao.getValorCentavos(),
                                2
                        ).toPlainString().replace('.', ',');

                        edtValor.setText(valor);

                        grupoTipo.check(
                                "RECEITA".equals(
                                        movimentacao.getTipo()
                                )
                                        ? R.id.radioReceita
                                        : R.id.radioDespesa
                        );

                        categoriaPendente =
                                movimentacao.getCategoriaId();

                        selecionarCategoriaPendente();

                        dataEdicao.setTimeInMillis(
                                movimentacao.getData()
                        );

                        btnDataMovimentacao.setVisibility(
                                View.VISIBLE
                        );

                        atualizarTextoData();

                        btnSalvar.setEnabled(true);
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Toast.makeText(
                                MovimentacoesActivity.this,
                                erro.getMessage() != null
                                        ? erro.getMessage()
                                        : "Erro ao carregar movimentação.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }
                }
        );
    }

    private void selecionarCategoriaPendente() {

        if (categoriaPendente == null) {
            spinnerCategoria.setSelection(0);
            return;
        }

        for (int i = 0; i < categorias.size(); i++) {

            if (categorias.get(i).getId()
                    == categoriaPendente) {

                spinnerCategoria.setSelection(i + 1);
                return;
            }
        }
    }

    private void escolherDataEdicao() {

        new DatePickerDialog(
                this,
                (view, ano, mes, dia) -> {

                    dataEdicao.set(Calendar.YEAR, ano);
                    dataEdicao.set(Calendar.MONTH, mes);
                    dataEdicao.set(Calendar.DAY_OF_MONTH, dia);

                    atualizarTextoData();
                },
                dataEdicao.get(Calendar.YEAR),
                dataEdicao.get(Calendar.MONTH),
                dataEdicao.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void atualizarTextoData() {

        SimpleDateFormat formato = new SimpleDateFormat(
                "dd/MM/yyyy",
                new Locale("pt", "BR")
        );

        btnDataMovimentacao.setText(
                "Data: " + formato.format(
                        dataEdicao.getTime()
                )
        );
    }

}
