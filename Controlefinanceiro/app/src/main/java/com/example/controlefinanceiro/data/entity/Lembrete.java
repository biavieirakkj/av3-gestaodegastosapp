
package com.example.controlefinanceiro.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "lembretes",
        foreignKeys = @ForeignKey(
                entity = Usuario.class,
                parentColumns = "id",
                childColumns = "usuarioId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("usuarioId")
        }
)
public class Lembrete {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int usuarioId;

    private String descricao;

    private long valorCentavos;

    // Data e horário do lembrete em milissegundos.
    private long dataHora;

    private boolean concluido;

    public Lembrete(
            int usuarioId,
            String descricao,
            long valorCentavos,
            long dataHora,
            boolean concluido
    ) {
        this.usuarioId = usuarioId;
        this.descricao = descricao;
        this.valorCentavos = valorCentavos;
        this.dataHora = dataHora;
        this.concluido = concluido;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public void setValorCentavos(long valorCentavos) {
        this.valorCentavos = valorCentavos;
    }

    public long getDataHora() {
        return dataHora;
    }

    public void setDataHora(long dataHora) {
        this.dataHora = dataHora;
    }

    public boolean isConcluido() {
        return concluido;
    }

    public void setConcluido(boolean concluido) {
        this.concluido = concluido;
    }
}
