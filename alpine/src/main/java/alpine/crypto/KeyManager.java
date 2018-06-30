/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.crypto;

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
 * @author Steve Springett
 * @since 1.0.0
 */
public final class KeyManager {

    /**
     * Defines the type of key.
     */
    enum KeyType {
        PRIVATE,
        PUBLIC,
        SECRET
    }

    private static final Logger LOGGER = Logger.getLogger(KeyManager.class);
    private static final KeyManager INSTANCE = new KeyManager();
    private KeyPair keyPair;
    private SecretKey secretKey;

    /**
     * Private constructor.
     */
    private KeyManager() {
        initialize();
    }

    /**
     * Returns an INSTANCE of the KeyManager.
     *
     * @return an instance of the KeyManager
     * @since 1.0.0
     */
    public static KeyManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the KeyManager
     */
    private void initialize() {
        if (keyPair == null) {
            try {
                loadKeyPair();
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                LOGGER.error("An error occurred loading key pair");
                LOGGER.error(e.getMessage());
            }
        }
        if (secretKey == null) {
            try {
                loadSecretKey();
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.error("An error occurred loading secret key");
                LOGGER.error(e.getMessage());
            }
        }
    }

    /**
     * Generates a key pair.
     *
     * @return a KeyPair (public / private keys)
     * @throws NoSuchAlgorithmException if the algorithm cannot be found
     * @since 1.0.0
     */
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        LOGGER.info("Generating new key pair");
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGen.initialize(4096, random);
        return this.keyPair = keyGen.generateKeyPair();
    }

    /**
     * Generates a secret key.
     *
     * @return a SecretKey
     * @throws NoSuchAlgorithmException if the algorithm cannot be found
     * @since 1.0.0
     */
    public SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        final KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGen.init(256, random);
        return this.secretKey = keyGen.generateKey();
    }

    /**
     * Retrieves the path where the keys should be stored.
     * @param keyType the type of key
     * @return a File representing the path to the key
     */
    private File getKeyPath(KeyType keyType) {
        return new File(Config.getInstance().getDataDirectorty()
                + File.separator
                + "keys" + File.separator
                + keyType.name().toLowerCase() + ".key");
    }

    /**
     * Given the type of key, this method will return the File path to that key.
     * @param key the type of key
     * @return a File representing the path to the key
     */
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
     * Saves a key pair.
     *
     * @param keyPair the key pair to save
     * @throws IOException if the files cannot be written
     * @since 1.0.0
     */
    public void save(KeyPair keyPair) throws IOException {
        LOGGER.info("Saving key pair");
        final PrivateKey privateKey = keyPair.getPrivate();
        final PublicKey publicKey = keyPair.getPublic();

        // Store Public Key
        final File publicKeyFile = getKeyPath(publicKey);
        publicKeyFile.getParentFile().mkdirs(); // make directories if they do not exist
        final X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        try (FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
            fos.write(x509EncodedKeySpec.getEncoded());
        }

        // Store Private Key.
        final File privateKeyFile = getKeyPath(privateKey);
        privateKeyFile.getParentFile().mkdirs(); // make directories if they do not exist
        final PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        try (FileOutputStream fos = new FileOutputStream(privateKeyFile)) {
            fos.write(pkcs8EncodedKeySpec.getEncoded());
        }
    }

    /**
     * Saves a secret key.
     *
     * @param key the SecretKey to save
     * @throws IOException if the file cannot be written
     * @since 1.0.0
     */
    public void save(SecretKey key) throws IOException {
        final File keyFile = getKeyPath(key);
        keyFile.getParentFile().mkdirs(); // make directories if they do not exist
        try (FileOutputStream fos = new FileOutputStream(keyFile);
             ObjectOutputStream oout = new ObjectOutputStream(fos)) {
            oout.writeObject(key);
        }
    }

    /**
     * Loads a key pair.
     * @return a KeyPair
     * @throws IOException if the file cannot be read
     * @throws NoSuchAlgorithmException if the algorithm cannot be found
     * @throws InvalidKeySpecException if the algorithm's key spec is incorrect
     */
    private KeyPair loadKeyPair() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Read Private Key
        final File filePrivateKey = getKeyPath(KeyType.PRIVATE);

        // Read Public Key
        final File filePublicKey = getKeyPath(KeyType.PUBLIC);

        final byte[] encodedPrivateKey;
        final byte[] encodedPublicKey;

        try (FileInputStream pvtfis = new FileInputStream(filePrivateKey);
             FileInputStream pubfis = new FileInputStream(filePublicKey)) {

            encodedPrivateKey = new byte[(int) filePrivateKey.length()];
            pvtfis.read(encodedPrivateKey);

            encodedPublicKey = new byte[(int) filePublicKey.length()];
            pubfis.read(encodedPublicKey);
        }

        // Generate KeyPair
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        final PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        final PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        final PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return this.keyPair = new KeyPair(publicKey, privateKey);
    }

    /**
     * Loads the secret key.
     * @return a SecretKey
     * @throws IOException if the file cannot be read
     * @throws ClassNotFoundException if deserialization of the SecretKey fails
     */
    private SecretKey loadSecretKey() throws IOException, ClassNotFoundException {
        final File file = getKeyPath(KeyType.SECRET);
        final SecretKey key;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            key = (SecretKey) ois.readObject();
        }
        return this.secretKey = key;
    }

    /**
     * Checks to see if the key pair exists. Both (public and private) need to exist to return true.
     *
     * @return true if keypair exists, false if not
     * @since 1.0.0
     */
    public boolean keyPairExists() {
        return (getKeyPath(KeyType.PUBLIC).exists() && getKeyPath(KeyType.PRIVATE).exists());
    }

    /**
     * Checks to see if the secret key exists.
     *
     * @return true if secret key exists, false if not
     * @since 1.0.0
     */
    public boolean secretKeyExists() {
        return getKeyPath(KeyType.SECRET).exists();
    }

    /**
     * Returns the keypair.
     *
     * @return the KeyPair
     * @since 1.0.0
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /**
     * Returns only the public key.
     *
     * @return the PublicKey
     * @since 1.0.0
     */
    public PublicKey getPublicKey() {
        return (keyPair != null) ? keyPair.getPublic() : null;
    }

    /**
     * Returns only the private key.
     *
     * @return the PrivateKey
     * @since 1.0.0
     */
    public PrivateKey getPrivateKey() {
        return (keyPair != null) ? keyPair.getPrivate() : null;
    }

    /**
     * Returns the secret key.
     *
     * @return the SecretKey
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
