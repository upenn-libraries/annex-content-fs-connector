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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import javax.jcr.RepositoryException;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.modeshape.common.util.SecureHash;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.binary.EmptyBinaryValue;

/**
 *
 * @author magibney
 */
public class AnnexRemoteBinaryResolver implements RemoteBinaryResolver {
    private static final String BACKEND = "SHA256E";
    private static final String PREFIX = BACKEND.concat("-s");
    private static final int PREFIX_LENGTH = PREFIX.length();
    private static final String CHECKSUM_DELIM = "--";
    private static final int CHECKSUM_DELIM_LENGTH = CHECKSUM_DELIM.length();
    private static final int HEX_CHECKSUM_LENGTH = SecureHash.Algorithm.SHA_256.getHexadecimalStringLength();
    private static final int SHA1_HEX_CHECKSUM_LENGTH = SecureHash.Algorithm.SHA_1.getHexadecimalStringLength();

    private static String idForFile(File f) {
        try {
            return f.toURI().toURL().toExternalForm();
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    @Override
    public RemoteBinaryMetadata resolve(File symlink, MimeTypeDetector mimeTypeDetector) {
        return resolve(symlink, AnnexContentFedoraFileSystemConnector.getSignificantTargetPath(symlink), mimeTypeDetector);
    }
    private static final String EMPTY_SHA1 = EmptyBinaryValue.INSTANCE.getHexHash();
    private static final URI EMPTY_SHA1_URI = ContentDigest.asURI("SHA-1", EMPTY_SHA1);

    @Override
    public RemoteBinaryMetadata resolve(File symlink, Path target, MimeTypeDetector mimeTypeDetector) {
        String id = idForFile(symlink);
        String annexId = target.getFileName().toString();
        if (!annexId.startsWith(PREFIX)) {
            return new RemoteBinaryMetadata(id, EMPTY_SHA1, EMPTY_SHA1_URI, 0L, null, null);
        }
        int delimIndex = annexId.indexOf("--", PREFIX_LENGTH);
        if (delimIndex < 0) {
            return new RemoteBinaryMetadata(id, EMPTY_SHA1, EMPTY_SHA1_URI, 0L, null, null);
        }
        long size;
        try {
            size = Long.parseLong(annexId.substring(PREFIX_LENGTH, delimIndex));
        } catch (NumberFormatException ex) {
            return new RemoteBinaryMetadata(id, EMPTY_SHA1, EMPTY_SHA1_URI, 0L, null, null);
        }
        int checksumStart = delimIndex + CHECKSUM_DELIM_LENGTH;
        int checksumEnd = checksumStart + HEX_CHECKSUM_LENGTH;
        if (checksumEnd > annexId.length()) {
            return new RemoteBinaryMetadata(id, EMPTY_SHA1, EMPTY_SHA1_URI, size, null, null);
        }
        String nativeChecksum = annexId.substring(checksumStart, checksumStart + HEX_CHECKSUM_LENGTH);
        URI checksumURI = ContentDigest.asURI("SHA-256", nativeChecksum);
        String mimeType;
        if (mimeTypeDetector == null) {
            mimeType = null;
        } else {
            try {
                mimeType = mimeTypeDetector.mimeTypeOf(symlink.getName(), null);
            } catch (RepositoryException | IOException ex) {
                mimeType = null;
            }
        }
        /*
        Modeshape has a baked-in assumption of SHA-1. Since modeshape/fcrepo will not see (nor thus verify)
        the content fixity, the only hard requirement on "sha1" in this context is that it be 40 hex
        characters, and sufficiently unique (in modeshape's opinion) to uniquely identify binary content.
        Modeshape need never know that this is simply a truncated SHA-256 checksum.
         */
        String binaryKeySha1Equivalent = nativeChecksum.substring(0, SHA1_HEX_CHECKSUM_LENGTH);
        return new RemoteBinaryMetadata(id, binaryKeySha1Equivalent, checksumURI, size, mimeType, new S3AnnexResolver(annexId));
    }
    
}
