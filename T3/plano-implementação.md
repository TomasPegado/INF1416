# Plano de Implementação - Cofre Digital (TDD, Desktop/Swing)
- O objetivo do Trabalho 3 (T3)  é implementar uma aplicação de um cofre digital (digital vault) que utiliza uma pasta segura para armazenar arquivos com controle de integridade, autenticidade, confidencialidade (sigilo) e acesso. A aplicação deve utilizar um sistema de controle de autenticação de usuários bi-fator para validar a senha pessoal do usuário (algo que o usuário conhece) e um iToken TOTP (algo que o usuário possui). A entrada da senha pessoal deve utilizar um teclado virtual com teclas sobrecarregadas para prevenir ataques de keyloggers. Essa aplicação também deve registrar todas as atividades do usuário para possibilitar a auditoria de uso do sistema. 

- Este pacote possui duas pastas: Keys, com chaves e certificados do administrador e dos usuários do sistema; e Files, pasta segura. A frase secreta da chave privada do administrador é "admin". E, a frase secreta da chave privada do usuário 1 é "user01" e do usuário 2 é "user02". Utilize esse pacote para testar as suas rotinas de verificação de integridade, autenticidade e sigilo, e controle de acesso ao sistema.


## 1. Configuração do Projeto

- [x] Criar estrutura básica do projeto Java (diretórios, pacotes)
- [x] Configurar dependências necessárias (pom.xml: ZXing, BouncyCastle, etc.)
- [x] Configurar framework de testes (JUnit, Mockito)
- [x] Definir pacotes principais da aplicação
- [x] Configurar ambiente de desenvolvimento

## 2. Módulo de Autenticação

### 2.1 Cadastro e Gerenciamento de Usuários
- [x] Implementar classe de modelo `Usuario`
- [x] Implementar painel de cadastro Swing (`CadastroUsuarioPanel`)
- [x] Implementar serviço de gerenciamento de usuários (em memória ou arquivo)
- [x] Implementar painel de login Swing (`LoginPanel`)
- [x] Implementar validação e armazenamento de certificado digital e chave privada (PKCS8/X.509, UID/KID, formato seguro, criptografia AES-256, frase secreta)
- [x] Implementar validação da frase secreta da chave privada (assinatura digital de array aleatório)
- [x] Implementar armazenamento do certificado digital em PEM e chave privada criptografada na base de dados (tabela Chaveiro)
- [x] Implementar geração e armazenamento do KID e associação ao UID do usuário
- [x] Implementar armazenamento da senha pessoal com hash bcrypt (2y, custo 8, salt/hash em BASE64)

### 2.2 Autenticação Bi-fator (TOTP)
- [x] Implementar sistema de autenticação por senha
- [x] Implementar interface de autenticação por senha (já integrado ao painel de login)
- [ ] Implementar geração de chave TOTP (20 bytes aleatórios, BASE32, criptografada com AES-256 derivada da senha pessoal)
- [ ] Implementar armazenamento seguro da chave TOTP no registro do usuário
- [ ] Implementar classe TOTP própria (sem libs externas, conforme especificação)
- [x] Implementar interface de exibição do segredo TOTP (BASE32) e QRCode opcional
- [x] Integrar biblioteca TOTP para segunda etapa de autenticação
- [x] Implementar painel/interface de verificação TOTP
- [x] Desenvolver gerador de QR Code para configuração inicial do TOTP
- [x] Implementar interface de exibição do QR Code
- [x] Implementar integração dos dois fatores de autenticação (lógica backend)
- [x] Implementar integração dos dois fatores de autenticação (interface)

### 2.3 Teclado Virtual
- [x] Implementar lógica de teclas sobrecarregadas
- [x] Implementar painel Swing do teclado virtual (JPanel customizado)
- [x] Integrar teclado virtual com tela de autenticação (backend)
- [x] Integrar teclado virtual com tela de autenticação (interface)
- [x] Adicionar medidas anti-keylogger (backend)
- [x] Adicionar medidas anti-keylogger (interface)

