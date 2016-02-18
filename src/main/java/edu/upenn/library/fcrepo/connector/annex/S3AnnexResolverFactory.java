/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.upenn.library.fcrepo.connector.annex;

import java.util.Properties;


public class S3AnnexResolverFactory implements AnnexRemoteBinaryResolver.AnnexResolverFactory {

    private static final String ACCESS_KEY_PROPNAME = "access_key";
    private static final String SECRET_KEY_PROPNAME = "secret_key";
    private static final String BUCKET_PROPNAME = "bucket";
    
    private String bucket;
    private String accessKey;
    private String secretKey;
    
    @Override
    public AnnexResolver getAnnexResolver(String annexId) {
        return new S3AnnexResolver(annexId, bucket);
    }

    @Override
    public void initialize(Properties props) {
        this.accessKey = props.getProperty(ACCESS_KEY_PROPNAME);
        this.secretKey = props.getProperty(SECRET_KEY_PROPNAME);
        this.bucket = props.getProperty(BUCKET_PROPNAME);
    }
    
}
