
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;

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

    private Button btnAdicionar;
    private ImageButton btnOpcoes;

    private boolean observadoresConfigurados = false;

    private final Locale brasil = new Locale("pt", "BR");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

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
                    Color.DKGRAY
            );
            return;
        }

        DateFormat formatoData = DateFormat.getDateInstance(
                DateFormat.SHORT,
                brasil
        );

        for (Movimentacao movimentacao : movimentacoes) {

            boolean receita =
                    "RECEITA".equals(movimentacao.getTipo());

            String sinal = receita ? "+ " : "- ";

            String valor = formatarMoeda(
                    movimentacao.getValorCentavos()
            );

            String data = formatoData.format(
                    new Date(movimentacao.getData())
            );

            String texto =
                    movimentacao.getDescricao()
                            + "\n"
                            + sinal + valor
                            + "  •  " + data;

            adicionarLinha(
                    texto,
                    receita
                            ? Color.rgb(22, 120, 75)
                            : Color.rgb(180, 50, 50)
            );
        }
    }

    private void adicionarLinha(String texto, int cor) {

        TextView linha = new TextView(this);

        linha.setText(texto);
        linha.setTextSize(17);
        linha.setTextColor(cor);
        linha.setPadding(16, 22, 16, 22);

        listaMovimentacoes.addView(linha);

        View separador = new View(this);

        separador.setBackgroundColor(
                Color.rgb(220, 225, 232)
        );

        listaMovimentacoes.addView(
                separador,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                )
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
}
