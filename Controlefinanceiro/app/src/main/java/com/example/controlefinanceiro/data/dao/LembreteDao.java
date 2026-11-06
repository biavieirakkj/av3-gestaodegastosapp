
package com.example.controlefinanceiro.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.controlefinanceiro.data.entity.Lembrete;

import java.util.List;

@Dao
public interface LembreteDao {

    @Insert
    long inserir(Lembrete lembrete);

    @Update
    void atualizar(Lembrete lembrete);

    @Delete
    void excluir(Lembrete lembrete);

    @Query("SELECT * FROM lembretes WHERE usuarioId = :usuarioId ORDER BY dataHora ASC")
    LiveData<List<Lembrete>> listarPorUsuario(int usuarioId);

    @Query("SELECT * FROM lembretes WHERE id = :id AND usuarioId = :usuarioId LIMIT 1")
    Lembrete buscarPorId(int id, int usuarioId);

    @Query("SELECT * FROM lembretes WHERE usuarioId = :usuarioId AND concluido = 0 ORDER BY dataHora ASC")
    LiveData<List<Lembrete>> listarPendentes(int usuarioId);
}
