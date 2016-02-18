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
import org.fcrepo.kernel.api.models.RemoteResource;

/**
 *
 * @author magibney
 */
public class RemoteBinaryMetadata {
    public final String sha1;
    public final URI checksumURI;
    public final long size;
    public final String mimeType;
    public final String id;
    public final RemoteResource remoteResolver;

    public RemoteBinaryMetadata(String id, String sha1, URI checksumURI, long size, String mimeType, RemoteResource remoteResolver) {
        this.sha1 = sha1;
        this.checksumURI = checksumURI;
        this.size = size;
        this.mimeType = mimeType;
        this.id = id;
        this.remoteResolver = remoteResolver;
    }
    
}
