
package com.example.controlefinanceiro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;

public class AuthViewModel extends AndroidViewModel {

    private final FinanceiroRepository repository;

    public AuthViewModel(@NonNull Application application) {
        super(application);

        repository = new FinanceiroRepository(application);
    }

    public void cadastrar(
            String nome,
            String email,
            String senha,
            FinanceiroRepository.Callback<Long> callback
    ) {
        repository.cadastrarComSenha(
                nome,
                email,
                senha,
                callback
        );
    }

    public void login(
            String email,
            String senha,
            FinanceiroRepository.Callback<Usuario> callback
    ) {
        repository.fazerLogin(
                email,
                senha,
                callback
        );
    }

    public void consultarSessao(
            FinanceiroRepository.Callback<Sessao> callback
    ) {
        repository.buscarSessaoAtiva(callback);
    }

    public void logout(
            FinanceiroRepository.Callback<Void> callback
    ) {
        repository.encerrarSessao(callback);
    }

    public void buscarUsuario(
            int usuarioId,
            FinanceiroRepository.Callback<Usuario> callback
    ) {
        repository.buscarUsuarioPorId(
                usuarioId,
                callback
        );
    }

    public void editarPerfil(
            String nome,
            String email,
            String senhaAtual,
            String novaSenha,
            FinanceiroRepository.Callback<Usuario> callback
    ) {
        repository.editarPerfil(
                nome,
                email,
                senhaAtual,
                novaSenha,
                callback
        );
    }

    public void alterarFotoPerfil(
            String fotoUri,
            FinanceiroRepository.Callback<Usuario> callback
    ) {

        repository.alterarFotoPerfil(
                fotoUri,
                callback
        );
    }

}