## 3. Módulo de Armazenamento Seguro

### 3.1 Criptografia e Indexação
- [x] Implementar gerenciador de chaves criptográficas (AES-256, SHA1PRNG, derivação de senha/frase secreta)
- [ ] Implementar sistema de criptografia de arquivos (AES-256/ECB/PKCS5Padding)
- [ ] Implementar geração e validação de envelope digital para arquivos e índice (proteção da semente SHA1PRNG)
- [ ] Implementar assinatura digital de arquivos e índice (Signature, chave assimétrica do usuário)
- [ ] Implementar verificação de integridade e autenticidade de arquivos e índice
- [ ] Implementar armazenamento e leitura dos arquivos: .enc (criptografado), .env (envelope), .asd (assinatura)
- [ ] Implementar interface de upload/download de arquivos secretos
- [ ] Implementar interface de visualização/validação de assinatura e integridade

### 3.2 Gestão de Arquivos
- [ ] Implementar classe de modelo `Arquivo` (nome código, nome secreto, dono, grupo)
- [ ] Implementar painel de visualização/gerenciamento de arquivos
- [ ] Implementar serviço de metadados de arquivos (em memória ou arquivo, leitura do index.enc)
- [ ] Implementar interface de gerenciamento de metadados
- [ ] Desenvolver serviço de upload e criptografia automática
- [ ] Implementar interface de upload
- [ ] Desenvolver serviço de download e descriptografia automática
- [ ] Implementar interface de download
- [ ] Adicionar funcionalidades de renomear, mover e excluir arquivos
- [ ] Implementar interface correspondente

## 4. Módulo de Controle de Acesso

- [ ] Implementar sistema de níveis de acesso (dono, grupo, admin)
- [ ] Implementar política de acesso: apenas o dono pode acessar/decriptar arquivos
- [ ] Implementar interface de configuração de níveis de acesso
- [ ] Desenvolver controle de permissões por usuário/grupo
- [ ] Implementar interface de permissões
- [ ] Implementar controle de acesso por arquivo/pasta
- [ ] Implementar interface de controle de acesso
- [ ] Criar interface Swing para gestão de permissões

## 5. Módulo de Auditoria e Logs

- [x] Implementar classe de modelo `Evento` (registro de operações)
- [ ] Implementar painel de visualização de eventos
- [x] Implementar serviço de eventos de auditoria (em memória ou arquivo)
- [ ] Implementar interface de consulta de eventos
- [x] Implementar sistema de registro de atividades conforme tabela de mensagens (RID, MID, data/hora, usuário, arquivo)
- [ ] Implementar interface de relatórios de atividades
- [ ] Implementar gerador de relatórios de auditoria
- [ ] Implementar interface de exportação/visualização de relatórios
- [ ] Adicionar monitoramento de tentativas de invasão
- [ ] Implementar interface de alertas
- [ ] Implementar programa de apoio logView para visualização dos registros (ordem cronológica, apenas leitura)

## 6. Banco de Dados e Integração

- [x] Implementar estrutura das tabelas: Usuarios, Chaveiro, Grupos, Mensagens, Registros
- [x] Garantir relacionamento correto entre UID, KID, GID, MID, RID
- [x] Implementar persistência e recuperação dos dados conforme especificação
- [ ] Verificar integração entre autenticação, armazenamento, controle de acesso e auditoria (backend)
- [ ] Verificar integração entre autenticação, armazenamento, controle de acesso e auditoria (interface)
- [ ] Desenvolver testes de fluxo completo da aplicação (manual ou automatizado)
- [ ] Verificar resistência a ataques comuns
- [ ] Testar segurança da criptografia implementada
- [ ] Validar proteção contra keyloggers

## 7. Finalização e Documentação

- [ ] Revisar código e corrigir bugs
- [ ] Otimizar performance
- [ ] Documentar arquitetura e código
- [ ] Criar manual do usuário
- [ ] Preparar apresentação do projeto