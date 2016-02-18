/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.upenn.library.fcrepo.connector.annex;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import java.util.Properties;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;


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

    public URI getObjectURI(String annexId) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, annexId);
        try {
            return conn.generatePresignedUrl(request).toURI();
        } catch (URISyntaxException ex) {
            throw null;
        }
    }

}
