
package com.example.controlefinanceiro.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.controlefinanceiro.data.entity.Sessao;

@Dao
public interface SessaoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvar(Sessao sessao);

    @Query("SELECT * FROM sessoes WHERE id = 1 LIMIT 1")
    Sessao buscarSessaoAtiva();

    @Query("DELETE FROM sessoes")
    void encerrarSessao();
}
