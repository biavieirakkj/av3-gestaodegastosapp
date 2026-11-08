package com.example.controlefinanceiro.notificacao;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.controlefinanceiro.MainActivity;
import com.example.controlefinanceiro.data.database.AppDatabase;
import com.example.controlefinanceiro.data.entity.Lembrete;
import com.example.controlefinanceiro.data.entity.Sessao;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class LembreteReceiver extends BroadcastReceiver {

    public static final String CANAL_ID =
            "lembretes_financeiros";

    public static void criarCanal(Context context) {

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        if (manager == null) {
            return;
        }

        NotificationChannel canal =
                new NotificationChannel(
                        CANAL_ID,
                        "Lembretes financeiros",
                        NotificationManager.IMPORTANCE_DEFAULT
                );

        canal.setDescription(
                "Avisos de contas próximas do vencimento"
        );

        manager.createNotificationChannel(canal);
    }

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        if (intent == null ||
                !AgendadorLembretes.ACAO_LEMBRETE
                        .equals(intent.getAction())) {
            return;
        }

        int lembreteId = intent.getIntExtra(
                AgendadorLembretes.EXTRA_ID,
                -1
        );

        if (lembreteId < 1) {
            return;
        }

        // Mantém o BroadcastReceiver ativo enquanto
        // a consulta ao Room acontece em outra Thread.
        PendingResult resultadoPendente = goAsync();

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

                Lembrete lembrete = db.lembreteDao()
                        .buscarPorId(
                                lembreteId,
                                sessao.getUsuarioId()
                        );

                if (lembrete == null ||
                        lembrete.isConcluido()) {
                    return;
                }

                long agora = System.currentTimeMillis();

                long horario =
                        AgendadorLembretes.horarioAviso(
                                lembrete
                        );

                // Não mostrar notificações de um alarme
                // antigo se o vencimento foi alterado.
                if (horario > agora) {

                    AgendadorLembretes.agendar(
                            aplicativo,
                            lembrete
                    );

                    return;
                }

                // Evita mostrar avisos muito antigos
                // após longos períodos com celular desligado.
                long doisDias = 48L * 60 * 60 * 1000;

                if (agora - horario <= doisDias) {

                    mostrarNotificacao(
                            aplicativo,
                            lembrete
                    );
                }

                if (lembrete.isMensal()) {

                    AgendadorLembretes.avancarParaProximoMes(
                            lembrete,
                            agora
                    );

                    db.lembreteDao().atualizar(lembrete);

                    AgendadorLembretes.agendar(
                            aplicativo,
                            lembrete
                    );

                } else {

                    lembrete.setConcluido(true);
                    db.lembreteDao().atualizar(lembrete);
                }

            } catch (Exception erro) {

                android.util.Log.e(
                        "Lembretes",
                        "Falha ao disparar lembrete",
                        erro
                );

            } finally {

                resultadoPendente.finish();
            }

        }, "DisparoLembrete").start();
    }

    private void mostrarNotificacao(
            Context context,
            Lembrete lembrete
    ) {

        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            // Permissão negada: não tenta publicar.
            return;
        }

        criarCanal(context);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        if (manager == null ||
                !manager.areNotificationsEnabled()) {
            return;
        }

        NumberFormat moeda =
                NumberFormat.getCurrencyInstance(
                        new Locale("pt", "BR")
                );

        String valor = moeda.format(
                BigDecimal.valueOf(
                        lembrete.getValorCentavos(),
                        2
                )
        );

        String mensagem = lembrete.getDiasAntes() == 1
                ? "A conta " + lembrete.getDescricao()
                  + " de " + valor
                  + " vence amanhã."
                : "A conta " + lembrete.getDescricao()
                  + " de " + valor
                  + " vence hoje.";

        Intent abrirAplicativo = new Intent(
                context,
                MainActivity.class
        );

        abrirAplicativo.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        PendingIntent abrir = PendingIntent.getActivity(
                context,
                lembrete.getId(),
                abrirAplicativo,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificacao =
                new NotificationCompat.Builder(
                        context,
                        CANAL_ID
                )
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle("Meu Pé de Meia 🍀")
                        .setContentText(mensagem)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(mensagem)
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_DEFAULT
                        )
                        .setContentIntent(abrir)
                        .setAutoCancel(true);

        manager.notify(
                lembrete.getId(),
                notificacao.build()
        );
    }
}
