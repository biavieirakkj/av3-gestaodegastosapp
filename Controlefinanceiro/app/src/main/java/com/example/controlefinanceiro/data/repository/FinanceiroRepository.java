
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

    // Método auxiliar para executar operações em background.
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

        executar(() -> {

            db.movimentacaoDao()
                    .atualizar(movimentacao);

            return null;

        }, callback);
    }

    public void excluirMovimentacao(
            Movimentacao movimentacao,
            Callback<Void> callback
    ) {

        executar(() -> {

            db.movimentacaoDao()
                    .excluir(movimentacao);

            return null;

        }, callback);
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

}
