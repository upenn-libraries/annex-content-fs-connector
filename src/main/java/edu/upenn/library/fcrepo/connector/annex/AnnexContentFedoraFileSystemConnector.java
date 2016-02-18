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
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.fcrepo.connector.file.FedoraFileSystemConnector;
import static org.fcrepo.kernel.api.FedoraTypes.HAS_MIME_TYPE;
import org.infinispan.schematic.document.Document;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.BinaryValue;
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
    
    private static final String CONFIG_FILE = "annexRemoteBinaryResolver.properties";

    @Override
    public void initialize(NamespaceRegistry registry, NodeTypeManager nodeTypeManager) throws IOException {
        super.initialize(registry, nodeTypeManager);
        Properties props = new Properties();
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            props.load(reader);
        }
        System.err.println("loading rbr");
        remoteBinaryResolver = new AnnexRemoteBinaryResolver(props);
        System.err.println("loaded rbr");
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
            return new RemoteBinaryValue(rbm.sha1, rbm.checksumURI, getSourceName(), rbm.id, rbm.size, rbm.mimeType, rbm.remoteResolver);
        }
    }
    
    static Path getSignificantTargetPath(File file) {
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
}
