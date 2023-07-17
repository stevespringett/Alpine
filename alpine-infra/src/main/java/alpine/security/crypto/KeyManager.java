/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.security.crypto;

import alpine.Config;
import alpine.common.logging.Logger;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        createKeysIfNotExist();
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
                if (secretKeyHasOldFormat()) {
                    loadSecretKey();
                } else {
                    loadEncodedSecretKey();
                }
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.error("An error occurred loading secret key");
                LOGGER.error(e.getMessage());
            }
        }
    }

    /**
     * Checks if the keys exists. If not, they will be created.
     */
    private void createKeysIfNotExist() {
        if (!keyPairExists()) {
            try {
                final KeyPair keyPair = generateKeyPair();
                save(keyPair);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error("An error occurred generating new keypair");
                LOGGER.error(e.getMessage());
            } catch (IOException e) {
                LOGGER.error("An error occurred saving newly generated keypair");
                LOGGER.error(e.getMessage());
            }
        }
        if (!secretKeyExists()) {
            try {
                final SecretKey secretKey = generateSecretKey();
                saveEncoded(secretKey);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error("An error occurred generating new secret key");
                LOGGER.error(e.getMessage());
            } catch (IOException e) {
                LOGGER.error("An error occurred saving newly generated secret key");
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
    private File getKeyPath(final KeyType keyType) {
        if (keyType == KeyType.SECRET) {
            final String secretKeyPath = Config.getInstance().getProperty(Config.AlpineKey.SECRET_KEY_PATH);
            if (secretKeyPath != null) {
                return Paths.get(secretKeyPath).toFile();
            }
        } else if (keyType == KeyType.PRIVATE) {
            final String privateKeyPath = Config.getInstance().getProperty(Config.AlpineKey.PRIVATE_KEY_PATH);
            if (privateKeyPath != null) {
                return Paths.get(privateKeyPath).toFile();
            }
        } else if (keyType == KeyType.PUBLIC) {
            final String publicKeyPath = Config.getInstance().getProperty(Config.AlpineKey.PUBLIC_KEY_PATH);
            if (publicKeyPath != null) {
                return Paths.get(publicKeyPath).toFile();
            }
        }
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
    private File getKeyPath(final Key key) {
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
    public void save(final KeyPair keyPair) throws IOException {
        LOGGER.info("Saving key pair");
        final PrivateKey privateKey = keyPair.getPrivate();
        final PublicKey publicKey = keyPair.getPublic();

        // Store Public Key
        final File publicKeyFile = getKeyPath(publicKey);
        publicKeyFile.getParentFile().mkdirs(); // make directories if they do not exist
        final X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        try (OutputStream fos = Files.newOutputStream(publicKeyFile.toPath())) {
            fos.write(x509EncodedKeySpec.getEncoded());
        }

        // Store Private Key.
        final File privateKeyFile = getKeyPath(privateKey);
        privateKeyFile.getParentFile().mkdirs(); // make directories if they do not exist
        final PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        try (OutputStream fos = Files.newOutputStream(privateKeyFile.toPath())) {
            fos.write(pkcs8EncodedKeySpec.getEncoded());
        }
    }

    /**
     * Saves a secret key.
     *
     * @param key the SecretKey to save
     * @throws IOException if the file cannot be written
     * @since 1.0.0
     * @deprecated Use {@link #saveEncoded(SecretKey)} instead
     */
    @Deprecated(forRemoval = true)
    public void save(final SecretKey key) throws IOException {
        final File keyFile = getKeyPath(key);
        keyFile.getParentFile().mkdirs(); // make directories if they do not exist
        try (OutputStream fos = Files.newOutputStream(keyFile.toPath());
             ObjectOutputStream oout = new ObjectOutputStream(fos)) {
            oout.writeObject(key);
        }
    }

    /**
     * Saves a secret key in encoded format.
     *
     * @param key the SecretKey to save
     * @throws IOException if the file cannot be written
     * @since 2.2.0
     */
    public void saveEncoded(final SecretKey key) throws IOException {
        final File keyFile = getKeyPath(key);
        keyFile.getParentFile().mkdirs(); // make directories if they do not exist
        try (OutputStream fos = Files.newOutputStream(keyFile.toPath())) {
            fos.write(key.getEncoded());
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

        byte[] encodedPrivateKey;
        byte[] encodedPublicKey;

        try (InputStream pvtfis = Files.newInputStream(filePrivateKey.toPath());
             InputStream pubfis = Files.newInputStream(filePublicKey.toPath())) {

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
     * @throws IOException            if the file cannot be read
     * @throws ClassNotFoundException if deserialization of the SecretKey fails
     * @deprecated Use {@link #loadEncodedSecretKey()}
     */
    @Deprecated(forRemoval = true)
    SecretKey loadSecretKey() throws IOException, ClassNotFoundException {
        final File file = getKeyPath(KeyType.SECRET);
        SecretKey key;
        try (InputStream fis = Files.newInputStream(file.toPath());
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            key = (SecretKey) ois.readObject();
        }
        return this.secretKey = key;
    }

    /**
     * Loads the encoded secret key.
     *
     * @return a SecretKey
     * @throws IOException if the file cannot be read
     * @since 2.2.0
     */
    SecretKey loadEncodedSecretKey() throws IOException {
        final File file = getKeyPath(KeyType.SECRET);
        try (InputStream fis = Files.newInputStream(file.toPath())) {
            final byte[] encodedKey = fis.readAllBytes();
            return this.secretKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
        }
    }

    /**
     * Checks to see if the key pair exists. Both (public and private) need to exist to return true.
     *
     * @return true if keypair exists, false if not
     * @since 1.0.0
     */
    public boolean keyPairExists() {
        return getKeyPath(KeyType.PUBLIC).exists() && getKeyPath(KeyType.PRIVATE).exists();
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
     * Checks if the secret key was stored in the old Java Object Serialization format.
     *
     * @return {@code true} when the old format is detected, otherwise {@code false}
     * @throws IOException When reading the secret key file could not be read
     * @since 2.2.0
     */
    boolean secretKeyHasOldFormat() throws IOException {
        try (final InputStream fis = Files.newInputStream(getKeyPath(KeyType.SECRET).toPath())) {
            return ByteBuffer.wrap(fis.readNBytes(2)).getShort() == ObjectStreamConstants.STREAM_MAGIC;
        }
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
