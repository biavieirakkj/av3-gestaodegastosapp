package com.example.controlefinanceiro.notificacao;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.controlefinanceiro.data.database.AppDatabase;
import com.example.controlefinanceiro.data.entity.Lembrete;
import com.example.controlefinanceiro.data.entity.Sessao;

import java.util.List;

public class ReinicioReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        if (intent == null ||
                !Intent.ACTION_BOOT_COMPLETED.equals(
                        intent.getAction()
                )) {
            return;
        }

        PendingResult pendente = goAsync();

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

                List<Lembrete> lembretes =
                        db.lembreteDao()
                                .listarParaAgendamento(
                                        sessao.getUsuarioId()
                                );

                for (Lembrete lembrete : lembretes) {

                    if (lembrete.isMensal()) {

                        long anterior =
                                lembrete.getDataHora();

                        AgendadorLembretes
                                .avancarParaProximoMes(
                                        lembrete,
                                        System.currentTimeMillis()
                                );

                        if (anterior !=
                                lembrete.getDataHora()) {

                            db.lembreteDao()
                                    .atualizar(lembrete);
                        }
                    }

                    AgendadorLembretes.agendar(
                            aplicativo,
                            lembrete
                    );
                }

            } catch (Exception erro) {

                android.util.Log.e(
                        "Lembretes",
                        "Erro após reiniciar o celular",
                        erro
                );

            } finally {

                pendente.finish();
            }

        }, "ReinicioLembretes").start();
    }
}
