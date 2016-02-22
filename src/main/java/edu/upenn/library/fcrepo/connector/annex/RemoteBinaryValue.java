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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import org.fcrepo.kernel.api.models.ChecksumGeneralizedResource;
import org.fcrepo.kernel.api.models.RemoteResource;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

/**
 *
 * @author magibney
 */
public class RemoteBinaryValue extends ExternalBinaryValue implements ChecksumGeneralizedResource, RemoteResource {
    private final URI checksumURI;
    private final RemoteResource remoteResourceResolver;

    public RemoteBinaryValue(String sha1, URI checksumURI, String sourceName, String id, long size, String mimeType, RemoteResource remoteResourceResolver) {
        super(new BinaryKey(sha1), sourceName, id, size, null, null);
        super.setMimeType(mimeType);
        this.checksumURI = checksumURI;
        this.remoteResourceResolver = remoteResourceResolver;
    }

    @Override
    protected InputStream internalStream() throws Exception {
        return new ByteArrayInputStream(EMPTY_CONTENT);
    }

    @Override
    public URI getChecksumURI() {
        return checksumURI;
    }

    @Override
    public URI resolve(Map<String, String> remoteResponseHeaderHints) {
        return remoteResourceResolver == null ? null : remoteResourceResolver.resolve(remoteResponseHeaderHints);
    }
    
}
