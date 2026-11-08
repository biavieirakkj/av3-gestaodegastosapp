package com.example.controlefinanceiro.notificacao;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.controlefinanceiro.data.database.AppDatabase;
import com.example.controlefinanceiro.data.entity.Lembrete;
import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.entity.Usuario;

import java.util.Calendar;
import java.util.List;

public class AgendadorLembretes {

    public static final String ACAO_LEMBRETE =
            "com.example.controlefinanceiro.AVISO_LEMBRETE";

    public static final String EXTRA_ID = "lembrete_id";

    private AgendadorLembretes() {
    }

    // Calcula a hora de avisar, respeitando o calendário.
    public static long horarioAviso(Lembrete lembrete) {

        Calendar calendario = Calendar.getInstance();

        calendario.setTimeInMillis(
                lembrete.getDataHora()
        );

        calendario.add(
                Calendar.DAY_OF_MONTH,
                -lembrete.getDiasAntes()
        );

        return calendario.getTimeInMillis();
    }

    // Avança uma conta mensal até encontrar um aviso futuro.
    public static void avancarParaProximoMes(
            Lembrete lembrete,
            long referencia
    ) {

        if (!lembrete.isMensal()) {
            return;
        }

        Calendar calendario = Calendar.getInstance();

        calendario.setTimeInMillis(
                lembrete.getDataHora()
        );

        while (horarioAviso(lembrete) <= referencia) {

            calendario.set(Calendar.DAY_OF_MONTH, 1);
            calendario.add(Calendar.MONTH, 1);

            int ultimoDia = calendario.getActualMaximum(
                    Calendar.DAY_OF_MONTH
            );

            calendario.set(
                    Calendar.DAY_OF_MONTH,
                    Math.min(
                            lembrete.getDiaVencimento(),
                            ultimoDia
                    )
            );

            lembrete.setDataHora(
                    calendario.getTimeInMillis()
            );
        }
    }

    private static PendingIntent criarPendingIntent(
            Context context,
            int lembreteId,
            int flags
    ) {

        Intent intent = new Intent(
                context,
                LembreteReceiver.class
        );

        intent.setAction(ACAO_LEMBRETE);

        intent.putExtra(EXTRA_ID, lembreteId);

        return PendingIntent.getBroadcast(
                context,
                lembreteId,
                intent,
                flags | PendingIntent.FLAG_IMMUTABLE
        );
    }

    // Agenda um único disparo. Para lembretes mensais,
    // o próximo será agendado após o disparo atual.
    public static void agendar(
            Context context,
            Lembrete lembrete
    ) {

        if (lembrete.isConcluido()) {
            return;
        }

        long momento = horarioAviso(lembrete);

        if (momento <= System.currentTimeMillis()) {
            return;
        }

        AlarmManager gerenciador =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (gerenciador == null) {
            return;
        }

        PendingIntent operacao = criarPendingIntent(
                context,
                lembrete.getId(),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        gerenciador.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                momento,
                operacao
        );
    }

    public static void cancelar(
            Context context,
            int lembreteId
    ) {

        AlarmManager gerenciador =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (gerenciador == null) {
            return;
        }

        PendingIntent operacao = criarPendingIntent(
                context,
                lembreteId,
                PendingIntent.FLAG_NO_CREATE
        );

        if (operacao != null) {
            gerenciador.cancel(operacao);
            operacao.cancel();
        }
    }

    // Executa todas as consultas em uma Thread.
    public static void reagendarTodos(Context context) {

        Context aplicativo =
                context.getApplicationContext();

        new Thread(() -> {

            try {

                AppDatabase db =
                        AppDatabase.getInstance(aplicativo);

                Sessao sessao =
                        db.sessaoDao().buscarSessaoAtiva();

                if (sessao == null) {
                    return;
                }

                Usuario usuario = db.usuarioDao()
                        .buscarPorId(sessao.getUsuarioId());

                if (usuario == null) {
                    return;
                }

                List<Lembrete> lembretes =
                        db.lembreteDao()
                                .listarParaAgendamento(
                                        usuario.getId()
                                );

                for (Lembrete lembrete : lembretes) {

                    if (lembrete.isMensal()) {

                        long dataAnterior =
                                lembrete.getDataHora();

                        avancarParaProximoMes(
                                lembrete,
                                System.currentTimeMillis()
                        );

                        if (dataAnterior !=
                                lembrete.getDataHora()) {

                            db.lembreteDao()
                                    .atualizar(lembrete);
                        }
                    }

                    agendar(aplicativo, lembrete);
                }

            } catch (Exception erro) {
                android.util.Log.e(
                        "Lembretes",
                        "Falha ao reagendar alarmes",
                        erro
                );
            }

        }, "ReagendarLembretes").start();
    }
}
