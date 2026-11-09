
package com.example.controlefinanceiro.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.controlefinanceiro.data.database.AppDatabase;
import com.example.controlefinanceiro.data.entity.Categoria;
import com.example.controlefinanceiro.data.entity.Lembrete;
import com.example.controlefinanceiro.data.entity.Movimentacao;
import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.utils.SenhaUtils;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinanceiroRepository {

    private final AppDatabase db;

    // Executa operações do banco fora da thread principal.
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // Devolve os resultados para a interface do Android.
    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public FinanceiroRepository(Context context) {
        db = AppDatabase.getInstance(
                context.getApplicationContext()
        );
    }

    // Interface para receber resultados das operações.
    public interface Callback<T> {

        void onSucesso(T resultado);

        void onErro(Exception erro);
    }

    // Metodo auxiliar para executar operações em background.
    private <T> void executar(
            Callable<T> operacao,
            Callback<T> callback
    ) {

        executor.execute(() -> {

            try {

                T resultado = operacao.call();

                if (callback != null) {
                    mainHandler.post(
                            () -> callback.onSucesso(resultado)
                    );
                }

            } catch (Exception erro) {

                if (callback != null) {
                    mainHandler.post(
                            () -> callback.onErro(erro)
                    );
                }
            }
        });
    }

    // ========================================
    // USUÁRIOS
    // ========================================

    public void cadastrarUsuario(
            Usuario usuario,
            Callback<Long> callback
    ) {

        executar(
                () -> db.usuarioDao().inserir(usuario),
                callback
        );
    }

    public void buscarUsuarioPorEmail(
            String email,
            Callback<Usuario> callback
    ) {

        executar(
                () -> db.usuarioDao().buscarPorEmail(email),
                callback
        );
    }

    public void buscarUsuarioPorId(
            int usuarioId,
            Callback<Usuario> callback
    ) {

        executar(
                () -> db.usuarioDao().buscarPorId(usuarioId),
                callback
        );
    }

    public void atualizarUsuario(
            Usuario usuario,
            Callback<Void> callback
    ) {

        executar(() -> {

            db.usuarioDao().atualizar(usuario);

            return null;

        }, callback);
    }

    public void excluirUsuario(
            Usuario usuario,
            Callback<Void> callback
    ) {

        executar(() -> {

            db.usuarioDao().excluir(usuario);

            return null;

        }, callback);
    }

    // ========================================
    // SESSÃO
    // ========================================

    public void iniciarSessao(
            int usuarioId,
            Callback<Boolean> callback
    ) {

        executar(() -> db.runInTransaction(() -> {

            Usuario usuario =
                    db.usuarioDao().buscarPorId(usuarioId);

            if (usuario == null) {
                return false;
            }

            db.sessaoDao().salvar(
                    new Sessao(usuarioId)
            );

            return true;

        }), callback);
    }

    public void buscarSessaoAtiva(
            Callback<Sessao> callback
    ) {

        executar(
                () -> db.sessaoDao().buscarSessaoAtiva(),
                callback
        );
    }

    public void encerrarSessao(
            Callback<Void> callback
    ) {

        executar(() -> {

            db.sessaoDao().encerrarSessao();

            return null;

        }, callback);
    }

    // ========================================
    // CATEGORIAS
    // ========================================

    public LiveData<List<Categoria>> listarCategorias(
            int usuarioId
    ) {

        return db.categoriaDao()
                .listarPorUsuario(usuarioId);
    }

    public void cadastrarCategoria(
            Categoria categoria,
            Callback<Long> callback
    ) {

        executar(
                () -> db.categoriaDao().inserir(categoria),
                callback
        );
    }

    public void atualizarCategoria(
            Categoria categoria,
            Callback<Void> callback
    ) {

        executar(() -> {

            db.categoriaDao().atualizar(categoria);

            return null;

        }, callback);
    }

    public void excluirCategoria(
            Categoria categoria,
            Callback<Void> callback
    ) {

        executar(() -> {

            db.categoriaDao().excluir(categoria);

            return null;

        }, callback);
    }

    // ========================================
    // MOVIMENTAÇÕES FINANCEIRAS
    // ========================================

    public LiveData<List<Movimentacao>> listarMovimentacoes(
            int usuarioId
    ) {

        return db.movimentacaoDao()
                .listarPorUsuario(usuarioId);
    }

    public LiveData<List<Movimentacao>> listarPorPeriodo(
            int usuarioId,
            long inicio,
            long fim
    ) {

        return db.movimentacaoDao()
                .listarPorPeriodo(usuarioId, inicio, fim);
    }

    public void cadastrarMovimentacao(
            Movimentacao movimentacao,
            Callback<Long> callback
    ) {

        executar(
                () -> db.movimentacaoDao()
                        .inserir(movimentacao),
                callback
        );
    }


    public void atualizarMovimentacao(
            Movimentacao movimentacao,
            Callback<Void> callback
    ) {
        executar(() -> db.runInTransaction(() -> {

            if (movimentacao == null) {
                throw new IllegalArgumentException(
                        "Movimentação inválida."
                );
            }

            Sessao sessao = db.sessaoDao()
                    .buscarSessaoAtiva();

            if (sessao == null) {
                throw new IllegalStateException(
                        "Faça login novamente."
                );
            }

            int usuarioId = sessao.getUsuarioId();

            // Busca somente uma movimentação pertencente
            // ao usuário que está autenticado.
            Movimentacao original = db.movimentacaoDao()
                    .buscarPorId(movimentacao.getId(), usuarioId);

            if (original == null) {
                throw new IllegalArgumentException(
                        "Movimentação não encontrada."
                );
            }

            if (movimentacao.getDescricao() == null ||
                    movimentacao.getDescricao().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe uma descrição."
                );
            }

            if (!"RECEITA".equals(movimentacao.getTipo()) &&
                    !"DESPESA".equals(movimentacao.getTipo())) {
                throw new IllegalArgumentException(
                        "Tipo de movimentação inválido."
                );
            }

            if (movimentacao.getValorCentavos() <= 0 ||
                    movimentacao.getValorCentavos() > 100000000000L) {
                throw new IllegalArgumentException(
                        "Valor inválido."
                );
            }

            Integer categoriaId = movimentacao.getCategoriaId();

            if (categoriaId != null) {
                Categoria categoria = db.categoriaDao()
                        .buscarPorId(categoriaId, usuarioId);

                if (categoria == null) {
                    throw new IllegalArgumentException(
                            "Categoria inválida."
                    );
                }
            }

            // Atualiza apenas os campos permitidos.
            original.setDescricao(
                    movimentacao.getDescricao().trim()
            );
            original.setTipo(movimentacao.getTipo());
            original.setValorCentavos(
                    movimentacao.getValorCentavos()
            );
            original.setCategoriaId(categoriaId);
            original.setData(movimentacao.getData());

            db.movimentacaoDao().atualizar(original);

            return null;

        }), callback);
    }

    public void excluirMovimentacao(
            int movimentacaoId,
            Callback<Void> callback
    ) {
        executar(() -> db.runInTransaction(() -> {

            Sessao sessao = db.sessaoDao()
                    .buscarSessaoAtiva();

            if (sessao == null) {
                throw new IllegalStateException(
                        "Faça login novamente."
                );
            }

            Movimentacao movimentacao = db.movimentacaoDao()
                    .buscarPorId(
                            movimentacaoId,
                            sessao.getUsuarioId()
                    );

            if (movimentacao == null) {
                throw new IllegalArgumentException(
                        "Movimentação não encontrada."
                );
            }

            db.movimentacaoDao().excluir(movimentacao);

            return null;

        }), callback);
    }


    // ========================================
    // RESUMO FINANCEIRO
    // ========================================

    public LiveData<Long> consultarSaldo(
            int usuarioId
    ) {

        return db.movimentacaoDao()
                .consultarSaldo(usuarioId);
    }

    public LiveData<Long> totalReceitas(
            int usuarioId
    ) {

        return db.movimentacaoDao()
                .totalReceitas(usuarioId);
    }

    public LiveData<Long> totalDespesas(
            int usuarioId
    ) {

        return db.movimentacaoDao()
                .totalDespesas(usuarioId);
    }

    // ========================================
    // LEMBRETES
    // ========================================

    public LiveData<List<Lembrete>> listarLembretes(
            int usuarioId
    ) {

        return db.lembreteDao()
                .listarPorUsuario(usuarioId);
    }

    public LiveData<List<Lembrete>> listarLembretesPendentes(
            int usuarioId
    ) {

        return db.lembreteDao()
                .listarPendentes(usuarioId);
    }

    public void cadastrarLembrete(
            Lembrete lembrete,
            Callback<Long> callback
    ) {

        executar(
                () -> db.lembreteDao().inserir(lembrete),
                callback
        );
    }

    public void atualizarLembrete(
            Lembrete lembrete,
            Callback<Void> callback
    ) {

        executar(() -> {

            db.lembreteDao().atualizar(lembrete);

            return null;

        }, callback);
    }

    public void excluirLembrete(
            Lembrete lembrete,
            Callback<Void> callback
    ) {

        executar(() -> {

            db.lembreteDao().excluir(lembrete);

            return null;

        }, callback);
    }


    // ========================================
    // CADASTRO COM SENHA PROTEGIDA
    // ========================================

    public void cadastrarComSenha(
            String nome,
            String email,
            String senha,
            Callback<Long> callback
    ) {

        executar(() -> {

            if (nome == null || nome.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe seu nome."
                );
            }

            if (email == null ||
                    !android.util.Patterns.EMAIL_ADDRESS
                            .matcher(email.trim()).matches()) {
                throw new IllegalArgumentException(
                        "Informe um e-mail válido."
                );
            }

            if (senha == null || senha.length() < 8) {
                throw new IllegalArgumentException(
                        "A senha deve ter pelo menos 8 caracteres."
                );
            }

            String emailNormalizado =
                    email.trim().toLowerCase(Locale.ROOT);

            if (db.usuarioDao().buscarPorEmail(
                    emailNormalizado) != null) {

                throw new IllegalArgumentException(
                        "Este e-mail já está cadastrado."
                );
            }

            String hash = SenhaUtils.gerarHash(senha);

            Usuario usuario = new Usuario(
                    nome.trim(),
                    emailNormalizado,
                    hash,
                    null
            );

            return db.usuarioDao().inserir(usuario);

        }, callback);
    }

    // ========================================
    // LOGIN COM BCRYPT E SESSÃO
    // ========================================

    public void fazerLogin(
            String email,
            String senha,
            Callback<Usuario> callback
    ) {

        executar(() -> {

            if (email == null || senha == null ||
                    email.trim().isEmpty() ||
                    senha.isEmpty()) {

                throw new IllegalArgumentException(
                        "Informe e-mail e senha."
                );
            }

            String emailNormalizado =
                    email.trim().toLowerCase(Locale.ROOT);

            Usuario usuario = db.usuarioDao()
                    .buscarPorEmail(emailNormalizado);

            if (usuario == null ||
                    !SenhaUtils.verificarSenha(
                            senha,
                            usuario.getSenhaHash())) {

                throw new IllegalArgumentException(
                        "E-mail ou senha incorretos."
                );
            }

            // Confere novamente os dados antes de abrir a sessão.
            return db.runInTransaction(() -> {

                Usuario atual = db.usuarioDao()
                        .buscarPorId(usuario.getId());

                if (atual == null ||
                        !usuario.getSenhaHash().equals(
                                atual.getSenhaHash())) {

                    throw new IllegalStateException(
                            "Os dados da conta foram alterados. Tente novamente."
                    );
                }

                db.sessaoDao().salvar(
                        new Sessao(atual.getId())
                );

                return atual;
            });

        }, callback);
    }


    // ========================================
    // EDITAR PERFIL E ALTERAR SENHA
    // ========================================

    public void editarPerfil(
            String novoNome,
            String novoEmail,
            String senhaAtual,
            String novaSenha,
            Callback<Usuario> callback
    ) {

        executar(() -> {

            if (novoNome == null ||
                    novoNome.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe seu nome."
                );
            }

            if (novoEmail == null ||
                    !android.util.Patterns.EMAIL_ADDRESS
                            .matcher(novoEmail.trim()).matches()) {
                throw new IllegalArgumentException(
                        "Informe um e-mail válido."
                );
            }

            if (senhaAtual == null ||
                    senhaAtual.isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe sua senha atual."
                );
            }

            boolean alterarSenha =
                    novaSenha != null && !novaSenha.isEmpty();

            if (alterarSenha && novaSenha.length() < 8) {
                throw new IllegalArgumentException(
                        "A nova senha precisa ter pelo menos 8 caracteres."
                );
            }

            String emailNormalizado =
                    novoEmail.trim().toLowerCase(
                            java.util.Locale.ROOT
                    );

            // Consulta feita fora da thread principal.
            Sessao sessao = db.sessaoDao()
                    .buscarSessaoAtiva();

            if (sessao == null) {
                throw new IllegalStateException(
                        "Sessão encerrada. Entre novamente."
                );
            }

            Usuario usuario = db.usuarioDao()
                    .buscarPorId(sessao.getUsuarioId());

            if (usuario == null ||
                    !SenhaUtils.verificarSenha(
                            senhaAtual,
                            usuario.getSenhaHash())) {
                throw new IllegalArgumentException(
                        "Senha atual incorreta."
                );
            }

            // BCrypt executado na thread de trabalho.
            String novoHash = alterarSenha
                    ? SenhaUtils.gerarHash(novaSenha)
                    : usuario.getSenhaHash();

            // Atualização consistente, dentro de uma transação.
            return db.runInTransaction(() -> {

                Sessao sessaoAtual = db.sessaoDao()
                        .buscarSessaoAtiva();

                if (sessaoAtual == null ||
                        sessaoAtual.getUsuarioId() != usuario.getId()) {
                    throw new IllegalStateException(
                            "Sessão alterada. Entre novamente."
                    );
                }

                Usuario usuarioAtual = db.usuarioDao()
                        .buscarPorId(usuario.getId());

                if (usuarioAtual == null ||
                        !usuarioAtual.getSenhaHash().equals(
                                usuario.getSenhaHash())) {
                    throw new IllegalStateException(
                            "O perfil foi alterado. Tente novamente."
                    );
                }

                Usuario outroUsuario = db.usuarioDao()
                        .buscarPorEmail(emailNormalizado);

                if (outroUsuario != null &&
                        outroUsuario.getId() != usuarioAtual.getId()) {
                    throw new IllegalArgumentException(
                            "Este e-mail já está cadastrado."
                    );
                }

                usuarioAtual.setNome(novoNome.trim());
                usuarioAtual.setEmail(emailNormalizado);
                usuarioAtual.setSenhaHash(novoHash);

                db.usuarioDao().atualizar(usuarioAtual);

                return usuarioAtual;
            });

        }, callback);
    }

    // ========================================
    // ALTERAR FOTO DE PERFIL
    // ========================================

    public void alterarFotoPerfil(
            String fotoUri,
            Callback<Usuario> callback
    ) {

        executar(() -> db.runInTransaction(() -> {

            Sessao sessao = db.sessaoDao()
                    .buscarSessaoAtiva();

            if (sessao == null) {
                throw new IllegalStateException(
                        "É necessário fazer login."
                );
            }

            Usuario usuario = db.usuarioDao()
                    .buscarPorId(sessao.getUsuarioId());

            if (usuario == null) {
                throw new IllegalStateException(
                        "Usuário não encontrado."
                );
            }

            if (fotoUri == null || fotoUri.isEmpty()) {
                throw new IllegalArgumentException(
                        "Imagem inválida."
                );
            }

            usuario.setFotoUri(fotoUri);
            db.usuarioDao().atualizar(usuario);

            return usuario;

        }), callback);
    }


    // Cadastra uma receita ou despesa do usuário logado.
    public void registrarMovimentacao(
            String descricao,
            String tipo,
            long valorCentavos,
            Callback<Long> callback
    ) {

        executar(() -> {

            if (descricao == null ||
                    descricao.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe uma descrição."
                );
            }

            if (!"RECEITA".equals(tipo) &&
                    !"DESPESA".equals(tipo)) {
                throw new IllegalArgumentException(
                        "Tipo de movimentação inválido."
                );
            }

            if (valorCentavos <= 0) {
                throw new IllegalArgumentException(
                        "O valor deve ser maior que zero."
                );
            }

            if (valorCentavos > 100000000000L) {
                throw new IllegalArgumentException(
                        "Valor acima do limite permitido."
                );
            }

            return db.runInTransaction(() -> {

                Sessao sessao = db.sessaoDao()
                        .buscarSessaoAtiva();

                if (sessao == null) {
                    throw new IllegalStateException(
                            "Faça login novamente."
                    );
                }

                Usuario usuario = db.usuarioDao()
                        .buscarPorId(sessao.getUsuarioId());

                if (usuario == null) {
                    throw new IllegalStateException(
                            "Usuário não encontrado."
                    );
                }

                Movimentacao movimentacao =
                        new Movimentacao(
                                usuario.getId(),
                                null,
                                descricao.trim(),
                                tipo,
                                valorCentavos,
                                System.currentTimeMillis()
                        );

                return db.movimentacaoDao()
                        .inserir(movimentacao);
            });

        }, callback);
    }


    public void registrarLembrete(
            String descricao,
            long valorCentavos,
            long dataHora,
            boolean mensal,
            int diaVencimento,
            int diasAntes,
            Callback<Long> callback
    ) {

        executar(() -> {

            if (descricao == null ||
                    descricao.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe a descrição da conta."
                );
            }

            if (valorCentavos < 0) {
                throw new IllegalArgumentException(
                        "O valor não pode ser negativo."
                );
            }

            if (dataHora <= System.currentTimeMillis()) {
                throw new IllegalArgumentException(
                        "Escolha uma data futura."
                );
            }

            if (diaVencimento < 1 || diaVencimento > 31) {
                throw new IllegalArgumentException(
                        "Dia de vencimento inválido."
                );
            }

            if (diasAntes != 0 && diasAntes != 1) {
                throw new IllegalArgumentException(
                        "Antecedência inválida."
                );
            }

            return db.runInTransaction(() -> {

                Sessao sessao = db.sessaoDao()
                        .buscarSessaoAtiva();

                if (sessao == null) {
                    throw new IllegalStateException(
                            "Faça login novamente."
                    );
                }

                Usuario usuario = db.usuarioDao()
                        .buscarPorId(sessao.getUsuarioId());

                if (usuario == null) {
                    throw new IllegalStateException(
                            "Usuário não encontrado."
                    );
                }

                Lembrete lembrete = new Lembrete(
                        usuario.getId(),
                        descricao.trim(),
                        valorCentavos,
                        dataHora,
                        false
                );

                lembrete.setMensal(mensal);
                lembrete.setDiaVencimento(diaVencimento);
                lembrete.setDiasAntes(diasAntes);

                return db.lembreteDao()
                        .inserir(lembrete);
            });

        }, callback);
    }


    public void criarCategoria(
            String nome,
            Callback<Long> callback
    ) {

        executar(() -> db.runInTransaction(() -> {

            if (nome == null || nome.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe o nome da categoria."
                );
            }

            if (nome.trim().length() > 50) {
                throw new IllegalArgumentException(
                        "Use no máximo 50 caracteres."
                );
            }

            Sessao sessao = db.sessaoDao()
                    .buscarSessaoAtiva();

            if (sessao == null) {
                throw new IllegalStateException(
                        "Faça login novamente."
                );
            }

            Usuario usuario = db.usuarioDao()
                    .buscarPorId(sessao.getUsuarioId());

            if (usuario == null) {
                throw new IllegalStateException(
                        "Usuário não encontrado."
                );
            }

            List<Categoria> categorias =
                    db.categoriaDao()
                            .listarPorUsuario(usuario.getId())
                            .getValue();

            Categoria categoria = new Categoria(
                    nome.trim(),
                    usuario.getId()
            );

            return db.categoriaDao().inserir(categoria);

        }), callback);
    }


    public void registrarMovimentacao(
            String descricao,
            String tipo,
            long valorCentavos,
            Integer categoriaId,
            Callback<Long> callback
    ) {

        executar(() -> db.runInTransaction(() -> {

            if (descricao == null ||
                    descricao.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe uma descrição."
                );
            }

            if (!"RECEITA".equals(tipo) &&
                    !"DESPESA".equals(tipo)) {
                throw new IllegalArgumentException(
                        "Tipo inválido."
                );
            }

            if (valorCentavos <= 0 ||
                    valorCentavos > 100000000000L) {
                throw new IllegalArgumentException(
                        "Valor inválido."
                );
            }

            Sessao sessao = db.sessaoDao()
                    .buscarSessaoAtiva();

            if (sessao == null) {
                throw new IllegalStateException(
                        "Faça login novamente."
                );
            }

            Usuario usuario = db.usuarioDao()
                    .buscarPorId(sessao.getUsuarioId());

            if (usuario == null) {
                throw new IllegalStateException(
                        "Usuário não encontrado."
                );
            }

            if (categoriaId != null) {

                Categoria categoria = db.categoriaDao()
                        .buscarPorId(
                                categoriaId,
                                usuario.getId()
                        );

                if (categoria == null) {
                    throw new IllegalArgumentException(
                            "Categoria inválida."
                    );
                }
            }

            Movimentacao movimentacao = new Movimentacao(
                    usuario.getId(),
                    categoriaId,
                    descricao.trim(),
                    tipo,
                    valorCentavos,
                    System.currentTimeMillis()
            );

            return db.movimentacaoDao().inserir(movimentacao);

        }), callback);
    }


    public void buscarMovimentacao(
            int movimentacaoId,
            Callback<Movimentacao> callback
    ) {
        executar(() -> db.runInTransaction(() -> {

            Sessao sessao = db.sessaoDao()
                    .buscarSessaoAtiva();

            if (sessao == null) {
                throw new IllegalStateException(
                        "Faça login novamente."
                );
            }

            Movimentacao movimentacao =
                    db.movimentacaoDao().buscarPorId(
                            movimentacaoId,
                            sessao.getUsuarioId()
                    );

            if (movimentacao == null) {
                throw new IllegalArgumentException(
                        "Movimentação não encontrada."
                );
            }

            return movimentacao;

        }), callback);
    }


    public void editarLembreteSeguro(
            Lembrete dados,
            Callback<Lembrete> callback
    ) {
        executar(() -> db.runInTransaction(() -> {

            if (dados == null ||
                    dados.getDescricao() == null ||
                    dados.getDescricao().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Informe a descrição do lembrete."
                );
            }

            if (dados.getValorCentavos() < 0 ||
                    dados.getDiaVencimento() < 1 ||
                    dados.getDiaVencimento() > 31 ||
                    (dados.getDiasAntes() != 0 &&
                            dados.getDiasAntes() != 1)) {

                throw new IllegalArgumentException(
                        "Dados do lembrete inválidos."
                );
            }

            java.util.Calendar aviso =
                    java.util.Calendar.getInstance();

            aviso.setTimeInMillis(dados.getDataHora());
            aviso.add(
                    java.util.Calendar.DAY_OF_MONTH,
                    -dados.getDiasAntes()
            );

            if (aviso.getTimeInMillis()
                    <= System.currentTimeMillis()) {
                throw new IllegalArgumentException(
                        "O aviso deve ser em uma data futura."
                );
            }

            Sessao sessao = db.sessaoDao()
                    .buscarSessaoAtiva();

            if (sessao == null) {
                throw new IllegalStateException(
                        "Faça login novamente."
                );
            }

            Lembrete original = db.lembreteDao()
                    .buscarParaEdicao(
                            dados.getId(),
                            sessao.getUsuarioId()
                    );

            if (original == null) {
                throw new IllegalArgumentException(
                        "Lembrete não encontrado."
                );
            }

            original.setDescricao(
                    dados.getDescricao().trim()
            );
            original.setValorCentavos(
                    dados.getValorCentavos()
            );
            original.setDataHora(
                    dados.getDataHora()
            );
            original.setMensal(
                    dados.isMensal()
            );
            original.setDiaVencimento(
                    dados.getDiaVencimento()
            );
            original.setDiasAntes(
                    dados.getDiasAntes()
            );
            original.setConcluido(false);

            db.lembreteDao().atualizar(original);

            return original;

        }), callback);
    }

    public void apagarLembreteSeguro(
            int lembreteId,
            Callback<Void> callback
    ) {
        executar(() -> db.runInTransaction(() -> {

            Sessao sessao = db.sessaoDao()
                    .buscarSessaoAtiva();

            if (sessao == null) {
                throw new IllegalStateException(
                        "Faça login novamente."
                );
            }

            Lembrete lembrete = db.lembreteDao()
                    .buscarParaEdicao(
                            lembreteId,
                            sessao.getUsuarioId()
                    );

            if (lembrete == null) {
                throw new IllegalArgumentException(
                        "Lembrete não encontrado."
                );
            }

            db.lembreteDao().excluir(lembrete);

            return null;

        }), callback);
    }

}
