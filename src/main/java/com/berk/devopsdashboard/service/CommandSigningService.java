package com.berk.devopsdashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Service
@Slf4j
public class CommandSigningService {

    private final String privateKeyPath = "certs/signing_private.der";

    public String sign(String command) {
        try {
            File keyFile = new File(privateKeyPath);
            if (!keyFile.exists()) {
                log.warn("Signing key not found! Commands will not be signed.");
                return null;
            }

            byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(spec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(command.getBytes());

            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            log.error("Command signing failed: {}", e.getMessage());
            return null;
        }
    }
}
