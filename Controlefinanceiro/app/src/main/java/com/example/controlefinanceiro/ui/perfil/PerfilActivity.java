package com.example.controlefinanceiro.ui.perfil;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import android.net.Uri;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.controlefinanceiro.R;
import com.example.controlefinanceiro.data.entity.Sessao;
import com.example.controlefinanceiro.data.entity.Usuario;
import com.example.controlefinanceiro.data.repository.FinanceiroRepository;
import com.example.controlefinanceiro.ui.auth.LoginActivity;
import com.example.controlefinanceiro.viewmodel.AuthViewModel;

public class PerfilActivity extends AppCompatActivity {

    private EditText edtNome;
    private EditText edtEmail;
    private EditText edtSenhaAtual;
    private EditText edtNovaSenha;
    private EditText edtConfirmarNovaSenha;

    private Button btnSalvar;
    private ProgressBar progresso;

    private AuthViewModel authViewModel;
    private boolean processando = false;


    private ImageView imgFotoPerfil;

    private Button btnTirarFoto;
    private Button btnEscolherGaleria;

    private Uri fotoPendente;

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    sucesso -> {

                        if (Boolean.TRUE.equals(sucesso)
                                && fotoPendente != null) {

                            salvarFoto(fotoPendente);

                        } else {

                            Toast.makeText(
                                    this,
                                    "Foto cancelada.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    private final ActivityResultLauncher<String[]> galeriaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {

                        if (uri == null) {
                            return;
                        }

                        try {

                            getContentResolver()
                                    .takePersistableUriPermission(
                                            uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    );

                            salvarFoto(uri);

                        } catch (SecurityException erro) {

                            Toast.makeText(
                                    this,
                                    "Não foi possível acessar a imagem.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);


        imgFotoPerfil =
                findViewById(R.id.imgFotoPerfil);

        btnTirarFoto =
                findViewById(R.id.btnTirarFoto);

        btnEscolherGaleria =
                findViewById(R.id.btnEscolherGaleria);

        btnTirarFoto.setEnabled(false);
        btnEscolherGaleria.setEnabled(false);

        btnTirarFoto.setOnClickListener(
                v -> abrirCamera()
        );

        btnEscolherGaleria.setOnClickListener(
                v -> abrirGaleria()
        );

        if (savedInstanceState != null) {

            String uriPendente = savedInstanceState
                    .getString("foto_pendente");

            if (uriPendente != null) {
                fotoPendente = Uri.parse(uriPendente);
            }
        }


        edtNome = findViewById(R.id.edtNomePerfil);
        edtEmail = findViewById(R.id.edtEmailPerfil);
        edtSenhaAtual = findViewById(R.id.edtSenhaAtualPerfil);
        edtNovaSenha = findViewById(R.id.edtNovaSenhaPerfil);
        edtConfirmarNovaSenha =
                findViewById(R.id.edtConfirmarNovaSenhaPerfil);

        btnSalvar = findViewById(R.id.btnSalvarPerfil);
        progresso = findViewById(R.id.progressoPerfil);

        authViewModel = new ViewModelProvider(this)
                .get(AuthViewModel.class);

        btnSalvar.setEnabled(false);
        btnSalvar.setOnClickListener(v -> salvarPerfil());

        carregarPerfil();
    }

    private void carregarPerfil() {

        progresso.setVisibility(View.VISIBLE);

        authViewModel.consultarSessao(
                new FinanceiroRepository.Callback<Sessao>() {

                    @Override
                    public void onSucesso(Sessao sessao) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (sessao == null) {
                            abrirLogin();
                            return;
                        }

                        buscarUsuario(sessao.getUsuarioId());
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        abrirLogin();
                    }
                }
        );
    }

    private void buscarUsuario(int usuarioId) {

        authViewModel.buscarUsuario(
                usuarioId,
                new FinanceiroRepository.Callback<Usuario>() {

                    @Override
                    public void onSucesso(Usuario usuario) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        progresso.setVisibility(View.GONE);

                        if (usuario == null) {
                            abrirLogin();
                            return;
                        }

                        edtNome.setText(usuario.getNome());
                        edtEmail.setText(usuario.getEmail());
                        btnSalvar.setEnabled(true);

                        btnTirarFoto.setEnabled(true);
                        btnEscolherGaleria.setEnabled(true);

                        mostrarFoto(usuario.getFotoUri());

                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        progresso.setVisibility(View.GONE);

                        Toast.makeText(
                                PerfilActivity.this,
                                "Erro ao carregar o perfil.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }
                }
        );
    }

    private void salvarPerfil() {

        if (processando) {
            return;
        }

        String nome = edtNome.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String senhaAtual =
                edtSenhaAtual.getText().toString();
        String novaSenha =
                edtNovaSenha.getText().toString();
        String confirmarSenha =
                edtConfirmarNovaSenha.getText().toString();

        if (nome.isEmpty()) {
            edtNome.setError("Informe seu nome.");
            edtNome.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Informe um e-mail válido.");
            edtEmail.requestFocus();
            return;
        }

        if (senhaAtual.isEmpty()) {
            edtSenhaAtual.setError("Informe sua senha atual.");
            edtSenhaAtual.requestFocus();
            return;
        }

        if (!novaSenha.isEmpty() && novaSenha.length() < 8) {
            edtNovaSenha.setError(
                    "Use pelo menos 8 caracteres."
            );
            edtNovaSenha.requestFocus();
            return;
        }

        if (!novaSenha.equals(confirmarSenha)) {
            edtConfirmarNovaSenha.setError(
                    "As senhas não coincidem."
            );
            edtConfirmarNovaSenha.requestFocus();
            return;
        }

        setProcessando(true);

        authViewModel.editarPerfil(
                nome,
                email,
                senhaAtual,
                novaSenha,
                new FinanceiroRepository.Callback<Usuario>() {

                    @Override
                    public void onSucesso(Usuario usuario) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setProcessando(false);

                        Toast.makeText(
                                PerfilActivity.this,
                                "Perfil atualizado com sucesso!",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        setProcessando(false);

                        Toast.makeText(
                                PerfilActivity.this,
                                erro.getMessage() != null
                                        ? erro.getMessage()
                                        : "Erro ao atualizar o perfil.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void setProcessando(boolean valor) {

        processando = valor;
        btnSalvar.setEnabled(!valor);

        progresso.setVisibility(
                valor ? View.VISIBLE : View.GONE
        );
    }

    private void abrirLogin() {

        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }


    private void abrirCamera() {

        try {

            File pasta = new File(
                    getFilesDir(),
                    "fotos_perfil"
            );

            if (!pasta.exists() && !pasta.mkdirs()) {
                throw new IOException(
                        "Não foi possível criar a pasta."
                );
            }

            File arquivo = File.createTempFile(
                    "perfil_",
                    ".jpg",
                    pasta
            );

            fotoPendente = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    arquivo
            );

            cameraLauncher.launch(fotoPendente);

        } catch (Exception erro) {

            Toast.makeText(
                    this,
                    "Não foi possível abrir a câmera.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void abrirGaleria() {

        galeriaLauncher.launch(
                new String[]{"image/*"}
        );
    }

    private void salvarFoto(Uri uri) {

        if (processando) {
            return;
        }

        btnTirarFoto.setEnabled(false);
        btnEscolherGaleria.setEnabled(false);

        authViewModel.alterarFotoPerfil(
                uri.toString(),
                new FinanceiroRepository.Callback<Usuario>() {

                    @Override
                    public void onSucesso(Usuario usuario) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        btnTirarFoto.setEnabled(true);
                        btnEscolherGaleria.setEnabled(true);

                        mostrarFoto(usuario.getFotoUri());

                        Toast.makeText(
                                PerfilActivity.this,
                                "Foto atualizada com sucesso!",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        btnTirarFoto.setEnabled(true);
                        btnEscolherGaleria.setEnabled(true);

                        Toast.makeText(
                                PerfilActivity.this,
                                erro.getMessage() != null
                                        ? erro.getMessage()
                                        : "Erro ao salvar a foto.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void mostrarFoto(String uriSalva) {

        if (uriSalva == null || uriSalva.isEmpty()) {

            imgFotoPerfil.setImageResource(
                    android.R.drawable.ic_menu_camera
            );

            return;
        }

        try {

            imgFotoPerfil.setImageURI(null);

            imgFotoPerfil.setImageURI(
                    Uri.parse(uriSalva)
            );

            if (imgFotoPerfil.getDrawable() == null) {
                throw new IllegalStateException(
                        "Imagem indisponível."
                );
            }

        } catch (Exception erro) {

            imgFotoPerfil.setImageResource(
                    android.R.drawable.ic_menu_camera
            );

            Toast.makeText(
                    this,
                    "A foto salva não está mais disponível.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        if (fotoPendente != null) {

            outState.putString(
                    "foto_pendente",
                    fotoPendente.toString()
            );
        }

        super.onSaveInstanceState(outState);
    }

}
