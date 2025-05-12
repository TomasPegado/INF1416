# Plano de Implementação - Cofre Digital (TDD, Desktop/Swing)

## 1. Configuração do Projeto

- [x] Criar estrutura básica do projeto Java (diretórios, pacotes)
- [x] Configurar dependências necessárias (pom.xml: ZXing, BouncyCastle, etc.)
- [x] Configurar framework de testes (JUnit, Mockito)
- [x] Definir pacotes principais da aplicação
- [x] Configurar ambiente de desenvolvimento

## 2. Módulo de Autenticação

### 2.1 Cadastro e Gerenciamento de Usuários
- [x] Implementar classe de modelo `Usuario`
- [x] Implementar serviço de gerenciamento de usuários (em memória ou arquivo)
- [x] Desenvolver funcionalidade de cadastro de novo usuário (GUI)
- [x] Desenvolver funcionalidade de login de usuário (GUI)

### 2.2 Autenticação Bi-fator (TOTP)
- [x] Implementar sistema de autenticação por senha
- [x] Integrar biblioteca TOTP para segunda etapa de autenticação
- [x] Desenvolver gerador de QR Code para configuração inicial do TOTP
- [x] Implementar integração dos dois fatores de autenticação (lógica backend)

### 2.3 Teclado Virtual
- [ ] Implementar lógica de teclas sobrecarregadas
- [ ] Desenvolver componente Swing do teclado virtual (JPanel customizado)
- [ ] Integrar teclado virtual com tela de autenticação
- [ ] Adicionar medidas anti-keylogger

## 3. Módulo de Armazenamento Seguro

### 3.1 Criptografia
- [ ] Implementar gerenciador de chaves criptográficas
- [ ] Desenvolver sistema de criptografia de arquivos (AES)
- [ ] Implementar assinatura digital de arquivos
- [ ] Criar sistema de verificação de integridade com hashes

### 3.2 Gestão de Arquivos
- [ ] Implementar classe de modelo `Arquivo`
- [ ] Implementar serviço de metadados de arquivos (em memória ou arquivo)
- [ ] Desenvolver serviço de upload e criptografia automática
- [ ] Desenvolver serviço de download e descriptografia automática
- [ ] Adicionar funcionalidades de renomear, mover e excluir arquivos

## 4. Módulo de Controle de Acesso

- [ ] Implementar sistema de níveis de acesso
- [ ] Desenvolver controle de permissões por usuário/grupo
- [ ] Implementar controle de acesso por arquivo/pasta
- [ ] Criar interface Swing para gestão de permissões

## 5. Módulo de Auditoria

- [ ] Implementar classe de modelo `Evento`
- [ ] Implementar serviço de eventos de auditoria (em memória ou arquivo)
- [ ] Desenvolver sistema de registro de atividades
- [ ] Implementar gerador de relatórios de auditoria
- [ ] Adicionar monitoramento de tentativas de invasão

## 6. Interface Gráfica (Swing/JPanel)

### 6.1 Telas de Autenticação
- [ ] Criar protótipos e mockups das telas de autenticação (Swing)
- [ ] Implementar tela de login com teclado virtual (JPanel)
- [ ] Implementar tela de verificação TOTP (JPanel)
- [ ] Implementar tela de cadastro de novo usuário (JPanel)
- [ ] Implementar tela de recuperação de acesso (JPanel)

### 6.2 Telas Principais
- [ ] Criar protótipos e mockups das telas principais (Swing)
- [ ] Implementar dashboard principal (JPanel)
- [ ] Implementar visualizador de arquivos do cofre (JPanel)
- [ ] Implementar interface de upload/download de arquivos (JPanel)
- [ ] Implementar painel de administração (JPanel)

## 7. Integração e Sistema

- [ ] Verificar integração entre autenticação, armazenamento, controle de acesso e auditoria
- [ ] Desenvolver testes de fluxo completo da aplicação (manual ou automatizado)
- [ ] Verificar resistência a ataques comuns
- [ ] Testar segurança da criptografia implementada
- [ ] Validar proteção contra keyloggers

## 8. Finalização e Documentação

- [ ] Revisar código e corrigir bugs
- [ ] Otimizar performance
- [ ] Documentar arquitetura e código
- [ ] Criar manual do usuário
- [ ] Preparar apresentação do projeto

## Estrutura do Projeto 