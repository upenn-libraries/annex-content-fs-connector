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
import java.util.Map;

/**
 *
 * @author magibney
 */
class S3AnnexResolver extends AnnexResolver {

    private final S3AnnexResolverFactory sarf;
    
    public S3AnnexResolver(String objectId, S3AnnexResolverFactory sarf) {
        super(objectId);
        this.sarf = sarf;
    }

    @Override
    public URI resolve(Map<String, String> remoteResponseHeaderHints) {
        return sarf.getObjectURI(annexId, remoteResponseHeaderHints);
    }

}
