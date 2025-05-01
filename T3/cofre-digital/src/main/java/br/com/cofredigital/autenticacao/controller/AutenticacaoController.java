package br.com.cofredigital.autenticacao.controller;

import br.com.cofredigital.autenticacao.dto.CadastroRequest;
import br.com.cofredigital.autenticacao.dto.LoginRequest;
import br.com.cofredigital.autenticacao.dto.TecladoVirtualResponse;
import br.com.cofredigital.autenticacao.dto.VerificarTotpRequest;
import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.TecladoVirtualServico;
import br.com.cofredigital.autenticacao.servico.TOTPServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final UsuarioServico usuarioServico;
    private final TotpServico totpServico;
    private final TecladoVirtualServico tecladoVirtualServico;
    private final AuthenticationManager authenticationManager;
    
    // Mapa para armazenar temporariamente os layouts de teclado por sessão
    private final Map<String, Map<String, List<Character>>> layoutsPorSessao = new HashMap<>();

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrarUsuario(@RequestBody CadastroRequest cadastroRequest) {
        Usuario usuario = new Usuario();
        usuario.setNome(cadastroRequest.getNome());
        usuario.setEmail(cadastroRequest.getEmail());
        usuario.setSenha(cadastroRequest.getSenha());
        
        Usuario usuarioCadastrado = usuarioServico.cadastrarUsuario(usuario);
        
        // Gera QR Code para configuração do TOTP
        String qrCodeUrl = totpServico.gerarUrlQRCode(
                usuarioCadastrado.getChaveSecretaTotp(), 
                usuarioCadastrado.getEmail()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("mensagem", "Usuário cadastrado com sucesso");
        response.put("qrCodeUrl", qrCodeUrl);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teclado-virtual")
    public ResponseEntity<TecladoVirtualResponse> obterTecladoVirtual() {
        // Gera um layout aleatório para o teclado virtual
        Map<String, List<Character>> layout = tecladoVirtualServico.gerarLayoutAleatorio();
        
        // Gera um ID de sessão único
        String sessaoId = UUID.randomUUID().toString();
        
        // Armazena o layout para uso posterior na validação
        layoutsPorSessao.put(sessaoId, layout);
        
        // Retorna apenas as teclas, não os caracteres (para segurança)
        TecladoVirtualResponse response = new TecladoVirtualResponse();
        response.setSessaoId(sessaoId);
        response.setTeclas(layout.keySet());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/primeira-etapa")
    public ResponseEntity<?> loginPrimeiraEtapa(@RequestBody LoginRequest loginRequest) {
        // Recupera o layout do teclado virtual para a sessão
        Map<String, List<Character>> layout = layoutsPorSessao.get(loginRequest.getSessaoId());
        if (layout == null) {
            return ResponseEntity.badRequest().body("Sessão inválida ou expirada");
        }
        
        // Processa a entrada do teclado virtual
        String senhaProcessada = tecladoVirtualServico.processarEntrada(layout, loginRequest.getSequenciaTeclas());
        
        try {
            // Tenta autenticar o usuário
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), senhaProcessada)
            );
            
            // Armazena a autenticação no contexto de segurança
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Limpa o layout da sessão após o uso
            layoutsPorSessao.remove(loginRequest.getSessaoId());
            
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Primeira etapa de autenticação concluída com sucesso",
                    "aguardandoTotp", true
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Credenciais inválidas");
        }
    }

    @PostMapping("/login/segunda-etapa")
    public ResponseEntity<?> loginSegundaEtapa(@RequestBody VerificarTotpRequest request) {
        // Obtém o usuário autenticado na primeira etapa
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        Usuario usuario = usuarioServico.buscarPorEmail(email);
        
        // Verifica o código TOTP
        boolean codigoValido = totpServico.validarCodigo(
                usuario.getChaveSecretaTotp(), 
                request.getCodigoTotp()
        );
        
        if (codigoValido) {
            // Gera token JWT ou outro mecanismo de sessão
            String token = "token-jwt-simulado"; // Em uma implementação real, gerar um JWT
            
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Autenticação concluída com sucesso",
                    "token", token
            ));
        } else {
            return ResponseEntity.badRequest().body("Código TOTP inválido");
        }
    }
} 