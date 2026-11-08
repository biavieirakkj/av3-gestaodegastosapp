package com.example.controlefinanceiro.ui.lembrete;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.example.controlefinanceiro.notificacao.AgendadorLembretes;
import com.example.controlefinanceiro.notificacao.LembreteReceiver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.controlefinanceiro.R;
import com.example.controlefinanceiro.data.entity.Lembrete;
import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.ui.auth.LoginActivity;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;
import com.example.controlefinanceiro.viewmodel.FinanceiroViewModel;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class LembretesActivity extends AppCompatActivity {

    private EditText edtDescricao, edtValor;
    private Button btnData, btnHora, btnSalvar;
    private ProgressBar progresso;
    private RadioGroup grupoFrequencia;
    private CheckBox checkAvisarAntes;
    private LinearLayout lista;

    private AuthViewModel authViewModel;
    private FinanceiroViewModel financeiroViewModel;

    private final Calendar dataEscolhida =
            Calendar.getInstance();

    private final Locale brasil =
            new Locale("pt", "BR");

    private boolean processando = false;
    private boolean sessaoValida = false;


    private final ActivityResultLauncher<String>
            permissaoNotificacao = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            permitida -> {

                if (!permitida) {

                    Toast.makeText(
                            this,
                            "Os lembretes serão salvos, mas " +
                                    "as notificações estão desativadas.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lembretes);

        configurarNotificacoes();

        edtDescricao = findViewById(
                R.id.edtDescricaoLembrete
        );

        edtValor = findViewById(
                R.id.edtValorLembrete
        );

        btnData = findViewById(R.id.btnDataLembrete);
        btnHora = findViewById(R.id.btnHoraLembrete);
        btnSalvar = findViewById(R.id.btnSalvarLembrete);

        progresso = findViewById(R.id.progressoLembrete);
        lista = findViewById(R.id.listaLembretes);

        grupoFrequencia = findViewById(
                R.id.grupoFrequencia
        );

        checkAvisarAntes = findViewById(
                R.id.checkAvisarAntes
        );

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        financeiroViewModel = new ViewModelProvider(this)
                .get(FinanceiroViewModel.class);

        // Data inicial: amanhã, às 9h.
        dataEscolhida.add(Calendar.DAY_OF_MONTH, 1);
        dataEscolhida.set(Calendar.HOUR_OF_DAY, 9);
        dataEscolhida.set(Calendar.MINUTE, 0);
        dataEscolhida.set(Calendar.SECOND, 0);
        dataEscolhida.set(Calendar.MILLISECOND, 0);

        atualizarBotoesDataHora();

        btnSalvar.setEnabled(false);

        findViewById(R.id.btnVoltarLembretes)
                .setOnClickListener(v -> finish());

        btnData.setOnClickListener(
                v -> escolherData()
        );

        btnHora.setOnClickListener(
                v -> escolherHora()
        );

        btnSalvar.setOnClickListener(
                v -> salvarLembrete()
        );

        verificarSessao();
    }

    private void escolherData() {

        new DatePickerDialog(
                this,
                (view, ano, mes, dia) -> {

                    dataEscolhida.set(
                            Calendar.YEAR, ano
                    );

                    dataEscolhida.set(
                            Calendar.MONTH, mes
                    );

                    dataEscolhida.set(
                            Calendar.DAY_OF_MONTH, dia
                    );

                    atualizarBotoesDataHora();
                },
                dataEscolhida.get(Calendar.YEAR),
                dataEscolhida.get(Calendar.MONTH),
                dataEscolhida.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void escolherHora() {

        new TimePickerDialog(
                this,
                (view, hora, minuto) -> {

                    dataEscolhida.set(
                            Calendar.HOUR_OF_DAY, hora
                    );

                    dataEscolhida.set(
                            Calendar.MINUTE, minuto
                    );

                    atualizarBotoesDataHora();
                },
                dataEscolhida.get(Calendar.HOUR_OF_DAY),
                dataEscolhida.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void atualizarBotoesDataHora() {

        SimpleDateFormat formatoData =
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        brasil
                );

        SimpleDateFormat formatoHora =
                new SimpleDateFormat(
                        "HH:mm",
                        brasil
                );

        btnData.setText(
                "Vencimento: " +
                        formatoData.format(dataEscolhida.getTime())
        );

        btnHora.setText(
                "Horário do aviso: " +
                        formatoHora.format(dataEscolhida.getTime())
        );
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

                                        sessaoValida = true;
                                        btnSalvar.setEnabled(true);

                                        observarLembretes(usuario.getId());
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

    private void observarLembretes(int usuarioId) {

        financeiroViewModel.listarLembretes(usuarioId)
                .observe(this, this::mostrarLembretes);
    }

    private void salvarLembrete() {

        if (processando || !sessaoValida) {
            return;
        }

        String descricao = edtDescricao.getText()
                .toString().trim();

        String valorTexto = edtValor.getText()
                .toString().trim();

        if (descricao.isEmpty()) {
            edtDescricao.setError(
                    "Informe a descrição da conta."
            );
            return;
        }

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

        } catch (Exception erro) {

            edtValor.setError(
                    "Informe um valor válido, como 1200,00."
            );
            return;
        }

        boolean mensal =
                grupoFrequencia.getCheckedRadioButtonId()
                        == R.id.radioMensal;

        int diaVencimento =
                dataEscolhida.get(Calendar.DAY_OF_MONTH);

        int diasAntes =
                checkAvisarAntes.isChecked() ? 1 : 0;

        Calendar primeiroVencimento =
                (Calendar) dataEscolhida.clone();

        // Em um lembrete mensal, encontra o primeiro
        // vencimento cujo aviso ainda não passou.
        if (mensal) {

            while (primeiroVencimento.getTimeInMillis()
                    - diasAntes * 24L * 60L * 60L * 1000L
                    <= System.currentTimeMillis()) {

                primeiroVencimento.set(
                        Calendar.DAY_OF_MONTH, 1
                );

                primeiroVencimento.add(
                        Calendar.MONTH, 1
                );

                int ultimoDia = primeiroVencimento
                        .getActualMaximum(
                                Calendar.DAY_OF_MONTH
                        );

                primeiroVencimento.set(
                        Calendar.DAY_OF_MONTH,
                        Math.min(diaVencimento, ultimoDia)
                );
            }

        } else {

            long momentoAviso =
                    primeiroVencimento.getTimeInMillis()
                            - diasAntes * 24L * 60L * 60L * 1000L;

            if (momentoAviso <= System.currentTimeMillis()) {

                Toast.makeText(
                        this,
                        "Escolha uma data cujo aviso ainda não passou.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }
        }

        setProcessando(true);

        financeiroViewModel.registrarLembrete(
                descricao,
                valorCentavos,
                primeiroVencimento.getTimeInMillis(),
                mensal,
                diaVencimento,
                diasAntes,
                new FinanceiroRepository.Callback<Long>() {

                    @Override
                    public void onSucesso(Long id) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setProcessando(false);

                        AgendadorLembretes.reagendarTodos(
                                LembretesActivity.this
                        );

                        edtDescricao.setText("");
                        edtValor.setText("");

                        Toast.makeText(
                                LembretesActivity.this,
                                "Lembrete cadastrado!",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setProcessando(false);

                        Toast.makeText(
                                LembretesActivity.this,
                                erro.getMessage() != null
                                        ? erro.getMessage()
                                        : "Erro ao salvar lembrete.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void mostrarLembretes(List<Lembrete> lembretes) {

        lista.removeAllViews();

        if (!sessaoValida) {
            return;
        }

        if (lembretes == null || lembretes.isEmpty()) {
            adicionarLinha("Nenhum lembrete cadastrado.");
            return;
        }

        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "dd/MM/yyyy 'às' HH:mm",
                        brasil
                );

        NumberFormat moeda =
                NumberFormat.getCurrencyInstance(brasil);

        for (Lembrete lembrete : lembretes) {

            String frequencia = lembrete.isMensal()
                    ? "Todo mês, dia " +
                    lembrete.getDiaVencimento()
                    : "Uma única vez";

            String antecedencia = lembrete.getDiasAntes() == 1
                    ? "Aviso um dia antes"
                    : "Aviso no vencimento";

            String valor = moeda.format(
                    BigDecimal.valueOf(
                            lembrete.getValorCentavos(), 2
                    )
            );

            String data = formato.format(
                    lembrete.getDataHora()
            );

            String texto = lembrete.getDescricao()
                    + " — " + valor
                    + "\n" + frequencia
                    + "\nPróximo vencimento: " + data
                    + "\n" + antecedencia;

            adicionarLinha(texto);
        }
    }

    private void adicionarLinha(String texto) {

        TextView item = new TextView(this);

        item.setText(texto);
        item.setTextSize(16);
        item.setTextColor(Color.rgb(23, 54, 93));
        item.setPadding(16, 22, 16, 22);

        lista.addView(item);

        View divisor = new View(this);
        divisor.setBackgroundColor(
                Color.rgb(220, 225, 232)
        );

        lista.addView(
                divisor,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                )
        );
    }

    private void setProcessando(boolean valor) {

        processando = valor;

        btnSalvar.setEnabled(!valor && sessaoValida);

        progresso.setVisibility(
                valor ? View.VISIBLE : View.GONE
        );
    }

    private void abrirLogin() {

        sessaoValida = false;

        Intent intent = new Intent(
                this, LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void configurarNotificacoes() {

        LembreteReceiver.criarCanal(this);

        if (Build.VERSION.SDK_INT >= 33) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                permissaoNotificacao.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                );
            }
        }
    }

}
