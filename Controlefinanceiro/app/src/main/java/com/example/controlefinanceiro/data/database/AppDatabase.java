
package com.example.controlefinanceiro.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.controlefinanceiro.data.dao.UsuarioDao;
import com.example.controlefinanceiro.data.dao.CategoriaDao;
import com.example.controlefinanceiro.data.dao.MovimentacaoDao;
import com.example.controlefinanceiro.data.dao.LembreteDao;
import com.example.controlefinanceiro.data.dao.SessaoDao;

import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.data.entity.Categoria;
import com.example.controlefinanceiro.data.entity.Movimentacao;
import com.example.controlefinanceiro.data.entity.Lembrete;
import com.example.controlefinanceiro.data.entity.Sessao;

@Database(
        entities = {
                Usuario.class,
                Categoria.class,
                Movimentacao.class,
                Lembrete.class,
                Sessao.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract UsuarioDao usuarioDao();

    public abstract CategoriaDao categoriaDao();

    public abstract MovimentacaoDao movimentacaoDao();

    public abstract LembreteDao lembreteDao();

    public abstract SessaoDao sessaoDao();

    public static AppDatabase getInstance(Context context) {

        if (INSTANCE == null) {

            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "controle_financeiro_db"
                    )
                    .addMigrations(Migracoes.MIGRATION_1_2)
                    .build();
        }

        return INSTANCE;
    }
}
