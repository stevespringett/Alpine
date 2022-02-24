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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * This class performs symmetric encryption of data using the system-defined
 * secret key. The encryption routines in this class require the use of the
 * unlimited strength policy files, included in the most recent versions of
 * Java by default. Older Java versions may need to install the policy files
 * for the methods in this class to function correctly.
 *
 * @author Steve Springett
 * @since 1.3.0
 */
public class DataEncryption {

    /**
     * Private constructor.
     */
    private DataEncryption() { }

    /**
     * Encrypts the specified plainText using AES-256.
     * @param plainText the text to encrypt
     * @param secretKey the secret key to use to encrypt with
     * @return the encrypted bytes
     * @throws Exception a number of exceptions may be thrown
     * @since 1.3.0
     */
    public static byte[] encryptAsBytes(final SecretKey secretKey, final String plainText) throws Exception {
        final byte[] clean = plainText.getBytes();

        // Generating IV
        int ivSize = 16;
        final byte[] iv = new byte[ivSize];
        final SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Encrypt
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        final byte[] encrypted = cipher.doFinal(clean);

        // Combine IV and encrypted parts
        final byte[] encryptedIVAndText = new byte[ivSize + encrypted.length];
        System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize);
        System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.length);

        return encryptedIVAndText;
    }

    /**
     * Encrypts the specified plainText using AES-256. This method uses the default secret key.
     * @param plainText the text to encrypt
     * @return the encrypted bytes
     * @throws Exception a number of exceptions may be thrown
     * @since 1.3.0
     */
    public static byte[] encryptAsBytes(final String plainText) throws Exception {
        final SecretKey secretKey = KeyManager.getInstance().getSecretKey();
        return encryptAsBytes(secretKey, plainText);
    }

    /**
     * Encrypts the specified plainText using AES-256 and returns a Base64 encoded
     * representation of the encrypted bytes.
     * @param secretKey the secret key to use to encrypt with
     * @param plainText the text to encrypt
     * @return a Base64 encoded representation of the encrypted bytes
     * @throws Exception a number of exceptions may be thrown
     * @since 1.3.0
     */
    public static String encryptAsString(final SecretKey secretKey, final String plainText) throws Exception {
        return Base64.getEncoder().encodeToString(encryptAsBytes(secretKey, plainText));
    }

    /**
     * Encrypts the specified plainText using AES-256 and returns a Base64 encoded
     * representation of the encrypted bytes. This method uses the default secret key.
     * @param plainText the text to encrypt
     * @return a Base64 encoded representation of the encrypted bytes
     * @throws Exception a number of exceptions may be thrown
     * @since 1.3.0
     */
    public static String encryptAsString(final String plainText) throws Exception {
        return Base64.getEncoder().encodeToString(encryptAsBytes(plainText));
    }

    /**
     * Decrypts the specified bytes using AES-256.
     * @param secretKey the secret key to decrypt with
     * @param encryptedIvTextBytes the text to decrypt
     * @return the decrypted bytes
     * @throws Exception a number of exceptions may be thrown
     * @since 1.3.0
     */
    public static byte[] decryptAsBytes(final SecretKey secretKey, final byte[] encryptedIvTextBytes) throws Exception {
        int ivSize = 16;

        // Extract IV
        final byte[] iv = new byte[ivSize];
        System.arraycopy(encryptedIvTextBytes, 0, iv, 0, iv.length);
        final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Extract encrypted bytes
        final int encryptedSize = encryptedIvTextBytes.length - ivSize;
        final byte[] encryptedBytes = new byte[encryptedSize];
        System.arraycopy(encryptedIvTextBytes, ivSize, encryptedBytes, 0, encryptedSize);

        // Decrypt
        final Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        return cipherDecrypt.doFinal(encryptedBytes);
    }

    /**
     * Decrypts the specified bytes using AES-256. This method uses the default secret key.
     * @param encryptedIvTextBytes the text to decrypt
     * @return the decrypted bytes
     * @throws Exception a number of exceptions may be thrown
     * @since 1.3.0
     */
    public static byte[] decryptAsBytes(final byte[] encryptedIvTextBytes) throws Exception {
        final SecretKey secretKey = KeyManager.getInstance().getSecretKey();
        return decryptAsBytes(secretKey, encryptedIvTextBytes);
    }


    /**
     * Decrypts the specified string using AES-256. The encryptedText is
     * expected to be the Base64 encoded representation of the encrypted bytes
     * generated from {@link #encryptAsString(String)}.
     * @param secretKey the secret key to decrypt with
     * @param encryptedText the text to decrypt
     * @return the decrypted string
     * @throws Exception a number of exceptions may be thrown
     * @since 1.3.0
     */
    public static String decryptAsString(final SecretKey secretKey, final String encryptedText) throws Exception {
        return new String(decryptAsBytes(secretKey, Base64.getDecoder().decode(encryptedText)));
    }

    /**
     * Decrypts the specified string using AES-256. The encryptedText is
     * expected to be the Base64 encoded representation of the encrypted bytes
     * generated from {@link #encryptAsString(String)}. This method uses the default secret key.
     * @param encryptedText the text to decrypt
     * @return the decrypted string
     * @throws Exception a number of exceptions may be thrown
     * @since 1.3.0
     */
    public static String decryptAsString(final String encryptedText) throws Exception {
        return new String(decryptAsBytes(Base64.getDecoder().decode(encryptedText)));
    }

}
