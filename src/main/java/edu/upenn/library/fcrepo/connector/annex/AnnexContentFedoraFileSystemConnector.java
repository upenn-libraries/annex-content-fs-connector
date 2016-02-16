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
import java.nio.file.Files;
import java.nio.file.Path;
import org.fcrepo.connector.file.FedoraFileSystemConnector;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author magibney
 */
public class AnnexContentFedoraFileSystemConnector extends FedoraFileSystemConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnexContentFedoraFileSystemConnector.class);

    private static final String DELIMITER = "/";
    private static final String JCR_CONTENT = "jcr:content";
    private static final String JCR_CONTENT_SUFFIX = DELIMITER + JCR_CONTENT;
    private static final String NT_FILE = "nt:file";

    @Override
    protected ExternalBinaryValue createBinaryValue(File file) throws IOException {
        Path target = getSignificantTargetPath(file);
        if (target == null) {
            return super.createBinaryValue(file);
        } else {
            return AnnexBinaryValue.newBinaryValue(getSourceName(), file, target);
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
            Path path = file.toPath();
            if (!Files.isSymbolicLink(path)) {
                return false;
            } else {
                try {
                    Path target = Files.readSymbolicLink(path);
                    return Files.notExists(path.resolve(target));
                } catch (IOException ex) {
                    return false;
                }
            }
        }
    }
    
    @Override
    protected DocumentWriter otherType(String id, File file, boolean isRoot, boolean isResource) {
        if (super.acceptFile(file) || isResource) {
            return null;
        }
        Path target;
        Path path = file.toPath();
        if (!Files.isSymbolicLink(path)) {
            return null;
        } else {
            try {
                target = Files.readSymbolicLink(path);
                if (!Files.notExists(path.resolve(target))) {
                    return null;
                }
            } catch (IOException ex) {
                return null;
            }
        }
        DocumentWriter writer = newDocument(id);
        writer.setPrimaryType(NT_FILE);
        String childId = contentChildId(id, isRoot);
        writer.addChild(childId, JCR_CONTENT);
        return writer;
    }

    private static class AnnexBinaryValue extends ExternalBinaryValue {

        private static String idForFile(File f) {
            try {
                return f.toURI().toURL().toExternalForm();
            } catch (MalformedURLException ex) {
                return null;
            }
        }

        private static AnnexBinaryValue newBinaryValue(String sourceName, File symlink, Path target) {
            String sha1 = "0000000000000000000000000000000000000001";
            long size = 1L;
            String mimeType = "text/plain";
            String id = idForFile(symlink);
            return new AnnexBinaryValue(sha1, sourceName, id, size, mimeType);
        }

        private AnnexBinaryValue(String sha1, String sourceName, String id, long size, String mimeType) {
            super(new BinaryKey(sha1), sourceName, id, size, null, null);
            super.setMimeType(mimeType);
        }

        @Override
        protected InputStream internalStream() throws Exception {
            return new ByteArrayInputStream(EMPTY_CONTENT);
        }
    }
}
