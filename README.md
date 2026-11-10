# 🍀 Meu Pé de Meia

**Aplicativo Android para controle financeiro pessoal**  
**Trabalho AV3 — Programação para Dispositivos Móveis**  
**Universidade Federal de Mato Grosso do Sul (UFMS)**

## 👩‍💻 Integrantes

- Beatriz Vieira Gomes da Silva
- Luciana Nunes Viana
- Isabela Martins Garcia

## 📱 Sobre o projeto

O **Meu Pé de Meia** é um aplicativo de controle financeiro pessoal, desenvolvido em **Java** no **Android Studio**, que ajuda a organizar receitas, despesas e contas a pagar. O aplicativo permite acompanhar o saldo, categorizar lançamentos, consultar relatórios e receber notificações de vencimento.

Os dados financeiros são armazenados **localmente**, utilizando o banco de dados **Room**. Não é necessário um servidor externo para cadastrar ou consultar as movimentações.

## ✨ Funcionalidades

- **Cadastro e login** de usuários, com senhas protegidas por **BCrypt**.
- **Persistência de sessão**, logout e tela inicial de abertura (*Splash Screen*).
- **Edição de perfil e senha**, com foto pela **câmera** ou pela **galeria**.
- **Cadastro, edição e exclusão de receitas e despesas**.
- **Organização por categorias** e acompanhamento do **histórico financeiro**.
- **Exibição do saldo**, total de receitas e total de despesas.
- **Relatórios mensais** de movimentações.
- **Cadastro, edição e exclusão de lembretes** de contas a pagar.
- **Avisos agendados** por meio de `AlarmManager` e notificações do Android.
- **Lembretes mensais** e opção de aviso com antecedência.
- **Modos claro e escuro**, com cores adaptadas à interface.
- **Confirmação de exclusões**, validação de campos e mensagens de retorno ao usuário.

## 🛠️ Tecnologias e recursos utilizados

| Tecnologia / recurso | Aplicação no projeto |
| --- | --- |
| **Java** | Implementação do aplicativo Android nativo |
| **Android Studio** | Desenvolvimento, compilação e testes |
| **Room / SQLite** | Persistência local de usuários, movimentações, categorias, lembretes e sessão |
| **Entity, DAO e Database** | Organização e acesso aos dados |
| **Repository e ViewModel** | Separação entre interface e operações de dados |
| **LiveData** | Atualização da interface a partir de mudanças no banco |
| **ExecutorService e Handler** | Operações de banco em segundo plano e retorno dos resultados à interface |
| **BCrypt** | Proteção das senhas dos usuários |
| **FileProvider e URI** | Compartilhamento seguro de arquivos de imagem com a câmera |
| **AlarmManager e PendingIntent** | Programação de avisos de vencimento |
| **BroadcastReceiver** | Recebimento de alarmes e eventos de reinicialização |
| **NotificationChannel** | Organização das notificações no Android |

### Organização principal do código

O projeto utiliza o pacote Java **`com.example.controlefinanceiro`**. Entre os arquivos principais estão:

```text
app/src/main/java/com/example/controlefinanceiro/
├── MainActivity.java
├── SplashActivity.java
├── data/
│   ├── entity/             # Usuario, Sessao, Categoria, Movimentacao e Lembrete
│   ├── dao/                # Interfaces de acesso aos dados
│   ├── database/           # AppDatabase e migrações
│   └── repository/         # FinanceiroRepository
├── viewmodel/              # AuthViewModel e FinanceiroViewModel
├── ui/
│   ├── auth/               # Cadastro e login
│   ├── perfil/             # Perfil e foto
│   ├── movimentacao/      # Receitas e despesas
│   ├── categoria/          # Categorias
│   ├── relatorio/          # Relatórios
│   └── lembrete/           # Lembretes
├── notificacao/            # Alarmes e notificações
└── utils/                  # Utilitários, incluindo proteção de senha
```

## 📚 Conceitos das aulas aplicados

### Banco de Dados Room

As entidades representam os dados persistidos, os DAOs definem as operações de acesso e a classe `AppDatabase` configura o banco. O uso de `LiveData` permite refletir no histórico e nos totais as alterações feitas nas movimentações.

### Câmera e FileProvider

A foto do perfil pode ser capturada pela câmera do dispositivo ou selecionada da galeria. O `FileProvider` permite fornecer uma URI para compartilhamento controlado do arquivo com o aplicativo de câmera.

