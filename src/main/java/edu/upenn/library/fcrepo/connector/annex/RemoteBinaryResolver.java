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
import java.nio.file.Path;
import org.modeshape.jcr.mimetype.MimeTypeDetector;

/**
 *
 * @author magibney
 */
public interface RemoteBinaryResolver {

    RemoteBinaryMetadata resolve(File file, MimeTypeDetector mimeTypeDetector);

    RemoteBinaryMetadata resolve(File file, Path target, MimeTypeDetector mimeTypeDetector);
    
}
