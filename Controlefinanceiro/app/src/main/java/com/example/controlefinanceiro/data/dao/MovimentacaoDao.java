
package com.example.controlefinanceiro.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.controlefinanceiro.data.entity.Movimentacao;

import java.util.List;

@Dao
public interface MovimentacaoDao {

    @Insert
    long inserir(Movimentacao movimentacao);

    @Update
    void atualizar(Movimentacao movimentacao);

    @Delete
    void excluir(Movimentacao movimentacao);

    @Query("SELECT * FROM movimentacoes WHERE usuarioId = :usuarioId ORDER BY data DESC, id DESC")
    LiveData<List<Movimentacao>> listarPorUsuario(int usuarioId);

    @Query("SELECT * FROM movimentacoes WHERE id = :id AND usuarioId = :usuarioId LIMIT 1")
    Movimentacao buscarPorId(int id, int usuarioId);

    @Query("SELECT COALESCE(SUM(CASE WHEN tipo = 'RECEITA' THEN valorCentavos WHEN tipo = 'DESPESA' THEN -valorCentavos ELSE 0 END), 0) FROM movimentacoes WHERE usuarioId = :usuarioId")
    LiveData<Long> consultarSaldo(int usuarioId);

    @Query("SELECT COALESCE(SUM(valorCentavos), 0) FROM movimentacoes WHERE usuarioId = :usuarioId AND tipo = 'RECEITA'")
    LiveData<Long> totalReceitas(int usuarioId);

    @Query("SELECT COALESCE(SUM(valorCentavos), 0) FROM movimentacoes WHERE usuarioId = :usuarioId AND tipo = 'DESPESA'")
    LiveData<Long> totalDespesas(int usuarioId);

    @Query("SELECT * FROM movimentacoes WHERE usuarioId = :usuarioId AND data >= :inicio AND data < :fim ORDER BY data DESC")
    LiveData<List<Movimentacao>> listarPorPeriodo(
            int usuarioId,
            long inicio,
            long fim
    );
}
