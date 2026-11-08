package com.example.controlefinanceiro.ui.relatorio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.controlefinanceiro.R;
import com.example.controlefinanceiro.data.entity.Categoria;
import com.example.controlefinanceiro.data.entity.Movimentacao;
import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.ui.auth.LoginActivity;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;
import com.example.controlefinanceiro.viewmodel.FinanceiroViewModel;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class RelatoriosActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private FinanceiroViewModel financeiroViewModel;

    private TextView txtMes;
    private TextView txtReceitas;
    private TextView txtDespesas;
    private TextView txtSaldo;
    private LinearLayout listaCategorias;

    private final Calendar mesSelecionado =
            Calendar.getInstance();

    private final Locale brasil =
            new Locale("pt", "BR");

    private final Map<Integer, String> nomesCategorias =
            new HashMap<>();

    private LiveData<List<Movimentacao>> observacaoAtual;

    private List<Movimentacao> movimentacoesAtuais;

    private int usuarioId = -1;
    private boolean sessaoValida = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_relatorios);

        txtMes = findViewById(R.id.txtMesRelatorio);
        txtReceitas =
                findViewById(R.id.txtReceitasRelatorio);
        txtDespesas =
                findViewById(R.id.txtDespesasRelatorio);
        txtSaldo =
                findViewById(R.id.txtSaldoRelatorio);

        listaCategorias =
                findViewById(R.id.listaCategoriasRelatorio);

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        financeiroViewModel = new ViewModelProvider(this)
                .get(FinanceiroViewModel.class);

        mesSelecionado.set(Calendar.DAY_OF_MONTH, 1);

        findViewById(R.id.btnVoltarRelatorios)
                .setOnClickListener(v -> finish());

        findViewById(R.id.btnMesAnterior)
                .setOnClickListener(v -> mudarMes(-1));

        findViewById(R.id.btnProximoMes)
                .setOnClickListener(v -> mudarMes(1));

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

                                        usuarioId = usuario.getId();
                                        sessaoValida = true;

                                        observarCategorias();
                                        atualizarMes();
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
                        if (!isFinishing()) {
                            Toast.makeText(
                                    RelatoriosActivity.this,
                                    "Erro ao verificar sessão.",
                                    Toast.LENGTH_LONG
                            ).show();
                            finish();
                        }
                    }
                }
        );
    }

    private void observarCategorias() {

        financeiroViewModel.listarCategorias(usuarioId)
                .observe(this, categorias -> {

                    nomesCategorias.clear();

                    if (categorias != null) {

                        for (Categoria categoria : categorias) {
                            nomesCategorias.put(
                                    categoria.getId(),
                                    categoria.getNome()
                            );
                        }
                    }

                    if (movimentacoesAtuais != null) {
                        mostrarResumo(movimentacoesAtuais);
                    }
                });
    }

    private void mudarMes(int quantidade) {

        if (!sessaoValida) return;

        mesSelecionado.add(
                Calendar.MONTH,
                quantidade
        );

        atualizarMes();
    }

    private void atualizarMes() {

        if (!sessaoValida) return;

        SimpleDateFormat formato = new SimpleDateFormat(
                "MMMM 'de' yyyy",
                brasil
        );

        txtMes.setText(
                formato.format(mesSelecionado.getTime())
        );

        Calendar inicio = (Calendar) mesSelecionado.clone();

        inicio.set(Calendar.DAY_OF_MONTH, 1);
        inicio.set(Calendar.HOUR_OF_DAY, 0);
        inicio.set(Calendar.MINUTE, 0);
        inicio.set(Calendar.SECOND, 0);
        inicio.set(Calendar.MILLISECOND, 0);

        Calendar fim = (Calendar) inicio.clone();
        fim.add(Calendar.MONTH, 1);

        if (observacaoAtual != null) {
            observacaoAtual.removeObservers(this);
        }

        movimentacoesAtuais = null;

        observacaoAtual = financeiroViewModel.listarPorPeriodo(
                usuarioId,
                inicio.getTimeInMillis(),
                fim.getTimeInMillis()
        );

        observacaoAtual.observe(
                this,
                movimentacoes -> {

                    movimentacoesAtuais = movimentacoes;
                    mostrarResumo(movimentacoes);
                }
        );
    }

    private void mostrarResumo(
            List<Movimentacao> movimentacoes
    ) {

        if (!sessaoValida) return;

        long receitas = 0;
        long despesas = 0;

        Map<String, Long> gastosPorCategoria =
                new TreeMap<>();

        if (movimentacoes != null) {

            for (Movimentacao m : movimentacoes) {

                if ("RECEITA".equals(m.getTipo())) {

                    receitas += m.getValorCentavos();

                } else if ("DESPESA".equals(m.getTipo())) {

                    despesas += m.getValorCentavos();

                    String nome = "Sem categoria";

                    if (m.getCategoriaId() != null) {
                        nome = nomesCategorias.getOrDefault(
                                m.getCategoriaId(),
                                "Categoria removida"
                        );
                    }

                    long acumulado =
                            gastosPorCategoria.getOrDefault(
                                    nome,
                                    0L
                            );

                    gastosPorCategoria.put(
                            nome,
                            acumulado + m.getValorCentavos()
                    );
                }
            }
        }

        txtReceitas.setText(
                "Receitas: " + moeda(receitas)
        );

        txtDespesas.setText(
                "Despesas: " + moeda(despesas)
        );

        txtSaldo.setText(
                "Saldo: " + moeda(receitas - despesas)
        );

        listaCategorias.removeAllViews();

        if (gastosPorCategoria.isEmpty()) {

            adicionarLinha(
                    "Nenhuma despesa neste mês."
            );

            return;
        }

        for (Map.Entry<String, Long> item
                : gastosPorCategoria.entrySet()) {

            adicionarLinha(
                    item.getKey()
                            + ": "
                            + moeda(item.getValue())
            );
        }
    }

    private void adicionarLinha(String texto) {

        TextView linha = new TextView(this);

        linha.setText(texto);
        linha.setTextSize(17);
        linha.setTextColor(
                getColor(R.color.cor_texto)
        );
        linha.setPadding(12, 18, 12, 18);

        listaCategorias.addView(linha);
    }

    private String moeda(long centavos) {

        return NumberFormat
                .getCurrencyInstance(brasil)
                .format(
                        BigDecimal.valueOf(centavos, 2)
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
}
