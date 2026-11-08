
package com.example.controlefinanceiro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.controlefinanceiro.data.entity.Movimentacao;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.data.entity.Lembrete;
import java.util.List;

public class FinanceiroViewModel extends AndroidViewModel {

    private final FinanceiroRepository repository;

    public FinanceiroViewModel(
            @NonNull Application application
    ) {
        super(application);

        repository = new FinanceiroRepository(application);
    }

    // ========================================
    // CADASTRO DE RECEITAS E DESPESAS
    // ========================================

    public void registrarMovimentacao(
            String descricao,
            String tipo,
            long valorCentavos,
            FinanceiroRepository.Callback<Long> callback
    ) {
        repository.registrarMovimentacao(
                descricao,
                tipo,
                valorCentavos,
                callback
        );
    }

    // ========================================
    // HISTÓRICO DAS MOVIMENTAÇÕES
    // ========================================

    public LiveData<List<Movimentacao>> listarMovimentacoes(
            int usuarioId
    ) {
        return repository.listarMovimentacoes(usuarioId);
    }

    // ========================================
    // SALDO TOTAL
    // ========================================

    public LiveData<Long> consultarSaldo(
            int usuarioId
    ) {
        return repository.consultarSaldo(usuarioId);
    }

    // ========================================
    // TOTAL DE RECEITAS
    // ========================================

    public LiveData<Long> totalReceitas(
            int usuarioId
    ) {
        return repository.totalReceitas(usuarioId);
    }

    // ========================================
    // TOTAL DE DESPESAS
    // ========================================

    public LiveData<Long> totalDespesas(
            int usuarioId
    ) {
        return repository.totalDespesas(usuarioId);
    }

    public void registrarLembrete(
            String descricao,
            long valorCentavos,
            long dataHora,
            boolean mensal,
            int diaVencimento,
            int diasAntes,
            FinanceiroRepository.Callback<Long> callback
    ) {

        repository.registrarLembrete(
                descricao,
                valorCentavos,
                dataHora,
                mensal,
                diaVencimento,
                diasAntes,
                callback
        );
    }

    public LiveData<List<Lembrete>> listarLembretes(
            int usuarioId
    ) {
        return repository.listarLembretes(usuarioId);
    }

}
