package com.niu.netty.rpc.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.xml.crypto.Data;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 牛贞昊（niuzhenhao@58.com）
 * @date 2019/7/30 22:54
 * @desc
 */
public class NiuRsaUtil {
    private static final String KEY_ALGORITHM = "RSA";

    private static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    private static final String PUBLIC_KEY = "RSAPublicKey";

    private static final String PRIVATE_KEY = "RSAPrivateKey";

    private static final int MAX_ENCRYPT_BLOCK = 117;

    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * <p>
     * 生成密钥对
     * </p>
     *
     * @return
     * @throws Exception
     */
    public static Map<String, Object> getKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        Map<String, Object> keyMap = new HashMap<>(2);
        keyMap.put(PUBLIC_KEY, publicKey);
        keyMap.put(PRIVATE_KEY, privateKey);
        return keyMap;

    }

    /**
     * <p>
     *     用私钥对信息生成数字签名
     * </p>
     *
     *
     * @param data 已加密数据
     * @param privateKey 私钥
     * @return
     * @throws Exception
     */
    public static String sign(byte[] data, String privateKey) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(privateKey.getBytes("UTF-8"));
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey privateK = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateK);
        signature.update(data);
        return new String(Base64.encodeBase64(signature.sign()));
    }

    /**
     * <p>
     *     校验数字签名
     * </p>
     * @param data 已加密数据
     * @param publicKey 公钥
     * @param sign 签名
     * @return
     * @throws Exception
     */
    public static boolean verify(byte[] data, String publicKey, String sign) throws Exception{
        byte[] keyBytes = Base64.decodeBase64(publicKey.getBytes("UTF-8"));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey publicK = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicK);
        signature.update(data);
        return signature.verify(Base64.decodeBase64(sign.getBytes("UTF-8")));
    }

    /**
     * <p>
     *     私钥解密
     * </p>
     * @param encryptedData
     * @param privateKey
     * @return
     * @throws Exception
     */

    public static byte[] decryptByPrivateKey(byte[] encryptedData, String privateKey) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(privateKey.getBytes("UTF-8"));
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key privateK = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        return decrypt(encryptedData, privateK, keyFactory);
    }

    /**
     * <p>
     *  公钥解密
     * </p>
     * @param data
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPublicKey(byte[] data, String publicKey) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(publicKey.getBytes("UTF-8"));
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key privateK = keyFactory.generatePublic(x509EncodedKeySpec);
        return decrypt(data, privateK, keyFactory);
    }

    /**
     * 解密
     * @param data
     * @param key
     * @param keyFactory
     * @return
     * @throws Exception
     */
    private static byte[] decrypt(byte[] data, Key key, KeyFactory keyFactory) throws Exception{
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, key);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int inputLength = data.length;
        int offset = 0;
        byte[] cache;
        int i = 0;
        while (inputLength - offset > 0) {
            if (inputLength - offset > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(data, offset, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offset, inputLength - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    /**
     * <p>
     *     获取私钥
     * </p>
     * @param keyMap
     * @return
     * @throws Exception
     */
    public static String getPrivateKey(Map<String, Object> keyMap) throws Exception {
        Key key = (Key) keyMap.get(PRIVATE_KEY);
        return new String (Base64.encodeBase64(key.getEncoded()), "UTF-8");
    }

    /**
     * <p>
     *     获取私钥
     * </p>
     * @param keyMap
     * @return
     * @throws Exception
     */
    public static String getPublicKey(Map<String, Object> keyMap) throws Exception {
        Key key = (Key) keyMap.get(PUBLIC_KEY);
        return new String (Base64.encodeBase64(key.getEncoded()), "UTF-8");
    }

    /**
     * <p>
     * 公钥加密
     * </p>
     * @param data
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static byte[] encryptByPublicKey(byte[] data, String publicKey) throws Exception{
        byte[] keyBytes = Base64.decodeBase64(publicKey.getBytes("UTF-8"));
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicK = keyFactory.generatePublic(x509EncodedKeySpec);
        return encrypt(data, publicK,  keyFactory);
    }

    public static byte[] encryptByPrivateKey(byte[] data, String privateKey) throws Exception {
        byte[] bytes = Base64.decodeBase64(privateKey.getBytes());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key privateK = factory.generatePrivate(keySpec);
        //对数据进行加密
        return encrypt(data, privateK, factory);
    }

    private static byte[] encrypt(byte[] data, Key key, KeyFactory factory) throws Exception{
        //对数据加密
        Cipher cipher = Cipher.getInstance(factory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, key);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;
        while (inputLen - offset > 0) {
            if (inputLen - offset > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offset, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offset, inputLen - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }

}