### Threads e Handler

O `FinanceiroRepository` utiliza `ExecutorService` para executar operações de banco fora da thread principal e `Handler` para devolver os resultados à interface. **O projeto não implementa requisições HTTP externas**; aplica o conteúdo de processamento assíncrono dessa aula.

### Alarmes e Notificações

O `AlarmManager` agenda os avisos das contas a pagar. O aplicativo utiliza `PendingIntent`, `BroadcastReceiver` e canais de notificação para emitir lembretes. Ao editar ou excluir um lembrete, o agendamento correspondente é atualizado ou cancelado.

### Segurança e Testes

As senhas são protegidas com **BCrypt**; as operações financeiras são associadas ao usuário autenticado; os formulários validam dados antes de salvar; e foram realizados testes funcionais de caixa-preta para conferir o comportamento das funcionalidades.

## 🎯 Conceito de destaque: Usabilidade

O grupo escolheu a **usabilidade** como foco principal da AV3. O objetivo foi facilitar o uso do controle financeiro, mesmo para pessoas sem experiência com aplicativos desse tipo.

Esse conceito aparece na organização da tela inicial — com saldo, receitas, despesas e histórico —, na identificação **verde para entradas** e **vermelha para saídas**, na navegação entre as funcionalidades e nas mensagens claras de validação. O aplicativo também confirma exclusões para prevenir erros, permite editar registros sem precisar recriá-los e oferece **modo claro e modo escuro** para personalização da aparência.

## ✅ Testes realizados

Foram registradas duas baterias finais de **testes funcionais de caixa-preta**, com os seguintes resultados informados pelo grupo:

| Grupo de testes | Casos | Aprovados |
| --- | ---: | ---: |
| Movimentações financeiras (M01–M06) | 6 | 6 |
| Foto de perfil (P01–P03) | 3 | 3 |
| Alarmes e notificações (N01–N04) | 4 | 4 |
| Segurança e sessão (S01–S03) | 3 | 3 |
| Relatórios e navegação (R01–R02) | 2 | 2 |
| Edição e exclusão de lembretes (L01–L07) | 7 | 7 |
| **Total** | **25** | **25** |

**Resultado informado:** 25 testes aprovados e 0 reprovados nas duas baterias, abrangendo edição e exclusão de lançamentos, persistência de dados, fotos, notificações, sessões, relatórios e cancelamento/reagendamento de lembretes.

> Os resultados acima dizem respeito aos cenários executados e registrados pelo grupo. Não substituem testes adicionais em diferentes dispositivos ou versões do Android.

## ▶️ Como executar o projeto

1. Instale uma versão compatível do **Android Studio** e configure o **Android SDK**.
2. Abra o Android Studio e escolha **Open**.
3. Selecione a **pasta raiz do projeto**, que contém os arquivos de configuração do Gradle.
4. Aguarde a sincronização do Gradle e a instalação de eventuais componentes solicitados pelo Android Studio.
5. Conecte um **celular Android com depuração USB habilitada** ou inicie um **emulador**.
6. Selecione o dispositivo na barra superior e execute a configuração **`app`** pelo botão **Run ▶**.

O aplicativo será instalado no dispositivo selecionado com o nome **Meu Pé de Meia**.

### Permissões e observações

- Em versões recentes do Android, é necessário autorizar **notificações** para receber avisos de contas.
- A câmera e a galeria são acessadas por meio dos recursos apropriados do Android.
- O agendamento utiliza `setAndAllowWhileIdle()`, que **não garante precisão absoluta** em todos os aparelhos e condições de economia de bateria.
- **Não apague os dados do aplicativo** se desejar preservar os usuários e lançamentos já cadastrados localmente.

## 📌 Apresentação do projeto

A apresentação está organizada em três partes:

1. **Beatriz:** Room e processamento com threads.
2. **Luciana:** alarmes, notificações, câmera e FileProvider.
3. **Isabela:** segurança, testes e usabilidade.

Ao final, será realizada uma **demonstração prática do aplicativo**, com navegação pelas telas, movimentações financeiras, foto de perfil, lembretes e modos de aparência.

---

**Meu Pé de Meia 🍀 — Projeto acadêmico desenvolvido para a AV3 da disciplina de Programação para Dispositivos Móveis, UFMS, 2026.**
