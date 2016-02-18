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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author magibney
 */
class S3AnnexResolver extends AnnexResolver {

    private final String bucket;
    
    public S3AnnexResolver(String objectId, String bucket) {
        super(objectId);
        this.bucket = bucket;
    }

    @Override
    public URI resolve() {
        try {
            return new URI("http://ceph01.library.upenn.edu/test/"+annexId);
        } catch (URISyntaxException ex) {
            return null;
        }
    }
    
}
