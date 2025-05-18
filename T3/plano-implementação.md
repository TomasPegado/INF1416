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

### 2.2 Autenticação Bi-fator (TOTP)
- [x] Implementar sistema de autenticação por senha
- [x] Implementar interface de autenticação por senha (já integrado ao painel de login)
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

### 3.1 Criptografia
- [ ] Implementar gerenciador de chaves criptográficas
- [ ] Implementar interface de gerenciamento de chaves (se necessário)
- [ ] Desenvolver sistema de criptografia de arquivos (AES)
- [ ] Implementar interface de upload/download
- [ ] Implementar assinatura digital de arquivos
- [ ] Implementar interface de visualização/validação de assinatura
- [ ] Criar sistema de verificação de integridade com hashes
- [ ] Implementar interface de visualização/validação de integridade

### 3.2 Gestão de Arquivos
- [ ] Implementar classe de modelo `Arquivo`
- [ ] Implementar painel de visualização/gerenciamento de arquivos
- [ ] Implementar serviço de metadados de arquivos (em memória ou arquivo)
- [ ] Implementar interface de gerenciamento de metadados
- [ ] Desenvolver serviço de upload e criptografia automática
- [ ] Implementar interface de upload
- [ ] Desenvolver serviço de download e descriptografia automática
- [ ] Implementar interface de download
- [ ] Adicionar funcionalidades de renomear, mover e excluir arquivos
- [ ] Implementar interface correspondente

## 4. Módulo de Controle de Acesso

- [ ] Implementar sistema de níveis de acesso
- [ ] Implementar interface de configuração de níveis de acesso
- [ ] Desenvolver controle de permissões por usuário/grupo
- [ ] Implementar interface de permissões
- [ ] Implementar controle de acesso por arquivo/pasta
- [ ] Implementar interface de controle de acesso
- [ ] Criar interface Swing para gestão de permissões

## 5. Módulo de Auditoria

- [ ] Implementar classe de modelo `Evento`
- [ ] Implementar painel de visualização de eventos
- [ ] Implementar serviço de eventos de auditoria (em memória ou arquivo)
- [ ] Implementar interface de consulta de eventos
- [ ] Desenvolver sistema de registro de atividades
- [ ] Implementar interface de relatórios de atividades
- [ ] Implementar gerador de relatórios de auditoria
- [ ] Implementar interface de exportação/visualização de relatórios
- [ ] Adicionar monitoramento de tentativas de invasão
- [ ] Implementar interface de alertas


## 6. Integração e Sistema

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