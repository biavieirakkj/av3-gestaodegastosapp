
package com.example.controlefinanceiro.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.controlefinanceiro.data.entity.Categoria;

import java.util.List;

@Dao
public interface CategoriaDao {

    @Insert
    long inserir(Categoria categoria);

    @Update
    void atualizar(Categoria categoria);

    @Delete
    void excluir(Categoria categoria);

    @Query("SELECT * FROM categorias WHERE usuarioId = :usuarioId ORDER BY nome ASC")
    LiveData<List<Categoria>> listarPorUsuario(int usuarioId);

    @Query("SELECT * FROM categorias WHERE id = :id AND usuarioId = :usuarioId LIMIT 1")
    Categoria buscarPorId(int id, int usuarioId);
}
