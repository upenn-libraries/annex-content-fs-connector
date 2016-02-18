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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.fcrepo.connector.file.FedoraFileSystemConnector;
import static org.fcrepo.kernel.api.FedoraTypes.HAS_MIME_TYPE;
import org.fcrepo.kernel.api.models.ChecksumGeneralizedResource;
import org.fcrepo.kernel.api.models.RemoteResource;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.util.SecureHash;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.EmptyBinaryValue;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author magibney
 */
public class AnnexContentFedoraFileSystemConnector extends FedoraFileSystemConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnexContentFedoraFileSystemConnector.class);

    private RemoteBinaryResolver remoteBinaryResolver;

    @Override
    public void initialize(NamespaceRegistry registry, NodeTypeManager nodeTypeManager) throws IOException {
        super.initialize(registry, nodeTypeManager);
        remoteBinaryResolver = new AnnexRemoteBinaryResolver();
    }

    @Override
    public Document getDocumentById(String id) {
        Document doc = super.getDocumentById(id);
        final DocumentReader docReader = readDocument(doc);
        final String primaryType = docReader.getPrimaryTypeName();
        if (NT_RESOURCE.equals(primaryType) && getSignificantTargetPath(fileFor(id)) != null) {
            if (docReader.getProperty(HAS_MIME_TYPE) == null) {
                final BinaryValue binaryValue = getBinaryValue(docReader);
                String mimeType;
                try {
                    mimeType = binaryValue.getMimeType();
                } catch (IOException | RepositoryException ex) {
                    mimeType = null;
                }
                if (mimeType != null) {
                    final DocumentWriter docWriter = writeDocument(doc);
                    docWriter.addProperty(HAS_MIME_TYPE, mimeType);
                    doc = docWriter.document();
                }
            }
        }
        return doc;
    }

    @Override
    protected ExternalBinaryValue createBinaryValue(File file) throws IOException {
        Path target = getSignificantTargetPath(file);
        if (target == null) {
            return super.createBinaryValue(file);
        } else {
            RemoteBinaryMetadata rbm = remoteBinaryResolver.resolve(file, target, getMimeTypeDetector());
            return new RemoteBinaryValue(rbm.sha1, rbm.checksumURI, getSourceName(), rbm.id, rbm.size, rbm.mimeType);
        }
    }
    
    private static Path getSignificantTargetPath(File file) {
        Path path = file.toPath();
        if (!Files.isSymbolicLink(path)) {
            return null;
        } else {
            try {
                Path target = Files.readSymbolicLink(path);
                return Files.notExists(path.resolve(target)) ? target : null;
            } catch (IOException ex) {
                return null;
            }
        }
    }

    @Override
    protected boolean acceptFile(File file) {
        if (super.acceptFile(file)) {
            return true;
        } else {
            return getSignificantTargetPath(file) != null;
        }
    }

    @Override
    protected DocumentWriter otherType(String id, File file, boolean isRoot, boolean isResource) {
        if (isResource || super.acceptFile(file)
                || getSignificantTargetPath(file) == null) {
            return null;
        }
        DocumentWriter writer = newDocument(id);
        writer.setPrimaryType(NT_FILE);
        String childId = contentChildId(id, isRoot);
        writer.addChild(childId, JCR_CONTENT);
        return writer;
    }

    public static class RemoteBinaryMetadata {

        public final String sha1;
        public final URI checksumURI;
        public final long size;
        public final String mimeType;
        public final String id;
        public final URI remoteTarget;

        public RemoteBinaryMetadata(String id, String sha1, URI checksumURI, long size, String mimeType, URI remoteTarget) {
            this.sha1 = sha1;
            this.checksumURI = checksumURI;
            this.size = size;
            this.mimeType = mimeType;
            this.id = id;
            this.remoteTarget = remoteTarget;
        }
    }

    public static interface RemoteBinaryResolver {
        RemoteBinaryMetadata resolve(File file, MimeTypeDetector mimeTypeDetector);
        RemoteBinaryMetadata resolve(File file, Path target, MimeTypeDetector mimeTypeDetector);
    }

    public static class AnnexRemoteBinaryResolver implements RemoteBinaryResolver {

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
            return resolve(symlink, getSignificantTargetPath(symlink), mimeTypeDetector);
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
            URI remoteTarget;
            try {
                remoteTarget = new URI("http://dla.library.upenn.edu/dla/");
            } catch (URISyntaxException ex) {
                remoteTarget = null;
            }

            /*
            Modeshape has a baked-in assumption of SHA-1. Since modeshape/fcrepo will not see (nor thus verify)
            the content fixity, the only hard requirement on "sha1" in this context is that it be 40 hex
            characters, and sufficiently unique (in modeshape's opinion) to uniquely identify binary content.
            Modeshape need never know that this is simply a truncated SHA-256 checksum.
            */
            String binaryKeySha1Equivalent = nativeChecksum.substring(0, SHA1_HEX_CHECKSUM_LENGTH);
            return new RemoteBinaryMetadata(id, binaryKeySha1Equivalent, checksumURI, size, mimeType, remoteTarget);
        }

    }

    private static class RemoteBinaryValue extends ExternalBinaryValue implements ChecksumGeneralizedResource, RemoteResource {

        private final URI checksumURI;

        private RemoteBinaryValue(String sha1, URI checksumURI, String sourceName, String id, long size, String mimeType) {
            super(new BinaryKey(sha1), sourceName, id, size, null, null);
            super.setMimeType(mimeType);
            this.checksumURI = checksumURI;
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
        public URI resolve() {
            try {
                return new URI("target uri");
            } catch (URISyntaxException ex) {
                return null;
            }
        }
    }
}
