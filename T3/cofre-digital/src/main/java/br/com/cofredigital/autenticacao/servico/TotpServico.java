package br.com.cofredigital.autenticacao.servico;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class TotpServico {

    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;

    public TotpServico() {
        this.secretGenerator = new DefaultSecretGenerator();
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    public String gerarChaveSecreta() {
        return secretGenerator.generate();
    }

    public boolean validarCodigo(String chaveSecreta, String codigo) {
        return codeVerifier.isValidCode(chaveSecreta, codigo);
    }

    public String gerarUrlQRCode(String chaveSecreta, String email) {
        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(chaveSecreta)
                .issuer("Cofre Digital")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        byte[] qrCodeImage;
        try {
            qrCodeImage = qrGenerator.generate(qrData);
            return getDataUriForImage(qrCodeImage, qrGenerator.getImageMimeType());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar QR Code", e);
        }
    }
} 