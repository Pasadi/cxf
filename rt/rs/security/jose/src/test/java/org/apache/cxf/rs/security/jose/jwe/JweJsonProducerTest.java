/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.rs.security.jose.jwe;

import java.security.Security;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JweJsonProducerTest extends Assert {
    private static final byte[] WRAPPER_BYTES1 = {91, 96, 105, 38, 99, 108, 110, 8, -93, 50, -15, 62, 0, -115, 73, -39};
    private static final byte[] WRAPPER_BYTES2 = {-39, 96, 105, 38, 99, 108, 110, 8, -93, 50, -15, 62, 0, -115, 73, 91};
    private static final byte[] CEK_BYTES = {-43, 123, 77, 115, 40, 49, -4, -9, -48, -74, 62, 59, 60, 102, -22, -100};
    private static final String SINGLE_RECIPIENT_OUTPUT = 
        "{" 
        + "\"protected\":\"eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4R0NNIn0\","
        + "\"recipients\":" 
        + "["
        + "{\"encrypted_key\":\"b3-M9_CRgT3wEBhhXlpb-BoY7vtA4W_N\"}"
        + "],"
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"GxWlwvTPmHi4ZnQgafiHew\""
        + "}";
    private static final String SINGLE_RECIPIENT_FLAT_OUTPUT = 
        "{" 
        + "\"protected\":\"eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4R0NNIn0\","
        + "\"encrypted_key\":\"b3-M9_CRgT3wEBhhXlpb-BoY7vtA4W_N\","
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"GxWlwvTPmHi4ZnQgafiHew\""
        + "}";
    private static final String SINGLE_RECIPIENT_ALL_HEADERS_AAD_OUTPUT = 
        "{" 
        + "\"protected\":\"eyJlbmMiOiJBMTI4R0NNIn0\","
        + "\"unprotected\":{\"jku\":\"https://server.example.com/keys.jwks\"},"    
        + "\"recipients\":" 
        + "["
        + "{"
        + "\"header\":{\"alg\":\"A128KW\"},"
        + "\"encrypted_key\":\"b3-M9_CRgT3wEBhhXlpb-BoY7vtA4W_N\""
        + "}"
        + "],"
        + "\"aad\":\"WyJ2Y2FyZCIsW1sidmVyc2lvbiIse30sInRleHQiLCI0LjAiXSxbImZuIix7fSwidGV4dCIsIk1lcmlhZG9jIEJyYW5keWJ1Y"
                    + "2siXSxbIm4iLHt9LCJ0ZXh0IixbIkJyYW5keWJ1Y2siLCJNZXJpYWRvYyIsIk1yLiIsIiJdXSxbImJkYXkiLHt9LCJ0ZXh0"
                    + "IiwiVEEgMjk4MiJdLFsiZ2VuZGVyIix7fSwidGV4dCIsIk0iXV1d\","
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"oVUQGS9608D-INq61-vOaA\""
        + "}";
    private static final String MULTIPLE_RECIPIENTS_OUTPUT = 
        "{" 
        + "\"protected\":\"eyJlbmMiOiJBMTI4R0NNIn0\","
        + "\"unprotected\":{\"jku\":\"https://server.example.com/keys.jwks\",\"alg\":\"A128KW\"},"    
        + "\"recipients\":" 
        + "["
        + "{"
        + "\"encrypted_key\":\"b3-M9_CRgT3wEBhhXlpb-BoY7vtA4W_N\""
        + "},"
        + "{"
        + "\"encrypted_key\":\"6a_nnEYO45qB_Vp6N2QbFQ7Cv1uecbiE\""
        + "}"
        + "],"
        + "\"aad\":\"WyJ2Y2FyZCIsW1sidmVyc2lvbiIse30sInRleHQiLCI0LjAiXSxbImZuIix7fSwidGV4dCIsIk1lcmlhZG9jIEJyYW5keWJ1Y"
                    + "2siXSxbIm4iLHt9LCJ0ZXh0IixbIkJyYW5keWJ1Y2siLCJNZXJpYWRvYyIsIk1yLiIsIiJdXSxbImJkYXkiLHt9LCJ0ZXh0"
                    + "IiwiVEEgMjk4MiJdLFsiZ2VuZGVyIix7fSwidGV4dCIsIk0iXV1d\","
        + "\"iv\":\"48V1_ALb6US04U3b\","
        + "\"ciphertext\":\"KTuJBMk9QG59xPB-c_YLM5-J7VG40_eMPvyHDD7eB-WHj_34YiWgpBOydTBm4RW0zUCJZ09xqorhWJME-DcQ\","
        + "\"tag\":\"oVUQGS9608D-INq61-vOaA\""
        + "}";
    private static final String EXTRA_AAD_SOURCE = 
        "[\"vcard\",["
        + "[\"version\",{},\"text\",\"4.0\"],"
        + "[\"fn\",{},\"text\",\"Meriadoc Brandybuck\"],"
        + "[\"n\",{},\"text\",[\"Brandybuck\",\"Meriadoc\",\"Mr.\",\"\"]],"
        + "[\"bday\",{},\"text\",\"TA 2982\"],"
        + "[\"gender\",{},\"text\",\"M\"]"
        + "]]";
    private static final String SINGLE_RECIPIENT_A128CBCHS256_OUTPUT = 
        "{" 
        + "\"protected\":\"eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0\","
        + "\"recipients\":" 
        + "["
        + "{\"encrypted_key\":\"6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ\"}"
        + "],"
        + "\"iv\":\"AxY8DCtDaGlsbGljb3RoZQ\","
        + "\"ciphertext\":\"KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY\","
        + "\"tag\":\"U0m_YmjN04DJvceFICbCVQ\""
        + "}";
    @BeforeClass
    public static void registerBouncyCastleIfNeeded() throws Exception {
        try {
            Cipher.getInstance(Algorithm.AES_GCM_ALGO_JAVA);
            Cipher.getInstance(Algorithm.AES_CBC_ALGO_JAVA);
        } catch (Throwable t) {
            Security.addProvider(new BouncyCastleProvider());    
        }
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.class.getName());    
    }
    
    @Test
    public void testSingleRecipientGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text, SINGLE_RECIPIENT_OUTPUT, JoseConstants.A128GCM_ALGO, 
                              WRAPPER_BYTES1, JweCompactReaderWriterTest.INIT_VECTOR_A1, 
                              CEK_BYTES, false);
    }
    @Test
    public void testSingleRecipientFlatGcm() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        doTestSingleRecipient(text, SINGLE_RECIPIENT_FLAT_OUTPUT, JoseConstants.A128GCM_ALGO, 
                              WRAPPER_BYTES1, JweCompactReaderWriterTest.INIT_VECTOR_A1, 
                              CEK_BYTES, true);
    }
    @Test
    public void testSingleRecipientA128CBCHS256() throws Exception {
        String text = "Live long and prosper.";
        doTestSingleRecipient(text, SINGLE_RECIPIENT_A128CBCHS256_OUTPUT, JoseConstants.A128CBC_HS256_ALGO, 
                              Base64UrlUtility.decode(JweCompactReaderWriterTest.KEY_ENCRYPTION_KEY_A3),
                              JweCompactReaderWriterTest.INIT_VECTOR_A3,
                              JweCompactReaderWriterTest.CONTENT_ENCRYPTION_KEY_A3,
                              false);
    }
    
    private String doTestSingleRecipient(String text,
                                         String expectedOutput, 
                                         String contentEncryptionAlgo,
                                         byte[] wrapperKeyBytes,
                                         final byte[] iv,
                                         final byte[] cek,
                                         boolean canBeFlat) throws Exception {
        SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(wrapperKeyBytes, "AES");
        JweHeaders headers = new JweHeaders(JoseConstants.A128KW_ALGO,
                                            contentEncryptionAlgo);
        JweEncryptionProvider jwe = JweUtils.createJweEncryptionProvider(wrapperKey, headers);
        JweJsonProducer p = new JweJsonProducer(headers, StringUtils.toBytesUTF8(text), canBeFlat) {
            protected JweEncryptionInput createEncryptionInput(JweHeaders jsonHeaders) {
                JweEncryptionInput input = super.createEncryptionInput(jsonHeaders);
                input.setCek(cek);
                input.setIv(iv);
                return input;
            }    
        };
        String jweJson = p.encryptWith(jwe);
        assertEquals(expectedOutput, jweJson);
        return jweJson;
    }
    @Test
    public void testSingleRecipientAllTypeOfHeadersAndAad() {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        SecretKey wrapperKey = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES1, "AES");
        
        JweHeaders protectedHeaders = new JweHeaders(JoseConstants.A128GCM_ALGO);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setJsonWebKeysUrl("https://server.example.com/keys.jwks");
        
        JweEncryptionProvider jwe = JweUtils.createJweEncryptionProvider(wrapperKey, 
                                                                         JoseConstants.A128KW_ALGO,
                                                                         JoseConstants.A128GCM_ALGO,
                                                                         null);
        JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                sharedUnprotectedHeaders,
                                                StringUtils.toBytesUTF8(text),
                                                StringUtils.toBytesUTF8(EXTRA_AAD_SOURCE),
                                                false) {
            protected JweEncryptionInput createEncryptionInput(JweHeaders jsonHeaders) {
                JweEncryptionInput input = super.createEncryptionInput(jsonHeaders);
                input.setCek(CEK_BYTES);
                input.setIv(JweCompactReaderWriterTest.INIT_VECTOR_A1);
                return input;
            }
        };
        JweHeaders recepientUnprotectedHeaders = new JweHeaders();
        recepientUnprotectedHeaders.setKeyEncryptionAlgorithm(JoseConstants.A128KW_ALGO);
        String jweJson = p.encryptWith(jwe, recepientUnprotectedHeaders);
        assertEquals(SINGLE_RECIPIENT_ALL_HEADERS_AAD_OUTPUT, jweJson);
    }
    @Test
    public void testMultipleRecipients() {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        SecretKey wrapperKey1 = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES1, "AES");
        SecretKey wrapperKey2 = CryptoUtils.createSecretKeySpec(WRAPPER_BYTES2, "AES");
        
        JweHeaders protectedHeaders = new JweHeaders(JoseConstants.A128GCM_ALGO);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setJsonWebKeysUrl("https://server.example.com/keys.jwks");
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(JoseConstants.A128KW_ALGO);
        
        List<JweEncryptionProvider> jweList = new LinkedList<JweEncryptionProvider>();
        
        KeyEncryptionAlgorithm keyEncryption1 = 
            JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey1, JoseConstants.A128KW_ALGO);
        ContentEncryptionAlgorithm contentEncryption = 
            JweUtils.getContentEncryptionAlgorithm(JoseConstants.A128GCM_ALGO);
        JweEncryptionProvider jwe1 = new WrappedKeyJweEncryption(keyEncryption1, contentEncryption);
        KeyEncryptionAlgorithm keyEncryption2 = 
            JweUtils.getSecretKeyEncryptionAlgorithm(wrapperKey2, JoseConstants.A128KW_ALGO);
        JweEncryptionProvider jwe2 = new WrappedKeyJweEncryption(keyEncryption2, contentEncryption);
        jweList.add(jwe1);
        jweList.add(jwe2);
        
        JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                                sharedUnprotectedHeaders,
                                                StringUtils.toBytesUTF8(text),
                                                StringUtils.toBytesUTF8(EXTRA_AAD_SOURCE),
                                                false) {
            protected JweEncryptionInput createEncryptionInput(JweHeaders jsonHeaders) {
                JweEncryptionInput input = super.createEncryptionInput(jsonHeaders);
                input.setCek(CEK_BYTES);
                input.setIv(JweCompactReaderWriterTest.INIT_VECTOR_A1);
                return input;
            }
        };
        
        String jweJson = p.encryptWith(jweList);
        assertEquals(MULTIPLE_RECIPIENTS_OUTPUT, jweJson);
    }
}

