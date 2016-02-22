/*
 * Copyright 2016 The Trustees of the University of Pennsylvania
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.upenn.library.fcrepo.connector.annex;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import java.util.Properties;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;


public class S3AnnexResolverFactory implements AnnexRemoteBinaryResolver.AnnexResolverFactory {

    private static final String ACCESS_KEY_PROPNAME = "access_key";
    private static final String SECRET_KEY_PROPNAME = "secret_key";
    private static final String BUCKET_PROPNAME = "bucket";
    private static final String ENDPOINT_PROPNAME = "endpoint";
    
    private String bucket;
    private String accessKey;
    private String secretKey;
    private AmazonS3 conn;

    @Override
    public AnnexResolver getAnnexResolver(String annexId) {
        return new S3AnnexResolver(annexId, this);
    }

    @Override
    public void initialize(Properties props) {
        this.accessKey = props.getProperty(ACCESS_KEY_PROPNAME);
        this.secretKey = props.getProperty(SECRET_KEY_PROPNAME);
        this.bucket = props.getProperty(BUCKET_PROPNAME);
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);
        S3ClientOptions clientOptions = new S3ClientOptions();
        clientOptions.setPathStyleAccess(true);

        conn = new AmazonS3Client(credentials, clientConfig);
        conn.setS3ClientOptions(clientOptions);
        conn.setEndpoint(props.getProperty(ENDPOINT_PROPNAME));
    }

    public URI getObjectURI(String annexId, Map<String, String> remoteResponseHeaderHints) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, annexId);
        request.addRequestParameter(ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_TYPE, remoteResponseHeaderHints.get(Headers.CONTENT_TYPE));
        request.addRequestParameter(ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_DISPOSITION, remoteResponseHeaderHints.get(Headers.CONTENT_DISPOSITION));
        try {
            return conn.generatePresignedUrl(request).toURI();
        } catch (URISyntaxException ex) {
            throw null;
        }
    }

}
