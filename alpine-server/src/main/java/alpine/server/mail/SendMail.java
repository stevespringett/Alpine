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
package alpine.server.mail;

import alpine.security.crypto.RelaxedX509TrustManager;
import org.eclipse.angus.mail.util.MailSSLSocketFactory;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Properties;

public class SendMail {
    private Address from;
    private Address[] to;
    private Address[] cc;
    private Address[] bcc;
    private String subject;
    private String body;
    private String bodyMimeType;
    private File[] attachments = {};
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean useStartTLS;
    private boolean useNTLM;
    private boolean smtpauth;
    private boolean trustCert;
    private boolean debug;

    public SendMail from(final String from) throws SendMailException {
        this.from = parseAddress(from);
        return this;
    }

    public SendMail to(final String[] to) throws SendMailException {
        this.to = parseAddress(to);
        return this;
    }

    public SendMail to(final String to) throws SendMailException {
        this.to = parseAddress(new String[]{to});
        return this;
    }

    public SendMail cc(final String[] cc) throws SendMailException {
        this.cc = parseAddress(cc);
        return this;
    }

    public SendMail cc(final String cc) throws SendMailException {
        this.cc = parseAddress(new String[]{cc});
        return this;
    }

    public SendMail bcc(final String[] bcc) throws SendMailException {
        this.bcc = parseAddress(bcc);
        return this;
    }

    public SendMail bcc(final String bcc) throws SendMailException {
        this.bcc = parseAddress(new String[]{bcc});
        return this;
    }

    public SendMail subject(final String subject) {
        this.subject = subject;
        return this;
    }

    public SendMail body(final String body) {
        this.body = body;
        return this;
    }

    public SendMail bodyMimeType(final String bodyMimeType) {
        this.bodyMimeType = bodyMimeType;
        return this;
    }

    public SendMail attachments(final File[] attachments) {
        if (attachments == null) {
            this.attachments = null;
        } else {
            this.attachments = attachments.clone();
        }
        return this;
    }

    public SendMail host(final String host) {
        this.host = host;
        return this;
    }

    public SendMail port(final int port) {
        this.port = port;
        return this;
    }

    public SendMail username(final String username) {
        this.username = username;
        return this;
    }

    public SendMail password(final String password) {
        this.password = password;
        return this;
    }

    public SendMail useStartTLS(final boolean useStartTLS) {
        this.useStartTLS = useStartTLS;
        return this;
    }

    public SendMail useNTLM(final boolean useNTLM) {
        this.useNTLM = useNTLM;
        return this;
    }

    public SendMail smtpauth(final boolean smtpauth) {
        this.smtpauth = smtpauth;
        return this;
    }

    public SendMail trustCert(final boolean trustCert) {
        this.trustCert = trustCert;
        return this;
    }

    public SendMail debug(final boolean debug) {
        this.debug = debug;
        return this;
    }

    private Address parseAddress(final String address) throws SendMailException {
        return parseAddress(new String[]{address})[0];
    }

    private Address[] parseAddress(final String[] addresses) throws SendMailException {
        final InternetAddress[] internetAddresses = new InternetAddress[addresses.length];
        for (int i=0; i<addresses.length; i++) {
            try {
                internetAddresses[i] = new InternetAddress(addresses[i]);
            } catch (AddressException e) {
                throw new SendMailException("An error occurred processing internet addresses", e);
            }
        }
        return internetAddresses;
    }

    public void send() throws SendMailException {
        final Properties props = new Properties();
        if (trustCert) {
            // This block will automatically allow the SendMail client to accept any certificate,
            // even self-signed ones not currently in the local keystore.
            MailSSLSocketFactory sf;
            try {
                sf = new MailSSLSocketFactory();
                sf.setTrustAllHosts(true);
                final TrustManager[] trustManagers = {new RelaxedX509TrustManager()};
                sf.setTrustManagers(trustManagers);
            } catch (GeneralSecurityException e) {
                throw new SendMailException("An error occurred while configuring trust managers", e);
            }
            props.put("mail.smtp.ssl.socketFactory", sf);
            props.put("mail.smtp.ssl.checkserveridentity", false);
        } else if (useStartTLS) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.socketFactory.port", port);
        props.put("mail.smtp.auth", smtpauth);
        props.put("mail.smtp.starttls.enable", useStartTLS);

        if (useNTLM) {
            props.put("mail.smtp.auth.ntlm.domain", host);
        }

        final Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        session.setDebug(debug);
        try {
            final Message message = new MimeMessage(session);
            message.setFrom(from);
            message.setRecipients(Message.RecipientType.TO, to);

            if (cc != null && cc.length > 0) {
                message.setRecipients(Message.RecipientType.CC, cc);
            }
            if (bcc != null && bcc.length > 0) {
                message.setRecipients(Message.RecipientType.BCC, bcc);
            }

            message.setSubject(subject);
            BodyPart messageBodyPart = new MimeBodyPart();

            if(bodyMimeType != null) {
                messageBodyPart.setContent(body, bodyMimeType);
            } else {
                messageBodyPart.setText(body);
            }

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            for (final File file : attachments) {
                messageBodyPart = new MimeBodyPart();
                final DataSource source = new FileDataSource(file);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(file.getName());
                multipart.addBodyPart(messageBodyPart);
            }
            message.setContent(multipart);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new SendMailException("An error occurred while sending email", e);
        }
    }
}
