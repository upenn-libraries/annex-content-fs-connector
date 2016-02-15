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
import java.nio.file.Files;
import java.nio.file.Path;
import org.fcrepo.connector.file.FedoraFileSystemConnector;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.spi.federation.DocumentWriter;
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
    public Document getDocumentById(String id) {
        Document ret = super.getDocumentById(id);
        System.err.println("XXX getDocumentById: "+id);
        return ret;
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
                    return Files.notExists(target);
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
                if (!Files.notExists(target)) {
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

    @Override
    public String sha1(final File file) {
        if (!isExcluded(file) && file.exists() && file.canRead()) {
            return super.sha1(file);
        } else {
            final String cachedSha1 = getCachedSha1(file);
            if (cachedSha1 != null) {
                return cachedSha1;
            } else {
                String dummySha1 = "0000000000000000000000000000000000000000";
                final String id = idFor(file) + JCR_CONTENT_SUFFIX;
                cacheSha1(id, file, dummySha1);
                return dummySha1;
            }
        }
    }

}
