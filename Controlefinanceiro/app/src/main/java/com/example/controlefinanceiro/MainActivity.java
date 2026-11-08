
package com.example.controlefinanceiro;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import com.example.controlefinanceiro.ui.categoria.CategoriasActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import com.example.controlefinanceiro.notificacao.AgendadorLembretes;
import com.example.controlefinanceiro.ui.relatorio.RelatoriosActivity;
import com.example.controlefinanceiro.data.entity.Movimentacao;
import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.ui.auth.LoginActivity;
import com.example.controlefinanceiro.ui.movimentacao.MovimentacoesActivity;
import com.example.controlefinanceiro.ui.perfil.PerfilActivity;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;
import com.example.controlefinanceiro.viewmodel.FinanceiroViewModel;
import com.example.controlefinanceiro.ui.lembrete.LembretesActivity;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private FinanceiroViewModel financeiroViewModel;

    private LinearLayout conteudoPrincipal;
    private LinearLayout listaMovimentacoes;

    private TextView txtBoasVindas;
    private TextView txtSaldo;
    private TextView txtReceitas;
    private TextView txtDespesas;

    private LinearLayout btnAdicionar;
    private ImageButton btnOpcoes;

    private boolean observadoresConfigurados = false;

    private final Locale brasil = new Locale("pt", "BR");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        LinearLayout btnRelatorios = findViewById(R.id.btnRelatorios);

        btnRelatorios.setOnClickListener(v -> {
            startActivity(new Intent(
                    MainActivity.this,
                    RelatoriosActivity.class
            ));
        });


        LinearLayout btnCategorias = findViewById(R.id.btnCategorias);

        btnCategorias.setOnClickListener(v -> {
            startActivity(new Intent(
                    MainActivity.this,
                    CategoriasActivity.class
            ));
        });


        Button btnMeusLembretes =
                findViewById(R.id.btnMeusLembretes);

        btnMeusLembretes.setOnClickListener(v -> {

            startActivity(new Intent(
                    MainActivity.this,
                    LembretesActivity.class
            ));
        });

        conteudoPrincipal =
                findViewById(R.id.conteudoPrincipal);

        listaMovimentacoes =
                findViewById(R.id.listaMovimentacoesMain);

        txtBoasVindas =
                findViewById(R.id.txtBoasVindas);

        txtSaldo = findViewById(R.id.txtSaldo);
        txtReceitas = findViewById(R.id.txtReceitas);
        txtDespesas = findViewById(R.id.txtDespesas);

        btnAdicionar =
                findViewById(R.id.btnAdicionarMovimentacao);

        btnOpcoes = findViewById(R.id.btnOpcoes);

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        financeiroViewModel = new ViewModelProvider(this)
                .get(FinanceiroViewModel.class);

        conteudoPrincipal.setVisibility(View.GONE);

        btnAdicionar.setOnClickListener(v -> {
            startActivity(new Intent(
                    this,
                    MovimentacoesActivity.class
            ));
        });

        btnOpcoes.setOnClickListener(
                this::mostrarMenu
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Confere novamente a sessão ao voltar do perfil.
        conteudoPrincipal.setVisibility(View.GONE);
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
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Erro ao verificar a sessão.",
                                    Toast.LENGTH_LONG
                            ).show();
                            finish();
                        }
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

                        AgendadorLembretes.reagendarTodos(
                                MainActivity.this
                        );

                        txtBoasVindas.setText(
                                "Olá, " + usuario.getNome() + "!"
                        );

                        if (!observadoresConfigurados) {
                            configurarObservadores(usuario.getId());
                            observadoresConfigurados = true;
                        }

                        conteudoPrincipal.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onErro(Exception erro) {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Erro ao carregar o usuário.",
                                    Toast.LENGTH_LONG
                            ).show();
                            finish();
                        }
                    }
                }
        );
    }

    private void configurarObservadores(int usuarioId) {

        financeiroViewModel.consultarSaldo(usuarioId)
                .observe(this, valor -> {
                    txtSaldo.setText(formatarMoeda(valor));
                });

        financeiroViewModel.totalReceitas(usuarioId)
                .observe(this, valor -> {
                    txtReceitas.setText(
                            "Receitas: " + formatarMoeda(valor)
                    );
                });

        financeiroViewModel.totalDespesas(usuarioId)
                .observe(this, valor -> {
                    txtDespesas.setText(
                            "Despesas: " + formatarMoeda(valor)
                    );
                });

        financeiroViewModel.listarMovimentacoes(usuarioId)
                .observe(this, this::mostrarHistorico);
    }


    private void mostrarHistorico(
            List<Movimentacao> movimentacoes
    ) {

        listaMovimentacoes.removeAllViews();

        if (movimentacoes == null ||
                movimentacoes.isEmpty()) {

            adicionarLinha(
                    "Nenhuma movimentação cadastrada.",
                    getColor(R.color.cor_texto)
            );

            return;
        }

        DateFormat formatoData =
                DateFormat.getDateInstance(
                        DateFormat.SHORT,
                        brasil
                );

        // ====================================
        // ENTRADAS DE DINHEIRO
        // ====================================

        adicionarTituloHistorico(
                "↓ ENTRADAS",
                getColor(R.color.cor_receita)
        );

        boolean temReceitas = false;

        for (Movimentacao movimentacao : movimentacoes) {

            if ("RECEITA".equals(movimentacao.getTipo())) {

                temReceitas = true;

                String valor = formatarMoeda(
                        movimentacao.getValorCentavos()
                );

                String data = formatoData.format(
                        new Date(movimentacao.getData())
                );

                String texto =
                        movimentacao.getDescricao()
                                + "\n+ " + valor
                                + "  •  " + data;

                adicionarLinha(
                        texto,
                        getColor(R.color.cor_receita)
                );
            }
        }

        if (!temReceitas) {

            adicionarLinha(
                    "Nenhuma entrada cadastrada.",
                    getColor(R.color.cor_texto)
            );
        }

        // ====================================
        // SAÍDAS DE DINHEIRO
        // ====================================

        adicionarTituloHistorico(
                "↑ SAÍDAS",
                getColor(R.color.cor_despesa)
        );

        boolean temDespesas = false;

        for (Movimentacao movimentacao : movimentacoes) {

            if ("DESPESA".equals(movimentacao.getTipo())) {

                temDespesas = true;

                String valor = formatarMoeda(
                        movimentacao.getValorCentavos()
                );

                String data = formatoData.format(
                        new Date(movimentacao.getData())
                );

                String texto =
                        movimentacao.getDescricao()
                                + "\n- " + valor
                                + "  •  " + data;

                adicionarLinha(
                        texto,
                        getColor(R.color.cor_despesa)
                );
            }
        }

        if (!temDespesas) {

            adicionarLinha(
                    "Nenhuma saída cadastrada.",
                    getColor(R.color.cor_texto)
            );
        }
    }



    private void adicionarTituloHistorico(
            String titulo,
            int cor
    ) {

        TextView cabecalho = new TextView(this);

        cabecalho.setText(titulo);

        cabecalho.setTextSize(17);

        cabecalho.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        cabecalho.setTextColor(cor);

        cabecalho.setPadding(
                converterDp(4),
                converterDp(20),
                converterDp(4),
                converterDp(12)
        );

        listaMovimentacoes.addView(cabecalho);
    }



    private void adicionarLinha(String texto, int cor) {

        // Cria o card branco.
        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);

        int espaco = converterDp(16);

        card.setPadding(
                espaco,
                espaco,
                espaco,
                espaco
        );

        // Fundo branco com cantos arredondados.
        GradientDrawable fundo = new GradientDrawable();

        fundo.setColor(Color.WHITE);

        fundo.setCornerRadius(converterDp(12));

        card.setBackground(fundo);

        // Espaçamento entre os cards.
        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        parametros.setMargins(
                0,
                converterDp(4),
                0,
                converterDp(10)
        );

        // Separa a descrição do valor e da data.
        String[] partes = texto.split("\n", 2);

        TextView descricao = new TextView(this);

        descricao.setText(partes[0]);

        descricao.setTextSize(16);

        descricao.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        descricao.setTextColor(
                getColor(R.color.cor_texto)
        );

        card.addView(descricao);

        // Exibe valor e data com a cor da movimentação.
        if (partes.length > 1) {

            TextView detalhes = new TextView(this);

            detalhes.setText(partes[1]);

            detalhes.setTextSize(17);

            detalhes.setTextColor(cor);

            detalhes.setPadding(
                    0,
                    converterDp(8),
                    0,
                    0
            );

            card.addView(detalhes);
        }

        listaMovimentacoes.addView(
                card,
                parametros
        );
    }


    private String formatarMoeda(Long centavos) {

        long valor = centavos != null ? centavos : 0L;

        return NumberFormat.getCurrencyInstance(brasil)
                .format(BigDecimal.valueOf(valor, 2));
    }

    private void mostrarMenu(View ancora) {

        PopupMenu menu = new PopupMenu(this, ancora);

        menu.getMenu().add(0, 1, 0, "Meu perfil");
        menu.getMenu().add(0, 2, 1, "Sair");

        menu.setOnMenuItemClickListener(item -> {

            if (item.getItemId() == 1) {

                startActivity(new Intent(
                        this,
                        PerfilActivity.class
                ));

                return true;
            }

            if (item.getItemId() == 2) {

                encerrarSessao();
                return true;
            }

            return false;
        });

        menu.show();
    }

    private void encerrarSessao() {

        btnOpcoes.setEnabled(false);
        btnAdicionar.setEnabled(false);

        authViewModel.logout(
                new FinanceiroRepository.Callback<Void>() {

                    @Override
                    public void onSucesso(Void resultado) {

                        if (!isFinishing() && !isDestroyed()) {
                            abrirLogin();
                        }
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        btnOpcoes.setEnabled(true);
                        btnAdicionar.setEnabled(true);

                        Toast.makeText(
                                MainActivity.this,
                                "Não foi possível sair.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void abrirLogin() {

        conteudoPrincipal.setVisibility(View.GONE);

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


    private int converterDp(int valor) {

        return (int) (
                valor * getResources()
                        .getDisplayMetrics().density
                        + 0.5f
        );
    }

}
