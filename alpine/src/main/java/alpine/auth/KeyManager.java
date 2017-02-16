/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.auth;

import alpine.Config;
import alpine.logging.Logger;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Class that manages Alpine-generated default private, public, and secret keys.
 *
 * @since 1.0.0
 */
public class KeyManager {

    enum KeyType {
        PRIVATE,
        PUBLIC,
        SECRET
    }

    private static final Logger logger = Logger.getLogger(KeyManager.class);
    private static final KeyManager instance = new KeyManager();
    private KeyPair keyPair;
    private SecretKey secretKey;

    private KeyManager() {
        initialize();
    }

    /**
     * Returns an instance of the KeyManager
     *
     * @since 1.0.0
     */
    public static KeyManager getInstance() {
        return instance;
    }

    private void initialize() {
        if (keyPair == null) {
            try {
                loadKeyPair();
            } catch (IOException | NoSuchAlgorithmException |InvalidKeySpecException e) {
                logger.error("An error occurred loading key pair");
                logger.error(e.getMessage());
            }
        }
        if (secretKey == null) {
            try {
                loadSecretKey();
            } catch (IOException | ClassNotFoundException e) {
                logger.error("An error occurred loading secret key");
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Generates a key pair
     *
     * @since 1.0.0
     */
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        logger.info("Generating new key pair");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGen.initialize(4096, random);
        return keyGen.generateKeyPair();
    }

    /**
     * Generates a secret key\
     *
     * @since 1.0.0
     */
    public SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGen.init(256, random);
        return keyGen.generateKey();
    }

    private File getKeyPath(KeyType keyType) {
        return new File(Config.getInstance().getDataDirectorty() + File.separator +
                "keys" + File.separator +
                keyType.name().toLowerCase() + ".key");
    }

    private File getKeyPath(Key key) {
        KeyType keyType = null;
        if (key instanceof PrivateKey) {
            keyType = KeyType.PRIVATE;
        } else if (key instanceof PublicKey) {
            keyType = KeyType.PUBLIC;
        } else if (key instanceof SecretKey) {
            keyType = KeyType.SECRET;
        }
        return getKeyPath(keyType);
    }

    /**
     * Saves a key pair
     *
     * @since 1.0.0
     */
    public void save(KeyPair keyPair) throws IOException {
        logger.info("Saving key pair");
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Store Public Key
        File publicKeyFile = getKeyPath(publicKey);
        publicKeyFile.getParentFile().mkdirs(); // make directories if they do not exist
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        try (FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
            fos.write(x509EncodedKeySpec.getEncoded());
        }

        // Store Private Key.
        File privateKeyFile = getKeyPath(privateKey);
        privateKeyFile.getParentFile().mkdirs(); // make directories if they do not exist
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        try (FileOutputStream fos = new FileOutputStream(privateKeyFile)) {
            fos.write(pkcs8EncodedKeySpec.getEncoded());
        }
    }

    /**
     * Saves a secret key
     *
     * @since 1.0.0
     */
    public void save(SecretKey key) throws IOException {
        File keyFile = getKeyPath(key);
        keyFile.getParentFile().mkdirs(); // make directories if they do not exist
        try (FileOutputStream fos = new FileOutputStream(keyFile);
             ObjectOutputStream oout = new ObjectOutputStream(fos)) {
            oout.writeObject(key);
        }
    }

    /**
     * Loads a key pair
     */
    private KeyPair loadKeyPair() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Read Private Key
        File filePrivateKey = getKeyPath(KeyType.PRIVATE);

        // Read Public Key
        File filePublicKey = getKeyPath(KeyType.PUBLIC);

        byte[] encodedPrivateKey;
        byte[] encodedPublicKey;

        try (FileInputStream pvtfis = new FileInputStream(filePrivateKey);
             FileInputStream pubfis = new FileInputStream(filePublicKey)) {

            encodedPrivateKey = new byte[(int) filePrivateKey.length()];
            pvtfis.read(encodedPrivateKey);

            encodedPublicKey = new byte[(int) filePublicKey.length()];
            pubfis.read(encodedPublicKey);
        }

        // Generate KeyPair
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return this.keyPair = new KeyPair(publicKey, privateKey);
    }

    private SecretKey loadSecretKey() throws IOException, ClassNotFoundException {
        File file = getKeyPath(KeyType.SECRET);
        SecretKey key;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            key = (SecretKey) ois.readObject();
        }
        return this.secretKey = key;
    }

    /**
     * Checks to see if the key pair exists. Both (public and private) need to exist to return true
     *
     * @since 1.0.0
     */
    public boolean keyPairExists() {
        return (getKeyPath(KeyType.PUBLIC).exists() && getKeyPath(KeyType.PRIVATE).exists());
    }

    /**
     * Checks to see if the secret key exists.
     *
     * @since 1.0.0
     */
    public boolean secretKeyExists() {
        return getKeyPath(KeyType.SECRET).exists();
    }

    /**
     * Returns the keypair.
     *
     * @since 1.0.0
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /**
     * Returns only the public key.
     *
     * @since 1.0.0
     */
    public PublicKey getPublicKey() {
        return (keyPair != null) ? keyPair.getPublic() : null;
    }

    /**
     * Returns only the private key.
     *
     * @since 1.0.0
     */
    public PrivateKey getPrivateKey() {
        return (keyPair != null) ? keyPair.getPrivate() : null;
    }

    /**
     * Returns the secret key.
     *
     * @since 1.0.0
     */
    public SecretKey getSecretKey() {
        return secretKey;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

}
