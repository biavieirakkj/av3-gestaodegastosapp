
package com.example.controlefinanceiro.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "movimentacoes",
        foreignKeys = {
                @ForeignKey(
                        entity = Usuario.class,
                        parentColumns = "id",
                        childColumns = "usuarioId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Categoria.class,
                        parentColumns = "id",
                        childColumns = "categoriaId",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {
                @Index("usuarioId"),
                @Index("categoriaId")
        }
)
public class Movimentacao {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int usuarioId;
    private Integer categoriaId;

    private String descricao;

    // "RECEITA" ou "DESPESA"
    private String tipo;

    // Valor em centavos para evitar erros de arredondamento.
    private long valorCentavos;

    // Data da movimentação em milissegundos.
    private long data;

    public Movimentacao(
            int usuarioId,
            Integer categoriaId,
            String descricao,
            String tipo,
            long valorCentavos,
            long data
    ) {
        this.usuarioId = usuarioId;
        this.categoriaId = categoriaId;
        this.descricao = descricao;
        this.tipo = tipo;
        this.valorCentavos = valorCentavos;
        this.data = data;
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

    public Integer getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Integer categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public void setValorCentavos(long valorCentavos) {
        this.valorCentavos = valorCentavos;
    }

    public long getData() {
        return data;
    }

    public void setData(long data) {
        this.data = data;
    }
}
