package com.collarmc.security.jce;

import com.collarmc.security.messages.CipherException;
import com.collarmc.security.messages.CipherException.UnavailableCipherException;
import com.collarmc.utils.Utils;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public final class JCECipher {

    private static final String KEY_ALGORITHM = "ECDSA";
    private static final String CURVE_NAME = "curve25519";
    private static final String CIPHER_NAME = "ECIES";
    private static final String SIGNATURE_NAME = "SHA256withECDSA";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        KeyPair bobKeyPair = generateKeyPair();
        KeyPair aliceKeyPair = generateKeyPair();

        String message = "hello world";

        byte[] cipherText = encrypt(message.getBytes(StandardCharsets.UTF_8), aliceKeyPair.getPublic());
        byte[] plainText = decrypt(cipherText, aliceKeyPair.getPrivate());

        System.out.println(new String(plainText, StandardCharsets.UTF_8));

        byte[] signature = sign(message.getBytes(StandardCharsets.UTF_8), bobKeyPair.getPrivate());
        verify(message.getBytes(StandardCharsets.UTF_8), signature, bobKeyPair.getPublic());
        verify(message.getBytes(StandardCharsets.UTF_8), signature, aliceKeyPair.getPublic());
    }

    public static byte[] sign(byte[] bytes, PrivateKey privateKey) throws CipherException {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_NAME, BouncyCastleProvider.PROVIDER_NAME);
            signature.initSign(privateKey, Utils.secureRandom());
            signature.update(bytes);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new CipherException("signature generation failed", e);
        }
    }

    public static void verify(byte[] bytes, byte[] sig, PublicKey publicKey) throws CipherException {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_NAME, BouncyCastleProvider.PROVIDER_NAME);
            signature.initVerify(publicKey);
            signature.update(bytes);
            if (!signature.verify(sig)) {
                throw new CipherException("message signature could not be verified");
            }
        } catch (GeneralSecurityException e) {
            throw new CipherException("signature verification failed", e);
        }
    }

    public static byte[] decrypt(byte[] cipherText, PrivateKey privateKey) throws CipherException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_NAME, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            cipher.update(cipherText);
            return cipher.doFinal();
        } catch (GeneralSecurityException e) {
            throw new CipherException("decryption failed", e);
        }
    }

    public static byte[] encrypt(byte[] plainText, PublicKey publicKey) throws CipherException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_NAME, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, Utils.secureRandom());
            cipher.update(plainText);
            return cipher.doFinal();
        } catch (GeneralSecurityException e) {
            throw new CipherException("encrypt failed");
        }
    }

    public static KeyPair generateKeyPair() throws UnavailableCipherException {
        X9ECParameters ecP = CustomNamedCurves.getByName(CURVE_NAME);
        ECParameterSpec ecSpec = EC5Util.convertToSpec(ecP);
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            keyPairGenerator.initialize(ecSpec, Utils.secureRandom());
        } catch (GeneralSecurityException e) {
            throw new UnavailableCipherException("could not generate keypair", e);
        }
        return keyPairGenerator.generateKeyPair();
    }

    public static KeyPair keyPair(byte[] publicKey, byte[] privateKey) throws CipherException {
        return new KeyPair(publicKey(publicKey), privateKey(privateKey));
    }

    public static PublicKey publicKey(byte[] bytes) throws CipherException {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CipherException("public key deserialization failed", e);
        }
    }

    public static PrivateKey privateKey(byte[] bytes) throws CipherException {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CipherException("private key deserialization failed", e);
        }
    }

    private JCECipher() {}
}